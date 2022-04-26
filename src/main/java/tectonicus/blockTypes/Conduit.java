/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.chunk.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Conduit implements BlockType
{
	@Getter
	private final String name;
	@Getter
	private final String id;

	private final SubTexture texture;
	private final Colour4f colour;

	public Conduit(String name, String id, SubTexture texture)
	{
		this.name = name;
		this.id = id;

		this.texture = texture;

		colour = new Colour4f(1, 1, 1, 1);
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
		SubMesh subMesh = new SubMesh();

		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z, world.getNightLightAdjustment());
		final Vector4f color = new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a);

		final float offSet = 1.0f / 16.0f;

		SubTexture currentTexture = texture;

		float widthTexel = 1.0f / 32.0f;
		float heightTexel = 1.0f / 16.0f;

		SubTexture bottomTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*6, texture.v0, texture.u0+widthTexel*12, texture.v0+heightTexel*6);
		SubTexture topTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*12, texture.v0, texture.u0+widthTexel*18, texture.v0+heightTexel*6);
		SubTexture southTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*18, texture.v0+heightTexel*6, texture.u0+widthTexel*24, texture.v0+heightTexel*12);
		SubTexture northTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*6, texture.v0+heightTexel*6, texture.u0+widthTexel*12, texture.v0+heightTexel*12);
		SubTexture eastTexture = new SubTexture(currentTexture.texture, texture.u0+widthTexel*12, texture.v0+heightTexel*6, texture.u0+widthTexel*18, texture.v0+heightTexel*12);
		SubTexture westTexture = new SubTexture(currentTexture.texture, texture.u0, texture.v0+heightTexel*6, texture.u0+widthTexel*6, texture.v0+heightTexel*12);

		//Top
		subMesh.addQuad(new Vector3f(offSet*5,	offSet*11,	offSet*5),
						new Vector3f(offSet*11,	offSet*11,	offSet*5),
						new Vector3f(offSet*11,	offSet*11,	offSet*11),
						new Vector3f(offSet*5,	offSet*11,	offSet*11),
						color, topTexture);

		//Bottom
		subMesh.addQuad(new Vector3f(offSet*5,	offSet*5,	offSet*11),
						new Vector3f(offSet*11,	offSet*5,	offSet*11),
						new Vector3f(offSet*11,	offSet*5,	offSet*5),
						new Vector3f(offSet*5,	offSet*5,	offSet*5),
						color, bottomTexture);

		//North
		subMesh.addQuad(new Vector3f(offSet*11,	offSet*11,	offSet*5),
						new Vector3f(offSet*5,	offSet*11,	offSet*5),
						new Vector3f(offSet*5,	offSet*5,	offSet*5),
						new Vector3f(offSet*11,	offSet*5,	offSet*5),
						color, northTexture);

		//South
		subMesh.addQuad(new Vector3f(offSet*5,	offSet*11,	offSet*11),
						new Vector3f(offSet*11,	offSet*11,	offSet*11),
						new Vector3f(offSet*11,	offSet*5,	offSet*11),
						new Vector3f(offSet*5,	offSet*5,	offSet*11),
						color, southTexture);

		//East
		subMesh.addQuad(new Vector3f(offSet*11,	offSet*11,	offSet*11),
						new Vector3f(offSet*11,	offSet*11,	offSet*5),
						new Vector3f(offSet*11,	offSet*5,	offSet*5),
						new Vector3f(offSet*11,	offSet*5,	offSet*11),
						color, eastTexture);

		//West
		subMesh.addQuad(new Vector3f(offSet*5,	offSet*11,	offSet*5),
						new Vector3f(offSet*5,	offSet*11,	offSet*11),
						new Vector3f(offSet*5,	offSet*5,	offSet*11),
						new Vector3f(offSet*5,	offSet*5,	offSet*5),
						color, westTexture);

		subMesh.pushTo(geometry.getMesh(currentTexture.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
	}
}
