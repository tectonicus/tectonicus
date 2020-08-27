/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.util.List;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.blockTypes.BlockModel.BlockElement;

import tectonicus.rasteriser.MeshUtil;

import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;

public class GlazedTerracotta implements BlockType
{
	private final String name;
	private final String stringId;
	
	public GlazedTerracotta(String name, String stringId)
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
		List<BlockElement> elements = world.getModelRegistry().getModel(stringId.replace("minecraft:", "block/")).getElements();
		final int data = chunk.getBlockData(x, y, z);
		MeshUtil.addBlock(world, chunk, x, y, z, elements, geometry, 0, 90*data);
	}
}
