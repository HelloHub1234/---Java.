package tools;

import WindowApps.AppIconPainter;
import WindowApps.Theme;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Генерирует {@code assets/clicker.ico} из кода отрисовки иконки, чтобы .exe (jpackage --icon)
 * и окно приложения выглядели одинаково.
 * <p>
 * Запуск (из корня проекта, окно при этом не открывается):
 * <pre>
 * javac -encoding UTF-8 -d classes-out src/WindowApps/Theme.java src/WindowApps/AppIconPainter.java ^
 *       src/tools/IconExport.java
 * java -Djava.awt.headless=true -cp classes-out tools.IconExport
 * </pre>
 * Второй аргумент (необязательный) — путь к .ico, по умолчанию {@code assets/clicker.ico}.
 */
public final class IconExport {

    /** Размеры, которые Windows реально берёт из .ico: 16/32/48 для панелей и крупные для плиток. */
    private static final int[] ICO_SIZES = {16, 24, 32, 48, 64, 128, 256};

    private IconExport() {
    }

    public static void main(String[] args) throws IOException {
        Path target = Paths.get(args.length > 0 ? args[0] : "assets/clicker.ico");
        List<BufferedImage> images = new ArrayList<>();
        for (int size : ICO_SIZES) {
            images.add(AppIconPainter.render(size, Theme.STANDARD));
        }

        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        try (OutputStream out = Files.newOutputStream(target)) {
            writeIco(images, out);
        }
        System.out.println("Готово: " + target.toAbsolutePath() + " (" + ICO_SIZES.length + " размеров)");
    }

    /** Минимальный кодировщик ICO: заголовок + директория + PNG-кадры (Windows Vista+). */
    private static void writeIco(List<BufferedImage> images, OutputStream out) throws IOException {
        List<byte[]> frames = new ArrayList<>();
        for (BufferedImage image : images) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(image, "png", buffer);
            frames.add(buffer.toByteArray());
        }

        DataOutputStream data = new DataOutputStream(out);
        writeLE16(data, 0);             // reserved
        writeLE16(data, 1);             // type: 1 = icon
        writeLE16(data, frames.size()); // число кадров

        int offset = 6 + 16 * frames.size();
        for (int i = 0; i < frames.size(); i++) {
            BufferedImage image = images.get(i);
            int width = image.getWidth() >= 256 ? 0 : image.getWidth();   // 0 означает 256
            int height = image.getHeight() >= 256 ? 0 : image.getHeight();
            data.writeByte(width);
            data.writeByte(height);
            data.writeByte(0);  // цветовая палитра
            data.writeByte(0);  // reserved
            writeLE16(data, 1); // color planes
            writeLE16(data, 32);
            writeLE32(data, frames.get(i).length);
            writeLE32(data, offset);
            offset += frames.get(i).length;
        }
        for (byte[] frame : frames) {
            data.write(frame);
        }
        data.flush();
    }

    private static void writeLE16(DataOutputStream out, int value) throws IOException {
        out.writeByte(value & 0xFF);
        out.writeByte((value >>> 8) & 0xFF);
    }

    private static void writeLE32(DataOutputStream out, int value) throws IOException {
        out.writeByte(value & 0xFF);
        out.writeByte((value >>> 8) & 0xFF);
        out.writeByte((value >>> 16) & 0xFF);
        out.writeByte((value >>> 24) & 0xFF);
    }
}
