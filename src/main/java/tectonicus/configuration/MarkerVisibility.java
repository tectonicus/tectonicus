/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkerVisibility {
	private boolean spawn = true;
	private boolean players = true;
	private boolean portals = true;
	private boolean signs = true;
	private boolean beds = true;
	private boolean respawnAnchors = true;
	private boolean views = true;
	private boolean containers;
	private boolean beacons = true;
}
