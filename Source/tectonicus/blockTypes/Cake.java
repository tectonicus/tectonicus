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
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Cake implements BlockType
{
	private final String name;
	
	private SubTexture top, side, interior;
	
	public Cake(String name, SubTexture top, SubTexture s, SubTexture interior)
	{
		this.name = name;
		
		this.top = top;
		
		final float half = ((1.0f / 16.0f) / 2.0f);
		this.side = new SubTexture(s.texture, s.u0, s.v0+half, s.u1, s.v1);
		this.interior = new SubTexture(interior.texture, interior.u0, interior.v0+half, interior.u1, interior.v1);
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
		
		Mesh topMesh = geometry.getMesh(top.texture, Geometry.MeshType.AlphaTest);
		Mesh sideMesh = geometry.getMesh(side.texture, Geometry.MeshType.AlphaTest);
		Mesh interiorMesh = geometry.getMesh(interior.texture, Geometry.MeshType.AlphaTest);
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, chunk, x, y, z);
		
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		
		final float texel = 1.0f / 16.0f;
		final float offset = texel * 2 * data;
		
		final float actualY = y + 0.5f;
		
		final float uRange = top.u1 - top.u0;
		final float uEdge = uRange / 16.0f;
		final float uSeg = (uRange / 8.0f);
		float uInc = uEdge + uSeg * data;
		
		// Top
		MeshUtil.addQuad(topMesh,	new Vector3f(x + texel + offset,	actualY, z),
									new Vector3f(x+1-texel,				actualY, z),
									new Vector3f(x+1-texel,				actualY, z+1),
									new Vector3f(x + texel + offset,	actualY, z+1),
									white,
									new Vector2f(top.u0+uInc, top.v0),
									new Vector2f(top.u1-uSeg, top.v0),
									new Vector2f(top.u1-uSeg, top.v1),
									new Vector2f(top.u0+uInc, top.v1)
					);
		
		// South
		MeshUtil.addQuad(sideMesh,	new Vector3f(x + texel + offset,	actualY,	z+1-texel),
									new Vector3f(x+1-texel,				actualY,	z+1-texel),
									new Vector3f(x+1-texel,				y,			z+1-texel),
									new Vector3f(x + texel + offset,	y,			z+1-texel),
									white,
									new Vector2f(side.u0+uInc, side.v0),
									new Vector2f(side.u1-uSeg, side.v0),
									new Vector2f(side.u1-uSeg, side.v1),
									new Vector2f(side.u0+uInc, side.v1)
					);

		// North
		MeshUtil.addQuad(sideMesh,	new Vector3f(x+1-texel,	actualY,	z+texel),
									new Vector3f(x+texel+offset,	actualY,	z+texel),
									new Vector3f(x+texel+offset,	y,			z+texel),
									new Vector3f(x+1-texel,	y,			z+texel),
									white,
									new Vector2f(side.u0+uInc, side.v0),
									new Vector2f(side.u1-uSeg, side.v0),
									new Vector2f(side.u1-uSeg, side.v1),
									new Vector2f(side.u0+uInc, side.v1)
					);
		
		// West
		SubTexture westTex = data == 0 ? side : interior;
		Mesh westMesh = data == 0 ? sideMesh : interiorMesh;
		MeshUtil.addQuad(westMesh,	new Vector3f(x+texel+offset,	actualY,	z+texel),
									new Vector3f(x+texel+offset,	actualY,	z+1-texel),
									new Vector3f(x+texel+offset,	y,			z+1-texel),
									new Vector3f(x+texel+offset,	y,			z+texel),
									white,
									new Vector2f(westTex.u0+uSeg, westTex.v0),
									new Vector2f(westTex.u1-uSeg, westTex.v0),
									new Vector2f(westTex.u1-uSeg, westTex.v1),
									new Vector2f(westTex.u0+uSeg, westTex.v1)
					);
		
		// East (always the same)
		MeshUtil.addQuad(sideMesh,	new Vector3f(x+1-texel, actualY,	z+1),
									new Vector3f(x+1-texel, actualY,	z),
									new Vector3f(x+1-texel, y,			z),
									new Vector3f(x+1-texel, y,			z+1),
									white,
									side);
	}
	
}
