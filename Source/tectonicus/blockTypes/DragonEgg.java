/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class DragonEgg implements BlockType
{
	private final String name;
	
	private final SubTexture texture;
	
	public DragonEgg(String name, SubTexture texture)
	{
		this.name = name;
		this.texture = texture;
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Solid);
		
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		
		final float topLight = world.getLight(chunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float northLight = world.getLight(chunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
		final float southLight = world.getLight(chunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
		final float eastLight = world.getLight(chunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
		final float westLight = world.getLight(chunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
		
		// This is a bit of a hack - the actual block light will be really dark since it's actually solid
		final float ownLight = Math.max(topLight, Math.max(northLight, Math.max(southLight, Math.max(eastLight, westLight))));
		
		// Base extends through and does two layers
		BlockUtil.addBlock(mesh, x, y, z, 3, 0, 3, 10, 12, 10, colour, texture, ownLight, northLight, southLight, eastLight, westLight);
		
		// Base
		BlockUtil.addBlock(mesh, x, y, z, 2, 2, 2, 12, 6, 12, colour, texture, ownLight, northLight, southLight, eastLight, westLight);
		
		// Top pyramid
		
		BlockUtil.addBlock(mesh, x, y, z, 4, 12, 4, 8, 2, 8, colour, texture, ownLight, northLight, southLight, eastLight, westLight);
		
		BlockUtil.addBlock(mesh, x, y, z, 5, 14, 5, 6, 1, 6, colour, texture, ownLight, northLight, southLight, eastLight, westLight);
		
		BlockUtil.addBlock(mesh, x, y, z, 6, 15, 6, 4, 1, 4, colour, texture, ownLight, northLight, southLight, eastLight, westLight);
	}
}
