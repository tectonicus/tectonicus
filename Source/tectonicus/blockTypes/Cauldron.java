/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Cauldron implements BlockType
{
	private final String name;
	
	private final SubTexture top, side, bottom;
	private final SubTexture water;
	
	public Cauldron(String name, SubTexture top, SubTexture side, SubTexture bottom, SubTexture water)
	{
		this.name = name;
		
		this.top = top;
		this.side = side;
		this.bottom = bottom;
		
		this.water = water;
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
		Mesh sideMesh = geometry.getMesh(side.texture, Geometry.MeshType.AlphaTest);
		Mesh bottomMesh = geometry.getMesh(bottom.texture, Geometry.MeshType.AlphaTest);
		Mesh waterMesh = geometry.getMesh(water.texture, Geometry.MeshType.Transparent);
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		
		Vector4f colour = new Vector4f(lightness, lightness, lightness, 1.0f);
		
		// Top
		MeshUtil.addQuad(topMesh, new Vector3f(x,	y+1, z),
								  new Vector3f(x+1,	y+1, z),
								  new Vector3f(x+1,	y+1, z+1),
								  new Vector3f(x, 	y+1, z+1),
								  colour, top);
		
		// Optional water
		final int data = rawChunk.getBlockData(x, y, z);
		if (data > 0)
		{
			float waterLevel = 0.1f;
			if (data == 1)
			{
				waterLevel = 5.0f / 16.0f;
			}
			else if (data == 2)
			{
				waterLevel = 10.0f / 16.0f;
			}
			else if (data == 3)
			{
				waterLevel = 15.0f / 16.0f;
			}
			MeshUtil.addQuad(waterMesh, new Vector3f(x,	y+waterLevel, z),
									  	new Vector3f(x+1,	y+waterLevel, z),
									  	new Vector3f(x+1,	y+waterLevel, z+1),
									  	new Vector3f(x, 	y+waterLevel, z+1),
									  	colour, water);
		}
		
		final float height = 4.0f / 16.0f;
		
		// Bottom
		MeshUtil.addQuad(bottomMesh, new Vector3f(x,	y+height, z),
									 new Vector3f(x+1,	y+height, z),
									 new Vector3f(x+1,	y+height, z+1),
									 new Vector3f(x, 	y+height, z+1),
									 colour, bottom);
		
		// North
		MeshUtil.addQuad(sideMesh, new Vector3f(x, y+1, z),
								   new Vector3f(x, y+1, z+1),
								   new Vector3f(x, y, 	z+1),
								   new Vector3f(x, y, 	z),
								   colour, side);
		
		// South
		MeshUtil.addQuad(sideMesh, new Vector3f(x+1, y+1, 	z+1),
								   new Vector3f(x+1, y+1, 	z),
								   new Vector3f(x+1, y, 	z),
								   new Vector3f(x+1, y, 	z+1),
								   colour, side);
		
		// East
		MeshUtil.addQuad(sideMesh, new Vector3f(x+1, y+1, 	z),
								   new Vector3f(x, 	 y+1, 	z),
								   new Vector3f(x, 	 y, 	z),
								   new Vector3f(x+1, y, 	z),
								   colour, side);
		
		// West
		MeshUtil.addQuad(sideMesh, new Vector3f(x,	 y+1, 	z+1),
								   new Vector3f(x+1, y+1, 	z+1),
								   new Vector3f(x+1, y, 	z+1),
								   new Vector3f(x,	 y, 	z+1),
								   colour, side);
		
		final float offset = 2.0f / 16.0f;
		
		// Inv north
		MeshUtil.addQuad(sideMesh, new Vector3f(x+1-offset, y+1, z),
				   				   new Vector3f(x+1-offset, y+1, z+1),
				   				   new Vector3f(x+1-offset, y,   z+1),
				   				   new Vector3f(x+1-offset, y,   z),
				   				   colour, side);
		
		// Inv south
		MeshUtil.addQuad(sideMesh, new Vector3f(x+offset, y+1, z+1),
								   new Vector3f(x+offset, y+1, z),
								   new Vector3f(x+offset, y,   z),
								   new Vector3f(x+offset, y,   z+1),
								   colour, side);
		
		// Inv west
		MeshUtil.addQuad(sideMesh, new Vector3f(x,		y+1, z+offset),
								   new Vector3f(x+1,	y+1, z+offset),
								   new Vector3f(x+1,	y,   z+offset),
								   new Vector3f(x,		y,   z+offset),
								   colour, side);
		
		// Inv east
		MeshUtil.addQuad(sideMesh, new Vector3f(x+1,	y+1, z+1-offset),
								   new Vector3f(x,		y+1, z+1-offset),
								   new Vector3f(x,		y,   z+1-offset),
								   new Vector3f(x+1,	y,   z+1-offset),
								   colour, side);
	}
}
