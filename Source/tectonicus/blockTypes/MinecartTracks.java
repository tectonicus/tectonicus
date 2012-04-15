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
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class MinecartTracks implements BlockType
{
	private final String name;
	private final SubTexture straightTexture, cornerTexture, poweredTexture;
	private final boolean straightOnly;
	
	public MinecartTracks(String name, SubTexture straight, SubTexture corner, SubTexture powered, final boolean straightOnly)
	{
		this.name = name;
		this.straightTexture = straight;
		this.cornerTexture = corner;
		this.poweredTexture = powered;
		this.straightOnly = straightOnly;
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
		Mesh mesh = geometry.getMesh(straightTexture.texture, Geometry.MeshType.AlphaTest);
		
		// TODO: Should we change the light if the track is inclined?
		final float light = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.Top);
		
		final int data = chunk.getBlockData(x, y, z);

		int orientation;
		if (straightOnly)
			orientation = data & 0x7;
		else
			orientation = data;
		final boolean isPowered = (data & 0x8) > 0;
		
		Vector2f uv0 = new Vector2f();
		Vector2f uv1 = new Vector2f();
		Vector2f uv2 = new Vector2f();
		Vector2f uv3 = new Vector2f();
		
		final float groundOffset = 1.0f / 16.0f;
		
		float p0Height = y + groundOffset;
		float p1Height = y + groundOffset;
		float p2Height = y + groundOffset;
		float p3Height = y + groundOffset;
		
		SubTexture straightTex = isPowered ? poweredTexture : straightTexture;
		
		switch (orientation)
		{
			case 0x0:
			{
				// flat track going east-west
				uv0.set(straightTex.u0, straightTex.v0);
				uv1.set(straightTex.u1, straightTex.v0);
				uv2.set(straightTex.u1, straightTex.v1);
				uv3.set(straightTex.u0, straightTex.v1);
				break;
			}
			case 0x1:
			{
				// flat track going north-south
				uv0.set(straightTex.u1, straightTex.v0);
				uv1.set(straightTex.u1, straightTex.v1);
				uv2.set(straightTex.u0, straightTex.v1);
				uv3.set(straightTex.u0, straightTex.v0);
				break;
			}
			case 0x2:
			{
				// track ascending to the south
				uv0.set(straightTex.u1, straightTex.v0);
				uv1.set(straightTex.u1, straightTex.v1);
				uv2.set(straightTex.u0, straightTex.v1);
				uv3.set(straightTex.u0, straightTex.v0);
				
				p1Height++;
				p2Height++;
				break;
			}
			case 0x3:
			{
				// track ascending to the north
				uv0.set(straightTex.u1, straightTex.v0);
				uv1.set(straightTex.u1, straightTex.v1);
				uv2.set(straightTex.u0, straightTex.v1);
				uv3.set(straightTex.u0, straightTex.v0);
				
				p0Height++;
				p3Height++;
				break;
			}
			case 0x4:
			{
				// track ascending to the east
				uv0.set(straightTex.u0, straightTex.v0);
				uv1.set(straightTex.u1, straightTex.v0);
				uv2.set(straightTex.u1, straightTex.v1);
				uv3.set(straightTex.u0, straightTex.v1);
				
				p0Height++;
				p1Height++;
				break;
			}
			case 0x5:
			{
				// track ascending to the west
				uv0.set(straightTex.u0, straightTex.v0);
				uv1.set(straightTex.u1, straightTex.v0);
				uv2.set(straightTex.u1, straightTex.v1);
				uv3.set(straightTex.u0, straightTex.v1);
				
				p2Height++;
				p3Height++;
				break;
			}
			case 0x6:
			{
				// north-east corner
				uv0.set(cornerTexture.u0, cornerTexture.v0);
				uv1.set(cornerTexture.u1, cornerTexture.v0);
				uv2.set(cornerTexture.u1, cornerTexture.v1);
				uv3.set(cornerTexture.u0, cornerTexture.v1);
				break;
			}
			case 0x7:
			{
				// south-east
				uv0.set(cornerTexture.u1, cornerTexture.v0);
				uv1.set(cornerTexture.u0, cornerTexture.v0);
				uv2.set(cornerTexture.u0, cornerTexture.v1);
				uv3.set(cornerTexture.u1, cornerTexture.v1);
				break;
			}
			case 0x8:
			{
				// south-west
				uv0.set(cornerTexture.u1, cornerTexture.v1);
				uv1.set(cornerTexture.u0, cornerTexture.v1);
				uv2.set(cornerTexture.u0, cornerTexture.v0);
				uv3.set(cornerTexture.u1, cornerTexture.v0);
				break;
			}
			case 0x9:
			{
				// north-west
				uv0.set(cornerTexture.u0, cornerTexture.v1);
				uv1.set(cornerTexture.u1, cornerTexture.v1);
				uv2.set(cornerTexture.u1, cornerTexture.v0);
				uv3.set(cornerTexture.u0, cornerTexture.v0);
				break;
			}
			default:
				assert (false);
		}
		
		MeshUtil.addQuad(mesh,	new Vector3f(x,		p0Height,	z),
								new Vector3f(x+1,	p1Height,	z),
								new Vector3f(x+1,	p2Height,	z+1),
								new Vector3f(x,		p3Height,	z+1),
								new Vector4f(light, light, light, 1.0f),
								uv0, uv1, uv2, uv3);
	}
	
}
