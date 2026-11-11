/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

public enum LightStyle
{
	None("none"),
	Day("day"),
	Night("night"),
	Cave("cave");
	
	private String name;
	
	private LightStyle(String name)
	{
		this.name = name;
	}
	
	public String getName() { return name; }

	public boolean usesOverworldLightmap(boolean nether) {
		return this == Day || (this == Night && !nether);
	}
}
