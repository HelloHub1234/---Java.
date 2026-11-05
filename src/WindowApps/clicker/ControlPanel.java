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
        // Кнопки должны быть высокими:
        // GridLayout равномерно делит высоту колонки между строками, поэтому высоту задаёт
        // именно preferredSize кнопок, а не размер карточки. Раньше SIZE обнулялся,
        // и кнопки получали высоту из текста — то есть оставались маленькими.
        JButton shop = ClickerTheme.createButton(basic, "button.shop", SHOP);
        JButton settings = ClickerTheme.createButton(basic, "button.settings", SETTINGS);
        JButton reset = ClickerTheme.createButton(basic, "button.reset", RESET);
        JButton save = ClickerTheme.createButton(basic, "button.save", SAVE);
        for (JButton button : new JButton[]{shop, settings, reset, save}) {
            button.setPreferredSize(ClickerTheme.BUTTON_SIZE);
            button.setMinimumSize(ClickerTheme.BUTTON_SIZE);
            buttons.add(button);
        }

        JPanel exitPanel = new JPanel(new BorderLayout());
        exitPanel.setOpaque(false);
        exitPanel.setBorder(BorderFactory.createEmptyBorder(ClickerTheme.EXIT_GAP, 0, 0, 0));
        JButton exit = ClickerTheme.createButton(basic, "button.exit", Basic.EXIT);
        exit.setPreferredSize(ClickerTheme.BUTTON_SIZE);
        exit.setMinimumSize(ClickerTheme.BUTTON_SIZE);
        exitPanel.add(exit);

        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(buttons, BorderLayout.CENTER);
        column.add(exitPanel, BorderLayout.SOUTH);

        JPanel card = ClickerTheme.createCard(basic.getActiveTheme(), new BorderLayout(0, 14), 22);
        // Заголовок стоит по центру: JLabel с BorderLayout.NORTH растягивался на всю ширину
        // и прижимал текст влево — именно это делало подпись «Действия» сырой.
        JPanel header = new JPanel(new GridBagLayout());
        header.setOpaque(false);
        header.add(ClickerTheme.createCaption(Text.get("panel.actions"), basic.getActiveTheme()));
        card.add(header, BorderLayout.NORTH);
        card.add(column, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(ClickerTheme.BUTTON_WIDTH + 104, Integer.MAX_VALUE));
        card.setMinimumSize(new Dimension(ClickerTheme.BUTTON_WIDTH + 104, 620));

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
