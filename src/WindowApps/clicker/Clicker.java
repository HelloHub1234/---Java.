package WindowApps.clicker;

import WindowApps.Basic;

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
        output.setForeground(TEXT_COLOR);
        output.setHorizontalAlignment(SwingConstants.CENTER);
        output.setFont(ClickerTheme.COUNT_FONT);

        panel.setBorder(BorderFactory.createEmptyBorder(
                ClickerTheme.PANEL_PADDING.top, ClickerTheme.PANEL_PADDING.left,
                ClickerTheme.PANEL_PADDING.bottom, ClickerTheme.PANEL_PADDING.right));
        panel.add(output, BorderLayout.CENTER);
        panel.add(new ControlPanel(this), BorderLayout.EAST);
        panel.setComponentZOrder(particleLayer(), 0);

        storage.load();
        showCount();
        resume();
    }

    @Override
    protected void initActions() {
        super.initActions();

        actions.put(EXIT, this::exitApplication);
        actions.put(ControlPanel.CLICK, () -> {
            progress.addClicks(progress.clicksPerAction());
            spawnParticles(panel.getComponentAt(panel.getWidth() / 2, panel.getHeight() / 2));
        });
        actions.put(ControlPanel.SHOP, () -> new ShopDialog(this, frame, progress, this::showCount).setVisible(true));
        actions.put(ControlPanel.RESET, () -> {
            if (confirm("Сбросить количество кликов?")) {
                progress.reset();
            }
        });
        actions.put(ControlPanel.SAVE, this::saveWithNotice);

        progress.addListener(this::showCount);
    }

    @Override
    protected void handle(String command) {
        actions.getOrDefault(command, () -> output.setText("Не знаю такой команды: " + command)).run();
    }

    private void pause() {
        progress.setPaused(true);
        if (autoClicker != null) {
            autoClicker.stop();
        }
    }

    private void resume() {
        progress.setPaused(false);
        int level = progress.getAutoClickerLevel();
        if (level <= 0) {
            return;
        }
        if (autoClicker == null) {
            autoClicker = new Timer(ClickerTheme.AUTOCLICKER_DELAY_MS,
                    e -> progress.addClicks(progress.clicksPerAction()));
        }
        autoClicker.setInitialDelay(ClickerTheme.AUTOCLICKER_DELAY_MS / level);
        autoClicker.setDelay(ClickerTheme.AUTOCLICKER_DELAY_MS / level);
        autoClicker.start();
    }

    private void exitApplication() {
        pause();

        if (!progress.hasUnsavedChanges()) {
            if (confirm("Вы уверены, что хотите выйти?")) {
                close();
            } else {
                resume();
            }
            return;
        }

        int answer = JOptionPane.showConfirmDialog(frame,
                "Сохранить изменения перед выходом?",
                "Выход",
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
        output.setText("Кликов: " + progress.getClickCount());
    }

    private void saveWithNotice() {
        if (storage.save()) {
            JOptionPane.showMessageDialog(frame, "Сохранено!");
        }
    }
}
