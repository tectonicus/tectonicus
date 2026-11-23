/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import org.junit.jupiter.api.Test;
import org.tectonicus.jnbt.ByteTag;
import org.tectonicus.jnbt.CompoundTag;
import org.tectonicus.jnbt.IntTag;
import org.tectonicus.jnbt.ListTag;
import org.tectonicus.jnbt.NBTCompression;
import org.tectonicus.jnbt.NBTOutputStream;
import org.tectonicus.jnbt.StringTag;
import tectonicus.BlockIds;
import tectonicus.PortalColumn;
import tectonicus.Version;
import tectonicus.chunk.ChunkData;
import tectonicus.raw.BedEntity;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.world.Colors;
import tectonicus.world.WorldInfo;

import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindEntityUtilTest {
	@Test
	void findsTheHeadOfEachBedInScanOrder() {
		RawChunk chunk = new RawChunk();
		chunk.setBlockName(0, 0, 0, "minecraft:stone");
		bed(chunk, 3, 70, 5, "minecraft:red_bed", "head");
		bed(chunk, 3, 70, 6, "minecraft:red_bed", "foot");
		bed(chunk, 9, 20, 1, "minecraft:blue_bed", "head"); // lower, so found first
		bed(chunk, 1, 20, 1, "minecraft:straw_bed", "head"); // not a bed
		chunk.setBlockName(2, 20, 2, "minecraft:white_bed"); // no state at all

		Queue<BedEntity> beds = new ArrayDeque<>();
		FindEntityUtil.findBeds(chunk, beds);

		assertEquals(2, beds.size());
		List<BedEntity> found = List.copyOf(beds);
		assertBed(found.get(0), 9, 20, 1, Colors.byName("blue"));
		assertBed(found.get(1), 3, 70, 5, Colors.byName("red"));
	}

	private static void bed(RawChunk chunk, int x, int y, int z, String name, String part) {
		chunk.setBlockName(x, y, z, name);
		chunk.setBlockState(x, y, z, new BlockProperties(Map.of("part", part, "facing", "north")));
	}

	private static void assertBed(BedEntity bed, int x, int y, int z, Colors color) {
		assertEquals(x, bed.getLocalX());
		assertEquals(y, bed.getLocalY());
		assertEquals(z, bed.getLocalZ());
		assertEquals(x, bed.getX()); // chunk 0,0
		assertEquals(z, bed.getZ());
		assertEquals(color.getId(), bed.getColor());
	}

	@Test
	void findsPortalColumnsInModernChunks() throws Exception {
		List<PortalColumn> columns = FindEntityUtil.findPortalColumns(sectionOf("minecraft:nether_portal"));

		// The lowest section is all portal: one column of 16 blocks per x, z.
		assertEquals(256, columns.size());
		assertEquals(new PortalColumn(0, 0, 0, 15, PortalColumn.Axis.X), columns.getFirst());
	}

	@Test
	void modernChunksWithoutPortalsHaveNoColumns() throws Exception {
		assertTrue(FindEntityUtil.findPortalColumns(sectionOf("minecraft:stone")).isEmpty());
	}

	@Test
	void findsPortalsStoredByIdInOldChunks() {
		RawChunk chunk = new RawChunk();
		chunk.setBlockId(2, 5, 3, BlockIds.PORTAL);
		chunk.setBlockData(2, 5, 3, (byte) 2); // along z

		assertEquals(List.of(new PortalColumn(2, 3, 5, 5, PortalColumn.Axis.Z)), FindEntityUtil.findPortalColumns(chunk));
	}

	@Test
	void oldChunksWithoutPortalsHaveNoColumns() {
		RawChunk chunk = new RawChunk();
		chunk.setBlockId(2, 5, 3, BlockIds.STONE);

		assertTrue(FindEntityUtil.findPortalColumns(chunk).isEmpty());
	}

	/** A 1.21 chunk whose lowest section is filled with one block, with axis=x. */
	private static RawChunk sectionOf(String block) throws Exception {
		CompoundTag paletteEntry = new CompoundTag("", Map.of("Name", new StringTag("Name", block),
				"Properties", new CompoundTag("Properties", Map.of("axis", new StringTag("axis", "x")))));
		CompoundTag section = new CompoundTag("", Map.of("Y", new ByteTag("Y", (byte) 0),
				"block_states", new CompoundTag("block_states", Map.of(
						"palette", new ListTag("palette", CompoundTag.class, List.of(paletteEntry))))));
		CompoundTag root = new CompoundTag("", Map.of(
				"DataVersion", new IntTag("DataVersion", Version.VERSION_21.getDataVersion()),
				"Status", new StringTag("Status", "full"), "yPos", new IntTag("yPos", 0),
				"sections", new ListTag("sections", CompoundTag.class, List.of(section))));
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (NBTOutputStream out = new NBTOutputStream(bytes)) {
			out.writeTag(root);
		}
		return new RawChunk(new ChunkData(bytes.toByteArray(), NBTCompression.GZIP), null,
				new WorldInfo(Version.VERSION_21, false));
	}
}
