package WindowApps.clicker;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ProgressStorage {

    private static final String CONFIG_FILE = "config.properties";
    private static final String CLICK_COUNT_KEY = "clickCount";
    private static final String CLICKS_PER_PRESS_KEY = "clicksPerPress";
    private static final String AUTO_CLICKER_KEY = "autoClicker";
    private static final String MULTIPLIER_KEY = "multiplier";

    private final GameProgress progress;

    ProgressStorage(GameProgress progress) {
        this.progress = progress;
    }

    boolean save() {
        Properties props = new Properties();
        props.setProperty(CLICK_COUNT_KEY, String.valueOf(progress.getClickCount()));
        props.setProperty(CLICKS_PER_PRESS_KEY, String.valueOf(progress.getClicksPerPress()));
        props.setProperty(AUTO_CLICKER_KEY, String.valueOf(progress.getAutoClickerLevel()));
        props.setProperty(MULTIPLIER_KEY, String.valueOf(progress.getMultiplierLevel()));

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "App Settings");
            progress.markSaved();
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
            return false;
        }
    }

    void load() {
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            progress.setState(
                    readInt(props, CLICK_COUNT_KEY, 0),
                    readInt(props, CLICKS_PER_PRESS_KEY, 1),
                    readInt(props, AUTO_CLICKER_KEY, 0),
                    readInt(props, MULTIPLIER_KEY, 0));
        } catch (FileNotFoundException e) {
            // первого запуска файла ещё нет — стартуем с нуля
        } catch (IOException | NumberFormatException e) {
            System.err.println("Ошибка загрузки: " + e.getMessage());
        }
    }

    private int readInt(Properties props, String key, int defaultValue) {
        return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
    }
}
