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
import tectonicus.raw.LegacyItems;
import tectonicus.texture.ResourcePack;
import tectonicus.texture.ZipStack;
import tectonicus.world.Colors;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Browser assets referenced by the markers exported across one world's dimensions. */
@Slf4j
public final class WorldViewerAssets {
	private static final List<String> ENGLISH_TABLES = MapLocalization.packLanguagePaths("en_us");
	private static final Pattern CUSTOM_NAME = Pattern.compile("\\{(?:text:[^,)]*)?,?(?:translate:(?<key>[^,)]*))?,?(?:color:[^)]*)?}");
	private static final Pattern POTION_DURATION = Pattern.compile("^(strong|long)_");
	private static final Pattern MASK_NAME = Pattern.compile("[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*");
	private static final Set<String> LEATHER_OVERLAYS = Set.of("leather_boots", "leather_chestplate", "leather_helmet", "leather_leggings");

	private final Set<String> translationKeys = new TreeSet<>(Set.of("attribute.name.armor", "attribute.name.generic.armor",
			"container.inventory", "container.enderchest", "item.dyed", "item.minecraft.smithing_template.upgrade"));
	private final Set<String> itemMasks = new TreeSet<>();
	private final Set<String> itemIds = new TreeSet<>();
	private final Set<SignBackground> signBackgrounds = new HashSet<>();
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

	/** @return the IDs of the items shown in exported inventories */
	Set<String> itemIds() {
		return itemIds;
	}

	void addItem(String id, String customName, boolean dyed, String potion, ArmorTrimTag trim, List<EnchantmentTag> enchantments) {
		String[] item = id.split(":", -1);
		if (item.length < 2) return;
		String name = item[1];
		itemIds.add(id);
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
		}
	}

	String addSignBackground(String blockName) {
		SignBackground background = SignBackground.forBlock(blockName);
		signBackgrounds.add(background);
		return background.filename();
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

	void write(ZipStack assets, MapLocalization localization, String worldId, Path scriptsDir, Path imagesDir,
			boolean usesBlockSignTextures) {
		Map<String, String> translations = readTranslations(assets, localization, worldId);
		Map<String, String> masks = new TreeMap<>();
		for (String filename : itemMasks) addMask(masks, imagesDir, filename);
		writeScript(scriptsDir.resolve("masks.js"), "registerWorldMasks", worldId, masks);
		Set<String> exportedSignBackgrounds = new TreeSet<>();
		for (SignBackground background : signBackgrounds) {
			if (background.write(assets, imagesDir, usesBlockSignTextures)) exportedSignBackgrounds.add(background.filename());
		}
		writeScript(scriptsDir.resolve("localizations.js"), "registerWorldLocalizations", worldId,
				Map.of("translations", translations, "signBackgrounds", exportedSignBackgrounds));
	}

	private Map<String, String> readTranslations(ZipStack assets, MapLocalization localization, String worldId) {
		Map<String, String> translations = new TreeMap<>();
		translationKeys.addAll(MapLocalization.english().keySet());
		try {
			addTranslations(translations, MapLocalization.readMinecraftTable(ENGLISH_TABLES, assets::getBaseStream));
		} catch (IOException | JacksonException e) {
			log.warn("Unable to read US English translations in {}", assets.getBaseFileName(), e);
		}
		List<String> paths = localization.resourcePaths();
		// Language files merge key by key: apply packs from lowest to highest priority so higher packs win.
		Map<String, String> packTranslations = new HashMap<>();
		for (ResourcePack pack : assets.getResourcePacks().reversed()) {
			try {
				packTranslations.putAll(MapLocalization.readMinecraftTable(paths,
						path -> assets.getLayers(path).contains(pack) ? pack.open(path) : null));
			} catch (IOException | JacksonException e) {
				log.warn("Unable to read resource pack translations at {} in {}", paths, pack, e);
			}
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
			if (value == null) {
				// Language files before 1.13 use different keys, for example item.swordDiamond.name.
				for (String legacyKey : LegacyItems.get().translationAliases(key)) {
					value = source.get(legacyKey);
					if (value != null) break;
				}
			}
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
