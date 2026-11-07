package WindowApps.clicker.storage;

import WindowApps.clicker.model.GameProgress;

import java.nio.file.Path;

/**
 * Хранилище прогресса приложения: тонкая обёртка над {@link WindowApps.ProgressStorage}.
 * Основной формат — бинарный clicker.sav (%APPDATA%\Clicker), клики в config.properties больше не пишутся.
 */
public class ProgressStorage {

    private final WindowApps.ProgressStorage store;

    public ProgressStorage(GameProgress progress) {
        this.store = new WindowApps.ProgressStorage(progress);
    }

    public boolean save() {
        return store.save();
    }

    public void load() {
        store.load();
    }

    public Path getStorePath() {
        return store.getStorePath();
    }
}
