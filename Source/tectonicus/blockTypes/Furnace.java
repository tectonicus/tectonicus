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

public class Furnace implements BlockType
{
	private final String name;
	
	private final SubTexture topTexture;
	private final SubTexture sideTexture;
	private final SubTexture frontTexture;
	
	public Furnace(String name, SubTexture topTexture, SubTexture sideTexture, SubTexture frontTexture)
	{
		this.name = name;
		
		this.topTexture = topTexture;
		this.sideTexture = sideTexture;
		this.frontTexture = frontTexture;
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		Mesh topMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);
		Mesh frontMesh = geometry.getMesh(frontTexture.texture, Geometry.MeshType.Solid);

		Colour4f colour = new Colour4f(1, 1, 1, 1);

		final int data = chunk.getBlockData(x, y, z);
		
		// 0x2: Facing east
		// 0x3: Facing west
		// 0x4: Facing north
		// 0x5: Facing south
		
		SubTexture northTex = data == 0x4 ? frontTexture : sideTexture;
		SubTexture southTex = data == 0x5 ? frontTexture : sideTexture;
		SubTexture eastTex = data == 0x2 ? frontTexture : sideTexture;
		SubTexture westTex = data == 0x3 ? frontTexture : sideTexture;
		
		Mesh northMesh = data == 0x4 ? frontMesh : sideMesh;
		Mesh southMesh = data == 0x4 ? frontMesh : sideMesh;
		Mesh eastMesh = data == 0x4 ? frontMesh : sideMesh;
		Mesh westMesh = data == 0x4 ? frontMesh : sideMesh;
		
		BlockUtil.addTop(world, chunk, topMesh, x, y, z, colour, topTexture, registry);
		BlockUtil.addBottom(world, chunk, topMesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addNorth(world, chunk, northMesh, x, y, z, colour, northTex, registry);
		BlockUtil.addSouth(world, chunk, southMesh, x, y, z, colour, southTex, registry);
		BlockUtil.addEast(world, chunk, eastMesh, x, y, z, colour, eastTex, registry);
		BlockUtil.addWest(world, chunk, westMesh, x, y, z, colour, westTex, registry);
	}
}
