package WindowApps.clicker.ui;

import WindowApps.Basic;
import WindowApps.clicker.ui.theme.ClickerTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Нижняя панель команд: горизонтальный ряд кнопок под счётчиком.
 * Раньше кнопки стояли вертикальной колонкой, но с появлением достижений их стало шесть,
 * и колонка перестала помещаться по высоте.
 */
public class ControlPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final String CLICK = "click";
    public static final String SHOP = "shop";
    public static final String ACHIEVEMENTS = "achievements";
    public static final String SETTINGS = "settings";
    public static final String RESET = "reset";
    public static final String SAVE = "save";

    public ControlPanel(Basic basic) {
        super(new BorderLayout());
        setOpaque(false);

        JPanel row = new JPanel(new GridLayout(1, 5, ClickerTheme.BUTTON_GAP, 0));
        row.setOpaque(false);
        row.add(makeButton(basic, "button.shop", SHOP));
        row.add(makeButton(basic, "button.achievements", ACHIEVEMENTS));
        row.add(makeButton(basic, "button.settings", SETTINGS));
        row.add(makeButton(basic, "button.save", SAVE));
        row.add(makeButton(basic, "button.reset", RESET));

        JPanel exitWrapper = new JPanel(new GridBagLayout());
        exitWrapper.setOpaque(false);
        exitWrapper.setBorder(BorderFactory.createEmptyBorder(0, ClickerTheme.BUTTON_GAP * 2, 0, 0));
        exitWrapper.add(makeButton(basic, "button.exit", Basic.EXIT));

        JPanel card = ClickerTheme.createCard(basic.getActiveTheme(), new BorderLayout(), 18);
        card.add(row, BorderLayout.CENTER);
        card.add(exitWrapper, BorderLayout.EAST);

        add(card, BorderLayout.CENTER);
    }

    private static JButton makeButton(Basic basic, String key, String command) {
        JButton button = ClickerTheme.createButton(basic, key, command);
        button.setPreferredSize(ClickerTheme.HORIZONTAL_BUTTON_SIZE);
        button.setMinimumSize(ClickerTheme.HORIZONTAL_BUTTON_SIZE);
        return button;
    }
}
