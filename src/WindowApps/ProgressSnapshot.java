package WindowApps;

import java.util.Objects;

public record ProgressSnapshot(int clicks, int clicksPerPress, int autoClickerLevel, int multiplierLevel,
                              String language, Theme theme) {

    public ProgressSnapshot {
        Objects.requireNonNull(language);
        Objects.requireNonNull(theme);
    }
}
