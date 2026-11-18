/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.extern.slf4j.Slf4j;
import tectonicus.util.FileUtils;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts item stacks saved before the 1.13 flattening to the item IDs used since 1.13, and describes how to find
 * names and icons for those items in a pre-1.13 Minecraft jar.
 * <p>
 * The tables in {@code legacyItems.json} follow the conversions Minecraft 1.13 applies when it upgrades an old world
 * (numeric IDs, damage-value variants, renamed items, old potion damage values, spawn egg entities and enchantment IDs).
 * Each 1.13 item ID also lists the 1.12 item model name, old language key, item textures and pre-1.5 item sheet slot.
 */
@Slf4j
public final class LegacyItems {
	/** Data version of the first snapshot (17w47a) that saved flattened item IDs. */
	public static final int FLATTENING_DATA_VERSION = 1451;
	private static final int SPLASH_POTION_FLAG = 16384;

	private final Map<Integer, String> numericIds = new HashMap<>();
	private final Map<String, String> preFlatteningRenames = new HashMap<>();
	private final Map<String, String> flattening = new HashMap<>();
	private final Set<String> flattenedIds = new HashSet<>();
	private final Map<String, String> postFlatteningRenames = new HashMap<>();
	private final String[] potions = new String[128];
	private final Map<Integer, String> spawnEggEntities = new HashMap<>();
	private final Map<String, String> entityRenames = new HashMap<>();
	private final Map<String, String> spawnEggs = new HashMap<>();
	private final Map<Integer, String> enchantments = new HashMap<>();
	private final Map<String, Entry> items = new HashMap<>();
	private final Map<String, String> translationAliases = new HashMap<>();

	/**
	 * How an item was saved before 1.13 and where to find its name and icon in an old jar.
	 *
	 * @param legacyId the string ID used by 1.8 to 1.12
	 * @param damage the damage value that selected this variant
	 * @param numericId the numeric ID used before 1.8, or -1
	 * @param model the 1.8 to 1.12 item model name, or null
	 * @param langKey the 1.12 language key, or null
	 * @param textures the item textures of a flat item model (for example "items/diamond_sword"), empty for 3D items
	 * @param itemSheetIndex the slot in the 1.4 and older gui/items.png sheet, or -1
	 */
	public record Entry(String legacyId, int damage, int numericId, String model, String langKey, List<String> textures,
			int itemSheetIndex) {
		public boolean isBlock() {
			return numericId >= 0 && numericId < 256;
		}
	}

	private static final class Holder {
		private static final LegacyItems INSTANCE = load();
	}

	public static LegacyItems get() {
		return Holder.INSTANCE;
	}

	private static LegacyItems load() {
		try (InputStream in = LegacyItems.class.getResourceAsStream("/legacyItems.json")) {
			if (in == null)
				throw new IOException("legacyItems.json is missing");
			return new LegacyItems(FileUtils.getOBJECT_MAPPER().readTree(in));
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to load legacy item tables", e);
		}
	}

	LegacyItems(JsonNode root) {
		root.path("numericIds").properties().forEach(e -> numericIds.put(Integer.parseInt(e.getKey()), e.getValue().asString()));
		readMap(root.path("preFlatteningRenames"), preFlatteningRenames);
		readMap(root.path("flattening"), flattening);
		for (String key : flattening.keySet())
			flattenedIds.add(key.substring(0, key.lastIndexOf('.')));
		readMap(root.path("postFlatteningRenames"), postFlatteningRenames);
		int i = 0;
		for (JsonNode potion : root.path("potions")) {
			if (i < potions.length)
				potions[i] = potion.isString() ? potion.asString() : null;
			i++;
		}
		root.path("spawnEggEntities").properties().forEach(e -> spawnEggEntities.put(Integer.parseInt(e.getKey()), e.getValue().asString()));
		readMap(root.path("entityRenames"), entityRenames);
		readMap(root.path("spawnEggs"), spawnEggs);
		root.path("enchantments").properties().forEach(e -> enchantments.put(Integer.parseInt(e.getKey()), e.getValue().asString()));
		root.path("items").properties().forEach(e -> {
			JsonNode item = e.getValue();
			List<String> textures = new ArrayList<>();
			for (JsonNode texture : item.path("textures"))
				textures.add(texture.asString());
			items.put(e.getKey(), new Entry(item.path("legacy").asString(), item.path("damage").asInt(0),
					item.path("numericId").asInt(-1), textOrNull(item.path("model")), textOrNull(item.path("lang")),
					Collections.unmodifiableList(textures), item.path("atlas").asInt(-1)));
		});
		readMap(root.path("translationAliases"), translationAliases);
	}

