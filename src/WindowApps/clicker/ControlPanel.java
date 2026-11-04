package WindowApps.clicker;

import WindowApps.Basic;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final String CLICK = "click";
    public static final String SHOP = "shop";
    public static final String RESET = "reset";
    public static final String SAVE = "save";

    public ControlPanel(Basic basic) {
        super(new BorderLayout());
        setOpaque(false);

        JPanel buttons = new JPanel(new GridLayout(4, 1, 0, ClickerTheme.BUTTON_GAP));
        buttons.setOpaque(false);
        buttons.add(ClickerTheme.createButton(basic, "Нажми", CLICK));
        buttons.add(ClickerTheme.createButton(basic, "Магазин", SHOP));
        buttons.add(ClickerTheme.createButton(basic, "Сброс", RESET));
        buttons.add(ClickerTheme.createButton(basic, "Сохраниться", SAVE));

        JPanel exitPanel = new JPanel(new BorderLayout());
        exitPanel.setOpaque(false);
        exitPanel.setBorder(BorderFactory.createEmptyBorder(ClickerTheme.EXIT_GAP, 0, 0, 0));
        exitPanel.add(ClickerTheme.createButton(basic, "Выход", Basic.EXIT));

        add(buttons, BorderLayout.CENTER);
        add(exitPanel, BorderLayout.SOUTH);
    }
}
