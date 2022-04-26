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
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.chunk.Chunk;
import tectonicus.Version;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSIONS_9_TO_11;
import static tectonicus.Version.VERSION_4;

public class RedstoneWire implements BlockType
{
	private final SubTexture junction;
	private final SubTexture line;
	private final Version version;

	public RedstoneWire(SubTexture offJunction, SubTexture onJunction, SubTexture offLine, SubTexture onLine)
	{
		version = offJunction.texturePackVersion;
		if (version != VERSION_4)
		{
			final float tile = offJunction.texture.getWidth()/offJunction.texture.getHeight();
			this.junction = new SubTexture(offJunction.texture, offJunction.u0, offJunction.v0, offJunction.u1, offJunction.v0+tile);
			this.line = new SubTexture(offLine.texture, offLine.u0, offLine.v0, offLine.u1, offLine.v0+tile);
		}
		else
		{
			this.junction = offJunction;
			this.line = offLine;
		}
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
		Mesh lineMesh = geometry.getMesh(line.texture, Geometry.MeshType.AlphaTest);
		
		final int data = chunk.getBlockData(x, y, z);
		
		final boolean hasNorthAbove = hasRedstoneOnly(x-1, y+1, z, world, chunk);
		final boolean hasSouthAbove = hasRedstoneOnly(x+1, y+1, z, world, chunk);
		final boolean hasEastAbove = hasRedstoneOnly(x, y+1, z-1, world, chunk);
		final boolean hasWestAbove = hasRedstoneOnly(x, y+1, z+1, world, chunk);

		final boolean hasNorthBelow = hasRedstoneOnly(x-1, y-1, z, world, chunk);
		final boolean hasSouthBelow = hasRedstoneOnly(x+1, y-1, z, world, chunk);
		final boolean hasEastBelow = hasRedstoneOnly(x, y-1, z-1, world, chunk);
		final boolean hasWestBelow = hasRedstoneOnly(x, y-1, z+1, world, chunk);
		
		final boolean hasNorth = hasRedstone(x-1, y, z, world, chunk) || hasNorthAbove || hasNorthBelow;
		final boolean hasSouth = hasRedstone(x+1, y, z, world, chunk) || hasSouthAbove || hasSouthBelow;
		final boolean hasEast = hasRedstone(x, y, z-1, world, chunk) || hasEastAbove || hasEastBelow;
		final boolean hasWest = hasRedstone(x, y, z+1, world, chunk) || hasWestAbove || hasWestBelow;
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, chunk, x, y, z, world.getNightLightAdjustment());
		final float intensity = ((float)data / 16.0f) * lightness;
		
		Vector4f colour = new Vector4f(1.0f * intensity + 0.25f, 0.2f * intensity, 0.2f * intensity, 1);

		if (colour.x > 1.0f)
			colour.x = 1.0f;
		else if (colour.x < 0.25f)
			colour.x = 0.25f;
		
		final float nudge = 0.001f;
		final float actualY = y + nudge;
		final boolean newTexture = version.getNumVersion() >= VERSIONS_9_TO_11.getNumVersion();
		
