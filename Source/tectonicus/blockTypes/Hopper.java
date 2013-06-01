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
 *   * Neither the name of 'Tectonicus' nor the names of
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
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Hopper implements BlockType
{
	private final String name;
	
	private final SubTexture top, side, inside, insideSide;
	
	public Hopper(String name, SubTexture top, SubTexture side, SubTexture inside)
	{
		this.name = name;
		
		this.top = top;
		this.side = side;
		this.inside = inside;
		
		final float texel = 1.0f / 16.0f;
		this.insideSide = new SubTexture(side.texture, side.u0, side.v0, side.u1, side.v0+texel*6);
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
		Mesh topMesh = geometry.getMesh(top.texture, Geometry.MeshType.AlphaTest);
		Mesh insideMesh = geometry.getMesh(side.texture, Geometry.MeshType.AlphaTest);
		Mesh insideBottomMesh = geometry.getMesh(inside.texture, Geometry.MeshType.AlphaTest);
		SubMesh sideMesh = new SubMesh();
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		
		Vector4f colour = new Vector4f(lightness, lightness, lightness, 1.0f);
		
		final int data = rawChunk.getBlockData(x, y, z);
		
		final float offSet = 1.0f / 16.0f;
		
		// Top
		MeshUtil.addQuad(topMesh, new Vector3f(x,	y+1, z),
								  new Vector3f(x+1,	y+1, z),
								  new Vector3f(x+1,	y+1, z+1),
								  new Vector3f(x, 	y+1, z+1),
								  colour, top);
		
		// Inside Bottom
		MeshUtil.addQuad(insideBottomMesh, new Vector3f(x,	y+offSet*10, z),
									new Vector3f(x+1,	y+offSet*10, z),
									new Vector3f(x+1,	y+offSet*10, z+1),
									new Vector3f(x, 	y+offSet*10, z+1),
									colour, inside);
		
		SubMesh.addBlock(sideMesh, 0, offSet*10, 0, offSet*16, offSet*6, offSet*16, colour, side, null, side);
		SubMesh.addBlock(sideMesh, offSet*4, offSet*4, offSet*4, offSet*8, offSet*6, offSet*8, colour, side, null, side);
		
		// Inv north
		MeshUtil.addQuad(insideMesh, new Vector3f(x+1-offSet*2, y+1, z),
				   				   new Vector3f(x+1-offSet*2, y+1, z+1),
				   				   new Vector3f(x+1-offSet*2, y+offSet*10,   z+1),
				   				   new Vector3f(x+1-offSet*2, y+offSet*10,   z),
				   				   colour, insideSide);
		
		// Inv south
		MeshUtil.addQuad(insideMesh, new Vector3f(x+offSet*2, y+1, z+1),
								   new Vector3f(x+offSet*2, y+1, z),
								   new Vector3f(x+offSet*2, y+offSet*10,   z),
								   new Vector3f(x+offSet*2, y+offSet*10,   z+1),
								   colour, insideSide);
		
		// Inv west
		MeshUtil.addQuad(insideMesh, new Vector3f(x,		y+1, z+offSet*2),
								   new Vector3f(x+1,	y+1, z+offSet*2),
								   new Vector3f(x+1,	y+offSet*10,   z+offSet*2),
								   new Vector3f(x,		y+offSet*10,   z+offSet*2),
								   colour, insideSide);
		
		// Inv east
		MeshUtil.addQuad(insideMesh, new Vector3f(x+1,	y+1, z+1-offSet*2),
								   new Vector3f(x,		y+1, z+1-offSet*2),
								   new Vector3f(x,		y+offSet*10,   z+1-offSet*2),
								   new Vector3f(x+1,	y+offSet*10,   z+1-offSet*2),
								   colour, insideSide);
		
		
		if(data == 0 || data == 1)
			SubMesh.addBlock(sideMesh, offSet*6, 0, offSet*6, offSet*4, offSet*4, offSet*4, colour, side, null, side);
		else if(data == 2) // Attached to north
			SubMesh.addBlock(sideMesh, offSet*6, offSet*4, 0, offSet*4, offSet*4, offSet*4, colour, side, side, side);
		else if(data == 3) // Attached to south
			SubMesh.addBlock(sideMesh, offSet*6, offSet*4, offSet*12, offSet*4, offSet*4, offSet*4, colour, side, side, side);
		else if(data == 4) // Attached to west
			SubMesh.addBlock(sideMesh, 0, offSet*4, offSet*6, offSet*4, offSet*4, offSet*4, colour, side, side, side);
		else if(data == 5) // Attached to east
			SubMesh.addBlock(sideMesh, offSet*12, offSet*4, offSet*6, offSet*4, offSet*4, offSet*4, colour, side, side, side);
		
		sideMesh.pushTo(geometry.getMesh(side.texture, Geometry.MeshType.AlphaTest), x, y, z, Rotation.None, 0);
	}
}
