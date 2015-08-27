/*
 * Copyright (c) 2012-2015, John Campbell and other contributors.  All rights reserved.
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
import tectonicus.raw.TileEntity;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Beacon implements BlockType
{	
	private final String name;

	private final SubTexture glass, beacon, obsidian, beam;
	
	public Beacon(String name, SubTexture glass, SubTexture beacon, SubTexture obsidian, SubTexture beam)
	{
		this.name = name;
		this.glass = glass;
		this.obsidian = obsidian;
		this.beam = beam;
		
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
		SubMesh beamMesh = new SubMesh();
		SubMesh.addBlock(glassMesh, 0, 0, 0, offSet*16, offSet*16, offSet*16, colour, glass, glass, glass);
		SubMesh.addBlock(beaconMesh, offSet*3, offSet*3, offSet*3, offSet*10, offSet*10, offSet*10, colour, beacon, beacon, beacon);
		SubMesh.addBlock(obsidianMesh, offSet*2, offSet*0.5f, offSet*2, offSet*12, offSet*3, offSet*12, colour, obsidian, obsidian, obsidian);
		
		for (TileEntity te : rawChunk.getBeacons())  
		{
			if (te.localX == x && te.localY == y && te.localZ == z && te.blockData > 0)
			{
				Colour4f color = new Colour4f(1, 1, 1, 1);
				for (int i=1; i<256-te.localY; i++)  
				{
					final int blockID = world.getBlockId(rawChunk.getChunkCoord(), x, te.localY+i, z);
					
					if (blockID == 95)
					{
						final int colorID = rawChunk.getBlockData(x, te.localY+i, z);
						
						Colour4f newColor = new Colour4f(1, 1, 1, 1);
						
						if (colorID == 0)
							newColor = new Colour4f(1, 1, 1, 1); // White
						else if (colorID == 1)
							newColor = new Colour4f(216f/255f, 127f/255f, 51f/255f, 1); // Orange
						else if (colorID == 2)
							newColor = new Colour4f(178f/255f, 76f/255f, 216f/255f, 1); // Magenta
						else if (colorID == 3)
							newColor = new Colour4f(102f/255f, 153f/255f, 216f/255f, 1); // Light Blue
						else if (colorID == 4)
							newColor = new Colour4f(229f/255f, 229f/255f, 51f/255f, 1); // Yellow
						else if (colorID == 5)
							newColor = new Colour4f(127f/255f, 204f/255f, 25f/255f, 1); // Lime
						else if (colorID == 6)
							newColor = new Colour4f(242f/255f, 127f/255f, 165f/255f, 1); // Pink
						else if (colorID == 7)
							newColor = new Colour4f(76f/255f, 76f/255f, 76f/255f, 1); // Gray
						else if (colorID == 8)
							newColor = new Colour4f(153f/255f, 153f/255f, 153f/255f, 1); // Light Gray
						else if (colorID == 9)
							newColor = new Colour4f(76f/255f, 127f/255f, 153f/255f, 1); // Cyan
						else if (colorID == 10)
							newColor = new Colour4f(127f/255f, 63f/255f, 178f/255f, 1); // Purple
						else if (colorID == 11)
							newColor = new Colour4f(51f/255f, 76f/255f, 178f/255f, 1); // Blue
						else if (colorID == 12)
							newColor = new Colour4f(102f/255f, 76f/255f, 51f/255f, 1); // Brown
						else if (colorID == 13)
							newColor = new Colour4f(102f/255f, 127f/255f, 51f/255f, 1); // Green
						else if (colorID == 14)
							newColor = new Colour4f(153f/255f, 51f/255f, 51f/255f, 1); // Red	
						else if (colorID == 15)
							newColor = new Colour4f(25f/255f, 25f/255f, 25f/255f, 1);  // Black
						
						color.average(newColor);
					}
					
					SubMesh.addBlockSimple(beamMesh, offSet*5, offSet*(16*i), offSet*5, offSet*5, 1, offSet*5, 
													new Vector4f(color.r, color.g, color.b, 1), beam, null, null);  //Beacon beam
					SubMesh.addBlockSimple(beamMesh, offSet*3, offSet*(16*i), offSet*3, offSet*10, 1, offSet*10, 
													new Vector4f(color.r, color.g, color.b, 0.4f), beam, null, null);
				}
			}
		}
		
		glassMesh.pushTo(geometry.getMesh(glass.texture, Geometry.MeshType.AlphaTest), x, y, z, Rotation.None, 0);
		beaconMesh.pushTo(geometry.getMesh(beacon.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
		obsidianMesh.pushTo(geometry.getMesh(obsidian.texture, Geometry.MeshType.Solid), x, y, z, Rotation.None, 0);
		beamMesh.pushTo(geometry.getMesh(beam.texture, Geometry.MeshType.Transparent), x, y, z, Rotation.Clockwise, 35);
	}
	
}
