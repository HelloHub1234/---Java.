package WindowApps.clicker;

import WindowApps.Basic;
import WindowApps.Text;
import WindowApps.Theme;

import javax.swing.*;
import java.awt.*;

/** Окно настроек: выбор языка интерфейса и темы оформления. */
public class SettingsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final Basic basic;

    public SettingsDialog(Basic basic, JFrame owner) {
        super(owner, Text.get("settings.title"), true);
        this.basic = basic;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(basic.getActiveTheme().getBackgroundTop());
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel body = new JPanel(new GridLayout(2, 1, 0, 16));
        body.setOpaque(false);
        body.add(makeLanguageRow());
        body.add(makeThemeRow());

        JButton closeButton = ClickerTheme.createButton(basic, Text.get("settings.close"));
        closeButton.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setOpaque(false);
        footer.add(closeButton);

        content.add(body, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        setContentPane(content);

        setSize(520, 320);
        setLocationRelativeTo(owner);
    }

    private JComponent makeLanguageRow() {
        JComboBox<String> languages = new JComboBox<>(Text.supportedLanguages().toArray(new String[0]));
        languages.setSelectedItem(Text.getLanguage());
        languages.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value != null) {
                    setText(Text.displayName(value.toString()));
                }
                return this;
            }
        });
        languages.addActionListener(e -> {
            Object selected = languages.getSelectedItem();
            if (selected == null || selected.equals(Text.getLanguage())) {
                return;
            }
            Text.setLanguage(selected.toString());
            // строки уже созданных компонентов не переводятся автоматически — пересобираем окно
            dispose();
            basic.restart();
        });
        return makeRow(Text.get("settings.tab.language"), languages);
    }

    private JComponent makeThemeRow() {
        JComboBox<Theme> themes = new JComboBox<>(Theme.values());
        themes.setSelectedItem(basic.getActiveTheme());
        themes.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof Theme theme) {
                    setText(theme.getDisplayName());
                }
                return this;
            }
        });
        themes.addActionListener(e -> {
            if (themes.getSelectedItem() instanceof Theme theme) {
                basic.setTheme(theme);
            }
        });
        return makeRow(Text.get("settings.tab.theme"), themes);
    }

    private JComponent makeRow(String title, JComponent control) {
        JPanel row = new JPanel(new BorderLayout(0, 8));
        row.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setForeground(basic.getActiveTheme().getText());
        label.setFont(ClickerTheme.UPGRADE_FONT);

        row.add(label, BorderLayout.NORTH);
        row.add(control, BorderLayout.CENTER);
        return row;
    }
}
