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
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

public class Wool implements BlockType
{
	private static final int WHITE			= 0;
	private static final int ORANGE			= 1;
	private static final int MAGENTA		= 2;
	private static final int LIGHT_BLUE		= 3;
	private static final int YELLOW			= 4;
	private static final int LIGHT_GREEN	= 5;
	private static final int PINK			= 6;
	private static final int GREY			= 7;
	private static final int LIGHT_GREY		= 8;
	private static final int CYAN			= 9;
	private static final int PURPLE			= 10;
	private static final int BLUE			= 11;
	private static final int BROWN			= 12;
	private static final int DARK_GREEN		= 13;
	private static final int RED			= 14;
	private static final int BLACK			= 15;
	
	private final String name;
	
	private SubTexture[] textures;
	
	public Wool(String name, TexturePack texPack)
	{
		this.name = name;
		
		textures = new SubTexture[16];
		
		textures[WHITE] = texPack.getSubTile(0, 4);
		textures[ORANGE] = texPack.getSubTile(2, 13);		
		textures[MAGENTA] = texPack.getSubTile(2, 12);
		textures[LIGHT_BLUE] = texPack.getSubTile(2, 11);
		textures[YELLOW] = texPack.getSubTile(2, 10);
		textures[LIGHT_GREEN] = texPack.getSubTile(2, 9);
		
		textures[PINK] = texPack.getSubTile(2, 8);
		textures[GREY] = texPack.getSubTile(2, 7);
		textures[LIGHT_GREY] = texPack.getSubTile(1, 14);
		
		textures[CYAN] = texPack.getSubTile(1, 13);
		textures[PURPLE] = texPack.getSubTile(1, 12);
		textures[BLUE] = texPack.getSubTile(1, 11);
		textures[BROWN] = texPack.getSubTile(1, 10);
		
		textures[DARK_GREEN] = texPack.getSubTile(1, 9);
		textures[RED] = texPack.getSubTile(1, 8);
		textures[BLACK] = texPack.getSubTile(1, 7);
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
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		SubTexture texture = textures[rawChunk.getBlockData(x, y, z)];
		
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Solid);
		
		Colour4f colour = new Colour4f(1, 1, 1, 1);
		
		BlockUtil.addTop(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		BlockUtil.addBottom(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		
		BlockUtil.addNorth(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		BlockUtil.addSouth(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		BlockUtil.addEast(world, rawChunk, mesh, x, y, z, colour, texture, registry);
		BlockUtil.addWest(world, rawChunk, mesh, x, y, z, colour, texture, registry);
	}
	
}
