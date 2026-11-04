package WindowApps;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.awt.image.BufferedImage;

public class Basic {

    public static final Color BACKGROUND_TOP = new Color(0x1E2433);
    public static final Color BACKGROUND_BOTTOM = new Color(0x0E1118);
    public static final Color ACCENT = new Color(0x4C8DFF);
    public static final Color ACCENT_HOVER = new Color(0x6FA4FF);
    public static final Color AFFORDABLE_COLOR = new Color(0x2E7D5B);
    public static final Color BUTTON_COLOR = new Color(0x252C3D);
    public static final Color BUTTON_HOVER = new Color(0x323B52);
    public static final Color TEXT_COLOR = new Color(0xEDF1F7);
    public static final Color PARTICLE_COLOR = new Color(0x7FB3FF);
    public static final String EXIT = "exit";

    protected static final int PARTICLE_LIFETIME_MS = 900;
    protected static final int PARTICLE_FRAME_MS = 16;
    protected static final int[] ICON_SIZES = {16, 24, 32, 48, 64, 128, 256};

    protected JFrame frame;
    protected JPanel panel;
    protected JLabel output;
    protected final Map<String, Runnable> actions = new HashMap<>();

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

        panel = new BackgroundPanel();
        panel.setLayout(new BorderLayout());
        frame.add(panel);
    }

    protected void addParticleLayer() {
        panel.add(new ParticleLayer(), 0);
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
        g2.setPaint(new GradientPaint(0, 0, ACCENT, 0, size, BACKGROUND_BOTTOM));
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
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(Math.max(1f, size * 0.06f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolyline(xs, ys, xs.length - 1);

        g2.dispose();
        return image;
    }

    protected void initActions() {
        actions.put(EXIT, () -> {
            if (confirm("Вы уверены, что хотите выйти?")) {
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
        JButton button = new RoundedButton(text);
        button.setForeground(TEXT_COLOR);
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
        return panel;
    }

    protected void handle(ActionEvent e) {
        handle(e.getActionCommand());
    }

    protected void handle(String command) {
        actions.getOrDefault(command, () -> output.setText("Не знаю такой команды: " + command)).run();
    }

    protected boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(frame, message, "Подтверждение",
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
        panel.repaint();
    }

    protected static class BackgroundPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        protected BackgroundPanel() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setPaint(new GradientPaint(0, 0, BACKGROUND_TOP, 0, getHeight(), BACKGROUND_BOTTOM));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
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
            for (Particle particle : particles) {
                float life = particle.lifeLeft(now);
                int size = Math.max(1, Math.round((float) (particle.size * life)));
                g2.setColor(new Color(PARTICLE_COLOR.getRed(), PARTICLE_COLOR.getGreen(),
                        PARTICLE_COLOR.getBlue(), Math.round(255 * life)));
                g2.fillOval(Math.round((float) particle.x) - size / 2, Math.round((float) particle.y) - size / 2, size, size);
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
        private final long created = System.currentTimeMillis();

        private Particle(double x, double y, double dx, double dy, double size) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.size = size;
        }

        private void move() {
            x += dx;
            y += dy;
            dy += GRAVITY;
            dx *= 0.98;
        }

        private float lifeLeft(long now) {
            return Math.max(0f, 1f - (now - created) / (float) PARTICLE_LIFETIME_MS);
        }

        private boolean isFinished(long now) {
            return lifeLeft(now) <= 0f;
        }
    }

    protected static class RoundedButton extends JButton {

        private static final long serialVersionUID = 1L;

        private static final int ARC = 18;

        private boolean highlighted;
        private int phase;
        private final Timer marquee = new Timer(40, e -> {
            phase = (phase + 1) % 24;
            repaint();
        });

        private RoundedButton(String text) {
            super(text);
            setOpaque(false);
            setRolloverEnabled(true);
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    marquee.start();
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    marquee.stop();
                    repaint();
                }
            });
        }

        protected void setHighlighted(boolean highlighted) {
            if (this.highlighted != highlighted) {
                this.highlighted = highlighted;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            if (getModel().isPressed()) {
                g2.setColor(ACCENT.darker());
            } else if (getModel().isRollover()) {
                g2.setColor(BUTTON_HOVER);
            } else if (highlighted) {
                g2.setColor(AFFORDABLE_COLOR);
            } else {
                g2.setColor(BUTTON_COLOR);
            }
            g2.fillRoundRect(0, 0, width, height, ARC, ARC);

            if (isFocusOwner()) {
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                        new float[]{6f, 6f}, phase));
                g2.drawRoundRect(3, 3, width - 7, height - 7, ARC, ARC);
            }

            if (getModel().isRollover() || highlighted) {
                g2.setColor(highlighted ? AFFORDABLE_COLOR.brighter() : ACCENT_HOVER);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, width - 3, height - 3, ARC, ARC);
            }
            g2.dispose();

            super.paintComponent(g);
        }
    }
}
