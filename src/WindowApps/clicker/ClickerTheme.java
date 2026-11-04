package WindowApps.clicker;

import WindowApps.Basic;
import WindowApps.Text;

import javax.swing.*;
import java.awt.*;

public final class ClickerTheme {

    public static final int BUTTON_WIDTH = 180;
    public static final int BUTTON_HEIGHT = 60;
    public static final float BUTTON_FONT_SIZE = 15f;
    public static final int BUTTON_GAP = 10;
    public static final Insets PANEL_PADDING = new Insets(30, 40, 30, 40);
    public static final int EXIT_GAP = 20;
    public static final int AUTOCLICKER_DELAY_MS = 1000;

    public static final Dimension BUTTON_SIZE = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
    public static final Dimension UPGRADE_SIZE = new Dimension(520, 90);

    public static final Font BUTTON_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    public static final Font COUNT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 72);
    public static final Font UPGRADE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    public static final Font BALANCE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 22);

    private ClickerTheme() {
    }

    public static JButton createButton(Basic basic, String commandKey, String command) {
        JButton button = basic.createButton(Text.get(commandKey), command);
        button.setFont(BUTTON_FONT);
        button.setPreferredSize(BUTTON_SIZE);
        return button;
    }

    /** Кнопка без команды: текст задаётся сразу, обработчик вешает вызывающий код. */
    public static JButton createButton(Basic basic, String text) {
        JButton button = basic.createButton(text);
        button.setFont(BUTTON_FONT);
        button.setPreferredSize(BUTTON_SIZE);
        return button;
    }
}
