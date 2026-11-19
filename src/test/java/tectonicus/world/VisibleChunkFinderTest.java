/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import tectonicus.RegionCoord;
import tectonicus.chunk.ChunkCoord;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Camera;
import tectonicus.renderer.OrthoCamera;
import tectonicus.util.BoundingBox;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisibleChunkFinderTest {
	private static final int SIZE = 512;
	private static final Rasteriser RASTERISER = (Rasteriser) Proxy.newProxyInstance(
			Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class}, (proxy, method, args) -> null);

	/** Tile hashes are built from this list, so it must not change: same chunks in the same order. */
	@Test
	void findsTheSameChunksInTheSameOrderAsTheFullFrustumSearch() {
		Predicate<RegionCoord> regionExists = region -> Math.floorMod(region.x * 7 + region.z * 3, 5) != 0;
		Predicate<ChunkCoord> chunkExists = chunk -> regionExists.test(RegionCoord.fromChunkCoord(chunk))
				&& Math.floorMod(chunk.x * 31 + chunk.z * 17, 7) != 0;
		Predicate<ChunkCoord> inSubset = chunk -> Math.floorMod(chunk.x + chunk.z * 5, 11) != 0;

		int compared = 0;
		int nonEmpty = 0;
		for (int angle : new int[]{0, 30, 45, 135, 270}) {
			for (int elevation : new int[]{10, 30, 45, 60, 90}) {
				for (int zoom : new int[]{8, 32, 128}) {
					for (int height : new int[]{256, 384}) {
						for (double[] world : new double[][]{{0, 0}, {-100000, 87655}, {1000000, -1012345}}) {
							OrthoCamera camera = new OrthoCamera(RASTERISER, SIZE, SIZE);
							camera.lookAt(world[0], 64, world[1], zoom, radians(angle), radians(elevation));
							int baseX = (int) Math.floor(camera.projectd(world[0], 64, world[1]).x / SIZE);
							int baseY = (int) Math.floor(camera.projectd(world[0], 64, world[1]).y / SIZE);
							for (int dx = 0; dx < 2; dx++) {
								for (int dy = 0; dy < 2; dy++) {
									camera.lookAtTile(baseX + dx, baseY + dy, zoom, radians(angle), radians(elevation));
									List<ChunkCoord> expected = legacyFind(camera, height, inSubset, chunkExists);
									List<ChunkCoord> actual = VisibleChunkFinder.find(camera, height, inSubset, regionExists, chunkExists);
									String context = "angle=" + angle + " elevation=" + elevation + " zoom=" + zoom
											+ " height=" + height + " world=" + world[0] + "," + world[1] + " tile+" + dx + "," + dy;
									assertEquals(toStrings(expected), toStrings(actual), context);
									compared++;
									if (!expected.isEmpty())
										nonEmpty++;
								}
							}
						}
					}
				}
			}
		}
		assertTrue(nonEmpty > compared / 2, "most sample tiles should see chunks");
	}

	@Test
	void skipsChunksOfMissingRegions() {
		OrthoCamera camera = new OrthoCamera(RASTERISER, SIZE, SIZE);
		camera.lookAtTile(0, 0, 32, radians(45), radians(45));
		List<RegionCoord> checked = new ArrayList<>();
		List<ChunkCoord> found = VisibleChunkFinder.find(camera, 384, chunk -> true, region -> {
			checked.add(region);
			return false;
		}, chunk -> {
			throw new AssertionError("chunk lookup in a missing region: " + chunk);
		});
		assertTrue(found.isEmpty());
		assertFalse(checked.isEmpty());
	}

	/** Looking up a region loads its whole file, so only regions with a visible chunk in the subset are looked up. */
	@Test
	void looksUpOnlyRegionsWithVisibleChunksInTheSubset() {
		for (int angle : new int[]{0, 45, 135}) {
			for (int zoom : new int[]{8, 32}) {
				OrthoCamera camera = new OrthoCamera(RASTERISER, SIZE, SIZE);
				camera.lookAtTile(5, -3, zoom, radians(angle), radians(45));
				// All visible chunks in the subset, ignoring region and chunk existence.
				List<ChunkCoord> candidates = VisibleChunkFinder.find(camera, 384, chunk -> true, region -> true, chunk -> true);
				assertFalse(candidates.isEmpty());
				RegionCoord kept = RegionCoord.fromChunkCoord(candidates.get(0));
				Predicate<ChunkCoord> inSubset = chunk -> RegionCoord.fromChunkCoord(chunk).equals(kept);

				List<RegionCoord> checked = new ArrayList<>();
				List<ChunkCoord> found = VisibleChunkFinder.find(camera, 384, inSubset, region -> {
					checked.add(region);
					return true;
				}, chunk -> true);

				assertEquals(List.of(kept), checked);
				assertEquals(candidates.stream().filter(inSubset).toList(), found);
			}
		}
	}

	@Test
	void footprintIsTheFrustumCutToTheWorldHeight() {
		// A vertical box from y=-100 to y=100 over x 0..10, z 0..20, given in an arbitrary vertex order.
		Vector3d[] box = {
				new Vector3d(10, 100, 20), new Vector3d(0, -100, 0), new Vector3d(10, -100, 0), new Vector3d(0, 100, 20),
				new Vector3d(0, -100, 20), new Vector3d(10, 100, 0), new Vector3d(0, 100, 0), new Vector3d(10, -100, 20)
		};
		assertArrayEquals(new double[]{0, 0, 10, 20}, VisibleChunkFinder.slabFootprint(box, 0, 50), 1e-9);

		// A slanted segment from (0, 200, 0) to (100, -200, 0) crosses y=0..100 between x=25 and x=50.
		Vector3d[] slanted = {new Vector3d(0, 200, 0), new Vector3d(100, -200, 0)};
		assertArrayEquals(new double[]{25, 0, 50, 0}, VisibleChunkFinder.slabFootprint(slanted, 0, 100), 1e-9);

		Vector3d[] above = {new Vector3d(0, 500, 0), new Vector3d(100, 600, 0)};
		assertNull(VisibleChunkFinder.slabFootprint(above, 0, 100));
	}

	@Test
	void boxTestMatchesBoundingBox() {
		OrthoCamera camera = new OrthoCamera(RASTERISER, SIZE, SIZE);
		camera.lookAtTile(3, -2, 32, radians(30), radians(45));
		for (long chunkX = -64; chunkX <= 64; chunkX += 3) {
			for (long chunkZ = -64; chunkZ <= 64; chunkZ += 3) {
				BoundingBox box = new BoundingBox(new org.joml.Vector3f(chunkX * RawChunk.WIDTH, 0, chunkZ * RawChunk.DEPTH),
						RawChunk.WIDTH, 384, RawChunk.DEPTH);
				assertEquals(box.isVisible(camera), VisibleChunkFinder.isBoxVisible(camera, chunkX * RawChunk.WIDTH,
						chunkZ * RawChunk.DEPTH, RawChunk.WIDTH, 384, RawChunk.DEPTH), chunkX + "," + chunkZ);
			}
		}
	}

	/** The search used before {@link VisibleChunkFinder}: every chunk of every region touched by the whole frustum. */
	private static List<ChunkCoord> legacyFind(Camera camera, int height, Predicate<ChunkCoord> inSubset, Predicate<ChunkCoord> exists) {
		List<ChunkCoord> result = new ArrayList<>();
		long minX = Long.MAX_VALUE;
		long minZ = Long.MAX_VALUE;
		long maxX = Long.MIN_VALUE;
		long maxZ = Long.MIN_VALUE;
		for (Vector3d corner : camera.getWorldFrustumVertices()) {
			RegionCoord coord = RegionCoord.fromWorldCoord(corner.x, corner.z);
			minX = Math.min(minX, coord.x);
			minZ = Math.min(minZ, coord.z);
			maxX = Math.max(maxX, coord.x);
			maxZ = Math.max(maxZ, coord.z);
		}
		for (long regionX = minX; regionX <= maxX; regionX++) {
			for (long regionZ = minZ; regionZ <= maxZ; regionZ++) {
				BoundingBox regionBounds = new BoundingBox(new org.joml.Vector3f(regionX * RegionCoord.REGION_WIDTH * RawChunk.WIDTH, 0,
						regionZ * RegionCoord.REGION_HEIGHT * RawChunk.DEPTH), RawChunk.WIDTH * RegionCoord.REGION_WIDTH, height,
						RawChunk.DEPTH * RegionCoord.REGION_HEIGHT);
				if (!regionBounds.isVisible(camera))
					continue;
				for (long chunkX = 0; chunkX < RegionCoord.REGION_WIDTH; chunkX++) {
					for (long chunkZ = 0; chunkZ < RegionCoord.REGION_HEIGHT; chunkZ++) {
						ChunkCoord chunkCoord = new ChunkCoord(regionX * RegionCoord.REGION_WIDTH + chunkX,
								regionZ * RegionCoord.REGION_HEIGHT + chunkZ);
						if (inSubset.test(chunkCoord)) {
							BoundingBox chunkBounds = new BoundingBox(new org.joml.Vector3f(chunkCoord.x * RawChunk.WIDTH, 0,
									chunkCoord.z * RawChunk.DEPTH), RawChunk.WIDTH, height, RawChunk.DEPTH);
							if (chunkBounds.isVisible(camera) && exists.test(chunkCoord))
								result.add(chunkCoord);
						}
					}
				}
			}
		}
		return result;
	}

	private static List<String> toStrings(List<ChunkCoord> chunks) {
		List<String> result = new ArrayList<>();
		for (ChunkCoord chunk : chunks)
			result.add(chunk.x + "," + chunk.z);
		return result;
	}

	private static float radians(int degrees) {
		return degrees / 360.0f * (float) Math.PI * 2.0f;
	}
}
