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

import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class GlassPane implements BlockType
{
	private final String name;
	private final int blockId;
	private final SubTexture texture;
	
	public GlassPane(String name, final int blockId, SubTexture texture)
	{
		this.name = name;
		this.blockId = blockId;
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
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		
		final float topLight = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.Top);
		final float northSouthLight = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.NorthSouth);
		final float eastWestLight = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.EastWest);
		
		// Auto-connect to adjacent fences
		
		// Bars are two wide and three high
		
		// North
		boolean hasNorth = world.getBlockId(chunk.getChunkCoord(), x-1, y, z) == blockId;
		boolean hasSouth = world.getBlockId(chunk.getChunkCoord(), x+1, y, z) == blockId;
		boolean hasEast = world.getBlockId(chunk.getChunkCoord(), x, y, z-1) == blockId;
		boolean hasWest = world.getBlockId(chunk.getChunkCoord(), x, y, z+1) == blockId;
		
		if (!hasNorth && !hasSouth && !hasEast && !hasWest)
		{
			hasNorth = hasSouth = hasEast = hasWest = true;
		}
		
		if (hasNorth)
		{
			BlockUtil.addBlock(mesh, x, y, z,	0, 0, 7,
												8, 16, 2,
												colour, texture, topLight, northSouthLight, eastWestLight);
		}
		
		if (hasSouth)
		{
			BlockUtil.addBlock(mesh, x, y, z,	8, 0, 7,
												8, 16, 2,
												colour, texture, topLight, northSouthLight, eastWestLight);
			
		}
		
		if (hasEast)
		{
			BlockUtil.addBlock(mesh, x, y, z,	7, 0, 0,
												2, 16, 8,
												colour, texture, topLight, northSouthLight, eastWestLight);
		}
		
		if (hasWest)
		{
			BlockUtil.addBlock(mesh, x, y, z,	7, 0, 8,
												2, 16, 8,
												colour, texture, topLight, northSouthLight, eastWestLight);
		}
	}
}
