/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration.filter;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BeaconFilter {
	private final BeaconFilterType type;
	
	public boolean passesFilter(boolean activated) {
		if (type == BeaconFilterType.ALL) {
			return true;
		} else return type == BeaconFilterType.ACTIVATED && activated;
	}
	
	@Override
	public String toString() {
		return type.toString();
	}
}
