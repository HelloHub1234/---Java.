package WindowApps.clicker;

import WindowApps.ProgressSnapshot;
import WindowApps.Text;
import WindowApps.Theme;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class GameProgress {

    private int clickCount = 0;
    private int clicksPerPress = 1;
    private int autoClickerLevel = 0;
    private int multiplierLevel = 0;
    private Theme theme = Theme.STANDARD;
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

    public Theme getTheme() {
        return theme;
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

    public ProgressSnapshot snapshot() {
        return new ProgressSnapshot(clickCount, clicksPerPress, autoClickerLevel, multiplierLevel,
                Text.getLanguage(), theme);
    }

    void restore(ProgressSnapshot snapshot) {
        clickCount = snapshot.clicks();
        clicksPerPress = snapshot.clicksPerPress();
        autoClickerLevel = snapshot.autoClickerLevel();
        multiplierLevel = snapshot.multiplierLevel();
        theme = snapshot.theme();
        hasUnsavedChanges = false;
        notifyListeners();
    }

    public void setState(int clickCount, int clicksPerPress, int autoClickerLevel, int multiplierLevel) {
        this.clickCount = clickCount;
        this.clicksPerPress = clicksPerPress;
        this.autoClickerLevel = autoClickerLevel;
        this.multiplierLevel = multiplierLevel;
        this.hasUnsavedChanges = false;
        notifyListeners();
    }

    public void setTheme(Theme theme) {
        if (theme == null || this.theme == theme) {
            return;
        }
        this.theme = theme;
        hasUnsavedChanges = true;
        notifyListeners();
    }

    public void markDirty() {
        hasUnsavedChanges = true;
    }

    public void markSaved() {
        hasUnsavedChanges = false;
    }

    public int clicksPerPressCost() {
        return clicksPerPress * 50;
    }

    public int autoClickerCost() {
        return 100 * (autoClickerLevel + 1) * (autoClickerLevel + 1);
    }

    public int multiplierCost() {
        return 200 * (multiplierLevel + 1) * (multiplierLevel + 1);
    }

    public boolean buyClicksPerPress() {
        if (!spend(clicksPerPressCost())) {
            return false;
        }
        clicksPerPress++;
        notifyListeners();
        return true;
    }

    public boolean buyAutoClicker() {
        if (!spend(autoClickerCost())) {
            return false;
        }
        autoClickerLevel++;
        notifyListeners();
        return true;
    }

    public boolean buyMultiplier() {
        if (!spend(multiplierCost())) {
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

    public void showInsufficientFunds(JFrame owner, int cost) {
        JOptionPane.showMessageDialog(owner,
                Text.format("shop.insufficient", cost, clickCount),
                Text.get("shop.title"),
                JOptionPane.WARNING_MESSAGE);
    }
}
