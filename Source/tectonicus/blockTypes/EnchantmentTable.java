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

public class EnchantmentTable implements BlockType
{
	private final String name;
	
	private final SubTexture top, side, bottom;
	
	public EnchantmentTable(String name, SubTexture top, SubTexture side, SubTexture bottom)
	{
		this.name = name;
		
		if (side.texturePackVersion != VERSION_4)
		{
			final float topTexel = 1.0f / top.texture.getHeight();
			final float sideTexel = 1.0f / side.texture.getHeight();
			final float topTile = topTexel * top.texture.getWidth();
			final float sideTile = sideTexel * side.texture.getWidth();
			this.top = new SubTexture(top.texture, top.u0, top.v0, top.u1, top.v0+topTile);
			this.side = new SubTexture(side.texture, side.u0, side.v0, side.u1, side.v0+sideTile);
		}
		else
		{
			this.top = top;
			this.side = side;
		}
		
		this.bottom = bottom;
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
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, chunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh topMesh = geometry.getMesh(top.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(side.texture, Geometry.MeshType.AlphaTest);
		Mesh bottomMesh = geometry.getMesh(bottom.texture, Geometry.MeshType.Solid);
		
		final float topLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.NorthSouth);
		final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.EastWest);
		
		final float height = 12.0f / 16.0f;
		
		MeshUtil.addQuad(topMesh, new Vector3f(x,		y+height,	z),
								  new Vector3f(x+1,		y+height,	z),
								  new Vector3f(x+1,		y+height,	z+1),
								  new Vector3f(x,		y+height,	z+1),
								  new Vector4f(topLight, topLight, topLight, 1.0f),
								  top);
		
		MeshUtil.addQuad(bottomMesh,	new Vector3f(x+1,	y,	z),
								  		new Vector3f(x,		y,	z),
								  		new Vector3f(x,		y,	z+1),
								  		new Vector3f(x+1,	y,	z+1),
								  		new Vector4f(topLight, topLight, topLight, 1.0f),
								  		bottom);
		
		MeshUtil.addQuad(sideMesh,	new Vector3f(x,		y+1,	z),
									new Vector3f(x,		y+1,	z+1),
									new Vector3f(x,		y,		z+1),
									new Vector3f(x,		y,		z),
									new Vector4f(northSouthLight, northSouthLight, northSouthLight, 1.0f),
									side);

		MeshUtil.addQuad(sideMesh,	new Vector3f(x+1,	y+1,	z+1),
									new Vector3f(x+1,	y+1,	z),
									new Vector3f(x+1,	y,		z),
									new Vector3f(x+1,	y,		z+1),
									new Vector4f(northSouthLight, northSouthLight, northSouthLight, 1.0f),
									side);

		MeshUtil.addQuad(sideMesh,	new Vector3f(x+1,	y+1,	z),
									new Vector3f(x,		y+1,	z),
									new Vector3f(x,		y,		z),
									new Vector3f(x+1,	y,		z),
									new Vector4f(eastWestLight, eastWestLight, eastWestLight, 1.0f),
									side);

		MeshUtil.addQuad(sideMesh,	new Vector3f(x,		y+1,	z+1),
									new Vector3f(x+1,	y+1,	z+1),
									new Vector3f(x+1,	y,		z+1),
									new Vector3f(x,		y,		z+1),
									new Vector4f(eastWestLight, eastWestLight, eastWestLight, 1.0f),
									side);
	}
}
