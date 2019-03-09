/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector2f;
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
import tectonicus.util.Colour4f;

import static tectonicus.Version.VERSION_4;

public class Tripwire implements BlockType
{
	private final String name;
	
	private final SubTexture texture;
	private final SubTexture topTexture;
	
	private Colour4f colour;

	public Tripwire(String name, SubTexture texture)
	{
		this.name = name;
		
		this.texture = texture;
		
		final float texel;
		if (texture.texturePackVersion == VERSION_4)
			texel = 1.0f / 16.0f / 16.0f;
		else
			texel = 1.0f / 16.0f;
		
		topTexture = new SubTexture(texture.texture, texture.u0, texture.v0, texture.u1, texture.v0+texel*2);
		
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
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Transparent);
		
		final boolean hasNorth = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1) instanceof Tripwire || 
									world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1) instanceof TripwireHook;
		final boolean hasSouth = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1) instanceof Tripwire ||
									world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1) instanceof TripwireHook;
		final boolean hasEast = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z) instanceof Tripwire ||
									world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z) instanceof TripwireHook;
		final boolean hasWest = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z) instanceof Tripwire ||
									world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z) instanceof TripwireHook;

		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		Vector4f light = new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a);
		
		final float nudge = 1.0f/16.0f;
		final float actualY = y + nudge;

		//North/South tripwire
		if ((!hasNorth && !hasSouth && !hasEast && !hasWest) || hasNorth)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+0.48f,	actualY, z),
									new Vector3f(x+0.52f,	actualY, z),
									new Vector3f(x+0.52f,	actualY, z+0.25f),
									new Vector3f(x+0.48f,	actualY, z+0.25f), 
									light, new Vector2f(topTexture.u0, topTexture.v1), new Vector2f(topTexture.u0, topTexture.v0),new Vector2f(topTexture.u1, topTexture.v0), new Vector2f(topTexture.u1, topTexture.v1));
		}
		
		if ((!hasNorth && !hasSouth && !hasEast && !hasWest) || hasNorth || (hasSouth && !hasEast && !hasWest))
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+0.48f,	actualY, z+0.25f),
									new Vector3f(x+0.52f,	actualY, z+0.25f),
									new Vector3f(x+0.52f,	actualY, z+0.50f),
									new Vector3f(x+0.48f,	actualY, z+0.50f), 
									light, new Vector2f(topTexture.u0, topTexture.v1), new Vector2f(topTexture.u0, topTexture.v0),new Vector2f(topTexture.u1, topTexture.v0), new Vector2f(topTexture.u1, topTexture.v1));
		}
		
		if ((!hasNorth && !hasSouth && !hasEast && !hasWest) || (hasNorth && !hasEast && !hasWest) || hasSouth)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+0.48f,	actualY, z+0.50f),
									new Vector3f(x+0.52f,	actualY, z+0.50f),
									new Vector3f(x+0.52f,	actualY, z+0.75f),
									new Vector3f(x+0.48f,	actualY, z+0.75f), 
									light, new Vector2f(topTexture.u0, topTexture.v1), new Vector2f(topTexture.u0, topTexture.v0),new Vector2f(topTexture.u1, topTexture.v0), new Vector2f(topTexture.u1, topTexture.v1));
		}
		
		if ((!hasNorth && !hasSouth && !hasEast && !hasWest) || hasSouth)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+0.48f,	actualY, z+0.75f),
									new Vector3f(x+0.52f,	actualY, z+0.75f),
									new Vector3f(x+0.52f,	actualY, z+1),
									new Vector3f(x+0.48f,	actualY, z+1), 
									light, new Vector2f(topTexture.u0, topTexture.v1), new Vector2f(topTexture.u0, topTexture.v0),new Vector2f(topTexture.u1, topTexture.v0), new Vector2f(topTexture.u1, topTexture.v1));
		}
		
		
		//East/West tripwire	
		if (hasWest)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x,			actualY, z+0.48f),
									new Vector3f(x+0.25f,	actualY, z+0.48f),
									new Vector3f(x+0.25f,	actualY, z+0.52f),
									new Vector3f(x,			actualY, z+0.52f),
									light, topTexture);
		}
		
		if (hasWest || (hasEast && !hasNorth && !hasSouth))
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+0.25f,	actualY, z+0.48f),
									new Vector3f(x+0.50f,	actualY, z+0.48f),
									new Vector3f(x+0.50f,	actualY, z+0.52f),
									new Vector3f(x+0.25f,	actualY, z+0.52f),
									light, topTexture);
		}
		
		if ((hasWest && !hasNorth && !hasSouth) || hasEast)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+0.50f,	actualY, z+0.48f),
									new Vector3f(x+0.75f,	actualY, z+0.48f),
									new Vector3f(x+0.75f,	actualY, z+0.52f),
									new Vector3f(x+0.50f,	actualY, z+0.52f),
									light, topTexture);
		}
		
		if (hasEast)
		{
			MeshUtil.addQuad(mesh,	new Vector3f(x+0.75f,	actualY, z+0.48f),
									new Vector3f(x+1,		actualY, z+0.48f),
									new Vector3f(x+1,		actualY, z+0.52f),
									new Vector3f(x+0.75f,	actualY, z+0.52f),
									light, topTexture);
		}
	}

}
