package WindowApps.clicker.ui;

import WindowApps.Basic;
import WindowApps.Text;
import WindowApps.clicker.model.GameProgress;
import WindowApps.clicker.ui.theme.ClickerTheme;

import javax.swing.*;
import java.awt.*;
import java.util.function.BooleanSupplier;

public class ShopDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final GameProgress progress;
    private final Runnable onPurchased;

    private final JLabel balance = new JLabel();
    private final JButton perPressButton;
    private final JButton autoClickerButton;
    private final JButton multiplierButton;

    public ShopDialog(Basic basic, JFrame owner, GameProgress progress, Runnable onPurchased) {
        super(owner, Text.get("shop.title"), true);
        this.progress = progress;
        this.onPurchased = onPurchased;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        perPressButton = makeUpgrade(basic);
        autoClickerButton = makeUpgrade(basic);
        multiplierButton = makeUpgrade(basic);

        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(basic.getActiveTheme().getBackgroundTop());
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        balance.setForeground(basic.getActiveTheme().getText());
        balance.setFont(ClickerTheme.BALANCE_FONT);
        balance.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel upgrades = new JPanel(new GridLayout(3, 1, 0, ClickerTheme.BUTTON_GAP));
        upgrades.setOpaque(false);
        upgrades.add(perPressButton);
        upgrades.add(autoClickerButton);
        upgrades.add(multiplierButton);

        JButton closeButton = ClickerTheme.createButton(basic, Text.get("shop.close"));
        closeButton.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.add(closeButton);

        perPressButton.addActionListener(e ->
                purchase(progress::buyClicksPerPress, progress.clicksPerPressCost()));
        autoClickerButton.addActionListener(e ->
                purchase(progress::buyAutoClicker, progress.autoClickerCost()));
        multiplierButton.addActionListener(e ->
                purchase(progress::buyMultiplier, progress.multiplierCost()));

        content.add(balance, BorderLayout.NORTH);
        content.add(upgrades, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        setContentPane(content);

        setSize(620, 480);
        setLocationRelativeTo(owner);
        refreshUpgrades();
    }

    private JButton makeUpgrade(Basic basic) {
        JButton button = basic.createButton("");
        button.setPreferredSize(ClickerTheme.UPGRADE_SIZE);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(ClickerTheme.UPGRADE_FONT);
        return button;
    }

    private void purchase(BooleanSupplier purchase, int cost) {
        if (purchase.getAsBoolean()) {
            onPurchased.run();
            refreshUpgrades();
        } else {
            progress.showInsufficientFunds((JFrame) getOwner(), cost);
        }
    }

    private void refreshUpgrades() {
        balance.setText(Text.format("shop.balance", progress.getClickCount()));
        setAffordable(perPressButton, progress.clicksPerPressCost());
        setAffordable(autoClickerButton, progress.autoClickerCost());
        setAffordable(multiplierButton, progress.multiplierCost());

        int perPress = progress.getClicksPerPress();
        perPressButton.setText("<html><b>" + Text.get("upgrade.perPress.title") + "</b><br>"
                + Text.format("upgrade.perPress.line1", perPress, perPress + 1) + " · "
                + Text.format("upgrade.perPress.line2", perPress, progress.clicksPerPressCost()) + "</html>");

        int autoLevel = progress.getAutoClickerLevel();
        String autoLine1 = autoLevel == 0
                ? Text.get("upgrade.auto.line1")
                : Text.format("upgrade.auto.line1Owned", autoLevel);
        String autoLine2 = autoLevel == 0
                ? Text.format("upgrade.auto.line2Idle", progress.autoClickerCost())
                : Text.format("upgrade.auto.line2", ClickerTheme.AUTOCLICKER_DELAY_MS / autoLevel,
                progress.autoClickerCost());
        autoClickerButton.setText("<html><b>" + Text.get("upgrade.auto.title") + "</b><br>"
                + autoLine1 + " · " + autoLine2 + "</html>");

        int multiplier = progress.getMultiplierLevel();
        multiplierButton.setText("<html><b>" + Text.get("upgrade.multiplier.title") + "</b><br>"
                + Text.format("upgrade.multiplier.line1", multiplier, multiplier + 1) + " · "
                + Text.format("upgrade.multiplier.line2", multiplier + 1, progress.multiplierCost()) + "</html>");
    }

    private void setAffordable(JButton button, int cost) {
        if (button instanceof Basic.RoundedButton rounded) {
            rounded.setHighlighted(progress.getClickCount() >= cost);
        }
    }
}
