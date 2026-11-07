package WindowApps.clicker.model;

import WindowApps.ProgressSnapshot;
import WindowApps.Text;
import WindowApps.Theme;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class GameProgress {

    private int clickCount = 0;
    private int clicksPerPress = 1;
    private int autoClickerLevel = 0;
    private int multiplierLevel = 0;
    private Theme theme = Theme.STANDARD;
    private boolean hasUnsavedChanges = false;
    private boolean paused = false;

    private final List<Runnable> listeners = new ArrayList<>();
    private final Set<Achievement> unlockedAchievements = EnumSet.noneOf(Achievement.class);
    private final List<Consumer<Achievement>> achievementListeners = new ArrayList<>();

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    /** Подписка на НОВЫЕ достижения. Вызывается только в момент получения, не при загрузке. */
    public void addAchievementListener(Consumer<Achievement> listener) {
        achievementListeners.add(listener);
    }

    public Set<Achievement> getUnlockedAchievements() {
        return Collections.unmodifiableSet(unlockedAchievements);
    }

    public boolean isAchievementUnlocked(Achievement achievement) {
        return unlockedAchievements.contains(achievement);
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

    public void setPaused(boolean paused) {
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
        checkAchievements();
        hasUnsavedChanges = false;
        notifyListeners();
    }

    public void setState(int clickCount, int clicksPerPress, int autoClickerLevel, int multiplierLevel) {
        this.clickCount = clickCount;
        this.clicksPerPress = clicksPerPress;
        this.autoClickerLevel = autoClickerLevel;
        this.multiplierLevel = multiplierLevel;
        hasUnsavedChanges = false;
        // Достижения, которые уже выполнены, но не были записаны (старый файл прогресса),
        // добираются здесь же — ещё до подписки на уведомления, поэтому всплывать не будут.
        checkAchievements();
        hasUnsavedChanges = false;
        notifyListeners();
    }

    /** Восстанавливает полученные достижения из сохранения. */
    public void restoreAchievements(Set<Achievement> achievements) {
        unlockedAchievements.clear();
        if (achievements != null) {
            unlockedAchievements.addAll(achievements);
        }
    }

    public void setTheme(Theme theme) {
        if (theme == null || this.theme == theme) {
            return;
        }
        this.theme = theme;
        hasUnsavedChanges = true;
        checkAchievements();
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
        checkAchievements();
        notifyListeners();
        return true;
    }

    public boolean buyAutoClicker() {
        if (!spend(autoClickerCost())) {
            return false;
        }
        autoClickerLevel++;
        checkAchievements();
        notifyListeners();
        return true;
    }

    public boolean buyMultiplier() {
        if (!spend(multiplierCost())) {
            return false;
        }
        multiplierLevel++;
        checkAchievements();
        notifyListeners();
        return true;
    }

    public void addClicks(int amount) {
        if (paused) {
            return;
        }
        clickCount += amount;
        hasUnsavedChanges = true;
        checkAchievements();
        notifyListeners();
    }

    public void reset() {
        if (paused) {
            return;
        }
        clickCount = 0;
        hasUnsavedChanges = true;
        notifyListeners();
    }

    public int clicksPerAction() {
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

    /**
     * Выдаёт все достижения, условие которых уже выполнено, и сообщает подписчикам только о новых.
     * Вызывается после каждого изменения прогресса, поэтому проверка не должна быть дорогой.
     */
    private void checkAchievements() {
        for (Achievement achievement : Achievement.values()) {
            if (unlockedAchievements.contains(achievement) || !achievement.isEarned(this)) {
                continue;
            }
            unlockedAchievements.add(achievement);
            hasUnsavedChanges = true;
            for (Consumer<Achievement> listener : achievementListeners) {
                listener.accept(achievement);
            }
        }
    }

    public void showInsufficientFunds(JFrame owner, int cost) {
        JOptionPane.showMessageDialog(owner,
                Text.format("shop.insufficient", cost, clickCount),
                Text.get("shop.title"),
                JOptionPane.WARNING_MESSAGE);
    }
}
