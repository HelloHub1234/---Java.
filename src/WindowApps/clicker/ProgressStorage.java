package WindowApps.clicker;

import java.nio.file.Path;

/**
 * Хранилище прогресса приложения: тонкая обёртка над {@link WindowApps.ProgressStorage}.
 * Основной формат — бинарный clicker.sav (%APPDATA%\Clicker), клики в config.properties больше не пишутся.
 */
public class ProgressStorage {

    private final WindowApps.ProgressStorage store;

    ProgressStorage(GameProgress progress) {
        this.store = new WindowApps.ProgressStorage(progress);
    }

    boolean save() {
        return store.save();
    }

    void load() {
        store.load();
    }

    Path getStorePath() {
        return store.getStorePath();
    }
}
