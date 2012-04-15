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

import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.ChunkCoord;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Chest implements BlockType
{
	private final String name;
	
	private final SubTexture topTexture;
	private final SubTexture sideTexture;
	private final SubTexture frontTexture;
	
	private final SubTexture doubleSide0, doubleSide1;
	private final SubTexture doubleFront0, doubleFront1;
	
	private Colour4f colour;

	public Chest(String name,
					SubTexture top, SubTexture side, SubTexture front,
					SubTexture doubleSide0, SubTexture doubleSide1,
					SubTexture doubleFront0, SubTexture doubleFront1)
	{
		this.name = name;
		
		this.topTexture = top;
		this.sideTexture = side;
		this.frontTexture = front;
		
		this.doubleSide0 = doubleSide0;
		this.doubleSide1 = doubleSide1;
		
		this.doubleFront0 = doubleFront0;
		this.doubleFront1 = doubleFront1;
		
		colour = new Colour4f(1, 1, 1, 1);
	}

	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean isSolid()
	{
		return true;
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		
		final int northId = world.getBlockId(chunk.getChunkCoord(), x-1, y, z);
		final int southId = world.getBlockId(chunk.getChunkCoord(), x+1, y, z);
		final int eastId = world.getBlockId(chunk.getChunkCoord(), x, y, z-1);
		final int westId = world.getBlockId(chunk.getChunkCoord(), x, y, z+1);
		
		BlockType northType = world.getBlockType(chunk.getChunkCoord(), x-1, y, z);
		BlockType southType = world.getBlockType(chunk.getChunkCoord(), x+1, y, z);
		BlockType eastType = world.getBlockType(chunk.getChunkCoord(), x, y, z-1);
		BlockType westType = world.getBlockType(chunk.getChunkCoord(), x, y, z+1);
		
		final boolean chestNorth = northId == BlockIds.CHEST;
		final boolean chestSouth = southId == BlockIds.CHEST;
		final boolean chestEast = eastId == BlockIds.CHEST;
		final boolean chestWest = westId == BlockIds.CHEST;
		
		// Default everything to the side textures
		SubTexture northTex = sideTexture;
		SubTexture southTex = sideTexture;
		SubTexture eastTex = sideTexture;
		SubTexture westTex = sideTexture;
		
		if (chestNorth || chestSouth || chestEast || chestWest)
		{
			// Double chest!
			// We either can run north-south or east-west
			
			if (chestNorth)
			{
				// North-south, this south
				// face east if any blocks west, otherwise face west
				if (isSolid(chunk.getChunkCoord(), x, y, z+1, world, registry)
					|| isSolid(chunk.getChunkCoord(), x-1, y, z+1, world, registry))
				{
					// face east
					eastTex = doubleFront0;
					westTex = doubleSide1;
				}
				else
				{
					// face west
					westTex = doubleFront1;
					eastTex = doubleSide0;
				}
			}
			else if (chestSouth)
			{
				// North-south, this north
				// face east if any blocks west, otherwise face west
				if (isSolid(chunk.getChunkCoord(), x, y, z+1, world, registry)
					|| isSolid(chunk.getChunkCoord(), x-1, y, z+1, world, registry))
				{
					// face east
					eastTex = doubleFront1;
					westTex = doubleSide0;
				}
				else
				{
					// face west
					westTex = doubleFront0;
					eastTex = doubleSide1;
				}
			}
			else if (chestEast)
			{
				// East-west, this west
				// face north if any blocks south, otherwise face south
				if (isSolid(chunk.getChunkCoord(), x+1, y, z, world, registry)
					|| isSolid(chunk.getChunkCoord(), x+1, y, z-1, world, registry))
				{
					// face north
					northTex = doubleFront1;
					southTex = doubleSide0;	
				}
				else
				{
					// face south
					southTex = doubleFront0;
					northTex = doubleSide1;
				}
			}
			else
			{
				// East-west, this east
				// face north if any blocks south, otherwise face south
				if (isSolid(chunk.getChunkCoord(), x+1, y, z, world, registry)
					|| isSolid(chunk.getChunkCoord(), x+1, y, z+1, world, registry))
				{
					// face north
					northTex = doubleFront0;
					southTex = doubleSide1;	
				}
				else
				{
					// face south
					southTex = doubleFront1;
					northTex = doubleSide0;
				}
			}
		}
		else
		{
			// Single chest
			// Direction changes based on surrounding blocks
			int numSolid = 0;
			if (northType.isSolid())
				numSolid++;
			if (southType.isSolid())
				numSolid++;
			if (eastType.isSolid())
				numSolid++;
			if (westType.isSolid())
				numSolid++;
			
			if (numSolid == 4)
			{
				// Don't really care which is the front face, all hidden
			}
			else if (numSolid == 3)
			{
				// Faces the direction which isn't covered
				
				if (!northType.isSolid())
					northTex = frontTexture;
				else if (!southType.isSolid())
					southTex = frontTexture;
				else if (!eastType.isSolid())
					eastTex = frontTexture;
				else if (!westType.isSolid())
					westTex = frontTexture;
			}
			else if (numSolid == 2)
			{
				// Have to hard code all possibilities since MC logic seems a bit weird
				
				if (northType.isSolid() && southType.isSolid())
				{
					westTex = frontTexture;
				}
				else if (eastType.isSolid() && westType.isSolid())
				{
					// Front doesn't show, oddly!
					// Probably a MC bug, expect this to get fixed sometime
				}
				else if (northType.isSolid() && eastType.isSolid())
				{
					southTex = frontTexture;
				}
				else if (eastType.isSolid() && southType.isSolid())
				{
					northTex = frontTexture;
				}
				else if (southType.isSolid() && westType.isSolid())
				{
					northTex = frontTexture;
				}
				else if (westType.isSolid() && northType.isSolid())
				{
					southTex = frontTexture;
				}
			}
			else if (numSolid == 1)
			{
				// Faces away from a single solid block
				
				if (northType.isSolid())
					southTex = frontTexture;
				else if (southType.isSolid())
					northTex = frontTexture;
				else if (eastType.isSolid())
					westTex = frontTexture;
				else if (westType.isSolid())
					eastTex = frontTexture;
			}
			else
			{
				// Default to facing west
				westTex = frontTexture;
			}
		}
		
		// Top is always the same
		BlockUtil.addTop(world, chunk, mesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addNorth(world, chunk, mesh, x, y, z, colour, northTex, registry);
		BlockUtil.addSouth(world, chunk, mesh, x, y, z, colour, southTex, registry);
		BlockUtil.addEast(world, chunk, mesh, x, y, z, colour, eastTex, registry);
		BlockUtil.addWest(world, chunk, mesh, x, y, z, colour, westTex, registry);
	}
	
	private static final boolean isSolid(ChunkCoord coord, int x, int y, int z, BlockContext world, BlockTypeRegistry registry)
	{
		BlockType type = world.getBlockType(coord, x, y, z);
		return type.isSolid();
	}
}
