/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;
import org.joml.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.chunk.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Hopper implements BlockType
{
	private final String name;
	
	private final SubTexture top, side, inside, insideSide;
	
	public Hopper(String name, SubTexture top, SubTexture side, SubTexture inside)
	{
		this.name = name;
		
		this.top = top;
		this.side = side;
		this.inside = inside;
		
		final float texel = 1.0f / 16.0f;
		this.insideSide = new SubTexture(side.texture, side.u0, side.v0, side.u1, side.v0+texel*6);
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
		Mesh topMesh = geometry.getMesh(top.texture, Geometry.MeshType.AlphaTest);
		Mesh insideMesh = geometry.getMesh(side.texture, Geometry.MeshType.AlphaTest);
		Mesh insideBottomMesh = geometry.getMesh(inside.texture, Geometry.MeshType.AlphaTest);
		SubMesh sideMesh = new SubMesh();
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z, world.getNightLightAdjustment());
		
		Vector4f colour = new Vector4f(lightness, lightness, lightness, 1.0f);
		
		final int data = rawChunk.getBlockData(x, y, z);
		
		final float offSet = 1.0f / 16.0f;
		
		// Top
		MeshUtil.addQuad(topMesh, new Vector3f(x,	y+1, z),
								  new Vector3f(x+1,	y+1, z),
								  new Vector3f(x+1,	y+1, z+1),
								  new Vector3f(x, 	y+1, z+1),
								  colour, top);
		
		// Inside Bottom
		MeshUtil.addQuad(insideBottomMesh, new Vector3f(x,	y+offSet*10, z),
									new Vector3f(x+1,	y+offSet*10, z),
									new Vector3f(x+1,	y+offSet*10, z+1),
									new Vector3f(x, 	y+offSet*10, z+1),
									colour, inside);
		
		SubMesh.addBlock(sideMesh, 0, offSet*10, 0, offSet*16, offSet*6, offSet*16, colour, side, null, side);
		SubMesh.addBlock(sideMesh, offSet*4, offSet*4, offSet*4, offSet*8, offSet*6, offSet*8, colour, side, null, side);
		
		// Inv north
		MeshUtil.addQuad(insideMesh, new Vector3f(x+1-offSet*2, y+1, z),
				   				   new Vector3f(x+1-offSet*2, y+1, z+1),
				   				   new Vector3f(x+1-offSet*2, y+offSet*10,   z+1),
				   				   new Vector3f(x+1-offSet*2, y+offSet*10,   z),
				   				   colour, insideSide);
		
		// Inv south
		MeshUtil.addQuad(insideMesh, new Vector3f(x+offSet*2, y+1, z+1),
								   new Vector3f(x+offSet*2, y+1, z),
								   new Vector3f(x+offSet*2, y+offSet*10,   z),
								   new Vector3f(x+offSet*2, y+offSet*10,   z+1),
								   colour, insideSide);
		
		// Inv west
		MeshUtil.addQuad(insideMesh, new Vector3f(x,		y+1, z+offSet*2),
								   new Vector3f(x+1,	y+1, z+offSet*2),
								   new Vector3f(x+1,	y+offSet*10,   z+offSet*2),
								   new Vector3f(x,		y+offSet*10,   z+offSet*2),
								   colour, insideSide);
		
		// Inv east
		MeshUtil.addQuad(insideMesh, new Vector3f(x+1,	y+1, z+1-offSet*2),
								   new Vector3f(x,		y+1, z+1-offSet*2),
								   new Vector3f(x,		y+offSet*10,   z+1-offSet*2),
								   new Vector3f(x+1,	y+offSet*10,   z+1-offSet*2),
								   colour, insideSide);
		
		
		if(data == 0 || data == 1)
			SubMesh.addBlock(sideMesh, offSet*6, 0, offSet*6, offSet*4, offSet*4, offSet*4, colour, side, null, side);
		else if(data == 2) // Attached to north
			SubMesh.addBlock(sideMesh, offSet*6, offSet*4, 0, offSet*4, offSet*4, offSet*4, colour, side, side, side);
		else if(data == 3) // Attached to south
			SubMesh.addBlock(sideMesh, offSet*6, offSet*4, offSet*12, offSet*4, offSet*4, offSet*4, colour, side, side, side);
		else if(data == 4) // Attached to west
			SubMesh.addBlock(sideMesh, 0, offSet*4, offSet*6, offSet*4, offSet*4, offSet*4, colour, side, side, side);
		else if(data == 5) // Attached to east
			SubMesh.addBlock(sideMesh, offSet*12, offSet*4, offSet*6, offSet*4, offSet*4, offSet*4, colour, side, side, side);
		
		sideMesh.pushTo(geometry.getMesh(side.texture, Geometry.MeshType.AlphaTest), x, y, z, Rotation.None, 0);
	}
}
