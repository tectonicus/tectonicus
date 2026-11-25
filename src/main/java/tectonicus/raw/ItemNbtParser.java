/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import org.tectonicus.jnbt.ByteTag;
import org.tectonicus.jnbt.CompoundTag;
import org.tectonicus.jnbt.IntTag;
import org.tectonicus.jnbt.ListTag;
import org.tectonicus.jnbt.ShortTag;
import org.tectonicus.jnbt.StringTag;
import org.tectonicus.jnbt.Tag;
import tectonicus.world.Colors;

import java.util.ArrayList;
import java.util.List;

/** Reads item metadata shared by player inventories and containers. */
final class ItemNbtParser {
	private ItemNbtParser() {}

	static Item parse(CompoundTag itemTag, int slot) {
		return parse(itemTag, slot, false);
	}

	/**
	 * @param legacy true if the item was saved before the 1.13 flattening, where the damage value can pick an item
	 *               variant. Items saved with a numeric ID (before 1.8) are always read as legacy items.
	 */
	static Item parse(CompoundTag itemTag, int slot, boolean legacy) {
		Tag idTag = itemTag.getValue().get("id");
		String id = "";
		if (idTag instanceof StringTag stringId) {
			id = stringId.getValue();
		} else if (numberValue(idTag) != null) {
			legacy = true;
			String legacyId = LegacyItems.get().fromNumericId(numberValue(idTag));
			id = legacyId == null ? "" : legacyId;
		}
		int count = NbtUtil.getInt(itemTag, "Count", NbtUtil.getByte(itemTag, "Count", (byte) 1));
		if (id.isEmpty() || id.equals("minecraft:air") || id.equals("air") || count <= 0)
			return null;

		int damage = NbtUtil.getShort(itemTag, "Damage", (short) 0);
		if (legacy)
			return parseLegacy(id, damage, count, slot, NbtUtil.getChild(itemTag, "tag", CompoundTag.class));
		return new Item(id, damage, count, slot, parseItemComponents(itemTag));
	}

	/** Converts an item saved before 1.13 to its 1.13 ID, as Minecraft does when it upgrades a world. */
	private static Item parseLegacy(String id, int damage, int count, int slot, CompoundTag tag) {
		LegacyItems legacyItems = LegacyItems.get();
		if (id.indexOf(':') < 0)
			id = "minecraft:" + id;
		List<Object> components = parseTagTag(tag, id, true);
		if (components == null)
			components = new ArrayList<>();

		if (id.equals("minecraft:spawn_egg")) {
			// 1.9 to 1.12 eggs name their entity in EntityTag. Older eggs store the numeric entity ID as damage.
			CompoundTag entityTag = tag == null ? null : NbtUtil.getChild(tag, "EntityTag", CompoundTag.class);
			String entity = entityTag == null ? null : NbtUtil.getString(entityTag, "id", null);
			if (entity == null)
				entity = legacyItems.spawnEggEntity(damage);
			String egg = legacyItems.spawnEggItem(entity);
			return new Item(egg == null ? "minecraft:pig_spawn_egg" : egg, 0, count, slot, components);
		}

		if (id.equals("minecraft:potion") && (tag == null || NbtUtil.getChild(tag, "Potion", StringTag.class) == null)) {
			// Before 1.9 the damage value held the potion effect and whether it was a splash potion.
			components.add(new PotionContentsTag(legacyItems.potionFromDamage(damage)));
			return new Item(LegacyItems.isSplashPotionDamage(damage) ? "minecraft:splash_potion" : "minecraft:potion",
					0, count, slot, components);
		}

		boolean variant = legacyItems.usesDamageForVariant(id);
		return new Item(legacyItems.toModernId(id, damage), variant ? 0 : damage, count, slot, components);
	}

	private static Integer numberValue(Tag tag) {
		if (tag instanceof ShortTag shortTag)
			return (int) shortTag.getValue();
		if (tag instanceof IntTag intTag)
			return intTag.getValue();
		if (tag instanceof ByteTag byteTag)
			return (int) byteTag.getValue();
		return null;
	}

