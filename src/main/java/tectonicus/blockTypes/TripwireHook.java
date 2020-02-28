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
import static tectonicus.Version.VERSION_5;

public class TripwireHook implements BlockType
{	
	private final String name;

	private final SubTexture base, hookRing, hookRingSide, hookPole, tripwire;
	
	public TripwireHook(String name, SubTexture base, SubTexture hook, SubTexture tripwire)
	{
		this.name = name;
		
		this.base = base;
		
		final float texel;
		if (base.texturePackVersion == VERSION_4)
			texel = 1.0f / 16.0f / 16.0f;
		else
			texel = 1.0f / 16.0f;
		
		this.hookRing = new SubTexture(hook.texture, hook.u0+texel*5, hook.v0+texel*3, hook.u0+texel*11, hook.v1-texel*7);
		this.hookRingSide = new SubTexture(hook.texture, hook.u0+texel*5, hook.v0+texel*3, hook.u0+texel*7, hook.v1-texel*7);
		this.hookPole = new SubTexture(hook.texture, hook.u0+texel*7, hook.v0+texel*10, hook.u0+texel*9, hook.v1);
		
		if (base.texturePackVersion == VERSION_4 || base.texturePackVersion == VERSION_5)
			this.tripwire = new SubTexture(tripwire.texture, tripwire.u0, tripwire.v0+texel*2, tripwire.u1, tripwire.v0+texel*4);
		else
			this.tripwire = new SubTexture(tripwire.texture, tripwire.u0, tripwire.v0+texel*6, tripwire.u1, tripwire.v0+texel*8);
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
		final int data = rawChunk.getBlockData(x, y, z);
		
		final float lightness = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
		Vector4f white = new Vector4f(lightness, lightness, lightness, 1);
		
		final float offSet = 1.0f / 16.0f;
		
		SubMesh baseMesh = new SubMesh();
		SubMesh leverMesh = new SubMesh();
		SubMesh hookMesh = new SubMesh();
		SubMesh tripwireMesh = new SubMesh();
		
		Rotation horizRotation = Rotation.Clockwise;
		float horizAngle = 0;
		
		Rotation vertRotation = Rotation.None;
		float vertAngle = 0;
		
		// Set angle/rotation from block data flags
		if(data == 3 || data == 7 || data == 15) // Facing east
		{
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.Clockwise;
			horizAngle = 180;		
		}
		else if (data == 1 || data == 5 || data == 13) // Facing west
		{
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
		}
		else if (data == 0 || data == 4 || data == 12) // Facing south
		{
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.Clockwise;
			horizAngle = 90;
		}
		else if (data == 2 || data == 6 || data == 14) // Facing north
		{
			vertRotation = Rotation.Clockwise;
			vertAngle = 90;
			
			horizRotation = Rotation.AntiClockwise;
			horizAngle = 90;
		}
		
		float lrotate = 0;
		float hrotate = 0;
		
		SubMesh.addBlock(baseMesh, offSet, 0, offSet*6, offSet*8, offSet*2, offSet*4, white, base, base, base);
		if((data & 0x4) > 0 || (data & 0x8) > 0)
		{
			SubMesh.addBlockSimple(leverMesh, offSet*5, 0, offSet*7.2f, offSet*1.6f, offSet*6, offSet*1.6f, white, hookPole, hookPole, hookPole);
			SubMesh.addBlockSimple(hookMesh, offSet*6, offSet*5.9f, offSet*6.25f, offSet*0.5f, offSet*3.5f, offSet*3.5f, white, hookRing, hookRingSide, hookRingSide);
			tripwireMesh.addQuad(new Vector3f(offSet*-3.8f, offSet*6, offSet*7.7f), new Vector3f(offSet*6, offSet*6, offSet*7.7f), new Vector3f(offSet*6, offSet*6, offSet*8.3f), new Vector3f(offSet*-3.8f, offSet*6, offSet*8.3f), white, tripwire);
			lrotate = vertAngle+5;
			hrotate = vertAngle+8;
		}
		else
		{
			SubMesh.addBlockSimple(leverMesh, offSet*10, offSet, offSet*7.2f, offSet*1.6f, offSet*6, offSet*1.6f, white, hookPole, hookPole, hookPole);
			SubMesh.addBlockSimple(hookMesh, offSet*7, offSet*5, offSet*6.25f, offSet*0.5f, offSet*3.5f, offSet*3.5f, white, hookRing, hookRingSide, hookRingSide);
			
			lrotate = vertAngle-45;
			hrotate = vertAngle+45;
		}
		
		baseMesh.pushTo(geometry.getMesh(base.texture, Geometry.MeshType.AlphaTest), x, y, z, horizRotation, horizAngle, vertRotation, vertAngle);
		leverMesh.pushTo(geometry.getMesh(hookPole.texture, Geometry.MeshType.AlphaTest), x, y, z, horizRotation, horizAngle, vertRotation, lrotate);
		hookMesh.pushTo(geometry.getMesh(hookRing.texture, Geometry.MeshType.AlphaTest), x, y, z, horizRotation, horizAngle, vertRotation, hrotate);
		tripwireMesh.pushTo(geometry.getMesh(tripwire.texture, Geometry.MeshType.Transparent), x, y, z, horizRotation, horizAngle, vertRotation, 30);
	}
}
