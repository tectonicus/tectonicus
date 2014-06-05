/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
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
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Dispenser implements BlockType
{
	private final String name;
	
	private final SubTexture topTexture;
	private final SubTexture topBottomTexture;
	private final SubTexture sideTexture;
	private final SubTexture frontTexture;
	
	public Dispenser(String name, SubTexture topTexture, SubTexture topBottomTexture, SubTexture sideTexture, SubTexture frontTexture)
	{
		this.name = name;
		
		this.topTexture = topTexture;
		this.topBottomTexture = topBottomTexture;
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
		Mesh topBottomMesh = geometry.getMesh(topBottomTexture.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);
		Mesh frontMesh = geometry.getMesh(frontTexture.texture, Geometry.MeshType.Solid);

		Colour4f colour = new Colour4f(1, 1, 1, 1);

		final int data = chunk.getBlockData(x, y, z);
		
		// 0x2: Facing north
		// 0x3: Facing south
		// 0x4: Facing west
		// 0x5: Facing east
		
		/*SubTexture northTex = data == 0x2 ? frontTexture : sideTexture;
		SubTexture southTex = data == 0x3 ? frontTexture : sideTexture;
		SubTexture eastTex = data == 0x5 ? frontTexture : sideTexture;
		SubTexture westTex = data == 0x4 ? frontTexture : sideTexture;*/
		
		Mesh northMesh;
		Mesh southMesh;
		Mesh eastMesh;
		Mesh westMesh;
		
		if (data == 0x0)
		{
			northMesh = southMesh = eastMesh = westMesh = topMesh;
			BlockUtil.addTop(world, chunk, topMesh, x, y, z, colour, topTexture, registry);
			BlockUtil.addBottom(world, chunk, topBottomMesh, x, y, z, colour, topBottomTexture, registry);
		}
		else if(data == 0x1)
		{
			northMesh = southMesh = eastMesh = westMesh = topMesh;
			BlockUtil.addTop(world, chunk, topBottomMesh, x, y, z, colour, topBottomTexture, registry);
			BlockUtil.addBottom(world, chunk, topMesh, x, y, z, colour, topTexture, registry);
		}
		else
		{
			northMesh = data == 0x2 ? frontMesh : sideMesh;
			southMesh = data == 0x3 ? frontMesh : sideMesh;
			eastMesh = data == 0x5 ? frontMesh : sideMesh;
			westMesh = data == 0x4 ? frontMesh : sideMesh;
			BlockUtil.addTop(world, chunk, topMesh, x, y, z, colour, topTexture, registry);
			BlockUtil.addBottom(world, chunk, topMesh, x, y, z, colour, topTexture, registry);
		}
		BlockUtil.addNorth(world, chunk, westMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addSouth(world, chunk, eastMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addEast(world, chunk, northMesh, x, y, z, colour, sideTexture, registry);
		BlockUtil.addWest(world, chunk, southMesh, x, y, z, colour, sideTexture, registry);
	}
}
