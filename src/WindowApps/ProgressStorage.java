package WindowApps;

import WindowApps.clicker.GameProgress;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.DosFileAttributeView;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Бинарное хранилище прогресса: файл подписи (HMAC-SHA256) + AES/GCM.
 * Лежит в %APPDATA%\Clicker\clicker.sav (или ~/.clicker на не-Windows),
 * старый config.properties читается один раз как миграция.
 */
public class ProgressStorage {

    private static final byte[] FORMAT_MAGIC = {'C', 'L', 'K', '2'};
    private static final byte FORMAT_VERSION = 2;
    private static final int MAC_LENGTH = 32;
    private static final int NONCE_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int CHECKSUM_LENGTH = Long.BYTES;
    private static final String SIGNING_KEY = "Clicker::v2::HKq7w2LmTr9xVb4n";
    private static final String CIPHER_KEY = "ChlrKey::9fAs2Kd81PqZ";
    private static final String LEGACY_FILE = "config.properties";
    private static final String STORE_FILE = "clicker.sav";

    private final GameProgress progress;
    private final Path storePath = resolveStorePath();
    private final SecureRandom random = new SecureRandom();

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
            // Файла нет — возможно, остался прогресс старого формата.
            record = data == null ? readLegacy() : null;
            corrupted = data != null;
        }

        if (record != null) {
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
                Text.getLanguage(),
                progress.getTheme().name());

        byte[] payload = record.encode();
        byte[] nonce = new byte[NONCE_LENGTH];
        random.nextBytes(nonce);
        byte[] encrypted = encrypt(payload, nonce);

        ByteBuffer body = ByteBuffer.allocate(FORMAT_MAGIC.length + 1 + NONCE_LENGTH + encrypted.length);
        body.put(FORMAT_MAGIC).put(FORMAT_VERSION).put(nonce).put(encrypted);
        byte[] signed = body.array();

        ByteBuffer file = ByteBuffer.allocate(signed.length + MAC_LENGTH);
        file.put(signed).put(hmac(signed));

        Path parent = storePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(storePath, file.array());
        hide(storePath);
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
        if (!java.util.Arrays.equals(magic, FORMAT_MAGIC) || buffer.get() != FORMAT_VERSION) {
            return null;
        }

        byte[] nonce = new byte[NONCE_LENGTH];
        buffer.get(nonce);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);

        try {
            return ProgressRecord.decode(decrypt(encrypted, nonce));
        } catch (RuntimeException e) {
            return null;
        }
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

    private void hide(Path path) {
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

    private Path resolveStorePath() {
        String appData = System.getenv("APPDATA");
        Path base = (appData == null || appData.isBlank())
                ? Paths.get(System.getProperty("user.home"), ".clicker")
                : Paths.get(appData, "Clicker");
        return base.resolve(STORE_FILE);
    }

    private static byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SIGNING_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] encrypt(byte[] payload, byte[] nonce) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(CIPHER_KEY.getBytes(StandardCharsets.UTF_8), "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return cipher.doFinal(payload);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] decrypt(byte[] encrypted, byte[] nonce) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(CIPHER_KEY.getBytes(StandardCharsets.UTF_8), "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private record ProgressRecord(int clicks, int clicksPerPress, int autoClickerLevel, int multiplierLevel,
                                  String language, String theme) {

        /** 4 int + два short-длины + контрольная сумма FNV-1a. */
        private static final int FIXED_LENGTH = 4 * Integer.BYTES + 2 * Short.BYTES + CHECKSUM_LENGTH;

        byte[] encode() {
            byte[] lang = language.getBytes(StandardCharsets.UTF_8);
            byte[] themeCode = theme.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(FIXED_LENGTH + lang.length + themeCode.length);
            buffer.putInt(clicks);
            buffer.putInt(clicksPerPress);
            buffer.putInt(autoClickerLevel);
            buffer.putInt(multiplierLevel);
            buffer.putShort((short) lang.length).put(lang);
            buffer.putShort((short) themeCode.length).put(themeCode);
            buffer.putLong(checksum(buffer.array(), buffer.position()));
            return buffer.array();
        }

        static ProgressRecord decode(byte[] payload) {
            if (payload.length < FIXED_LENGTH) {
                throw new IllegalArgumentException("short payload");
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            int clicks = buffer.getInt();
            int clicksPerPress = buffer.getInt();
            int autoClickerLevel = buffer.getInt();
            int multiplierLevel = buffer.getInt();

            int langLength = buffer.getShort() & 0xFFFF;
            int themeLengthOffset = buffer.position() + langLength;
            if (themeLengthOffset + Short.BYTES > payload.length - CHECKSUM_LENGTH) {
                throw new IllegalArgumentException("bad string length");
            }
            byte[] lang = new byte[langLength];
            buffer.get(lang);
            byte[] themeCode = new byte[buffer.getShort() & 0xFFFF];
            buffer.get(themeCode);

            int checksumOffset = buffer.position();
            long expected = buffer.getLong();
            long actual = checksum(payload, checksumOffset);
            if (expected != actual) {
                throw new IllegalArgumentException("checksum mismatch");
            }
            return new ProgressRecord(clicks, clicksPerPress, autoClickerLevel, multiplierLevel,
                    new String(lang, StandardCharsets.UTF_8), new String(themeCode, StandardCharsets.UTF_8));
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
