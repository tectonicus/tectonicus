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
 *   * Neither the name of 'Tectonicus' nor the names of
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

import java.util.Calendar;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Chest implements BlockType
{
	private final String name;
	
	private final SubTexture smallTop, smallTopSide, smallTopFront, smallBottom, smallBaseSide, smallBaseFront, smallLock;
	private final SubTexture largeTopLeft, largeTopRight, largeTopSide, largeTopFrontLeft, largeTopFrontRight, largeTopBackLeft, largeTopBackRight,
							 largeBottomLeft, largeBottomRight, largeBaseSide, largeBaseFrontLeft, largeBaseFrontRight, largeBaseBackLeft, largeBaseBackRight, largeLock, largeLock2;

	public Chest(String name, SubTexture small, SubTexture large, SubTexture ender,
							SubTexture trappedSmall, SubTexture trappedLarge,
							SubTexture xmasSmall, SubTexture xmasLarge)
	{
		this.name = name;
		
		final float texel = 1.0f / 64.0f;
		final float wideTexel = 1.0f / 128.0f;
		
		Calendar calendar = Calendar.getInstance();
		SubTexture smallChest;
		if(name.equals("Ender Chest"))
			smallChest = ender;
		else if(name.equals("Trapped Chest"))
			smallChest = trappedSmall;
		else
			smallChest = small;
		
		SubTexture largeChest = name.equals("Trapped Chest") ? trappedLarge : large;
		
		if(calendar.get(Calendar.MONTH) == 11 && calendar.get(Calendar.DAY_OF_MONTH) == 25 && large.texturePackVersion == "1.5")
		{
			smallChest = xmasSmall;
			largeChest = xmasLarge;
		}
		
		// Small Chest Textures
		smallTop = new SubTexture(smallChest.texture, smallChest.u0+texel*14, smallChest.v0, smallChest.u0+texel*28, smallChest.v0+texel*14);
		smallTopSide = new SubTexture(smallChest.texture, smallChest.u0, smallChest.v0+texel*14, smallChest.u0+texel*14, smallChest.v0+texel*19);
		smallTopFront = new SubTexture(smallChest.texture, smallChest.u0+texel*14, smallChest.v0+texel*14, smallChest.u0+texel*28, smallChest.v0+texel*19);
		smallBottom = new SubTexture(smallChest.texture, smallChest.u0+texel*28, smallChest.v0+texel*19, smallChest.u0+texel*42, smallChest.v0+texel*33);
		smallBaseSide = new SubTexture(smallChest.texture, smallChest.u0, smallChest.v0+texel*34, smallChest.u0+texel*14, smallChest.v0+texel*43);
		smallBaseFront = new SubTexture(smallChest.texture, smallChest.u0+texel*14, smallChest.v0+texel*34, smallChest.u0+texel*28, smallChest.v0+texel*43);
		smallLock = new SubTexture(smallChest.texture, smallChest.u0+texel*3, smallChest.v0, smallChest.u0+texel*5, smallChest.v0+texel*4);
		
		//Large Chest Textures
		largeTopLeft = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*14, largeChest.v0, largeChest.u0+wideTexel*29, largeChest.v0+texel*14);
		largeTopRight = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*30, largeChest.v0, largeChest.u0+wideTexel*44, largeChest.v0+texel*14);
		largeTopSide = new SubTexture(largeChest.texture, largeChest.u0, largeChest.v0+texel*14, largeChest.u0+wideTexel*14, largeChest.v0+texel*19);
		largeTopFrontLeft = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*14, largeChest.v0+texel*14, largeChest.u0+wideTexel*29, largeChest.v0+texel*19);
		largeTopFrontRight = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*29, largeChest.v0+texel*14, largeChest.u0+wideTexel*44, largeChest.v0+texel*19);
		largeTopBackRight = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*57, largeChest.v0+texel*14, largeChest.u0+wideTexel*73, largeChest.v0+texel*19);
		largeTopBackLeft = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*73, largeChest.v0+texel*14, largeChest.u0+wideTexel*88, largeChest.v0+texel*19);
		largeBottomLeft = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*45, largeChest.v0+texel*20, largeChest.u0+wideTexel*60, largeChest.v0+texel*34);
		largeBottomRight = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*61, largeChest.v0+texel*20, largeChest.u0+wideTexel*74, largeChest.v0+texel*34);
		largeBaseSide = new SubTexture(largeChest.texture, largeChest.u0, largeChest.v0+texel*34, largeChest.u0+wideTexel*14, largeChest.v0+texel*43);
		largeBaseFrontLeft = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*14, largeChest.v0+texel*34, largeChest.u0+wideTexel*29, largeChest.v0+texel*43);
		largeBaseFrontRight = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*29, largeChest.v0+texel*34, largeChest.u0+wideTexel*44, largeChest.v0+texel*43);
		largeBaseBackRight = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*58, largeChest.v0+texel*34, largeChest.u0+wideTexel*73, largeChest.v0+texel*43);
		largeBaseBackLeft = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*73, largeChest.v0+texel*34, largeChest.u0+wideTexel*88, largeChest.v0+texel*43);
		largeLock = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*3, largeChest.v0, largeChest.u0+wideTexel*4, largeChest.v0+texel*4);
		largeLock2 = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*4, largeChest.v0, largeChest.u0+wideTexel*5, largeChest.v0+texel*4);
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		final int data = chunk.getBlockData(x, y, z);
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, chunk, x, y, z);
		Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
		
		SubMesh smallMesh = new SubMesh();
		SubMesh largeMesh = new SubMesh();
		
		final float offSet = 1.0f / 16.0f;
		final float height = offSet * 10.0f;
		
		final int westId = world.getBlockId(chunk.getChunkCoord(), x-1, y, z);
		final int eastId = world.getBlockId(chunk.getChunkCoord(), x+1, y, z);
		final int northId = world.getBlockId(chunk.getChunkCoord(), x, y, z-1);
		final int southId = world.getBlockId(chunk.getChunkCoord(), x, y, z+1);		
		
		final boolean chestNorth;
		final boolean chestSouth;
		final boolean chestEast;
		final boolean chestWest;
		if(name.equals("Trapped Chest"))
		{
			chestNorth = northId == BlockIds.TRAPPED_CHEST;
			chestSouth = southId == BlockIds.TRAPPED_CHEST;
			chestEast = eastId == BlockIds.TRAPPED_CHEST;
			chestWest = westId == BlockIds.TRAPPED_CHEST;
		}
		else if(name.equals("Chest"))
		{
			chestNorth = northId == BlockIds.CHEST;
			chestSouth = southId == BlockIds.CHEST;
			chestEast = eastId == BlockIds.CHEST;
			chestWest = westId == BlockIds.CHEST;
		}
		else
		{
			chestNorth = chestSouth = chestEast = chestWest = false;
		}
			
		

		if (chestNorth || chestSouth || chestEast || chestWest)
		{
			// Double chest!
			
			if((data == 2 && chestWest) || (data == 3 && chestEast) || (data == 4 && chestSouth) || (data == 5 && chestNorth))
			{
				// Left half
				
				// Top
				largeMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(1, 1-offSet*2, 0+offSet),
								new Vector3f(1, 1-offSet*2, 1-offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet), colour, largeTopLeft);
				// West
				largeMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet),
								new Vector3f(0+offSet, height, 1-offSet),  new Vector3f(0+offSet, height, 0+offSet), colour, largeTopSide);
				// North
				largeMesh.addQuad(new Vector3f(1, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 0+offSet),
								new Vector3f(0+offSet, height, 0+offSet),  new Vector3f(1, height, 0+offSet), colour, largeTopBackLeft);
				// South
				largeMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 1-offSet), new Vector3f(1, 1-offSet*2, 1-offSet),
								new Vector3f(1, height, 1-offSet),  new Vector3f(0+offSet, height, 1-offSet), colour, largeTopFrontLeft);
				// East
				/*largeLeftMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
								new Vector3f(1-offSet, height, 0+offSet), new Vector3f(1-offSet, height, 1-offSet), colour, largeTopSide);*/
				
				
				//Chest bottom
				
				// Bottom
				largeMesh.addQuad(new Vector3f(0+offSet, 0, 0+offSet), new Vector3f(0+offSet, 0, 1-offSet),
								new Vector3f(1, 0, 1-offSet), new Vector3f(1, 0, 0+offSet), colour, largeBottomLeft);
			
				// West
				largeMesh.addQuad(new Vector3f(0+offSet, height, 0+offSet), new Vector3f(0+offSet, height, 1-offSet),
								new Vector3f(0+offSet, 0, 1-offSet),  new Vector3f(0+offSet, 0, 0+offSet), colour, largeBaseSide);
				// North
				largeMesh.addQuad(new Vector3f(1, height, 0+offSet), new Vector3f(0+offSet, height, 0+offSet),
								new Vector3f(0+offSet, 0, 0+offSet),  new Vector3f(1, 0, 0+offSet), colour, largeBaseBackLeft);
				// South
				largeMesh.addQuad(new Vector3f(0+offSet, height, 1-offSet), new Vector3f(1, height, 1-offSet),
								new Vector3f(1, 0, 1-offSet),  new Vector3f(0+offSet, 0, 1-offSet), colour, largeBaseFrontLeft);
				// East
				/*largeLeftMesh.addQuad(new Vector3f(1-offSet, height, 1-offSet), new Vector3f(1-offSet, height, 0+offSet),
								new Vector3f(1-offSet, 0, 0+offSet), new Vector3f(1-offSet, 0, 1-offSet), colour, largeBaseSide);*/
				
				SubMesh.addBlockSimple(largeMesh, 1-offSet, offSet*8, offSet*15, offSet, offSet*4, offSet, colour, largeLock, largeLock, largeLock);
			}
			else if ((data == 2 && chestEast) || (data == 3 && chestWest) || (data == 4 && chestNorth) || (data == 5 && chestSouth))
			{
				// Right half
				
				// Top
				largeMesh.addQuad(new Vector3f(0, 1-offSet*2, 0+offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
								new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(0, 1-offSet*2, 1-offSet), colour, largeTopRight);
				// West
				/*largeLeftMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet),
								new Vector3f(0+offSet, height, 1-offSet),  new Vector3f(0+offSet, height, 0+offSet), colour, largeTopSide);*/
				// North
				largeMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 0+offSet), new Vector3f(0, 1-offSet*2, 0+offSet),
								new Vector3f(0, height, 0+offSet),  new Vector3f(1-offSet, height, 0+offSet), colour, largeTopBackRight);
				// South
				largeMesh.addQuad(new Vector3f(0, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 1-offSet),
								new Vector3f(1-offSet, height, 1-offSet),  new Vector3f(0, height, 1-offSet), colour, largeTopFrontRight);
				// East
				largeMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
								new Vector3f(1-offSet, height, 0+offSet), new Vector3f(1-offSet, height, 1-offSet), colour, largeTopSide);
				
				
				//Chest bottom
				
				// Bottom
				largeMesh.addQuad(new Vector3f(0, 0, 0+offSet), new Vector3f(0, 0, 1-offSet),
								new Vector3f(1-offSet, 0, 1-offSet), new Vector3f(1-offSet, 0, 0+offSet), colour, largeBottomRight);
			
				// West
				/*largeRightMesh.addQuad(new Vector3f(0+offSet, height, 0+offSet), new Vector3f(0+offSet, height, 1-offSet),
								new Vector3f(0+offSet, 0, 1-offSet),  new Vector3f(0+offSet, 0, 0+offSet), colour, largeBaseSide);*/
				// North
				largeMesh.addQuad(new Vector3f(1-offSet, height, 0+offSet), new Vector3f(0, height, 0+offSet),
								new Vector3f(0, 0, 0+offSet),  new Vector3f(1-offSet, 0, 0+offSet), colour, largeBaseBackRight);
				// South
				largeMesh.addQuad(new Vector3f(0, height, 1-offSet), new Vector3f(1-offSet, height, 1-offSet),
								new Vector3f(1-offSet, 0, 1-offSet),  new Vector3f(0, 0, 1-offSet), colour, largeBaseFrontRight);
				// East
				largeMesh.addQuad(new Vector3f(1-offSet, height, 1-offSet), new Vector3f(1-offSet, height, 0+offSet),
								new Vector3f(1-offSet, 0, 0+offSet), new Vector3f(1-offSet, 0, 1-offSet), colour, largeBaseSide);
				
				SubMesh.addBlockSimple(largeMesh, 0, offSet*8, offSet*15, offSet, offSet*4, offSet, colour, largeLock2, largeLock2, largeLock2);
			}
		}
		else
		{
			// Single chest
			
			// Chest top
			
			// Top
			smallMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
							new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet), colour, smallTop);
			// West
			smallMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet),
							new Vector3f(0+offSet, height, 1-offSet),  new Vector3f(0+offSet, height, 0+offSet), colour, smallTopSide);
			// North
			smallMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 0+offSet),
							new Vector3f(0+offSet, height, 0+offSet),  new Vector3f(1-offSet, height, 0+offSet), colour, smallTopSide);
			// South
			smallMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 1-offSet),
							new Vector3f(1-offSet, height, 1-offSet),  new Vector3f(0+offSet, height, 1-offSet), colour, smallTopFront);
			// East
			smallMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
							new Vector3f(1-offSet, height, 0+offSet), new Vector3f(1-offSet, height, 1-offSet), colour, smallTopSide);
			
			
			//Chest bottom
			
			// Bottom
			smallMesh.addQuad(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1),
							new Vector3f(1, 0, 1), new Vector3f(1, 0, 0), colour, smallBottom);
		
			// West
			smallMesh.addQuad(new Vector3f(0+offSet, height, 0+offSet), new Vector3f(0+offSet, height, 1-offSet),
							new Vector3f(0+offSet, 0, 1-offSet),  new Vector3f(0+offSet, 0, 0+offSet), colour, smallBaseSide);
			// North
			smallMesh.addQuad(new Vector3f(1-offSet, height, 0+offSet), new Vector3f(0+offSet, height, 0+offSet),
							new Vector3f(0+offSet, 0, 0+offSet),  new Vector3f(1-offSet, 0, 0+offSet), colour, smallBaseSide);
			// South
			smallMesh.addQuad(new Vector3f(0+offSet, height, 1-offSet), new Vector3f(1-offSet, height, 1-offSet),
							new Vector3f(1-offSet, 0, 1-offSet),  new Vector3f(0+offSet, 0, 1-offSet), colour, smallBaseFront);
			// East
			smallMesh.addQuad(new Vector3f(1-offSet, height, 1-offSet), new Vector3f(1-offSet, height, 0+offSet),
							new Vector3f(1-offSet, 0, 0+offSet), new Vector3f(1-offSet, 0, 1-offSet), colour, smallBaseSide);
			
			SubMesh.addBlockSimple(smallMesh, offSet*7, offSet*8, offSet*15, offSet*2, offSet*4, offSet, colour, smallLock, smallLock, smallLock);
		}

		
		Rotation horizRotation = Rotation.Clockwise;
		float horizAngle = 0;
	
		// Set angle/rotation from block data flags
		if (data == 2)
		{
			// north			
			horizRotation = Rotation.AntiClockwise;
			horizAngle = 180;
		}
		else if (data == 3)
		{
			// south
		}
		else if (data == 4)
		{
			// west
			horizRotation = Rotation.Clockwise;
			horizAngle = 270;

		}
		else if (data == 5)
		{
			// east			
			horizRotation = Rotation.Clockwise;
			horizAngle = 90;
		}
		
		smallMesh.pushTo(geometry.getMesh(smallTop.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, Rotation.None, 0);
		largeMesh.pushTo(geometry.getMesh(largeTopLeft.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, Rotation.None, 0);
	}
}
