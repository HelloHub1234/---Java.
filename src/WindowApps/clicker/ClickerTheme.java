package WindowApps.clicker;

import WindowApps.Basic;

import javax.swing.*;
import java.awt.*;

public final class ClickerTheme {

    public static final int BUTTON_WIDTH = 180;
    public static final int BUTTON_HEIGHT = 60;
    public static final float BUTTON_FONT_SIZE = 18f;
    public static final float COUNT_FONT_SIZE = 72f;
    public static final int BUTTON_GAP = 12;
    public static final Insets PANEL_PADDING = new Insets(30, 40, 30, 40);
    public static final int EXIT_GAP = 20;
    public static final int AUTOCLICKER_DELAY_MS = 1000;

    public static final Dimension BUTTON_SIZE = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
    public static final Dimension UPGRADE_SIZE = new Dimension(520, 90);

    public static final Font BUTTON_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    public static final Font COUNT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 72);
    public static final Font UPGRADE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    public static final Font BALANCE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 22);

    public static final Color UPGRADE_BACKGROUND = new Color(0x252C3D);
    public static final Color UPGRADE_AVAILABLE = new Color(0x2E7D5B);

    private ClickerTheme() {
    }

    public static JButton createButton(Basic basic, String text, String command) {
        JButton button = basic.createButton(text, command);
        styleButton(button, BUTTON_FONT);
        return button;
    }

    public static void styleButton(JButton button, Font font) {
        button.setFont(font);
        button.setPreferredSize(BUTTON_SIZE);
    }
}
