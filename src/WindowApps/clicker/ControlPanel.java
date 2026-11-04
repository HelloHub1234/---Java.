package WindowApps.clicker;

import WindowApps.Basic;
import WindowApps.Text;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final String CLICK = "click";
    public static final String SHOP = "shop";
    public static final String SETTINGS = "settings";
    public static final String RESET = "reset";
    public static final String SAVE = "save";

    public ControlPanel(Basic basic) {
        super(new BorderLayout());
        setOpaque(false);

        JPanel buttons = new JPanel(new GridLayout(5, 1, 0, ClickerTheme.BUTTON_GAP));
        buttons.setOpaque(false);
        buttons.add(ClickerTheme.createButton(basic, "button.click", CLICK));
        buttons.add(ClickerTheme.createButton(basic, "button.shop", SHOP));
        buttons.add(ClickerTheme.createButton(basic, "button.settings", SETTINGS));
        buttons.add(ClickerTheme.createButton(basic, "button.reset", RESET));
        buttons.add(ClickerTheme.createButton(basic, "button.save", SAVE));

        JPanel exitPanel = new JPanel(new BorderLayout());
        exitPanel.setOpaque(false);
        exitPanel.setBorder(BorderFactory.createEmptyBorder(ClickerTheme.EXIT_GAP, 0, 0, 0));
        exitPanel.add(ClickerTheme.createButton(basic, "button.exit", Basic.EXIT));

        add(buttons, BorderLayout.CENTER);
        add(exitPanel, BorderLayout.SOUTH);
    }
}
