/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tectonicus.BlockIds;
import tectonicus.world.Colors;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum ContainerKind {
	CHEST("chest", "Chest"),
	TRAPPED_CHEST("trapped_chest", "Trapped Chest"),
	ENDER_CHEST("ender_chest", "Ender Chest"),
	BARREL("barrel", "Barrel"),
	SHULKER_BOX("shulker_box", "Shulker Box");

	private final String id;
	private final String displayName;

	public boolean isChest() {
		return this == CHEST || this == TRAPPED_CHEST;
	}

	public static ContainerKind fromBlock(String entityId, String blockName, int blockId) {
		if (blockName != null) {
			for (ContainerKind kind : values()) {
				if (blockName.equals("minecraft:" + kind.id)) return kind;
			}
			if (shulkerColor(blockName, 0) != null) return SHULKER_BOX;
			// Copper chests share the chest block entity ID but aren't selected by this filter.
			if (blockName.endsWith("copper_chest")) return null;
		}
		if (blockId == BlockIds.TRAPPED_CHEST) return TRAPPED_CHEST;
		if (blockId >= 219 && blockId <= 234) return SHULKER_BOX;
		return switch (entityId) {
			case "Chest", "minecraft:chest" -> CHEST;
			case "minecraft:trapped_chest" -> TRAPPED_CHEST;
			case "EnderChest", "minecraft:ender_chest" -> ENDER_CHEST;
			case "minecraft:barrel" -> BARREL;
			case "ShulkerBox", "minecraft:shulker_box" -> SHULKER_BOX;
			default -> null;
		};
	}

	public static String shulkerColor(String blockName, int blockId) {
		if (blockId >= 219 && blockId <= 234) return colorName(Colors.byId(blockId - 219));
		if (blockName == null || !blockName.startsWith("minecraft:") || !blockName.endsWith("_shulker_box")) return null;
		Colors color = Colors.byName(blockName.substring("minecraft:".length(), blockName.length() - "_shulker_box".length()));
		return color == null ? null : colorName(color);
	}

	public static String colorName(Colors color) {
		return color == Colors.SILVER ? "light_gray" : color.getName().toLowerCase(Locale.ROOT);
	}
}
