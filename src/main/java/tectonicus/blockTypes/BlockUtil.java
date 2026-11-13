/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import lombok.experimental.UtilityClass;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.LegacyBlockLighting;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil.FaceDirection;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

@UtilityClass
public class BlockUtil
{
	public static void addTop(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, BlockTypeRegistry registry) {
		addTop(world, rawChunk, mesh, x, y, z, colour, texture, LegacyBlockLighting.FLAT);
	}

	public static void addTop(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, BlockTypeRegistry registry, Rotation rotation) {
		addTop(world, rawChunk, mesh, x, y, z, colour, texture, LegacyBlockLighting.FLAT, rotation);
	}

	public static void addBottom(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, BlockTypeRegistry registry) {
		addBottom(world, rawChunk, mesh, x, y, z, colour, texture, LegacyBlockLighting.FLAT);
	}

	public static void addBottom(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, BlockTypeRegistry registry, Rotation rotation) {
		addBottom(world, rawChunk, mesh, x, y, z, colour, texture, LegacyBlockLighting.FLAT, rotation);
	}

	public static void addNorth(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, BlockTypeRegistry registry) {
		addNorth(world, rawChunk, mesh, x, y, z, colour, texture, LegacyBlockLighting.FLAT);
	}

	public static void addWest(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, BlockTypeRegistry registry, Rotation rotation) {
		addWest(world, rawChunk, mesh, x, y, z, colour, texture, LegacyBlockLighting.FLAT, rotation);
	}

	public static void addSouth(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, BlockTypeRegistry registry) {
		addSouth(world, rawChunk, mesh, x, y, z, colour, texture, LegacyBlockLighting.FLAT);
	}

	public static void addEast(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, BlockTypeRegistry registry, Rotation rotation) {
		addEast(world, rawChunk, mesh, x, y, z, colour, texture, LegacyBlockLighting.FLAT, rotation);
	}

	public static void addEast(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, BlockTypeRegistry registry) {
		addEast(world, rawChunk, mesh, x, y, z, colour, texture, LegacyBlockLighting.FLAT);
	}

	public static void addNorth(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, BlockTypeRegistry registry, Rotation rotation) {
		addNorth(world, rawChunk, mesh, x, y, z, colour, texture, LegacyBlockLighting.FLAT, rotation);
	}

	public static void addWest(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, BlockTypeRegistry registry) {
		addWest(world, rawChunk, mesh, x, y, z, colour, texture, LegacyBlockLighting.FLAT);
	}

	public static void addSouth(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, BlockTypeRegistry registry, Rotation rotation) {
		addSouth(world, rawChunk, mesh, x, y, z, colour, texture, LegacyBlockLighting.FLAT, rotation);
	}


