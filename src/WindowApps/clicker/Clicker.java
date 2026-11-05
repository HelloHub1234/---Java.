package WindowApps.clicker;

import WindowApps.Basic;
import WindowApps.Text;
import WindowApps.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Clicker extends Basic {

    private final GameProgress progress = new GameProgress();
    private final ProgressStorage storage = new ProgressStorage(progress);
    private Timer autoClicker;
    private CountPanel countPanel;
    /**
     * Точка последнего нажатия в координатах панели. Кнопка отдаёт событие мыши, но действие
     * выполняется уже после него, поэтому координаты нужно запомнить заранее.
     */
    private Point lastClickPoint;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Clicker().start());
    }

    @Override
    protected String getTitle() {
        return "Тестовый кликкер";
    }

    @Override
    protected void buildUI() {
        super.buildUI();

        countPanel = new CountPanel(this, Text.get("button.click"), ControlPanel.CLICK);

        panel.setBorder(BorderFactory.createEmptyBorder(
                ClickerTheme.PANEL_PADDING.top, ClickerTheme.PANEL_PADDING.left,
                ClickerTheme.PANEL_PADDING.bottom, ClickerTheme.PANEL_PADDING.right));
        panel.add(countPanel, BorderLayout.CENTER);
        panel.add(new ControlPanel(this), BorderLayout.EAST);
        addParticleLayer();

        // Эффект должен рождаться в точке клика. Ловим два источника:
        // 1) кнопку «Нажми» отдельно — JButton не даёт mousePressed всплывать на панель,
        //    и раньше координаты оставались от ПРЕДЫДУЩЕГО клика по фону;
        // 2) саму панель — нажатие по пустому месту тоже запоминается, чтобы следующее
        //    действие не использовало устаревшую точку.
        listClickPoint(countPanel.getClickButton());
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                rememberClickPoint(e);
            }
        });

    }

    /** Запоминает точку нажатия в координатах панели (компоненты любого уровня вложенности). */
    private void rememberClickPoint(MouseEvent e) {
        lastClickPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), panel);
        WindowApps.clicker.ParticleDebug.log("PRESS comp=" + e.getComponent().getClass().getSimpleName()
                + " local=" + e.getPoint() + " panel=" + lastClickPoint);
    }

    /** Подписывает компонент на запоминание точки нажатия. */
    private void listClickPoint(Component component) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                rememberClickPoint(e);
            }
        });
    }

    @Override
    public void stopAnimation() {
        if (countPanel != null) {
            countPanel.stopAnimation();
        }
        super.stopAnimation();
    }

    @Override
    public void setTheme(Theme theme) {
        applyTheme(theme);
        progress.setTheme(theme);
        showCount();
        if (countPanel != null) {
            countPanel.updateTheme();
            countPanel.repaintTexts();
        }
    }

    @Override
    protected void initActions() {
        super.initActions();

        actions.put(EXIT, this::exitApplication);
        actions.put(ControlPanel.CLICK, () -> {
            progress.addClicks(progress.clicksPerAction());
            WindowApps.clicker.ParticleDebug.log("CLICK action point=" + lastClickPoint);
            spawnParticles(lastClickPoint);
        });
        actions.put(ControlPanel.SHOP, () -> {
            // на время диалога автокликер не тикает, чтобы счётчик не дёргался под модальным окном
            boolean wasPaused = progress.isPaused();
            pause();
            try {
                new ShopDialog(this, frame, progress, this::showCount).setVisible(true);
            } finally {
                if (!wasPaused) {
                    resume();
                }
            }
        });
        actions.put(ControlPanel.SETTINGS, () -> new SettingsDialog(this, frame).setVisible(true));
        actions.put(ControlPanel.RESET, () -> {
            if (confirm(Text.get("reset.confirm"))) {
                progress.reset();
            }
        });
        actions.put(ControlPanel.SAVE, this::saveWithNotice);

        progress.addListener(() -> {
            showCount();
            restartAutoClicker();
        });
    }

    @Override
    protected void handle(String command) {
        actions.getOrDefault(command, () -> {
            if (countPanel != null) {
                countPanel.showHint(Text.format("unknown.command", command));
            }
        }).run();
    }

    private void pause() {
        progress.setPaused(true);
        if (autoClicker != null) {
            autoClicker.stop();
        }
    }

    private void resume() {
        progress.setPaused(false);
        restartAutoClicker();
    }

    /** Перезапускает таймер под текущий уровень автокликера: раз в секунду на каждый уровень. */
    private void restartAutoClicker() {
        int level = progress.getAutoClickerLevel();
        if (progress.isPaused() || level <= 0) {
            if (autoClicker != null) {
                autoClicker.stop();
            }
            return;
        }
        int delay = Math.max(1, ClickerTheme.AUTOCLICKER_DELAY_MS / level);
        if (autoClicker == null) {
            autoClicker = new Timer(delay, e -> progress.addClicks(progress.clicksPerAction()));
            autoClicker.setInitialDelay(delay);
        }
        autoClicker.setDelay(delay);
        autoClicker.start();
    }

    private void exitApplication() {
        pause();

        if (!progress.hasUnsavedChanges()) {
            if (confirm(Text.get("exit.confirm"))) {
                close();
            } else {
                resume();
            }
            return;
        }

        int answer = JOptionPane.showConfirmDialog(frame,
                Text.get("exit.savePrompt"),
                Text.get("button.exit"),
                JOptionPane.YES_NO_CANCEL_OPTION);
        switch (answer) {
            case JOptionPane.YES_OPTION -> {
                storage.save();
                close();
            }
            case JOptionPane.NO_OPTION -> close();
            case JOptionPane.CANCEL_OPTION -> resume(); // остаёмся в игре
        }
    }

    private void showCount() {
        if (countPanel == null) {
            return;
        }
        countPanel.showCount(progress.getClickCount());
        countPanel.showPerPress(progress.clicksPerAction());
        int level = progress.getAutoClickerLevel();
        countPanel.showHint(level <= 0
                ? Text.get("count.hint.idle")
                : Text.format("count.hint.auto", ClickerTheme.AUTOCLICKER_DELAY_MS / level));
    }

    private void saveWithNotice() {
        if (storage.save()) {
            JOptionPane.showMessageDialog(frame, Text.format("save.done", storage.getStorePath()));
        }
    }
}
