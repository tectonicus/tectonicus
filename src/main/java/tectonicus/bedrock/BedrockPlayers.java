/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import org.jnbt.CompoundTag;
import org.jnbt.DoubleTag;
import org.jnbt.FloatTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts Bedrock player records into the Java player NBT Tectonicus reads. Bedrock saves no player names, so
 * players are labelled from the configuration or as "Player".
 */
public final class BedrockPlayers {
	/** Bedrock saves a player's eye position; Java saves their feet. */
	private static final double EYE_HEIGHT = 1.62;
	private static final String[] DIMENSIONS = {"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"};

	private BedrockPlayers() {}

	public static CompoundTag toJava(CompoundTag player, BedrockBlockMapper mapper) {
		Map<String, Tag> java = new HashMap<>();
		if (player.getValue().get("Pos") instanceof ListTag pos && pos.getValue().size() >= 3) {
			double x = ((Number) pos.getValue().get(0).getValue()).doubleValue();
			double y = ((Number) pos.getValue().get(1).getValue()).doubleValue() - EYE_HEIGHT;
			double z = ((Number) pos.getValue().get(2).getValue()).doubleValue();
			java.put("Pos", new ListTag("Pos", DoubleTag.class, List.of(new DoubleTag("", x), new DoubleTag("", y), new DoubleTag("", z))));
		}
		int dimension = Math.floorMod(BedrockItems.number(player.getValue().get("DimensionId"), 0), 3);
		java.put("Dimension", new StringTag("Dimension", DIMENSIONS[dimension]));

		if (player.getValue().get("Attributes") instanceof ListTag attributes) {
			for (Tag attribute : attributes.getValue()) {
				if (!(attribute instanceof CompoundTag compound))
					continue;
				float current = compound.getValue().get("Current") instanceof FloatTag f ? f.getValue() : 0;
				switch (BedrockChunkReader.string(compound, "Name", "")) {
					case "minecraft:health" -> java.put("Health", new FloatTag("Health", current));
					case "minecraft:player.hunger" -> java.put("foodLevel", new IntTag("foodLevel", Math.round(current)));
					default -> {
						// Not shown on the map
					}
				}
			}
		}
		java.put("XpLevel", new IntTag("XpLevel", BedrockItems.number(player.getValue().get("PlayerLevel"), 0)));

		int spawnY = BedrockItems.number(player.getValue().get("SpawnY"), Integer.MIN_VALUE);
		if (spawnY != Integer.MIN_VALUE && player.getValue().get("SpawnX") != null && player.getValue().get("SpawnZ") != null) {
			java.put("SpawnX", new IntTag("SpawnX", BedrockItems.number(player.getValue().get("SpawnX"), 0)));
			java.put("SpawnY", new IntTag("SpawnY", spawnY));
			java.put("SpawnZ", new IntTag("SpawnZ", BedrockItems.number(player.getValue().get("SpawnZ"), 0)));
			int spawnDimension = Math.floorMod(BedrockItems.number(player.getValue().get("SpawnDimension"), 0), 3);
			java.put("SpawnDimension", new StringTag("SpawnDimension", DIMENSIONS[spawnDimension]));
		}

		java.put("Inventory", rename(BedrockItems.toJava(list(player, "Inventory"), mapper), "Inventory"));
		java.put("EnderItems", rename(BedrockItems.toJava(list(player, "EnderChestInventory"), mapper), "EnderItems"));
		Map<String, Tag> equipment = new HashMap<>();
		ListTag armor = list(player, "Armor");
		if (armor != null) {
			String[] slots = {"head", "chest", "legs", "feet"};
			for (int i = 0; i < Math.min(4, armor.getValue().size()); i++)
				if (armor.getValue().get(i) instanceof CompoundTag item)
					equip(equipment, slots[i], item, mapper);
		}
		ListTag offhand = list(player, "Offhand");
		if (offhand != null && !offhand.getValue().isEmpty() && offhand.getValue().getFirst() instanceof CompoundTag item)
			equip(equipment, "offhand", item, mapper);
		if (!equipment.isEmpty())
			java.put("equipment", new CompoundTag("equipment", equipment));
		return new CompoundTag("", java);
	}

	private static ListTag list(CompoundTag tag, String name) {
		return tag.getValue().get(name) instanceof ListTag list ? list : null;
	}

	private static ListTag rename(ListTag list, String name) {
		return new ListTag(name, CompoundTag.class, list.getValue());
	}

	private static void equip(Map<String, Tag> equipment, String slot, CompoundTag item, BedrockBlockMapper mapper) {
		CompoundTag java = BedrockItems.toJava(item, null, mapper);
		if (java != null)
			equipment.put(slot, new CompoundTag(slot, java.getValue()));
	}
}
