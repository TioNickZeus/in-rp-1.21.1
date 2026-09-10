package com.tio.inrp.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tio.inrp.InRP;
import com.tio.inrp.config.InRPConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Server-side translation lookup.
 *
 * <p>The mod is server-only, so vanilla clients never receive the bundled language files. Messages are therefore
 * sent as {@link Component#translatableWithFallback} components: a modded client translates them locally, while a
 * vanilla client renders the server-side fallback string resolved here.
 *
 * <p>Lookup chain: configured language &rarr; {@value #FALLBACK_LANGUAGE} &rarr; the literal key.
 */
public final class LocalizationHelper {

    /** Language that ships with the mod and always acts as the last resort before the raw key. */
    public static final String FALLBACK_LANGUAGE = "en_us";

    /**
     * The configured language code is interpolated into a classpath resource path, so only characters that cannot
     * escape {@code assets/inrp/lang/} are accepted.
     */
    private static final Pattern SAFE_LANGUAGE_CODE = Pattern.compile("[a-z0-9_-]{2,32}");

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static final Map<String, String> FALLBACK_TRANSLATIONS = loadTranslations(FALLBACK_LANGUAGE);

    /**
     * Replaced wholesale on reload instead of being mutated in place, so readers on the server thread never observe
     * a half-populated map while a config reload is running.
     */
    private static volatile Map<String, String> activeTranslations = FALLBACK_TRANSLATIONS;

    private LocalizationHelper() {
    }

    /** Re-reads the language selected by {@code general.serverLanguage}. Safe to call before the config is loaded. */
    public static void reloadTranslations() {
        String language = resolveConfiguredLanguage();
        activeTranslations = FALLBACK_LANGUAGE.equals(language)
                ? FALLBACK_TRANSLATIONS
                : loadTranslations(language);
    }

    /** @return the translated string for {@code key}, or the key itself when no language defines it. */
    public static String getRaw(String key) {
        if (key == null) {
            return "";
        }
        String value = activeTranslations.get(key);
        if (value == null) {
            value = FALLBACK_TRANSLATIONS.get(key);
        }
        return value != null ? value : key;
    }

    /** @return the translated string with {@code args} substituted, or the unformatted string on a bad format. */
    public static String format(String key, Object... args) {
        String raw = getRaw(key);
        if (args == null || args.length == 0) {
            return raw;
        }
        try {
            return String.format(raw, args);
        } catch (RuntimeException e) {
            InRP.LOGGER.warn("Translation key '{}' does not accept {} argument(s)", key, args.length);
            return raw;
        }
    }

    /** @return a component that modded clients translate themselves and vanilla clients render from the fallback. */
    public static MutableComponent getMessage(String key, Object... args) {
        return Component.translatableWithFallback(key, getRaw(key), args);
    }

    /** @return {@link #getMessage} prefixed with the branded {@code [In-RP]} tag. */
    public static MutableComponent getPrefixedMessage(String key, Object... args) {
        return Component.translatableWithFallback("inrp.prefix", getRaw("inrp.prefix"))
                .append(getMessage(key, args));
    }

    private static String resolveConfiguredLanguage() {
        String configured;
        try {
            configured = InRPConfig.SERVER_LANGUAGE.get();
        } catch (RuntimeException e) {
            // Translations can be requested before the server config is attached (e.g. during mod construction).
            return FALLBACK_LANGUAGE;
        }
        if (configured == null || configured.isBlank()) {
            return FALLBACK_LANGUAGE;
        }

        String language = configured.strip().toLowerCase(Locale.ROOT);
        if (!SAFE_LANGUAGE_CODE.matcher(language).matches()) {
            InRP.LOGGER.warn("Ignoring invalid serverLanguage '{}', falling back to '{}'", configured, FALLBACK_LANGUAGE);
            return FALLBACK_LANGUAGE;
        }
        return language;
    }

    private static Map<String, String> loadTranslations(String language) {
        String path = "/assets/" + InRP.MODID + "/lang/" + language + ".json";
        try (InputStream in = LocalizationHelper.class.getResourceAsStream(path)) {
            if (in == null) {
                InRP.LOGGER.warn("Could not find translation file at: {}", path);
                return Map.of();
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Map<String, String> loaded = GSON.fromJson(reader, MAP_TYPE);
                return loaded == null || loaded.isEmpty() ? Map.of() : Map.copyOf(loaded);
            }
        } catch (Exception e) {
            InRP.LOGGER.error("Failed to load translation file: {}", path, e);
            return Map.of();
        }
    }
}
