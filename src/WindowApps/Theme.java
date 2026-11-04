package WindowApps;

import java.awt.*;

public enum Theme {
    STANDARD("Стандартная",
            new Color(0x1E2433), new Color(0x0E1118),
            new Color(0x4C8DFF), new Color(0x6FA4FF),
            new Color(0x2E7D5B), new Color(0x252C3D), new Color(0x323B52),
            new Color(0xEDF1F7), new Color(0x7FB3FF)),
    CYBERPUNK("Киберпанк · Неон",
            new Color(0x22043C), new Color(0x09000F),
            new Color(0xFF2E97), new Color(0xFF6FBF),
            new Color(0x1FB6A6), new Color(0x2A0B45), new Color(0x3C1259),
            new Color(0xF3E9FF), new Color(0x00E5FF)),
    FOREST("Лесная",
            new Color(0x1C3128), new Color(0x0A1411),
            new Color(0x4FA97A), new Color(0x79C79C),
            new Color(0x2E7D5B), new Color(0x213A2F), new Color(0x2E4E3F),
            new Color(0xEAF5E9), new Color(0xA9FFC9)),
    SOLAR("Солнечная · Светлая",
            new Color(0xFFF6E8), new Color(0xF3E2C8),
            new Color(0xE08A1E), new Color(0xF0A840),
            new Color(0x3D8B57), new Color(0xFFEFD8), new Color(0xFCE0BC),
            new Color(0x2A2015), new Color(0xFFB74D));

    private final String displayName;
    private final Color backgroundTop;
    private final Color backgroundBottom;
    private final Color accent;
    private final Color accentHover;
    private final Color affordable;
    private final Color button;
    private final Color buttonHover;
    private final Color text;
    private final Color particle;

    Theme(String displayName, Color backgroundTop, Color backgroundBottom,
          Color accent, Color accentHover, Color affordable,
          Color button, Color buttonHover, Color text, Color particle) {
        this.displayName = displayName;
        this.backgroundTop = backgroundTop;
        this.backgroundBottom = backgroundBottom;
        this.accent = accent;
        this.accentHover = accentHover;
        this.affordable = affordable;
        this.button = button;
        this.buttonHover = buttonHover;
        this.text = text;
        this.particle = particle;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Ищет тему по имени константы; неизвестное или пустое имя не должно ломать загрузку прогресса.
     */
    public static Theme byName(String name) {
        if (name == null) {
            return STANDARD;
        }
        for (Theme theme : values()) {
            if (theme.name().equals(name)) {
                return theme;
            }
        }
        return STANDARD;
    }

    public Color getBackgroundTop() {
        return backgroundTop;
    }

    public Color getBackgroundBottom() {
        return backgroundBottom;
    }

    public Color getAccent() {
        return accent;
    }

    public Color getAccentHover() {
        return accentHover;
    }

    public Color getAffordable() {
        return affordable;
    }

    public Color getButton() {
        return button;
    }

    public Color getButtonHover() {
        return buttonHover;
    }

    public Color getText() {
        return text;
    }

    public Color getParticle() {
        return particle;
    }
}
