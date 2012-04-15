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

public class Log implements BlockType
{
	private final String name;
	private final SubTexture normalSideTexture, spruceSideTexture, birtchSideTexture, topTexture;
	private Colour4f colour;
	
	public Log(String name, SubTexture normalSideTex, SubTexture spruceSideTex, SubTexture birchSideTex, SubTexture topTexture)
	{
		if (topTexture == null)
			throw new RuntimeException("top subtexture is null!");
		
		this.name = name;
		this.normalSideTexture = normalSideTex;
		this.spruceSideTexture = spruceSideTex;
		this.birtchSideTexture = birchSideTex;
		this.topTexture = topTexture;
		this.colour = new Colour4f(1, 1, 1, 1);
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean isSolid()
	{
		return true;
	}
	
	@Override
	public boolean isWater()
	{
		return false;
	}
	
	@Override
	public void addInteriorGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		SubTexture sideTexture = getSideTexture(rawChunk.getBlockData(x, y, z));
		
		Mesh topMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);
		
		BlockUtil.addInteriorTop(world, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addInteriorNorth(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorSouth(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorEast(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorWest(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
	}
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext context, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh mesh = geometry.getBaseMesh();
		
		SubTexture sideTexture = getSideTexture(rawChunk.getBlockData(x, y, z));
		
		BlockUtil.addTop(context, rawChunk, mesh, x, y, z, colour, topTexture, registry);
		BlockUtil.addBottom(context, rawChunk, mesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addNorth(context, rawChunk, mesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addSouth(context, rawChunk, mesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addEast(context, rawChunk, mesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addWest(context, rawChunk, mesh, x, y, z, colour, sideTexture, registry);
	}
	
	private SubTexture getSideTexture(final int data)
	{
		if (data == 0)
		{
			return normalSideTexture;
		}
		else if (data == 1)
		{
			return spruceSideTexture;
		}
		else if (data == 2)
		{
			return birtchSideTexture;
		}
		return normalSideTexture;
	}
	
}
