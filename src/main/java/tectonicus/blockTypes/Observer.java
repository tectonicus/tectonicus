/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.blockregistry.BlockModel;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;

public class Observer implements BlockType
{
	private final String name;
	private final String stringId;
	
	public Observer(String name, String stringId)
	{
		this.name = name;
		this.stringId = stringId;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean isSolid()
	{
		return true;
	}
	
	@Override
	public boolean isWater()
	{
		return false;
	}
	
	@Override
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		BlockModel model = world.getModelRegistry().getModel(stringId.replace("minecraft:", "block/"));
		final int data = chunk.getBlockData(x, y, z);
		int xRotation = data == 0 ? 90 : data == 1 ? 270 : 0;
		int yRotation = switch (data) {
			case 3 -> 180;
			case 4 -> 270;
			case 5 -> 90;
			default -> 0;
		};
		world.getModelRegistry().getBakedModel(model, xRotation, yRotation, 0, false)
				.createGeometry(x, y, z, world, chunk, geometry);
	}
}
