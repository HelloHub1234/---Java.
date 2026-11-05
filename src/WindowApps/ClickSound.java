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
    private static boolean unavailable;

    private ClickSound() {
    }

    /** Тихий «тик» для второстепенных кнопок: 880 Гц, 90 мс. */
    public static void play() {
        playButton();
    }

    /** Звук нажатия второстепенной кнопки. Безопасно вызывать из EDT сколько угодно раз. */
    public static void playButton() {
        trigger(true, 880, 90, 0.14);
    }

    /**
     * Звук главной кнопки «Нажми»: ниже и короче кнопочного, чтобы на слух отличался
     * от тиканья панели — это отдельное действие игрока, а не служебная кнопка.
     */
    public static void playClick() {
        trigger(false, 420, 70, 0.20);
    }

    private static void trigger(boolean primary, double frequency, int durationMs, double volume) {
        if (unavailable) {
            return;
        }
        try {
            Clip target = primary ? clip(true, frequency, durationMs, volume) : clip(false, frequency, durationMs, volume);
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

    private static synchronized Clip clip(boolean primary, double frequency, int durationMs, double volume)
            throws Exception {
        if (primary && buttonClip != null) {
            return buttonClip;
        }
        if (!primary && clickClip != null) {
            return clickClip;
        }
        Clip created = open(generate(frequency, durationMs, volume));
        if (primary) {
            buttonClip = created;
        } else {
            clickClip = created;
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

    /** Отключает звук (нужно, если пользователь не хочет аудио, например в тестах). */
    static void disable() {
        unavailable = true;
        closeQuietly(buttonClip);
        closeQuietly(clickClip);
        buttonClip = null;
        clickClip = null;
    }

    private static void closeQuietly(Clip clip) {
        if (clip != null) {
            clip.close();
        }
    }
}
