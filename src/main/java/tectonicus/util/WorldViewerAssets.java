/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */

package tectonicus.util;

import lombok.extern.slf4j.Slf4j;
import tectonicus.blockTypes.Banner;
import tectonicus.raw.ArmorTrimTag;
import tectonicus.raw.BannerPatternsTag;
import tectonicus.raw.EnchantmentTag;
import tectonicus.raw.Item;
import tectonicus.texture.ZipStack;
import tectonicus.world.Colors;
import tools.jackson.core.JacksonException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/** Browser assets referenced by the markers exported across one world's dimensions. */
@Slf4j
public final class WorldViewerAssets {
	private static final List<String> ENGLISH_TABLES = MapLocalization.languagePaths("en_us");
	private static final Pattern CUSTOM_NAME = Pattern.compile("\\{(?:text:[^,)]*)?,?(?:translate:(?<key>[^,)]*))?,?(?:color:[^)]*)?}");
	private static final Pattern POTION_DURATION = Pattern.compile("^(strong|long)_");
	private static final Pattern MASK_NAME = Pattern.compile("[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*");
	private static final Set<String> LEATHER_OVERLAYS = Set.of("leather_boots", "leather_chestplate", "leather_helmet", "leather_leggings");

	private final Set<String> translationKeys = new TreeSet<>(Set.of("attribute.name.armor", "attribute.name.generic.armor",
			"container.inventory", "container.enderchest", "item.dyed", "item.minecraft.smithing_template.upgrade"));
	private final Set<String> itemMasks = new TreeSet<>();
	private final Set<Integer> inventoryCharacters = new TreeSet<>();
	private final Map<BannerIcon, String> bannerIcons = new LinkedHashMap<>();

	record BannerIcon(String itemId, List<Banner.Pattern> patterns) {
		String filename() {
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] design = FileUtils.getOBJECT_MAPPER().writeValueAsBytes(List.of(itemId, patterns));
				return "banner_" + HexFormat.of().formatHex(digest.digest(design));
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	String addBannerIcon(Item item) {
		String prefix = "minecraft:";
		String suffix = "_banner";
		if (!item.id.startsWith(prefix) || !item.id.endsWith(suffix)
				|| Colors.byName(item.id.substring(prefix.length(), item.id.length() - suffix.length())) == null)
			return null;

		BannerPatternsTag patterns = item.getComponent(BannerPatternsTag.class);
		if (patterns == null || patterns.patterns().isEmpty())
			return null;

		return bannerIcons.computeIfAbsent(new BannerIcon(item.id, patterns.patterns()), BannerIcon::filename);
	}

	Map<BannerIcon, String> bannerIcons() {
		return bannerIcons;
	}

	void addItem(String id, String customName, boolean dyed, String potion, ArmorTrimTag trim, List<EnchantmentTag> enchantments) {
		String[] item = id.split(":", -1);
		if (item.length < 2) return;
		String name = item[1];
		addInventoryText(name);
		String itemKey = "item." + item[0] + "." + name;
		translationKeys.add(itemKey + ".desc");
		translationKeys.add("block." + item[0] + "." + name);
		if (potion == null) {
			translationKeys.add(itemKey);
		} else {
			String[] potionId = potion.split(":", -1);
			if (potionId.length > 1)
				translationKeys.add(itemKey + ".effect." + POTION_DURATION.matcher(potionId[1]).replaceFirst(""));
		}
		if (customName != null) {
			addInventoryText(customName);
			var match = CUSTOM_NAME.matcher(customName);
			if (match.find()) {
				String key = match.group("key");
				if (key != null) translationKeys.add(key);
			} else {
				translationKeys.add(customName);
			}
		}
		String icon = name;
		if (trim != null) {
			addNamespacedKey("trim_pattern", trim.pattern);
			addNamespacedKey("trim_material", trim.material);
			String[] material = trim.material.split(":", -1);
			icon += "_" + (material.length > 1 ? material[1] : "undefined") + "_trim";
		}
		if (dyed || potion != null || name.contains("leather_"))
			addItemMask(name);
		if (enchantments != null) {
			for (var enchantment : enchantments) addNamespacedKey("enchantment", enchantment.id);
			if (LEATHER_OVERLAYS.contains(name) || name.contains("potion") || name.equals("tipped_arrow"))
				addItemMask(icon + "_overlay");
		}
		if (enchantments != null || name.equals("enchanted_golden_apple"))
			addItemMask(icon);
	}

	private void addNamespacedKey(String category, String id) {
		String[] parts = id.split(":", -1);
		if (parts.length > 1) {
			translationKeys.add(category + "." + parts[0] + "." + parts[1]);
			addInventoryText(parts[1]);
		}
	}

