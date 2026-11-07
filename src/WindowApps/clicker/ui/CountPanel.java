package WindowApps.clicker.ui;

import WindowApps.Basic;
import WindowApps.Text;
import WindowApps.clicker.ui.component.ClickButton;
import WindowApps.clicker.ui.theme.ClickerTheme;
import WindowApps.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Центральная колонка кликера: суммарные клики, прибавка за нажатие,
 * большая кнопка «Кликнуть» и подпись с уровнем автокликера.
 */
public class CountPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** ~60 кадров в секунду и мягкое затухание вместо резкого скачка. */
    private static final int PULSE_FRAME_MS = 16;
    private static final int PULSE_DURATION_MS = 320;

    private final Basic basic;
    private final CountLabel countLabel = new CountLabel();
    private final JLabel perPressLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel hintLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton clickButton;
    private final Timer pulseTimer;

    public CountPanel(Basic basic, String clickButtonText, String clickCommand) {
        this.basic = basic;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        perPressLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        perPressLabel.setFont(ClickerTheme.PER_PRESS_FONT);
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        clickButton = ClickerTheme.createClickButton(basic, clickButtonText, clickCommand);
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonRow.add(clickButton);

        add(Box.createVerticalGlue());
        add(countLabel);
        add(Box.createVerticalStrut(6));
        add(perPressLabel);
        add(Box.createVerticalStrut(26));
        add(buttonRow);
        add(Box.createVerticalStrut(22));
        add(hintLabel);
        add(Box.createVerticalGlue());

        pulseTimer = new Timer(PULSE_FRAME_MS, e -> countLabel.advancePulse());
        pulseTimer.setRepeats(true);
        updateTheme();
    }

    public JButton getClickButton() {
        return clickButton;
    }

    /** Останавливает анимации панели (окно пересобирается после смены языка). */
    public void stopAnimation() {
        pulseTimer.stop();
        if (clickButton instanceof ClickButton click) {
            click.stopAnimation();
        }
    }

    public void showCount(int clicks) {
        countLabel.setText(String.valueOf(clicks));
        if (!pulseTimer.isRunning()) {
            countLabel.beginPulse();
            pulseTimer.start();
        } else {
            countLabel.beginPulse();
        }
    }

    public void showPerPress(int perPress) {
        perPressLabel.setText(Text.format("count.perPress", perPress));
    }

    public void showHint(String text) {
        hintLabel.setText(text);
    }

    public void repaintTexts() {
        perPressLabel.repaint();
        hintLabel.repaint();
        countLabel.repaint();
    }

    /** Переприменяет цвета темы к подписям, которые не умеют смотреть в тему сами. */
    public void updateTheme() {
        perPressLabel.setForeground(theme().getText());
        Color hint = theme().getText();
        hintLabel.setForeground(new Color(hint.getRed(), hint.getGreen(), hint.getBlue(), 160));
        repaint();
    }

    private Theme theme() {
        return basic.getActiveTheme();
    }

    /** Текст счётчика с мягким свечением акцентного цвета и короткой анимацией «толчка». */
    private final class CountLabel extends JComponent {

        private static final long serialVersionUID = 1L;

        private String text = "";
        private boolean pulsing;
        private long pulseStart;

        CountLabel() {
            setFont(ClickerTheme.COUNT_FONT);
        }

        void setText(String value) {
            this.text = value;
            revalidate();
            repaint();
        }

        void beginPulse() {
            pulsing = true;
            pulseStart = System.currentTimeMillis();
        }

        void advancePulse() {
            if (!pulsing || System.currentTimeMillis() - pulseStart >= PULSE_DURATION_MS) {
                pulsing = false;
                pulseTimer.stop();
            }
            repaint();
        }

        /**
         * Мягкий толчок: подъём по easeOutCubic и плавное возвращение.
         * Держим пик коротким, чтобы цифры не «прыгали» при быстрых кликах.
         */
        private float scale() {
            if (!pulsing) {
                return 1f;
            }
            long elapsed = System.currentTimeMillis() - pulseStart;
            float progress = Math.min(1f, elapsed / (float) PULSE_DURATION_MS);
            float wave = (float) Math.sin(Math.PI * progress);
            return 1f + 0.10f * wave * (1f - 0.35f * progress);
        }

        @Override
        public Dimension getPreferredSize() {
            Font font = getFont();
            FontMetrics metrics = font == null ? null : getFontMetrics(font);
            if (metrics == null) {
                return new Dimension(240, 140);
            }
            return new Dimension(metrics.stringWidth(text + "0000") + 90, metrics.getHeight() + 46);
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

            Theme theme = basic.getActiveTheme();
            float scale = scale();
            g2.setFont(getFont().deriveFont(getFont().getSize2D() * scale));
            FontMetrics metrics = g2.getFontMetrics();

            int x = (getWidth() - metrics.stringWidth(text)) / 2;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();

            // Свечение: несколько смещённых полупрозрачных копий текста.
            Color glow = theme.getAccent();
            for (int i = 7; i >= 1; i--) {
                g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), Math.min(80, 9 * i)));
                g2.drawString(text, x - i * 0.6f, y - i * 0.6f);
                g2.drawString(text, x + i * 0.6f, y + i * 0.6f);
            }

            // Основной текст с вертикальным градиентом.
            g2.setPaint(new GradientPaint(0, y - metrics.getAscent(), theme.getText(),
                    0, y + metrics.getDescent(), theme.getAccentHover()));
            g2.drawString(text, x, y);

            // Акцентная подчёркивающая линия.
            g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 120));
            g2.fill(new RoundRectangle2D.Float(x, y + metrics.getDescent() + 8,
                    metrics.stringWidth(text), 4, 4, 4));

            g2.dispose();
        }
    }
}
