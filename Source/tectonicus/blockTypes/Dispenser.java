/*
 * Copyright (c) 2012-2016, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.SubMesh.Rotation;
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
		
		final float topTile = (1.0f / topTexture.texture.getHeight()) * topTexture.texture.getWidth();
		final float frontTile = (1.0f / frontTexture.texture.getHeight()) * frontTexture.texture.getWidth();
		final float topBottomTile = (1.0f / topBottomTexture.texture.getHeight()) * topBottomTexture.texture.getWidth();
		final float sideTile = (1.0f / sideTexture.texture.getHeight()) * sideTexture.texture.getWidth();
		
		this.topTexture = new SubTexture(topTexture.texture, topTexture.u0, topTexture.v0, topTexture.u1, topTexture.v0+topTile);
		this.topBottomTexture = new SubTexture(topBottomTexture.texture, topBottomTexture.u0, topBottomTexture.v0, topBottomTexture.u1, topBottomTexture.v0+topBottomTile);
		this.sideTexture = new SubTexture(sideTexture.texture, sideTexture.u0, sideTexture.v0, sideTexture.u1, sideTexture.v0+sideTile);
		this.frontTexture = new SubTexture(frontTexture.texture, frontTexture.u0, frontTexture.v0, frontTexture.u1, frontTexture.v0+frontTile);
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

		final int id = world.getBlockId(chunk.getChunkCoord(), x, y, z);
		final boolean commandBlock = (id == BlockIds.COMMAND_BLOCK || id == BlockIds.CHAIN_COMMAND_BLOCK || id == BlockIds.REPEATING_COMMAND_BLOCK) ? true : false;
		final int data = chunk.getBlockData(x, y, z);
		
		// 0x2: Facing north
		// 0x3: Facing south
		// 0x4: Facing west
		// 0x5: Facing east
		
		SubTexture northTex;
		SubTexture southTex;
		SubTexture eastTex;
		SubTexture westTex;
		
		Mesh northMesh;
		Mesh southMesh;
		Mesh eastMesh;
		Mesh westMesh;
		
		Rotation northRot = Rotation.None;
		Rotation southRot = Rotation.None;
		Rotation eastRot = Rotation.None;
		Rotation westRot = Rotation.None;
		
		final boolean conditional = (data & 0x8) > 0;
		if (commandBlock && conditional)
			sideMesh = topMesh;

		if (data == 0 || data == 8)
		{
			if (commandBlock)
			{
				BlockUtil.addTop(world, chunk, topBottomMesh, x, y, z, colour, topBottomTexture, registry);
				northTex = southTex = eastTex = westTex = sideTexture;
				northMesh = southMesh = eastMesh = westMesh = sideMesh;
				BlockUtil.addBottom(world, chunk, frontMesh, x, y, z, colour, frontTexture, registry);
				northRot = Rotation.Flip;
				southRot = Rotation.Flip;
				eastRot = Rotation.Flip;
				westRot = Rotation.Flip;
			}
			else
			{
				BlockUtil.addTop(world, chunk, topMesh, x, y, z, colour, topTexture, registry);
				northTex = southTex = eastTex = westTex = topTexture;
				northMesh = southMesh = eastMesh = westMesh = topMesh;
				BlockUtil.addBottom(world, chunk, topBottomMesh, x, y, z, colour, topBottomTexture, registry);
			}
		}
		else if(data == 1 || data == 9)
		{			
			if (commandBlock)
			{
				BlockUtil.addTop(world, chunk, frontMesh, x, y, z, colour, frontTexture, registry);
				northTex = southTex = eastTex = westTex = sideTexture;
				northMesh = southMesh = eastMesh = westMesh = sideMesh;
				BlockUtil.addBottom(world, chunk, topBottomMesh, x, y, z, colour, topBottomTexture, registry);
			}
			else
			{
				BlockUtil.addTop(world, chunk, topBottomMesh, x, y, z, colour, topBottomTexture, registry);
				northTex = southTex = eastTex = westTex = topTexture;
				northMesh = southMesh = eastMesh = westMesh = topMesh;
				BlockUtil.addBottom(world, chunk, topMesh, x, y, z, colour, topTexture, registry);
			}
			
		}
		else if (data == 2 || data == 10)
		{
			northTex = frontTexture;
			northMesh = frontMesh;
			southTex = commandBlock ? topBottomTexture : sideTexture;
			southMesh = commandBlock ? topBottomMesh : sideMesh;
			eastTex = westTex = sideTexture;
			eastMesh = westMesh = sideMesh;
			if (commandBlock)
			{
				topMesh = sideMesh;
				eastRot = Rotation.Clockwise;
				westRot = Rotation.AntiClockwise;
			}
			BlockUtil.addTop(world, chunk, topMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addBottom(world, chunk, topMesh, x, y, z, colour, sideTexture, registry);
		}
		else if (data == 3 || data == 11)
		{
			northTex = commandBlock ? topBottomTexture : sideTexture;
			northMesh = commandBlock ? topBottomMesh : sideMesh;
			southTex = frontTexture;
			southMesh = frontMesh;
			eastTex = westTex = sideTexture;
			eastMesh = westMesh = sideMesh;
			if (commandBlock)
			{
				topMesh = sideMesh;
				eastRot = Rotation.AntiClockwise;
				westRot = Rotation.Clockwise;
			}
			BlockUtil.addTop(world, chunk, topMesh, x, y, z, colour, sideTexture, registry, Rotation.Flip);
			BlockUtil.addBottom(world, chunk, topMesh, x, y, z, colour, sideTexture, registry, Rotation.Flip);
		}
		else if (data == 4 || data == 12)
		{
			northTex = southTex = sideTexture;
			northMesh = southMesh = sideMesh;
			eastTex = commandBlock ? topBottomTexture : sideTexture;
			eastMesh = commandBlock ? topBottomMesh : sideMesh;
			westTex = frontTexture;
			westMesh = frontMesh;
			if (commandBlock)
			{
				topMesh = sideMesh;
				northRot = Rotation.Clockwise;
				southRot = Rotation.AntiClockwise;
			}
			BlockUtil.addTop(world, chunk, topMesh, x, y, z, colour, sideTexture, registry, Rotation.AntiClockwise);
			BlockUtil.addBottom(world, chunk, topMesh, x, y, z, colour, sideTexture, registry, Rotation.Clockwise);
		}
		else //if (data == 5 || data == 13)
		{
			northTex = southTex = sideTexture;
			northMesh = southMesh = sideMesh;
			eastTex = frontTexture;
			eastMesh = frontMesh;
			westTex = commandBlock ? topBottomTexture : sideTexture;
			westMesh = commandBlock ? topBottomMesh : sideMesh;
			if (commandBlock)
			{
				topMesh = sideMesh;
				northRot = Rotation.AntiClockwise;
				southRot = Rotation.Clockwise;
			}
			BlockUtil.addTop(world, chunk, topMesh, x, y, z, colour, sideTexture, registry, Rotation.Clockwise);
			BlockUtil.addBottom(world, chunk, topMesh, x, y, z, colour, sideTexture, registry, Rotation.AntiClockwise);
		}
		BlockUtil.addWest(world, chunk, westMesh, x, y, z, colour, westTex, registry, westRot);
		BlockUtil.addEast(world, chunk, eastMesh, x, y, z, colour, eastTex, registry, eastRot);
		BlockUtil.addNorth(world, chunk, northMesh, x, y, z, colour, northTex, registry, northRot);
		BlockUtil.addSouth(world, chunk, southMesh, x, y, z, colour, southTex, registry, southRot);
	}
}
