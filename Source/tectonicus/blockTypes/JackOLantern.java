/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
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

public class JackOLantern implements BlockType
{
	private final String name;
	
	private final SubTexture topTexture;
	private final SubTexture sideTexture;
	private final SubTexture front;
	
	private Colour4f colour;

	public JackOLantern(String name, SubTexture top, SubTexture side, SubTexture front)
	{
		this.name = name;
		this.topTexture = top;
		this.sideTexture = side;
		
		if (front.texturePackVersion != "1.4")
		{
			final float texel = 1.0f / front.texture.getHeight();
			final float tile = texel * front.texture.getWidth();
			this.front = new SubTexture(front.texture, front.u0, front.v0, front.u1, front.v0+tile);
		}
		else
		{
			this.front = front;
		}
		
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
		Mesh mesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh frontMesh = geometry.getMesh(front.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);
		
		final int data = chunk.getBlockData(x, y, z);
		
		// 0x0: Facing west
		// 0x1: Facing north
		// 0x2: Facing east
		// 0x3: Facing south
		
		SubTexture northTex = data == 0x1 ? front : sideTexture;
		SubTexture southTex = data == 0x3 ? front : sideTexture;
		SubTexture eastTex = data == 0x2 ? front : sideTexture;
		SubTexture westTex = data == 0x0 ? front : sideTexture;
		
		Mesh northMesh = data == 0x1 ? frontMesh : sideMesh;
		Mesh southMesh = data == 0x3 ? frontMesh : sideMesh;
		Mesh eastMesh = data == 0x2 ? frontMesh : sideMesh;
		Mesh westMesh = data == 0x0 ? frontMesh : sideMesh; 
		
		BlockUtil.addTop(world, chunk, mesh, x, y, z, colour, topTexture, registry);
		BlockUtil.addBottom(world, chunk, mesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addNorth(world, chunk, northMesh, x, y, z, colour, northTex, registry);
		BlockUtil.addSouth(world, chunk, southMesh, x, y, z, colour, southTex, registry);
		BlockUtil.addEast(world, chunk, eastMesh, x, y, z, colour, eastTex, registry);
		BlockUtil.addWest(world, chunk, westMesh, x, y, z, colour, westTex, registry);
	}
}
