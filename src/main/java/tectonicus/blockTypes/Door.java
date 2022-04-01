/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.joml.Vector2f;
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

import static tectonicus.Version.VERSION_4;

public class Door implements BlockType
{
	private final String name;
	private final SubTexture topTexture;
	private final SubTexture bottomTexture;
	private final SubTexture edgeTexture;
	private final SubTexture topEdgeTexture;
	
	public Door(String name, SubTexture topTexture, SubTexture bottomTexture)
	{
		this.name = name;
		this.topTexture = topTexture;
		this.bottomTexture = bottomTexture;
		
		final float uWidth; // fudge factor
		if (topTexture.texturePackVersion == VERSION_4)
			uWidth = 1.0f / 16.0f / 16.0f * 3f;
		else
			uWidth = 1.0f / 16.0f * 3f;
		
		this.edgeTexture = new SubTexture(topTexture.texture, topTexture.u0, topTexture.v0, topTexture.u0+uWidth, topTexture.v1);
		this.topEdgeTexture = new SubTexture(bottomTexture.texture, bottomTexture.u1-uWidth, bottomTexture.v0, bottomTexture.u1, bottomTexture.v1);
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
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		final float thickness = 1.0f / 16.0f * 3.0f;
		
		final int data = rawChunk.getBlockData(x, y, z);
		final boolean isTop = (data & 0x8) > 0;
		int hingePos = data & 0x1;
		boolean isOpen = (data & 0x4) > 0;
		int facing = data & 0x3;
		
		if(isTop)
		{
			final int bottomData = rawChunk.getBlockData(x, y-1, z);
			isOpen = (bottomData & 0x4) > 0;
			facing = bottomData & 0x3;
		}
		else {
			final int topData = rawChunk.getBlockData(x, y+1, z);
			hingePos = topData & 0x1;
		}
			
		
		SubMesh subMesh = new SubMesh();
		SubMesh doorTopMesh = new SubMesh();
		
		SubTexture frontTexture = isTop ? topTexture : bottomTexture;
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		
		// Front face
		if (hingePos == 0){ //Hinge on the left
			subMesh.addQuad(new Vector3f(0, 1, thickness), new Vector3f(1, 1, thickness), new Vector3f(1, 0, thickness), new Vector3f(0, 0, thickness),
						white, frontTexture);
		}
		else {  //Hinge on the right
			subMesh.addQuad(new Vector3f(0, 1, thickness), new Vector3f(1, 1, thickness), new Vector3f(1, 0, thickness), new Vector3f(0, 0, thickness),
					white, new Vector2f(frontTexture.u1, frontTexture.v0), new Vector2f(frontTexture.u0, frontTexture.v0),new Vector2f(frontTexture.u0, frontTexture.v1), new Vector2f(frontTexture.u1, frontTexture.v1));
		}
		
		// Back face
		if (hingePos == 0){ //Hinge on the left
		subMesh.addQuad(new Vector3f(1, 1, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), white,
						new Vector2f(frontTexture.u1, frontTexture.v0), new Vector2f(frontTexture.u0, frontTexture.v0),new Vector2f(frontTexture.u0, frontTexture.v1), new Vector2f(frontTexture.u1, frontTexture.v1) );
		}
		else {  //Hinge on the right
			subMesh.addQuad(new Vector3f(1, 1, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), white, frontTexture );
		}
		
		// Top edge
		/*if (isTop) {
			doorTopMesh.addQuad(new Vector3f(0, 1, 0), new Vector3f(1, 1, 0), new Vector3f(1, 1, thickness), new Vector3f(0, 1, thickness), white, 
						new Vector2f(topEdgeTexture.u1, topEdgeTexture.v0), new Vector2f(topEdgeTexture.u1, topEdgeTexture.v1),new Vector2f(topEdgeTexture.u0, topEdgeTexture.v1), new Vector2f(topEdgeTexture.u0, topEdgeTexture.v0));
		}*/
		
		// Now rotate depending on door facing direction and open flag
		Rotation rotation = Rotation.None;
		float angle = 0;
		
		final float texel = 1.0f / 16.0f;
		
		float xOffset = x;
		float yOffset = y;
		float zOffset = z;

		if (isOpen)
		{
			// Top edge
			if (isTop) {
				doorTopMesh.addQuad(new Vector3f(0, 1, 0), new Vector3f(1, 1, 0), new Vector3f(1, 1, thickness), new Vector3f(0, 1, thickness), white, 
							new Vector2f(topEdgeTexture.u0, topEdgeTexture.v1), new Vector2f(topEdgeTexture.u0, topEdgeTexture.v0),new Vector2f(topEdgeTexture.u1, topEdgeTexture.v0), new Vector2f(topEdgeTexture.u1, topEdgeTexture.v1));
			}
			
			// Hinge edge
			subMesh.addQuad(new Vector3f(0, 1, 0), new Vector3f(0, 1, thickness), new Vector3f(0, 0, thickness), new Vector3f(0, 0, 0),
							white, edgeTexture);
			
			// Non-hinge edge
			subMesh.addQuad(new Vector3f(1, 1, thickness), new Vector3f(1, 1, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, thickness),
					white, new Vector2f(edgeTexture.u1, edgeTexture.v0), new Vector2f(edgeTexture.u0, edgeTexture.v0),new Vector2f(edgeTexture.u0, edgeTexture.v1), new Vector2f(edgeTexture.u1, edgeTexture.v1));
			
			if (hingePos == 0){  //Hinge on the left
				if (facing == 0)  //Facing West
				{
					// ...already correct
				}
				else if (facing == 1)  //Facing North
				{
					rotation = Rotation.AntiClockwise;
					angle = 90;
				}
				else if (facing == 2)  //Facing East
				{
					rotation = Rotation.Clockwise;
					angle = 180;
				}
				else if (facing == 3)  //Facing South
				{
					rotation = Rotation.Clockwise;
					angle = 90;
				}
			}
			else { //Hinge on the right
				if (facing == 0)  //Facing West
				{
					rotation = Rotation.Clockwise;
					angle = 180;
					
				}
				else if (facing == 1)  //Facing North
				{
					rotation = Rotation.Clockwise;
					angle = 90;
				}
				else if (facing == 2)  //Facing East
				{
					// ...already correct
				}
				else if (facing == 3)  //Facing South
				{
					rotation = Rotation.AntiClockwise;
					angle = 90;
				}
			}
		}
		else  //Door is closed
		{
			// Top edge
			if (isTop) {
				doorTopMesh.addQuad(new Vector3f(0, 1, 0), new Vector3f(1, 1, 0), new Vector3f(1, 1, thickness), new Vector3f(0, 1, thickness), white, 
							new Vector2f(topEdgeTexture.u1, topEdgeTexture.v0), new Vector2f(topEdgeTexture.u1, topEdgeTexture.v1),new Vector2f(topEdgeTexture.u0, topEdgeTexture.v1), new Vector2f(topEdgeTexture.u0, topEdgeTexture.v0));
			}
			
			// Hinge edge
			subMesh.addQuad(new Vector3f(0, 1, 0), new Vector3f(0, 1, thickness), new Vector3f(0, 0, thickness), new Vector3f(0, 0, 0),
							white, new Vector2f(edgeTexture.u1, edgeTexture.v0), new Vector2f(edgeTexture.u0, edgeTexture.v0),new Vector2f(edgeTexture.u0, edgeTexture.v1), new Vector2f(edgeTexture.u1, edgeTexture.v1));
			
			// Non-hinge edge
			subMesh.addQuad(new Vector3f(1, 1, thickness), new Vector3f(1, 1, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, thickness),
					white, edgeTexture);
			
			if (facing == 0) //Facing West
			{
				rotation = Rotation.AntiClockwise;
				angle = 90;
				xOffset -= texel * 13;
			}
			else if (facing == 1) //Facing North
			{
				rotation = Rotation.Clockwise;
				angle = 180;
				zOffset -= texel * 13;
			}
			else if (facing == 2)  //Facing East
			{
				rotation = Rotation.Clockwise;
				angle = 90;
				xOffset += texel * 13;
			}
			else if (facing == 3)  //Facing South
			{
				zOffset += texel * 13;
			}
		}
		
		if (isTop) {
			subMesh.pushTo(geometry.getMesh(topTexture.texture, Geometry.MeshType.AlphaTest), xOffset, yOffset, zOffset, rotation, angle);
			doorTopMesh.pushTo(geometry.getMesh(bottomTexture.texture, Geometry.MeshType.AlphaTest), xOffset, yOffset, zOffset, rotation, angle);
		}
		else
			subMesh.pushTo(geometry.getMesh(bottomTexture.texture, Geometry.MeshType.AlphaTest), xOffset, yOffset, zOffset, rotation, angle);
	}
	
}
