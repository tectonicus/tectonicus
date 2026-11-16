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

@Getter
public class SignEntity extends BlockEntity
{
	private final Integer blockData;
	private final BlockProperties properties;
	private final String blockName;
	private final Face front;
	private final Face back;
	
	public SignEntity(int x, int y, int z, int localX, int localY, int localZ, String text1, String text2, String text3, String text4, Integer data, BlockProperties properties, String color)
	{
		this(x, y, z, localX, localY, localZ, text1, text2, text3, text4, data, properties, color, false, List.of());
	}

	public SignEntity(int x, int y, int z, int localX, int localY, int localZ, String text1, String text2, String text3, String text4,
			Integer data, BlockProperties properties, String color, boolean glowing, List<List<SignTextColor>> textColors)
	{
		this(x, y, z, localX, localY, localZ, text1, text2, text3, text4, data, properties, color, glowing, textColors, "");
	}

	public SignEntity(int x, int y, int z, int localX, int localY, int localZ, String text1, String text2, String text3, String text4,
			Integer data, BlockProperties properties, String color, boolean glowing, List<List<SignTextColor>> textColors, String blockName)
	{
		this(x, y, z, localX, localY, localZ, new Face(List.of(text1, text2, text3, text4), color, glowing, textColors),
				null, data, properties, blockName);
	}

	public SignEntity(int x, int y, int z, int localX, int localY, int localZ, Face front, Face back,
			Integer data, BlockProperties properties, String blockName)
	{
		super(x, y, z, localX, localY, localZ);
		this.front = front;
		this.blockData = data;
		this.properties = properties;
		this.blockName = blockName;
		this.back = back;
	}

	public String getText1() { return front.lines().get(0); }
	public String getText2() { return front.lines().get(1); }
	public String getText3() { return front.lines().get(2); }
	public String getText4() { return front.lines().get(3); }
	public String getColor() { return front.color(); }
	public boolean isGlowing() { return front.glowing(); }
	public List<List<SignTextColor>> getTextColors() { return front.textColors(); }

	public record Face(List<String> lines, String color, boolean glowing, List<List<SignTextColor>> textColors) {}

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
