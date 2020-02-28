/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.joml.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class ChorusFlower implements BlockType
{
	private final String name;

	private final SubTexture alive, dead;
	
	public ChorusFlower(String name, SubTexture alive, SubTexture dead)
	{
		this.name = name;
		this.alive = alive;
		this.dead = dead;
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
		final float lightVal = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
		Vector4f colour = new Vector4f(lightVal, lightVal, lightVal, 1.0f);
	
		final int data = rawChunk.getBlockData(x, y, z);		
		SubTexture tex = null;
		if(data == 5)
			tex = dead;
		else
			tex = alive;
		
		final float offSet = 1.0f / 16.0f;
		SubMesh flowerMesh = new SubMesh();
		SubMesh.addBlock(flowerMesh, offSet*2, offSet*2, 0, offSet*12, offSet*12, offSet*16, colour, tex, tex, tex); // north-south
		SubMesh.addBlock(flowerMesh, 0, offSet*2, offSet*2, offSet*16, offSet*12, offSet*12, colour, tex, tex, tex); // east-west
		SubMesh.addBlock(flowerMesh, offSet*2, 0, offSet*2, offSet*12, offSet*16, offSet*12, colour, tex, tex, tex); // up-down
		
		flowerMesh.pushTo(geometry.getMesh(tex.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
	}
}
