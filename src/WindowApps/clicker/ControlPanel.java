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

        JPanel buttons = new JPanel(new GridLayout(4, 1, 0, ClickerTheme.BUTTON_GAP));
        buttons.setOpaque(false);
        buttons.add(ClickerTheme.createButton(basic, "button.shop", SHOP));
        buttons.add(ClickerTheme.createButton(basic, "button.settings", SETTINGS));
        buttons.add(ClickerTheme.createButton(basic, "button.reset", RESET));
        buttons.add(ClickerTheme.createButton(basic, "button.save", SAVE));

        JPanel exitPanel = new JPanel(new BorderLayout());
        exitPanel.setOpaque(false);
        exitPanel.setBorder(BorderFactory.createEmptyBorder(ClickerTheme.EXIT_GAP, 0, 0, 0));
        exitPanel.add(ClickerTheme.createButton(basic, "button.exit", Basic.EXIT));

        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(buttons, BorderLayout.CENTER);
        column.add(exitPanel, BorderLayout.SOUTH);

        JPanel card = ClickerTheme.createCard(basic.getActiveTheme(), new BorderLayout(), 18);
        card.add(ClickerTheme.createCaption(Text.get("panel.actions"), basic.getActiveTheme()), BorderLayout.NORTH);
        card.add(column, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(ClickerTheme.BUTTON_WIDTH + 60, Integer.MAX_VALUE));

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weighty = 1;
        wrapper.add(card, constraints);

        setLayout(new BorderLayout());
        add(wrapper, BorderLayout.CENTER);
    }
}
