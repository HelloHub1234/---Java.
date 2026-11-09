package WindowApps;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
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
    /** Шрифт всплывающих цифр «+N»: крупный и жирный, чтобы читался на лету. */
    private static final Font NUMBER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 26);

    protected JFrame frame;
    protected JPanel panel;
    protected JLabel output;
    protected final Map<String, Runnable> actions = new HashMap<>();
    protected Theme activeTheme = Theme.STANDARD;

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private Timer particleTimer;

    /** Эффекты поверх всего окна: фейерверк достижения и волна сброса. */
    private final List<Effect> effects = new ArrayList<>();
    private Timer effectTimer;

    /** Всплывающие цифры «+N» у курсора: живут отдельно от частиц, чтобы не мешать их лимиту. */
    private final List<FloatingNumber> numbers = new ArrayList<>();
    private Timer numberTimer;

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
        if (effectTimer != null) {
            effectTimer.stop();
            effectTimer = null;
        }
        effects.clear();
        if (numberTimer != null) {
            numberTimer.stop();
            numberTimer = null;
        }
        numbers.clear();
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
        // Отдельный компонент-слой больше не нужен: частицы рисуются в BackgroundPanel.paintChildren,
        // т.е. всегда поверх содержимого и без участия в BorderLayout (раньше он ломал раскладку).
        if (panel == null) {
            throw new IllegalStateException("addParticleLayer() must be called after super.buildUI()");
        }
    }

    /** Пересортировывает компоненты так, чтобы слой частиц всегда рисовался последним. */
    private void keepParticleLayerOnTop() {
        // Слой рисуется отдельно поверх содержимого панели, пересортировка z-order не нужна.
    }

    public Theme getActiveTheme() {
        return activeTheme;
    }

    public void applyTheme(Theme theme) {
        if (theme == null) {
            return;
        }
        // Даже если тема та же — перерисовать всё: цвета мог подхватить только что собранный UI.
        activeTheme = theme;
        restyle(panel);
        keepParticleLayerOnTop();
        if (frame != null) {
            frame.setIconImages(createAppIcons()); // пересоздать иконки под новую тему
        }
        if (panel != null) {
            // repaint() панели уже перерисовывает всё её дерево; отдельный обход repaintDeep
            // дублировал каждый компонент и удваивал работу при смене темы.
            panel.repaint();
        }
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

    /**
     * Иконка окна: тот же рисунок, что и у .exe (см. {@link AppIconPainter}), но с прозрачным
     * скруглённым «воздухом», чтобы в панели задач Windows она смотрелась как обычная иконка.
     */
    protected Image paintAppIcon(int size) {
        return AppIconPainter.renderPadded(size, activeTheme);
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

    /** Выполняет команду из {@link #actions}; используется кнопками с собственной отрисовкой. */
    public void runCommand(String command) {
        handle(command);
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
        return null;
    }

    private ParticleLayer findParticleLayer() {
        // Слой частиц больше не является отдельным компонентом.
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
        // Останавливаем все анимации и освобождаем окно: иначе таймеры Swing держат EDT
        // и процесс остаётся висеть после закрытия (см. жалобу «игра не закрывается полностью»).
        if (frame != null) {
            frame.dispose();
        }
        System.exit(0);
    }

    /**
     * Частицы в точке клика. Точка задаётся в координатах панели: кнопка может быть в любом месте окна,
     * и эффект должен рождаться там, куда пришёлся курсор, а не в центре кнопки.
     */
    protected void spawnParticles(Point origin) {
        Point point = origin == null ? new Point(panel.getWidth() / 2, panel.getHeight() / 2) : origin;
        for (int i = 0; i < 14; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1.5 + random.nextDouble() * 3.5;
            particles.add(new Particle(point.x, point.y,
                    Math.cos(angle) * speed,
                    Math.sin(angle) * speed - 1.5,
                    3 + random.nextDouble() * 4));
        }
        // Несколько искр-бликов: маленькие и быстро летящие звёздочки.
        for (int i = 0; i < 5; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 4 + random.nextDouble() * 5;
            particles.add(new Particle(point.x, point.y,
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

    /**
     * Всплывающая цифра «+N» в точке клика: поднимается вверх и тает.
     * Слегка смещается вбок, чтобы быстрые клики не складывали цифры в одну стопку.
     */
    protected void spawnFloatingNumber(Point origin, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Point point = origin == null ? new Point(panel.getWidth() / 2, panel.getHeight() / 2) : origin;
        double drift = (random.nextDouble() - 0.5) * 26;
        numbers.add(new FloatingNumber(point.x + drift, point.y, text));
        if (numberTimer == null) {
            numberTimer = new Timer(PARTICLE_FRAME_MS, e -> tickNumbers());
            numberTimer.start();
        }
    }

    private void tickNumbers() {
        long now = System.currentTimeMillis();
        for (Iterator<FloatingNumber> it = numbers.iterator(); it.hasNext(); ) {
            if (it.next().isFinished(now)) {
                it.remove();
            }
        }
        if (numbers.isEmpty()) {
            numberTimer.stop();
            numberTimer = null;
        }
        repaintOverlay();
    }

    /**
     * Фейерверк по всему окну: несколько залпов из разных точек, каждый — разлёт искр
     * с гравитацией и затуханием. Используется как награда за достижение.
     */
    protected void spawnFireworks() {
        int width = Math.max(1, panel.getWidth());
        int height = Math.max(1, panel.getHeight());
        int bursts = 3;
        for (int burst = 0; burst < bursts; burst++) {
            // Залпы идут по горизонтали с разбросом, чтобы не бить в одну точку.
            double x = width * (0.25 + 0.25 * burst) + (random.nextDouble() - 0.5) * width * 0.12;
            double y = height * (0.22 + random.nextDouble() * 0.22);
            effects.add(new Firework(x, y, burst * 120L));
        }
        startEffectTimer();
    }

    /**
     * Волна сброса: от центра расходится кольцо, а из него вылетают искры внутрь экрана.
     * Читается как «всё обнулилось» — без праздничности фейерверка.
     */
    protected void spawnResetWave() {
        effects.add(new ResetWave(panel.getWidth() / 2.0, panel.getHeight() / 2.0));
        startEffectTimer();
    }

    /**
     * Пульс рамки: по периметру окна пробегает короткая вспышка обводки.
     * Ненавязчивая награда, которая видна даже если игрок смотрит не в центр экрана.
     */
    protected void spawnFramePulse() {
        effects.add(new FramePulse());
        startEffectTimer();
    }

    private void startEffectTimer() {
        if (effectTimer == null) {
            effectTimer = new Timer(PARTICLE_FRAME_MS, e -> tickEffects());
            effectTimer.start();
        }
    }

    private void tickEffects() {
        long now = System.currentTimeMillis();
        for (Iterator<Effect> it = effects.iterator(); it.hasNext(); ) {
            if (it.next().isFinished(now)) {
                it.remove();
            }
        }
        if (effects.isEmpty()) {
            effectTimer.stop();
            effectTimer = null;
        }
        repaintOverlay();
    }

    /**
     * Перерисовка только слоя эффектов и частиц, а не всей панели.
     * Полный repaint() заставлял заново рисовать фон, карточки и кнопки 60 раз в секунду,
     * хотя меняются лишь искры поверх окна.
     */
    private void repaintOverlay() {
        if (panel instanceof BackgroundPanel background) {
            background.repaintOverlay();
        } else if (panel != null) {
            panel.repaint();
        }
    }

    /** Частицы в центре компонента — для вызовов, где конкретная точка неизвестна. */
    protected void spawnParticles(Component source) {
        Component anchor = source == null ? panel : source;
        spawnParticles(SwingUtilities.convertPoint(anchor, anchor.getWidth() / 2, anchor.getHeight() / 2, panel));
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
        repaintOverlay();
    }

    protected static class BackgroundPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        /** Количество «орбов» — больших размытых пятен на фоне. */
        private static final int ORB_COUNT = 5;
        /** Фон «дышит» медленно: 20 кадров в секунду достаточно, а нагрузка вдвое ниже. */
        private static final int ORB_FRAME_MS = 50;

        private final Basic owner;
        private final float[] orbX = new float[ORB_COUNT];
        private final float[] orbY = new float[ORB_COUNT];
        private final float[] orbRadius = new float[ORB_COUNT];
        private final float[] orbPhase = new float[ORB_COUNT];
        private final float[] orbSpeed = new float[ORB_COUNT];
        private final Random orbRandom = new Random();
        private Timer orbTimer;
        /** Кэш фона: градиенты и пятна пересчитываются только при смене размера или темы. */
        private BufferedImage backgroundCache;
        private int cachedWidth;
        private int cachedHeight;
        private Theme cachedTheme;

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
            orbTimer = new Timer(ORB_FRAME_MS, e -> repaint());
            orbTimer.start();
        }

        /** Останавливает анимацию фона и слоя частиц — нужно перед пересборкой окна. */
        void stopAnimation() {
            if (orbTimer != null) {
                orbTimer.stop();
                orbTimer = null;
            }
            backgroundCache = null;
        }

        /**
         * Слой частиц рисуется здесь, уже после всех детей панели,
         * поэтому не занимает места в BorderLayout и не может сломать раскладку.
         */
        @Override
        public void paintChildren(Graphics g) {
            super.paintChildren(g);
            if (owner.particles.isEmpty() && owner.effects.isEmpty() && owner.numbers.isEmpty()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                owner.paintParticles(g2);
                owner.paintNumbers(g2);
                owner.paintEffects(g2);
            } finally {
                g2.dispose();
            }
        }

        /**
         * Перерисовка только области эффектов: фон и содержимое остаются как есть,
         * поверх них заново рисуются искры. Так кадр анимации не тянет за собой
         * перерисовку всего окна.
         */
        void repaintOverlay() {
            if (owner.particles.isEmpty() && owner.effects.isEmpty() && owner.numbers.isEmpty()) {
                return;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Theme theme = owner.activeTheme;
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }

            // Градиент, пятна и виньетка не меняются от кадра к кадру — рисуем их один раз
            // в буфер и дальше только копируем. Раньше каждый кадр заново считал 5 радиальных
            // градиентов на весь экран, что и было главной нагрузкой на отрисовку.
            if (backgroundCache == null || cachedWidth != width || cachedHeight != height
                    || cachedTheme != theme) {
                backgroundCache = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D cache = backgroundCache.createGraphics();
                try {
                    paintBackground(cache, theme, width, height);
                } finally {
                    cache.dispose();
                }
                cachedWidth = width;
                cachedHeight = height;
                cachedTheme = theme;
            }
            g.drawImage(backgroundCache, 0, 0, null);
            // super.paintComponent здесь НЕ вызывается: JPanel залил бы свой ColorUIResource-фон
            // поверх градиента, и тема перестала бы быть видна вообще.
        }

        private void paintBackground(Graphics2D g2, Theme theme, int width, int height) {
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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
            // Слой не должен быть «главным» компонентом: он либо лежит поверх (размер = размер панели),
            // либо ничего не рисует и не участвует в раскладке.
            setAlignmentX(0f);
            setAlignmentY(0f);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(0, 0);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, 0);
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

    /** Рисует частицы в координатах панели; вызывается из {@link BackgroundPanel#paintChildren}. */
    void paintParticles(Graphics2D g2) {
        Graphics2D copy = (Graphics2D) g2.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
                copy.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
                copy.setStroke(new BasicStroke(Math.max(1f, size * 0.5f)));
                int ray = Math.max(2, Math.round(size * 2.2f));
                copy.drawLine(centerX - ray, centerY, centerX + ray, centerY);
                copy.drawLine(centerX, centerY - ray, centerX, centerY + ray);
                continue;
            }
            int radius = Math.max(1, Math.round(size));
            copy.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(alpha * 0.35f)));
            copy.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            copy.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            copy.fillOval(centerX - radius / 2, centerY - radius / 2, radius, radius);
        }
        copy.dispose();
    }

    /**
     * Рисует всплывающие цифры «+N». Шрифт берётся из темы, поэтому цифра не выбивается
     * из оформления; тёмная подложка-обводка держит читаемость на светлых темах.
     */
    void paintNumbers(Graphics2D g2) {
        if (numbers.isEmpty()) {
            return;
        }
        Graphics2D copy = (Graphics2D) g2.create();
        copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        copy.setFont(NUMBER_FONT);
        long now = System.currentTimeMillis();
        Color accent = activeTheme.getAccent();
        Color text = activeTheme.getText();
        for (FloatingNumber number : numbers) {
            float alpha = number.alpha(now);
            if (alpha <= 0f) {
                continue;
            }
            int x = (int) Math.round(number.x);
            int y = (int) Math.round(number.y - number.rise(now));
            FontMetrics metrics = copy.getFontMetrics();
            int drawX = x - metrics.stringWidth(number.text) / 2;
            // Тень-обводка: два смещённых слоя вместо дорогого TextLayout с контуром.
            copy.setColor(new Color(0, 0, 0, Math.round(110 * alpha)));
            copy.drawString(number.text, drawX + 1, y + 2);
            copy.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    Math.round(255 * alpha)));
            copy.drawString(number.text, drawX, y);
            // Светлая кромка сверху — цифра выглядит объёмной.
            copy.setColor(new Color(text.getRed(), text.getGreen(), text.getBlue(),
                    Math.round(90 * alpha)));
            copy.drawString(number.text, drawX, y - 1);
        }
        copy.dispose();
    }

    /** Рисует эффекты (фейерверк, волна сброса) в координатах панели. */
    void paintEffects(Graphics2D g2) {
        if (effects.isEmpty()) {
            return;
        }
        Graphics2D copy = (Graphics2D) g2.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        long now = System.currentTimeMillis();
        for (Effect effect : effects) {
            effect.paint(copy, now, activeTheme);
        }
        copy.dispose();
    }

    /** Длительный эффект поверх окна: сам знает, когда закончился, и рисует себя. */
    private abstract static class Effect {

        abstract void paint(Graphics2D g2, long now, Theme theme);

        abstract boolean isFinished(long now);
    }

    /**
     * Залп фейерверка: искры разлетаются из точки, тормозятся и падают под гравитацией.
     * Цвета берутся из темы, поэтому эффект не выбивается из оформления.
     */
    private static final class Firework extends Effect {

        private static final int SPARKS = 46;
        private static final int LIFETIME_MS = 1500;
        private static final double GRAVITY = 0.11;

        private final double[] x = new double[SPARKS];
        private final double[] y = new double[SPARKS];
        private final double[] dx = new double[SPARKS];
        private final double[] dy = new double[SPARKS];
        private final long start;

        Firework(double originX, double originY, long delayMs) {
            Random random = new Random();
            for (int i = 0; i < SPARKS; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                // Разная скорость у искр: залп получается «рваным», а не идеальным кругом.
                double speed = 2.2 + random.nextDouble() * 4.6;
                x[i] = originX;
                y[i] = originY;
                dx[i] = Math.cos(angle) * speed;
                dy[i] = Math.sin(angle) * speed - 0.8;
            }
            start = System.currentTimeMillis() + delayMs;
        }

        @Override
        void paint(Graphics2D g2, long now, Theme theme) {
            long elapsed = now - start;
            if (elapsed < 0) {
                return;
            }
            float life = 1f - elapsed / (float) LIFETIME_MS;
            if (life <= 0f) {
                return;
            }
            // Позиции считаются от времени, а не накапливаются: кадры не зависят от FPS.
            double seconds = elapsed / 1000.0;
            Color accent = theme.getAccent();
            Color particle = theme.getParticle();
            int alpha = Math.round(255 * Math.min(1f, life * 1.6f));
            for (int i = 0; i < SPARKS; i++) {
                double px = x[i] + dx[i] * seconds * 60;
                double py = y[i] + dy[i] * seconds * 60 + 0.5 * GRAVITY * Math.pow(seconds * 60, 2);
                Color color = i % 3 == 0 ? particle : accent;
                int radius = i % 4 == 0 ? 3 : 2;
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(alpha * 0.30f)));
                g2.fillOval((int) Math.round(px) - radius * 2, (int) Math.round(py) - radius * 2,
                        radius * 4, radius * 4);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                g2.fillOval((int) Math.round(px) - radius, (int) Math.round(py) - radius,
                        radius * 2, radius * 2);
            }
        }

        @Override
        boolean isFinished(long now) {
            return now - start > LIFETIME_MS;
        }
    }

    /**
     * Волна сброса: расходящееся кольцо и гаснущая вспышка в центре.
     * Кольцо расширяется и истончается — визуально «прогресс ушёл в ноль».
     */
    private static final class ResetWave extends Effect {

        private static final int LIFETIME_MS = 900;

        private final double centerX;
        private final double centerY;
        private final long start = System.currentTimeMillis();

        ResetWave(double centerX, double centerY) {
            this.centerX = centerX;
            this.centerY = centerY;
        }

        @Override
        void paint(Graphics2D g2, long now, Theme theme) {
            float progress = Math.min(1f, (now - start) / (float) LIFETIME_MS);
            float fade = 1f - progress;
            double maxRadius = Math.hypot(centerX, centerY) * 1.05;
            double radius = maxRadius * (1 - Math.pow(1 - progress, 3));
            Color accent = theme.getAccent();

            // Вспышка в центре: быстро гаснет, чтобы не засвечивать счётчик.
            int flashAlpha = Math.round(70 * fade * fade);
            if (flashAlpha > 0) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), flashAlpha));
                g2.fillOval((int) (centerX - radius * 0.35), (int) (centerY - radius * 0.35),
                        (int) (radius * 0.7), (int) (radius * 0.7));
            }

            // Кольцо: толщина падает вместе с прозрачностью.
            float thickness = Math.max(1f, 10f * fade);
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    Math.round(200 * fade)));
            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int r = (int) Math.round(radius);
            g2.drawOval((int) Math.round(centerX) - r, (int) Math.round(centerY) - r, r * 2, r * 2);
        }

        @Override
        boolean isFinished(long now) {
            return now - start > LIFETIME_MS;
        }
    }

    /**
     * Пульс рамки окна: несколько контуров по периметру панели, которые расходятся
     * от края внутрь и гаснут. Двойной «удар» (две волны с задержкой) читается как сердцебиение.
     */
    private static final class FramePulse extends Effect {

        private static final int LIFETIME_MS = 1100;
        private static final int WAVES = 2;
        private static final long WAVE_DELAY_MS = 220;

        private final long start = System.currentTimeMillis();

        @Override
        void paint(Graphics2D g2, long now, Theme theme) {
            Color accent = theme.getAccent();
            for (int wave = 0; wave < WAVES; wave++) {
                long elapsed = now - start - wave * WAVE_DELAY_MS;
                if (elapsed < 0) {
                    continue;
                }
                float progress = Math.min(1f, elapsed / (float) LIFETIME_MS);
                float fade = 1f - progress;
                if (fade <= 0f) {
                    continue;
                }
                // Контур уходит внутрь на 26 px и истончается: рамка «дышит», а не мигает.
                int inset = Math.round(progress * 26);
                float thickness = Math.max(1f, 7f * fade);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                        Math.round(190 * fade * fade)));
                g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Rectangle bounds = g2.getClipBounds();
                int width = bounds == null ? 0 : bounds.width;
                int height = bounds == null ? 0 : bounds.height;
                g2.drawRoundRect(inset, inset, Math.max(1, width - inset * 2 - 1),
                        Math.max(1, height - inset * 2 - 1), 28, 28);
            }
        }

        @Override
        boolean isFinished(long now) {
            return now - start > LIFETIME_MS + WAVE_DELAY_MS * (WAVES - 1);
        }
    }

    /** Всплывающая цифра «+N»: поднимается вверх с замедлением и тает. */
    private static final class FloatingNumber {

        private static final int LIFETIME_MS = 850;
        private static final double RISE_PX = 62;

        private final double x;
        private final double y;
        private final String text;
        private final long created = System.currentTimeMillis();

        FloatingNumber(double x, double y, String text) {
            this.x = x;
            this.y = y;
            this.text = text;
        }

        /** Текущее смещение вверх: easeOutCubic — быстро отрывается, потом плавно тормозит. */
        private double rise(long now) {
            double progress = Math.min(1.0, (now - created) / (double) LIFETIME_MS);
            return RISE_PX * (1 - Math.pow(1 - progress, 3));
        }

        private float alpha(long now) {
            float progress = Math.min(1f, (now - created) / (float) LIFETIME_MS);
            // Держим цифру полностью видимой первую половину жизни, потом гасим.
            return progress < 0.5f ? 1f : 1f - (progress - 0.5f) * 2f;
        }

        private boolean isFinished(long now) {
            return now - created > LIFETIME_MS;
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

        private static final int ARC = 20;

        private final Basic owner;
        private boolean highlighted;
        private String lastText;

        private RoundedButton(Basic owner, String text) {
            super(text);
            this.owner = owner;
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Звук нажатия — одно место для всех кнопок приложения: подписка на модель,
            // поэтому и кнопки магазина со своим ActionListener получают звук автоматически.
            addActionListener(e -> ClickSound.play());
            addPropertyChangeListener("text", event -> {
                // Кнопка магазина пересобирает HTML-текст регулярно: без этой защиты
                // каждое обновление приводило бы к лишнему пересчёту раскладки.
                String next = (String) event.getNewValue();
                if (next != null && next.equals(lastText)) {
                    return;
                }
                lastText = next;
                revalidate();
            });
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
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                g2.dispose();
                return;
            }

            boolean pressed = getModel().isPressed();
            boolean rollover = getModel().isRollover();

            // Оформление — контурная кнопка: вместо плотной заливки остаётся только
            // едва заметная подложка, а роль «тела» берёт на себя двойная обводка.
            // Так кнопки не сливаются с полупрозрачной карточкой ни в одной теме.
            Color accent = theme.getAccent();
            Color accentHover = theme.getAccentHover();
            Color text = theme.getText();

            int fillAlpha = pressed ? 46 : (rollover ? 30 : (highlighted ? 22 : 12));
            Color fill = withAlpha(highlighted ? theme.getAffordable() : accent, fillAlpha);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, width, height, ARC, ARC);

            // Мягкое свечение вокруг контура при наведении/нажатии.
            if (rollover || pressed) {
                for (int i = 3; i >= 1; i--) {
                    g2.setColor(withAlpha(accentHover, pressed ? 26 : 16));
                    g2.setStroke(new BasicStroke(i * 1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawRoundRect(i, i, width - 1 - i * 2, height - 1 - i * 2, ARC, ARC);
                }
            }

            // Основной контур: акцентный и тем светлее, чем активнее кнопка.
            Color border = pressed ? accentHover
                    : (rollover ? accentHover : (highlighted ? theme.getAffordable().brighter() : accent));
            g2.setColor(withAlpha(border, pressed || rollover ? 245 : 190));
            g2.setStroke(new BasicStroke(pressed ? 2.6f : (rollover || highlighted ? 2.2f : 1.6f)));
            g2.drawRoundRect(1, 1, width - 3, height - 3, ARC, ARC);

            // Внутренняя светлая кромка — объём без градиентной заливки.
            g2.setColor(withAlpha(text, rollover ? 44 : 24));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(4, 4, width - 9, height - 9, ARC - 3, ARC - 3);

            g2.dispose();
            super.paintComponent(g);
        }

        private static Color withAlpha(Color color, int alpha) {
            return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    Math.max(0, Math.min(255, alpha)));
        }
    }
}
