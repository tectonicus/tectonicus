/*
 * Copyright (c) 2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class SkullEntity extends BlockEntity
{
	private final int skullType;
	private final int rotation;
	private final String name;
	private final String UUID;
	private final String skinURL;
	
	public SkullEntity(int x, int y, int z, int localX, int localY, int localZ, int skullType, int rotation, String name, String uuid, String skinURL)
	{
		super(x, y, z, localX, localY, localZ);
		this.skullType = skullType;
		this.rotation = rotation;
		this.name = name;
		this.UUID = uuid;
		this.skinURL = skinURL;
	}
	
	public int getSkullType() { return skullType; }
	public int getRotation() { return rotation; }
	public String getName() { return name; }
	public String getUUID() { return UUID; }
	public String getSkinURL() { return skinURL; }
}
