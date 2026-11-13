/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */

package tectonicus.util;

import lombok.extern.slf4j.Slf4j;
import tectonicus.configuration.Configuration;
import tectonicus.texture.ZipStack;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public final class MapLocalization {
	private static final Map<String, String> ENGLISH = loadEnglish();
	private final Locale locale;
	private final String localeCode;
	private final Map<String, String> overrides;
	private final MinecraftLanguageAssets languageAssets;
	private final Map<String, Map<String, String>> catalogs = new HashMap<>();

	private MapLocalization(Locale locale, String localeCode, Map<String, String> overrides, MinecraftLanguageAssets languageAssets) {
		this.locale = locale;
		this.localeCode = localeCode;
		this.overrides = overrides;
		this.languageAssets = languageAssets;
	}

	public Locale locale() {
		return locale;
	}

	Map<String, String> overrides() {
		return overrides;
	}

	Map<String, String> minecraftTranslations(ZipStack assets) {
		return languageAssets.load(assets, localeCode);
	}

	byte[] minecraftFontAsset(ZipStack assets, String key) {
		return languageAssets.readAsset(assets, key);
	}

	public Map<String, String> languageNames(ZipStack assets) {
		return catalogs.computeIfAbsent(assets.getBaseFileName(), jar -> {
			var names = new TreeMap<String, String>();
			try (var metadata = assets.getBaseStream("pack.mcmeta")) {
				if (metadata != null) addLanguageNames(names, FileUtils.getOBJECT_MAPPER().readTree(metadata));
				if (names.isEmpty()) {
					byte[] catalog = languageAssets.readAsset(assets, "pack.mcmeta");
					if (catalog.length > 0) addLanguageNames(names, FileUtils.getOBJECT_MAPPER().readTree(catalog));
				}
			} catch (IOException | JacksonException e) {
				log.warn("Unable to read language names for {}: {}", jar, e.getMessage());
			}
			return Collections.unmodifiableMap(names);
		});
	}

	private static void addLanguageNames(Map<String, String> names, JsonNode metadata) {
		for (var entry : metadata.path("language").properties()) {
			var value = entry.getValue();
			String name = value.path("name").asString();
			if (!name.isBlank()) names.put(entry.getKey(), displayName(name, value.path("region").asString()));
		}
	}

	private static String displayName(String name, String region) {
		return region.isBlank() ? name : name + " (" + region + ")";
	}

	void logLanguage(ZipStack assets, String worldId, Map<String, String> minecraftTranslations, Map<String, String> packTranslations) {
		String code = localeCode;
		if (code.equals("en_us")) {
			log.info("Map language for {}: English (US) [{}]", worldId, code);
			return;
		}
		Map<String, String> names = languageNames(assets);
		String name = names.get(code);
		if (name == null) {
			for (var table : List.of(minecraftTranslations, overrides, packTranslations)) {
				String languageName = table.get("language.name");
				if (languageName != null && !languageName.isBlank()) {
					name = displayName(languageName, table.getOrDefault("language.region", ""));
					break;
				}
			}
		}
		if (!names.isEmpty() && !names.containsKey(code) && minecraftTranslations.isEmpty() && overrides.isEmpty() && packTranslations.isEmpty())
			log.warn("Unknown map locale '{}' for {}. Run --listLanguages to see the Minecraft language codes. Using English fallbacks.", code, worldId);
		log.info("Map language for {}: {} [{}]", worldId, name == null ? locale.getDisplayName(locale) : name, code);
	}

	public static MapLocalization load(Configuration config) throws IOException {
		return load(config, new MinecraftLanguageAssets());
	}

	static MapLocalization load(Configuration config, MinecraftLanguageAssets languageAssets) throws IOException {
		Locale locale = parseLocale(config.getMapLocale());
		// Minecraft's catalog keys can differ from the normalized Java locale.
		String localeCode = config.getMapLocale().strip().replace('-', '_').toLowerCase(Locale.ROOT);
		var file = config.getMapLocaleFile();
		if (file == null) return new MapLocalization(locale, localeCode, Map.of(), languageAssets);
		try (var stream = Files.newInputStream(file.toPath())) {
			return new MapLocalization(locale, localeCode, Map.copyOf(readTable(stream, true)), languageAssets);
		} catch (IOException | JacksonException e) {
			throw new IOException("Invalid mapLocaleFile " + file + ": " + e.getMessage(), e);
		}
	}

	public static Locale parseLocale(String value) {
		try {
			Locale locale = new Locale.Builder().setLanguageTag(value.strip().replace('_', '-')).build();
			if (!locale.getLanguage().isEmpty()) return locale;
		} catch (IllformedLocaleException ignored) {
			// Report the configuration option with its invalid value.
		}
		throw new IllegalArgumentException("Invalid mapLocale: " + value);
	}

	List<String> resourcePaths() {
		return languagePaths(localeCode);
	}

	static List<String> languagePaths(String code) {
		String path = "assets/minecraft/lang/" + code;
		return List.of(path + ".json", path + ".lang");
	}

	static Map<String, String> readMinecraftTable(List<String> paths, LanguageSource source) throws IOException {
		for (String path : paths) {
			try (var stream = source.open(path)) {
				if (stream != null) return readMinecraftTable(stream, path);
			}
		}
		return Map.of();
	}

	static Map<String, String> readMinecraftTable(InputStream stream, String path) throws IOException {
		if (path.endsWith(".json")) return readTable(stream, false);
		var translations = new HashMap<String, String>();
		var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
		for (String line; (line = reader.readLine()) != null; ) {
			if (line.startsWith("#")) continue;
			int separator = line.indexOf('=');
			if (separator > 0) translations.put(line.substring(0, separator), line.substring(separator + 1));
		}
		return translations;
	}

	@FunctionalInterface
	interface LanguageSource {
		InputStream open(String path) throws IOException;
	}

	static Map<String, String> english() {
		return ENGLISH;
	}

	static Map<String, String> readTable(InputStream stream, boolean strict) throws IOException {
		var table = FileUtils.getOBJECT_MAPPER().reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(stream);
		if (!table.isObject()) throw new IOException("Expected a JSON object containing translations");
		Map<String, String> translations = new HashMap<>();
		for (var entry : table.properties()) {
			if (entry.getValue().isString()) translations.put(entry.getKey(), entry.getValue().asString());
			else if (strict) throw new IOException("Expected a string for translation key " + entry.getKey());
		}
		return translations;
	}

	private static Map<String, String> loadEnglish() {
		try (var stream = MapLocalization.class.getResourceAsStream("/map_en_us.json")) {
			return Map.copyOf(readTable(stream, true));
		} catch (IOException e) {
			throw new IllegalStateException("Unable to load the built-in map translations", e);
		}
	}
}
