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
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Fire implements BlockType
{
	private final String name;
	
	private final SubTexture texture;
	
	public Fire(String name, SubTexture texture)
	{
		if (texture == null)
			throw new RuntimeException("fire texture is null!");
		
		this.name = name;
		this.texture = texture;
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		
		addFireGeometry(x, y, z, mesh, colour, texture);
	}
	
	public static void addFireGeometry(final float x, final float y, final float z, Mesh mesh, Vector4f colour, SubTexture texture)
	{
		final float offset = 0.7f;
		
		// North edge
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x,		y+1,	z),
											new Vector3f(x,		y+1,	z+1),
											new Vector3f(x,		y,		z+1),
											new Vector3f(x,		y,		z),
											colour,
											texture);
		
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x+offset,	y+1,	z),
											new Vector3f(x+offset,	y+1,	z+1),
											new Vector3f(x,		y,		z+1),
											new Vector3f(x,		y,		z),
											colour,
											texture);
		
		// South edge
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x+1,		y+1,	z),
											new Vector3f(x+1,		y+1,	z+1),
											new Vector3f(x+1,		y,		z+1),
											new Vector3f(x+1,		y,		z),
											colour,
											texture);
		
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x+1-offset,	y+1,	z),
											new Vector3f(x+1-offset,	y+1,	z+1),
											new Vector3f(x+1,			y,		z+1),
											new Vector3f(x+1,			y,		z),
											colour,
											texture);
		
		// East edge
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x,			y+1,	z),
											new Vector3f(x+1,		y+1,	z),
											new Vector3f(x+1,		y,		z),
											new Vector3f(x,			y,		z),
											colour,
											texture);
		
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x,			y+1,	z+offset),
											new Vector3f(x+1,		y+1,	z+offset),
											new Vector3f(x+1,		y,		z),
											new Vector3f(x,			y,		z),
											colour,
											texture);
		
		// West edge
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x,			y+1,	z+1),
											new Vector3f(x+1,		y+1,	z+1),
											new Vector3f(x+1,		y,		z+1),
											new Vector3f(x,			y,		z+1),
											colour,
											texture);
		
		MeshUtil.addDoubleSidedQuad(mesh,	new Vector3f(x,			y+1,	z+1-offset),
											new Vector3f(x+1,		y+1,	z+1-offset),
											new Vector3f(x+1,		y,		z+1),
											new Vector3f(x,			y,		z+1),
											colour,
											texture);
	}
}
