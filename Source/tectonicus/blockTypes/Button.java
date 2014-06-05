/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Button implements BlockType
{
	private static final int WIDTH = 6;
	private static final int HEIGHT = 4;
	private static final int DEPTH = 2;
	
	private final String name;
	private final SubTexture frontTexture;
	private final SubTexture sideTexture;
	private final SubTexture edgeTexture;
	
	public Button(String name, SubTexture texture)
	{
		this.name = name;
		
		final float texel;
		if (texture.texturePackVersion == "1.4")
			texel = 1.0f / 16.0f / 16.0f;
		else
			texel = 1.0f / 16.0f;
		
		this.frontTexture = new SubTexture(texture.texture, texture.u0+texel*HEIGHT, texture.v0+texel*WIDTH, texture.u1-texel*HEIGHT, texture.v1-texel*WIDTH);
		this.sideTexture = new SubTexture(texture.texture, texture.u0, texture.v0, texture.u0+texel*WIDTH, texture.v0+texel*DEPTH);
		this.edgeTexture = new SubTexture(texture.texture, texture.u0, texture.v0, texture.u0+texel*DEPTH, texture.v0+texel*HEIGHT);
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
		
		final float texel = 1.0f / 16.0f;
		
		final float width = texel * WIDTH;
		final float height = texel * HEIGHT;
		final float depth = texel * DEPTH;
		
		final float xMin = texel * 5.0f; // correct?
		final float xMax = xMin + width;
		
		final float yMin = texel * 6.0f; // correct?
		final float yMax = yMin + height;
		
		final float zMin = 0;
		final float zMax = zMin + depth;
		
		Vector4f white = new Vector4f(1, 1, 1, 1);
		
		SubMesh subMesh = new SubMesh();
		
		// Front quad
		subMesh.addQuad(new Vector3f(xMin, yMax, zMax), new Vector3f(xMax, yMax, zMax), new Vector3f(xMax, yMin, zMax), new Vector3f(xMin, yMin, zMax), white, frontTexture);
		
		// South quad
		subMesh.addQuad(new Vector3f(xMax, yMax, zMax), new Vector3f(xMax, yMax, zMin), new Vector3f(xMax, yMin, zMin), new Vector3f(xMax, yMin, zMax), white, edgeTexture);
		
		// North quad
		subMesh.addQuad(new Vector3f(xMin, yMax, zMin), new Vector3f(xMin, yMax, zMax), new Vector3f(xMin, yMin, zMax), new Vector3f(xMin, yMin, zMin), white, edgeTexture);
		
		// Top quad
		subMesh.addQuad(new Vector3f(xMin, yMax, zMin), new Vector3f(xMax, yMax, zMin), new Vector3f(xMax, yMax, zMax), new Vector3f(xMin, yMax, zMax), white, sideTexture);
		
		// TODO: May need a bottom quad when we get streetview views and can see the button from underneath
		
		float xOffset = x;
		float yOffset = y;
		float zOffset = z;
		
		Rotation rotation = Rotation.None;
		float angle = 0;
		
		final int direction = data & 0x3;
		
		if (direction == 0)
		{
			// Facing east
			rotation = Rotation.Clockwise;
			angle = 180;
		}
		else if (direction == 1)
		{
			// Facing south
			rotation = Rotation.Clockwise;
			angle = 90;
		}
		else if (direction == 2)
		{
			// Facing north
			rotation = Rotation.Clockwise;
			angle = -90;
		}
		else if (direction == 3)
		{
			// Facing west
			// ...build in this direction
		}
		
		subMesh.pushTo(geometry.getMesh(frontTexture.texture, Geometry.MeshType.Solid), xOffset, yOffset, zOffset, rotation, angle);
	}
	
}
