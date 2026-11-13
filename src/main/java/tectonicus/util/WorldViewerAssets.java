/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */

package tectonicus.util;

import lombok.extern.slf4j.Slf4j;
import tectonicus.raw.ArmorTrimTag;
import tectonicus.raw.EnchantmentTag;
import tectonicus.texture.ZipStack;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Browser assets referenced by the markers exported across one world's dimensions. */
@Slf4j
public final class WorldViewerAssets {
	private static final String ENGLISH_TABLE = "assets/minecraft/lang/en_us.json";
	private static final Pattern CUSTOM_NAME = Pattern.compile("\\{(?:text:[^,)]*)?,?(?:translate:(?<key>[^,)]*))?,?(?:color:[^)]*)?}");
	private static final Pattern POTION_DURATION = Pattern.compile("^(strong|long)_");
	private static final Pattern MASK_NAME = Pattern.compile("[a-z0-9_.-]+(?:/[a-z0-9_.-]+)*");
	private static final Set<String> LEATHER_OVERLAYS = Set.of("leather_boots", "leather_chestplate", "leather_helmet", "leather_leggings");

	private final Set<String> translationKeys = new TreeSet<>(Set.of("attribute.name.armor", "attribute.name.generic.armor",
			"container.inventory", "container.enderchest", "item.dyed", "item.minecraft.smithing_template.upgrade"));
	private final Set<String> itemMasks = new TreeSet<>();

	void addItem(String id, String customName, boolean dyed, String potion, ArmorTrimTag trim, List<EnchantmentTag> enchantments) {
		String[] item = id.split(":", -1);
		if (item.length < 2) return;
		String name = item[1];
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
		if (parts.length > 1) translationKeys.add(category + "." + parts[0] + "." + parts[1]);
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

	void write(ZipStack assets, String worldId, Path scriptsDir, Path imagesDir, boolean hasFont) {
		Map<String, String> masks = new TreeMap<>();
		boolean fontAvailable = hasFont && Files.isRegularFile(imagesDir.resolve("Font.png"));
		if (fontAvailable) addMask(masks, imagesDir, "Font.png");
		for (String filename : itemMasks) addMask(masks, imagesDir, filename);
		writeScript(scriptsDir.resolve("masks.js"), "registerWorldMasks", worldId, masks);
		writeScript(scriptsDir.resolve("localizations.js"), "registerWorldLocalizations", worldId,
				Map.of("translations", readTranslations(assets), "hasFont", fontAvailable));
	}

	private Map<String, String> readTranslations(ZipStack assets) {
		Map<String, String> translations = new TreeMap<>();
		try (var stream = assets.getBaseStream(ENGLISH_TABLE)) {
			if (stream == null) return translations;
			var table = FileUtils.getOBJECT_MAPPER().reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(stream);
			if (!table.isObject()) {
				log.warn("Expected an object in {} in {}", ENGLISH_TABLE, assets.getBaseFileName());
				return translations;
			}
			for (String key : translationKeys) {
				var value = table.get(key);
				if (value != null && value.isString()) translations.put(key, value.asString());
			}
		} catch (IOException | JacksonException e) {
			log.warn("Unable to read {} in {}", ENGLISH_TABLE, assets.getBaseFileName(), e);
		}
		return translations;
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
