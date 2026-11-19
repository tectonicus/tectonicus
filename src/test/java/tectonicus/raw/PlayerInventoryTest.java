/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import org.tectonicus.jnbt.ByteTag;
import org.tectonicus.jnbt.CompoundTag;
import org.tectonicus.jnbt.IntTag;
import org.tectonicus.jnbt.ListTag;
import org.tectonicus.jnbt.ShortTag;
import org.tectonicus.jnbt.StringTag;
import org.tectonicus.jnbt.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.blockTypes.Banner.Pattern;
import tectonicus.world.Colors;

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

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void readsOrderedBannerPatternsInPlayerAndEnderChestInventories(boolean modern) {
		String patternTag = modern ? "pattern" : "Pattern";
		String colorTag = modern ? "color" : "Color";
		String first = modern ? "minecraft:stripe_top" : "ts";
		String second = modern ? "minecraft:cross" : "cr";
		String listName = modern ? "minecraft:banner_patterns" : "Patterns";
		ListTag layers = new ListTag(listName, CompoundTag.class, List.of(
				new CompoundTag("", Map.of(patternTag, new StringTag(patternTag, first),
						colorTag, modern ? new StringTag(colorTag, "blue") : new IntTag(colorTag, 11))),
				new CompoundTag("", Map.of(patternTag, new StringTag(patternTag, second),
						colorTag, modern ? new StringTag(colorTag, "white") : new IntTag(colorTag, 0)))));
		String metadataName = modern ? "components" : "tag";
		CompoundTag metadata = new CompoundTag(metadataName, modern ? Map.of(listName, layers)
				: Map.of("BlockEntityTag", new CompoundTag("BlockEntityTag", Map.of(listName, layers))));
		CompoundTag item = new CompoundTag("", Map.of("id", new StringTag("id", "minecraft:red_banner"),
				"Slot", new ByteTag("Slot", (byte) 0), metadataName, metadata));
		Player player = new Player("Alex", new CompoundTag("", Map.of(
				"Inventory", new ListTag("Inventory", CompoundTag.class, List.of(item)),
				"EnderItems", new ListTag("EnderItems", CompoundTag.class, List.of(item)))));
		List<Pattern> expected = List.of(new Pattern(first, Colors.BLUE), new Pattern(second, Colors.WHITE));
		assertEquals(expected, player.getInventory().getFirst().getComponent(BannerPatternsTag.class).patterns());
		assertEquals(expected, player.getEnderChestInventory().getFirst().getComponent(BannerPatternsTag.class).patterns());
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

	@Test
	void convertsItemsSavedBeforeTheFlattening() {
		CompoundTag numericSword = new CompoundTag("", Map.of(
				"id", new ShortTag("id", (short) 276),
				"Slot", new ByteTag("Slot", (byte) 0),
				"Count", new ByteTag("Count", (byte) 1),
				"Damage", new ShortTag("Damage", (short) 12),
				"tag", new CompoundTag("tag", Map.of(
						"ench", new ListTag("ench", CompoundTag.class, List.of(
								new CompoundTag("", Map.of("id", new ShortTag("id", (short) 16),
										"lvl", new ShortTag("lvl", (short) 4)))))))));
		CompoundTag wool = legacyStack("minecraft:wool", 1, 14, 32);
		CompoundTag splashPotion = legacyStack("minecraft:potion", 2, 16386, 1);
		CompoundTag numericEgg = new CompoundTag("", Map.of("id", new ShortTag("id", (short) 383),
				"Slot", new ByteTag("Slot", (byte) 3), "Count", new ByteTag("Count", (byte) 1),
				"Damage", new ShortTag("Damage", (short) 90)));
		CompoundTag egg = new CompoundTag("", Map.of("id", new StringTag("id", "minecraft:spawn_egg"),
				"Slot", new ByteTag("Slot", (byte) 4), "Count", new ByteTag("Count", (byte) 1),
				"tag", new CompoundTag("tag", Map.of("EntityTag", new CompoundTag("EntityTag",
						Map.of("id", new StringTag("id", "minecraft:creeper")))))));
		CompoundTag book = new CompoundTag("", Map.of("id", new StringTag("id", "minecraft:enchanted_book"),
				"Slot", new ByteTag("Slot", (byte) 5), "Count", new ByteTag("Count", (byte) 1),
				"tag", new CompoundTag("tag", Map.of("StoredEnchantments", new ListTag("StoredEnchantments", CompoundTag.class,
						List.of(new CompoundTag("", Map.of("id", new ShortTag("id", (short) 70),
								"lvl", new ShortTag("lvl", (short) 1)))))))));
		Player player = new Player("Alex", new CompoundTag("", Map.of(
				"DataVersion", new IntTag("DataVersion", 1343),
				"Inventory", new ListTag("Inventory", CompoundTag.class,
						List.of(numericSword, wool, splashPotion, numericEgg, egg, book)))));

		List<Item> inventory = player.getInventory();
		assertEquals(List.of("minecraft:diamond_sword", "minecraft:red_wool", "minecraft:splash_potion",
				"minecraft:pig_spawn_egg", "minecraft:creeper_spawn_egg", "minecraft:enchanted_book"),
				inventory.stream().map(item -> item.id).toList());
		Item sword = inventory.getFirst();
		assertEquals(12, sword.damage);
		EnchantmentTag sharpness = sword.getComponent(EnchantmentsTag.class).enchantments.getFirst();
		assertEquals("minecraft:sharpness", sharpness.id);
		assertEquals(4, sharpness.level);
		assertEquals(0, inventory.get(1).damage);
		assertEquals(32, inventory.get(1).count);
		assertEquals("minecraft:swiftness", inventory.get(2).getComponent(PotionContentsTag.class).potion);
		assertEquals("minecraft:mending", inventory.get(5).getComponent(StoredEnchantmentsTag.class).enchantments.getFirst().id);
	}

	@Test
	void readsLevelDatPlayersFromBeforeTheFlatteningAsLegacy() {
		Player player = new Player("", new CompoundTag("", Map.of("Inventory", new ListTag("Inventory", CompoundTag.class,
				List.of(legacyStack("minecraft:planks", 0, 2, 1))))), 0);
		assertEquals("minecraft:birch_planks", player.getInventory().getFirst().id);
		// Without a data version, string IDs are read as current IDs.
		Player modern = new Player("", new CompoundTag("", Map.of("Inventory", new ListTag("Inventory", CompoundTag.class,
				List.of(legacyStack("minecraft:birch_planks", 0, 0, 1))))));
		assertEquals("minecraft:birch_planks", modern.getInventory().getFirst().id);
	}

	private static CompoundTag legacyStack(String id, int slot, int damage, int count) {
		return new CompoundTag("", Map.of("id", new StringTag("id", id), "Slot", new ByteTag("Slot", (byte) slot),
				"Count", new ByteTag("Count", (byte) count), "Damage", new ShortTag("Damage", (short) damage)));
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
