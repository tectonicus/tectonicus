/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */


package tectonicus.world;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.Minecraft;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.BeaconEntity;
import tectonicus.raw.RawChunk;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BeaconActivationTest {
	private int previousHeight;
	private RawChunk origin;
	private final Map<ChunkCoord, RawChunk> chunks = new HashMap<>();

	@BeforeEach
	void setUp() {
		previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(384);
		origin = new RawChunk();
		chunks.put(new ChunkCoord(0, 0), origin);
	}

	@AfterEach
	void restoreHeight() {
		Minecraft.setChunkHeight(previousHeight);
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void reconstructsPyramidDespiteZeroSavedLevels(int levels) {
		BeaconEntity beacon = beacon(8, 100, 8, 0);
		pyramid(beacon, levels, "minecraft:iron_block");
		assertEquals(levels, resolve(beacon));
		assertEquals(0, beacon.getLevels(), "The NBT snapshot is not modified");
	}

	@Test
	void stopsAtFirstIncompleteLayerAndRejectsStalePositiveLevels() {
		BeaconEntity beacon = beacon(8, 100, 8, 4);
		pyramid(beacon, 4, "minecraft:diamond_block");
		put(6, 98, 6, "minecraft:stone");
		assertEquals(1, resolve(beacon));
		put(7, 99, 7, "minecraft:stone");
		assertEquals(0, resolve(beacon));
	}

	@ParameterizedTest
	@ValueSource(strings = {"minecraft:gold_block", "minecraft:diamond_block", "minecraft:emerald_block", "minecraft:netherite_block"})
	void acceptsAllVanillaBaseMaterials(String block) {
		BeaconEntity beacon = beacon(8, 100, 8, 0);
		pyramid(beacon, 1, block);
		assertEquals(1, resolve(beacon));
	}

	@Test
	void resolvesNegativeAndPositiveChunkBoundariesOncePerNeighbor() {
		for (int corner : new int[] {0, 15}) {
			chunks.clear();
			chunks.put(new ChunkCoord(0, 0), origin = new RawChunk());
			BeaconEntity beacon = beacon(corner, 100, corner, 0);
			pyramid(beacon, 4, "minecraft:iron_block");
			AtomicInteger reads = new AtomicInteger();
			assertEquals(4, BeaconActivation.resolve(origin, beacon, coord -> {
				reads.incrementAndGet();
				return chunks.get(coord);
			}, this::blocksBeam));
			assertEquals(3, reads.get());
		}
	}

	@Test
	void missingNeighborDoesNotFallBackToSavedLevels() {
		BeaconEntity beacon = beacon(0, 100, 0, 4);
		pyramid(beacon, 4, "minecraft:iron_block");
		chunks.remove(new ChunkCoord(-1, -1));
		assertEquals(0, resolve(beacon));
	}

	@Test
	void checksEntireBeamColumnIncludingModernBuildHeight() {
		BeaconEntity beacon = beacon(8, 100, 8, 0);
		pyramid(beacon, 4, "minecraft:iron_block");
		put(8, 383, 8, "minecraft:stone");
		assertEquals(0, resolve(beacon));
		put(8, 383, 8, "minecraft:bedrock");
		assertEquals(4, resolve(beacon));
		put(8, 200, 8, "minecraft:red_stained_glass");
		assertEquals(4, resolve(beacon));
		put(8, 200, 8, "minecraft:tinted_glass");
		assertEquals(0, resolve(beacon));
	}

	@Test
	void supportsLegacyBaseIdsAndBedrock() {
		BeaconEntity beacon = beacon(8, 100, 8, 0);
		for (int x = 7; x <= 9; x++)
			for (int z = 7; z <= 9; z++)
				origin.setBlockId(x, 99, z, 42);
		origin.setBlockId(8, 200, 8, 7);
		assertEquals(1, BeaconActivation.resolve(origin, beacon, chunks::get,
				(raw, x, y, z) -> raw.getBlockId(x, y, z) != 0));
	}

	@Test
	void pyramidCannotExtendBelowWorldBottom() {
		BeaconEntity beacon = beacon(8, 1, 8, 4);
		pyramid(beacon, 1, "minecraft:iron_block");
		assertEquals(1, resolve(beacon));
		assertEquals(0, resolve(beacon(8, 0, 8, 4)));
	}

	private int resolve(BeaconEntity beacon) {
		return BeaconActivation.resolve(origin, beacon, chunks::get, this::blocksBeam);
	}

	private boolean blocksBeam(RawChunk chunk, int x, int y, int z) {
		return "minecraft:stone".equals(chunk.getBlockName(x, y, z))
				|| "minecraft:bedrock".equals(chunk.getBlockName(x, y, z));
	}

	private BeaconEntity beacon(int x, int y, int z, int savedLevels) {
		return new BeaconEntity(x, y - 64, z, x, y, z, savedLevels, Effect.SPEED, Effect.REGENERATION);
	}

	private void pyramid(BeaconEntity beacon, int levels, String block) {
		for (int depth = 1; depth <= levels; depth++)
			for (int dx = -depth; dx <= depth; dx++)
				for (int dz = -depth; dz <= depth; dz++)
					put(beacon.getLocalX() + dx, beacon.getLocalY() - depth, beacon.getLocalZ() + dz, block);
	}

	private void put(int x, int y, int z, String name) {
		RawChunk chunk = chunks.computeIfAbsent(new ChunkCoord(Math.floorDiv(x, 16), Math.floorDiv(z, 16)), key -> new RawChunk());
		chunk.setBlockName(Math.floorMod(x, 16), y, Math.floorMod(z, 16), name);
	}
}