	void addInventoryText(String text) {
		if (text != null) Normalizer.normalize(text, Normalizer.Form.NFC).codePoints()
				.filter(code -> !Character.isISOControl(code) && Character.getType(code) != Character.FORMAT)
				.forEach(inventoryCharacters::add);
	}

	private void addItemMask(String name) {
		if (!MASK_NAME.matcher(name).matches()) return;
		String filename = name + ".png";
		if (Arrays.stream(filename.split("/")).noneMatch(part -> part.equals(".") || part.equals("..")))
			itemMasks.add("Items/" + filename);
	}

	void addBeaconEffect(String effect) {
		if (!effect.equals("none")) translationKeys.add("effect.minecraft." + effect);
	}

	void write(ZipStack assets, File resourcePack, MapLocalization localization, String worldId, Path scriptsDir, Path imagesDir, boolean hasFont) {
		Map<String, String> translations = readTranslations(assets, resourcePack, localization, worldId);
		for (int code = 32; code <= 126; code++) inventoryCharacters.add(code);
		// Counts can use the map locale's numbering system.
		addInventoryText(java.text.NumberFormat.getIntegerInstance(localization.locale()).format(1023456789));
		MapLocalization.english().values().forEach(this::addInventoryText);
		translations.values().forEach(this::addInventoryText);
		Map<Integer, double[]> inventoryFont = InventoryFont.write(assets, localization, inventoryCharacters, imagesDir.resolve(InventoryFont.IMAGE));
		Map<String, String> masks = new TreeMap<>();
		boolean fontAvailable = hasFont && Files.isRegularFile(imagesDir.resolve("Font.png"));
		if (fontAvailable) addMask(masks, imagesDir, "Font.png");
		if (!inventoryFont.isEmpty()) addMask(masks, imagesDir, InventoryFont.IMAGE);
		for (String filename : itemMasks) addMask(masks, imagesDir, filename);
		writeScript(scriptsDir.resolve("masks.js"), "registerWorldMasks", worldId, masks);
		writeScript(scriptsDir.resolve("localizations.js"), "registerWorldLocalizations", worldId,
				Map.of("translations", translations, "hasFont", fontAvailable, "inventoryFont", inventoryFont));
	}

	private Map<String, String> readTranslations(ZipStack assets, File resourcePack, MapLocalization localization, String worldId) {
		Map<String, String> translations = new TreeMap<>();
		translationKeys.addAll(MapLocalization.english().keySet());
		try {
			addTranslations(translations, MapLocalization.readMinecraftTable(ENGLISH_TABLES, assets::getBaseStream));
		} catch (IOException | JacksonException e) {
			log.warn("Unable to read US English translations in {}", assets.getBaseFileName(), e);
		}
		List<String> paths = localization.resourcePaths();
		Map<String, String> packTranslations = Map.of();
		try {
			if (resourcePack != null && !resourcePack.getPath().equals(assets.getOverrideFileName())) {
				// Programmer Art may have replaced the configured pack for rendering.
				try (var zip = new ZipFile(resourcePack)) {
					packTranslations = MapLocalization.readMinecraftTable(paths, path -> {
						var entry = zip.getEntry(path);
						return entry == null ? null : zip.getInputStream(entry);
					});
				}
			} else {
				packTranslations = MapLocalization.readMinecraftTable(paths, assets::getOverrideStream);
			}
		} catch (IOException | JacksonException e) {
			log.warn("Unable to read resource pack translations at {}", paths, e);
		}
		Map<String, String> minecraftTranslations = localization.minecraftTranslations(assets);
		addTranslations(translations, packTranslations);
		addTranslations(translations, localization.overrides());
		addTranslations(translations, minecraftTranslations);
		localization.logLanguage(assets, worldId, minecraftTranslations, packTranslations);
		return translations;
	}

	private void addTranslations(Map<String, String> translations, Map<String, String> source) {
		for (String key : translationKeys) {
			String value = source.get(key);
			if (value != null && !value.isEmpty()) translations.put(key, value);
		}
	}

	private static void addMask(Map<String, String> masks, Path imagesDir, String filename) {
		Path image = imagesDir.resolve(filename);
		if (!Files.isRegularFile(image)) return;
		try {
			masks.put(filename, "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(image)));
		} catch (IOException e) {
			log.warn("Unable to read image mask {}", image, e);
		}
	}

	private static void writeScript(Path file, String register, String worldId, Map<String, ?> table) {
		var mapper = FileUtils.getOBJECT_MAPPER();
		try {
			String json = mapper.writeValueAsString(table);
			// Parsing a JSON string also preserves special property names such as __proto__.
			String script = register + "(" + mapper.writeValueAsString(worldId) + ",JSON.parse(" + mapper.writeValueAsString(json) + "));\n";
			Files.writeString(file, script);
		} catch (IOException | JacksonException e) {
			log.error("Unable to write viewer asset {}", file, e);
		}
	}
}
