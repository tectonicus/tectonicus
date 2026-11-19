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
import tectonicus.RegionCoord;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Camera;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Finds the chunks that a camera can see.
 * <p>
 * Chunk geometry only exists between y=0 and the world height, so the search area is the part of the
 * view frustum inside that slab, not the whole frustum. A tile camera's frustum is thousands of blocks
 * deep, while the slab cuts it down to a few hundred blocks, so far fewer regions and chunks are tested.
 * Regions with no file are skipped before any of their chunks are tested.
 * <p>
 * The visibility tests and the result order are the same as the previous full-frustum search.
 */
final class VisibleChunkFinder {
	/**
	 * A chunk box can pass the per-plane frustum test while lying just outside the frustum.
	 * Widen the search area by more than a chunk box so those chunks are still found and tile hashes stay unchanged.
	 */
	static final double SEARCH_MARGIN_BLOCKS = 4.0 * RawChunk.WIDTH;

	private VisibleChunkFinder() {}

	/**
	 * @param camera camera whose world frustum vertices and classify function are used
	 * @param worldHeight height of a chunk column, as used for the chunk bounding boxes
	 * @param inSubset whether a chunk belongs to the rendered world subset
	 * @param regionExists whether a region can contain chunks at all; checked once per region
	 * @param chunkExists whether a chunk is present in its region
	 */
	static List<ChunkCoord> find(Camera camera, int worldHeight, Predicate<ChunkCoord> inSubset,
			Predicate<RegionCoord> regionExists, Predicate<ChunkCoord> chunkExists) {
		List<ChunkCoord> result = new ArrayList<>();
		Vector3d[] vertices = camera.getWorldFrustumVertices();
		double[] area = slabFootprint(vertices, 0, worldHeight);
		if (area == null)
			return result;

		final long minChunkX = (long) Math.floor((area[0] - SEARCH_MARGIN_BLOCKS) / RawChunk.WIDTH);
		final long minChunkZ = (long) Math.floor((area[1] - SEARCH_MARGIN_BLOCKS) / RawChunk.DEPTH);
		final long maxChunkX = (long) Math.floor((area[2] + SEARCH_MARGIN_BLOCKS) / RawChunk.WIDTH);
		final long maxChunkZ = (long) Math.floor((area[3] + SEARCH_MARGIN_BLOCKS) / RawChunk.DEPTH);

		final long regionWidth = RegionCoord.REGION_WIDTH;
		final long regionHeight = RegionCoord.REGION_HEIGHT;
		// The previous search only looked in regions containing a frustum vertex. Stay inside them too:
		// a chunk that only touches the frustum edge from outside them must not start being found.
		long minRegionX = Math.floorDiv(minChunkX, regionWidth);
		long minRegionZ = Math.floorDiv(minChunkZ, regionHeight);
		long maxRegionX = Math.floorDiv(maxChunkX, regionWidth);
		long maxRegionZ = Math.floorDiv(maxChunkZ, regionHeight);
		long vertexMinX = Long.MAX_VALUE, vertexMinZ = Long.MAX_VALUE, vertexMaxX = Long.MIN_VALUE, vertexMaxZ = Long.MIN_VALUE;
		for (Vector3d vertex : vertices) {
			RegionCoord region = RegionCoord.fromWorldCoord(vertex.x, vertex.z);
			vertexMinX = Math.min(vertexMinX, region.x);
			vertexMinZ = Math.min(vertexMinZ, region.z);
			vertexMaxX = Math.max(vertexMaxX, region.x);
			vertexMaxZ = Math.max(vertexMaxZ, region.z);
		}
		minRegionX = Math.max(minRegionX, vertexMinX);
		minRegionZ = Math.max(minRegionZ, vertexMinZ);
		maxRegionX = Math.min(maxRegionX, vertexMaxX);
		maxRegionZ = Math.min(maxRegionZ, vertexMaxZ);

		// Same loop order as before (region x, region z, chunk x, chunk z), so callers see the same list order.
		for (long regionX = minRegionX; regionX <= maxRegionX; regionX++) {
			for (long regionZ = minRegionZ; regionZ <= maxRegionZ; regionZ++) {
				final long baseChunkX = regionX * regionWidth;
				final long baseChunkZ = regionZ * regionHeight;
				if (!isBoxVisible(camera, baseChunkX * RawChunk.WIDTH, baseChunkZ * RawChunk.DEPTH,
						RawChunk.WIDTH * RegionCoord.REGION_WIDTH, worldHeight, RawChunk.DEPTH * RegionCoord.REGION_HEIGHT))
					continue;

				// Checking for the region file loads the whole region, so do it last: only once a chunk
				// of this region is known to be in the subset and visible.
				Boolean regionPresent = null;

				final long fromX = Math.max(minChunkX, baseChunkX);
				final long toX = Math.min(maxChunkX, baseChunkX + regionWidth - 1);
				final long fromZ = Math.max(minChunkZ, baseChunkZ);
				final long toZ = Math.min(maxChunkZ, baseChunkZ + regionHeight - 1);
				chunks:
				for (long chunkX = fromX; chunkX <= toX; chunkX++) {
					for (long chunkZ = fromZ; chunkZ <= toZ; chunkZ++) {
						ChunkCoord chunkCoord = new ChunkCoord(chunkX, chunkZ);
						if (!inSubset.test(chunkCoord)
								|| !isBoxVisible(camera, chunkX * RawChunk.WIDTH, chunkZ * RawChunk.DEPTH, RawChunk.WIDTH, worldHeight, RawChunk.DEPTH))
							continue;
						if (regionPresent == null)
							regionPresent = regionExists.test(new RegionCoord(regionX, regionZ));
						if (!regionPresent)
							break chunks;
						if (chunkExists.test(chunkCoord))
							result.add(chunkCoord);
					}
				}
			}
		}

		return result;
	}

