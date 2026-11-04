package WindowApps;

import java.util.IllegalFormatException;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Text {

    private static final List<String> SUPPORTED = List.of("ru", "en", "de", "es", "fr");
    private static final String BUNDLE = "WindowApps.messages";

    private static String language = fromSystemLocale();
    private static ResourceBundle bundle = load(language);

    private Text() {
    }

    public static List<String> supportedLanguages() {
        return SUPPORTED;
    }

    public static String getLanguage() {
        return language;
    }

    public static void setLanguage(String code) {
        if (code == null || !SUPPORTED.contains(code) || code.equals(language)) {
            return;
        }
        language = code;
        bundle = load(code);
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    public static String format(String key, Object... args) {
        // get() возвращает сам ключ, если строки нет — String.format на нём падает, поэтому проверяем заранее
        String pattern = get(key);
        if (pattern.equals(key)) {
            return key;
        }
        try {
            return String.format(pattern, args);
        } catch (IllegalFormatException e) {
            return pattern;
        }
    }

    public static String displayName(String code) {
        Locale locale = Locale.forLanguageTag(code);
        String name = locale.getDisplayLanguage(locale);
        return name.isEmpty() ? code : name;
    }

    private static String fromSystemLocale() {
        String tag = Locale.getDefault().getLanguage();
        return SUPPORTED.contains(tag) ? tag : "en";
    }

    private static ResourceBundle load(String code) {
        try {
            return ResourceBundle.getBundle(BUNDLE, Locale.forLanguageTag(code));
        } catch (MissingResourceException e) {
            // нет ни одного messages*.properties — UI покажет ключи вместо падения на старте
            return new EmptyBundle();
        }
    }

    private static final class EmptyBundle extends ListResourceBundle {

        @Override
        protected Object[][] getContents() {
            return new Object[0][];
        }
    }
}
