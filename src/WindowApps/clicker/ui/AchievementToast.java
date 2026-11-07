package WindowApps.clicker.ui;

import WindowApps.Basic;
import WindowApps.Text;
import WindowApps.Theme;
import WindowApps.clicker.model.Achievement;
import WindowApps.clicker.ui.theme.ClickerTheme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Всплывающее уведомление о новом достижении в правом нижнем углу окна.
 * Сделано отдельным JWindow, а не JOptionPane: модальный диалог останавливал бы игру,
 * а уведомление должно лишь сообщить о награде и само исчезнуть.
 */
public final class AchievementToast {

    private static final int SHOW_MS = 4000;
    private static final int MARGIN = 28;
    private static final Deque<Achievement> QUEUE = new ArrayDeque<>();

    private static JWindow current;

    private AchievementToast() {
    }

    /** Ставит уведомление в очередь: несколько достижений подряд показываются по одному. */
    public static void show(JFrame owner, Basic basic, Achievement achievement) {
        QUEUE.addLast(achievement);
        if (current == null) {
            showNext(owner, basic);
        }
    }

    private static void showNext(JFrame owner, Basic basic) {
        Achievement achievement = QUEUE.pollFirst();
        if (achievement == null) {
            current = null;
            return;
        }

        JWindow window = new JWindow(owner);
        window.setContentPane(createContent(basic.getActiveTheme(), achievement));
        window.pack();
        position(window, owner);
        window.setAlwaysOnTop(true);
        window.setVisible(true);
        current = window;

        Timer timer = new Timer(SHOW_MS, e -> {
            window.dispose();
            current = null;
            showNext(owner, basic);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static JComponent createContent(Theme theme, Achievement achievement) {
        JPanel card = new JPanel(new BorderLayout(14, 0));
        card.setBackground(theme.getBackgroundTop());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getAccent(), 2, true),
                BorderFactory.createEmptyBorder(14, 18, 14, 22)));

        JLabel icon = new JLabel("\u2605");
        icon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        icon.setForeground(theme.getAccent());
        icon.setVerticalAlignment(SwingConstants.CENTER);

        Color text = theme.getText();
        JLabel caption = new JLabel(Text.get("achievement.unlocked.caption"));
        caption.setFont(ClickerTheme.UPGRADE_FONT);
        caption.setForeground(new Color(text.getRed(), text.getGreen(), text.getBlue(), 190));

        JLabel title = new JLabel(achievement.getTitle());
        title.setFont(ClickerTheme.BALANCE_FONT);
        title.setForeground(text);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(caption);
        texts.add(Box.createVerticalStrut(4));
        texts.add(title);

        card.add(icon, BorderLayout.WEST);
        card.add(texts, BorderLayout.CENTER);
        return card;
    }

    private static void position(JWindow window, JFrame owner) {
        Rectangle area;
        if (owner != null && owner.isShowing()) {
            area = owner.getBounds();
        } else {
            area = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        }
        window.setLocation(area.x + area.width - window.getWidth() - MARGIN,
                area.y + area.height - window.getHeight() - MARGIN);
    }
}
