package WindowApps.clicker;

import WindowApps.Basic;

import javax.swing.*;
import java.awt.*;

public class ShopDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final GameProgress progress;
    private final Runnable onPurchased;

    private final JLabel balance = new JLabel();
    private final JButton perPressButton;
    private final JButton autoClickerButton;
    private final JButton multiplierButton;

    public ShopDialog(Basic basic, JFrame owner, GameProgress progress, Runnable onPurchased) {
        super(owner, "Магазин", true);
        this.progress = progress;
        this.onPurchased = onPurchased;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        perPressButton = makeUpgrade(basic);
        autoClickerButton = makeUpgrade(basic);
        multiplierButton = makeUpgrade(basic);

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(Basic.BACKGROUND_BOTTOM);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        balance.setForeground(Basic.TEXT_COLOR);
        balance.setFont(ClickerTheme.BALANCE_FONT);
        balance.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel upgrades = new JPanel(new GridLayout(3, 1, 0, ClickerTheme.BUTTON_GAP));
        upgrades.setOpaque(false);
        upgrades.add(perPressButton);
        upgrades.add(autoClickerButton);
        upgrades.add(multiplierButton);

        JButton closeButton = ClickerTheme.createButton(basic, "Закрыть", null);
        closeButton.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.add(closeButton);

        perPressButton.addActionListener(e -> purchase(progress::buyClicksPerPress));
        autoClickerButton.addActionListener(e -> purchase(progress::buyAutoClicker));
        multiplierButton.addActionListener(e -> purchase(progress::buyMultiplier));

        content.add(balance, BorderLayout.NORTH);
        content.add(upgrades, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        setContentPane(content);

        setSize(620, 480);
        setLocationRelativeTo(owner);
        refreshUpgrades();
    }

    private JButton makeUpgrade(Basic basic) {
        JButton button = ClickerTheme.createButton(basic, "", null);
        button.setPreferredSize(ClickerTheme.UPGRADE_SIZE);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(ClickerTheme.UPGRADE_FONT);
        return button;
    }

    private void purchase(java.util.function.BooleanSupplier purchase) {
        if (purchase.getAsBoolean()) {
            onPurchased.run();
            refreshUpgrades();
        } else {
            progress.showInsufficientFunds((JFrame) getOwner());
        }
    }

    private void refreshUpgrades() {
        balance.setText("Кликов: " + progress.getClickCount());

        int perPress = progress.getClicksPerPress();
        perPressButton.setBackground(progress.getClickCount() >= progress.clicksPerPressCost()
                ? ClickerTheme.UPGRADE_AVAILABLE : ClickerTheme.UPGRADE_BACKGROUND);
        perPressButton.setText(String.format("<html><b>+1 клик за нажатие</b> &nbsp; ур. %d → %d<br>"
                        + "сейчас %d за нажатие &nbsp;·&nbsp; цена %d</html>",
                perPress, perPress + 1, perPress, progress.clicksPerPressCost()));

        int autoClickerLevel = progress.getAutoClickerLevel();
        autoClickerButton.setText(autoClickerLevel == 0
                ? String.format("<html><b>Автокликер</b> &nbsp; не куплен<br>"
                        + "кликает сам раз в секунду &nbsp;·&nbsp; цена %d</html>", progress.autoClickerCost())
                : String.format("<html><b>Автокликер</b> &nbsp; ур. %d<br>"
                        + "клик каждые %d мс &nbsp;·&nbsp; цена %d</html>",
                autoClickerLevel, ClickerTheme.AUTOCLICKER_DELAY_MS / autoClickerLevel, progress.autoClickerCost()));

        int multiplier = progress.getMultiplierLevel();
        multiplierButton.setText(String.format("<html><b>Множитель кликов</b> &nbsp; ур. %d → %d<br>"
                        + "клики ×%d &nbsp;·&nbsp; цена %d</html>",
                multiplier, multiplier + 1, multiplier + 1, progress.multiplierCost()));
    }
}