	/**
	 * Same test as {@link tectonicus.util.BoundingBox#isVisible}, including its float arithmetic,
	 * without allocating a box. A box is hidden only when all eight corners are outside the same plane.
	 */
	static boolean isBoxVisible(Camera camera, long originX, long originZ, float width, float height, float depth) {
		final float minX = originX;
		final float minY = 0;
		final float minZ = originZ;
		final float maxX = minX + width;
		final float maxY = minY + height;
		final float maxZ = minZ + depth;

		int outside = camera.classify(minX, minY, minZ);
		if (outside == 0) return true;
		outside &= camera.classify(maxX, minY, minZ);
		if (outside == 0) return true;
		outside &= camera.classify(minX, minY, maxZ);
		if (outside == 0) return true;
		outside &= camera.classify(maxX, minY, maxZ);
		if (outside == 0) return true;
		outside &= camera.classify(minX, maxY, minZ);
		if (outside == 0) return true;
		outside &= camera.classify(maxX, maxY, minZ);
		if (outside == 0) return true;
		outside &= camera.classify(minX, maxY, maxZ);
		if (outside == 0) return true;
		outside &= camera.classify(maxX, maxY, maxZ);
		return outside == 0;
	}

	/**
	 * The x/z bounds {minX, minZ, maxX, maxZ} of the convex hull of {@code vertices} cut to minY..maxY,
	 * or null when the hull lies entirely above or below the slab.
	 * Clipping every vertex pair (not only the hull edges) keeps this independent of the vertex order;
	 * the extra points lie inside the hull and do not widen the bounds.
	 */
	static double[] slabFootprint(Vector3d[] vertices, double minY, double maxY) {
		double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
		boolean found = false;
		for (int i = 0; i < vertices.length; i++) {
			Vector3d a = vertices[i];
			if (a.y >= minY && a.y <= maxY) {
				include(bounds, a.x, a.z);
				found = true;
			}
			for (int j = i + 1; j < vertices.length; j++) {
				Vector3d b = vertices[j];
				found |= includeCrossing(bounds, a, b, minY);
				found |= includeCrossing(bounds, a, b, maxY);
			}
		}
		return found ? bounds : null;
	}

	private static boolean includeCrossing(double[] bounds, Vector3d a, Vector3d b, double y) {
		if ((a.y < y && b.y < y) || (a.y > y && b.y > y) || a.y == b.y)
			return false;
		double t = (y - a.y) / (b.y - a.y);
		include(bounds, a.x + (b.x - a.x) * t, a.z + (b.z - a.z) * t);
		return true;
	}

	private static void include(double[] bounds, double x, double z) {
		bounds[0] = Math.min(bounds[0], x);
		bounds[1] = Math.min(bounds[1], z);
		bounds[2] = Math.max(bounds[2], x);
		bounds[3] = Math.max(bounds[3], z);
	}
}
