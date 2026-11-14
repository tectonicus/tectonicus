/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.DoubleTag;
import org.jnbt.FloatTag;
import org.jnbt.IntArrayTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream.Compression;
import org.jnbt.NBTOutputStream;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tectonicus.Minecraft;
import tectonicus.Version;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;
import tectonicus.chunk.ChunkData;
import tectonicus.raw.BlockEntity;
import tectonicus.raw.CushionEntity;
import tectonicus.raw.RawChunk;
import tectonicus.util.Vector3l;
import tectonicus.world.WorldInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubsetEntityFilterTest {
	private static final int Z = 8;

	static Stream<WorldSubset> partialSubsets() {
		var circle = new CircularWorldSubset(new Vector3l(-5, 64, Z), 3);
		var rectangle = new RectangularWorldSubset(new Vector3l(-5, 64, Z), 6, 6);
		var union = new UnionWorldSubset(List.of(new UnionWorldSubset.Member("Near", rectangle),
				new UnionWorldSubset.Member("Far", new RectangularWorldSubset(new Vector3l(8, 64, Z), 4, 4))));
		return Stream.of(circle, rectangle, union);
	}

	@ParameterizedTest
	@MethodSource("partialSubsets")
	void keepsOnlyEntitiesInVisibleColumnsOfAPartialChunk(WorldSubset subset) throws IOException {
		RawChunk chunk = load(subset, new ChunkCoord(-1, 0),
				armorStand(-2), armorStand(-1), cushion(-1.75), cushion(-0.75),
				hanging("painting", -2), hanging("painting", -1),
				hanging("glow_item_frame", -2), hanging("glow_item_frame", -1));

		assertEquals(List.of(-2), chunk.getArmorStands().stream().map(BlockEntity::getX).toList());
		assertEquals(List.of(14.25f), chunk.getCushions().stream().map(CushionEntity::localX).toList());
		assertEquals(List.of(-2), chunk.getPaintings().stream().map(BlockEntity::getX).toList());
		assertEquals(List.of(-2), chunk.getItemFrames().stream().map(BlockEntity::getX).toList());
	}

	@Test
	void usesTheAnchorColumnWhenHangingEntitiesAreStoredAcrossAChunkBoundary() throws IOException {
		var subset = new RectangularWorldSubset(new Vector3l(1, 64, Z), 4, 4);
		RawChunk chunk = load(subset, new ChunkCoord(0, 0),
				hanging("painting", -1), hanging("painting", -2),
				hanging("item_frame", -1), hanging("item_frame", -2));

		assertEquals(List.of(-1), chunk.getPaintings().stream().map(BlockEntity::getX).toList());
		assertEquals(List.of(-1), chunk.getItemFrames().stream().map(BlockEntity::getX).toList());
	}

	@Test
	void removesHangingEntitiesAnchoredInAnExclusionAcrossAChunkBoundary() throws IOException {
		var exclusion = new RectangularWorldSubset(new Vector3l(-2, 64, Z), 1, 1);
		var subset = new UnionWorldSubset(List.of(), List.of(exclusion));
		RawChunk chunk = load(subset, new ChunkCoord(0, 0),
				hanging("painting", -1), hanging("painting", -2),
				hanging("item_frame", -1), hanging("item_frame", -2));

		assertEquals(List.of(-1), chunk.getPaintings().stream().map(BlockEntity::getX).toList());
		assertEquals(List.of(-1), chunk.getItemFrames().stream().map(BlockEntity::getX).toList());
	}

	private static RawChunk load(WorldSubset subset, ChunkCoord coord, CompoundTag... entities) throws IOException {
		ChunkData blocks = data(Map.of("sections", new ListTag("sections", CompoundTag.class, List.of()),
				"block_entities", new ListTag("block_entities", CompoundTag.class, List.of()),
				"xPos", new IntTag("xPos", (int) coord.x), "zPos", new IntTag("zPos", (int) coord.z),
				"yPos", new IntTag("yPos", -4)));
		ChunkData entityData = data(Map.of("Entities", new ListTag("Entities", CompoundTag.class, List.of(entities))));
		int previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(384);
		try {
			var chunk = new Chunk(coord, null);
			chunk.loadRaw(blocks, entityData, subset.getBlockFilter(coord), null, new WorldInfo(Version.VERSION_26_3, false));
			return chunk.getRawChunk();
		} finally {
			Minecraft.setChunkHeight(previousHeight);
		}
	}

	private static CompoundTag armorStand(double x) {
		return new CompoundTag("", Map.of("id", new StringTag("id", "minecraft:armor_stand"), "Pos", pos(x),
				"Rotation", new ListTag("Rotation", FloatTag.class, List.of(new FloatTag("", 0), new FloatTag("", 0)))));
	}

	private static CompoundTag cushion(double x) {
		return new CompoundTag("", Map.of("id", new StringTag("id", "minecraft:cushion"), "Pos", pos(x),
				"block_pos", new IntArrayTag("block_pos", new int[] {(int) Math.floor(x), 64, Z})));
	}

	private static CompoundTag hanging(String id, int x) {
		return new CompoundTag("", Map.of("id", new StringTag("id", "minecraft:" + id),
				"Direction", new ByteTag("Direction", (byte) 0), "Facing", new ByteTag("Facing", (byte) 3),
				"block_pos", new IntArrayTag("block_pos", new int[] {x, 64, Z}),
				"variant", new StringTag("variant", "minecraft:kebab")));
	}

	private static ListTag pos(double x) {
		return new ListTag("Pos", DoubleTag.class, List.of(new DoubleTag("", x), new DoubleTag("", 64), new DoubleTag("", Z)));
	}

	private static ChunkData data(Map<String, Tag> tags) throws IOException {
		var bytes = new ByteArrayOutputStream();
		try (NBTOutputStream out = new NBTOutputStream(bytes)) {
			out.writeTag(new CompoundTag("", tags));
		}
		return new ChunkData(bytes.toByteArray(), Compression.Gzip);
	}
}
