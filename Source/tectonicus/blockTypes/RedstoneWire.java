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
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class RedstoneWire implements BlockType
{
	private final SubTexture junction;
	private final SubTexture line;
	
	public RedstoneWire(SubTexture offJunction, SubTexture onJunction, SubTexture offLine, SubTexture onLine)
	{
		this.junction = offJunction;
		
		this.line = offLine;
	}
	
	@Override
	public String getName()
	{
		return "Redstone wire";
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
		// Figure out if we're connected to anything N/S/E/W
		
		Mesh mesh = geometry.getMesh(junction.texture, Geometry.MeshType.AlphaTest);
		
		final int data = chunk.getBlockData(x, y, z);
		
		final boolean hasNorthAbove = hasRedstone(x-1, y+1, z, world, chunk);
		final boolean hasSouthAbove = hasRedstone(x+1, y+1, z, world, chunk);
		final boolean hasEastAbove = hasRedstone(x, y+1, z-1, world, chunk);
		final boolean hasWestAbove = hasRedstone(x, y+1, z+1, world, chunk);

		final boolean hasNorthBelow = hasRedstone(x-1, y-1, z, world, chunk);
		final boolean hasSouthBelow = hasRedstone(x+1, y-1, z, world, chunk);
		final boolean hasEastBelow = hasRedstone(x, y-1, z-1, world, chunk);
		final boolean hasWestBelow = hasRedstone(x, y-1, z+1, world, chunk);
		
		final boolean hasNorth = hasRedstone(x-1, y, z, world, chunk) || hasNorthAbove || hasNorthBelow;
		final boolean hasSouth = hasRedstone(x+1, y, z, world, chunk) || hasSouthAbove || hasSouthBelow;
		final boolean hasEast = hasRedstone(x, y, z-1, world, chunk) || hasEastAbove || hasEastBelow;
		final boolean hasWest = hasRedstone(x, y, z+1, world, chunk) || hasWestAbove || hasWestBelow;
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, chunk, x, y, z);
		final float intensity = ((float)data / 16.0f) * lightness;
		
		Vector4f colour = new Vector4f(1.0f * intensity, 0.2f * intensity, 0.2f * intensity, 1);
		
		final float nudge = 0.001f;
		final float actualY = y + nudge;
		
		if ( (hasNorth && hasSouth && hasEast && hasWest)
			|| (!hasNorth && !hasSouth && !hasEast && !hasWest))
		{
			// Full junction
			MeshUtil.addQuad(mesh,	new Vector3f(x,		actualY, z),
									new Vector3f(x+1,	actualY, z),
									new Vector3f(x+1,	actualY, z+1),
									new Vector3f(x,		actualY, z+1), colour, junction);
		}
		else if ( (hasNorth && hasSouth && !hasEast && !hasWest)		// North and south
					|| ((hasNorth ^ hasSouth) && !hasEast && !hasWest))	// Just north or south
		{
			// Single line north-south
			
			MeshUtil.addQuad(mesh,	new Vector3f(x,		actualY, z),
									new Vector3f(x+1,	actualY, z),
									new Vector3f(x+1,	actualY, z+1),
									new Vector3f(x,		actualY, z+1), colour, line);
		}
		else if ((hasEast && hasWest && !hasNorth && !hasSouth) // east and west
				|| ((hasEast ^ hasWest) && !hasNorth && !hasSouth)) // just east or just west
		{
			// Single line east-west
			
			MeshUtil.addQuad(mesh,	new Vector3f(x,		actualY, z+1),
									new Vector3f(x,		actualY, z),
									new Vector3f(x+1,	actualY, z),
									new Vector3f(x+1,	actualY, z+1), colour, line);
		}
		else
		{
			// t-junction
			
			final float one16th = 1.0f / 16.0f;
			final float leftOffset = one16th * 4;
			final float rightOffset = one16th * 12;
			
			final float texel = 1.0f / 16.0f / 16.0f;
			final float leftTexOffset = texel * 4;
			
			SubTexture center = new SubTexture(junction.texture,
												junction.u0 + leftTexOffset, junction.v0 + leftTexOffset,
												junction.u1 - leftTexOffset, junction.v1 - leftTexOffset);
			
			MeshUtil.addQuad(mesh,	new Vector3f(x + leftOffset,	actualY, z + rightOffset),
									new Vector3f(x + leftOffset,	actualY, z + leftOffset),
									new Vector3f(x + rightOffset,	actualY, z + leftOffset),
									new Vector3f(x + rightOffset,	actualY, z + rightOffset), colour, center);
			
			if (hasNorth)
			{
				SubTexture northTex = new SubTexture(junction.texture,
						junction.u0, junction.v0,
						junction.u0 + leftTexOffset, junction.v1);
				
				MeshUtil.addQuad(mesh,	new Vector3f(x,	actualY, z),
										new Vector3f(x + leftOffset,	actualY, z),
										new Vector3f(x + leftOffset,	actualY, z + 1),
										new Vector3f(x,	actualY, z + 1), colour, northTex);
			}
			
			if (hasSouth)
			{
				SubTexture southTex = new SubTexture(junction.texture,
						junction.u1 - leftTexOffset, junction.v0,
						junction.u1, junction.v1);
				
				MeshUtil.addQuad(mesh,	new Vector3f(x + 1 - leftOffset,	actualY, z),
										new Vector3f(x + 1,	actualY, z),
										new Vector3f(x + 1,	actualY, z + 1),
										new Vector3f(x + 1 - leftOffset,	actualY, z + 1), colour, southTex);
			}
			
			if (hasEast)
			{
				SubTexture eastTex = new SubTexture(junction.texture,
						junction.u0, junction.v0,
						junction.u1, junction.v0 + leftTexOffset);
				
				MeshUtil.addQuad(mesh,	new Vector3f(x,	actualY, z),
										new Vector3f(x + 1,	actualY, z),
										new Vector3f(x + 1,	actualY, z + leftOffset),
										new Vector3f(x,	actualY, z + leftOffset), colour, eastTex);
			}
			
			if (hasWest)
			{
				SubTexture westTex = new SubTexture(junction.texture,
						junction.u0, junction.v1 - leftTexOffset,
						junction.u1, junction.v1);
				
				MeshUtil.addQuad(mesh,	new Vector3f(x,	actualY, z + 1 - leftOffset),
										new Vector3f(x + 1,	actualY, z + 1 - leftOffset),
										new Vector3f(x + 1,	actualY, z + 1),
										new Vector3f(x,	actualY, z + 1), colour, westTex);
			}			
		}
		
		// On the sides
		
		if (hasNorthAbove)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x + nudge,	y,		z+1),
									new Vector3f(x + nudge,	y+1,	z+1),
									new Vector3f(x + nudge,	y+1,	z),
									new Vector3f(x + nudge,	y,		z), colour, line);
		}
		
		if (hasSouthAbove)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x + 1 - nudge,	y,		z),
									new Vector3f(x + 1 - nudge,	y+1,	z),
									new Vector3f(x + 1 - nudge,	y+1,	z+1),
									new Vector3f(x + 1 - nudge,	y,		z+1), colour, line);
		}
		
		if (hasEastAbove)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,		y,		z + nudge),
									new Vector3f(x,		y+1,	z + nudge),
									new Vector3f(x+1,	y+1,	z + nudge),
									new Vector3f(x+1,	y,		z + nudge), colour, line);
		}
		
		if (hasWestAbove)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x + 1,	y,		z + 1 - nudge),
									new Vector3f(x + 1,	y + 1,	z + 1 - nudge),
									new Vector3f(x,		y + 1,	z + 1 - nudge),
									new Vector3f(x,		y,		z + 1 - nudge), colour, line);
		}
	}
	
	private static boolean hasRedstone(final int x, final int y, final int z, BlockContext world, RawChunk chunk)
	{
		final int id = world.getBlockId(chunk.getChunkCoord(), x, y, z);
		
		return id == BlockIds.REDSTONE_WIRE
				|| id == BlockIds.REDSTONE_TORCH_ON
				|| id == BlockIds.LEVER
				|| id == BlockIds.STONE_PRESSURE_PLATE
				|| id == BlockIds.WOOD_PRESSURE_PLATE
				|| id == BlockIds.STONE_BUTTON;
	}
}
