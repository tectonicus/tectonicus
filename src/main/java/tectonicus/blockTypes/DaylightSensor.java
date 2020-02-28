/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
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
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class DaylightSensor implements BlockType
{	
	private final String name;

	private final SubTexture top, bottom, side;
	
	public DaylightSensor(String name, SubTexture top, SubTexture bottom)
	{
		this.name = name;
		
		if (top.texturePackVersion != VERSION_4)
		{
			final float texel = 1.0f / top.texture.getHeight();
			final float tile = texel * top.texture.getWidth();
			this.top = new SubTexture(top.texture, top.u0, top.v0, top.u1, top.v0+tile);
		}
		else
		{
			this.top = top;
		}
		
		this.bottom = bottom;
		
		final float texel = 1.0f / 16.0f;
		
		this.side = new SubTexture(bottom.texture, bottom.u0, bottom.v0, bottom.u1, bottom.v0+texel*6);
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
		
		SubMesh topMesh = new SubMesh();
		SubMesh bottomMesh = new SubMesh();

		SubMesh.addBlockSimple(bottomMesh, 0, 0, 0, 1, offSet*6, 1, colour, side, null, bottom);
	
		// Top
		topMesh.addQuad(new Vector3f(1, offSet*6, 0),
						new Vector3f(1, offSet*6, 1),
						new Vector3f(0, offSet*6, 1),
						new Vector3f(0, offSet*6, 0),
						colour, top);
		
		topMesh.pushTo(geometry.getMesh(top.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
		bottomMesh.pushTo(geometry.getMesh(bottom.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
	}
}