	static List<Object> parseItemComponents(CompoundTag itemTag) {
		CompoundTag components = NbtUtil.getChild(itemTag, "components", CompoundTag.class);
		return components != null ? parseComponentsTag(components)
				: parseTagTag(NbtUtil.getChild(itemTag, "tag", CompoundTag.class), NbtUtil.getString(itemTag, "id", ""), false);
	}

	// 1.20.5 and later
	private static List<Object> parseComponentsTag(CompoundTag componentsTag) {
		List<Object> components = new ArrayList<>();
		
		final CompoundTag potionContentsTag = NbtUtil.getChild(componentsTag, "minecraft:potion_contents", CompoundTag.class);
		if (potionContentsTag != null) {
			final StringTag potionTag = NbtUtil.getChild(potionContentsTag, "potion", StringTag.class);
			if (potionTag != null) {
				components.add(new PotionContentsTag(potionTag.getValue()));
			}
		}
		
		final IntTag dyedColorTagInt = NbtUtil.getChild(componentsTag, "minecraft:dyed_color", IntTag.class); //As of 1.21.5 dyed_color will always be an int tag
		final CompoundTag dyedColorTag = NbtUtil.getChild(componentsTag, "minecraft:dyed_color", CompoundTag.class);
		if (dyedColorTagInt != null) {
			components.add(new DyedColorTag(dyedColorTagInt.getValue()));
		} else if (dyedColorTag != null) {
			IntTag rgbTag = NbtUtil.getChild(dyedColorTag, "rgb", IntTag.class);
			if (rgbTag != null) {
				components.add(new DyedColorTag(rgbTag.getValue()));
			}
		}
		
		final StringTag customNameTag = NbtUtil.getChild(componentsTag, "minecraft:custom_name", StringTag.class);
		if (customNameTag != null) {
			final String name = customNameTag.getValue().replace("\"", ""); // Replace " characters
			components.add(new CustomNameTag(name));
		}
		
		boolean isStoredEnchantments = true;
		CompoundTag enchantmentsTag = NbtUtil.getChild(componentsTag, "minecraft:stored_enchantments", CompoundTag.class);
		if (enchantmentsTag == null) {
			isStoredEnchantments = false;
			enchantmentsTag = NbtUtil.getChild(componentsTag, "minecraft:enchantments", CompoundTag.class);
		}
		
		if (enchantmentsTag != null) {
			CompoundTag levelsTag = NbtUtil.getChild(enchantmentsTag, "levels", CompoundTag.class);
			
			if (levelsTag == null) {
				// From 1.21.5, simplified format is used, where levels field is inlined to top-level
				levelsTag = enchantmentsTag;
			}
			
			List<EnchantmentTag> enchantments = new ArrayList<>();
			
			for (String enchantmentId : levelsTag.getValue().keySet()) {
				if (levelsTag.contains(enchantmentId, IntTag.class)) {
					enchantments.add(new EnchantmentTag(enchantmentId, levelsTag.getInt(enchantmentId)));
				}
			}
			
			components.add(isStoredEnchantments
				? new StoredEnchantmentsTag(enchantments)
				: new EnchantmentsTag(enchantments));
		}
		
		CompoundTag trimTag = NbtUtil.getChild(componentsTag, "minecraft:trim", CompoundTag.class);
		if (trimTag != null) {
			StringTag materialTag = NbtUtil.getChild(trimTag, "material", StringTag.class);
			StringTag patternTag = NbtUtil.getChild(trimTag, "pattern", StringTag.class);
			if (materialTag != null && patternTag != null) {
				components.add(new ArmorTrimTag(materialTag.getValue(), patternTag.getValue()));
			}
		}
		
		addBannerPatterns(components, NbtUtil.getChild(componentsTag, "minecraft:banner_patterns", ListTag.class), false);
		StringTag baseColorTag = NbtUtil.getChild(componentsTag, "minecraft:base_color", StringTag.class);
		Colors baseColor = baseColorTag == null ? null : Colors.byName(baseColorTag.getValue().replace("minecraft:", ""));
		if (baseColor != null)
			components.add(new BaseColorTag(baseColor));
		return components;
	}
	
