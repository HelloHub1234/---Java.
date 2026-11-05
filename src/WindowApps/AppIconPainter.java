package WindowApps;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Единый источник иконки приложения: рисует значок в цветах темы.
 * <p>
 * Используется и окном ({@link Basic#createAppIcons()}), и офлайн-утилитой
 * {@code tools/IconExport}, которая генерирует {@code clicker.ico} для .exe (jpackage --icon),
 * поэтому картинка в окне и в Проводнике совпадает.
 */
public final class AppIconPainter {

    /** Разрешение растра для генератора, чтобы не тащить сюда Basic (и весь Swing) ради одной константы. */
    public static final int RASTER_SIZE = 256;

    private AppIconPainter() {
    }

    /**
     * Рисует иконку на весь {@code size} без внутренних отступов и прозрачного «воздуха»:
     * так её можно сразу рендерить в PNG/ICO нужного разрешения.
     */
    public static void paint(Graphics2D g2, double size, Theme theme) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Фон — сплошная заливка по всему размеру, без скругления и отступов.
        Color top = blend(theme.getAccent(), Color.WHITE, 0.18);
        g2.setPaint(new GradientPaint(0f, 0f, top, 0f, (float) size, theme.getBackgroundBottom()));
        g2.fillRect(0, 0, (int) Math.round(size), (int) Math.round(size));

        Path2D.Double cursor = cursorShape(size);
        // Никакого контура-обводки: при 16×16 она заливает просвет между телом и хвостом.
        // «Дырку» делает сам фон — вокруг курсора его достаточно.
        g2.setPaint(fillPaint(size, top, theme));
        g2.fill(cursor);
    }

    /**
     * Классический курсор-«стрелка»: остриё влево-вверх, правое плечо, выемка и отделённый ею хвост.
     * Координаты — доли от size, поэтому форма одинаково читается и в 16, и в 256 пикселях.
     */
    private static Path2D.Double cursorShape(double size) {
        double x = size * 0.22;
        double y = size * 0.13;
        double w = size * 0.48;
        double h = size * 0.72;

        Path2D.Double p = new Path2D.Double();
        p.moveTo(x, y);                        // остриё
        p.lineTo(x + w * 0.88, y + h * 0.33);  // правое плечо
        p.lineTo(x + w * 0.56, y + h * 0.52);  // выемка — начало хвоста
        p.lineTo(x + w * 0.92, y + h * 0.98);  // кончик хвоста
        p.lineTo(x + w * 0.66, y + h * 1.02);
        p.lineTo(x + w * 0.34, y + h * 0.33);  // возврат по хвосту вглубь — даёт зазор с телом
        p.lineTo(x, y + h * 0.56);             // нижний край тела, замкнёт на остриё
        p.closePath();
        return p;
    }

    /** Градиент глифа: от осветлённого акцента к контрастному к фону цвету. */
    private static Paint fillPaint(double size, Color light, Theme theme) {
        // На светлых темах (SOLAR) акцент почти не подмешиваем и дополнительно затемняем:
        // иначе глиф по светлоте сравнивается с фоном и теряет контраст.
        boolean lightTheme = luminance(theme.getText()) < luminance(theme.getBackgroundTop());
        Color dark = lightTheme
                ? blend(theme.getText(), Color.BLACK, 0.35)
                : blend(theme.getText(), theme.getAccent(), 0.45);
        return new GradientPaint(0f, 0f, light, (float) size, (float) size, dark);
    }

    private static double luminance(Color color) {
        return 0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue();
    }

    /** Линейное смешивание цветов: weight — доля второго цвета (0…1). */
    public static Color blend(Color first, Color second, double weight) {
        double w = Math.max(0, Math.min(1, weight));
        return new Color(
                (int) Math.round(first.getRed() * (1 - w) + second.getRed() * w),
                (int) Math.round(first.getGreen() * (1 - w) + second.getGreen() * w),
                (int) Math.round(first.getBlue() * (1 - w) + second.getBlue() * w),
                (int) Math.round(first.getAlpha() * (1 - w) + second.getAlpha() * w));
    }

    /** Растровая иконка нужного размера (для окна и для экспорта в PNG). */
    public static BufferedImage render(int size, Theme theme) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            paint(g2, size, theme);
        } finally {
            g2.dispose();
        }
        return image;
    }

    /** Иконка с прозрачным скруглённым «воздухом» — её и показывает окно. */
    public static BufferedImage renderPadded(int size, Theme theme) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            double inset = size * 0.06;
            double side = size - 2 * inset;
            double radius = side * 0.2;

            RoundRectangle2D.Double clip = new RoundRectangle2D.Double(inset, inset, side, side, radius, radius);
            Graphics2D tile = (Graphics2D) g2.create();
            try {
                tile.clip(clip);
                tile.translate(inset, inset);
                paint(tile, side, theme);
            } finally {
                tile.dispose();
            }
        } finally {
            g2.dispose();
        }
        return image;
    }
}
