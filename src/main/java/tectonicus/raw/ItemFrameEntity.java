/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import lombok.Getter;
import org.tectonicus.jnbt.ByteTag;
import org.tectonicus.jnbt.CompoundTag;
import org.tectonicus.jnbt.IntTag;

@Getter
public class ItemFrameEntity extends BlockEntity {
	private static final String FILLED_MAP = "minecraft:filled_map";

	private final int direction;
	private final boolean glow;
	private final boolean invisible;
	private final boolean filledMap;
	private final Integer mapId;
	/** Rotation of a map in quarter turns, 0 to 3 */
	private final int rotation;
	/** The item shown in the frame with its 1.13 ID, or null for an empty frame or a map */
	private final Item item;
	/** Rotation of an item other than a map in eighth turns, 0 to 7 */
	private final int itemRotation;

	ItemFrameEntity(int x, int y, int z, int localX, int localY, int localZ, String id, int direction, CompoundTag entity) {
		this(x, y, z, localX, localY, localZ, id, direction, entity, false);
	}

	/** @param legacyItems true if the item was saved before the 1.13 flattening */
	ItemFrameEntity(int x, int y, int z, int localX, int localY, int localZ, String id, int direction, CompoundTag entity,
			boolean legacyItems) {
		super(x, y, z, localX, localY, localZ);
		this.direction = direction;
		glow = id.equals("minecraft:glow_item_frame");
		invisible = NbtUtil.getByte(entity, "Invisible", (byte) 0) != 0;
		int savedRotation = NbtUtil.getByte(entity, "ItemRotation", (byte) 0);
		rotation = savedRotation & 3;
		// Frames saved before 1.8 (without Facing) turn items in quarter turns rather than eighth turns.
		boolean quarterTurns = NbtUtil.getChild(entity, "Facing", ByteTag.class) == null;
		itemRotation = quarterTurns ? (savedRotation & 3) * 2 : savedRotation & 7;

		CompoundTag itemTag = NbtUtil.getChild(entity, "Item", CompoundTag.class);
		filledMap = itemTag != null && (FILLED_MAP.equals(NbtUtil.getString(itemTag, "id", ""))
				|| NbtUtil.getShort(itemTag, "id", (short) -1) == 358);
		mapId = filledMap ? readMapId(itemTag) : null;
		item = itemTag == null || filledMap ? null : ItemNbtParser.parse(itemTag, 0, legacyItems);
	}

	private static Integer readMapId(CompoundTag item) {
		CompoundTag components = NbtUtil.getChild(item, "components", CompoundTag.class);
		IntTag mapId = components == null ? null : NbtUtil.getChild(components, "minecraft:map_id", IntTag.class);
		if (mapId == null) {
			CompoundTag tag = NbtUtil.getChild(item, "tag", CompoundTag.class);
			mapId = tag == null ? null : NbtUtil.getChild(tag, "map", IntTag.class);
		}
		int id = mapId == null ? NbtUtil.getShort(item, "Damage", (short) -1) : mapId.getValue();
		return id >= 0 ? id : null;
	}
}
