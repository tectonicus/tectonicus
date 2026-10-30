/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.Getter;

import java.util.List;

public class ContainerEntity extends BlockEntity {
	private final String customName;
	private final String lock;
	private final boolean unopenedContainer;
	@Getter
	private final ContainerKind containerKind;
	@Getter
	private final String shulkerColor;
	private final String facing;
	private final String type;
	private final List<Item> items;
	
	public ContainerEntity(int x, int y, int z, int localX, int localY, int localZ, String customName, String lock, boolean unopenedContainer, String facing, String type, List<Item> items) {
		this(x, y, z, localX, localY, localZ, customName, lock, unopenedContainer, facing, type, items, false);
	}

	public ContainerEntity(int x, int y, int z, int localX, int localY, int localZ, String customName, String lock, boolean unopenedContainer, String facing, String type, List<Item> items, boolean enderChest) {
		this(x, y, z, localX, localY, localZ, customName, lock, unopenedContainer, facing, type, items,
				enderChest ? ContainerKind.ENDER_CHEST : ContainerKind.CHEST, null);
	}

	public ContainerEntity(int x, int y, int z, int localX, int localY, int localZ, String customName, String lock, boolean unopenedContainer, String facing, String type, List<Item> items, ContainerKind containerKind, String shulkerColor) {
		super(x, y, z, localX, localY, localZ);
		this.customName = customName;
		this.lock = lock;
		this.unopenedContainer = unopenedContainer;
		this.containerKind = containerKind;
		this.shulkerColor = shulkerColor;
		this.facing = facing;
		this.type = type;
		this.items = items;
	}
	
	public String getCustomName() { return customName; }
	public String getLock() { return lock; }
	public boolean isUnopenedContainer() { return unopenedContainer; }
	public boolean isEnderChest() { return containerKind == ContainerKind.ENDER_CHEST; }
	public String getFacing() { return facing; }
	public String getType() { return type; }
	public List<Item> getItems() { return items; }
}
