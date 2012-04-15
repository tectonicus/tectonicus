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

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

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

public class Bed implements BlockType
{
	private final SubTexture headTop, footTop;
	private final SubTexture headSide, footSide;
	private final SubTexture headEdge, footEdge;
	
	public Bed(SubTexture headTop, SubTexture footTop, SubTexture headSide, SubTexture footSide, SubTexture headEdge, SubTexture footEdge)
	{
		this.headTop = headTop;
		this.footTop = footTop;
		
		final float vHeight = 1.0f / 16.0f / 16.0f * 9.0f;
		this.headSide = new SubTexture(headSide.texture, headSide.u0, headSide.v0+vHeight, headSide.u1, headSide.v1);
		this.footSide = new SubTexture(footSide.texture, footSide.u0, footSide.v0+vHeight, footSide.u1, footSide.v1);
		
		this.headEdge = new SubTexture(headEdge.texture, headEdge.u0, headEdge.v0+vHeight, headEdge.u1, headEdge.v1);
		this.footEdge = new SubTexture(footEdge.texture, footEdge.u0, footEdge.v0+vHeight, footEdge.u1, footEdge.v1);
	}
	
	@Override
	public String getName()
	{
		return "Bed";
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
		final int data = rawChunk.getBlockData(x, y, z);
		final boolean isHead = (data & 0x8) > 0;
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		
		final float height = 1.0f / 16.0f * 9.0f;
		
		
		SubMesh subMesh = new SubMesh();
		
		// Create the geometry in the unit cube
		
		SubTexture topTex = isHead ? headTop : footTop;
		subMesh.addQuad(new Vector3f(0, height, 0), new Vector3f(1, height, 0), new Vector3f(1, height, 1), new Vector3f(0, height, 1), white, topTex);
		
		// Head or feet sides
		if (isHead)
		{
			subMesh.addQuad(new Vector3f(1, height, 1), new Vector3f(1, height, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 1), white, headSide);
		}
		else
		{
			subMesh.addQuad(new Vector3f(0, height, 0), new Vector3f(0, height, 1), new Vector3f(0, 0, 1), new Vector3f(0, 0, 0), white, footSide);
		}
		
		// Left side (if lying down in bed facing up)
		subMesh.addQuad(new Vector3f(0, height, 1), new Vector3f(1, height, 1), new Vector3f(1, 0, 1), new Vector3f(0, 0, 1), white, isHead ? headEdge : footEdge);
		
		// Right side
		SubTexture rightSide = isHead ? headEdge : footEdge;
		subMesh.addQuad(new Vector3f(1, height, 0), new Vector3f(0, height, 0), new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), white,
						new Vector2f(rightSide.u1, rightSide.v0), new Vector2f(rightSide.u0, rightSide.v0),new Vector2f(rightSide.u0, rightSide.v1), new Vector2f(rightSide.u1, rightSide.v1) );
		
		SubMesh.Rotation rotation = Rotation.None;
		float angle = 0;
		
		final int dir = (data & 0x3);
		if (dir == 0)
		{
			// Head is pointing west
			rotation = Rotation.AntiClockwise;
			angle = 90;
		}
		else if (dir == 1)
		{
			// Head is pointing north
			rotation = Rotation.Clockwise;
			angle = 180;
		}
		else if (dir == 2)
		{
			// Head is pointing east
			rotation = Rotation.Clockwise;
			angle = 90;
		}
		else if (dir == 3)
		{
			// Head is pointing south
		}
		else
		{
			System.err.println("Warning: Unknown block data for bed");
		}
		
		// Apply rotation
		subMesh.pushTo(geometry.getMesh(headTop.texture, Geometry.MeshType.AlphaTest), x, y, z, rotation, angle);
	}
	
}
