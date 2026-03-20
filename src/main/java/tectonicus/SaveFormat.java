/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

public enum SaveFormat {
	ALPHA("dat"),
	MC_REGION("mcr"),
	ANVIL("mca");
	
	public final String extension;
	
	SaveFormat(String ext) {
		this.extension = ext;
	}
}
