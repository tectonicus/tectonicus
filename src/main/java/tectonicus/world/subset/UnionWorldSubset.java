/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world.subset;

import tectonicus.SaveFormat;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.RawChunk;
import tectonicus.util.Vector3l;
import tectonicus.world.filter.ArrayBlockFilter;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class UnionWorldSubset implements WorldSubset {
	public record Member(String name, BoundedWorldSubset shape) {}

	private final List<Member> members;
	private final List<BoundedWorldSubset> exclusions;
	private final HeightRange heightRange;
	private final boolean hasHeightBounds;

	public UnionWorldSubset(List<Member> members) {
		this(members, List.of());
	}

	public UnionWorldSubset(List<Member> members, List<BoundedWorldSubset> exclusions) {
		this(members, exclusions, HeightRange.UNBOUNDED);
	}

	public UnionWorldSubset(List<Member> members, List<BoundedWorldSubset> exclusions, HeightRange heightRange) {
		if (members.isEmpty() && exclusions.isEmpty()) {
			throw new IllegalArgumentException("A combined subset requires at least one shape");
		}
		this.members = List.copyOf(members);
		this.exclusions = List.copyOf(exclusions);
		this.heightRange = heightRange;
		hasHeightBounds = heightRange.isBounded() || this.members.stream().anyMatch(member -> member.shape().hasHeightBounds())
				|| this.exclusions.stream().anyMatch(WorldSubset::hasHeightBounds);
	}

	public List<Member> getMembers() {
		return members;
	}

	public boolean hasExclusions() {
		return !exclusions.isEmpty();
	}

	@Override
	public RegionIterator createRegionIterator(SaveFormat format, File dimensionDir) {
		if (members.isEmpty()) {
			return new AllRegionsIterator(dimensionDir, format);
		}
		return new RegionIterator() {
			private final Iterator<Member> remaining = members.iterator();
			private final Set<File> visited = new HashSet<>();
			private RegionIterator current = remaining.next().shape().createRegionIterator(format, dimensionDir);
			private File next;

			@Override
			public File getBaseDir() {
				return dimensionDir;
			}

			@Override
			public boolean hasNext() {
				while (next == null) {
					if (current.hasNext()) {
						File file = current.next();
						if (file != null && visited.add(file)) {
							next = file;
						}
					} else if (remaining.hasNext()) {
						current = remaining.next().shape().createRegionIterator(format, dimensionDir);
					} else {
						return false;
					}
				}
				return true;
			}

			@Override
			public File next() {
				if (!hasNext()) {
					return null;
				}
				File file = next;
				next = null;
				return file;
			}
		};
	}

	@Override
	public boolean contains(ChunkCoord coord) {
		// Keep candidate chunks for hashing, neighboring geometry and entities anchored across chunk boundaries.
		if (members.isEmpty()) {
			return true;
		}
		for (Member member : members) {
			if (member.shape().contains(coord)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsBlock(double x, double z) {
		if (!Double.isFinite(x) || !Double.isFinite(z)) {
			return false;
		}
		long columnX = (long) Math.floor(x);
		long columnZ = (long) Math.floor(z);
		return (members.isEmpty() || members.stream().anyMatch(member -> member.shape().containsBlock(x, z)))
				&& exclusions.stream().noneMatch(shape -> shape.containsColumn(columnX, columnZ));
	}

	@Override
	public HeightRange getHeightRange() {
		return heightRange;
	}

	@Override
	public boolean hasHeightBounds() {
		return hasHeightBounds;
	}

	@Override
	public boolean containsBlock(double x, double y, double z) {
		if (!hasHeightBounds)
			return containsBlock(x, z);
		if (!heightRange.contains(Math.floor(y)) || !Double.isFinite(x) || !Double.isFinite(z))
			return false;
		return containsBlock((long) Math.floor(x), Math.floor(y), (long) Math.floor(z), null);
	}

	private boolean containsBlock(long x, double blockY, long z, Member member) {
		if (member != null) {
			if (!contains(member.shape(), x, blockY, z))
				return false;
		} else if (!members.isEmpty() && !included(x, blockY, z)) {
			return false;
		}
		for (BoundedWorldSubset shape : exclusions) {
			if (contains(shape, x, blockY, z))
				return false;
		}
		return true;
	}

	private boolean included(long x, double blockY, long z) {
		for (Member member : members) {
			if (contains(member.shape(), x, blockY, z))
				return true;
		}
		return false;
	}

	private static boolean contains(BoundedWorldSubset shape, long x, double blockY, long z) {
		return shape.getHeightRange().contains(blockY) && shape.containsColumn(x, z);
	}

	@Override
	public RawChunk.BlockVisibility visibility(ChunkCoord coord) {
		return visibility(coord, null);
	}

	public RawChunk.BlockVisibility visibility(ChunkCoord coord, Member member) {
		long originX = coord.x * RawChunk.WIDTH;
		long originZ = coord.z * RawChunk.DEPTH;
		return (x, y, z) -> heightRange.contains(y) && containsBlock(originX + x, y, originZ + z, member);
	}

	@Override
	public ArrayBlockFilter getBlockFilter(ChunkCoord coord) {
		ArrayBlockFilter filter = new ArrayBlockFilter(coord, this::getBlockFilter);
		if (members.isEmpty()) {
			filter.includeAll();
		}
		for (Member member : members) {
			if (member.shape().contains(coord)) {
				filter.include(member.shape().getBlockFilter(coord));
			}
		}
		applyExclusions(coord, filter);
		return filter;
	}

	public ArrayBlockFilter getBlockFilter(Member member, ChunkCoord coord) {
		// Cross-chunk anchors need this member's mask with the same exclusions.
		ArrayBlockFilter filter = new ArrayBlockFilter(coord, anchor -> getBlockFilter(member, anchor));
		if (member.shape().contains(coord)) {
			filter.include(member.shape().getBlockFilter(coord));
		}
		applyExclusions(coord, filter);
		return filter;
	}

	private void applyExclusions(ChunkCoord coord, ArrayBlockFilter filter) {
		for (BoundedWorldSubset shape : exclusions) {
			if (shape.contains(coord)) {
				filter.exclude(shape.getBlockFilter(coord));
			}
		}
	}

	@Override
	public Vector3l getOrigin() {
		return members.isEmpty() ? null : members.getFirst().shape().getOrigin();
	}

	@Override
	public boolean needsOrigin() {
		return members.stream().anyMatch(member -> member.shape().needsOrigin())
				|| exclusions.stream().anyMatch(WorldSubset::needsOrigin);
	}

	@Override
	public void setOrigin(Vector3l origin) {
		for (Member member : members) {
			if (member.shape().needsOrigin()) {
				member.shape().setOrigin(origin);
			}
		}
		for (BoundedWorldSubset shape : exclusions) {
			if (shape.needsOrigin()) {
				shape.setOrigin(origin);
			}
		}
	}

	@Override
	public String toString() {
		return cacheKey(Object::toString) + heightRange.cacheSuffix();
	}

	public String horizontalCacheKey() {
		return cacheKey(BoundedWorldSubset::horizontalCacheKey);
	}

	private String cacheKey(Function<BoundedWorldSubset, String> shapeKey) {
		// The tile and view caches depend on geometry. Presentation order and labels do not affect it.
		String included = "UnionWorldSubset" + members.stream().map(member -> shapeKey.apply(member.shape())).distinct().sorted().toList();
		return exclusions.isEmpty() ? included : included + " excluding "
				+ exclusions.stream().map(shapeKey).distinct().sorted().toList();
	}
}
