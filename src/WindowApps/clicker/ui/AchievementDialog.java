package WindowApps.clicker.ui;

import WindowApps.Basic;
import WindowApps.Text;
import WindowApps.Theme;
import WindowApps.clicker.model.Achievement;
import WindowApps.clicker.model.GameProgress;
import WindowApps.clicker.ui.theme.ClickerTheme;

import javax.swing.*;
import java.awt.*;

/** Окно со списком всех достижений: полученные подсвечены, остальные показаны приглушённо. */
public class AchievementDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final int ITEM_HEIGHT = 78;

    public AchievementDialog(Basic basic, JFrame owner, GameProgress progress) {
        super(owner, Text.get("achievements.title"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Theme theme = basic.getActiveTheme();

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(theme.getBackgroundTop());
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel summary = new JLabel(Text.format("achievements.progress",
                progress.getUnlockedAchievements().size(), Achievement.values().length));
        summary.setFont(ClickerTheme.BALANCE_FONT);
        summary.setForeground(theme.getText());
        summary.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (Achievement achievement : Achievement.values()) {
            list.add(createItem(basic, progress, achievement));
            list.add(Box.createVerticalStrut(ClickerTheme.BUTTON_GAP));
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        JButton closeButton = ClickerTheme.createButton(basic, Text.get("achievements.close"));
        closeButton.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.add(closeButton);

        content.add(summary, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        setContentPane(content);

        setSize(640, 620);
        setLocationRelativeTo(owner);
    }

    private static JButton createItem(Basic basic, GameProgress progress, Achievement achievement) {
        boolean unlocked = progress.isAchievementUnlocked(achievement);

        JButton item = basic.createButton("");
        item.setFont(ClickerTheme.UPGRADE_FONT);
        item.setHorizontalAlignment(SwingConstants.LEFT);
        item.setPreferredSize(new Dimension(540, ITEM_HEIGHT));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, ITEM_HEIGHT));
        item.setFocusable(false);
        item.setText("<html><b>" + (unlocked ? "\u2713 " : "\u00b7 ") + achievement.getTitle()
                + "</b><br>" + achievement.getDescription() + "</html>");

        if (item instanceof Basic.RoundedButton rounded) {
            rounded.setHighlighted(unlocked);
        }
        if (!unlocked) {
            Color text = basic.getActiveTheme().getText();
            item.setForeground(new Color(text.getRed(), text.getGreen(), text.getBlue(), 130));
        }
        return item;
    }
}
