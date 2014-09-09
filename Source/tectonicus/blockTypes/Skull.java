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
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Skull implements BlockType 
{
	private final String name;
	
	private final SubTexture topTexture;
	private final SubTexture bottomTexture;
	private final SubTexture faceTexture;
	private final SubTexture rearTexture;
	private final SubTexture rightSideTexture;
	private final SubTexture leftSideTexture;

	private Colour4f colour;

	public Skull(String name, SubTexture texture) 
	{
		this.name = name;
		
		final float widthTexel = 1.0f / texture.texture.getWidth();
		final float heightTexel = 1.0f / texture.texture.getHeight();
		
		topTexture = new SubTexture(texture.texture, texture.u0+widthTexel*8, texture.v0, texture.u0+widthTexel*16, texture.v0+heightTexel*8);
		bottomTexture = new SubTexture(texture.texture, texture.u0+widthTexel*16, texture.v0, texture.u0+widthTexel*24, texture.v0+heightTexel*8);
		faceTexture = new SubTexture(texture.texture, texture.u0+widthTexel*8, texture.v0+heightTexel*8, texture.u0+widthTexel*16, texture.v0+heightTexel*16);
		rearTexture = new SubTexture(texture.texture, texture.u0+widthTexel*24, texture.v0+heightTexel*8, texture.u0+widthTexel*32, texture.v0+heightTexel*16);
		rightSideTexture = new SubTexture(texture.texture, texture.u0+widthTexel*16, texture.v0+heightTexel*8, texture.u0+widthTexel*24, texture.v0+heightTexel*16);
		leftSideTexture = new SubTexture(texture.texture, texture.u0, texture.v0+heightTexel*8, texture.u0+widthTexel*8, texture.v0+heightTexel*16);
		
		System.out.println("width" + topTexture.texture.getWidth());
		System.out.println("height" + topTexture.texture.getHeight());
		
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
		SubMesh subMesh = new SubMesh();
		
		final int data = rawChunk.getBlockData(x, y, z);
		
		final float offSet = 1.0f / 16.0f;
		float xOffset = x;
		float yOffset = y;
		float zOffset = z;
		
		Rotation rotation = Rotation.None;
		float angle = 0;
		float lightness = 1;
		
		if (data > 1) {  // add lighting for each face based on rotation
			/*float xOffset = x;
			float yOffset = y;
			float zOffset = z;
			
			Rotation rotation = Rotation.None;
			float angle = 0;*/
			
			if (data == 2)
			{
				// Facing north
				rotation = Rotation.Clockwise;
				angle = 180;
				zOffset += offSet*4;
			}
			if (data == 3)
			{
				// Facing south
				zOffset -= offSet*4;
			}
			else if (data == 4)
			{
				// Facing west
				rotation = Rotation.Clockwise;
				angle = -90;
				xOffset += offSet*4;
			}
			else if (data == 5)
			{
				// Facing east
				rotation = Rotation.Clockwise;
				angle = 90;
				xOffset -= offSet*4;
			}
		}
		
		//Top
		//float lightness = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);

		subMesh.addQuad(new Vector3f(offSet*4,	offSet*8,	offSet*4),
						new Vector3f(offSet*12,	offSet*8,	offSet*4),
						new Vector3f(offSet*12,	offSet*8,	offSet*12),
						new Vector3f(offSet*4,	offSet*8,	offSet*12),
						new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
						topTexture);

		
		//Bottom
		//lightness = world.getLight(rawChunk.getChunkCoord(), x, y-1, z, LightFace.Top);
		
		subMesh.addQuad(new Vector3f(offSet*4,	0,	offSet*12),
						new Vector3f(offSet*12,	0,	offSet*12),
						new Vector3f(offSet*12,	0,	offSet*4),
						new Vector3f(offSet*4,	0,	offSet*4),
						new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
						bottomTexture);

	
		//North
		//lightness = world.getLight(rawChunk.getChunkCoord(), x, y, z-1, LightFace.EastWest);
		
		subMesh.addQuad(new Vector3f(offSet*12,	offSet*8,	offSet*4),
						new Vector3f(offSet*4,	offSet*8,	offSet*4),
						new Vector3f(offSet*4,	0,			offSet*4),
						new Vector3f(offSet*12,	0,			offSet*4),
						new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
						rearTexture);

		
		//South
		//lightness = world.getLight(rawChunk.getChunkCoord(), x, y, z+1, LightFace.EastWest);
		
		subMesh.addQuad(new Vector3f(offSet*4,	offSet*8,	offSet*12),
						new Vector3f(offSet*12,	offSet*8,	offSet*12),
						new Vector3f(offSet*12,	0,			offSet*12),
						new Vector3f(offSet*4,	0,			offSet*12),
						new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
						faceTexture);
		
		//East	
		//lightness = world.getLight(rawChunk.getChunkCoord(), x+1, y, z, LightFace.NorthSouth);
			
		subMesh.addQuad(new Vector3f(offSet*12,	offSet*8,	offSet*12),
						new Vector3f(offSet*12,	offSet*8,	offSet*4),
						new Vector3f(offSet*12,	0,			offSet*4),
						new Vector3f(offSet*12,	0,			offSet*12),
						new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
						rightSideTexture);
			
		//West
		//lightness = world.getLight(rawChunk.getChunkCoord(), x-1, y, z, LightFace.NorthSouth);
		
		subMesh.addQuad(new Vector3f(offSet*4,	offSet*8,	offSet*4),
						new Vector3f(offSet*4,	offSet*8,	offSet*12),
						new Vector3f(offSet*4,		0,		offSet*12),
						new Vector3f(offSet*4,		0,		offSet*4),
						new Vector4f(colour.r * lightness, colour.g * lightness, colour.b * lightness, colour.a),
						leftSideTexture);
		
		
		
		if(data > 1)
			subMesh.pushTo(geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid), xOffset, yOffset+offSet*4, zOffset, rotation, angle);
		else
			subMesh.pushTo(geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid), x, y, z, null, 0);
			
	}
}
