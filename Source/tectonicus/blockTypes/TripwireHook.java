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

public class TripwireHook implements BlockType
{	
	private final String name;

	private final SubTexture base, hookRing, hookRingSide, hookPole;
	
	public TripwireHook(String name, SubTexture base, SubTexture hook)
	{
		this.name = name;
		
		this.base = base;
		
		final float texel;
		if (base.texturePackVersion == "1.4")
			texel = 1.0f / 16.0f / 16.0f;
		else
			texel = 1.0f / 16.0f;
		
		this.hookRing = new SubTexture(hook.texture, hook.u0+texel*5, hook.v0+texel*3, hook.u0+texel*11, hook.v1-texel*7);
		this.hookRingSide = new SubTexture(hook.texture, hook.u0+texel*5, hook.v0+texel*3, hook.u0+texel*7, hook.v1-texel*7);
		this.hookPole = new SubTexture(hook.texture, hook.u0+texel*7, hook.v0+texel*10, hook.u0+texel*9, hook.v1);
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
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		
		final float offSet = 1.0f / 16.0f;
		
		SubMesh baseMesh = new SubMesh();
		SubMesh leverMesh = new SubMesh();
		SubMesh hookMesh = new SubMesh();
				
		
		
		Rotation horizRotation = Rotation.Clockwise;
		float horizAngle = 0;
		
		Rotation vertRotation = Rotation.None;
		float vertAngle = 0;
		
		// Set angle/rotation from block data flags
		if(data == 3 || data == 7) // Facing east
		{
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.Clockwise;
			horizAngle = 180;		
		}
		else if (data == 1 || data == 5) // Facing west
		{
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
		}
		else if (data == 0 || data == 4) // Facing south
		{
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.Clockwise;
			horizAngle = 90;
		}
		else if (data == 2 || data == 6) // Facing north
		{
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.AntiClockwise;
			horizAngle = 90;
		}
		
		float lrotate = 0;
		float hrotate = 0;
		
		SubMesh.addBlock(baseMesh, offSet, 0, offSet*6, offSet*8, offSet*2, offSet*4, white, base, base, base);
		if((data & 0x4) > 0)
		{
			SubMesh.addBlockSimple(leverMesh, offSet*5, 0, offSet*7.2f, offSet*1.6f, offSet*6, offSet*1.6f, white, hookPole, hookPole, hookPole);
			SubMesh.addBlockSimple(hookMesh, offSet*6, offSet*5.9f, offSet*6.25f, offSet*0.5f, offSet*3.5f, offSet*3.5f, white, hookRing, hookRingSide, hookRingSide);
			lrotate = vertAngle+5;
			hrotate = vertAngle+8;
		}
		else
		{
			SubMesh.addBlockSimple(leverMesh, offSet*10, offSet, offSet*7.2f, offSet*1.6f, offSet*6, offSet*1.6f, white, hookPole, hookPole, hookPole);
			SubMesh.addBlockSimple(hookMesh, offSet*7, offSet*5, offSet*6.25f, offSet*0.5f, offSet*3.5f, offSet*3.5f, white, hookRing, hookRingSide, hookRingSide);
			
			lrotate = vertAngle-45;
			hrotate = vertAngle+45;
		}
		
		baseMesh.pushTo(geometry.getMesh(base.texture, Geometry.MeshType.AlphaTest), x, y, z, horizRotation, horizAngle, vertRotation, vertAngle);
		leverMesh.pushTo(geometry.getMesh(hookPole.texture, Geometry.MeshType.AlphaTest), x, y, z, horizRotation, horizAngle, vertRotation, lrotate);
		hookMesh.pushTo(geometry.getMesh(hookRing.texture, Geometry.MeshType.AlphaTest), x, y, z, horizRotation, horizAngle, vertRotation, hrotate);
	}
}