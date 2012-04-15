/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

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

public class PistonExtension implements BlockType
{
	private final String name;
	
	private SubTexture pistonEdge, normalFace, stickyFace;
	
	public PistonExtension(String name, SubTexture edge, SubTexture normalFace, SubTexture stickyFace)
	{
		this.name = name;
		
		this.normalFace = normalFace;
		this.stickyFace = stickyFace;
		
		final float divide = 1.0f / 16.0f / 16.0f * 4.0f;
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
		
		final float height = 1.0f / 16.0f * 4.0f;
		
		SubTexture pistonFace = isSticky ? stickyFace : normalFace;
		
		// Piston face
		SubMesh.addBlock(subMesh, 0, 1f-height, 0, 1, height, 1, colour, pistonEdge, pistonFace, normalFace);
		
		// Piston arm
		{
			final float armX = 1.0f / 16.0f * 6.0f;
			final float armY = 1.0f / 16.0f * -4.0f;
			final float armZ = armX;
			
			final float armWidth = 1.0f / 16.0f * 4.0f;
		
			// Have to add these manually as they have rotated texture coords
			
			// North
			subMesh.addQuad( new Vector3f(armX, armY+1, armZ), new Vector3f(armX, armY+1, armZ+armWidth),
							new Vector3f(armX, armY, armZ+armWidth),  new Vector3f(armX, armY, armZ),
							colour,
							new Vector2f(pistonEdge.u1, pistonEdge.v0), new Vector2f(pistonEdge.u1, pistonEdge.v1),
							new Vector2f(pistonEdge.u0, pistonEdge.v1), new Vector2f(pistonEdge.u0, pistonEdge.v0));
			
			// East
			subMesh.addQuad( new Vector3f(armX+armWidth, armY+1, armZ), new Vector3f(armX, armY+1, armZ),
							new Vector3f(armX, armY, armZ),  new Vector3f(armX+armWidth, armY, armZ), 
							colour,
							new Vector2f(pistonEdge.u1, pistonEdge.v0), new Vector2f(pistonEdge.u1, pistonEdge.v1),
							new Vector2f(pistonEdge.u0, pistonEdge.v1), new Vector2f(pistonEdge.u0, pistonEdge.v0));
			
			// West
			subMesh.addQuad( new Vector3f(armX, armY+1, armZ+armWidth), new Vector3f(armX+armWidth, armY+1, armZ+armWidth),
							new Vector3f(armX+armWidth, armY, armZ+armWidth),  new Vector3f(armX, armY, armZ+armWidth), 
							colour,
							new Vector2f(pistonEdge.u1, pistonEdge.v0), new Vector2f(pistonEdge.u1, pistonEdge.v1),
							new Vector2f(pistonEdge.u0, pistonEdge.v1), new Vector2f(pistonEdge.u0, pistonEdge.v0));
			
			// South
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
		
		subMesh.pushTo(geometry.getMesh(normalFace.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, vertRotation, vertAngle);
	}
	
}
