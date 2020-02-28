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
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import static tectonicus.Version.VERSION_4;

public class MinecartTracks implements BlockType
{
	private final String name;
	private final SubTexture straightTexture, cornerTexture, powered;
	private final boolean straightOnly;
	
	public MinecartTracks(String name, SubTexture straight, SubTexture corner, SubTexture powered, final boolean straightOnly)
	{
		this.name = name;
		this.straightTexture = straight;
		this.cornerTexture = corner;
		
		if (powered.texturePackVersion != VERSION_4)
		{
			final float texel = 1.0f / powered.texture.getHeight();
			final float tile = texel * powered.texture.getWidth();
			this.powered = new SubTexture(powered.texture, powered.u0, powered.v0, powered.u1, powered.v0+tile);
		}
		else
		{
			this.powered = powered;
		}
		
		this.straightOnly = straightOnly;
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
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		Mesh straightMesh = geometry.getMesh(straightTexture.texture, Geometry.MeshType.AlphaTest);
		Mesh poweredMesh = geometry.getMesh(powered.texture, Geometry.MeshType.AlphaTest);
		Mesh cornerMesh = geometry.getMesh(cornerTexture.texture, Geometry.MeshType.AlphaTest);
		
		// TODO: Should we change the light if the track is inclined?
		final float light = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.Top);
		
		final int data = chunk.getBlockData(x, y, z);

		int orientation;
		if (straightOnly)
			orientation = data & 0x7;
		else
			orientation = data;
		final boolean isPowered = (data & 0x8) > 0;
		
		Vector2f uv0 = new Vector2f();
		Vector2f uv1 = new Vector2f();
		Vector2f uv2 = new Vector2f();
		Vector2f uv3 = new Vector2f();
		
		final float groundOffset = 1.0f / 16.0f;
		
		float p0Height = y + groundOffset;
		float p1Height = y + groundOffset;
		float p2Height = y + groundOffset;
		float p3Height = y + groundOffset;
		
		SubTexture straightTex = isPowered ? powered : straightTexture;
		
		switch (orientation)
		{
			case 0x0:
			{
				// flat track going north-south
				uv0.set(straightTex.u0, straightTex.v0);
				uv1.set(straightTex.u1, straightTex.v0);
				uv2.set(straightTex.u1, straightTex.v1);
				uv3.set(straightTex.u0, straightTex.v1);
				break;
			}
			case 0x1:
			{
				// flat track going east-west
				uv0.set(straightTex.u1, straightTex.v0);
				uv1.set(straightTex.u1, straightTex.v1);
				uv2.set(straightTex.u0, straightTex.v1);
				uv3.set(straightTex.u0, straightTex.v0);
				break;
			}
			case 0x2:
			{
				// track ascending to the east
				uv0.set(straightTex.u1, straightTex.v0);
				uv1.set(straightTex.u1, straightTex.v1);
				uv2.set(straightTex.u0, straightTex.v1);
				uv3.set(straightTex.u0, straightTex.v0);
				
				p1Height++;
				p2Height++;
				break;
			}
			case 0x3:
			{
				// track ascending to the west
				uv0.set(straightTex.u1, straightTex.v0);
				uv1.set(straightTex.u1, straightTex.v1);
				uv2.set(straightTex.u0, straightTex.v1);
				uv3.set(straightTex.u0, straightTex.v0);
				
				p0Height++;
				p3Height++;
				break;
			}
			case 0x4:
			{
				// track ascending to the north
				uv0.set(straightTex.u0, straightTex.v0);
				uv1.set(straightTex.u1, straightTex.v0);
				uv2.set(straightTex.u1, straightTex.v1);
				uv3.set(straightTex.u0, straightTex.v1);
				
				p0Height++;
				p1Height++;
				break;
			}
			case 0x5:
			{
				// track ascending to the south
				uv0.set(straightTex.u0, straightTex.v0);
				uv1.set(straightTex.u1, straightTex.v0);
				uv2.set(straightTex.u1, straightTex.v1);
				uv3.set(straightTex.u0, straightTex.v1);
				
				p2Height++;
				p3Height++;
				break;
			}
			case 0x6:
			{
				// north-west corner
				uv0.set(cornerTexture.u0, cornerTexture.v0);
				uv1.set(cornerTexture.u1, cornerTexture.v0);
				uv2.set(cornerTexture.u1, cornerTexture.v1);
				uv3.set(cornerTexture.u0, cornerTexture.v1);
				break;
			}
			case 0x7:
			{
				// north-east
				uv0.set(cornerTexture.u1, cornerTexture.v0);
				uv1.set(cornerTexture.u0, cornerTexture.v0);
				uv2.set(cornerTexture.u0, cornerTexture.v1);
				uv3.set(cornerTexture.u1, cornerTexture.v1);
				break;
			}
			case 0x8:
			{
				// south-east
				uv0.set(cornerTexture.u1, cornerTexture.v1);
				uv1.set(cornerTexture.u0, cornerTexture.v1);
				uv2.set(cornerTexture.u0, cornerTexture.v0);
				uv3.set(cornerTexture.u1, cornerTexture.v0);
				break;
			}
			case 0x9:
			{
				// south-west
				uv0.set(cornerTexture.u0, cornerTexture.v1);
				uv1.set(cornerTexture.u1, cornerTexture.v1);
				uv2.set(cornerTexture.u1, cornerTexture.v0);
				uv3.set(cornerTexture.u0, cornerTexture.v0);
				break;
			}
			default:
				assert (false);
		}
		
		if(orientation >= 0x6)
		{
			MeshUtil.addQuad(cornerMesh,	new Vector3f(x,		p0Height,	z),
					new Vector3f(x+1,	p1Height,	z),
					new Vector3f(x+1,	p2Height,	z+1),
					new Vector3f(x,		p3Height,	z+1),
					new Vector4f(light, light, light, 1.0f),
					uv0, uv1, uv2, uv3);
		}
		else if(isPowered)
		{
			MeshUtil.addDoubleSidedQuad(poweredMesh,	new Vector3f(x,		p0Height,	z),
					new Vector3f(x+1,	p1Height,	z),
					new Vector3f(x+1,	p2Height,	z+1),
					new Vector3f(x,		p3Height,	z+1),
					new Vector4f(light, light, light, 1.0f),
					uv0, uv1, uv2, uv3);
		}
		else
		{
			MeshUtil.addDoubleSidedQuad(straightMesh,	new Vector3f(x,		p0Height,	z),
								new Vector3f(x+1,	p1Height,	z),
								new Vector3f(x+1,	p2Height,	z+1),
								new Vector3f(x,		p3Height,	z+1),
								new Vector4f(light, light, light, 1.0f),
								uv0, uv1, uv2, uv3);
		}
	}
	
}
