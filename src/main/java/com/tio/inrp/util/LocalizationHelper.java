package com.tio.inrp.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tio.inrp.InRP;
import com.tio.inrp.config.InRPConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class LocalizationHelper {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static final Map<String, String> EN_US_TRANSLATIONS = new HashMap<>();
    private static final Map<String, String> ACTIVE_TRANSLATIONS = new HashMap<>();

    static {
        loadTranslations("en_us", EN_US_TRANSLATIONS);
        reloadTranslations();
    }

    public static void reloadTranslations() {
        ACTIVE_TRANSLATIONS.clear();
        String lang = InRPConfig.SERVER_LANGUAGE != null ? InRPConfig.SERVER_LANGUAGE.get() : "en_us";
        loadTranslations(lang, ACTIVE_TRANSLATIONS);
    }

    private static void loadTranslations(String lang, Map<String, String> targetMap) {
        String path = "/assets/" + InRP.MODID + "/lang/" + lang + ".json";
        try (InputStream in = LocalizationHelper.class.getResourceAsStream(path)) {
            if (in != null) {
                try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    Map<String, String> map = GSON.fromJson(reader, MAP_TYPE);
                    if (map != null) {
                        targetMap.putAll(map);
                    }
                }
            } else {
                InRP.LOGGER.warn("Could not find translation file at: {}", path);
            }
        } catch (Exception e) {
            InRP.LOGGER.error("Failed to load translation file: {}", path, e);
        }
    }

    public static String getRaw(String key) {
        if (ACTIVE_TRANSLATIONS.containsKey(key)) {
            return ACTIVE_TRANSLATIONS.get(key);
        }
        if (EN_US_TRANSLATIONS.containsKey(key)) {
            return EN_US_TRANSLATIONS.get(key);
        }
        return key;
    }

    public static String format(String key, Object... args) {
        String raw = getRaw(key);
        try {
            return String.format(raw, args);
        } catch (Exception e) {
            return raw;
        }
    }

    public static MutableComponent getMessage(String key, Object... args) {
        String raw = getRaw(key);
        return Component.translatableWithFallback(key, raw, args);
    }

    public static MutableComponent getPrefixedMessage(String key, Object... args) {
        return Component.translatableWithFallback("inrp.prefix", getRaw("inrp.prefix"))
                .append(getMessage(key, args));
    }
}
