package WindowApps;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Basic {

    public static final String EXIT = "exit";

    protected static final int PARTICLE_LIFETIME_MS = 900;
    protected static final int PARTICLE_FRAME_MS = 16;
    protected static final int[] ICON_SIZES = {16, 24, 32, 48, 64, 128, 256};

    protected JFrame frame;
    protected JPanel panel;
    protected JLabel output;
    protected final Map<String, Runnable> actions = new HashMap<>();
    protected Theme activeTheme = Theme.STANDARD;

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private Timer particleTimer;

    protected String getTitle() {
        return "Мой тестовый каркас";
    }

    public final void start() {
        initActions();
        buildUI();
        frame.setVisible(true);
    }

    /**
     * Пересобирает окно с нуля — нужно после смены языка, т.к. тексты уже созданных компонентов не обновляются.
     */
    public void restart() {
        stopAnimation();
        if (panel instanceof BackgroundPanel background) {
            background.stopAnimation();
        }
        if (particleTimer != null) {
            particleTimer.stop();
            particleTimer = null;
        }
        particles.clear();
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        panel = null;
        output = null;
        start();
    }

    /** Останавливает анимации подкласса перед пересборкой окна. */
    public void stopAnimation() {
        // по умолчанию своих анимаций нет
    }

    protected void buildUI() {
        frame = new JFrame(getTitle());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handle(EXIT);
            }
        });
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
        frame.setIconImages(createAppIcons());

        panel = new BackgroundPanel(this);
        panel.setLayout(new BorderLayout());
        frame.add(panel);
    }

    /** Запускает анимацию фона; вызывается после того, как окно собрано. */
    protected void startBackgroundAnimation() {
        if (panel instanceof BackgroundPanel background) {
            background.startAnimation();
        }
    }

    protected void addParticleLayer() {
        if (panel == null) {
            throw new IllegalStateException("addParticleLayer() must be called after super.buildUI()");
        }
        panel.add(new ParticleLayer(), 0);
    }

    public Theme getActiveTheme() {
        return activeTheme;
    }

    public void applyTheme(Theme theme) {
        if (theme == null || theme == activeTheme) {
            return;
        }
        activeTheme = theme;
        restyle(panel);
        if (frame != null) {
            frame.setIconImages(createAppIcons()); // пересоздать иконки под новую тему
        }
        panel.repaint();
    }

    /** Применяет тему к текущему окну; подклассы могут дополнительно сохранить выбор. */
    public void setTheme(Theme theme) {
        applyTheme(theme);
    }

    private void restyle(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof RoundedButton button) {
                button.setForeground(activeTheme.getText());
                button.repaint();
            }
            if (component instanceof Container child) {
                restyle(child);
            }
            if (component instanceof JLabel label) {
                label.setForeground(activeTheme.getText());
            }
        }
    }

    protected List<Image> createAppIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : ICON_SIZES) {
            icons.add(paintAppIcon(size));
        }
        return icons;
    }

    protected Image paintAppIcon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        float inset = size * 0.06f;
        g2.setPaint(new GradientPaint(0, 0, activeTheme.getAccent(), 0, size, activeTheme.getBackgroundBottom()));
        g2.fillRoundRect(Math.round(inset), Math.round(inset),
                Math.round(size - 2 * inset), Math.round(size - 2 * inset),
                Math.round(size * 0.28f), Math.round(size * 0.28f));

        int arrow = Math.round(size * 0.5f);
        int head = Math.round(size * 0.30f);
        int wing = Math.round(size * 0.16f);
        int left = Math.round(size * 0.34f);
        int top = Math.round(size * 0.26f);
        int[] xs = {left, left + arrow, left + arrow, left + arrow + wing, left + arrow, left + arrow, left};
        int[] ys = {top, top, top + arrow - wing, top + arrow + head / 2, top + arrow + head / 2 + head,
                top + arrow + head / 2 + head, top + arrow - wing};
        g2.setColor(activeTheme.getText());
        g2.setStroke(new BasicStroke(Math.max(1f, size * 0.06f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolyline(xs, ys, xs.length - 1);

        g2.dispose();
        return image;
    }

    protected void initActions() {
        actions.put(EXIT, () -> {
            if (confirm(Text.get("exit.confirm"))) {
                close();
            }
        });
    }

    protected JButton makeButton(String text, String command) {
        return makeButton(null, text, command);
    }

    protected JButton makeButton(Container container, String text, String command) {
        JButton button = createButton(text, command);
        (container == null ? panel : container).add(button);
        return button;
    }

    public JButton createButton(String text) {
        return createButton(text, null);
    }

    public JButton createButton(String text, String command) {
        JButton button = new RoundedButton(this, text);
        button.setForeground(activeTheme.getText());
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        if (command != null) {
            button.setActionCommand(command);
            button.addActionListener(this::handle);
        }
        return button;
    }

    protected JComponent particleLayer() {
        for (Component component : panel.getComponents()) {
            if (component instanceof ParticleLayer layer) {
                return layer;
            }
        }
        return null;
    }

    protected void handle(ActionEvent e) {
        handle(e.getActionCommand());
    }

    protected void handle(String command) {
        actions.getOrDefault(command, () -> output.setText(Text.format("unknown.command", command))).run();
    }

    protected boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(frame, message, Text.get("dialog.confirm"),
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    protected void close() {
        frame.dispose();
    }

    protected void spawnParticles(Component source) {
        Component anchor = source == null ? panel : source;
        Point origin = SwingUtilities.convertPoint(anchor, anchor.getWidth() / 2, anchor.getHeight() / 2, panel);
        for (int i = 0; i < 14; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.5 + random.nextDouble() * 3.5;
            particles.add(new Particle(origin.x, origin.y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.5,
                    3 + random.nextDouble() * 4));
        }
        // Несколько искр-бликов: маленькие и быстро летящие звёздочки.
        for (int i = 0; i < 5; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 4 + random.nextDouble() * 5;
            particles.add(new Particle(origin.x, origin.y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed,
                    2 + random.nextDouble() * 2,
                    true));
        }
        if (particleTimer == null) {
            particleTimer = new Timer(PARTICLE_FRAME_MS, e -> tickParticles());
            particleTimer.start();
        }
    }

    private void tickParticles() {
        long now = System.currentTimeMillis();
        for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
            Particle particle = it.next();
            particle.move();
            if (particle.isFinished(now)) {
                it.remove();
            }
        }
        if (particles.isEmpty()) {
            particleTimer.stop();
            particleTimer = null;
        }
        JComponent layer = particleLayer();
        if (layer != null) {
            layer.repaint();
        }
    }

    protected static class BackgroundPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        /** Количество «орбов» — больших размытых пятен на фоне. */
        private static final int ORB_COUNT = 5;

        private final Basic owner;
        private final float[] orbX = new float[ORB_COUNT];
        private final float[] orbY = new float[ORB_COUNT];
        private final float[] orbRadius = new float[ORB_COUNT];
        private final float[] orbPhase = new float[ORB_COUNT];
        private final float[] orbSpeed = new float[ORB_COUNT];
        private final Random orbRandom = new Random();
        private Timer orbTimer;

        protected BackgroundPanel(Basic owner) {
            this.owner = owner;
            setOpaque(true);
            for (int i = 0; i < ORB_COUNT; i++) {
                orbX[i] = orbRandom.nextFloat();
                orbY[i] = orbRandom.nextFloat();
                orbRadius[i] = 0.18f + orbRandom.nextFloat() * 0.22f;
                orbPhase[i] = orbRandom.nextFloat() * (float) (Math.PI * 2);
                orbSpeed[i] = 0.0002f + orbRandom.nextFloat() * 0.0005f;
            }
        }

        /** Запускает медленное «дыхание» фона; вызывается один раз при показе окна. */
        void startAnimation() {
            if (orbTimer != null) {
                return;
            }
            orbTimer = new Timer(50, e -> repaint());
            orbTimer.start();
        }

        /** Останавливает анимацию — нужно перед пересборкой окна, иначе таймер утечёт. */
        void stopAnimation() {
            if (orbTimer != null) {
                orbTimer.stop();
                orbTimer = null;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Theme theme = owner.activeTheme;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            g2.setPaint(new GradientPaint(0, 0, theme.getBackgroundTop(), 0, height, theme.getBackgroundBottom()));
            g2.fillRect(0, 0, width, height);

            // Медленно дышащие пятна акцентного цвета — фон перестаёт быть плоским.
            long now = System.currentTimeMillis();
            Color accent = theme.getAccent();
            Color secondary = theme.getParticle();
            for (int i = 0; i < ORB_COUNT; i++) {
                float t = (float) (now * orbSpeed[i] + orbPhase[i]);
                float cx = (orbX[i] + (float) Math.sin(t) * 0.12f) * width;
                float cy = (orbY[i] + (float) Math.cos(t * 0.8f) * 0.10f) * height;
                float radius = orbRadius[i] * Math.max(width, height) * (0.85f + 0.15f * (float) Math.sin(t * 1.3f));
                Color base = i % 2 == 0 ? accent : secondary;
                int alpha = Math.round(16f + 10f * (float) Math.sin(t * 0.9f));
                g2.setPaint(new RadialGradientPaint(cx, cy, Math.max(1f, radius),
                        new float[]{0f, 1f},
                        new Color[]{withAlpha(base, Math.max(0, alpha)), withAlpha(base, 0)}));
                g2.fillOval(Math.round(cx - radius), Math.round(cy - radius),
                        Math.round(radius * 2), Math.round(radius * 2));
            }

            paintVignette(g2, width, height);
            g2.dispose();
            super.paintComponent(g);
        }

        /** Легкое затемнение по краям, чтобы центр композиции смотрелся выигрышнее. */
        private void paintVignette(Graphics2D g2, int width, int height) {
            float radius = Math.max(width, height) * 0.78f;
            g2.setPaint(new RadialGradientPaint(width / 2f, height / 2f, radius,
                    new float[]{0.45f, 1f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 90)}));
            g2.fillRect(0, 0, width, height);
        }

        private static Color withAlpha(Color color, int alpha) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
        }
    }

    protected class ParticleLayer extends JComponent {

        private static final long serialVersionUID = 1L;

        protected ParticleLayer() {
            setOpaque(false);
            setFocusable(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            long now = System.currentTimeMillis();
            Color color = activeTheme.getParticle();
            Color accent = activeTheme.getAccent();
            for (Particle particle : particles) {
                float life = particle.lifeLeft(now);
                float size = (float) (particle.size * life);
                int centerX = Math.round((float) particle.x);
                int centerY = Math.round((float) particle.y);
                int alpha = Math.round(255 * life);
                if (particle.spark) {
                    // Искра рисуется как четырёхлучевая звезда.
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
                    g2.setStroke(new BasicStroke(Math.max(1f, size * 0.5f)));
                    int ray = Math.max(2, Math.round(size * 2.2f));
                    g2.drawLine(centerX - ray, centerY, centerX + ray, centerY);
                    g2.drawLine(centerX, centerY - ray, centerX, centerY + ray);
                    continue;
                }
                int radius = Math.max(1, Math.round(size));
                // Светящееся ядро + прозрачная аура вокруг.
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(alpha * 0.35f)));
                g2.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                g2.fillOval(centerX - radius / 2, centerY - radius / 2, radius, radius);
            }
            g2.dispose();
        }
    }

    protected static class Particle {

        private static final double GRAVITY = 0.18;

        private double x;
        private double y;
        private double dx;
        private double dy;
        private final double size;
        private final boolean spark;
        private final long created = System.currentTimeMillis();

        private Particle(double x, double y, double dx, double dy, double size) {
            this(x, y, dx, dy, size, false);
        }

        private Particle(double x, double y, double dx, double dy, double size, boolean spark) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.size = size;
            this.spark = spark;
        }

        private void move() {
            x += dx;
            y += dy;
            dy += spark ? GRAVITY * 0.4 : GRAVITY;
            dx *= 0.98;
        }

        private float lifeLeft(long now) {
            return Math.max(0f, 1f - (now - created) / (float) PARTICLE_LIFETIME_MS);
        }

        private boolean isFinished(long now) {
            return lifeLeft(now) <= 0f;
        }
    }

    public static class RoundedButton extends JButton {

        private static final long serialVersionUID = 1L;

        private static final int ARC = 18;

        private final Basic owner;
        private boolean highlighted;

        private RoundedButton(Basic owner, String text) {
            super(text);
            this.owner = owner;
            setOpaque(false);
            setRolloverEnabled(true);
        }

        public void setHighlighted(boolean highlighted) {
            if (this.highlighted != highlighted) {
                this.highlighted = highlighted;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Theme theme = owner.activeTheme;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            if (getModel().isPressed()) {
                g2.setColor(theme.getAccent().darker());
            } else if (getModel().isRollover()) {
                g2.setColor(theme.getButtonHover());
            } else if (highlighted) {
                g2.setColor(theme.getAffordable());
            } else {
                g2.setColor(theme.getButton());
            }
            g2.fillRoundRect(0, 0, width, height, ARC, ARC);

            if (getModel().isRollover() || highlighted) {
                g2.setColor(highlighted ? theme.getAffordable().brighter() : theme.getAccentHover());
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, width - 3, height - 3, ARC, ARC);
            }
            g2.dispose();

            super.paintComponent(g);
        }
    }
}
