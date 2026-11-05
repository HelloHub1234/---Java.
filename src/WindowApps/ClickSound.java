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
 * Тихий звук нажатия кнопки. Сэмпл синтезируется в памяти (короткий «тик» из синуса с затуханием),
 * поэтому в репозиторий не попадает ни одного аудиофайла, а звук работает и в собранном .exe.
 * <p>
 * Сэмпл держится в одной структуре {@link Clip}, а вызовы идут через её собственный поток:
 * {@code Clip#setFramePosition(0) + start()} на уже играющем клипе бросает
 * {@code IllegalStateException}, и при быстрых кликах это ломало бы звук.
 */
public final class ClickSound {

    private static final float SAMPLE_RATE = 44100f;
    private static final int BITS = 16;
    private static final int CHANNELS = 1;
    /** Длительность «тика»: коротко, чтобы не раздражать при быстром клике. */
    private static final int DURATION_MS = 90;
    /** Базовая частота: 880 Гц читается как мягкое «щёлк» и не режет ухо. */
    private static final double FREQUENCY = 880;
    /** Общая громкость относительно полной шкалы: звук должен быть едва слышным. */
    private static final double VOLUME = 0.14;

    private static Clip clip;
    private static boolean unavailable;

    private ClickSound() {
    }

    /** Проигрывает короткий звук нажатия. Безопасно вызывать из EDT сколько угодно раз. */
    public static void play() {
        if (unavailable) {
            return;
        }
        try {
            Clip target = clip();
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

    private static Clip clip() throws Exception {
        if (clip != null) {
            return clip;
        }
        byte[] pcm = generateTick();
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
            clip = created;
            return clip;
        }
    }

    /** Один «тик»: синус с экспоненциальным затуханием и короткой атакой против щелчка. */
    private static byte[] generateTick() {
        int samples = (int) (SAMPLE_RATE * DURATION_MS / 1000f);
        ByteArrayOutputStream out = new ByteArrayOutputStream(samples * 2);
        for (int i = 0; i < samples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / samples;
            double envelope = Math.exp(-progress * 7.5);
            // Первые миллисекунды гасим атаку, иначе получается резкий цифровой щелчок.
            double attack = Math.min(1.0, t / 0.004);
            double wave = Math.sin(2 * Math.PI * FREQUENCY * t) * 0.75
                    + Math.sin(2 * Math.PI * FREQUENCY * 2 * t) * 0.25;
            short value = (short) Math.round(wave * envelope * attack * VOLUME * Short.MAX_VALUE);
            out.write(value & 0xFF);
            out.write((value >>> 8) & 0xFF);
        }
        return out.toByteArray();
    }

    /** Отключает звук (нужно, если пользователь не хочет аудио, например в тестах). */
    static void disable() {
        unavailable = true;
        if (clip != null) {
            clip.close();
            clip = null;
        }
    }
}
