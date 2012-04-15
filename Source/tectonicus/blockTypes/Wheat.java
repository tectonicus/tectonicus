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
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Wheat implements BlockType
{
	private final String name;
	private final SubTexture[] textures;
	
	public Wheat(String name, SubTexture t0, SubTexture t1, SubTexture t2, SubTexture t3, SubTexture t4, SubTexture t5, SubTexture t6, SubTexture t7)
	{
		this.name = name;
		
		textures = new SubTexture[8];
		textures[0] = t0;
		textures[1] = t1;
		textures[2] = t2;
		textures[3] = t3;
		textures[4] = t4;
		textures[5] = t5;
		textures[6] = t6;
		textures[7] = t7;
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
		final float lightVal = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
		
		Vector4f colour = new Vector4f(lightVal, lightVal, lightVal, 1.0f);
	
		final int data = rawChunk.getBlockData(x, y, z);
		
		SubTexture texture = textures[data];
		
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		// NE corner to SW corner
		MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z),
								new Vector3f(x+1,	y+1,	z+1),
								new Vector3f(x+1,	y,		z+1),
								new Vector3f(x,		y,		z),
								colour,
								texture);
	
		// SE corner to NW corner
		MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+1,	z),
								new Vector3f(x,		y+1,	z+1),
								new Vector3f(x,		y,		z+1),
								new Vector3f(x+1,	y,		z),
								colour,
								texture); 
	
		// SW corner to NE corner
		MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+1,	z+1),
								new Vector3f(x,		y+1,	z),
								new Vector3f(x,		y,		z),
								new Vector3f(x+1,	y,		z+1),
								colour,
								texture);
		
		// NW corner to SE corner
		MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z+1),
								new Vector3f(x+1,	y+1,	z),
								new Vector3f(x+1,	y,		z),
								new Vector3f(x,		y,		z+1),
								colour,
								texture); 
	}
	
}
