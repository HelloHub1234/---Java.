package WindowApps.clicker;

import WindowApps.Basic;
import WindowApps.Text;
import WindowApps.Theme;

import javax.swing.*;
import java.awt.*;

/** Константы размеров, шрифтов, задержек и фабрики визуальных компонентов кликера. */
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
    public static final Dimension CLICK_BUTTON_SIZE = new Dimension(320, 116);

    public static final int CARD_ARC = 26;
    public static final int CARD_GLOW = 26;

    public static final Font BUTTON_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    public static final Font COUNT_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 96);
    public static final Font UPGRADE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    public static final Font BALANCE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 22);
    public static final Font CLICK_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 26);
    public static final Font PER_PRESS_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    public static final Font CAPTION_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);

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

    /**
     * Большая кнопка «Кликнуть» с собственным оформлением (градиент, блик, свечение).
     * Команда обязательна: без неё {@code handle} получает пустую строку и действие не находится.
     */
    public static ClickButton createClickButton(Basic basic, String text, String command) {
        ClickButton button = new ClickButton(basic, text);
        button.setFont(CLICK_FONT);
        button.setPreferredSize(CLICK_BUTTON_SIZE);
        button.setActionCommand(command);
        button.addActionListener(e -> basic.runCommand(command));
        return button;
    }

    /** Стеклянная карточка: полупрозрачная заливка, тонкая рамка и мягкая тень. */
    public static JPanel createCard(Theme theme, LayoutManager layout, int padding) {
        CardPanel card = new CardPanel(theme);
        card.setLayout(layout);
        card.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return card;
    }

    public static JLabel createCaption(String text, Theme theme) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(CAPTION_FONT);
        Color textColor = theme.getText();
        label.setForeground(new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 165));
        return label;
    }
}
