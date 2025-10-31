package vn.pmgteam.luna;

import java.util.*;

public class I18n {
    private static Map<String, String> translations = new HashMap<>();
    private static Locale currentLocale = Locale.ENGLISH;
    private static String baseName = "messages";

    // Danh sách listener để refresh UI
    private static final List<Runnable> listeners = new ArrayList<>();

    public static void load(Class<?> clazz, String baseName, Locale locale) {
        I18n.baseName = baseName;
        currentLocale = locale;
        translations.clear();

        String filePath = "/resources/lang/" + baseName + "_" + locale.getLanguage() + ".lang";
        try (var in = clazz.getResourceAsStream(filePath)) {
            if (in == null) throw new RuntimeException("Missing lang file: " + filePath);
            try (var br = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) translations.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load lang file: " + filePath, e);
        }

        // Gọi tất cả listener để refresh UI
        for (Runnable r : listeners) r.run();
    }

    public static String get(String key) {
        return translations.getOrDefault(key, "??" + key + "??");
    }

    public static String get(String key, Object... args) {
        return java.text.MessageFormat.format(get(key), args);
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static void setLocale(Class<?> clazz, Locale newLocale) {
        load(clazz, baseName, newLocale);
    }

    // ===== Listener =====
    public static void addListener(Runnable r) {
        listeners.add(r);
    }

    public static void removeListener(Runnable r) {
        listeners.remove(r);
    }
}
