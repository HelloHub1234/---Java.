package WindowApps;

import WindowApps.clicker.model.Achievement;
import WindowApps.clicker.model.GameProgress;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Бинарное хранилище прогресса: AES/GCM-шифрование + HMAC-SHA256 поверх конверта.
 * Лежит в %APPDATA%\Clicker\clicker.sav (или ~/.clicker на не-Windows),
 * старый config.properties читается один раз как миграция.
 *
 * <p>Файл дополнительно закрывается правами ОС: на Windows из ACL убираются записи всех,
 * кроме владельца и SYSTEM, на POSIX — выставляются права 600. Это защищает от случайного
 * или небрежного редактирования; от владельца процесса данные, разумеется, не защищены:
 * ключ лежит в самом приложении.
 */
public class ProgressStorage {

    private static final byte[] FORMAT_MAGIC = {'C', 'L', 'K', '4'};
    /** Версия 4: в прогресс добавлены полученные достижения. */
    private static final byte FORMAT_VERSION = 4;
    /** Версия 3: исправлена длина AES-ключа; достижений в файле нет. */
    private static final byte PREVIOUS_FORMAT_VERSION = 3;
    private static final byte LEGACY_FORMAT_VERSION = 2;
    private static final int MAC_LENGTH = 32;
    private static final int NONCE_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int CHECKSUM_LENGTH = Long.BYTES;
    private static final int AES_KEY_BYTES = 32;
    private static final String SIGNING_SECRET = "Clicker::v3::HKq7w2LmTr9xVb4nQz8Yt";
    private static final String CIPHER_SECRET = "ChlrKey::v3::9fAs2Kd81PqZx7Lm";
    /** Ключ старого формата — только чтобы прочитать progress.sav, созданный версией 2. */
    private static final String LEGACY_CIPHER_SECRET = "ChlrKey::9fAs2Kd81PqZ";
    private static final String LEGACY_FILE = "config.properties";
    private static final String STORE_FILE = "clicker.sav";

    private final GameProgress progress;
    private final Path storePath = resolveStorePath();
    private final SecureRandom random = new SecureRandom();

    private final SecretKeySpec signingKey = new SecretKeySpec(deriveKey(SIGNING_SECRET), "HmacSHA256");
    private final SecretKeySpec cipherKey = new SecretKeySpec(deriveKey(CIPHER_SECRET), "AES");

    public ProgressStorage(GameProgress progress) {
        this.progress = progress;
    }

    public Path getStorePath() {
        return storePath;
    }

    public boolean save() {
        try {
            write();
            return true;
        } catch (IOException | RuntimeException e) {
            System.err.println(Text.format("error.save", e.getMessage()));
            return false;
        }
    }

    public void load() {
        Theme theme = null;
        byte[] data = null;
        try {
            if (Files.exists(storePath)) {
                data = Files.readAllBytes(storePath);
            }
        } catch (IOException e) {
            System.err.println(Text.format("error.load", e.getMessage()));
        }

        boolean corrupted = false;
        ProgressRecord record = data == null ? null : read(data);
        if (record == null) {
            // Файла нет — возможно, остался прогресс старого текстового формата.
            record = data == null ? readLegacy() : null;
            corrupted = data != null;
        }

        if (record != null) {
            // Сначала достижения из файла, затем состояние: setState доберёт те, что уже выполнены,
            // но почему-то не были записаны, и ничего не потеряет.
            progress.restoreAchievements(record.achievements());
            progress.setState(record.clicks(), record.clicksPerPress(), record.autoClickerLevel(),
                    record.multiplierLevel());
            Text.setLanguage(record.language());
            theme = Theme.byName(record.theme());
        } else {
            progress.setState(0, 1, 0, 0);
        }

        progress.setTheme(theme == null ? Theme.STANDARD : theme);
        progress.markSaved();
        if (corrupted) {
            System.err.println(Text.format("error.corrupted", storePath));
        }
    }

    public void deleteStore() {
        try {
            if (Files.exists(storePath)) {
                byte[] zeros = new byte[(int) Files.size(storePath)];
                Files.write(storePath, zeros, StandardOpenOption.WRITE);
                Files.delete(storePath);
            }
        } catch (IOException ignored) {
            // несмотря на ошибку, прогресс сброшен в памяти
        }
        progress.setState(0, 1, 0, 0);
        progress.markDirty();
    }

