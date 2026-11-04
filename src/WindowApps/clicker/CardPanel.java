package WindowApps.clicker;

import WindowApps.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Полупрозрачная «карточка» на фоне окна: скруглённый прямоугольник с тонкой рамкой,
 * мягким свечением сверху и тенью снизу. Не перехватывает события мыши.
 */
public class CardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final float FILL_ALPHA = 0.42f;
    private static final float BORDER_ALPHA = 0.5f;

    private final Theme theme;

    public CardPanel(Theme theme) {
        this.theme = theme;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int arc = ClickerTheme.CARD_ARC;
        int margin = ClickerTheme.CARD_GLOW;
        int width = getWidth() - margin * 2;
        int height = getHeight() - margin * 2;
        if (width <= 0 || height <= 0) {
            g2.dispose();
            return;
        }

        RoundRectangle2D shape = new RoundRectangle2D.Float(margin, margin, width, height, arc, arc);

        // Тень: несколько всё более размытых полупрозрачных контуров вокруг карточки.
        for (int i = 6; i >= 1; i--) {
            Color accent = theme.getAccent();
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), Math.round(4f * (7 - i))));
            g2.setStroke(new BasicStroke(i * 2f));
            g2.draw(new RoundRectangle2D.Float(margin - i, margin + i * 0.6f, width + i * 2f, height + i * 2f, arc, arc));
        }

        // Заливка: полупрозрачный градиент от акцента к подложке.
        Color top = theme.getButton();
        Color bottom = theme.getBackgroundBottom();
        g2.setPaint(new GradientPaint(0, margin, withAlpha(top, 92), 0, margin + height, withAlpha(bottom, 120)));
        g2.fill(shape);

        // Светлая кромка сверху и тонкая рамка.
        Color text = theme.getText();
        g2.setColor(new Color(text.getRed(), text.getGreen(), text.getBlue(), Math.round(255 * 0.07f)));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(margin + 1, margin + 1, width - 2, height - 2, arc, arc));

        g2.setColor(withAlpha(theme.getAccent(), Math.round(255 * BORDER_ALPHA / 2f)));
        g2.setStroke(new BasicStroke(1.4f));
        g2.draw(shape);

        g2.dispose();
        super.paintComponent(g);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
