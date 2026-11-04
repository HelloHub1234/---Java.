package WindowApps.clicker;

import WindowApps.Basic;
import WindowApps.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Главная кнопка кликера: градиент, светящийся ореол акцентного цвета,
 * бегущий блик-«шиммер» и проседание при нажатии.
 */
public class ClickButton extends JButton {

    private static final long serialVersionUID = 1L;

    private static final int ARC = 34;
    private static final int SHIMMER_PERIOD_MS = 2600;
    private static final int SHIMMER_FRAME_MS = 33;

    private final Basic owner;
    private final Timer shimmerTimer;

    public ClickButton(Basic owner, String text) {
        super(text);
        this.owner = owner;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        shimmerTimer = new Timer(SHIMMER_FRAME_MS, e -> repaint());
        shimmerTimer.start();
    }

    /** Останавливает анимацию, когда кнопка больше не нужна (окно пересобирается). */
    public void stopAnimation() {
        shimmerTimer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Theme theme = owner.getActiveTheme();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        boolean pressed = getModel().isPressed();
        boolean rollover = getModel().isRollover();

        int inset = 10;
        int width = getWidth() - inset * 2;
        int height = getHeight() - inset * 2;
        if (width <= 0 || height <= 0) {
            g2.dispose();
            return;
        }
        int sink = pressed ? 3 : 0;
        int y = inset + sink;

        Color accent = theme.getAccent();
        Color accentHover = theme.getAccentHover();

        // Ореол: расширяющиеся полупрозрачные контуры акцентного цвета.
        int glow = rollover ? 9 : 6;
        for (int i = glow; i >= 1; i--) {
            int alpha = Math.round((rollover ? 11f : 7f) * (glow - i + 1));
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), Math.min(150, alpha)));
            g2.setStroke(new BasicStroke(i * 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new RoundRectangle2D.Float(inset - i, y - i, width + i * 2f, height + i * 2f, ARC, ARC));
        }

        RoundRectangle2D body = new RoundRectangle2D.Float(inset, y, width, height, ARC, ARC);
        Color from = pressed ? accent.darker() : (rollover ? accentHover : accent);
        Color to = pressed ? accent : accentHover.darker();
        g2.setPaint(new GradientPaint(inset, y, from, inset, y + height, to));
        g2.fill(body);

        // Внутренний свет сверху, чтобы кнопка выглядела объёмной.
        g2.setPaint(new GradientPaint(inset, y, new Color(255, 255, 255, 70), inset, y + height * 0.55f,
                new Color(255, 255, 255, 0)));
        g2.fill(body);

        paintShimmer(g2, inset, y, width, height);

        g2.setColor(new Color(255, 255, 255, 90));
        g2.setStroke(new BasicStroke(1.4f));
        g2.draw(new RoundRectangle2D.Float(inset + 1, y + 1, width - 2, height - 2, ARC, ARC));

        g2.dispose();
        super.paintComponent(g);
    }

    private void paintShimmer(Graphics2D g2, int x, int y, int width, int height) {
        double phase = (System.currentTimeMillis() % SHIMMER_PERIOD_MS) / (double) SHIMMER_PERIOD_MS;
        int bandWidth = Math.max(40, width / 4);
        int travel = width + bandWidth * 2;
        int bandX = x - bandWidth + (int) (phase * travel);

        Shape oldClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Float(x, y, width, height, ARC, ARC));
        g2.setPaint(new GradientPaint(bandX, y, new Color(255, 255, 255, 0),
                bandX + bandWidth, y + height, new Color(255, 255, 255, 55)));
        g2.fillRect(bandX, y, bandWidth, height);
        g2.setClip(oldClip);
    }
}
