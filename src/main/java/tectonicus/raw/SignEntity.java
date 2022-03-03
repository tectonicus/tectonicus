/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.Getter;

@Getter
public class SignEntity extends BlockEntity
{
	private final String text1, text2, text3, text4;
	private final Integer blockData;
	private final BlockProperties properties;
	private final String color;
	
	public SignEntity(int x, int y, int z, int localX, int localY, int localZ, String text1, String text2, String text3, String text4, Integer data, BlockProperties properties, String color)
	{
		super(x, y, z, localX, localY, localZ);
		this.text1 = text1;
		this.text2 = text2;
		this.text3 = text3;
		this.text4 = text4;
		this.blockData = data;
		this.properties = properties;
		this.color = color;
	}

	public int getBlockData() {
		int data = 0;
		if (blockData != null) {
			data = blockData;
		} else {
			if (properties.containsKey("rotation")) {
				data = Integer.parseInt(properties.get("rotation"));
			}
		}

		return data;
	}
}
