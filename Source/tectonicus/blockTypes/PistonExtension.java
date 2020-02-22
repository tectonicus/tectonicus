/*
 * Copyright (c) 2012-2020, John Campbell and other contributors.  All rights reserved.
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

public class PistonExtension implements BlockType
{
	private final String name;
	
	private SubTexture pistonEdge, normalFace, stickyFace;
	
	public PistonExtension(String name, SubTexture edge, SubTexture normalFace, SubTexture stickyFace)
	{
		this.name = name;
		
		this.normalFace = normalFace;
		if (stickyFace.texturePackVersion != VERSION_4)
		{
			final float texel = 1.0f / stickyFace.texture.getHeight();
			final float tile = texel * stickyFace.texture.getWidth();
			this.stickyFace = new SubTexture(stickyFace.texture, stickyFace.u0, stickyFace.v0, stickyFace.u1, stickyFace.v0+tile);
		}
		else
		{
			this.stickyFace = stickyFace;
		}
		
		final float divide;
		if (edge.texturePackVersion == VERSION_4)
			divide = 1.0f / 16.0f / 16.0f * 4.0f;
		else
			divide = 1.0f / 16.0f * 4.0f;
		
		this.pistonEdge = new SubTexture(edge.texture, edge.u0, edge.v0, edge.u1, edge.v0+divide);
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
	public void addInteriorGeometry(int x, int y, int z, BlockContext world,
			BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, chunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		final int data = chunk.getBlockData(x, y, z);
		final boolean isSticky = (data & 0x8) > 0;
		final int direction = data & 0x7;
		
		SubMesh subMesh = new SubMesh();
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, chunk, x, y, z);
		Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
		
		final float height = 1.0f / 16.0f * 12.0f;
		
		SubTexture pistonFace = isSticky ? stickyFace : normalFace;
		
		// Piston face
		// We do this manually with multiple SubMeshes in order to work with MC 1.5 texture packs, but still use the SubMesh rotation
		SubMesh topMesh = new SubMesh();
		// Top
		topMesh.addQuad(new Vector3f(0, 1, 0), new Vector3f(1, 1, 0),
						new Vector3f(1, 1, 1), new Vector3f(0, 1, 1), colour, pistonFace);
		
		SubMesh bottomMesh = new SubMesh();
		// Bottom
		bottomMesh.addQuad(new Vector3f(0, height, 0), new Vector3f(0, height, 1),
						new Vector3f(1, height, 1), new Vector3f(1, height, 0), colour, normalFace);
	
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
		
		// Piston arm
		{
			final float armX = 1.0f / 16.0f * 6.0f;
			final float armY = 1.0f / 16.0f * -4.0f;
			final float armZ = armX;
			
			final float armWidth = 1.0f / 16.0f * 4.0f;
		
			// Have to add these manually as they have rotated texture coords
			
			// West
			subMesh.addQuad( new Vector3f(armX, armY+1, armZ), new Vector3f(armX, armY+1, armZ+armWidth),
							new Vector3f(armX, armY, armZ+armWidth),  new Vector3f(armX, armY, armZ),
							colour,
							new Vector2f(pistonEdge.u1, pistonEdge.v0), new Vector2f(pistonEdge.u1, pistonEdge.v1),
							new Vector2f(pistonEdge.u0, pistonEdge.v1), new Vector2f(pistonEdge.u0, pistonEdge.v0));
			
			// South
			subMesh.addQuad( new Vector3f(armX+armWidth, armY+1, armZ), new Vector3f(armX, armY+1, armZ),
							new Vector3f(armX, armY, armZ),  new Vector3f(armX+armWidth, armY, armZ), 
							colour,
							new Vector2f(pistonEdge.u1, pistonEdge.v0), new Vector2f(pistonEdge.u1, pistonEdge.v1),
							new Vector2f(pistonEdge.u0, pistonEdge.v1), new Vector2f(pistonEdge.u0, pistonEdge.v0));
			
			// North
			subMesh.addQuad( new Vector3f(armX, armY+1, armZ+armWidth), new Vector3f(armX+armWidth, armY+1, armZ+armWidth),
							new Vector3f(armX+armWidth, armY, armZ+armWidth),  new Vector3f(armX, armY, armZ+armWidth), 
							colour,
							new Vector2f(pistonEdge.u1, pistonEdge.v0), new Vector2f(pistonEdge.u1, pistonEdge.v1),
							new Vector2f(pistonEdge.u0, pistonEdge.v1), new Vector2f(pistonEdge.u0, pistonEdge.v0));
			
			// East
			subMesh.addQuad(new Vector3f(armX+armWidth, armY+1, armZ+armWidth), new Vector3f(armX+armWidth, armY+1, armZ),
							new Vector3f(armX+armWidth, armY, armZ), new Vector3f(armX+armWidth, armY, armZ+armWidth), 
							colour,
							new Vector2f(pistonEdge.u1, pistonEdge.v0), new Vector2f(pistonEdge.u1, pistonEdge.v1),
							new Vector2f(pistonEdge.u0, pistonEdge.v1), new Vector2f(pistonEdge.u0, pistonEdge.v0));
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
			// east
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.AntiClockwise;
			horizAngle = 90;
		}
		else if (direction == 3)
		{
			// west
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.Clockwise;
			horizAngle = 90;
		}
		else if (direction == 4)
		{
			// north
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
		}
		else if (direction == 5)
		{
			// south
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.Clockwise;
			horizAngle = 180;
		}
		
		subMesh.pushTo(geometry.getMesh(pistonEdge.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, vertRotation, vertAngle);
		topMesh.pushTo(geometry.getMesh(pistonFace.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, vertRotation, vertAngle);
		bottomMesh.pushTo(geometry.getMesh(normalFace.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, vertRotation, vertAngle);
	}
}
