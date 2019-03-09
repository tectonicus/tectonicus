/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class BrewingStand implements BlockType
{
	private final String name;
	
	private final SubTexture base;
	private final SubTexture standTile;
	private final SubTexture post;
	private final SubTexture standWithBottle;
	private final SubTexture standWithoutBottle;
	
	public BrewingStand(String name, SubTexture base, SubTexture stand)
	{
		this.name = name;
		
		this.base = base;
		
		if (stand.texturePackVersion != VERSION_4)
		{
			final float texel = 1.0f / stand.texture.getHeight();
			final float tile = texel * stand.texture.getWidth();
			standTile = new SubTexture(stand.texture, stand.u0, stand.v0, stand.u1, stand.v0+tile);
		}
		else
		{
			standTile = stand;
		}
		
		this.post = new SubTexture(standTile.texture, standTile.u0, standTile.v1, standTile.u1, standTile.v0);
		
		final float midU = (standTile.u0 + standTile.u1) / 2.0f;
		this.standWithBottle = new SubTexture(standTile.texture, standTile.u0, standTile.v0, midU, standTile.v1);
		this.standWithoutBottle = new SubTexture(standTile.texture, standTile.u1, standTile.v0, midU, standTile.v1);
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean isSolid()
	{
		return false;
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world,BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh baseMesh = geometry.getMesh(base.texture, Geometry.MeshType.Solid);
		Mesh standMesh = geometry.getMesh(standTile.texture, Geometry.MeshType.Solid);
		Mesh bottleMesh = geometry.getMesh(standTile.texture, Geometry.MeshType.AlphaTest);
		
	//	final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		
		final float topLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float northLight = world.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
		final float southLight = world.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
		final float eastLight = world.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
		final float westLight = world.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
		
	//	Vector4f colour = new Vector4f(lightness, lightness, lightness, 1.0f);
		
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		Vector4f litColour = new Vector4f(topLight, topLight, topLight, 1.0f);
		
		// Base blocks
		BlockUtil.addBlock(baseMesh, x, y, z, 2, 0, 1, 6, 2, 6, colour, base, topLight, northLight, southLight, eastLight, westLight);
		
		BlockUtil.addBlock(baseMesh, x, y, z, 2, 0, 9, 6, 2, 6, colour, base, topLight, northLight, southLight, eastLight, westLight);
		
		BlockUtil.addBlock(baseMesh, x, y, z, 9, 0, 5, 6, 2, 6, colour, base, topLight, northLight, southLight, eastLight, westLight);
		
		// Center post
		BlockUtil.addBlock(standMesh, x, y, z, 7, 0, 7, 2, 14, 2, colour, post, topLight, northLight, southLight, eastLight, westLight);
		
		// Stands
		
		final int data = rawChunk.getBlockData(x, y, z);
		
		SubTexture post1 = ((data & 0x1) > 0 ? standWithBottle : standWithoutBottle);
		SubTexture post2 = ((data & 0x2) > 0 ? standWithBottle : standWithoutBottle);;
		SubTexture post3 = ((data & 0x4) > 0 ? standWithBottle : standWithoutBottle);;
		
		final float one = 1.0f / 16.0f;
		final float two = 2.0f / 16.0f;
		final float seven = 7.0f / 16.0f;
		final float eight = 8.0f / 16.0f;
		final float nine = 9.0f / 16.0f;
		final float fifthteen = 15.0f / 16.0f;
		
		// Along south compass pointer
		MeshUtil.addDoubleSidedQuad(bottleMesh,
									new Vector3f(x+1,		y+1,	z+eight),
									new Vector3f(x+nine,	y+1,	z+eight),
									new Vector3f(x+nine,	y,		z+eight),
									new Vector3f(x+1,		y, 		z+eight),
									litColour, post1);
		
		// Along NE pointer
		MeshUtil.addDoubleSidedQuad(bottleMesh,
									new Vector3f(x+two,		y+1,	z+one),
									new Vector3f(x+eight,	y+1,	z+seven),
									new Vector3f(x+eight,	y, 		z+seven),
									new Vector3f(x+two,		y,		z+one),
									litColour, post2);
		
		// Along NW pointer
		MeshUtil.addDoubleSidedQuad(bottleMesh,
									new Vector3f(x+two,		y+1,	z+fifthteen),
									new Vector3f(x+eight,	y+1,	z+nine),
									new Vector3f(x+eight,	y, 		z+nine),
									new Vector3f(x+two,		y,		z+fifthteen),
									litColour, post3);
	}
}
