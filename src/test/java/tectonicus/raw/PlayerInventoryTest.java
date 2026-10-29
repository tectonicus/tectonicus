/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerInventoryTest {
	@Test
	void readsLegacyInventoryWithoutRequiringDamageAndKeepsItemDetails() {
		CompoundTag item = new CompoundTag("", Map.of(
				"id", new StringTag("id", "minecraft:diamond_sword"),
				"Slot", new ByteTag("Slot", (byte) 0),
				"Count", new ByteTag("Count", (byte) 1),
				"tag", new CompoundTag("tag", Map.of(
						"display", new CompoundTag("display", Map.of("Name", new StringTag("Name", "Sword"))),
						"Enchantments", new ListTag("Enchantments", CompoundTag.class, List.of(
								new CompoundTag("", Map.of("id", new StringTag("id", "minecraft:sharpness"),
										"lvl", new ShortTag("lvl", (short) 3)))))))));
		Player player = playerWithInventory(item, stack("minecraft:shield", -106, new ByteTag("Count", (byte) 1)),
				stack("minecraft:diamond_helmet", 103, new ByteTag("Count", (byte) 1)));

		assertEquals(3, player.getInventory().size());
		Item sword = player.getInventory().getFirst();
		assertEquals(0, sword.damage);
		assertEquals("Sword", sword.getComponent(CustomNameTag.class).name);
		assertEquals(3, sword.getComponent(EnchantmentsTag.class).enchantments.getFirst().level);
		assertEquals(-106, player.getInventory().get(1).slot);
		assertEquals(103, player.getInventory().get(2).slot);
	}

	@Test
	void readsModernCountsAndComponents() {
		CompoundTag item = new CompoundTag("", Map.of(
				"id", new StringTag("id", "minecraft:leather_chestplate"),
				"Slot", new ByteTag("Slot", (byte) 9),
				"count", new IntTag("count", 2),
				"components", new CompoundTag("components", Map.of(
						"minecraft:dyed_color", new IntTag("minecraft:dyed_color", 0x123456),
						"minecraft:enchantments", new CompoundTag("minecraft:enchantments", Map.of(
								"minecraft:protection", new IntTag("minecraft:protection", 4)))))));
		Item parsed = playerWithInventory(item).getInventory().getFirst();
		assertEquals(9, parsed.slot);
		assertEquals(2, parsed.count);
		assertEquals(0x123456, parsed.getComponent(DyedColorTag.class).color);
		assertEquals("minecraft:protection", parsed.getComponent(EnchantmentsTag.class).enchantments.getFirst().id);
	}

	@Test
	void readsEquipmentSeparatelyAndReplacesLegacyEquipmentInSameSlot() {
		String[] equipmentNames = {"feet", "legs", "chest", "head", "offhand"};
		Map<String, Tag> equipment = new java.util.HashMap<>();
		for (String name : equipmentNames)
			equipment.put(name, new CompoundTag(name, Map.of("id", new StringTag("id", "minecraft:stone"))));
		Player player = new Player("Alex", new CompoundTag("", Map.of(
				"Inventory", new ListTag("Inventory", CompoundTag.class, List.of(
						stack("minecraft:dirt", 103, new IntTag("count", 1)),
						stack("minecraft:diamond", 0, new IntTag("count", 64)))),
				"equipment", new CompoundTag("equipment", equipment))));

		assertEquals(6, player.getInventory().size());
		assertEquals(List.of(0, 100, 101, 102, 103, -106), player.getInventory().stream().map(item -> item.slot).toList());
		assertEquals(64, player.getInventory().getFirst().count);
		assertTrue(player.getInventory().stream().skip(1).allMatch(item -> item.count == 1 && item.id.equals("minecraft:stone")));
	}

	@Test
	void readsEnderChestStacksSeparatelyWithLegacyAndModernItemFormats() {
		CompoundTag modernItem = new CompoundTag("", Map.of(
				"id", new StringTag("id", "minecraft:diamond_sword"),
				"Slot", new ByteTag("Slot", (byte) 26),
				"count", new IntTag("count", 1),
				"components", new CompoundTag("components", Map.of(
						"minecraft:custom_name", new StringTag("minecraft:custom_name", "Treasure"),
						"minecraft:enchantments", new CompoundTag("minecraft:enchantments", Map.of(
								"minecraft:sharpness", new IntTag("minecraft:sharpness", 5)))))));
		ListTag enderItems = new ListTag("EnderItems", CompoundTag.class, List.of(
				stack("minecraft:ender_pearl", 0, new ByteTag("Count", (byte) 16)), modernItem,
				stack("minecraft:air", 1, new IntTag("count", 1)),
				stack("minecraft:dirt", 2, new IntTag("count", 0)),
				new CompoundTag("", Map.of("Slot", new ByteTag("Slot", (byte) 3)))));
		Player player = new Player("Alex", new CompoundTag("", Map.of(
				"Inventory", new ListTag("Inventory", CompoundTag.class, List.of(
						stack("minecraft:stone", 0, new IntTag("count", 64)))),
				"EnderItems", enderItems)));

		assertEquals(1, player.getInventory().size());
		assertEquals("minecraft:stone", player.getInventory().getFirst().id);
		assertEquals(2, player.getEnderChestInventory().size());
		Item pearls = player.getEnderChestInventory().getFirst();
		assertEquals("minecraft:ender_pearl", pearls.id);
		assertEquals(0, pearls.slot);
		assertEquals(16, pearls.count);
		Item sword = player.getEnderChestInventory().get(1);
		assertEquals(26, sword.slot);
		assertEquals("Treasure", sword.getComponent(CustomNameTag.class).name);
		assertEquals(5, sword.getComponent(EnchantmentsTag.class).enchantments.getFirst().level);
	}

	@Test
	void skipsEmptyOrIncompleteStacksAndInitializesProfileOnlyPlayers() {
		Player player = playerWithInventory(
				stack("minecraft:air", 0, new IntTag("count", 1)),
				stack("minecraft:stone", 1, new IntTag("count", 0)),
				new CompoundTag("", Map.of("Slot", new ByteTag("Slot", (byte) 2))),
				new CompoundTag("", Map.of("id", new StringTag("id", "minecraft:stone"))));
		assertTrue(player.getInventory().isEmpty());
		assertTrue(player.getEnderChestInventory().isEmpty());
		assertTrue(new Player("Alex", "uuid", null).getInventory().isEmpty());
		assertTrue(new Player("Alex", "uuid", null).getEnderChestInventory().isEmpty());
		assertTrue(new Player("Alex", new CompoundTag("", Map.of(
				"EnderItems", new ListTag("EnderItems", CompoundTag.class, List.of())))).getEnderChestInventory().isEmpty());
	}

	private static Player playerWithInventory(CompoundTag... items) {
		return new Player("Alex", new CompoundTag("", Map.of(
				"Inventory", new ListTag("Inventory", CompoundTag.class, List.of(items)))));
	}

	private static CompoundTag stack(String id, int slot, Tag count) {
		return new CompoundTag("", Map.of("id", new StringTag("id", id),
				"Slot", new ByteTag("Slot", (byte) slot), count.getName(), count));
	}
}
