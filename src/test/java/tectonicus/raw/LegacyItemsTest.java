/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyItemsTest {
	private final LegacyItems items = LegacyItems.get();

	@Test
	void convertsNumericIdsAndDamageVariantsToFlattenedIds() {
		assertEquals("minecraft:wool", items.fromNumericId(35));
		assertEquals("minecraft:red_wool", items.toModernId("minecraft:wool", 14));
		assertEquals("minecraft:granite", items.toModernId("stone", 1));
		assertEquals("minecraft:lapis_lazuli", items.toModernId("minecraft:dye", 4));
		assertEquals("minecraft:oak_planks", items.toModernId("minecraft:planks", 99));
		assertEquals("minecraft:diamond_sword", items.toModernId(items.fromNumericId(276), 12));
		assertFalse(items.usesDamageForVariant("minecraft:diamond_sword"));
		assertTrue(items.usesDamageForVariant("minecraft:wool"));
	}

	@Test
	void appliesRenamesAroundTheFlattening() {
		assertEquals("minecraft:totem_of_undying", items.toModernId("minecraft:totem", 0));
		assertEquals("minecraft:cooked_cod", items.toModernId("minecraft:cooked_fished", 0));
		assertEquals("minecraft:melon_slice", items.toModernId("minecraft:melon", 0));
		assertEquals("minecraft:melon", items.toModernId("minecraft:melon_block", 0));
		assertEquals("minecraft:spawner", items.toModernId("minecraft:mob_spawner", 0));
		assertEquals("minecraft:popped_chorus_fruit", items.toModernId("minecraft:chorus_fruit_popped", 0));
	}

	@Test
	void readsOldPotionsSpawnEggsAndEnchantments() {
		assertEquals("minecraft:swiftness", items.potionFromDamage(8194));
		assertTrue(LegacyItems.isSplashPotionDamage(16386));
		assertFalse(LegacyItems.isSplashPotionDamage(8194));
		assertEquals("minecraft:pig", items.spawnEggEntity(90));
		assertEquals("minecraft:pig_spawn_egg", items.spawnEggItem("Pig"));
		assertEquals("minecraft:pig_spawn_egg", items.spawnEggItem("minecraft:pig"));
		assertEquals("minecraft:evoker_spawn_egg", items.spawnEggItem("minecraft:evocation_illager"));
		assertEquals("minecraft:sharpness", items.enchantment(16));
		assertEquals("minecraft:mending", items.enchantment(70));
	}

	@Test
	void describesOldModelsNamesAndIcons() {
		LegacyItems.Entry wool = items.item("minecraft:red_wool");
		assertEquals("minecraft:wool", wool.legacyId());
		assertEquals(14, wool.damage());
		assertEquals(35, wool.numericId());
		assertTrue(wool.isBlock());
		assertEquals("red_wool", wool.model());
		assertEquals("tile.cloth.red.name", wool.langKey());

		LegacyItems.Entry sword = items.item("minecraft:diamond_sword");
		assertFalse(sword.isBlock());
		assertEquals(List.of("items/diamond_sword"), sword.textures());
		assertEquals(3 + 4 * 16, sword.itemSheetIndex());
		assertNull(items.item("minecraft:netherite_sword"));
	}

	@Test
	void mapsModernTranslationKeysToOldLanguageFiles() {
		assertEquals("item.swordDiamond.name", items.translationAlias("item.minecraft.diamond_sword"));
		assertEquals("item.swordDiamond.name", items.translationAlias("block.minecraft.diamond_sword"));
		assertEquals("tile.cloth.red.name", items.translationAlias("block.minecraft.red_wool"));
		assertEquals("enchantment.damage.all", items.translationAlias("enchantment.minecraft.sharpness"));
		assertEquals("potion.effect.swiftness", items.translationAlias("item.minecraft.potion.effect.swiftness"));
		assertNull(items.translationAlias("item.minecraft.diamond_sword.desc"));
		assertNull(items.translationAlias("container.inventory"));
	}

	@Test
	void triesLanguageKeysOfJarsBefore1_3() {
		assertEquals(List.of("tile.log.oak.name", "tile.log.name"), items.translationAliases("item.minecraft.oak_log"));
		assertEquals(List.of("tile.stone.stone.name", "tile.stone.name"), items.translationAliases("block.minecraft.stone"));
		assertEquals(List.of("item.diamond.name", "item.emerald.name"), items.translationAliases("item.minecraft.diamond"));
		assertEquals(List.of("tile.stoneSlab.name", "tile.stoneSlab.stone.name"),
				items.translationAliases("item.minecraft.stone_slab"));
		assertEquals(List.of("item.swordDiamond.name"), items.translationAliases("item.minecraft.diamond_sword"));
		assertEquals(List.of(), items.translationAliases("container.inventory"));
	}

	@Test
	void treatsSavesBeforeTheFlatteningAsLegacy() {
		assertTrue(LegacyItems.isLegacy(0));
		assertTrue(LegacyItems.isLegacy(1343));
		assertFalse(LegacyItems.isLegacy(1519));
	}
}