	/**
	 * Reads the item tag used by 1.20.4 and older.
	 *
	 * @param legacy true if the item was saved before the 1.13 flattening, when dye colors had inverted IDs
	 */
	private static List<Object> parseTagTag(CompoundTag tagTag, String itemId, boolean legacy) {
		if (tagTag == null) {
			return null;
		}
		
		List<Object> tag = new ArrayList<>();
		
		final StringTag potionTag = NbtUtil.getChild(tagTag, "Potion", StringTag.class);
		if (potionTag != null) {
			tag.add(new PotionContentsTag(potionTag.getValue()));
		}
		
		final CompoundTag displayTag = NbtUtil.getChild(tagTag, "display", CompoundTag.class);
		if (displayTag != null) {
			final StringTag nameTag = NbtUtil.getChild(displayTag, "Name", StringTag.class);
			if (nameTag != null) {
				String name = nameTag.getValue().replace("\"", ""); // Replace " characters
				tag.add(new CustomNameTag(name));
			}
			
			IntTag colorTag = NbtUtil.getChild(displayTag, "color", IntTag.class);
			if (colorTag != null) {
				tag.add(new DyedColorTag(colorTag.getValue()));
			}
		}
		
		boolean isStoredEnchantments = true;
		ListTag enchantmentsTag = NbtUtil.getChild(tagTag, "StoredEnchantments", ListTag.class);
		if (enchantmentsTag == null) {
			isStoredEnchantments = false;
			enchantmentsTag = NbtUtil.getChild(tagTag, "Enchantments", ListTag.class);
		}
		if (enchantmentsTag == null) {
			// 1.12 and older
			enchantmentsTag = NbtUtil.getChild(tagTag, "ench", ListTag.class);
		}
		
		if (enchantmentsTag != null) {
			List<EnchantmentTag> enchantments = new ArrayList<>();
			
			for (var enchantmentTag : enchantmentsTag.getValue()) {
				if (enchantmentTag instanceof CompoundTag enchantmentCompoundTag) {
					Tag idTag = enchantmentCompoundTag.getValue().get("id");
					Integer level = numberValue(enchantmentCompoundTag.getValue().get("lvl"));
					// Enchantments had numeric IDs before 1.13
					String id = idTag instanceof StringTag stringId ? stringId.getValue()
							: numberValue(idTag) == null ? null : LegacyItems.get().enchantment(numberValue(idTag));
					if (id != null && level != null) {
						enchantments.add(new EnchantmentTag(id, level));
					}
				}
			}
			
			tag.add(isStoredEnchantments
				? new StoredEnchantmentsTag(enchantments)
				: new EnchantmentsTag(enchantments));
		}
		
		CompoundTag trimTag = NbtUtil.getChild(tagTag, "Trim", CompoundTag.class);
		if (trimTag != null) {
			StringTag materialTag = NbtUtil.getChild(trimTag, "material", StringTag.class);
			StringTag patternTag = NbtUtil.getChild(trimTag, "pattern", StringTag.class);
			if (materialTag != null && patternTag != null) {
				tag.add(new ArmorTrimTag(materialTag.getValue(), patternTag.getValue()));
			}
		}
		
		CompoundTag blockEntity = NbtUtil.getChild(tagTag, "BlockEntityTag", CompoundTag.class);
		if (blockEntity != null) {
			IntTag baseTag = NbtUtil.getChild(blockEntity, "Base", IntTag.class);
			// Banner items only kept a Base color before 1.13. Shields kept it after the flattening, with
			// the dye IDs no longer inverted.
			boolean shield = itemId.equals("minecraft:shield") || itemId.equals("shield");
			boolean legacyColors = legacy || (baseTag != null && !shield);
			addBannerPatterns(tag, NbtUtil.getChild(blockEntity, "Patterns", ListTag.class), legacyColors);
			if (shield && baseTag != null) {
				int colorId = baseTag.getValue();
				if (colorId >= 0 && colorId <= 15)
					tag.add(new BaseColorTag(Colors.byId(legacyColors ? 15 - colorId : colorId)));
			}
		}
		return tag;
	}

	private static void addBannerPatterns(List<Object> components, ListTag layers, boolean legacyColors) {
		var patterns = BannerEntity.parsePatterns(layers, legacyColors);
		if (!patterns.isEmpty())
			components.add(new BannerPatternsTag(patterns));
	}
}
