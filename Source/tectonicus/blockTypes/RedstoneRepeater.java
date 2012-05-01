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
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class RedstoneRepeater implements BlockType
{
	private static final int HEIGHT_IN_TEXELS = 4;
	
	private final String name;
	
	private final SubTexture baseTexture, sideTexture;
	private final SubTexture torchTopTexture, torchSideTexture;
	
	public RedstoneRepeater(String name, SubTexture baseTexture, SubTexture sideTexture, SubTexture torchTexture)
	{
		this.name = name;
		
		this.baseTexture = baseTexture;
		
		final float vHeight = 1.0f / 16.0f / 16.0f * HEIGHT_IN_TEXELS;
		
		this.sideTexture = new SubTexture(sideTexture.texture, sideTexture.u0, sideTexture.v0, sideTexture.u1, sideTexture.v0+vHeight);
		
		
		// Torch textures
		
		final float texelSize = 1.0f / 16.0f / 16.0f;
		
		final float topOffset = texelSize * 6;
		this.torchSideTexture = new SubTexture(torchTexture.texture, torchTexture.u0, torchTexture.v0 + topOffset, torchTexture.u1, torchTexture.v1);
		
		final float uOffset = texelSize * 7;
		final float vOffset0 = texelSize * 6;
		final float vOffset1 = texelSize * 8;
		this.torchTopTexture = new SubTexture(torchTexture.texture, torchTexture.u0 + uOffset, torchTexture.v0 + vOffset0, torchTexture.u1 - uOffset, torchTexture.v1 - vOffset1);
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
		// TODO Auto-generated method stub
		
		final int data = chunk.getBlockData(x, y, z);
		
		final float height = 1.0f / 16.0f * HEIGHT_IN_TEXELS;
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, chunk, x, y, z);
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		
		SubMesh subMesh = new SubMesh();
		
		subMesh.addQuad(new Vector3f(0, height, 0), new Vector3f(1, height, 0), new Vector3f(1, height, 1), new Vector3f(0, height, 1), white, baseTexture);
		
		// North edge
		subMesh.addQuad(new Vector3f(0, height, 0), new Vector3f(0, height, 1), new Vector3f(0, 0, 1), new Vector3f(0, 0, 0), white, sideTexture);
		
		// South edge
		subMesh.addQuad(new Vector3f(1, height, 1), new Vector3f(1, height, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 1), white, sideTexture);
		
		// East edge
		subMesh.addQuad(new Vector3f(1, height, 0), new Vector3f(0, height, 0), new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), white, sideTexture);
		
		// West edge
		subMesh.addQuad(new Vector3f(0, height, 1), new Vector3f(1, height, 1), new Vector3f(1, 0, 1), new Vector3f(0, 0, 1), white, sideTexture);
		
		
		final float texel = 1.0f / 16.0f;
		
		// Static torch
		addTorch(subMesh, texel*7, 0, texel*2);
		
		// Delay torch
		final int delay = (data>>2) & 0x3;
		
		final float yPixel = delay * 2 + 6; // Valid offsets are from 6 to 12
		addTorch(subMesh, texel*7, 0, texel*yPixel);
		
		// Now do rotation
		Rotation rotation = Rotation.None;
		float angle = 0;
		
		final int direction = data & 0x3;
		if (direction == 2)
		{
			// Facing east
			rotation = Rotation.Clockwise;
			angle = 180;
		}
		else if (direction == 3)
		{
			// Facing south
			rotation = Rotation.Clockwise;
			angle = 90;
		}
		else if (direction == 0)
		{
			// Facing west (built direction)
		}
		else if (direction == 1)
		{
			// Facing south
			rotation = Rotation.AntiClockwise;
			angle = 90;			
		}
		
		Mesh mesh = geometry.getMesh(baseTexture.texture, Geometry.MeshType.AlphaTest);
		subMesh.pushTo(mesh, x, y, z, rotation, angle);
	}
	
	private void addTorch(SubMesh subMesh, float x, float y, float z)
	{
		final float lightness = 1.0f;
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		
		final float leftSide = 7.0f / 16.0f;
		final float rightSide = 9.0f / 16.0f;
		final float height = 10.0f / 16.0f;
		
		final float texel = 1.0f / 16.0f;
		
		// Shift so x/y/z of zero starts the torch just next to the origin
		x -= texel*7;
		z -= texel*7;
		
		// Data defines torch placement
		// 0x1: Pointing south
		// 0x2: Pointing north
		// 0x3; Pointing west
		// 0x4: Pointing east
		// 0x5: Standing on the floor
		
		final float bottomOffsetX;
		final float bottomOffsetZ;
		final float bottomOffsetY;
		/*
		if (data == 1)
		{
			// Pointing south
			bottomOffsetX = -0.5f;
			bottomOffsetZ = 0.0f;
			bottomOffsetY = 0.4f;
		}
		else if (data == 2)
		{
			// Pointing north
			bottomOffsetX = 0.5f;
			bottomOffsetZ = 0.0f;
			bottomOffsetY = 0.4f;
		}
		else if (data == 3)
		{
			// Pointing west
			bottomOffsetX = 0.0f;
			bottomOffsetZ = -0.5f;
			bottomOffsetY = 0.4f;
		}
		else if (data == 4)
		{
			// Pointing east
			bottomOffsetX = 0.0f;
			bottomOffsetZ = 0.5f;
			bottomOffsetY = 0.4f;
		}
		else
		*/
		{
			// Standing on the floor
			bottomOffsetX = 0.0f;
			bottomOffsetZ = 0.0f;
			bottomOffsetY = 0.0f;
		}
		
		// Top
		subMesh.addQuad(		new Vector3f(x+leftSide,	y+height+bottomOffsetY,	z+leftSide),
								new Vector3f(x+rightSide,	y+height+bottomOffsetY,	z+leftSide),
								new Vector3f(x+rightSide,	y+height+bottomOffsetY,	z+rightSide),
								new Vector3f(x+leftSide,	y+height+bottomOffsetY,	z+rightSide),
								new Vector4f(colour.x * lightness, colour.y * lightness, colour.z * lightness, colour.w),
								torchTopTexture);
		
		// North
		subMesh.addQuad(		new Vector3f(x+leftSide,					y+height+bottomOffsetY,	z),
								new Vector3f(x+leftSide,					y+height+bottomOffsetY,	z+1),
								new Vector3f(x+leftSide + bottomOffsetX,	y+bottomOffsetY,			z+1 + bottomOffsetZ),
								new Vector3f(x+leftSide + bottomOffsetX,	y+bottomOffsetY,			z + bottomOffsetZ),
								new Vector4f(colour.x * lightness, colour.y * lightness, colour.z * lightness, colour.w),
								torchSideTexture);
		
		// South
		subMesh.addQuad(		new Vector3f(x+rightSide,					y+height+bottomOffsetY,		z+1),
								new Vector3f(x+rightSide,					y+height+bottomOffsetY,		z),
								new Vector3f(x+rightSide + bottomOffsetX,	y+bottomOffsetY,			z + bottomOffsetZ),
								new Vector3f(x+rightSide + bottomOffsetX,	y+bottomOffsetY,			z+1 + bottomOffsetZ),
								new Vector4f(colour.x * lightness, colour.y * lightness, colour.z * lightness, colour.w),
								torchSideTexture);
		
		// East
		subMesh.addQuad(		new Vector3f(x+1,					y+height+bottomOffsetY,	z+leftSide),
								new Vector3f(x,						y+height+bottomOffsetY,	z+leftSide),
								new Vector3f(x + bottomOffsetX,		y+bottomOffsetY,			z+leftSide + bottomOffsetZ),
								new Vector3f(x+1 + bottomOffsetX,	y+bottomOffsetY,			z+leftSide + bottomOffsetZ),
								new Vector4f(colour.x * lightness, colour.y * lightness, colour.z * lightness, colour.w),
								torchSideTexture);
		
		// West
		subMesh.addQuad(		new Vector3f(x,						y+height+bottomOffsetY,	z+rightSide),
								new Vector3f(x+1,					y+height+bottomOffsetY,	z+rightSide),
								new Vector3f(x+1 + bottomOffsetX,	y+bottomOffsetY,			z+rightSide + bottomOffsetZ),
								new Vector3f(x + bottomOffsetX,		y+bottomOffsetY,			z+rightSide + bottomOffsetZ),
								new Vector4f(colour.x * lightness, colour.y * lightness, colour.z * lightness, colour.w),
								torchSideTexture);
	}
}