	public static void addTop(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.UP, Rotation.None, false);
	}

	public static void addTop(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting, Rotation rotation) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.UP, rotation, false);
	}

	public static void addBottom(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.DOWN, Rotation.None, false);
	}

	public static void addBottom(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting, Rotation rotation) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.DOWN, rotation, false);
	}

	public static void addNorth(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.WEST, Rotation.None, false);
	}

	public static void addWest(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting, Rotation rotation) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.WEST, rotation, false);
	}

	public static void addSouth(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.EAST, Rotation.None, false);
	}

	public static void addEast(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting, Rotation rotation) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.EAST, rotation, false);
	}

	public static void addEast(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.NORTH, Rotation.None, false);
	}

	public static void addNorth(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting, Rotation rotation) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.NORTH, rotation, false);
	}

	public static void addWest(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.SOUTH, Rotation.None, false);
	}

	public static void addSouth(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting, Rotation rotation) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.SOUTH, rotation, false);
	}

	public static void addInteriorTop(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.UP, Rotation.None, true);
	}

	public static void addInteriorBottom(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.DOWN, Rotation.None, true);
	}

	public static void addInteriorNorth(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.WEST, Rotation.None, true);
	}

	public static void addInteriorSouth(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.EAST, Rotation.None, true);
	}

	public static void addInteriorEast(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.NORTH, Rotation.None, true);
	}

	public static void addInteriorWest(BlockContext world, RawChunk rawChunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting) {
		addCubeFace(world, rawChunk, mesh, x, y, z, colour, texture, lighting, FaceDirection.SOUTH, Rotation.None, true);
	}

	// The legacy renderer uses X for north/south face shading and Z for east/west.
	private static void addCubeFace(BlockContext world, RawChunk chunk, Mesh mesh, int x, int y, int z,
			Colour4f colour, SubTexture texture, LegacyBlockLighting lighting, FaceDirection face,
			Rotation rotation, boolean interior) {
		int neighborX = x + face.getDx();
		int neighborY = y + face.getDy();
		int neighborZ = z + face.getDz();
		if (world.getBlockType(chunk.getChunkCoord(), neighborX, neighborY, neighborZ).isSolid())
			return;

		LightFace shade = switch (face) {
			case UP, DOWN -> LightFace.Top;
			case WEST, EAST -> LightFace.NorthSouth;
			case NORTH, SOUTH -> LightFace.EastWest;
		};
		float light = 0;
		if (!lighting.isSmooth()) {
			light = interior
					? world.getLight(chunk, neighborX, neighborY, neighborZ, shade, world.getNightLightAdjustment())
					: world.getLight(chunk.getChunkCoord(), neighborX, neighborY, neighborZ, shade);
		}
		Vector4f tint = new Vector4f(colour.r, colour.g, colour.b, colour.a);
		switch (face) {
			case UP -> addRotatedQuad(lighting, mesh, face, shade, rotation, tint, light, texture,
					new Vector3f(x, y + 1, z), new Vector3f(x + 1, y + 1, z), new Vector3f(x + 1, y + 1, z + 1), new Vector3f(x, y + 1, z + 1));
			case DOWN -> addRotatedQuad(lighting, mesh, face, shade, rotation, tint, light, texture,
					new Vector3f(x, y, z + 1), new Vector3f(x + 1, y, z + 1), new Vector3f(x + 1, y, z), new Vector3f(x, y, z));
			case WEST -> addRotatedQuad(lighting, mesh, face, shade, rotation, tint, light, texture,
					new Vector3f(x, y + 1, z), new Vector3f(x, y + 1, z + 1), new Vector3f(x, y, z + 1), new Vector3f(x, y, z));
			case EAST -> addRotatedQuad(lighting, mesh, face, shade, rotation, tint, light, texture,
					new Vector3f(x + 1, y + 1, z + 1), new Vector3f(x + 1, y + 1, z), new Vector3f(x + 1, y, z), new Vector3f(x + 1, y, z + 1));
			case NORTH -> addRotatedQuad(lighting, mesh, face, shade, rotation, tint, light, texture,
					new Vector3f(x + 1, y + 1, z), new Vector3f(x, y + 1, z), new Vector3f(x, y, z), new Vector3f(x + 1, y, z));
			case SOUTH -> addRotatedQuad(lighting, mesh, face, shade, rotation, tint, light, texture,
					new Vector3f(x, y + 1, z + 1), new Vector3f(x + 1, y + 1, z + 1), new Vector3f(x + 1, y, z + 1), new Vector3f(x, y, z + 1));
		}
	}

	private static void addRotatedQuad(LegacyBlockLighting lighting, Mesh mesh, FaceDirection face, LightFace shade,
			Rotation rotation, Vector4f tint, float light, SubTexture texture, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3) {
		if (rotation == Rotation.Clockwise)
			lighting.addQuad(mesh, face, shade, p1, p2, p3, p0, tint, light, texture);
		else if (rotation == Rotation.AntiClockwise)
			lighting.addQuad(mesh, face, shade, p3, p0, p1, p2, tint, light, texture);
		else if (rotation == Rotation.Flip)
			lighting.addQuad(mesh, face, shade, p2, p3, p0, p1, tint, light, texture);
		else
			lighting.addQuad(mesh, face, shade, p0, p1, p2, p3, tint, light, texture);
	}

	public static final void clamp(Vector4f vec)
	{
		vec.x = clamp(vec.x, 0, 1);
		vec.y = clamp(vec.y, 0, 1);
		vec.z = clamp(vec.z, 0, 1);
		vec.w = clamp(vec.w, 0, 1);
	}
	
	private static float clamp(float val, float min, float max)
	{
		if (val < min)
			return min;
		if (val > max)
			return max;
		return val;
	}

	public static void addBlock(Mesh mesh, final float blockX, final float blockY, final float blockZ,
										final int offsetX, final int offsetY, final int offsetZ,
										final int width, final int height, final int depth,
										Vector4f colour, SubTexture texture,
										final float topLight, final float northSouthLight, final float eastWestLight) {
		addBlock(LegacyBlockLighting.FLAT, mesh, blockX, blockY, blockZ, offsetX, offsetY, offsetZ, width, height, depth, colour, texture, topLight, northSouthLight, eastWestLight);
	}

	public static void addBlock(Mesh mesh, final float blockX, final float blockY, final float blockZ,
			final int offsetX, final int offsetY, final int offsetZ,
			final int width, final int height, final int depth,
			Vector4f colour, SubTexture texture,
			final float topLight, final float northLight, final float southLight, final float eastLight, final float westLight) {
		addBlock(LegacyBlockLighting.FLAT, mesh, blockX, blockY, blockZ, offsetX, offsetY, offsetZ, width, height, depth, colour, texture, topLight, northLight, southLight, eastLight, westLight);
	}

	public static void addPartialBlock(Mesh mesh, final float blockX, final float blockY, final float blockZ,
			final int offsetX, final int offsetY, final int offsetZ,
			final int width, final int height, final int depth,
			Vector4f colour, SubTexture texture,
			final float topLight, final float northSouthLight, final float eastWestLight,
			final boolean top, final boolean bottom, final boolean north, final boolean south, final boolean east, final boolean west) {
		addPartialBlock(LegacyBlockLighting.FLAT, mesh, blockX, blockY, blockZ, offsetX, offsetY, offsetZ, width, height, depth, colour, texture, topLight, northSouthLight, eastWestLight, top, bottom, north, south, east, west);
	}

	public static void addPartialBlock(Mesh mesh, final float blockX, final float blockY, final float blockZ,
			final int offsetX, final int offsetY, final int offsetZ,
			final int width, final int height, final int depth,
			Vector4f colour, SubTexture texture,
			final float topLight, final float northLight, final float southLight, final float eastLight, final float westLight,
			final boolean top, final boolean bottom, final boolean north, final boolean south, final boolean east, final boolean west) {
		addPartialBlock(LegacyBlockLighting.FLAT, mesh, blockX, blockY, blockZ, offsetX, offsetY, offsetZ, width, height, depth, colour, texture, topLight, northLight, southLight, eastLight, westLight, top, bottom, north, south, east, west);
	}

	public static void addBlock(LegacyBlockLighting lighting, Mesh mesh, final float blockX, final float blockY, final float blockZ,
										final int offsetX, final int offsetY, final int offsetZ,
										final int width, final int height, final int depth,
										Vector4f colour, SubTexture texture,
										final float topLight, final float northSouthLight, final float eastWestLight)
	{
		addBlock(lighting, mesh, blockX, blockY, blockZ, offsetX, offsetY, offsetZ, width, height, depth, colour, texture, topLight, northSouthLight, northSouthLight, eastWestLight, eastWestLight);
	}
	
	public static void addBlock(LegacyBlockLighting lighting, Mesh mesh, final float blockX, final float blockY, final float blockZ,
			final int offsetX, final int offsetY, final int offsetZ,
			final int width, final int height, final int depth,
			Vector4f colour, SubTexture texture,
			final float topLight, final float northLight, final float southLight, final float eastLight, final float westLight) {
		// The partial-block helper names its side parameters by the opposite axis.
		addPartialBlock(lighting, mesh, blockX, blockY, blockZ, offsetX, offsetY, offsetZ, width, height, depth,
				colour, texture, topLight, eastLight, westLight, northLight, southLight, true, true, true, true, true, true);
	}

	public static void addPartialBlock(LegacyBlockLighting lighting, Mesh mesh, final float blockX, final float blockY, final float blockZ,
			final int offsetX, final int offsetY, final int offsetZ,
			final int width, final int height, final int depth,
			Vector4f colour, SubTexture texture,
			final float topLight, final float northSouthLight, final float eastWestLight,
			final boolean top, final boolean bottom, final boolean north, final boolean south, final boolean east, final boolean west)
	{
		addPartialBlock(lighting, mesh, blockX, blockY, blockZ, offsetX, offsetY, offsetZ, width, height, depth, colour, texture, topLight, northSouthLight, northSouthLight, eastWestLight, eastWestLight, top, bottom, north, south, east, west);
	}
	
	public static void addPartialBlock(LegacyBlockLighting lighting, Mesh mesh, final float blockX, final float blockY, final float blockZ,
			final int offsetX, final int offsetY, final int offsetZ,
			final int width, final int height, final int depth,
			Vector4f colour, SubTexture texture,
			final float topLight, final float northLight, final float southLight, final float eastLight, final float westLight, 
			final boolean top, final boolean bottom, final boolean north, final boolean south, final boolean east, final boolean west)
	{
		final float normOffX = (offsetX / 16.0f);
		final float normOffY = (offsetY / 16.0f);
		final float normOffZ = (offsetZ / 16.0f);
		
		final float x = blockX + normOffX;
		final float y = blockY + normOffY;
		final float z = blockZ + normOffZ;
		
		final float w = width / 16.0f;
		final float h = height / 16.0f;
		final float d = depth / 16.0f;
		
		final float uRange = texture.u1 - texture.u0;
		final float vRange = texture.v1 - texture.v0;
		
		
		// Top
		if(top)
		{
			Vector3f p0 = new Vector3f(x,		y + h,	z);
			Vector3f p1 = new Vector3f(x + w,	y + h,	z);
			Vector3f p2 = new Vector3f(x + w,	y + h,	z + d);
			Vector3f p3 =  new Vector3f(x,		y + h,	z + d);
			lighting.addQuad(mesh, FaceDirection.UP, LightFace.Top,	p0, p1, p2, p3,
									colour, topLight,
									new Vector2f(texture.u0 + uRange * normOffX,		texture.v0 + vRange * normOffZ),
									new Vector2f(texture.u0 + uRange * (normOffX + w),	texture.v0 + vRange * normOffZ),
									new Vector2f(texture.u0 + uRange * (normOffX + w),	texture.v0 + vRange * (normOffZ + d)),
									new Vector2f(texture.u0 + uRange * normOffX,		texture.v0 + vRange * (normOffZ + d))
			);
		}

		// Bottom
		if(bottom)
		{
			Vector3f p0 = new Vector3f(x,		y,	z);
			Vector3f p1 = new Vector3f(x + w,	y,	z);
			Vector3f p2 = new Vector3f(x + w,	y,	z + d);
			Vector3f p3 =  new Vector3f(x,		y,	z + d);
			lighting.addQuad(mesh, FaceDirection.DOWN, LightFace.Top,	p3, p2, p1, p0,
									colour, topLight,
									new Vector2f(texture.u0 + uRange * normOffX,		texture.v0 + vRange * normOffZ),
									new Vector2f(texture.u0 + uRange * (normOffX + w),	texture.v0 + vRange * normOffZ),
									new Vector2f(texture.u0 + uRange * (normOffX + w),	texture.v0 + vRange * (normOffZ + d)),
									new Vector2f(texture.u0 + uRange * normOffX,		texture.v0 + vRange * (normOffZ + d))
			);
		}
		
		// East
		if(east)
		{
			lighting.addQuad(mesh, FaceDirection.WEST, LightFace.NorthSouth,	new Vector3f(x,	y + h,	z),
				 					new Vector3f(x,	y + h,	z + d),
				 					new Vector3f(x,	y,		z + d),
				 					new Vector3f(x,	y,		z),
									colour, eastLight,
				 					new Vector2f(texture.u0 + uRange * normOffZ,			texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u0 + uRange * (normOffZ + d),		texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u0 + uRange * (normOffZ + d),		texture.v1 - vRange * normOffY),
				 					new Vector2f(texture.u0 + uRange * normOffZ,			texture.v1 - vRange * normOffY)
			);
		}
		
		// West
		if(west)
		{
			lighting.addQuad(mesh, FaceDirection.EAST, LightFace.NorthSouth,	new Vector3f(x + w,	y + h,	z + d),
				 					new Vector3f(x + w,	y + h,	z),
				 					new Vector3f(x + w,	y,		z),
				 					new Vector3f(x + w,	y,		z + d),
									colour, westLight,
				 					new Vector2f(texture.u0 + uRange * normOffZ,			texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u0 + uRange * (normOffZ + d),		texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u0 + uRange * (normOffZ + d),		texture.v1 - vRange * normOffY),
				 					new Vector2f(texture.u0 + uRange * normOffZ,			texture.v1 - vRange * normOffY)
			);
		}

		// north
		if(north)
		{
			lighting.addQuad(mesh, FaceDirection.NORTH, LightFace.EastWest,	new Vector3f(x + w,	y + h,	z),
				 					new Vector3f(x,		y + h,	z),
				 					new Vector3f(x,		y,		z),
				 					new Vector3f(x + w,	y,		z ),
									colour, northLight,
				 					new Vector2f(texture.u1 - uRange * (normOffX + w),		texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u1 - uRange * normOffX,			texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u1 - uRange * normOffX,			texture.v1 - vRange * normOffY),
				 					new Vector2f(texture.u1 - uRange * (normOffX + w),		texture.v1 - vRange * normOffY)
			);
		}
		
		// South
		if(south)
		{
			lighting.addQuad(mesh, FaceDirection.SOUTH, LightFace.EastWest,	new Vector3f(x,		y + h,	z + d),
				 					new Vector3f(x + w,	y + h,	z + d),
				 					new Vector3f(x + w,	y,		z + d),
				 					new Vector3f(x,		y,		z + d),
									colour, southLight,
				 					new Vector2f(texture.u1 - uRange * (normOffX + w),		texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u1 - uRange * normOffX,			texture.v1 - vRange * (normOffY + h)),
				 					new Vector2f(texture.u1 - uRange * normOffX,			texture.v1 - vRange * normOffY),
				 					new Vector2f(texture.u1 - uRange * (normOffX + w),		texture.v1 - vRange * normOffY)
			);
		}
	}
}
