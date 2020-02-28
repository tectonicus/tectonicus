/*
 * Copyright (c) 2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.SubMesh.Rotation;
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
	}
	
	@Override
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext context, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		final int data = rawChunk.getBlockData(x, y, z);
		final int type = rawChunk.getBlockId(x, y, z);
		
		Mesh topMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);
		
		//0x4 - 0x7 sideways log east/west facing
		//0x8 - 0x11 sideways log north/south facing
		
		// Quartz Pillar
		//3 east/west facing
		//4 north/south facing

		if (type != 155 && data >= 0x4 && data <= 0x7 || type == 155 && data == 3)  // side, top, and bottom textures for east/west facing sideways blocks have to be rotated
		{
			BlockUtil.addTop(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Clockwise);
			BlockUtil.addBottom(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.AntiClockwise);
			BlockUtil.addNorth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.AntiClockwise);
			BlockUtil.addSouth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Clockwise);
			BlockUtil.addEast(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry, Rotation.None);
			BlockUtil.addWest(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry, Rotation.None);
		}
		else if (data >= 0x8 && data <= 0x11 || type == 155 && data == 4)  // side textures for north/south facing sideways blocks have to be rotated
		{
			BlockUtil.addTop(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addBottom(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addEast(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Clockwise);
			BlockUtil.addWest(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.AntiClockwise);			
			BlockUtil.addNorth(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry, Rotation.None);
			BlockUtil.addSouth(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry, Rotation.None);
		}
		else
		{
			BlockUtil.addTop(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
			BlockUtil.addBottom(context, rawChunk, topMesh, x, y, z, colour, topTexture, registry);
			BlockUtil.addEast(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addWest(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addNorth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addSouth(context, rawChunk, sideMesh, x, y, z, colour, sideTexture, registry);
		}
	}
	
}