    private void write() throws IOException {
        ProgressRecord record = new ProgressRecord(
                progress.getClickCount(),
                progress.getClicksPerPress(),
                progress.getAutoClickerLevel(),
                progress.getMultiplierLevel(),
                progress.getUnlockedAchievements(),
                Text.getLanguage(),
                progress.getTheme().name());

        byte[] payload = record.encode();
        byte[] nonce = new byte[NONCE_LENGTH];
        random.nextBytes(nonce);
        byte[] encrypted = encrypt(payload, nonce, cipherKey);

        ByteBuffer body = ByteBuffer.allocate(FORMAT_MAGIC.length + 1 + NONCE_LENGTH + encrypted.length);
        body.put(FORMAT_MAGIC).put(FORMAT_VERSION).put(nonce).put(encrypted);
        byte[] signed = body.array();

        ByteBuffer file = ByteBuffer.allocate(signed.length + MAC_LENGTH);
        file.put(signed).put(hmac(signed));

        Path parent = storePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
            restrictDirectory(parent);
        }
        Files.write(storePath, file.array());
        restrictFile(storePath);
        progress.markSaved();
    }

    private ProgressRecord read(byte[] data) {
        if (data.length < FORMAT_MAGIC.length + 1 + NONCE_LENGTH + GCM_TAG_BITS / 8 + MAC_LENGTH) {
            return null;
        }
        int signedLength = data.length - MAC_LENGTH;
        byte[] signed = java.util.Arrays.copyOfRange(data, 0, signedLength);
        byte[] mac = java.util.Arrays.copyOfRange(data, signedLength, data.length);
        if (!MessageDigest.isEqual(mac, hmac(signed))) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(signed);
        byte[] magic = new byte[FORMAT_MAGIC.length];
        buffer.get(magic);
        boolean magicMatches = magic[0] == FORMAT_MAGIC[0] && magic[1] == FORMAT_MAGIC[1]
                && magic[2] == FORMAT_MAGIC[2];
        byte version = buffer.get();
        if (!magicMatches || (version != FORMAT_VERSION && version != PREVIOUS_FORMAT_VERSION
                && version != LEGACY_FORMAT_VERSION)) {
            return null;
        }
        if (magic[3] != (byte) ('0' + version)) {
            return null;
        }

        byte[] nonce = new byte[NONCE_LENGTH];
        buffer.get(nonce);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);

        SecretKeySpec key = version == LEGACY_FORMAT_VERSION
                ? new SecretKeySpec(legacyKeyBytes(), "AES")
                : cipherKey;
        try {
            return ProgressRecord.decode(decrypt(encrypted, nonce, key), version >= FORMAT_VERSION);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Ключ старого формата был некорректен (21 байт), поэтому расширяем его до допустимой длины AES.
     * Теоретически такой файл мог создать только патченный/исправленный билд v2.
     */
    private static byte[] legacyKeyBytes() {
        byte[] raw = deriveKey(LEGACY_CIPHER_SECRET);
        return java.util.Arrays.copyOf(raw, 24);
    }

    private ProgressRecord readLegacy() {
        Path legacy = Paths.get(LEGACY_FILE).toAbsolutePath();
        if (!Files.exists(legacy)) {
            return null;
        }
        ProgressRecord record;
        try {
            java.util.Properties props = new java.util.Properties();
            try (java.io.InputStream in = Files.newInputStream(legacy)) {
                props.load(in);
            }
            record = new ProgressRecord(
                    readInt(props, "clickCount", 0),
                    readInt(props, "clicksPerPress", 1),
                    readInt(props, "autoClicker", 0),
                    readInt(props, "multiplier", 0),
                    java.util.EnumSet.noneOf(Achievement.class),
                    languageTag(),
                    progress.getTheme().name());
        } catch (IOException e) {
            System.err.println(Text.format("error.load", e.getMessage()));
            return null;
        }
        try {
            byte[] zeros = new byte[(int) Files.size(legacy)];
            Files.write(legacy, zeros, StandardOpenOption.WRITE);
            Files.delete(legacy);
        } catch (IOException ignored) {
            // старый файл не критичен: он больше не читается
        }
        return record;
    }

    private static String languageTag() {
        String tag = java.util.Locale.getDefault().getLanguage();
        return Text.supportedLanguages().contains(tag) ? tag : "en";
    }

    private int readInt(java.util.Properties props, String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // --- защита файла средствами ОС -------------------------------------------------------------

    /** Оставляет владельцу полный доступ, всем остальным — никакого. */
    private static void restrictFile(Path path) {
        hide(path);
        dropInheritance(path);
    }

    private static void restrictDirectory(Path path) {
        restrictFile(path);
    }

    private static void hide(Path path) {
        try {
            DosFileAttributeView view = Files.getFileAttributeView(path, DosFileAttributeView.class);
            if (view != null) {
                view.setHidden(true);
                view.setSystem(false);
            }
        } catch (IOException ignored) {
            // на не-Windows файл остаётся без атрибута, но каталог всё равно скрыт
        }
    }

    private static void dropInheritance(Path path) {
        try {
            AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
            if (acl != null) {
                lockDownAcl(acl);
                return;
            }
        } catch (IOException | RuntimeException ignored) {
            // файловая система без ACL (FAT/NTFS без прав) — пробуем POSIX ниже
        }
        try {
            Set<PosixFilePermission> ownerOnly = PosixFilePermissions.fromString("rw-------");
            Files.setPosixFilePermissions(path, ownerOnly);
        } catch (IOException | UnsupportedOperationException ignored) {
            // ни ACL, ни POSIX — ограничение прав недоступно, оставляем как есть
        }
    }

    private static void lockDownAcl(AclFileAttributeView acl) throws IOException {
        List<AclEntry> entries = acl.getAcl();
        if (entries.isEmpty()) {
            return;
        }
        String current = currentUser();

        Set<AclEntryPermission> allowed = EnumSet.of(
                AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA,
                AclEntryPermission.APPEND_DATA, AclEntryPermission.READ_NAMED_ATTRS,
                AclEntryPermission.WRITE_NAMED_ATTRS, AclEntryPermission.EXECUTE,
                AclEntryPermission.DELETE_CHILD, AclEntryPermission.READ_ATTRIBUTES,
                AclEntryPermission.WRITE_ATTRIBUTES, AclEntryPermission.DELETE,
                AclEntryPermission.READ_ACL, AclEntryPermission.WRITE_ACL,
                AclEntryPermission.WRITE_OWNER, AclEntryPermission.SYNCHRONIZE);

        List<AclEntry> reset = new java.util.ArrayList<>();
        boolean ownerAdded = false;
        Set<String> seen = new java.util.HashSet<>();
        for (AclEntry entry : entries) {
            String principal = entry.principal().getName();
            if (entry.type() != AclEntryType.ALLOW || !seen.add(principal)) {
                continue;
            }
            boolean self = current != null && current.equalsIgnoreCase(principal);
            if (!self && !isSystemPrincipal(principal)) {
                // Все прочие (Пользователи, Authenticated Users, Everyone) теряют доступ.
                continue;
            }
            // Наследование снимаем: только явные записи, иначе права вернутся из родителя.
            reset.add(AclEntry.newBuilder(entry)
                    .setType(AclEntryType.ALLOW)
                    .setPermissions(self ? allowed : EnumSet.of(AclEntryPermission.READ_DATA,
                            AclEntryPermission.READ_ATTRIBUTES, AclEntryPermission.READ_ACL,
                            AclEntryPermission.SYNCHRONIZE))
                    .setFlags()
                    .build());
            ownerAdded |= self;
        }

        if (!ownerAdded && current != null) {
            // Текущего пользователя не было в ACL (например, файл от другой учётной записи) —
            // без доступа игра не сможет сохраняться, поэтому лучше оставить права как есть.
            return;
        }
        acl.setAcl(reset);
    }

    /** Имя текущего пользователя Windows; если узнать не удалось — null (тогда ACL не трогаем). */
    private static String currentUser() {
        try {
            return java.nio.file.Files.getOwner(Paths.get(".")).getName();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static boolean isSystemPrincipal(String name) {
        // Имена встроенных групп локализованы (BUILTIN\\Система, BUILTIN\\Администраторы),
        // поэтому проверяем все встречающиеся варианты окончаний.
        return name.endsWith("\\SYSTEM") || name.equals("SYSTEM")
                || name.endsWith("\\Система") || name.endsWith("\\СИСТЕМА")
                || name.endsWith("\\Администраторы") || name.endsWith("\\Administrators")
                || name.endsWith("\\Администратор") || name.endsWith("\\Administrator");
    }

    // --- криптография ---------------------------------------------------------------------------

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] encrypt(byte[] payload, byte[] nonce, SecretKeySpec key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return cipher.doFinal(payload);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] decrypt(byte[] encrypted, byte[] nonce, SecretKeySpec key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Растягивает пароль произвольной длины в ключ нужного размера через SHA-256.
     * Так длина секрета больше не может сломать AES (в версии 2 ключ был 21 байт
     * и AES/GCM падал с {@link java.security.InvalidKeyException}).
     */
    private static byte[] deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.Arrays.copyOf(digest.digest(secret.getBytes(StandardCharsets.UTF_8)), AES_KEY_BYTES);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Path resolveStorePath() {
        String appData = System.getenv("APPDATA");
        Path base = (appData == null || appData.isBlank())
                ? Paths.get(System.getProperty("user.home"), ".clicker")
                : Paths.get(appData, "Clicker");
        return base.resolve(STORE_FILE);
    }

    private record ProgressRecord(int clicks, int clicksPerPress, int autoClickerLevel, int multiplierLevel,
                                  Set<Achievement> achievements, String language, String theme) {

        /** 4 int + три short-длины + контрольная сумма FNV-1a. */
        private static final int FIXED_LENGTH = 4 * Integer.BYTES + 3 * Short.BYTES + CHECKSUM_LENGTH;

        byte[] encode() {
            byte[] lang = language.getBytes(StandardCharsets.UTF_8);
            byte[] themeCode = theme.getBytes(StandardCharsets.UTF_8);
            byte[] achievementCodes = encodeAchievements(achievements).getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(
                    FIXED_LENGTH + lang.length + themeCode.length + achievementCodes.length);
            buffer.putInt(clicks);
            buffer.putInt(clicksPerPress);
            buffer.putInt(autoClickerLevel);
            buffer.putInt(multiplierLevel);
            buffer.putShort((short) lang.length).put(lang);
            buffer.putShort((short) themeCode.length).put(themeCode);
            buffer.putShort((short) achievementCodes.length).put(achievementCodes);
            buffer.putLong(checksum(buffer.array(), buffer.position()));
            return buffer.array();
        }

        static ProgressRecord decode(byte[] payload, boolean hasAchievements) {
            int minimum = 4 * Integer.BYTES + 2 * Short.BYTES + CHECKSUM_LENGTH;
            if (payload.length < minimum) {
                throw new IllegalArgumentException("short payload");
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            int clicks = buffer.getInt();
            int clicksPerPress = buffer.getInt();
            int autoClickerLevel = buffer.getInt();
            int multiplierLevel = buffer.getInt();

            int langLength = buffer.getShort() & 0xFFFF;
            if (buffer.position() + langLength + Short.BYTES > payload.length - CHECKSUM_LENGTH) {
                throw new IllegalArgumentException("bad string length");
            }
            byte[] lang = new byte[langLength];
            buffer.get(lang);

            int themeLength = buffer.getShort() & 0xFFFF;
            if (buffer.position() + themeLength > payload.length - CHECKSUM_LENGTH) {
                throw new IllegalArgumentException("bad theme length");
            }
            byte[] themeCode = new byte[themeLength];
            buffer.get(themeCode);

            Set<Achievement> achievements = java.util.EnumSet.noneOf(Achievement.class);
            if (hasAchievements) {
                if (buffer.position() + Short.BYTES > payload.length - CHECKSUM_LENGTH) {
                    throw new IllegalArgumentException("missing achievements length");
                }
                int achievementLength = buffer.getShort() & 0xFFFF;
                if (buffer.position() + achievementLength > payload.length - CHECKSUM_LENGTH) {
                    throw new IllegalArgumentException("bad achievements length");
                }
                byte[] achievementCodes = new byte[achievementLength];
                buffer.get(achievementCodes);
                achievements = decodeAchievements(new String(achievementCodes, StandardCharsets.UTF_8));
            }

            int checksumOffset = buffer.position();
            long expected = buffer.getLong();
            long actual = checksum(payload, checksumOffset);
            if (expected != actual) {
                throw new IllegalArgumentException("checksum mismatch");
            }
            return new ProgressRecord(clicks, clicksPerPress, autoClickerLevel, multiplierLevel,
                    achievements, new String(lang, StandardCharsets.UTF_8),
                    new String(themeCode, StandardCharsets.UTF_8));
        }

        private static String encodeAchievements(Set<Achievement> achievements) {
            StringBuilder builder = new StringBuilder();
            for (Achievement achievement : achievements) {
                if (builder.length() > 0) {
                    builder.append(',');
                }
                builder.append(achievement.getId());
            }
            return builder.toString();
        }

        private static Set<Achievement> decodeAchievements(String value) {
            Set<Achievement> achievements = java.util.EnumSet.noneOf(Achievement.class);
            if (value.isEmpty()) {
                return achievements;
            }
            for (String id : value.split(",")) {
                Achievement achievement = Achievement.byId(id.trim());
                if (achievement != null) {
                    achievements.add(achievement);
                }
            }
            return achievements;
        }

        private static long checksum(byte[] data, int length) {
            long hash = 0xCBF29CE484222325L;
            for (int i = 0; i < length; i++) {
                hash ^= data[i] & 0xFF;
                hash *= 0x100000001B3L;
            }
            return hash;
        }
    }
}
