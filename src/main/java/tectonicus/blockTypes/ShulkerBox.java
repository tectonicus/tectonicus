/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
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
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class ShulkerBox implements BlockType
{
	private final String name;
	private final String stringId;
	
	public ShulkerBox(String name, String stringId)
	{
		this.name = name;
		this.stringId = stringId;
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
		String shulkerType = stringId.replace("minecraft:", "").replace("_box", "");
		
		final float texel = 1.0f / 64.0f;
		
		SubTexture texture = world.getTexturePack().findTexture(null, shulkerType);
		SubTexture topTexture = new SubTexture(texture.texture, texture.u0+texel*16.1f, texture.v0, texture.u0+texel*31.9f, texture.v0+texel*16);
		SubTexture bottomTexture = new SubTexture(texture.texture, texture.u0+texel*32.1f, texture.v0+texel*28.1f, texture.u0+texel*47.9f, texture.v0+texel*43.9f);
		SubTexture sideTexture = world.getTexturePack().findTexture(null, shulkerType + "_side");
		
		Mesh topBottomMesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);

		Colour4f colour = new Colour4f(1, 1, 1, 1);

		final int data = chunk.getBlockData(x, y, z);
		int direction = data & 0x7;
		final BlockProperties properties = chunk.getBlockState(x, y, z);
		if (properties != null) {
			switch (properties.get("facing")) {
				case "down":
					direction = 0;
					break;
				case "north":
					direction = 2;
					break;
				case "south":
					direction = 3;
					break;
				case "west":
					direction = 4;
					break;
				case "east":
					direction = 5;
					break;
				default:
					direction = 1;

			}
		}

		if (direction == 0) { //Facing down
			BlockUtil.addTop(world, chunk, topBottomMesh, x, y, z, colour, bottomTexture, registry); //The bottom texture isn't quite correct but close enough
			BlockUtil.addBottom(world, chunk, topBottomMesh, x, y, z, colour, topTexture, registry);
			BlockUtil.addNorth(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Flip);
			BlockUtil.addSouth(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Flip);
			BlockUtil.addEast(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Flip);
			BlockUtil.addWest(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Flip);
		} else if (direction == 1) { //Facing up
			BlockUtil.addTop(world, chunk, topBottomMesh, x, y, z, colour, topTexture, registry);
			BlockUtil.addBottom(world, chunk, topBottomMesh, x, y, z, colour, bottomTexture, registry);
			BlockUtil.addEast(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addWest(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addNorth(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry);
			BlockUtil.addSouth(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry);
		} else if (direction == 2) { //Facing north
			BlockUtil.addTop(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.None);
			BlockUtil.addBottom(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.None);
			BlockUtil.addEast(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Clockwise);
			BlockUtil.addWest(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.AntiClockwise);
			BlockUtil.addNorth(world, chunk, topBottomMesh, x, y, z, colour, topTexture, registry, Rotation.None);
			BlockUtil.addSouth(world, chunk, topBottomMesh, x, y, z, colour, bottomTexture, registry, Rotation.None);
		} else if (direction == 3) { //Facing south
			BlockUtil.addTop(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Flip);
			BlockUtil.addBottom(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.None);
			BlockUtil.addEast(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.AntiClockwise);
			BlockUtil.addWest(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Clockwise);
			BlockUtil.addNorth(world, chunk, topBottomMesh, x, y, z, colour, bottomTexture, registry, Rotation.None);
			BlockUtil.addSouth(world, chunk, topBottomMesh, x, y, z, colour, topTexture, registry, Rotation.None);
		} else if (direction == 4) { //Facing west
			BlockUtil.addTop(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.AntiClockwise);
			BlockUtil.addBottom(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Clockwise);
			BlockUtil.addEast(world, chunk, topBottomMesh, x, y, z, colour, bottomTexture, registry, Rotation.None);
			BlockUtil.addWest(world, chunk, topBottomMesh, x, y, z, colour, topTexture, registry, Rotation.None);
			BlockUtil.addNorth(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Clockwise);
			BlockUtil.addSouth(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.AntiClockwise);
		} else if (direction == 5) { //Facing east
			BlockUtil.addTop(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Clockwise);
			BlockUtil.addBottom(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.AntiClockwise);
			BlockUtil.addEast(world, chunk, topBottomMesh, x, y, z, colour, topTexture, registry, Rotation.None);
			BlockUtil.addWest(world, chunk, topBottomMesh, x, y, z, colour, bottomTexture, registry, Rotation.None);
			BlockUtil.addNorth(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.AntiClockwise);
			BlockUtil.addSouth(world, chunk, sideMesh, x, y, z, colour, sideTexture, registry, Rotation.Clockwise);
		}
	}
}
