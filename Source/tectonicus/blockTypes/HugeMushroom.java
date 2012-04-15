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

public class HugeMushroom implements BlockType
{
	private final String name;
	
	private SubTexture cap, pores, stem;
	
	private Colour4f colour;
	
	public HugeMushroom(String name, SubTexture cap, SubTexture pores, SubTexture stem)
	{
		this.name = name;
		
		this.cap = cap;
		this.pores = pores;
		this.stem = stem;
		
		colour = new Colour4f(1, 1, 1, 1);
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
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, chunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		final int data = chunk.getBlockData(x, y, z);
		
		SubTexture top = pores;
		SubTexture bottom = pores;
		SubTexture north = pores;
		SubTexture south = pores;
		SubTexture east = pores;
		SubTexture west = pores;
		
		/*
		0	 Fleshy piece	 Pores on all sides
		1	 Corner piece	 Cap texture on top, directions 1 (cloud direction) and 2 (sunrise)
		2	 Side piece	 Cap texture on top and direction 2 (sunrise)
		3	 Corner piece	 Cap texture on top, directions 2 (sunrise) and 3 (cloud origin)
		4	 Side piece	 Cap texture on top and direction 1 (cloud direction)
		5	 Top piece	 Cap texture on top
		6	 Side piece	 Cap texture on top and direction 3 (cloud origin)
		7	 Corner piece	 Cap texture on top, directions 0 (sunset) and 1 (cloud direction)
		8	 Side piece	 Cap texture on top and direction 0 (sunset)
		9	 Corner piece	 Cap texture on top, directions 3 (cloud origin) and 0 (sunset)
		10	 Stem piece	 Stem texture on all four sides, pores on top and bottom
		*/
		// N
		//sets	rises
		//	S
		
		// cloud direction N
		// cloud origin S
		// sunrise E
		// sunset W
		
		switch (data)
		{
			case 0:
				// Pores on all sides
				break;
			case 1:
				// Corner - Cap texture on top, directions 1 (cloud direction) and 2 (sunrise)
				top = north = east = cap;
				break;
			case 2:
				// Side - Cap texture on top and direction 2 (sunrise)
				top = east = cap;
				break;
			case 3:
				// Corner - Cap texture on top, directions 2 (sunrise) and 3 (cloud origin)
				top = east = south = cap;
				break;
			case 4:
				// Side - Cap texture on top and direction 1 (cloud direction)
				top = north = cap;
				break;
			case 5:
				// Top - Cap texture on top
				top = cap;
				break;
			case 6:
				// Side - Cap texture on top and direction 3 (cloud origin)
				top = south = cap;
				break;
			case 7:
				// Corner - Cap texture on top, directions 0 (sunset) and 1 (cloud direction)
				top = west = north = cap;
				break;
			case 8:
				// Side - Cap texture on top and direction 0 (sunset)
				top = west = cap;
				break;
			case 9:
				// Corner - Cap texture on top, directions 3 (cloud origin) and 0 (sunset)
				top = south = west = cap;
				break;
			case 10:
				// Stem
				north = south = east = west = stem;
				break;
			default:
		}
		
		top = cap;
		
		Mesh topMesh = geometry.getMesh(top.texture, Geometry.MeshType.Solid);
		Mesh bottomMesh = geometry.getMesh(bottom.texture, Geometry.MeshType.Solid);
		
		Mesh northMesh = geometry.getMesh(north.texture, Geometry.MeshType.Solid);
		Mesh southMesh = geometry.getMesh(south.texture, Geometry.MeshType.Solid);
		Mesh eastMesh = geometry.getMesh(east.texture, Geometry.MeshType.Solid);
		Mesh westMesh = geometry.getMesh(west.texture, Geometry.MeshType.Solid);
		
		BlockUtil.addTop(world, chunk, topMesh, x, y, z, colour, top, registry);
		BlockUtil.addBottom(world, chunk, bottomMesh, x, y, z, colour, bottom, registry);
		
		BlockUtil.addNorth(world, chunk, northMesh, x, y, z, colour, north, registry);
		BlockUtil.addSouth(world, chunk, southMesh, x, y, z, colour, south, registry);
		BlockUtil.addEast(world, chunk, eastMesh, x, y, z, colour, east, registry);
		BlockUtil.addWest(world, chunk, westMesh, x, y, z, colour, west, registry);
	}
}
