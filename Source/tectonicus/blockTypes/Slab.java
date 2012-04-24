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
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Slab implements BlockType
{
	private final String name;
	
	private SubTexture sideTexture;
	private SubTexture topTexture;
	
	public Slab(String name, SubTexture sideTexture, SubTexture topTexture)
	{
		if (sideTexture == null)
			throw new RuntimeException("side subtexture is null!");
		if (topTexture == null)
			throw new RuntimeException("top subtexture is null!");
		
		this.name = name;
		
		this.sideTexture = sideTexture;
		this.topTexture = topTexture;
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
		final int data = rawChunk.getBlockData(x, y, z);
		final boolean upsidedown = data > 7;
		
		final float halfV = 1.0f / 16.0f / 2.0f;
		final float vOffset = (upsidedown ? 0f : halfV);
		SubTexture halfSideTexture = new SubTexture(this.sideTexture.texture, this.sideTexture.u0, this.sideTexture.v0+vOffset, this.sideTexture.u1, this.sideTexture.v0+halfV+vOffset);
		
		Mesh mesh = geometry.getBaseMesh();
		
		final float yOffset = (upsidedown ? 0.5f : 0f);
		
		final float topLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float bottomLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float northLight = world.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
		final float southLight = world.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
		final float eastLight = world.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
		final float westLight = world.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
		
		final BlockType above = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
		if (!upsidedown || !above.isSolid())
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+0.5f+yOffset,	z),
									new Vector3f(x+1,	y+0.5f+yOffset,	z),
									new Vector3f(x+1,	y+0.5f+yOffset,	z+1),
									new Vector3f(x,		y+0.5f+yOffset,	z+1),
									new Vector4f(topLight, topLight, topLight, 1.0f),
									this.topTexture); 
		}
		
		final BlockType bellow = world.getBlockType(rawChunk.getChunkCoord(), x, y-1, z);
		if (upsidedown || !bellow.isSolid())
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+yOffset,	z),
									new Vector3f(x+1,	y+yOffset,	z),
									new Vector3f(x+1,	y+yOffset,	z+1),
									new Vector3f(x,		y+yOffset,	z+1),
									new Vector4f(bottomLight, bottomLight, bottomLight, 1.0f),
									this.topTexture); 
		}
		
	//	final int northId = rawChunk.getBlockIdClamped(x-1, y, z, BlockIds.AIR);
	//	BlockType north = registry.find(northId);
		final BlockType north = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
		if (!north.isSolid())
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+0.5f+yOffset,	z),
									new Vector3f(x,		y+0.5f+yOffset,	z+1),
									new Vector3f(x,		y+yOffset,		z+1),
									new Vector3f(x,		y+yOffset,		z),
									new Vector4f(northLight, northLight, northLight, 1.0f),
									halfSideTexture); 
		}
		
	//	final int southId = rawChunk.getBlockIdClamped(x+1, y, z, BlockIds.AIR);
	//	BlockType south = registry.find(southId);
		final BlockType south = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
		if (!south.isSolid())
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+1,		y+0.5f+yOffset,		z+1),
									new Vector3f(x+1,		y+0.5f+yOffset,	z),
									new Vector3f(x+1,		y+yOffset,	z),
									new Vector3f(x+1,		y+yOffset,	z+1),
									new Vector4f(southLight, southLight, southLight, 1.0f),
									halfSideTexture); 
		}
		
	//	final int eastId = rawChunk.getBlockIdClamped(x, y, z-1, BlockIds.AIR);
	//	BlockType east = registry.find(eastId);
		final BlockType east = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);
		if (!east.isSolid())
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+0.5f+yOffset,	z),
									new Vector3f(x,		y+0.5f+yOffset,	z),
									new Vector3f(x,		y+yOffset,		z),
									new Vector3f(x+1,	y+yOffset,		z),
									new Vector4f(eastLight, eastLight, eastLight, 1.0f),
									halfSideTexture); 
		}
		
	//	final int westId = rawChunk.getBlockIdClamped(x, y, z+1, BlockIds.AIR);
	//	BlockType west = registry.find(westId);
		final BlockType west = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
		if (!west.isSolid())
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y+0.5f+yOffset,	z+1),
									new Vector3f(x+1,	y+0.5f+yOffset,	z+1),
									new Vector3f(x+1,	y+yOffset,		z+1),
									new Vector3f(x,		y+yOffset,		z+1),
									new Vector4f(westLight, westLight, westLight, 1.0f),
									halfSideTexture); 
		}
	}
	
}
