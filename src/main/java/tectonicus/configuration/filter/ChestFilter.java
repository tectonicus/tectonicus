/*
 * Copyright (c) 2025 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration.filter;

public class ChestFilter {
	private final ChestFilterType type;
	
	public ChestFilter() {
		this.type = ChestFilterType.NONE;
	}
	
	public ChestFilter(ChestFilterType type) {
		this.type = type;
	}
	
	public boolean passesFilter(boolean unopenedChest) {
		return type == ChestFilterType.ALL || type == ChestFilterType.PLAYER && !unopenedChest;
	}
	
	@Override
	public String toString() {
		// We need to override this so that MutableConfiguration.printActive works
		return type.toString();
	}
}
