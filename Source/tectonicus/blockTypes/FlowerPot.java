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
import tectonicus.raw.RawChunk;
import tectonicus.raw.TileEntity;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class FlowerPot implements BlockType
{
	private final String name;
	
	private final SubTexture texture, dirt, plant, side, top;
	
	public FlowerPot(String name, SubTexture texture, SubTexture dirt, SubTexture plant)
	{
		this.name = name;
		
		this.texture = texture;
		this.dirt = dirt;
		this.plant = plant;
		
		final float texel;
		if (texture.texturePackVersion == "1.4")
			texel = 1.0f / 16.0f / 16.0f;
		else
			texel = 1.0f / 16.0f;
		
		top = new SubTexture(texture.texture, texture.u0+(texel*5), texture.v0+(texel*5), texture.u0+(texel*11f), texture.v0+(texel*11f));
		side = new SubTexture(texture.texture, texture.u0+(texel*5), texture.v0+(texel*10), texture.u1-(texel*5), texture.v1);
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
		Mesh sideMesh = geometry.getMesh(texture.texture, Geometry.MeshType.AlphaTest);
		Mesh dirtMesh = geometry.getMesh(dirt.texture, Geometry.MeshType.Solid);
		Mesh plantMesh = null;
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		
		Vector4f colour = new Vector4f(lightness, lightness, lightness, 1.0f);
		
		final int data = rawChunk.getBlockData(x, y, z);
		
		final float width = 5.0f / 16.0f;
		final float height = 6.0f / 16.0f;
		
		// Top
		MeshUtil.addQuad(sideMesh, new Vector3f(x+width,	y+height, z+width),
								  new Vector3f(x+(1-width),	y+height, z+width),
								  new Vector3f(x+(1-width),	y+height, z+(1-width)),
								  new Vector3f(x+width, 	y+height, z+(1-width)),
								  colour, top);
		
		// Dirt inside pot
		float dirtLevel = 4.0f / 16.0f;
		MeshUtil.addQuad(dirtMesh, new Vector3f(x+0.1f+width,	y+dirtLevel, z+0.1f+width),
									new Vector3f(x+0.9f-width,	y+dirtLevel, z+0.1f+width),
									new Vector3f(x+0.9f-width,	y+dirtLevel, z+0.9f-width),
									new Vector3f(x+0.1f+width, 	y+dirtLevel, z+0.9f-width),
									colour, dirt);
		
		// Bottom
		MeshUtil.addQuad(sideMesh, new Vector3f(x+width,	y, z+width),
									new Vector3f(x+1-width, y, z+width),
									new Vector3f(x+1-width, y, z+1-width),
									new Vector3f(x+width, 	y, z+1-width),
									colour, side);
				
		// West
		MeshUtil.addQuad(sideMesh, new Vector3f(x+width, y+height, z+width),
								   new Vector3f(x+width, y+height, z+1-width),
								   new Vector3f(x+width, y, 	z+1-width),
								   new Vector3f(x+width, y, 	z+width),
								   colour, side);
		
		// East
		MeshUtil.addQuad(sideMesh, new Vector3f(x+1-width, y+height, 	z+1-width),
								   new Vector3f(x+1-width, y+height, 	z+width),
								   new Vector3f(x+1-width, y, 	z+width),
								   new Vector3f(x+1-width, y, 	z+1-width),
								   colour, side);
		
		// North
		MeshUtil.addQuad(sideMesh, new Vector3f(x+1-width, y+height, 	z+width),
								   new Vector3f(x+width,   y+height, 	z+width),
								   new Vector3f(x+width,   y, 	z+width),
								   new Vector3f(x+1-width, y, 	z+width),
								   colour, side);
		
		// South
		MeshUtil.addQuad(sideMesh, new Vector3f(x+width,   y+height, 	z+1-width),
								   new Vector3f(x+1-width, y+height, 	z+1-width),
								   new Vector3f(x+1-width, y, 	z+1-width),
								   new Vector3f(x+width,   y, 	z+1-width),
								   colour, side);
		
		final float offset = 1.0f / 16.0f + width;
		
		// Inv west
		MeshUtil.addQuad(sideMesh, new Vector3f(x+1-offset, y+height, z+width),
				   				   new Vector3f(x+1-offset, y+height, z+1-width),
				   				   new Vector3f(x+1-offset, y,   z+1-width),
				   				   new Vector3f(x+1-offset, y,   z+width),
				   				   colour, side);
		
		// Inv east
		MeshUtil.addQuad(sideMesh, new Vector3f(x+offset, y+height, z+1-width),
								   new Vector3f(x+offset, y+height, z+width),
								   new Vector3f(x+offset, y,   z+width),
								   new Vector3f(x+offset, y,   z+1-width),
								   colour, side);
		
		// Inv north
		MeshUtil.addQuad(sideMesh, new Vector3f(x+width,	y+height, z+offset),
								   new Vector3f(x+1-width,	y+height, z+offset),
								   new Vector3f(x+1-width,	y,   z+offset),
								   new Vector3f(x+width,	y,   z+offset),
								   colour, side);
		
		// Inv south
		MeshUtil.addQuad(sideMesh, new Vector3f(x+1-width,	y+height, z+1-offset),
								   new Vector3f(x+width,	y+height, z+1-offset),
								   new Vector3f(x+width,	y,   z+1-offset),
								   new Vector3f(x+1-width,	y,   z+1-offset),
								   colour, side);
		
		
		// Flowerpots use a Tile Entity to store which plant they contain
		for (TileEntity te : rawChunk.getFlowerPots())
		{
			if (te.localX == x && te.localY == y && te.localZ == z)
			{
				BlockType type = registry.find(te.item, te.data);
				if(type instanceof Plant)
				{
					Plant p = (Plant)type;
					plantMesh = geometry.getMesh(p.getTexture().texture, Geometry.MeshType.AlphaTest);
					Plant.addPlantGeometry(x, y, z, dirtLevel, plantMesh, colour, plant);
					//System.out.println("Name: " + p.getName() + " Item: " + te.item + " Data: " + te.data);
				}
			}
		}
		
		if(plantMesh == null && data > 0 && data != 9 && data != 11)
		{
			plantMesh = geometry.getMesh(plant.texture, Geometry.MeshType.AlphaTest);
			Plant.addPlantGeometry(x, y, z, dirtLevel, plantMesh, colour, plant);
		}
		else if(plantMesh == null && data == 9)
		{
			plantMesh = geometry.getMesh(plant.texture, Geometry.MeshType.AlphaTest);
			BlockUtil.addBlock(plantMesh, x, y, z, 6, 4, 6, 4, 16, 4, colour, plant, lightness, lightness, lightness);
		}
		else if(plantMesh == null && data == 11)
		{
			plantMesh = geometry.getMesh(plant.texture, Geometry.MeshType.AlphaTest);
			Colour4f baseColour = world.getGrassColour(rawChunk.getChunkCoord(), x, y, z);
			final float lightVal = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
			colour = new Vector4f(baseColour.r * lightVal, baseColour.g * lightVal, baseColour.b * lightVal, baseColour.a);
			
			Plant.addPlantGeometry(x, y, z, dirtLevel, plantMesh, colour, plant);
		}
	}
}
