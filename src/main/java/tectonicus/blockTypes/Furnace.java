/*
 * Copyright (c) 2019, John Campbell and other contributors.  All rights reserved.
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

import static tectonicus.Version.VERSION_4;

public class Furnace implements BlockType
{
	private final String name;
	
	private final SubTexture topTexture;
	private final SubTexture sideTexture;
	private final SubTexture frontTexture;
	
	public Furnace(String name, SubTexture topTexture, SubTexture sideTexture, SubTexture frontTexture)
	{
		this.name = name;
		
		this.topTexture = topTexture;
		this.sideTexture = sideTexture;

		if (frontTexture.texturePackVersion != VERSION_4)
		{
			final float texel = 1.0f / frontTexture.texture.getHeight();
			final float tile = texel * frontTexture.texture.getWidth();
			this.frontTexture = new SubTexture(frontTexture.texture, frontTexture.u0, frontTexture.v0, frontTexture.u1, frontTexture.v0+tile);
		}
		else
		{
			this.frontTexture = frontTexture;
		}
		
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
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);
		Mesh frontMesh = geometry.getMesh(frontTexture.texture, Geometry.MeshType.Solid);

		Colour4f colour = new Colour4f(1, 1, 1, 1);

		final int data = chunk.getBlockData(x, y, z);
		
		// 0x2: Facing north
		// 0x3: Facing south
		// 0x4: Facing west
		// 0x5: Facing east
		
		SubTexture northTex = data == 0x2 ? frontTexture : sideTexture;
		SubTexture southTex = data == 0x3 ? frontTexture : sideTexture;
		SubTexture eastTex = data == 0x5 ? frontTexture : sideTexture;
		SubTexture westTex = data == 0x4 ? frontTexture : sideTexture;
		
		Mesh northMesh = data == 0x2 ? frontMesh : sideMesh;
		Mesh southMesh = data == 0x3 ? frontMesh : sideMesh;
		Mesh eastMesh = data == 0x5 ? frontMesh : sideMesh;
		Mesh westMesh = data == 0x4 ? frontMesh : sideMesh;
		
		BlockUtil.addTop(world, chunk, topMesh, x, y, z, colour, topTexture, registry);
		BlockUtil.addBottom(world, chunk, topMesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addNorth(world, chunk, westMesh, x, y, z, colour, westTex, registry);
		BlockUtil.addSouth(world, chunk, eastMesh, x, y, z, colour, eastTex, registry);
		BlockUtil.addEast(world, chunk, northMesh, x, y, z, colour, northTex, registry);
		BlockUtil.addWest(world, chunk, southMesh, x, y, z, colour, southTex, registry);
	}
}
