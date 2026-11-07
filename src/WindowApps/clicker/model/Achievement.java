package WindowApps.clicker.model;

import WindowApps.Text;
import WindowApps.Theme;

import java.util.function.Predicate;

/**
 * Все достижения игры. Идентификатор {@link #getId()} стабилен и попадает в файл прогресса,
 * поэтому его нельзя менять без миграции сохранений; тексты берутся из messages*.properties.
 */
public enum Achievement {

    FIRST_CLICK("first_click", "achievement.firstClick.title", "achievement.firstClick.desc",
            progress -> progress.getClickCount() >= 1),
    CLICKS_100("clicks_100", "achievement.clicks100.title", "achievement.clicks100.desc",
            progress -> progress.getClickCount() >= 100),
    CLICKS_1000("clicks_1000", "achievement.clicks1k.title", "achievement.clicks1k.desc",
            progress -> progress.getClickCount() >= 1000),
    CLICKS_10000("clicks_10000", "achievement.clicks10k.title", "achievement.clicks10k.desc",
            progress -> progress.getClickCount() >= 10000),
    CLICKS_100000("clicks_100000", "achievement.clicks100k.title", "achievement.clicks100k.desc",
            progress -> progress.getClickCount() >= 100000),
    CLICKS_1000000("clicks_1000000", "achievement.clicks1m.title", "achievement.clicks1m.desc",
            progress -> progress.getClickCount() >= 1000000),
    PER_PRESS_UPGRADE("per_press_upgrade", "achievement.perPress.title", "achievement.perPress.desc",
            progress -> progress.getClicksPerPress() >= 2),
    AUTO_CLICKER("auto_clicker", "achievement.auto.title", "achievement.auto.desc",
            progress -> progress.getAutoClickerLevel() >= 1),
    AUTO_CLICKER_5("auto_clicker_5", "achievement.auto5.title", "achievement.auto5.desc",
            progress -> progress.getAutoClickerLevel() >= 5),
    MULTIPLIER("multiplier", "achievement.multiplier.title", "achievement.multiplier.desc",
            progress -> progress.getMultiplierLevel() >= 1),
    MULTIPLIER_5("multiplier_5", "achievement.multiplier5.title", "achievement.multiplier5.desc",
            progress -> progress.getMultiplierLevel() >= 5),
    THEME_CHANGED("theme_changed", "achievement.theme.title", "achievement.theme.desc",
            progress -> progress.getTheme() != Theme.STANDARD);

    private final String id;
    private final String titleKey;
    private final String descriptionKey;
    private final Predicate<GameProgress> condition;

    Achievement(String id, String titleKey, String descriptionKey, Predicate<GameProgress> condition) {
        this.id = id;
        this.titleKey = titleKey;
        this.descriptionKey = descriptionKey;
        this.condition = condition;
    }

    /** Ключ сохранения в файле прогресса — не зависит от языка. */
    public String getId() {
        return id;
    }

    public String getTitle() {
        return Text.get(titleKey);
    }

    public String getDescription() {
        return Text.get(descriptionKey);
    }

    boolean isEarned(GameProgress progress) {
        return condition.test(progress);
    }

    /** Ищет достижение по идентификатору из сохранения; неизвестный id просто пропускается. */
    public static Achievement byId(String id) {
        for (Achievement achievement : values()) {
            if (achievement.id.equals(id)) {
                return achievement;
            }
        }
        return null;
    }
}
