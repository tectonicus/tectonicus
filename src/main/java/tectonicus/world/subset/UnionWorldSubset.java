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

	public UnionWorldSubset(List<Member> members) {
		if (members.isEmpty()) {
			throw new IllegalArgumentException("A subset union requires at least one shape");
		}
		this.members = List.copyOf(members);
	}

	public List<Member> getMembers() {
		return members;
	}

	@Override
	public RegionIterator createRegionIterator(SaveFormat format, File dimensionDir) {
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
		for (Member member : members) {
			if (member.shape().contains(coord)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsBlock(double x, double z) {
		return members.stream().anyMatch(member -> member.shape().containsBlock(x, z));
	}

	@Override
	public ArrayBlockFilter getBlockFilter(ChunkCoord coord) {
		ArrayBlockFilter filter = new ArrayBlockFilter(coord, this::getBlockFilter);
		for (Member member : members) {
			if (member.shape().contains(coord)) {
				filter.include(member.shape().getBlockFilter(coord));
			}
		}
		return filter;
	}

	@Override
	public Vector3l getOrigin() {
		return members.getFirst().shape().getOrigin();
	}

	@Override
	public boolean needsOrigin() {
		return members.stream().anyMatch(member -> member.shape().needsOrigin());
	}

	@Override
	public void setOrigin(Vector3l origin) {
		for (Member member : members) {
			if (member.shape().needsOrigin()) {
				member.shape().setOrigin(origin);
			}
		}
	}

	@Override
	public String toString() {
		// The tile and view caches depend on geometry. Presentation order and labels do not affect it.
		return "UnionWorldSubset" + members.stream().map(member -> member.shape().toString()).distinct().sorted().toList();
	}
}
