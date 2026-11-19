/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.LongTag;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import tectonicus.raw.ContainerKind;
import tectonicus.world.Colors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts Bedrock Edition item stacks into the Java Edition item NBT that Tectonicus reads: an id, a count, a slot
 * and 1.21.5+ components for custom names, enchantments, dye colours and maps.
 */
public final class BedrockItems {
	private static final Map<String, String> RENAMES = Map.ofEntries(
			Map.entry("minecraft:appleenchanted", "minecraft:enchanted_golden_apple"),
			Map.entry("minecraft:muttonraw", "minecraft:mutton"),
			Map.entry("minecraft:muttoncooked", "minecraft:cooked_mutton"),
			Map.entry("minecraft:fish", "minecraft:cod"),
			Map.entry("minecraft:cooked_fish", "minecraft:cooked_cod"),
			Map.entry("minecraft:clownfish", "minecraft:tropical_fish"),
			Map.entry("minecraft:fireworks", "minecraft:firework_rocket"),
			Map.entry("minecraft:fireworkscharge", "minecraft:firework_star"),
			Map.entry("minecraft:empty_map", "minecraft:map"),
			Map.entry("minecraft:emptymap", "minecraft:map"),
			Map.entry("minecraft:netherbrick", "minecraft:nether_brick"),
			Map.entry("minecraft:speckled_melon", "minecraft:glistering_melon_slice"),
			Map.entry("minecraft:melon", "minecraft:melon_slice"),
			Map.entry("minecraft:reeds", "minecraft:sugar_cane"),
			Map.entry("minecraft:horsearmorleather", "minecraft:leather_horse_armor"),
			Map.entry("minecraft:horsearmoriron", "minecraft:iron_horse_armor"),
			Map.entry("minecraft:horsearmorgold", "minecraft:golden_horse_armor"),
			Map.entry("minecraft:horsearmordiamond", "minecraft:diamond_horse_armor"),
			Map.entry("minecraft:carrotonastick", "minecraft:carrot_on_a_stick"),
			Map.entry("minecraft:turtle_shell_piece", "minecraft:turtle_scute"),
			Map.entry("minecraft:lodestonecompass", "minecraft:compass"),
			Map.entry("minecraft:lodestone_compass", "minecraft:compass"),
			Map.entry("minecraft:frame", "minecraft:item_frame"),
			Map.entry("minecraft:glow_frame", "minecraft:glow_item_frame"),
			Map.entry("minecraft:wooden_door", "minecraft:oak_door"),
			Map.entry("minecraft:sign", "minecraft:oak_sign"),
			Map.entry("minecraft:boat", "minecraft:oak_boat"),
			Map.entry("minecraft:chest_boat", "minecraft:oak_chest_boat"),
			Map.entry("minecraft:totem", "minecraft:totem_of_undying"),
			Map.entry("minecraft:scute", "minecraft:turtle_scute"),
			Map.entry("minecraft:redstone_wire", "minecraft:redstone"),
			Map.entry("minecraft:tripwire", "minecraft:string"),
			Map.entry("minecraft:spawn_egg", "minecraft:pig_spawn_egg"));
	/** Bedrock's numeric enchantment ids. */
	private static final String[] ENCHANTMENTS = {"protection", "fire_protection", "feather_falling", "blast_protection",
			"projectile_protection", "thorns", "respiration", "depth_strider", "aqua_affinity", "sharpness", "smite",
			"bane_of_arthropods", "knockback", "fire_aspect", "looting", "efficiency", "silk_touch", "unbreaking", "fortune",
			"power", "punch", "flame", "infinity", "luck_of_the_sea", "lure", "frost_walker", "mending", "binding_curse",
			"vanishing_curse", "impaling", "riptide", "loyalty", "channeling", "multishot", "piercing", "quick_charge",
			"soul_speed", "swift_sneak", "wind_burst", "density", "breach"};
	private static final String[] HEADS = {"skeleton_skull", "wither_skeleton_skull", "zombie_head", "player_head",
			"creeper_head", "dragon_head", "piglin_head"};

	private static final Map<Long, Integer> MAP_IDS = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> MAP_UUIDS = new ConcurrentHashMap<>();

	private BedrockItems() {}

	/**
	 * Converts an item stack, or returns null for an empty one.
	 *
	 * @param slot the inventory slot to record, or null for an item that is not in a slot
	 */
	public static CompoundTag toJava(CompoundTag item, Integer slot, BedrockBlockMapper mapper) {
		if (item == null)
			return null;
		String name = BedrockChunkReader.string(item, "Name", "").toLowerCase(Locale.ROOT);
		int count = number(item.getValue().get("Count"), 1);
		if (name.isEmpty() || name.equals("minecraft:air") || count <= 0)
			return null;
		if (name.indexOf(':') < 0)
			name = "minecraft:" + name;
		int damage = number(item.getValue().get("Damage"), 0);
		CompoundTag tag = BedrockChunkReader.compound(item, "tag");

		String id = javaId(name, damage, BedrockChunkReader.compound(item, "Block"), mapper);
		Map<String, Tag> java = new HashMap<>();
		java.put("id", new StringTag("id", id));
		java.put("count", new IntTag("count", count));
		if (slot != null)
			java.put("Slot", new ByteTag("Slot", (byte) (int) slot));
		if (tag != null) {
			int wear = number(tag.getValue().get("Damage"), 0);
			if (wear > 0)
				java.put("Damage", new ShortTag("Damage", (short) wear));
			Map<String, Tag> components = components(id, tag);
			if (!components.isEmpty())
				java.put("components", new CompoundTag("components", components));
		}
		return new CompoundTag("", java);
	}

