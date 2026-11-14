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
import tectonicus.util.Vector3l;
import tectonicus.world.filter.ArrayBlockFilter;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class UnionWorldSubset implements WorldSubset {
	public record Member(String name, BoundedWorldSubset shape) {}

	private final List<Member> members;
	private final List<BoundedWorldSubset> exclusions;

	public UnionWorldSubset(List<Member> members) {
		this(members, List.of());
	}

	public UnionWorldSubset(List<Member> members, List<BoundedWorldSubset> exclusions) {
		if (members.isEmpty() && exclusions.isEmpty()) {
			throw new IllegalArgumentException("A combined subset requires at least one shape");
		}
		this.members = List.copyOf(members);
		this.exclusions = List.copyOf(exclusions);
	}

	public List<Member> getMembers() {
		return members;
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
		// The tile and view caches depend on geometry. Presentation order and labels do not affect it.
		String included = "UnionWorldSubset" + members.stream().map(member -> member.shape().toString()).distinct().sorted().toList();
		return exclusions.isEmpty() ? included : included + " excluding "
				+ exclusions.stream().map(Object::toString).distinct().sorted().toList();
	}
}