	private static void readMap(JsonNode node, Map<String, String> map) {
		node.properties().forEach(e -> map.put(e.getKey(), e.getValue().asString()));
	}

	private static String textOrNull(JsonNode node) {
		return node.isString() ? node.asString() : null;
	}

	public static boolean isLegacy(int dataVersion) {
		return dataVersion < FLATTENING_DATA_VERSION;
	}

	/** @return the 1.8 to 1.12 string ID for a numeric ID saved before 1.8, or null */
	public String fromNumericId(int id) {
		return numericIds.get(id);
	}

	/** Converts a 1.8 to 1.12 string ID and damage value to the 1.13 item ID. */
	public String toModernId(String legacyId, int damage) {
		String id = namespaced(legacyId);
		id = preFlatteningRenames.getOrDefault(id, id);
		if (flattenedIds.contains(id)) {
			String flattened = flattening.get(id + "." + damage);
			if (flattened == null)
				flattened = flattening.get(id + ".0");
			if (flattened != null)
				id = flattened;
		}
		return postFlatteningRenames.getOrDefault(id, id);
	}

	/** @return true if the damage value of this 1.8 to 1.12 item ID picks a variant rather than counting wear */
	public boolean usesDamageForVariant(String legacyId) {
		return flattenedIds.contains(preFlatteningRenames.getOrDefault(namespaced(legacyId), namespaced(legacyId)));
	}

	/** @return the potion ID stored in the damage value of a potion saved before 1.9 */
	public String potionFromDamage(int damage) {
		String potion = potions[damage & 127];
		return potion == null ? "minecraft:water" : potion;
	}

	public static boolean isSplashPotionDamage(int damage) {
		return (damage & SPLASH_POTION_FLAG) == SPLASH_POTION_FLAG;
	}

	/** @return the entity ID of a spawn egg saved before 1.9, where the damage value held the numeric entity ID */
	public String spawnEggEntity(int damage) {
		String entity = spawnEggEntities.get(damage);
		return entity == null ? null : modernEntityId(entity);
	}

	public String modernEntityId(String entityId) {
		String id = entityRenames.getOrDefault(entityId, entityId);
		return namespaced(id);
	}

	/** @return the 1.13 spawn egg item for an entity ID, or null */
	public String spawnEggItem(String entityId) {
		String entity = entityId;
		// Follow renames until an egg matches: "Pig" (1.10) to "minecraft:pig", or "minecraft:snowman" to "minecraft:snow_golem"
		for (int i = 0; entity != null && i < 4; i++) {
			String egg = spawnEggs.get(namespaced(entity));
			if (egg != null)
				return postFlatteningRenames.getOrDefault(egg, egg);
			String renamed = entityRenames.get(entity);
			if (renamed == null)
				renamed = entityRenames.get(namespaced(entity));
			entity = renamed;
		}
		return null;
	}

	/** @return the namespaced ID of an enchantment saved by its numeric ID before 1.13 */
	public String enchantment(int id) {
		return enchantments.get(id);
	}

	/** @return how a 1.13 item ID was saved before 1.13, or null if Tectonicus doesn't know it */
	public Entry item(String modernId) {
		return modernId == null ? null : items.get(namespaced(modernId));
	}

	/**
	 * @return the key used by pre-1.13 language files for a 1.13 translation key, such as
	 * {@code item.swordDiamond.name} for {@code item.minecraft.diamond_sword}, or null
	 */
	public String translationAlias(String key) {
		String alias = translationAliases.get(key);
		if (alias != null)
			return alias;
		for (String prefix : List.of("item.", "block.")) {
			if (key.startsWith(prefix)) {
				String[] parts = key.substring(prefix.length()).split("\\.");
				if (parts.length == 2) {
					Entry entry = items.get(parts[0] + ":" + parts[1]);
					return entry == null ? null : entry.langKey();
				}
			}
		}
		return null;
	}

	private static String namespaced(String id) {
		return id.indexOf(':') >= 0 ? id : "minecraft:" + id;
	}
}
