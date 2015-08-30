/*
 * Copyright (c) 2012-2015, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.util.Random;

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
	private final int texHeight;
	private final int texWidth;
	private final int numTiles;
	private int frame;
	
	public Fire(String name, SubTexture texture, int frame)
	{
		if (texture == null)
			throw new RuntimeException("fire texture is null!");

		this.name = name;
		this.texHeight = texture.texture.getHeight();
		this.texWidth = texture.texture.getWidth();
		this.numTiles = texHeight/texWidth;
		this.texture = texture;
		this.frame = frame;
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
		int newFrame;
		if(numTiles > 1 && frame == 0)
		{
			Random rand = new Random();
			newFrame = rand.nextInt(numTiles)+1;
		}
		else
		{
			newFrame = frame;
		}

		SubTexture randomTexture = new SubTexture(texture.texture, texture.u0, texture.v0+(float)((newFrame-1)*texWidth)/texHeight, texture.u1, texture.v0+(float)(newFrame*texWidth)/texHeight);
		
		Mesh mesh = geometry.getMesh(randomTexture.texture, Geometry.MeshType.AlphaTest);
		
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		
		addFireGeometry(x, y, z, mesh, colour, randomTexture);
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
