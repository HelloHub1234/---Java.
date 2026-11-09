package WindowApps;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Короткие звуки интерфейса. Все сэмплы синтезируются в памяти (синус с затуханием),
 * поэтому в репозиторий не попадает ни одного аудиофайла, а звук работает и в собранном .exe.
 * <p>
 * Каждый сэмпл держится в одной структуре {@link Clip}, и вызовы идут через её собственный поток:
 * {@code Clip#setFramePosition(0) + start()} на уже играющем клипе бросает
 * {@code IllegalStateException}, и при быстрых кликах это ломало бы звук.
 */
public final class ClickSound {

    private static final float SAMPLE_RATE = 44100f;
    private static final int BITS = 16;
    private static final int CHANNELS = 1;

    private static Clip buttonClip;
    private static Clip clickClip;
    private static Clip achievementClip;
    private static Clip resetClip;
    private static boolean unavailable;

    private ClickSound() {
    }

    /** Тихий «тик» для второстепенных кнопок: 880 Гц, 90 мс. */
    public static void play() {
        playButton();
    }

    /** Звук нажатия второстепенной кнопки. Безопасно вызывать из EDT сколько угодно раз. */
    public static void playButton() {
        trigger(Slot.BUTTON, 880, 90, 0.14);
    }

    /**
     * Звук главной кнопки «Нажми»: ниже и короче кнопочного, чтобы на слух отличался
     * от тиканья панели — это отдельное действие игрока, а не служебная кнопка.
     */
    public static void playClick() {
        trigger(Slot.CLICK, 420, 70, 0.20);
    }

    /**
     * Награда за достижение: короткое восходящее арпеджио C-E-G-C, тише кнопок.
     * Звук должен радовать, но не перекрывать клики, поэтому громкость ниже и хвост мягкий.
     */
    public static void playAchievement() {
        trigger(Slot.ACHIEVEMENT, 0, 0, 0);
    }

    /**
     * Сброс прогресса: нисходящий «вздох» — сигнал, что всё начинается заново.
     * Намеренно тихий и без резкой атаки: это не награда, а подтверждение действия.
     */
    public static void playReset() {
        trigger(Slot.RESET, 0, 0, 0);
    }

    /** Какой из кэшированных клипов проигрывать. */
    private enum Slot {
        BUTTON, CLICK, ACHIEVEMENT, RESET
    }

    private static void trigger(Slot slot, double frequency, int durationMs, double volume) {
        if (unavailable) {
            return;
        }
        try {
            Clip target = clip(slot, frequency, durationMs, volume);
            if (target == null) {
                return;
            }
            // Сброс позиции на лету безопасен: клип останавливает сам себя по LineEvent.STOP.
            target.setFramePosition(0);
            target.start();
        } catch (Exception e) {
            // Звук — украшение: если микшера нет (сервер, терминал-сессия), просто выключаемся.
            unavailable = true;
        }
    }

    private static synchronized Clip clip(Slot slot, double frequency, int durationMs, double volume)
            throws Exception {
        Clip cached = switch (slot) {
            case BUTTON -> buttonClip;
            case CLICK -> clickClip;
            case ACHIEVEMENT -> achievementClip;
            case RESET -> resetClip;
        };
        if (cached != null) {
            return cached;
        }
        byte[] pcm = switch (slot) {
            case BUTTON, CLICK -> generate(frequency, durationMs, volume);
            case ACHIEVEMENT -> generateChime();
            case RESET -> generateSweep();
        };
        Clip created = open(pcm);
        switch (slot) {
            case BUTTON -> buttonClip = created;
            case CLICK -> clickClip = created;
            case ACHIEVEMENT -> achievementClip = created;
            case RESET -> resetClip = created;
        }
        return created;
    }

    private static Clip open(byte[] pcm) throws Exception {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, BITS, CHANNELS, true, false);
        try (InputStream in = new ByteArrayInputStream(pcm)) {
            AudioInputStream stream = new AudioInputStream(in, format, pcm.length / (BITS / 8L));
            Clip created = AudioSystem.getClip();
            created.open(stream);
            created.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    created.setFramePosition(0);
                }
            });
            return created;
        }
    }

    /**
     * Один «тик»: синус с экспоненциальным затуханием и короткой атакой против щелчка.
     * Вторая гармоника добавляет «деревянности» — так звук не похож на писк динамика.
     */
    private static byte[] generate(double frequency, int durationMs, double volume) {
        int samples = (int) (SAMPLE_RATE * durationMs / 1000f);
        ByteArrayOutputStream out = new ByteArrayOutputStream(samples * 2);
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / samples;
            double envelope = Math.exp(-progress * 7.5);
            // Первые миллисекунды гасим атаку, иначе получается резкий цифровой щелчок.
            double attack = Math.min(1.0, t / 0.004);
            double wave = Math.sin(2 * Math.PI * frequency * t) * 0.75
                    + Math.sin(2 * Math.PI * frequency * 2 * t) * 0.25;
            short value = (short) Math.round(wave * envelope * attack * volume * Short.MAX_VALUE);
            out.write(value & 0xFF);
            out.write((value >>> 8) & 0xFF);
        }
        return out.toByteArray();
    }

    /**
     * Арпеджио достижения: четыре ноты C5-E5-G5-C6, каждая со своим затуханием.
     * Ноты накладываются друг на друга — получается «звон», а не четыре отдельных пика.
     */
    private static byte[] generateChime() {
        double[] notes = {523.25, 659.25, 783.99, 1046.50};
        int noteMs = 110;
        int tailMs = 260;
        int totalMs = noteMs * (notes.length - 1) + tailMs;
        int samples = (int) (SAMPLE_RATE * totalMs / 1000f);
        ByteArrayOutputStream out = new ByteArrayOutputStream(samples * 2);
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double wave = 0;
            for (int n = 0; n < notes.length; n++) {
                double start = n * noteMs / 1000.0;
                if (t < start) {
                    continue;
                }
                double local = t - start;
                double envelope = Math.exp(-local * 9.0) * Math.min(1.0, local / 0.006);
                wave += Math.sin(2 * Math.PI * notes[n] * local) * envelope;
            }
            // 0.09 на ноту: четыре наложенные ноты в сумме дают ~0.36 — тише кнопочного тика.
            short value = (short) Math.round(wave * 0.09 * Short.MAX_VALUE);
            out.write(value & 0xFF);
            out.write((value >>> 8) & 0xFF);
        }
        return out.toByteArray();
    }

    /** Нисходящий свип 660 → 220 Гц: «всё сбрасывается», без резкой атаки. */
    private static byte[] generateSweep() {
        int durationMs = 520;
        int samples = (int) (SAMPLE_RATE * durationMs / 1000f);
        ByteArrayOutputStream out = new ByteArrayOutputStream(samples * 2);
        double phase = 0;
        for (int i = 0; i < samples; i++) {
            double progress = (double) i / samples;
            double frequency = 660 - 440 * progress;
            phase += 2 * Math.PI * frequency / SAMPLE_RATE;
            double envelope = Math.exp(-progress * 2.6) * Math.min(1.0, progress * 12);
            double wave = Math.sin(phase) * 0.8 + Math.sin(phase * 2) * 0.2;
            short value = (short) Math.round(wave * envelope * 0.12 * Short.MAX_VALUE);
            out.write(value & 0xFF);
            out.write((value >>> 8) & 0xFF);
        }
        return out.toByteArray();
    }

    /** Отключает звук (нужно, если пользователь не хочет аудио, например в тестах). */
    static void disable() {
        unavailable = true;
        closeQuietly(buttonClip);
        closeQuietly(clickClip);
        closeQuietly(achievementClip);
        closeQuietly(resetClip);
        buttonClip = null;
        clickClip = null;
        achievementClip = null;
        resetClip = null;
    }

    private static void closeQuietly(Clip clip) {
        if (clip != null) {
            clip.close();
        }
    }
}
