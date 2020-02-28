/*
 * Copyright (c) 2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class ContainerEntity extends BlockEntity
{
	private final String customName;
	private final String lock;
	private final boolean unopenedContainer;
	
	public ContainerEntity(int x, int y, int z, int localX, int localY, int localZ, String customName, String lock, boolean unopenedContainer)
	{
		super(x, y, z, localX, localY, localZ);
		this.customName = customName;
		this.lock = lock;
		this.unopenedContainer = unopenedContainer;
	}
	
	public String getCustomName() { return customName; }
	public String getLock() { return lock; }
	public boolean isUnopenedContainer() { return unopenedContainer; }
}
