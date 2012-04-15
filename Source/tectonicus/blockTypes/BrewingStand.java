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

public class BrewingStand implements BlockType
{
	private final String name;
	
	private final SubTexture base;
	private final SubTexture stand;
	private final SubTexture post;
	private final SubTexture standWithBottle;
	private final SubTexture standWithoutBottle;
	
	public BrewingStand(String name, SubTexture base, SubTexture stand)
	{
		this.name = name;
		
		this.base = base;
		this.stand = stand;
		
		this.post = new SubTexture(stand.texture, stand.u0, stand.v1, stand.u1, stand.v0);
		
		final float midU = (stand.u0 + stand.u1) / 2.0f;
		this.standWithBottle = new SubTexture(stand.texture, stand.u0, stand.v0, midU, stand.v1);
		this.standWithoutBottle = new SubTexture(stand.texture, stand.u1, stand.v0, midU, stand.v1);
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world,BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh baseMesh = geometry.getMesh(base.texture, Geometry.MeshType.Solid);
		Mesh standMesh = geometry.getMesh(stand.texture, Geometry.MeshType.Solid);
		Mesh bottleMesh = geometry.getMesh(stand.texture, Geometry.MeshType.AlphaTest);
		
	//	final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		
		final float topLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
		final float northLight = world.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
		final float southLight = world.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
		final float eastLight = world.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
		final float westLight = world.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
		
	//	Vector4f colour = new Vector4f(lightness, lightness, lightness, 1.0f);
		
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		Vector4f litColour = new Vector4f(topLight, topLight, topLight, 1.0f);
		
		// Base blocks
		BlockUtil.addBlock(baseMesh, x, y, z, 2, 0, 1, 6, 2, 6, colour, base, topLight, northLight, southLight, eastLight, westLight);
		
		BlockUtil.addBlock(baseMesh, x, y, z, 2, 0, 9, 6, 2, 6, colour, base, topLight, northLight, southLight, eastLight, westLight);
		
		BlockUtil.addBlock(baseMesh, x, y, z, 9, 0, 5, 6, 2, 6, colour, base, topLight, northLight, southLight, eastLight, westLight);
		
		// Center post
		BlockUtil.addBlock(standMesh, x, y, z, 7, 0, 7, 2, 14, 2, colour, post, topLight, northLight, southLight, eastLight, westLight);
		
		// Stands
		
		final int data = rawChunk.getBlockData(x, y, z);
		
		SubTexture post1 = ((data & 0x1) > 0 ? standWithBottle : standWithoutBottle);
		SubTexture post2 = ((data & 0x2) > 0 ? standWithBottle : standWithoutBottle);;
		SubTexture post3 = ((data & 0x4) > 0 ? standWithBottle : standWithoutBottle);;
		
		final float one = 1.0f / 16.0f;
		final float two = 2.0f / 16.0f;
		final float seven = 7.0f / 16.0f;
		final float eight = 8.0f / 16.0f;
		final float nine = 9.0f / 16.0f;
		final float fifthteen = 15.0f / 16.0f;
		
		// Along south compass pointer
		MeshUtil.addDoubleSidedQuad(bottleMesh,
									new Vector3f(x+1,		y+1,	z+eight),
									new Vector3f(x+nine,	y+1,	z+eight),
									new Vector3f(x+nine,	y,		z+eight),
									new Vector3f(x+1,		y, 		z+eight),
									litColour, post1);
		
		// Along NE pointer
		MeshUtil.addDoubleSidedQuad(bottleMesh,
									new Vector3f(x+two,		y+1,	z+one),
									new Vector3f(x+eight,	y+1,	z+seven),
									new Vector3f(x+eight,	y, 		z+seven),
									new Vector3f(x+two,		y,		z+one),
									litColour, post2);
		
		// Along NW pointer
		MeshUtil.addDoubleSidedQuad(bottleMesh,
									new Vector3f(x+two,		y+1,	z+fifthteen),
									new Vector3f(x+eight,	y+1,	z+nine),
									new Vector3f(x+eight,	y, 		z+nine),
									new Vector3f(x+two,		y,		z+fifthteen),
									litColour, post3);
	}
}
