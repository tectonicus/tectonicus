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
 *   * Neither the name of 'Tectonicus' nor the names of
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
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Anvil implements BlockType
{	
	private final String name;

	private final SubTexture base, top0, top1, top2;
	
	public Anvil(String name, SubTexture base, SubTexture top0, SubTexture top1, SubTexture top2)
	{
		this.name = name;
		
		this.base = base;
		
		final float texel;
		if (base.texturePackVersion == "1.4")
			texel = 1.0f / 16.0f / 16.0f;
		else
			texel = 1.0f / 16.0f;
		
		this.top0 = new SubTexture(top0.texture, top0.u0+texel*3, top0.v0, top0.u0+texel*12, top0.v1);
		this.top1 = new SubTexture(top1.texture, top1.u0+texel*3, top1.v0, top1.u0+texel*12, top1.v1);
		this.top2 = new SubTexture(top2.texture, top2.u0+texel*3, top2.v0, top2.u0+texel*12, top2.v1);
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
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
		
		final float offSet = 1.0f / 16.0f;
		
		final int data = rawChunk.getBlockData(x, y, z);
		final int direction = data & 0x1;
		final int damage1 = data & 0x4;
		final int damage2 = data & 0x8;
		
		SubTexture top = null;
		if(damage1 > 0)
		{
			// Slightly damaged
			top = top1;
		}
		else if(damage2 > 0)
		{
			// Very damaged
			top = top2;
		}
		else
		{
			// No damage
			top = top0;
		}
		
		
		SubMesh baseMesh = new SubMesh();
		SubMesh topMesh = new SubMesh();

		SubMesh.addBlock(baseMesh, offSet*2, 0, offSet*2, offSet*12, offSet*4, offSet*12, colour, base, base, base);
		
		if(direction > 0)
		{
			//East-West 
			SubMesh.addBlock(baseMesh, offSet*3, offSet*4, offSet*4, offSet*10, offSet, offSet*8, colour, base, base, base);
			SubMesh.addBlock(baseMesh, offSet*4, offSet*5, offSet*6, offSet*8, offSet*5, offSet*4, colour, base, base, base);
			SubMesh.addBlock(baseMesh, 0, offSet*10, offSet*3, offSet*16, offSet*6, offSet*10, colour, base, null, base);
			
			BlockType above = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
			if (!above.isSolid())
			{
				// Top
				topMesh.addQuad(new Vector3f(1, 1, offSet*3),
								new Vector3f(1, 1, 1-offSet*3), 
								new Vector3f(0, 1, 1-offSet*3),
								new Vector3f(0, 1, offSet*3),
								colour, top);
			}
		}
		else
		{
			//North-South
			SubMesh.addBlock(baseMesh, offSet*4, offSet*5, offSet*3, offSet*8, offSet, offSet*10, colour, base, base, base);
			SubMesh.addBlock(baseMesh, offSet*6, offSet*6, offSet*4, offSet*4, offSet*5, offSet*8, colour, base, base, base);
			SubMesh.addBlock(baseMesh, offSet*3, offSet*10, 0, offSet*10, offSet*6, offSet*16, colour, base, null, base);
			
			BlockType above = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
			if (!above.isSolid())
			{
				// Top
				topMesh.addQuad(new Vector3f(1-offSet*3, 1, 1), 
								new Vector3f(offSet*3, 1, 1),
								new Vector3f(offSet*3, 1, 0),
								new Vector3f(1-offSet*3, 1, 0),
								colour, top);
			}
		}
		
		baseMesh.pushTo(geometry.getMesh(base.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
		topMesh.pushTo(geometry.getMesh(top.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
	}
}
