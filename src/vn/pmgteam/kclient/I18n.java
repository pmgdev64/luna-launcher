package vn.pmgteam.kclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class I18n {
    private static Map<String, String> translations = new HashMap<>();

    public static void load(Class<?> clazz, String baseName, Locale locale) {
        translations.clear();
        String filePath = "/resources/lang/" + baseName + "_" + locale.getLanguage() + ".lang";
        try (var in = clazz.getResourceAsStream(filePath)) {
            if (in == null) throw new RuntimeException("Missing lang file: " + filePath);
            try (var br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        translations.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load lang file: " + filePath, e);
        }
    }

    /** Lấy text đơn giản theo key */
    public static String get(String key) {
        return translations.getOrDefault(key, "??" + key + "??");
    }

    /** Lấy text và format với tham số */
    public static String get(String key, Object... args) {
        String pattern = get(key);
        return MessageFormat.format(pattern, args);
    }
}
