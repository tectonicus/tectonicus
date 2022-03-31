/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
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
import tectonicus.rasteriser.MeshUtil;
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
		if (data == 0)
			MeshUtil.addBlock(world, chunk, x, y, z, model, geometry, 90, 0);
		else if (data == 1)
			MeshUtil.addBlock(world, chunk, x, y, z, model, geometry, 270, 0);
		else if (data == 2)
			MeshUtil.addBlock(world, chunk, x, y, z, model, geometry, 0, 0);
		else if (data == 3)
			MeshUtil.addBlock(world, chunk, x, y, z, model, geometry, 0, 180);
		else if (data == 4)
			MeshUtil.addBlock(world, chunk, x, y, z, model, geometry, 0, 270);
		else if (data == 5)
			MeshUtil.addBlock(world, chunk, x, y, z, model, geometry, 0, 90);
	}
}
