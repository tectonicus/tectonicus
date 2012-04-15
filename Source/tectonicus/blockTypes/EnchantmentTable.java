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

public class EnchantmentTable implements BlockType
{
	private final String name;
	
	private final SubTexture top, side, bottom;
	
	public EnchantmentTable(String name, SubTexture top, SubTexture side, SubTexture bottom)
	{
		this.name = name;
		this.top = top;
		this.side = side;
		this.bottom = bottom;
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
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, chunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh topMesh = geometry.getMesh(top.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(side.texture, Geometry.MeshType.AlphaTest);
		Mesh bottomMesh = geometry.getMesh(bottom.texture, Geometry.MeshType.Solid);
		
		final float topLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.NorthSouth);
		final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.EastWest);
		
		final float height = 12.0f / 16.0f;
		
		MeshUtil.addQuad(topMesh, new Vector3f(x,		y+height,	z),
								  new Vector3f(x+1,		y+height,	z),
								  new Vector3f(x+1,		y+height,	z+1),
								  new Vector3f(x,		y+height,	z+1),
								  new Vector4f(topLight, topLight, topLight, 1.0f),
								  top);
		
		MeshUtil.addQuad(bottomMesh,	new Vector3f(x+1,	y,	z),
								  		new Vector3f(x,		y,	z),
								  		new Vector3f(x,		y,	z+1),
								  		new Vector3f(x+1,	y,	z+1),
								  		new Vector4f(topLight, topLight, topLight, 1.0f),
								  		bottom);
		
		MeshUtil.addQuad(sideMesh,	new Vector3f(x,		y+1,	z),
									new Vector3f(x,		y+1,	z+1),
									new Vector3f(x,		y,		z+1),
									new Vector3f(x,		y,		z),
									new Vector4f(northSouthLight, northSouthLight, northSouthLight, 1.0f),
									side);

		MeshUtil.addQuad(sideMesh,	new Vector3f(x+1,	y+1,	z+1),
									new Vector3f(x+1,	y+1,	z),
									new Vector3f(x+1,	y,		z),
									new Vector3f(x+1,	y,		z+1),
									new Vector4f(northSouthLight, northSouthLight, northSouthLight, 1.0f),
									side);

		MeshUtil.addQuad(sideMesh,	new Vector3f(x+1,	y+1,	z),
									new Vector3f(x,		y+1,	z),
									new Vector3f(x,		y,		z),
									new Vector3f(x+1,	y,		z),
									new Vector4f(eastWestLight, eastWestLight, eastWestLight, 1.0f),
									side);

		MeshUtil.addQuad(sideMesh,	new Vector3f(x,		y+1,	z+1),
									new Vector3f(x+1,	y+1,	z+1),
									new Vector3f(x+1,	y,		z+1),
									new Vector3f(x,		y,		z+1),
									new Vector4f(eastWestLight, eastWestLight, eastWestLight, 1.0f),
									side);
	}
}
