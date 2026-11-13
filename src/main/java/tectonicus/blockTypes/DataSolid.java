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
import tectonicus.BlockOcclusion;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.rasteriser.LegacyBlockLighting;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class DataSolid implements BlockType
{
	private final String name;
	
	private SubTexture[] sideTextures;
	private SubTexture[] topTextures;
	
	private boolean alphaTest, transparent;
	
	public DataSolid(String name, SubTexture[] sideTexture, SubTexture[] topTextures, boolean alphaTest, boolean transparent)
	{
		this.name = name;
	
		this.sideTextures = sideTexture;
		this.topTextures = topTextures;
		
		this.alphaTest = alphaTest;
		this.transparent = transparent;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean isSolid()
	{
		return !alphaTest;
	}

	@Override
	public BlockOcclusion getOcclusion() {
		return transparent ? BlockOcclusion.TRANSLUCENT : BlockType.super.getOcclusion();
	}
	
	@Override
	public boolean isWater()
	{
		return false;
	}
	
	@Override
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		LegacyBlockLighting lighting = LegacyBlockLighting.forBlock(world, rawChunk, x, y, z);
		final int data = rawChunk.getBlockData(x, y, z);
		
		int sideIndex = data;
		if (data >= sideTextures.length)
			sideIndex = 0;
		
		int topIndex = data;
		if (data >= topTextures.length)
			topIndex = 0;
		
		SubTexture topTexture = topTextures[topIndex];
		SubTexture sideTexture = sideTextures[sideIndex];
		
		Geometry.MeshType type = alphaTest ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid;
		
		if (transparent)
			type = Geometry.MeshType.Transparent;
		
		Mesh topMesh = geometry.getMesh(topTexture.texture, type);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, type);
		
		Colour4f colour = new Colour4f(1, 1, 1, 1);
		
		BlockUtil.addInteriorTop(world, rawChunk, topMesh, x, y, z, colour, topTexture, lighting);
		BlockUtil.addInteriorBottom(world, rawChunk, topMesh, x, y, z, colour, topTexture, lighting);
		
		BlockUtil.addInteriorNorth(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, lighting);
		BlockUtil.addInteriorSouth(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, lighting);
		BlockUtil.addInteriorEast(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, lighting);
		BlockUtil.addInteriorWest(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, lighting);
	}
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext context, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		LegacyBlockLighting lighting = LegacyBlockLighting.forBlock(context, rawChunk, x, y, z);
		Colour4f colour = new Colour4f(1, 1, 1, 1);
		
		final int data = rawChunk.getBlockData(x, y, z);
		
		int sideIndex = data;
		if (data >= sideTextures.length)
			sideIndex = 0;
		
		int topIndex = data;
		if (data >= topTextures.length)
			topIndex = 0;
		
		SubTexture sideTexture = sideTextures[sideIndex];
		SubTexture topTexture = topTextures[topIndex];
		
		Geometry.MeshType type = alphaTest ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid;
		
		if (transparent)
			type = Geometry.MeshType.Transparent;
		
		Mesh topMesh = geometry.getMesh(topTexture.texture, type);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, type);
		
		BlockUtil.addTop(context, rawChunk, topMesh, x, y, z, colour, topTexture, lighting);
		BlockUtil.addBottom(context, rawChunk, topMesh, x, y, z, colour, topTexture, lighting);
		
		BlockUtil.addNorth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, lighting);
		BlockUtil.addSouth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, lighting);
		BlockUtil.addEast(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, lighting);
		BlockUtil.addWest(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, lighting);
	}
	
}
