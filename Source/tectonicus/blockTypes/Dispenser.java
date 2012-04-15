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

public class Dispenser implements BlockType
{
	private SubTexture top, side, front;
	
	public Dispenser(SubTexture top, SubTexture side, SubTexture front)
	{
		this.top = top;
		this.side = side;
		this.front = front;
	}

	@Override
	public String getName()
	{
		return "Dispenser";
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(top.texture, Geometry.MeshType.Solid);
		
		Colour4f colour = new Colour4f(1, 1, 1, 1);
		
		final int data = chunk.getBlockData(x, y, z);

		// 0x2: Facing east
		// 0x3: Facing west
		// 0x4: Facing north
		// 0x5: Facing south
		
		SubTexture northTex = data == 0x4 ? front : side;
		SubTexture southTex = data == 0x5 ? front : side;
		SubTexture eastTex = data == 0x4 ? front : side;
		SubTexture westTex = data == 0x3 ? front : side;
		
		BlockUtil.addTop(world, chunk, mesh, x, y, z, colour, top, registry);
		BlockUtil.addBottom(world, chunk, mesh, x, y, z, colour, top, registry);
		
		BlockUtil.addNorth(world, chunk, mesh, x, y, z, colour, northTex, registry);
		BlockUtil.addSouth(world, chunk, mesh, x, y, z, colour, southTex, registry);
		BlockUtil.addEast(world, chunk, mesh, x, y, z, colour, eastTex, registry);
		BlockUtil.addWest(world, chunk, mesh, x, y, z, colour, westTex, registry);
	}
	
}
