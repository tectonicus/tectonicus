/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;
import org.joml.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.Chunk;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class PistonBase implements BlockType
{
	private final String name;
	private SubTexture baseSide, side, top, bottom, pistonFace, pistonEdge;
	
	public PistonBase(String name, SubTexture entireSide, SubTexture top, SubTexture bottom, SubTexture pistonFace)
	{
		this.name = name;
		
		this.pistonFace = pistonFace;
		
		final float divide;
		final float esTile, topTile, bottomTile, pfTile;
		if (top.texturePackVersion == VERSION_4)
		{
			divide = 1.0f / 16.0f / 16.0f * 4.0f;
			esTile = topTile = bottomTile = pfTile = 1.0f / 16.0f / 16.0f;
		}
		else
		{
			divide = 1.0f / 16.0f * 4.0f;
			esTile = (1.0f / entireSide.texture.getHeight()) * entireSide.texture.getWidth();
			topTile = (1.0f / top.texture.getHeight()) * top.texture.getWidth();
			bottomTile = (1.0f / bottom.texture.getHeight()) * bottom.texture.getWidth();
			pfTile = (1.0f / pistonFace.texture.getHeight()) * pistonFace.texture.getWidth();
		}
		
		this.top = new SubTexture(top.texture, top.u0, top.v0, top.u1, top.v0+topTile);
		this.bottom = new SubTexture(bottom.texture, bottom.u0, bottom.v0, bottom.u1, bottom.v0+bottomTile);
		this.pistonFace = new SubTexture(pistonFace.texture, pistonFace.u0, pistonFace.v0, pistonFace.u1, pistonFace.v0+pfTile);
		side = new SubTexture(entireSide.texture, entireSide.u0, entireSide.v0, entireSide.u1, entireSide.v0+esTile);
		baseSide = new SubTexture(side.texture, side.u0, side.v0+divide, side.u1, side.v1);
		pistonEdge = new SubTexture(side.texture, side.u0, side.v0, side.u1, side.v0+divide);
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		final int data = chunk.getBlockData(x, y, z);
		
		final boolean isExtended = (data & 0x8) > 0;
		final int direction = data & 0x7;
		
		SubMesh subMesh = new SubMesh();
		SubMesh pistonFaceMesh = null;
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, chunk, x, y, z);
		Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
		
		final float height = 1.0f / 16.0f * 12.0f;
		
		// Piston base

		SubMesh topMesh = new SubMesh();
		// Top
		topMesh.addQuad(new Vector3f(0, height, 0), new Vector3f(1, height, 0),
						new Vector3f(1, height, 1), new Vector3f(0, height, 1), colour, top);
		
		SubMesh bottomMesh = new SubMesh();
		// Bottom
		bottomMesh.addQuad(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1),
						new Vector3f(1, 0, 1), new Vector3f(1, 0, 0), colour, bottom);
	
		SubMesh baseMesh = new SubMesh();
		// West
		baseMesh.addQuad(new Vector3f(0, height, 0), new Vector3f(0, height, 1),
						new Vector3f(0, 0, 1),  new Vector3f(0, 0, 0), colour, baseSide);
		// North
		baseMesh.addQuad(new Vector3f(1, height, 0), new Vector3f(0, height, 0),
						new Vector3f(0, 0, 0),  new Vector3f(1, 0, 0), colour, baseSide);
		// South
		baseMesh.addQuad(new Vector3f(0, height, 1), new Vector3f(1, height, 1),
						new Vector3f(1, 0, 1),  new Vector3f(0, 0, 1), colour, baseSide);
		// East
		baseMesh.addQuad(new Vector3f(1, height, 1), new Vector3f(1, height, 0),
						new Vector3f(1, 0, 0), new Vector3f(1, 0, 1), colour, baseSide);
		
		if (isExtended)
		{
			// base of piston arm?
		}
		else
		{
			// Unextended piston top
			
			pistonFaceMesh = new SubMesh();
			// Top
			pistonFaceMesh.addQuad(new Vector3f(0, 1, 0), new Vector3f(1, 1, 0),
							new Vector3f(1, 1, 1), new Vector3f(0, 1, 1), colour, pistonFace);
			// West
			subMesh.addQuad(new Vector3f(0, 1, 0), new Vector3f(0, 1, 1),
							new Vector3f(0, height, 1),  new Vector3f(0, height, 0), colour, pistonEdge);
			// North
			subMesh.addQuad(new Vector3f(1, 1, 0), new Vector3f(0, 1, 0),
							new Vector3f(0, height, 0),  new Vector3f(1, height, 0), colour, pistonEdge);
			// South
			subMesh.addQuad(new Vector3f(0, 1, 1), new Vector3f(1, 1, 1),
							new Vector3f(1, height, 1),  new Vector3f(0, height, 1), colour, pistonEdge);
			// East
			subMesh.addQuad(new Vector3f(1, 1, 1), new Vector3f(1, 1, 0),
							new Vector3f(1, height, 0), new Vector3f(1, height, 1), colour, pistonEdge);
		}
		
		Rotation horizRotation = Rotation.Clockwise;
		float horizAngle = 0;
		
		Rotation vertRotation = Rotation.None;
		float vertAngle = 0;
	
		// Set angle/rotation from block data flags
		if (direction == 0)
		{
			// down
			vertRotation = Rotation.Clockwise;
			vertAngle = 180;
		}
		else if (direction == 1)
		{
			// up
			// ...unchanged
		}
		else if (direction == 2)
		{
			// north
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.AntiClockwise;
			horizAngle = 90;
		}
		else if (direction == 3)
		{
			// south
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.Clockwise;
			horizAngle = 90;
		}
		else if (direction == 4)
		{
			// west
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
		}
		else if (direction == 5)
		{
			// east
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.Clockwise;
			horizAngle = 180;
		}
		
		baseMesh.pushTo(geometry.getMesh(baseSide.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, vertRotation, vertAngle);
		subMesh.pushTo(geometry.getMesh(pistonEdge.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, vertRotation, vertAngle);
		topMesh.pushTo(geometry.getMesh(top.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, vertRotation, vertAngle);
		bottomMesh.pushTo(geometry.getMesh(bottom.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, vertRotation, vertAngle);
		if(pistonFaceMesh != null)
			pistonFaceMesh.pushTo(geometry.getMesh(pistonFace.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, vertRotation, vertAngle);
	}
}
