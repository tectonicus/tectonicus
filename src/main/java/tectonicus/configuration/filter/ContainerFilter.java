/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration.filter;

import tectonicus.raw.ContainerEntity;
import tectonicus.raw.ContainerKind;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ContainerFilter {
	private final ContainerFilterType type;
	private final Set<ContainerKind> containers;
	
	public ContainerFilter() {
		this(ContainerFilterType.NONE);
	}
	
	public ContainerFilter(ContainerFilterType type) {
		this(type, null);
	}

	public ContainerFilter(ContainerFilterType type, String types) {
		this.type = type;
		containers = types == null ? EnumSet.allOf(ContainerKind.class) : parseTypes(types);
	}

	private static Set<ContainerKind> parseTypes(String types) {
		Set<ContainerKind> result = EnumSet.noneOf(ContainerKind.class);
		for (String entry : types.split(",", -1)) {
			try {
				result.add(ContainerKind.valueOf(entry.trim().toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid container type '" + entry.trim() + "'. Valid types: "
						+ java.util.Arrays.stream(ContainerKind.values()).map(ContainerKind::getId).collect(Collectors.joining(", ")), e);
			}
		}
		return result;
	}

	public boolean passesFilter(ContainerEntity entity) {
		return containers.contains(entity.getContainerKind()) && passesFilter(entity.isUnopenedContainer());
	}
	
	public boolean passesFilter(boolean unopenedContainer) {
		return type == ContainerFilterType.ALL || type == ContainerFilterType.PLAYER && !unopenedContainer;
	}
	
	@Override
	public String toString() {
		// We need to override this so that Configuration.printActive works
		return type + " " + containers;
	}
}
