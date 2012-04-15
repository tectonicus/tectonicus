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

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class DataSolid implements BlockType
{
	private final String name;
	
	private SubTexture[] sideTextures;
	private SubTexture[] topTextures;
	
	private boolean alphaTest;
	
	public DataSolid(String name, SubTexture[] sideTexture, SubTexture[] topTextures, boolean alphaTest)
	{
		this.name = name;
	
		this.sideTextures = sideTexture;
		this.topTextures = topTextures;
		
		this.alphaTest = alphaTest;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean isSolid()
	{
		return !alphaTest;
	}
	
	@Override
	public boolean isWater()
	{
		return false;
	}
	
	@Override
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		final int data = rawChunk.getBlockData(x, y, z);
		
		int sideIndex = data;
		if (data >= sideTextures.length)
			sideIndex = 0;
		
		int topIndex = data;
		if (data >= topTextures.length)
			topIndex = 0;
		
		SubTexture topTexture = topTextures[topIndex];
		SubTexture sideTexture = sideTextures[sideIndex];
		
		Geometry.MeshType type = alphaTest ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid;
		
		Mesh topMesh = geometry.getMesh(topTexture.texture, type);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, type);
		
		Colour4f colour = new Colour4f(1, 1, 1, 1);
		
		BlockUtil.addInteriorTop(world, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		BlockUtil.addInteriorBottom(world, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addInteriorNorth(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorSouth(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorEast(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorWest(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
	}
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext context, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Colour4f colour = new Colour4f(1, 1, 1, 1);
		
		final int data = rawChunk.getBlockData(x, y, z);
		
		int sideIndex = data;
		if (data >= sideTextures.length)
			sideIndex = 0;
		
		int topIndex = data;
		if (data >= topTextures.length)
			topIndex = 0;
		
		SubTexture sideTexture = sideTextures[sideIndex];
		SubTexture topTexture = topTextures[topIndex];
		
		Geometry.MeshType type = alphaTest ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid;
		
		Mesh topMesh = geometry.getMesh(topTexture.texture, type);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, type);
		
		BlockUtil.addTop(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		BlockUtil.addBottom(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addNorth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addSouth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addEast(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addWest(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
	}
	
}
