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

import org.lwjgl.util.vector.Vector2f;
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
import tectonicus.util.Colour4f;

public class Log implements BlockType
{
	private final String name;
	private final SubTexture sideTexture, topTexture;
	private Colour4f colour;
	
	public Log(String name, SubTexture sideTex, SubTexture topTexture)
	{
		if (topTexture == null)
			throw new RuntimeException("top subtexture is null!");
		
		this.name = name;
		this.sideTexture = sideTex;
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
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
		
		/*Mesh topMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);
		
		BlockUtil.addInteriorTop(world, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addInteriorNorth(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorSouth(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorEast(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addInteriorWest(world, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);*/
	}
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext context, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		Mesh mesh = geometry.getBaseMesh();
		
		final int data = rawChunk.getBlockData(x, y, z);
		
		//0x4 - 0x7 sideways log east/west facing
		//0x8 - 0x11 sideways log north/south facing
		
		//SubTexture eastWestTex = data >= 0x4 && data <= 0x7 ? topTexture : sideTexture;
		//SubTexture northSouthTex = data >= 0x8 && data <= 0x11 ? topTexture : sideTexture;
		SubTexture topBottomTex = data >= 0x4 && data <= 0x11 ? sideTexture : topTexture;
		
		
		if (data >= 0x4 && data <= 0x7)  // side, top, and bottom textures for east/west facing sideways blocks have to be rotated
		{
			BlockType above = context.getBlockType(rawChunk.getChunkCoord(), x, y+1, z);
			if (!above.isSolid())
			{
				final float lightness = context.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
				
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z),
										new Vector3f(x+1,	y+1,	z),
										new Vector3f(x+1,	y+1,	z+1),
										new Vector3f(x,	y+1,		z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										new Vector2f(sideTexture.u0, sideTexture.v1), new Vector2f(sideTexture.u0, sideTexture.v0),new Vector2f(sideTexture.u1, sideTexture.v0), new Vector2f(sideTexture.u1, sideTexture.v1));
			}
			
			BlockType below = context.getBlockType(rawChunk.getChunkCoord(), x, y-1, z);
			if (!below.isSolid())
			{
				final float lightness = context.getLight(rawChunk.getChunkCoord(), x, y-1, z, LightFace.Top);
				
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y,	z+1),
										new Vector3f(x+1,	y,	z+1),
										new Vector3f(x+1,	y,	z),
										new Vector3f(x,		y,	z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										new Vector2f(sideTexture.u1, sideTexture.v1), new Vector2f(sideTexture.u1, sideTexture.v0),new Vector2f(sideTexture.u0, sideTexture.v0), new Vector2f(sideTexture.u0, sideTexture.v1));
			}
			
			BlockType south = context.getBlockType(rawChunk.getChunkCoord(), x, y, z-1);  //TODO:  Some side textures need a bit of adjusting still
			if (!south.isSolid())
			{
				final float lightness = context.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
				
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,	y+1,	z),
										new Vector3f(x,		y+1,	z),
										new Vector3f(x,		y,		z),
										new Vector3f(x+1,	y,		z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										new Vector2f(sideTexture.u0, sideTexture.v0), new Vector2f(sideTexture.u0, sideTexture.v1),new Vector2f(sideTexture.u1, sideTexture.v1), new Vector2f(sideTexture.u1, sideTexture.v0));
			}
			
			BlockType north = context.getBlockType(rawChunk.getChunkCoord(), x, y, z+1);
			if (!north.isSolid())
			{
				final float lightness = context.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
				
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z+1),
										new Vector3f(x+1,	y+1,	z+1),
										new Vector3f(x+1,	y,		z+1),
										new Vector3f(x,		y,		z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										new Vector2f(sideTexture.u1, sideTexture.v1), new Vector2f(sideTexture.u1, sideTexture.v0),new Vector2f(sideTexture.u0, sideTexture.v0), new Vector2f(sideTexture.u0, sideTexture.v1));
			}
			
			BlockUtil.addNorth(context, rawChunk, mesh, x, y, z, colour, topTexture, registry);
			BlockUtil.addSouth(context, rawChunk, mesh, x, y, z, colour, topTexture, registry);
		}
		else if (data >= 0x8 && data <= 0x11)  // side textures for north/south facing sideways blocks have to be rotated
		{
			BlockUtil.addTop(context, rawChunk, mesh, x, y, z, colour, topBottomTex, registry);
			BlockUtil.addBottom(context, rawChunk, mesh, x, y, z, colour, topBottomTex, registry);
			
			BlockType east = context.getBlockType(rawChunk.getChunkCoord(), x+1, y, z);
			if (!east.isSolid())
			{
				final float lightness = context.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
				
				MeshUtil.addQuad(mesh,	new Vector3f(x+1,		y+1,	z+1),
										new Vector3f(x+1,		y+1,	z),
										new Vector3f(x+1,		y,		z),
										new Vector3f(x+1,		y,		z+1),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										new Vector2f(sideTexture.u1, sideTexture.v1), new Vector2f(sideTexture.u1, sideTexture.v0),new Vector2f(sideTexture.u0, sideTexture.v0), new Vector2f(sideTexture.u0, sideTexture.v1));
			}
			
			BlockType west = context.getBlockType(rawChunk.getChunkCoord(), x-1, y, z);
			if (!west.isSolid())
			{
				final float lightness = context.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
				
				MeshUtil.addQuad(mesh,	new Vector3f(x,		y+1,	z),
										new Vector3f(x,		y+1,	z+1),
										new Vector3f(x,		y,		z+1),
										new Vector3f(x,		y,		z),
										new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
										new Vector2f(sideTexture.u1, sideTexture.v1), new Vector2f(sideTexture.u1, sideTexture.v0),new Vector2f(sideTexture.u0, sideTexture.v0), new Vector2f(sideTexture.u0, sideTexture.v1));
			}
			
			BlockUtil.addEast(context, rawChunk, mesh, x, y, z, colour, topTexture, registry);
			BlockUtil.addWest(context, rawChunk, mesh, x, y, z, colour, topTexture, registry);
		}
		else
		{
			BlockUtil.addTop(context, rawChunk, mesh, x, y, z, colour, topBottomTex, registry);
			BlockUtil.addBottom(context, rawChunk, mesh, x, y, z, colour, topBottomTex, registry);
			BlockUtil.addEast(context, rawChunk, mesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addWest(context, rawChunk, mesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addNorth(context, rawChunk, mesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addSouth(context, rawChunk, mesh, x, y, z, colour, sideTexture, registry);
		}
	}
	
}
