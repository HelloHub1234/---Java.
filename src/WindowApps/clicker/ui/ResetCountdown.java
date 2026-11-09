package WindowApps.clicker.ui;

import WindowApps.Text;
import WindowApps.Theme;
import WindowApps.clicker.ui.theme.ClickerTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Обратный отсчёт перед полным сбросом прогресса: 3 → 2 → 1, затем действие выполняется.
 * Немодальное окно поверх игры с кнопкой «Отмена» — случайное нажатие «Сброс» можно остановить
 * в последний момент, а не подтверждать сброс ещё раз в диалоге.
 */
public final class ResetCountdown {

    private static final int STEP_MS = 800;
    private static final int START_VALUE = 3;
    private static final Dimension SIZE = new Dimension(260, 200);

    private ResetCountdown() {
    }

    /**
     * Показывает отсчёт и вызывает {@code onFinish} после последнего шага.
     * Если игрок нажал «Отмена» или закрыл окно, действие не выполняется.
     */
    public static void show(JFrame owner, Theme theme, Runnable onFinish) {
        JWindow window = new JWindow(owner);
        CountdownPanel panel = new CountdownPanel(theme);
        window.setContentPane(panel);
        window.setSize(SIZE);
        window.setAlwaysOnTop(true);

        JButton cancel = new JButton(Text.get("reset.cancel"));
        cancel.setFont(ClickerTheme.BUTTON_FONT);
        cancel.setFocusPainted(false);
        cancel.addActionListener(e -> {
            panel.stop();
            window.dispose();
        });
        panel.add(cancel, BorderLayout.SOUTH);

        position(window, owner);
        window.setVisible(true);
        panel.start(() -> {
            window.dispose();
            onFinish.run();
        });
    }

    private static void position(JWindow window, JFrame owner) {
        Rectangle area = owner != null && owner.isShowing()
                ? owner.getBounds()
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        window.setLocation(area.x + (area.width - window.getWidth()) / 2,
                area.y + (area.height - window.getHeight()) / 2);
    }

    /** Круг с цифрой, которая сжимается и гаснет на каждом шаге. */
    private static final class CountdownPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private final Theme theme;
        private final Timer timer;
        private int value = START_VALUE;
        private long stepStart = System.currentTimeMillis();
        private Runnable onFinish;

        CountdownPanel(Theme theme) {
            this.theme = theme;
            setLayout(new BorderLayout());
            setOpaque(true);
            setBackground(theme.getBackgroundTop());
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(theme.getAccent(), 2, true),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            timer = new Timer(16, e -> tick());
        }

        void start(Runnable onFinish) {
            this.onFinish = onFinish;
            stepStart = System.currentTimeMillis();
            timer.start();
        }

        void stop() {
            timer.stop();
        }

        private void tick() {
            long elapsed = System.currentTimeMillis() - stepStart;
            if (elapsed < STEP_MS) {
                repaint();
                return;
            }
            value--;
            stepStart = System.currentTimeMillis();
            if (value > 0) {
                repaint();
                return;
            }
            timer.stop();
            if (onFinish != null) {
                onFinish.run();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            Color accent = theme.getAccent();
            Color text = theme.getText();

            // Подпись сверху: что именно сейчас произойдёт.
            g2.setFont(ClickerTheme.UPGRADE_FONT);
            g2.setColor(new Color(text.getRed(), text.getGreen(), text.getBlue(), 190));
            String caption = Text.get("reset.countdown");
            FontMetrics captionMetrics = g2.getFontMetrics();
            g2.drawString(caption, (width - captionMetrics.stringWidth(caption)) / 2, 26);

            // Кольцо сжимается к концу шага — видно, сколько осталось до следующей цифры.
            float progress = Math.min(1f, (System.currentTimeMillis() - stepStart) / (float) STEP_MS);
            int diameter = 96;
            int cx = width / 2;
            int cy = height / 2 - 6;
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new Ellipse2D.Float(cx - diameter / 2f, cy - diameter / 2f, diameter, diameter));
            g2.setColor(accent);
            g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new java.awt.geom.Arc2D.Float(cx - diameter / 2f, cy - diameter / 2f, diameter, diameter,
                    90, -360 * (1 - progress), java.awt.geom.Arc2D.OPEN));

            // Сама цифра: к концу шага чуть уменьшается и гаснет.
            float scale = 1f - 0.18f * progress;
            Font font = ClickerTheme.COUNT_FONT.deriveFont(ClickerTheme.COUNT_FONT.getSize2D() * 0.62f * scale);
            g2.setFont(font);
            FontMetrics metrics = g2.getFontMetrics();
            String number = String.valueOf(Math.max(1, value));
            int x = cx - metrics.stringWidth(number) / 2;
            int y = cy + (metrics.getAscent() - metrics.getDescent()) / 2;
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    Math.round(255 * (1f - 0.5f * progress))));
            g2.drawString(number, x, y);

            g2.dispose();
        }
    }
}
