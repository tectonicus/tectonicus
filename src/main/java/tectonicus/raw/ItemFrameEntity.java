/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import lombok.Getter;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;

@Getter
public class ItemFrameEntity extends BlockEntity {
	private static final String FILLED_MAP = "minecraft:filled_map";

	private final int direction;
	private final boolean glow;
	private final boolean filledMap;
	private final Integer mapId;
	private final int rotation;

	ItemFrameEntity(int x, int y, int z, int localX, int localY, int localZ, String id, int direction, CompoundTag entity) {
		super(x, y, z, localX, localY, localZ);
		this.direction = direction;
		glow = id.equals("minecraft:glow_item_frame");
		rotation = NbtUtil.getByte(entity, "ItemRotation", (byte) 0) & 3;

		CompoundTag item = NbtUtil.getChild(entity, "Item", CompoundTag.class);
		filledMap = item != null && (FILLED_MAP.equals(NbtUtil.getString(item, "id", ""))
				|| NbtUtil.getShort(item, "id", (short) -1) == 358);
		mapId = filledMap ? readMapId(item) : null;
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
