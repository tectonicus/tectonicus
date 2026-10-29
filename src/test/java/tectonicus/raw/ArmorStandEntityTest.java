/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import org.jnbt.*;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.Minecraft;
import tectonicus.Version;
import tectonicus.chunk.ChunkData;
import tectonicus.world.WorldInfo;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArmorStandEntityTest {
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void readsBothHandSlotsFromLegacyAndModernEquipment(boolean modern) throws Exception {
		Map<String, Tag> tags = standTags();
		CompoundTag shield = stack("minecraft:shield");
		CompoundTag sword = stack("minecraft:diamond_sword");
		if (modern)
			tags.put("equipment", new CompoundTag("equipment", Map.of("mainhand", new CompoundTag("mainhand", shield.getValue()),
					"offhand", new CompoundTag("offhand", sword.getValue()))));
		else
			tags.put("HandItems", new ListTag("HandItems", CompoundTag.class, List.of(shield, sword)));
		ArmorStandEntity stand = read(tags, modern);
		assertEquals("minecraft:shield", stand.getMainHandItem().id);
		assertEquals("minecraft:diamond_sword", stand.getOffHandItem().id);
		assertFalse(stand.getInvisible());
		assertFalse(stand.getNoBasePlate());
		assertFalse(stand.isLeftHanded());
		assertFalse(stand.isShowArms());
		assertEquals(new Vector3f(-10, 0, -10), stand.getLeftArmPose());
		assertEquals(new Vector3f(-15, 0, 10), stand.getRightArmPose());
	}

	@Test
	void readsArmPosesAndLeftHandedStandsWithEmptyHandSlots() throws Exception {
		Map<String, Tag> tags = standTags();
		tags.put("LeftHanded", new ByteTag("LeftHanded", (byte) 1));
		tags.put("ShowArms", new ByteTag("ShowArms", (byte) 1));
		tags.put("Invisible", new ByteTag("Invisible", (byte) 1));
		tags.put("NoBasePlate", new ByteTag("NoBasePlate", (byte) 1));
		tags.put("HandItems", new ListTag("HandItems", CompoundTag.class, List.of(new CompoundTag("", Map.of()), stack("minecraft:shield"))));
		tags.put("Pose", new CompoundTag("Pose", Map.of("LeftArm", angles("LeftArm", -90, 20, 30))));
		ArmorStandEntity stand = read(tags, false);
		assertNull(stand.getMainHandItem());
		assertEquals("minecraft:shield", stand.getOffHandItem().id);
		assertTrue(stand.isLeftHanded());
		assertTrue(stand.isShowArms());
		assertTrue(stand.getInvisible());
		assertTrue(stand.getNoBasePlate());
		assertEquals(new Vector3f(-90, 20, 30), stand.getLeftArmPose());
		assertEquals(new Vector3f(-15, 0, 10), stand.getRightArmPose());
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void readsExplicitArmVisibilityInLegacyAndModernEntityChunks(boolean modern) throws Exception {
		Map<String, Tag> tags = standTags();
		tags.put("ShowArms", new ByteTag("ShowArms", (byte) 1));
		assertTrue(read(tags, modern).isShowArms());
		tags.put("ShowArms", new ByteTag("ShowArms", (byte) 0));
		assertFalse(read(tags, modern).isShowArms());
	}

	private Map<String, Tag> standTags() {
		Map<String, Tag> tags = new HashMap<>();
		tags.put("id", new StringTag("id", "minecraft:armor_stand"));
		tags.put("Pos", new ListTag("Pos", DoubleTag.class, List.of(new DoubleTag("", 5), new DoubleTag("", 64), new DoubleTag("", 9))));
		tags.put("Rotation", new ListTag("Rotation", FloatTag.class, List.of(new FloatTag("", 90), new FloatTag("", 0))));
		return tags;
	}

	private ListTag angles(String name, float x, float y, float z) {
		return new ListTag(name, FloatTag.class, List.of(new FloatTag("", x), new FloatTag("", y), new FloatTag("", z)));
	}

	private CompoundTag stack(String id) {
		return new CompoundTag("", Map.of("id", new StringTag("id", id)));
	}

	private ArmorStandEntity read(Map<String, Tag> tags, boolean modern) throws Exception {
		int previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(384);
		try {
			ListTag entities = new ListTag("Entities", CompoundTag.class, List.of(new CompoundTag("", tags)));
			if (modern) {
				var root = new CompoundTag("", Map.of("sections", new ListTag("sections", CompoundTag.class, List.of()),
						"block_entities", new ListTag("block_entities", CompoundTag.class, List.of()), "yPos", new IntTag("yPos", -4)));
				return new RawChunk(data(root), data(new CompoundTag("", Map.of("Entities", entities))), null,
						new WorldInfo(Version.VERSION_26_3, false)).getArmorStands().getFirst();
			}
			var level = new CompoundTag("Level", Map.of("Sections", new ListTag("Sections", CompoundTag.class, List.of()), "Entities", entities));
			return new RawChunk(data(new CompoundTag("", Map.of("Level", level))), null,
					new WorldInfo(Version.VERSION_16, false)).getArmorStands().getFirst();
		} finally {
			Minecraft.setChunkHeight(previousHeight);
		}
	}

	private ChunkData data(CompoundTag root) throws Exception {
		var bytes = new ByteArrayOutputStream();
		try (NBTOutputStream out = new NBTOutputStream(bytes)) { out.writeTag(root); }
		return new ChunkData(bytes.toByteArray(), NBTInputStream.Compression.Gzip);
	}
}
