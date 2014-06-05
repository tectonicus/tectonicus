/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
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
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Plant implements BlockType
{
	private final String name;
	private final int blockId;
	private final SubTexture texture, bottomTexture;
	
	public Plant(String name, final int blockId, SubTexture texture, SubTexture bottomTexture)
	{
		if (texture == null)
			throw new RuntimeException("plant texture is null!");
		
		this.name = name;
		this.blockId = blockId;
		this.texture = texture;
		this.bottomTexture = bottomTexture;
	}
	
	public SubTexture getTexture()
	{
		return texture;
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		final float lightVal = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
		final int data = rawChunk.getBlockData(x, y, z);
		Colour4f baseColour = getColour(x, y, z, data, world, rawChunk);
		Vector4f colour = new Vector4f(baseColour.r * lightVal, baseColour.g * lightVal, baseColour.b * lightVal, baseColour.a);
		
		//Vector4f colour = new Vector4f(lightVal, lightVal, lightVal, 1.0f);
		if(blockId == BlockIds.LARGE_FLOWERS)
		{
			Mesh bottomMesh = geometry.getMesh(bottomTexture.texture, Geometry.MeshType.AlphaTest);
			addPlantGeometry(x, y, z, 0, bottomMesh, colour, bottomTexture);
			addPlantGeometry(x, y, z, 1, mesh, colour, texture);
		}
		else
		{
			addPlantGeometry(x, y, z, 0, mesh, colour, texture);
		}
	}
	
	public static void addPlantGeometry(final float x, final float y, final float z, final float heightOffGround, Mesh mesh, Vector4f colour, SubTexture texture)
	{
		// NE corner to SW corner
		MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1+heightOffGround,	z),
								new Vector3f(x+1,	y+1+heightOffGround,	z+1),
								new Vector3f(x+1,	y+heightOffGround,		z+1),
								new Vector3f(x,		y+heightOffGround,		z),
								colour,
								texture);
	
		// SE corner to NW corner
		MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+1+heightOffGround,	z),
								new Vector3f(x,		y+1+heightOffGround,	z+1),
								new Vector3f(x,		y+heightOffGround,		z+1),
								new Vector3f(x+1,	y+heightOffGround,		z),
								colour,
								texture); 
	
		// SW corner to NE corner
		MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+1+heightOffGround,	z+1),
								new Vector3f(x,		y+1+heightOffGround,	z),
								new Vector3f(x,		y+heightOffGround,		z),
								new Vector3f(x+1,	y+heightOffGround,		z+1),
								colour,
								texture);
		
		// NW corner to SE corner
		MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1+heightOffGround,	z+1),
								new Vector3f(x+1,	y+1+heightOffGround,	z),
								new Vector3f(x+1,	y+heightOffGround,		z),
								new Vector3f(x,		y+heightOffGround,		z+1),
								colour,
								texture); 
	}
	
	private Colour4f getColour(final int x, final int y, final int z, final int data, BlockContext world, RawChunk rawChunk)
	{
		//final int type = data & 0x3;
		
		if (blockId == 175 && (data == 2 || data == 3))
		{
			// Double Tall grass and Large ferns take the biome colour
			
			/*BiomeData biomeData = biomeCache.loadBiomeData(rawChunk.getChunkCoord());
			BiomeData.ColourCoord colourCoord = biomeData.getColourCoord(x, z);
			Colour4f colour = new Colour4f( texturePack.getGrassColour(colourCoord.getX(), colourCoord.getY()) );*/
			Colour4f colour = world.getGrassColour(rawChunk.getChunkCoord(), x, y, z);
			return colour;
			
			//return new Colour4f(1, 1, 1, 1);
		}
		else
		{
			return new Colour4f(1, 1, 1, 1);
		}
	}
	
}
