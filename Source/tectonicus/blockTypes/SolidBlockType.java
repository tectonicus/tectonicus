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

public class SolidBlockType implements BlockType
{
	public static final float NORTH_SOUTH_LIGHT_DEC = 0.3f;
	public static final float EAST_WEST_LIGHT_DEC = 0.1f;
	
	private String name;
	private SubTexture sideTexture, topTexture;
	private Colour4f colour;
	private boolean alphaTest;
	
	public SolidBlockType(String name, SubTexture sideTexture, SubTexture topTexture)
	{
		init(name, sideTexture, topTexture, null, false);
	}
	
	public SolidBlockType(String name, SubTexture subTexture)
	{
		init(name, subTexture, subTexture, null, false);
	}
	
	public SolidBlockType(String name, SubTexture subTexture, final boolean alphaTest)
	{
		init(name, subTexture, subTexture, null, alphaTest);
	}
	
	public SolidBlockType(String name, SubTexture side, SubTexture top, final boolean alphaTest)
	{
		init(name, side, top, null, alphaTest);
	}
	
	public SolidBlockType(String name, SubTexture subTexture, Colour4f colour, final boolean alphaTest)
	{
		init(name, subTexture, subTexture, colour, alphaTest);
	}
	
	private void init(String name, SubTexture sideTexture, SubTexture topTexture, Colour4f colour, final boolean alphaTest)
	{
		if (name == null)
			throw new RuntimeException("name is null!");
		if (sideTexture == null)
			throw new RuntimeException("side subtexture is null!");
		if (topTexture == null)
			throw new RuntimeException("top subtexture is null!");
		
		this.name = name;
		this.sideTexture = sideTexture;
		this.topTexture = topTexture;
		this.alphaTest = alphaTest;
		this.colour = colour;
		if (colour == null)
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
		Mesh topMesh = geometry.getMesh(topTexture.texture, alphaTest ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, alphaTest ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid);
		
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
		Mesh topMesh = geometry.getMesh(topTexture.texture, alphaTest ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, alphaTest ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid);
		
		BlockUtil.addTop(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		BlockUtil.addBottom(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addNorth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addSouth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addEast(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addWest(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
	}
	
}