	/** Converts a Bedrock list of items, giving each the slot it is saved with, or its position when it has none. */
	public static ListTag toJava(ListTag items, BedrockBlockMapper mapper) {
		List<Tag> converted = new ArrayList<>();
		if (items != null) {
			int position = 0;
			for (Tag tag : items.getValue()) {
				if (tag instanceof CompoundTag item) {
					int slot = number(item.getValue().get("Slot"), position);
					CompoundTag java = toJava(item, slot, mapper);
					if (java != null)
						converted.add(java);
				}
				position++;
			}
		}
		return new ListTag("Items", CompoundTag.class, converted);
	}

	private static String javaId(String name, int damage, CompoundTag block, BedrockBlockMapper mapper) {
		switch (name) {
			case "minecraft:bed":
				return "minecraft:" + ContainerKind.colorName(Colors.byId(Math.floorMod(damage, 16))) + "_bed";
			case "minecraft:banner":
				return "minecraft:" + legacyColor(damage) + "_banner";
			case "minecraft:skull":
				return "minecraft:" + HEADS[Math.floorMod(damage, HEADS.length)];
			default:
				break;
		}
		String renamed = RENAMES.get(name);
		if (renamed != null)
			return renamed;
		if (name.startsWith("minecraft:record_"))
			return "minecraft:music_disc_" + name.substring("minecraft:record_".length());
		if (block != null && name.equals(BedrockBlockMapper.bedrockName(block))) {
			// A block item named the Bedrock way, such as minecraft:wool, gets its Java block's name
			String java = mapper.map(block).name();
			if (!java.startsWith("tectonicus:"))
				return RENAMES.getOrDefault(java, java);
		}
		return name;
	}

	/** The colour name for a Bedrock banner colour, which counts from black (0) to white (15). */
	static String legacyColor(int color) {
		return ContainerKind.colorName(Colors.byId(15 - Math.floorMod(color, 16)));
	}

	private static Map<String, Tag> components(String id, CompoundTag tag) {
		Map<String, Tag> components = new HashMap<>();
		CompoundTag display = BedrockChunkReader.compound(tag, "display");
		if (display != null) {
			String customName = BedrockChunkReader.string(display, "Name", null);
			if (customName != null)
				components.put("minecraft:custom_name", new StringTag("minecraft:custom_name", customName));
		}
		if (tag.getValue().get("customColor") instanceof IntTag color)
			components.put("minecraft:dyed_color", new IntTag("minecraft:dyed_color", color.getValue() & 0xFFFFFF));
		if (tag.getValue().get("ench") instanceof ListTag enchantments) {
			Map<String, Tag> levels = new HashMap<>();
			for (Tag entry : enchantments.getValue()) {
				if (!(entry instanceof CompoundTag enchantment))
					continue;
				int number = number(enchantment.getValue().get("id"), -1);
				if (number < 0 || number >= ENCHANTMENTS.length)
					continue;
				String key = "minecraft:" + ENCHANTMENTS[number];
				levels.put(key, new IntTag(key, number(enchantment.getValue().get("lvl"), 1)));
			}
			if (!levels.isEmpty()) {
				String component = id.equals("minecraft:enchanted_book") ? "minecraft:stored_enchantments" : "minecraft:enchantments";
				components.put(component, new CompoundTag(component, levels));
			}
		}
		if (tag.getValue().get("map_uuid") instanceof LongTag uuid)
			components.put("minecraft:map_id", new IntTag("minecraft:map_id", mapId(uuid.getValue())));
		return components;
	}

	/**
	 * The map id Tectonicus uses for a Bedrock map. Bedrock names maps with 64-bit ids and Java with 32-bit ones, so
	 * each Bedrock id gets a stable 32-bit id, found again with {@link #mapUuid(int)}.
	 */
	public static int mapId(long uuid) {
		return MAP_IDS.computeIfAbsent(uuid, u -> {
			int id = Long.hashCode(u) & 0x7FFFFFFF;
			while (MAP_UUIDS.putIfAbsent(id, u) != null)
				id = (id + 1) & 0x7FFFFFFF;
			return id;
		});
	}

	/** The Bedrock map id for a map id from {@link #mapId(long)}, or null. */
	public static Long mapUuid(int mapId) {
		return MAP_UUIDS.get(mapId);
	}

	static int number(Tag tag, int fallback) {
		return switch (tag) {
			case ByteTag b -> b.getValue();
			case ShortTag s -> s.getValue();
			case IntTag i -> i.getValue();
			case LongTag l -> (int) (long) l.getValue();
			case null, default -> fallback;
		};
	}
}
