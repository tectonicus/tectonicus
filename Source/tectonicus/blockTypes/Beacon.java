/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Beacon implements BlockType
{	
	private final String name;

	private final SubTexture glass, beacon, obsidian;
	
	public Beacon(String name, SubTexture glass, SubTexture beacon, SubTexture obsidian)
	{
		this.name = name;
		this.glass = glass;
		this.obsidian = obsidian;
		
		final float texel;
		if (glass.texturePackVersion == "1.4")
			texel = 1.0f / 16.0f / 16.0f;
		else
			texel = 1.0f / 16.0f;
		
		this.beacon = new SubTexture(beacon.texture, beacon.u0+texel, beacon.v0+texel, beacon.u1-texel, beacon.v1-texel);
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
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		
		final float offSet = 1.0f / 16.0f;
		
		SubMesh glassMesh = new SubMesh();
		SubMesh beaconMesh = new SubMesh();
		SubMesh obsidianMesh = new SubMesh();
		SubMesh.addBlock(glassMesh, 0, 0, 0, offSet*16, offSet*16, offSet*16, colour, glass, glass, glass);
		SubMesh.addBlock(beaconMesh, offSet*3, offSet*3, offSet*3, offSet*10, offSet*10, offSet*10, colour, beacon, beacon, beacon);
		SubMesh.addBlock(obsidianMesh, offSet*2, offSet*0.5f, offSet*2, offSet*12, offSet*3, offSet*12, colour, obsidian, obsidian, obsidian);
		glassMesh.pushTo(geometry.getMesh(glass.texture, Geometry.MeshType.AlphaTest), x, y, z, Rotation.None, 0);
		beaconMesh.pushTo(geometry.getMesh(beacon.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
		obsidianMesh.pushTo(geometry.getMesh(obsidian.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
	}
	
}
