package WindowApps.clicker;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class GameProgress {

    private int clickCount = 0;
    private int clicksPerPress = 1;
    private int autoClickerLevel = 0;
    private int multiplierLevel = 0;
    private boolean hasUnsavedChanges = false;
    private boolean paused = false;

    private final List<Runnable> listeners = new ArrayList<>();

    void addListener(Runnable listener) {
        listeners.add(listener);
    }

    void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public int getClickCount() {
        return clickCount;
    }

    public int getClicksPerPress() {
        return clicksPerPress;
    }

    public int getAutoClickerLevel() {
        return autoClickerLevel;
    }

    public int getMultiplierLevel() {
        return multiplierLevel;
    }

    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    public boolean isPaused() {
        return paused;
    }

    void setPaused(boolean paused) {
        this.paused = paused;
    }

    void setState(int clickCount, int clicksPerPress, int autoClickerLevel, int multiplierLevel) {
        this.clickCount = clickCount;
        this.clicksPerPress = clicksPerPress;
        this.autoClickerLevel = autoClickerLevel;
        this.multiplierLevel = multiplierLevel;
        this.hasUnsavedChanges = false;
        notifyListeners();
    }

    void markSaved() {
        hasUnsavedChanges = false;
    }

    int clicksPerPressCost() {
        return clicksPerPress * 50;
    }

    int autoClickerCost() {
        return 100 * (autoClickerLevel + 1) * (autoClickerLevel + 1);
    }

    int multiplierCost() {
        return 200 * (multiplierLevel + 1) * (multiplierLevel + 1);
    }

    boolean buyClicksPerPress() {
        if (paused || !spend(clicksPerPressCost())) {
            return false;
        }
        clicksPerPress++;
        notifyListeners();
        return true;
    }

    boolean buyAutoClicker() {
        if (paused || !spend(autoClickerCost())) {
            return false;
        }
        autoClickerLevel++;
        notifyListeners();
        return true;
    }

    boolean buyMultiplier() {
        if (paused || !spend(multiplierCost())) {
            return false;
        }
        multiplierLevel++;
        notifyListeners();
        return true;
    }

    void addClicks(int amount) {
        if (paused) {
            return;
        }
        clickCount += amount;
        hasUnsavedChanges = true;
        notifyListeners();
    }

    void reset() {
        if (paused) {
            return;
        }
        clickCount = 0;
        hasUnsavedChanges = true;
        notifyListeners();
    }

    int clicksPerAction() {
        return clicksPerPress * (multiplierLevel + 1);
    }

    private boolean spend(int cost) {
        if (clickCount < cost) {
            return false;
        }
        clickCount -= cost;
        hasUnsavedChanges = true;
        return true;
    }

    void showInsufficientFunds(JFrame owner) {
        JOptionPane.showMessageDialog(owner,
                "Не хватает кликов: нужно " + Math.min(Math.min(clicksPerPressCost(), autoClickerCost()), multiplierCost())
                        + ", есть " + clickCount + ".",
                "Магазин",
                JOptionPane.WARNING_MESSAGE);
    }
}
