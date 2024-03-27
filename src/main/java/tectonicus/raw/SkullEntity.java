/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.Getter;

@Getter
public class SkullEntity extends BlockEntity
{
        // Used to position the skull when on armor stand
        private float yOffset = 0;
        
	private final int skullType;
	private final int rotation;
	private final String name;
	private final String UUID;
	private final String skinURL;

	public SkullEntity(int x, int y, int z, int localX, int localY, int localZ, String name, String uuid, String skinURL)
	{
		this(x, y, z, localX, localY, localZ, -1, -1, name, uuid, skinURL);
	}
        
        public SkullEntity(int x, int y, int z, int localX, int localY, int localZ, int skullType, int rotation, float yOffset)
	{
		this(x, y, z, localX, localY, localZ, skullType, rotation, "", "", "");
                this.yOffset = yOffset;
	}

	public SkullEntity(int x, int y, int z, int localX, int localY, int localZ, int skullType, int rotation, String name, String uuid, String skinURL)
	{
		super(x, y, z, localX, localY, localZ);
		this.skullType = skullType;
		this.rotation = rotation;
		this.name = name;
		this.UUID = uuid;
		this.skinURL = skinURL;
	}
}
