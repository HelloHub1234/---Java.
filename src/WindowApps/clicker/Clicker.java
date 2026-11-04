package WindowApps.clicker;

import WindowApps.Basic;
import WindowApps.Text;
import WindowApps.Theme;

import javax.swing.*;
import java.awt.*;

public class Clicker extends Basic {

    private final GameProgress progress = new GameProgress();
    private final ProgressStorage storage = new ProgressStorage(progress);
    private Timer autoClicker;

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
        addParticleLayer();

        output = new JLabel();
        output.setForeground(activeTheme.getText());
        output.setHorizontalAlignment(SwingConstants.CENTER);
        output.setFont(ClickerTheme.COUNT_FONT);

        panel.setBorder(BorderFactory.createEmptyBorder(
                ClickerTheme.PANEL_PADDING.top, ClickerTheme.PANEL_PADDING.left,
                ClickerTheme.PANEL_PADDING.bottom, ClickerTheme.PANEL_PADDING.right));
        panel.add(output, BorderLayout.CENTER);
        panel.add(new ControlPanel(this), BorderLayout.EAST);

        storage.load();
        applyTheme(progress.getTheme());
        showCount();
        resume();
    }

    @Override
    public void setTheme(Theme theme) {
        applyTheme(theme);
        progress.setTheme(theme);
        showCount();
    }

    @Override
    protected void initActions() {
        super.initActions();

        actions.put(EXIT, this::exitApplication);
        actions.put(ControlPanel.CLICK, () -> {
            progress.addClicks(progress.clicksPerAction());
            spawnParticles(output);
        });
        actions.put(ControlPanel.SHOP, () -> new ShopDialog(this, frame, progress, this::showCount).setVisible(true));
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
        actions.getOrDefault(command, () -> output.setText(Text.format("unknown.command", command))).run();
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
        output.setText(Text.format("count.clicks", progress.getClickCount()));
    }

    private void saveWithNotice() {
        if (storage.save()) {
            JOptionPane.showMessageDialog(frame, Text.format("save.done", storage.getStorePath()));
        }
    }
}