		if ( (hasNorth && hasSouth && hasEast && hasWest))
		{
			// Full junction
			MeshUtil.addQuad(mesh,	new Vector3f(x,		actualY, z),
									new Vector3f(x+1,	actualY, z),
									new Vector3f(x+1,	actualY, z+1),
									new Vector3f(x,		actualY, z+1), colour, junction);
			
			if(newTexture)
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x,		actualY, z),
											new Vector3f(x+1,	actualY, z),
											new Vector3f(x+1,	actualY, z+1),
											new Vector3f(x,		actualY, z+1), colour, line);
				
				MeshUtil.addQuad(lineMesh,	new Vector3f(x,		actualY, z+1),
											new Vector3f(x,		actualY, z),
											new Vector3f(x+1,	actualY, z),
											new Vector3f(x+1,	actualY, z+1), colour, line);
			}
		}
		else if ( (hasNorth && hasSouth && !hasEast && !hasWest)		// North and south
					|| ((hasNorth ^ hasSouth) && !hasEast && !hasWest))	// Just north or south
		{
			// Single line north-south
			if(newTexture)
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x,		actualY, z+1),
											new Vector3f(x,		actualY, z),
											new Vector3f(x+1,	actualY, z),
											new Vector3f(x+1,	actualY, z+1), colour, line);
			}
			else
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x,		actualY, z),
											new Vector3f(x+1,	actualY, z),
											new Vector3f(x+1,	actualY, z+1),
											new Vector3f(x,		actualY, z+1), colour, line);
			}
		}
		else if ((hasEast && hasWest && !hasNorth && !hasSouth) // east and west
				|| ((hasEast ^ hasWest) && !hasNorth && !hasSouth)) // just east or just west
		{
			// Single line east-west
			
			if(newTexture)
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x,		actualY, z),
											new Vector3f(x+1,	actualY, z),
											new Vector3f(x+1,	actualY, z+1),
											new Vector3f(x,		actualY, z+1), colour, line);
			}
			else
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x,		actualY, z+1),
											new Vector3f(x,		actualY, z),
											new Vector3f(x+1,	actualY, z),
											new Vector3f(x+1,	actualY, z+1), colour, line);
			}
		}
		else
		{
			// t-junction
			
			final float one16th = 1.0f / 16.0f;
			final float leftOffset = one16th * 4;
			final float rightOffset = one16th * 12;
			final float texel;
			if (junction.texturePackVersion == VERSION_4)
				texel = 1.0f / 16.0f / 16.0f;
			else
				texel = 1.0f / junction.texture.getHeight();
			final float leftTexOffset = texel * 4;
			
			SubTexture center = new SubTexture(junction.texture,
												junction.u0 + leftTexOffset, junction.v0 + leftTexOffset,
												junction.u1 - leftTexOffset, junction.v1 - leftTexOffset);
			
			Mesh centerMesh = geometry.getMesh(center.texture, Geometry.MeshType.AlphaTest);
			
			MeshUtil.addQuad(centerMesh,	new Vector3f(x + leftOffset,	actualY, z + leftOffset),
											new Vector3f(x + rightOffset,	actualY, z + leftOffset),
											new Vector3f(x + rightOffset,	actualY, z + rightOffset), 
											new Vector3f(x + leftOffset,	actualY, z + rightOffset), colour, center);
			
			if (hasNorth) //Actually West
			{
				if (newTexture)
				{
					SubTexture northTex = new SubTexture(line.texture,
							line.u0, line.v0,
							line.u1, line.v0 + leftTexOffset);
					
					Mesh northMesh = geometry.getMesh(northTex.texture, Geometry.MeshType.AlphaTest);
					
					MeshUtil.addQuad(northMesh, new Vector3f(x,	actualY, z + 1),	
												new Vector3f(x,	actualY, z),
												new Vector3f(x + leftOffset,	actualY, z),
												new Vector3f(x + leftOffset,	actualY, z + 1), colour, northTex);
				}
				else
				{
					SubTexture northTex = new SubTexture(junction.texture,
							junction.u0, junction.v0,
							junction.u0 + leftTexOffset, junction.v1);
					
					Mesh northMesh = geometry.getMesh(northTex.texture, Geometry.MeshType.AlphaTest);
					
					MeshUtil.addQuad(northMesh,	new Vector3f(x,	actualY, z),
												new Vector3f(x + leftOffset,	actualY, z),
												new Vector3f(x + leftOffset,	actualY, z + 1),
												new Vector3f(x,	actualY, z + 1), colour, northTex);
				}
			}
			
			if (hasSouth) //Actually East
			{
				if (newTexture)
				{
					SubTexture southTex = new SubTexture(line.texture,
							line.u0, line.v1 - leftTexOffset,
							line.u1, line.v1);
					
					Mesh southMesh = geometry.getMesh(southTex.texture, Geometry.MeshType.AlphaTest);
					
					MeshUtil.addQuad(southMesh,	new Vector3f(x + 1 - leftOffset,	actualY, z + 1),
												new Vector3f(x + 1 - leftOffset,	actualY, z),
												new Vector3f(x + 1,	actualY, z),
												new Vector3f(x + 1,	actualY, z + 1), colour, southTex);
				}
				else
				{
					SubTexture southTex = new SubTexture(junction.texture,
							junction.u1 - leftTexOffset, junction.v0,
							junction.u1, junction.v1);
					
					Mesh southMesh = geometry.getMesh(southTex.texture, Geometry.MeshType.AlphaTest);
					
					MeshUtil.addQuad(southMesh,	new Vector3f(x + 1 - leftOffset,	actualY, z),
												new Vector3f(x + 1,	actualY, z),
												new Vector3f(x + 1,	actualY, z + 1),
												new Vector3f(x + 1 - leftOffset,	actualY, z + 1), colour, southTex);
				}
			}
			
			if (hasEast)  //Actually North
			{
				SubTexture eastTex = null;
				if (newTexture)
				{
					eastTex = new SubTexture(line.texture,
							line.u0, line.v0,
							line.u1, line.v0 + leftTexOffset);
				}
				else
				{
					eastTex = new SubTexture(junction.texture,
							junction.u0, junction.v0,
							junction.u1, junction.v0 + leftTexOffset);
				}
				
				Mesh eastMesh = geometry.getMesh(eastTex.texture, Geometry.MeshType.AlphaTest);
				
				MeshUtil.addQuad(eastMesh,	new Vector3f(x,	actualY, z),
											new Vector3f(x + 1,	actualY, z),
											new Vector3f(x + 1,	actualY, z + leftOffset),
											new Vector3f(x,	actualY, z + leftOffset), colour, eastTex);
			}
			
			if (hasWest)  //Actually South
			{
				SubTexture westTex = null;
				if (newTexture)
				{
					westTex = new SubTexture(line.texture,
							line.u0, line.v1 - leftTexOffset,
							line.u1, line.v1);
				}
				else
				{
					westTex = new SubTexture(junction.texture,
							junction.u0, junction.v1 - leftTexOffset,
							junction.u1, junction.v1);
				}
				
				Mesh westMesh = geometry.getMesh(westTex.texture, Geometry.MeshType.AlphaTest);
				
				MeshUtil.addQuad(westMesh,	new Vector3f(x,	actualY, z + 1 - leftOffset),
											new Vector3f(x + 1,	actualY, z + 1 - leftOffset),
											new Vector3f(x + 1,	actualY, z + 1),
											new Vector3f(x,	actualY, z + 1), colour, westTex);
			}			
		}
		
		// On the sides
		
		if (hasNorthAbove) //Actually West
		{
			if(newTexture)
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x + nudge,	y,		z),
											new Vector3f(x + nudge,	y,		z+1),
											new Vector3f(x + nudge,	y+1,	z+1),
											new Vector3f(x + nudge,	y+1,	z), colour, line);
			}
			else
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x + nudge,	y,		z+1),
											new Vector3f(x + nudge,	y+1,	z+1),
											new Vector3f(x + nudge,	y+1,	z),
											new Vector3f(x + nudge,	y,		z), colour, line);
			}
		}
		
		if (hasSouthAbove) //Actually East
		{
			if(newTexture)
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x + 1 - nudge,	y,		z+1),
											new Vector3f(x + 1 - nudge,	y,		z),
											new Vector3f(x + 1 - nudge,	y+1,	z),
											new Vector3f(x + 1 - nudge,	y+1,	z+1), colour, line);
			}
			else
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x + 1 - nudge,	y,		z),
											new Vector3f(x + 1 - nudge,	y+1,	z),
											new Vector3f(x + 1 - nudge,	y+1,	z+1),
											new Vector3f(x + 1 - nudge,	y,		z+1), colour, line);
			}
		}
		
		if (hasEastAbove)  //Actually North
		{
			if(newTexture)
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x+1,	y,		z + nudge),
											new Vector3f(x,		y,		z + nudge),
											new Vector3f(x,		y+1,	z + nudge),
											new Vector3f(x+1,	y+1,	z + nudge), colour, line);
			}
			else
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x,		y,		z + nudge),
											new Vector3f(x,		y+1,	z + nudge),
											new Vector3f(x+1,	y+1,	z + nudge),
											new Vector3f(x+1,	y,		z + nudge), colour, line);
			}
		}
		
		if (hasWestAbove) //Actually South
		{
			if(newTexture)
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x,		y,		z + 1 - nudge),
											new Vector3f(x + 1,	y,		z + 1 - nudge),
											new Vector3f(x + 1,	y + 1,	z + 1 - nudge),
											new Vector3f(x,		y + 1,	z + 1 - nudge), colour, line);
			}
			else
			{
				MeshUtil.addQuad(lineMesh,	new Vector3f(x + 1,	y,		z + 1 - nudge),
											new Vector3f(x + 1,	y + 1,	z + 1 - nudge),
											new Vector3f(x,		y + 1,	z + 1 - nudge),
											new Vector3f(x,		y,		z + 1 - nudge), colour, line);
			}
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
				|| id == BlockIds.STONE_BUTTON
				|| id == BlockIds.WOOD_BUTTON;
	}
	
	private static boolean hasRedstoneOnly(final int x, final int y, final int z, BlockContext world, RawChunk chunk)
	{
		final int id = world.getBlockId(chunk.getChunkCoord(), x, y, z);
		
		return id == BlockIds.REDSTONE_WIRE;
	}
}
