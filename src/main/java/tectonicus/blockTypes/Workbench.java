/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
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
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Workbench implements BlockType
{
	private final String name;
	
	private final SubTexture topTexture;
	private final SubTexture side1Texture;
	private final SubTexture side2Texture;
	
	private Colour4f colour;

	public Workbench(String name, SubTexture top, SubTexture side1, SubTexture side2)
	{
		this.name = name;
		
		this.topTexture = top;
		this.side1Texture = side1;
		this.side2Texture = side2;
		
		colour = new Colour4f(1, 1, 1, 1);
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
		Mesh topMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh side1Mesh = geometry.getMesh(side1Texture.texture, Geometry.MeshType.Solid);
		Mesh side2Mesh = geometry.getMesh(side2Texture.texture, Geometry.MeshType.Solid);
		
		BlockUtil.addTop(world, chunk, topMesh, x, y, z, colour, topTexture, registry);
		BlockUtil.addBottom(world, chunk, topMesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addNorth(world, chunk, side2Mesh, x, y, z, colour, side2Texture, registry);
		BlockUtil.addSouth(world, chunk, side1Mesh, x, y, z, colour, side1Texture, registry);
		BlockUtil.addEast(world, chunk, side2Mesh, x, y, z, colour, side2Texture, registry);
		BlockUtil.addWest(world, chunk, side1Mesh, x, y, z, colour, side1Texture, registry);
	}
}
