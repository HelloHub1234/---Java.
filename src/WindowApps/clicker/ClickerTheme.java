package WindowApps.clicker;

import WindowApps.Basic;
import WindowApps.Text;
import WindowApps.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Константы размеров, шрифтов, задержек и фабрики визуальных компонентов кликера. */
public final class ClickerTheme {

    public static final int BUTTON_WIDTH = 200;
    public static final int BUTTON_HEIGHT = 66;
    public static final float BUTTON_FONT_SIZE = 16f;
    public static final int BUTTON_GAP = 12;
    public static final Insets PANEL_PADDING = new Insets(30, 40, 30, 40);
    public static final int EXIT_GAP = 22;
    public static final int AUTOCLICKER_DELAY_MS = 1000;

    public static final Dimension BUTTON_SIZE = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
    public static final Dimension UPGRADE_SIZE = new Dimension(540, 96);
    public static final Dimension CLICK_BUTTON_SIZE = new Dimension(360, 130);

    public static final int CARD_ARC = 26;
    public static final int CARD_GLOW = 26;

    public static final Font BUTTON_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    public static final Font COUNT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 96);
    public static final Font UPGRADE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    public static final Font BALANCE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    public static final Font CLICK_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 28);
    public static final Font PER_PRESS_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    public static final Font CAPTION_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    private ClickerTheme() {
    }

    public static JButton createButton(Basic basic, String commandKey, String command) {
        JButton button = basic.createButton(Text.get(commandKey), command);
        button.setFont(BUTTON_FONT);
        button.setPreferredSize(BUTTON_SIZE);
        return button;
    }

    /** Кнопка без команды: текст задаётся сразу, обработчик вешает вызывающий код. */
    public static JButton createButton(Basic basic, String text) {
        JButton button = basic.createButton(text);
        button.setFont(BUTTON_FONT);
        button.setPreferredSize(BUTTON_SIZE);
        return button;
    }

    /**
     * Большая кнопка «Кликнуть» с собственным оформлением (градиент, блик, свечение).
     * Команда обязательна: без неё {@code handle} получает пустую строку и действие не находится.
     */
    public static ClickButton createClickButton(Basic basic, String text, String command) {
        ClickButton button = new ClickButton(basic, text);
        button.setFont(CLICK_FONT);
        button.setPreferredSize(CLICK_BUTTON_SIZE);
        button.setActionCommand(command);
        button.addActionListener(e -> basic.runCommand(command));
        return button;
    }

    /** Стеклянная карточка: полупрозрачная заливка, тонкая рамка и мягкая тень. */
    public static JPanel createCard(Theme theme, LayoutManager layout, int padding) {
        CardPanel card = new CardPanel(theme);
        card.setLayout(layout);
        card.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return card;
    }

    public static JComponent createCaption(String text, Theme theme) {
        return new CaptionLabel(text, theme);
    }

    /**
     * Заголовок карточки: разрядка букв, лёгкое свечение и акцентные штрихи по бокам.
     * Раньше это был обычный JLabel — на полупрозрачной карточке он читался как «сырой» текст.
     */
    private static final class CaptionLabel extends JComponent {

        private static final long serialVersionUID = 1L;

        /** Межбуквенное расстояние в пикселях: даёт «заголовочный» вид без подбора шрифта. */
        private static final float TRACKING = 3.2f;

        private final String text;
        private final Theme theme;

        CaptionLabel(String text, Theme theme) {
            this.text = text == null ? "" : text.toUpperCase();
            this.theme = theme;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(CAPTION_FONT);
            return new Dimension(metrics.stringWidth(text) + Math.round(TRACKING * text.length()) + 24,
                    metrics.getHeight() + 14);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (text.isEmpty()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setFont(CAPTION_FONT);
            FontMetrics metrics = g2.getFontMetrics();

            int glyphWidth = metrics.stringWidth(text);
            int total = glyphWidth + Math.round(TRACKING * (text.length() - 1));
            float startX = (getWidth() - total) / 2f;
            float baseline = (getHeight() - metrics.getHeight()) / 2f + metrics.getAscent() - 1;

            Color textColor = theme.getText();
            Color accent = theme.getAccent();

            // Свечение: одна размытая копия со смещением — читается и на тёмных, и на светлых темах.
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
            drawTracked(g2, text, startX + 0.8f, baseline + 0.8f);

            g2.setPaint(new GradientPaint(0, baseline - metrics.getAscent(), textColor,
                    0, baseline + metrics.getDescent(), blend(textColor, accent, 0.45f)));
            drawTracked(g2, text, startX, baseline);

            // Акцентные штрихи-«тире» по краям: завершают заголовок без лишних элементов.
            int dashLength = Math.max(10, Math.round(total * 0.10f));
            int dashY = Math.round(baseline - metrics.getAscent() / 2f);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 170));
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float gap = 10f;
            g2.drawLine(Math.round(startX - dashLength - gap), dashY, Math.round(startX - gap), dashY);
            g2.drawLine(Math.round(startX + total + gap), dashY,
                    Math.round(startX + total + gap + dashLength), dashY);

            g2.dispose();
        }

        /** Рисует строку посимвольно с разрядкой: drawString не умеет tracking. */
        private void drawTracked(Graphics2D g2, String value, float x, float y) {
            FontMetrics metrics = g2.getFontMetrics();
            float cursor = x;
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                g2.drawString(String.valueOf(ch), cursor, y);
                cursor += metrics.charWidth(ch) + TRACKING;
            }
        }

        private static Color blend(Color first, Color second, float weight) {
            float w = Math.max(0f, Math.min(1f, weight));
            return new Color(
                    Math.round(first.getRed() + (second.getRed() - first.getRed()) * w),
                    Math.round(first.getGreen() + (second.getGreen() - first.getGreen()) * w),
                    Math.round(first.getBlue() + (second.getBlue() - first.getBlue()) * w));
        }
    }
}
