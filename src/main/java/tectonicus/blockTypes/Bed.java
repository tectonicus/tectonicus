/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.joml.Vector2f;
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

public class Bed implements BlockType
{
	private final SubTexture headTop, footTop;
	private final SubTexture headSide, footSide;
	private final SubTexture headEdge, footEdge;
	
	public Bed(SubTexture headTop, SubTexture footTop, SubTexture headSide, SubTexture footSide, SubTexture headEdge, SubTexture footEdge)
	{
		this.headTop = headTop;
		this.footTop = footTop;
		
		final float vHeight;
		if (headTop.texturePackVersion == VERSION_4)
			vHeight = 1.0f / 16.0f / 16.0f * 9.0f;
		else
			vHeight = 1.0f / 16.0f * 9.0f;
		
		this.headSide = new SubTexture(headSide.texture, headSide.u0, headSide.v0+vHeight, headSide.u1, headSide.v1);
		this.footSide = new SubTexture(footSide.texture, footSide.u0, footSide.v0+vHeight, footSide.u1, footSide.v1);
		
		this.headEdge = new SubTexture(headEdge.texture, headEdge.u0, headEdge.v0+vHeight, headEdge.u1, headEdge.v1);
		this.footEdge = new SubTexture(footEdge.texture, footEdge.u0, footEdge.v0+vHeight, footEdge.u1, footEdge.v1);
	}
	
	@Override
	public String getName()
	{
		return "Bed";
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
	public void addEdgeGeometry(final int x, final int y, final int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		final int data = rawChunk.getBlockData(x, y, z);
		final boolean isHead = (data & 0x8) > 0;
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		
		final float height = 1.0f / 16.0f * 9.0f;
		
		SubMesh headSideMesh = new SubMesh();
		SubMesh headTopMesh = new SubMesh();
		SubMesh footSideMesh = new SubMesh();
		SubMesh rightMesh = new SubMesh();
		SubMesh leftMesh = new SubMesh();
		
		// Create the geometry in the unit cube
		
		SubTexture topTex = isHead ? headTop : footTop;
		headTopMesh.addQuad(new Vector3f(0, height, 0), new Vector3f(1, height, 0), new Vector3f(1, height, 1), new Vector3f(0, height, 1), white, topTex);
		
		// Head or feet sides
		if (isHead)
		{
			headSideMesh.addQuad(new Vector3f(1, height, 1), new Vector3f(1, height, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 1), white, headSide);
		}
		else
		{
			footSideMesh.addQuad(new Vector3f(0, height, 0), new Vector3f(0, height, 1), new Vector3f(0, 0, 1), new Vector3f(0, 0, 0), white, footSide);
		}
		
		// Left side (if lying down in bed facing up)
		SubTexture leftSide = isHead ? headEdge : footEdge;
		leftMesh.addQuad(new Vector3f(0, height, 1), new Vector3f(1, height, 1), new Vector3f(1, 0, 1), new Vector3f(0, 0, 1), white, leftSide);
		
		// Right side
		
		SubTexture rightSide = isHead ? headEdge : footEdge;
		rightMesh.addQuad(new Vector3f(1, height, 0), new Vector3f(0, height, 0), new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), white,
						new Vector2f(rightSide.u1, rightSide.v0), new Vector2f(rightSide.u0, rightSide.v0),new Vector2f(rightSide.u0, rightSide.v1), new Vector2f(rightSide.u1, rightSide.v1) );
		
		SubMesh.Rotation rotation = Rotation.None;
		float angle = 0;
		
		final int dir = (data & 0x3);
		if (dir == 0)
		{
			// Head is pointing west
			rotation = Rotation.AntiClockwise;
			angle = 90;
		}
		else if (dir == 1)
		{
			// Head is pointing north
			rotation = Rotation.Clockwise;
			angle = 180;
		}
		else if (dir == 2)
		{
			// Head is pointing east
			rotation = Rotation.Clockwise;
			angle = 90;
		}
		else if (dir == 3)
		{
			// Head is pointing south
		}
		else
		{
			System.err.println("Warning: Unknown block data for bed");
		}
		
		// Apply rotation
		rightMesh.pushTo(geometry.getMesh(rightSide.texture, Geometry.MeshType.AlphaTest), x, y, z, rotation, angle);
		leftMesh.pushTo(geometry.getMesh(leftSide.texture, Geometry.MeshType.AlphaTest), x, y, z, rotation, angle);
		headSideMesh.pushTo(geometry.getMesh(headSide.texture, Geometry.MeshType.AlphaTest), x, y, z, rotation, angle);
		headTopMesh.pushTo(geometry.getMesh(topTex.texture, Geometry.MeshType.AlphaTest), x, y, z, rotation, angle);
		footSideMesh.pushTo(geometry.getMesh(footSide.texture, Geometry.MeshType.AlphaTest), x, y, z, rotation, angle);
	}
	
}
