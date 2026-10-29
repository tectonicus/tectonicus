/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;

import java.util.ArrayList;
import java.util.List;

/** Reads item metadata shared by player inventories and containers. */
final class ItemNbtParser {
	private ItemNbtParser() {}

	static Item parse(CompoundTag itemTag, int slot) {
		String id = NbtUtil.getString(itemTag, "id", "");
		int count = NbtUtil.getInt(itemTag, "Count", NbtUtil.getByte(itemTag, "Count", (byte) 1));
		if (id.isEmpty() || id.equals("minecraft:air") || count <= 0)
			return null;

		List<Object> components = parseComponentsTag(NbtUtil.getChild(itemTag, "components", CompoundTag.class));
		if (components == null)
			components = parseTagTag(NbtUtil.getChild(itemTag, "tag", CompoundTag.class));
		return new Item(id, NbtUtil.getShort(itemTag, "Damage", (short) 0), count, slot, components);
	}

	// 1.20.5 and later
	static List<Object> parseComponentsTag(CompoundTag componentsTag) {
		if (componentsTag == null) {
			return null;
		}
		
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
			
			for (var levelTagId : levelsTag.getValue().keySet()) {
				IntTag levelTag = NbtUtil.getChild(levelsTag, levelTagId, IntTag.class);
				if (levelTag != null) {
					enchantments.add(new EnchantmentTag(levelTagId, levelTag.getValue()));
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
		
		return components;
	}
	
	// 1.20.4 and older
	static List<Object> parseTagTag(CompoundTag tagTag) {
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
		
		if (enchantmentsTag != null) {
			List<EnchantmentTag> enchantments = new ArrayList<>();
			
			for (var enchantmentTag : enchantmentsTag.getValue()) {
				if (enchantmentTag instanceof CompoundTag enchantmentCompoundTag) {
					StringTag idTag = NbtUtil.getChild(enchantmentCompoundTag, "id", StringTag.class);
					ShortTag levelTag = NbtUtil.getChild(enchantmentCompoundTag, "lvl", ShortTag.class);
					if (idTag != null && levelTag != null) {
						enchantments.add(new EnchantmentTag(idTag.getValue(), Integer.valueOf(levelTag.getValue())));
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
		
		return tag;
	}

}
