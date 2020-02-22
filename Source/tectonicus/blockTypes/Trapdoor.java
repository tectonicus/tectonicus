/*
 * Copyright (c) 2012-2020, John Campbell and other contributors.  All rights reserved.
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
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class Trapdoor implements BlockType
{
	private final String name;
	private final SubTexture texture;
	private final SubTexture rightEdgeTexture;
	private final SubTexture leftEdgeTexture;
	private final SubTexture topEdgeTexture;
	private final SubTexture bottomEdgeTexture;
	
	public Trapdoor(String name, SubTexture texture)
	{
		this.name = name;
		this.texture = texture;

		final float uWidth; // fudge factor
		if (texture.texturePackVersion == VERSION_4)
			uWidth = 1.0f / 16.0f / 16.0f * 2.5f;
		else
			uWidth = 1.0f / 16.0f * 2.5f;
		
		this.rightEdgeTexture = new SubTexture(texture.texture, texture.u1-uWidth, texture.v0, texture.u1, texture.v1);
		this.leftEdgeTexture = new SubTexture(texture.texture, texture.u0, texture.v0, texture.u0+uWidth, texture.v1);
		this.topEdgeTexture = new SubTexture(texture.texture, texture.u0, texture.v0, texture.u1, texture.v0+uWidth);
		this.bottomEdgeTexture = new SubTexture(texture.texture, texture.u0, texture.v1-uWidth, texture.u1, texture.v1);
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
		final boolean isOpen = (data & 0x4) > 0;
		final boolean isOnTop = (data & 0x8) > 0;
		final int hingePos = data & 0x3;
		
		SubMesh subMesh = new SubMesh();
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);

		final BlockType above = world.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
		final BlockType below = world.getBlockType(rawChunk.getChunkCoord(), x, y-1, z);
		final BlockType north = world.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
		final BlockType south = world.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
		final BlockType west = world.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
		final BlockType east = world.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);

		if (!isOpen)
		{
			if(isOnTop)  // check if trapdoor hinge is on the top of the block
			{
				if (!above.isSolid())
				{
					// top face
					subMesh.addQuad(new Vector3f(0, 1, 0),
									new Vector3f(1, 1, 0),
									new Vector3f(1, 1, 1),
									new Vector3f(0, 1, 1),
									white, texture);
				}
				//if (!below.isSolid())
				//{
					// bottom face
					subMesh.addQuad(new Vector3f(0, 1-thickness, 1),
									new Vector3f(1, 1-thickness, 1),
									new Vector3f(1, 1-thickness, 0),
									new Vector3f(0, 1-thickness, 0),
									white, new Vector2f(texture.u1, texture.v0), new Vector2f(texture.u0, texture.v0),new Vector2f(texture.u0, texture.v1), new Vector2f(texture.u1, texture.v1) );
				//}
	
				if (!north.isSolid())
				{
					// actually west edge
					subMesh.addQuad(new Vector3f(0, 1, 0),
									new Vector3f(0, 1, 1),
									new Vector3f(0, 1-thickness, 1),
									new Vector3f(0, 1-thickness, 0),									
									white, topEdgeTexture);
				}
		
				if (!south.isSolid())
				{
					// actually east edge
					subMesh.addQuad(new Vector3f(1, 1, 1),
									new Vector3f(1, 1, 0),
									new Vector3f(1, 1-thickness, 0),
									new Vector3f(1, 1-thickness, 1),
									white, topEdgeTexture);
				}
		
				if (!east.isSolid())
				{
					// actually south edge
					subMesh.addQuad(new Vector3f(1, 1, 0),
									new Vector3f(0, 1, 0),																		
									new Vector3f(0, 1-thickness, 0),
									new Vector3f(1, 1-thickness, 0),
									white, topEdgeTexture);
				}
		
				if (!west.isSolid())
				{
					// actually north edge
					subMesh.addQuad(new Vector3f(0, 1, 1),
									new Vector3f(1, 1, 1),
									new Vector3f(1, 1-thickness, 1),
									new Vector3f(0, 1-thickness, 1),									
									white, topEdgeTexture);
				}
			}
			else if(!isOnTop)
			{
				//if (!above.isSolid())
				//{
					// top face
					subMesh.addQuad(new Vector3f(0, thickness, 0),
									new Vector3f(1, thickness, 0),
									new Vector3f(1, thickness, 1),
									new Vector3f(0, thickness, 1),
									white, texture);
				//}
				
				if (!below.isSolid())
				{
					// bottom face
					subMesh.addQuad(new Vector3f(0, 0, 1),
									new Vector3f(1, 0, 1),
									new Vector3f(1, 0, 0),
									new Vector3f(0, 0, 0),
									white, new Vector2f(texture.u1, texture.v0), new Vector2f(texture.u0, texture.v0),new Vector2f(texture.u0, texture.v1), new Vector2f(texture.u1, texture.v1) );
				}
				
				if (!north.isSolid())
				{
					// north edge
					subMesh.addQuad(new Vector3f(0, thickness, 0),
									new Vector3f(0, thickness, 1),
									new Vector3f(0, 0, 1),
									new Vector3f(0, 0, 0),
									white, topEdgeTexture);
				}
		
				if (!south.isSolid())
				{
					// south edge
					subMesh.addQuad(new Vector3f(1, thickness, 1),
									new Vector3f(1, thickness, 0),
									new Vector3f(1, 0, 0),
									new Vector3f(1, 0, 1),
									white, topEdgeTexture);
				}
		
				if (!east.isSolid())
				{
					// east edge
					subMesh.addQuad(new Vector3f(1, thickness, 0),
									new Vector3f(0, thickness, 0),
									new Vector3f(0, 0, 0),
									new Vector3f(1, 0, 0),
									white, topEdgeTexture);
				}
		
				if (!west.isSolid())
				{
					// west edge
					subMesh.addQuad(new Vector3f(0, thickness, 1),
									new Vector3f(1, thickness, 1),
									new Vector3f(1, 0, 1),
									new Vector3f(0, 0, 1),
									white, topEdgeTexture);
				}
			}
		}
		else if (hingePos == 0) // west
		{
			// east face (top)
			subMesh.addQuad(new Vector3f(1, 1, 1-thickness),
							new Vector3f(0, 1, 1-thickness),
							new Vector3f(0, 0, 1-thickness),
							new Vector3f(1, 0, 1-thickness),
							white, texture);

			if (!west.isSolid())
			{
				// west face (bottom)
				subMesh.addQuad(new Vector3f(0, 1, 1),
								new Vector3f(1, 1, 1),
								new Vector3f(1, 0, 1),
								new Vector3f(0, 0, 1),
								white, texture);//, new Vector2f(texture.u1, texture.v0), new Vector2f(texture.u0, texture.v0),new Vector2f(texture.u0, texture.v1), new Vector2f(texture.u1, texture.v1) );
			}

			if (!north.isSolid())
			{
				// north edge
				subMesh.addQuad(new Vector3f(0, 1, 1-thickness),
								new Vector3f(0, 1, 1),
								new Vector3f(0, 0, 1),
								new Vector3f(0, 0, 1-thickness),
								white, rightEdgeTexture);
			}

			if (!south.isSolid())
			{
				// south edge (MC 1.2.5 incorrectly uses the right edge)
				subMesh.addQuad(new Vector3f(1, 1, 1),
								new Vector3f(1, 1, 1-thickness),
								new Vector3f(1, 0, 1-thickness),
								new Vector3f(1, 0, 1),
								white, rightEdgeTexture);
			}

			if (!above.isSolid())
			{
				// top edge
				subMesh.addQuad(new Vector3f(0, 1, 1-thickness),
								new Vector3f(1, 1, 1-thickness),
								new Vector3f(1, 1, 1),
								new Vector3f(0, 1, 1),
								white, bottomEdgeTexture);
			}

			if (!below.isSolid())
			{
				// bottom edge
				subMesh.addQuad(new Vector3f(0, 0, 1),
								new Vector3f(1, 0, 1),
								new Vector3f(1, 0, 1-thickness),
								new Vector3f(0, 0, 1-thickness),
								//white, bottomEdgeTexture);
								white, new Vector2f(bottomEdgeTexture.u1, bottomEdgeTexture.v0), new Vector2f(bottomEdgeTexture.u0, bottomEdgeTexture.v0),new Vector2f(bottomEdgeTexture.u0, bottomEdgeTexture.v1), new Vector2f(bottomEdgeTexture.u1, bottomEdgeTexture.v1));
			}
		}
		else if (hingePos == 1) // east
		{
			//if (!west.isSolid())
			//{
				// west face (top)
				subMesh.addQuad(new Vector3f(0, 1, thickness),
								new Vector3f(1, 1, thickness),
								new Vector3f(1, 0, thickness),
								new Vector3f(0, 0, thickness),
								white, texture);
			//}

			if (!east.isSolid())
			{
				// east face (bottom)
				subMesh.addQuad(new Vector3f(1, 1, 0),
								new Vector3f(0, 1, 0),
								new Vector3f(0, 0, 0),
								new Vector3f(1, 0, 0),
								white, texture);
			}

			if (!north.isSolid())
			{
				// north edge
				subMesh.addQuad(new Vector3f(0, 1, 0),
								new Vector3f(0, 1, thickness),
								new Vector3f(0, 0, thickness),
								new Vector3f(0, 0, 0),
								white, leftEdgeTexture);
			}

			if (!south.isSolid())
			{
				// south edge (MC 1.2.5 incorrectly uses the left edge here)
				subMesh.addQuad(new Vector3f(1, 1, thickness),
								new Vector3f(1, 1, 0),
								new Vector3f(1, 0, 0),
								new Vector3f(1, 0, thickness),
								white, leftEdgeTexture);
			}

			if (!above.isSolid())
			{
				// top edge
				subMesh.addQuad(new Vector3f(0, 1, 0),
								new Vector3f(1, 1, 0),
								new Vector3f(1, 1, thickness),
								new Vector3f(0, 1, thickness),
								white, topEdgeTexture);
			}

			if (!below.isSolid())
			{
				// bottom edge
				subMesh.addQuad(new Vector3f(0, 0, thickness),
								new Vector3f(1, 0, thickness),
								new Vector3f(1, 0, 0),
								new Vector3f(0, 0, 0),
								//white, topEdgeTexture);
								white, new Vector2f(topEdgeTexture.u1, topEdgeTexture.v0), new Vector2f(topEdgeTexture.u0, topEdgeTexture.v0),new Vector2f(topEdgeTexture.u0, topEdgeTexture.v1), new Vector2f(topEdgeTexture.u1, topEdgeTexture.v1));
			}
		}
		else if (hingePos == 2) // south
		{
			//if (!north.isSolid())
			//{
				// north face (top)
				subMesh.addQuad(new Vector3f(1-thickness, 1, 0),
								new Vector3f(1-thickness, 1, 1),
								new Vector3f(1-thickness, 0, 1),
								new Vector3f(1-thickness, 0, 0),
								white, texture);
			//}

			if (!south.isSolid())
			{
				// south face (bottom)
				subMesh.addQuad(new Vector3f(1, 1, 1),
								new Vector3f(1, 1, 0),
								new Vector3f(1, 0, 0),
								new Vector3f(1, 0, 1),
								white, texture);
			}

			if (!east.isSolid())
			{
				// east edge (MC 1.2.5 incorrectly uses the right edge here)
				subMesh.addQuad(new Vector3f(1, 1, 0),
								new Vector3f(1-thickness, 1, 0),
								new Vector3f(1-thickness, 0, 0),
								new Vector3f(1, 0, 0),
								white, rightEdgeTexture);
			}

			if (!west.isSolid())
			{
				// west edge
				subMesh.addQuad(new Vector3f(1-thickness, 1, 1),
								new Vector3f(1, 1, 1),
								new Vector3f(1, 0, 1),
								new Vector3f(1-thickness, 0, 1),
								white, rightEdgeTexture);
			}

			if (!above.isSolid())
			{
				// top edge
				subMesh.addQuad(new Vector3f(1-thickness, 1, 0),
								new Vector3f(1, 1, 0),
								new Vector3f(1, 1, 1),
								new Vector3f(1-thickness, 1, 1),
								white, rightEdgeTexture);
			}

			if (!below.isSolid())
			{
				// bottom edge
				subMesh.addQuad(new Vector3f(1-thickness, 0, 1),
								new Vector3f(1, 0, 1),
								new Vector3f(1, 0, 0),
								new Vector3f(1-thickness, 0, 0),
								//white, rightEdgeTexture);
								white, new Vector2f(rightEdgeTexture.u1, rightEdgeTexture.v0), new Vector2f(rightEdgeTexture.u0, rightEdgeTexture.v0),new Vector2f(rightEdgeTexture.u0, rightEdgeTexture.v1), new Vector2f(rightEdgeTexture.u1, rightEdgeTexture.v1));
			}
		}
		else if (hingePos == 3) // north
		{
			//if (!south.isSolid())
			//{
				// south face (top)
				subMesh.addQuad(new Vector3f(thickness, 1, 1),
								new Vector3f(thickness, 1, 0),
								new Vector3f(thickness, 0, 0),
								new Vector3f(thickness, 0, 1),
								white, texture);
			//}

			if (!north.isSolid())
			{
				// north face (bottom)
				subMesh.addQuad(new Vector3f(0, 1, 0),
								new Vector3f(0, 1, 1),
								new Vector3f(0, 0, 1),
								new Vector3f(0, 0, 0),
								white, texture);
			}

			if (!east.isSolid())
			{
				// east edge (MC 1.2.5 incorrectly uses the left edge here)
				subMesh.addQuad(new Vector3f(thickness, 1, 0),
								new Vector3f(0, 1, 0),
								new Vector3f(0, 0, 0),
								new Vector3f(thickness, 0, 0),
								white, leftEdgeTexture);
			}

			if (!west.isSolid())
			{
				// west edge
				subMesh.addQuad(new Vector3f(0, 1, 1),
								new Vector3f(thickness, 1, 1),
								new Vector3f(thickness, 0, 1),
								new Vector3f(0, 0, 1),
								white, leftEdgeTexture);
			}

			if (!above.isSolid())
			{
				// top edge
				subMesh.addQuad(new Vector3f(0, 1, 0),
								new Vector3f(thickness, 1, 0),
								new Vector3f(thickness, 1, 1),
								new Vector3f(0, 1, 1),
								white, leftEdgeTexture);
			}

			if (!below.isSolid())
			{
				// bottom edge
				subMesh.addQuad(new Vector3f(0, 0, 1),
								new Vector3f(thickness, 0, 1),
								new Vector3f(thickness, 0, 0),
								new Vector3f(0, 0, 0),
								//white, leftEdgeTexture);
								white, new Vector2f(leftEdgeTexture.u1, leftEdgeTexture.v0), new Vector2f(leftEdgeTexture.u0, leftEdgeTexture.v0),new Vector2f(leftEdgeTexture.u0, leftEdgeTexture.v1), new Vector2f(leftEdgeTexture.u1, leftEdgeTexture.v1));
			}
		}
		else
		{
			return;
		}

		subMesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest), x, y, z, Rotation.None, 0);

		/*
		// Now rotate depending on hinge position and open flag
		
		Rotation rotation = Rotation.None;
		float angle = 0;
		
		//final float texel = 1.0f / 16.0f;
		
		float xOffset = x;
		float yOffset = y;
		float zOffset = z;
		
		if (isOpen)
		{
			if (hingePos == 0)
			{
				// Hinge in north-east corner
				// ...already correct
			}
			else if (hingePos == 1)
			{
				// Hinge in south-east corner
				rotation = Rotation.AntiClockwise;
				angle = 90;
			}
			else if (hingePos == 2)
			{
				// Hinge in south-west corner
				rotation = Rotation.Clockwise;
				angle = 180;
			}
			else if (hingePos == 3)
			{
				// Hinge in north-west corner
				rotation = Rotation.Clockwise;
				angle = 90;
			}
		}

		subMesh.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest), xOffset, yOffset, zOffset, Rotation.None, 0f, rotation, angle);
		*/
	}
	
}
