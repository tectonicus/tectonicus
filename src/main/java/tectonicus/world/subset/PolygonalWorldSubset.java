/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world.subset;

import lombok.Getter;
import tectonicus.Region;
import tectonicus.SaveFormat;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.RawChunk;
import tectonicus.util.Vector3l;
import tectonicus.world.filter.ArrayBlockFilter;

import java.io.File;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PolygonalWorldSubset implements BoundedWorldSubset {

	public record Point(long x, long z) {}

	private static final long CHUNK_BUFFER = RawChunk.WIDTH + RawChunk.DEPTH;
	private static final Comparator<Point> POINT_ORDER = Comparator.comparingLong(Point::x).thenComparingLong(Point::z);

	private final List<Point> points;
	@Getter
	private final HeightRange heightRange;
	@Getter
	private final Bounds bounds;
	private final Bounds chunkBounds;
	private final long originX;
	private final long originZ;
	private final String horizontalCacheKey;

	public PolygonalWorldSubset(List<Point> points, HeightRange heightRange) {
		List<Point> vertices = List.copyOf(points);
		if (vertices.size() > 1 && vertices.getFirst().equals(vertices.getLast())) {
			vertices = vertices.subList(0, vertices.size() - 1);
		}
		validate(vertices);
		this.points = vertices;
		this.heightRange = heightRange;

		long minX = Long.MAX_VALUE;
		long minZ = Long.MAX_VALUE;
		long maxX = Long.MIN_VALUE;
		long maxZ = Long.MIN_VALUE;
		int first = 0;
		for (int i = 0; i < vertices.size(); i++) {
			Point point = vertices.get(i);
			minX = Math.min(minX, point.x());
			minZ = Math.min(minZ, point.z());
			maxX = Math.max(maxX, point.x());
			maxZ = Math.max(maxZ, point.z());
			if (POINT_ORDER.compare(point, vertices.get(first)) < 0) {
				first = i;
			}
		}
		bounds = new Bounds(minX, minZ, maxX, maxZ);
		chunkBounds = new Bounds(bufferedMinChunk(minX, RawChunk.WIDTH), bufferedMinChunk(minZ, RawChunk.DEPTH),
				bufferedMaxChunk(maxX, RawChunk.WIDTH), bufferedMaxChunk(maxZ, RawChunk.DEPTH));

		// Average without overflowing when both endpoints are far from zero.
		long midX = (minX & maxX) + ((minX ^ maxX) >> 1);
		long midZ = (minZ & maxZ) + ((minZ ^ maxZ) >> 1);
		Point origin = containsColumn(midX, midZ) ? new Point(midX, midZ) : vertices.get(first);
		originX = origin.x();
		originZ = origin.z();
		horizontalCacheKey = createHorizontalCacheKey(first);
	}

	private static long bufferedMinChunk(long block, int chunkSize) {
		return Math.floorDiv(Math.max(block, Long.MIN_VALUE + CHUNK_BUFFER) - CHUNK_BUFFER, chunkSize);
	}

	private static long bufferedMaxChunk(long block, int chunkSize) {
		return Math.floorDiv(Math.min(block, Long.MAX_VALUE - CHUNK_BUFFER) + CHUNK_BUFFER, chunkSize);
	}

	@Override
	public RegionIterator createRegionIterator(SaveFormat format, File dimensionDir) {
		long regionWidth = Region.WIDTH_IN_CHUNKS;
		long minX = Math.floorDiv(chunkBounds.minX(), regionWidth);
		long minZ = Math.floorDiv(chunkBounds.minZ(), regionWidth);
		long maxX = Math.floorDiv(chunkBounds.maxX(), regionWidth);
		long maxZ = Math.floorDiv(chunkBounds.maxZ(), regionWidth);
		return new AllRegionsIterator(dimensionDir, format, file -> {
			var region = Region.extractRegionCoord(file);
			return region != null && between(region.x, minX, maxX) && between(region.z, minZ, maxZ);
		});
	}

	@Override
	public boolean contains(ChunkCoord coord) {
		return between(coord.x, chunkBounds.minX(), chunkBounds.maxX())
				&& between(coord.z, chunkBounds.minZ(), chunkBounds.maxZ());
	}

	@Override
	public boolean containsBlock(double x, double z) {
		return WorldSubset.isBlockCoordinate(x) && WorldSubset.isBlockCoordinate(z)
				&& containsColumn((long) Math.floor(x), (long) Math.floor(z));
	}

	@Override
	public boolean containsColumn(long x, long z) {
		if (!between(x, bounds.minX(), bounds.maxX()) || !between(z, bounds.minZ(), bounds.maxZ())) {
			return false;
		}
		boolean inside = false;
		Point a = points.getLast();
		for (Point b : points) {
			if (between(z, a.z(), b.z())) {
				int side = orientation(a, b, x, z);
				if (side == 0 && between(x, a.x(), b.x())) {
					return true;
				}
				// Count crossings on one side of the ray and count a shared vertex only once.
				if ((a.z() > z) != (b.z() > z) && (side > 0) == (b.z() > a.z())) {
					inside = !inside;
				}
			}
			a = b;
		}
		return inside;
	}

	@Override
	public ArrayBlockFilter getBlockFilter(ChunkCoord coord) {
		ArrayBlockFilter filter = new ArrayBlockFilter(coord, this::getBlockFilter);
		if (!contains(coord)) {
			return filter;
		}
		long startX = coord.x * RawChunk.WIDTH;
		long startZ = coord.z * RawChunk.DEPTH;
		for (int x = 0; x < RawChunk.WIDTH; x++) {
			for (int z = 0; z < RawChunk.DEPTH; z++) {
				filter.set(x, z, containsColumn(startX + x, startZ + z));
			}
		}
		return filter;
	}

	@Override
	public RawChunk.BlockVisibility visibility(ChunkCoord coord) {
		ArrayBlockFilter filter = getBlockFilter(coord);
		return (x, y, z) -> heightRange.contains(y) && filter.containsColumn(x, z);
	}

	@Override
	public Vector3l getOrigin() {
		return new Vector3l(originX, 64, originZ);
	}

	@Override
	public void setOrigin(Vector3l origin) {
		// Absolute vertices fix the polygon and its initial view position.
	}

	@Override
	public String horizontalCacheKey() {
		return horizontalCacheKey;
	}

	@Override
	public String toString() {
		return horizontalCacheKey + heightRange.cacheSuffix();
	}

	private String createHorizontalCacheKey(int first) {
		int size = points.size();
		int forward = (first + 1) % size;
		int backward = Math.floorMod(first - 1, size);
		int step = POINT_ORDER.compare(points.get(forward), points.get(backward)) < 0 ? 1 : -1;
		StringBuilder key = new StringBuilder("PolygonalWorldSubset[");
		for (int i = 0, index = first; i < size; i++, index = Math.floorMod(index + step, size)) {
			Point point = points.get(index);
			key.append('(').append(point.x()).append(',').append(point.z()).append(')');
		}
		return key.append(']').toString();
	}

	private static void validate(List<Point> points) {
		int size = points.size();
		if (size < 3) {
			throw new IllegalArgumentException("at least three distinct points are required");
		}
		Set<Point> seen = new HashSet<>();
		for (int i = 0; i < size; i++) {
			if (!seen.add(points.get(i))) {
				throw new IllegalArgumentException("point " + (i + 1) + " repeats an earlier point");
			}
		}
		Point first = points.getFirst();
		Point second = points.get(1);
		if (points.stream().allMatch(point -> orientation(first, second, point.x(), point.z()) == 0)) {
			throw new IllegalArgumentException("polygon has zero area");
		}
		for (int i = 0; i < size; i++) {
			Point a = points.get(i);
			Point b = points.get((i + 1) % size);
			Point c = points.get((i + 2) % size);
			if (orientation(a, b, c.x(), c.z()) == 0 && (onSegment(a, b, c) || onSegment(b, c, a))) {
				throw new IllegalArgumentException("edges overlap at point " + ((i + 1) % size + 1));
			}
			for (int j = i + 2; j < size - (i == 0 ? 1 : 0); j++) {
				if (segmentsIntersect(a, b, points.get(j), points.get((j + 1) % size))) {
					throw new IllegalArgumentException("edges starting at points " + (i + 1) + " and " + (j + 1) + " intersect");
				}
			}
		}
	}

	private static boolean segmentsIntersect(Point a, Point b, Point c, Point d) {
		int sideC = orientation(a, b, c.x(), c.z());
		int sideD = orientation(a, b, d.x(), d.z());
		int sideA = orientation(c, d, a.x(), a.z());
		int sideB = orientation(c, d, b.x(), b.z());
		return (sideC == 0 && onSegment(a, b, c)) || (sideD == 0 && onSegment(a, b, d))
				|| (sideA == 0 && onSegment(c, d, a)) || (sideB == 0 && onSegment(c, d, b))
				|| (sideC * sideD < 0 && sideA * sideB < 0);
	}

	// Callers have already checked collinearity.
	private static boolean onSegment(Point a, Point b, Point p) {
		return between(p.x(), a.x(), b.x()) && between(p.z(), a.z(), b.z());
	}

	private static boolean between(long value, long a, long b) {
		return value >= Math.min(a, b) && value <= Math.max(a, b);
	}

	private static int orientation(Point a, Point b, long x, long z) {
		try {
			long left = Math.multiplyExact(Math.subtractExact(b.x(), a.x()), Math.subtractExact(z, a.z()));
			long right = Math.multiplyExact(Math.subtractExact(b.z(), a.z()), Math.subtractExact(x, a.x()));
			return Long.compare(left, right);
		} catch (ArithmeticException overflow) {
			BigInteger ax = BigInteger.valueOf(a.x());
			BigInteger az = BigInteger.valueOf(a.z());
			BigInteger left = BigInteger.valueOf(b.x()).subtract(ax).multiply(BigInteger.valueOf(z).subtract(az));
			BigInteger right = BigInteger.valueOf(b.z()).subtract(az).multiply(BigInteger.valueOf(x).subtract(ax));
			return left.compareTo(right);
		}
	}
}
