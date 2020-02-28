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

public class SolidBlockType implements BlockType
{
	public static final float NORTH_SOUTH_LIGHT_DEC = 0.3f;
	public static final float EAST_WEST_LIGHT_DEC = 0.1f;
	
	private String name;
	private SubTexture sideTexture, topTexture;
	private Colour4f colour;
	private boolean alphaTest;
	
	public SolidBlockType(String name, SubTexture sideTexture, SubTexture topTexture)
	{
		init(name, sideTexture, topTexture, null, false);
	}
	
	public SolidBlockType(String name, SubTexture subTexture)
	{
		init(name, subTexture, subTexture, null, false);
	}
	
	public SolidBlockType(String name, SubTexture subTexture, final boolean alphaTest)
	{
		init(name, subTexture, subTexture, null, alphaTest);
	}
	
	public SolidBlockType(String name, SubTexture side, SubTexture top, final boolean alphaTest)
	{
		init(name, side, top, null, alphaTest);
	}
	
	public SolidBlockType(String name, SubTexture subTexture, Colour4f colour, final boolean alphaTest)
	{
		init(name, subTexture, subTexture, colour, alphaTest);
	}
	
	private void init(String name, SubTexture sideTexture, SubTexture topTexture, Colour4f colour, final boolean alphaTest)
	{
		if (name == null)
			throw new RuntimeException("name is null!");
		if (sideTexture == null)
			throw new RuntimeException("side subtexture is null!");
		if (topTexture == null)
			throw new RuntimeException("top subtexture is null!");
		
		this.name = name;
		this.sideTexture = sideTexture;
		this.topTexture = topTexture;
		this.alphaTest = alphaTest;
		this.colour = colour;
		if (colour == null)
			this.colour = new Colour4f(1, 1, 1, 1);
		
		if(sideTexture.texturePackVersion != VERSION_4)
		{
			final float texel = 1.0f / sideTexture.texture.getHeight();
			final float tile = texel * sideTexture.texture.getWidth();
			this.sideTexture = new SubTexture(sideTexture.texture, sideTexture.u0, sideTexture.v0, sideTexture.u1, sideTexture.v0+tile);
			this.topTexture = new SubTexture(topTexture.texture, topTexture.u0, topTexture.v0, topTexture.u1, topTexture.v0+tile);
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
		return !alphaTest;
	}
	
	@Override
	public boolean isWater()
	{
		return false;
	}
	
	@Override
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh topMesh = geometry.getMesh(topTexture.texture, alphaTest ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, alphaTest ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid);
		
		BlockUtil.addInteriorTop(world, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		BlockUtil.addInteriorBottom(world, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addInteriorNorth(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorSouth(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorEast(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorWest(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
	}
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext context, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh topMesh = geometry.getMesh(topTexture.texture, alphaTest ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, alphaTest ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid);
		
		BlockUtil.addTop(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		BlockUtil.addBottom(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addNorth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addSouth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addEast(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addWest(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
	}
	
}
