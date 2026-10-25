/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.Minecraft;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

import java.util.List;
import java.util.Random;

/** A deterministic snapshot of the rising and spiraling particles in bubble columns. */
public final class BubbleColumnParticles {
	// Static density choices, not Minecraft's per-animation-callback emission counts.
	private static final int UP_SAMPLES = 8;
	private static final int DOWN_SAMPLES = 4;

	private BubbleColumnParticles() {}

	public static void addGeometry(int x, int y, int z, BlockProperties properties, BlockContext world, RawChunk chunk, Geometry geometry) {
		boolean down = properties == null || !"false".equals(properties.get("drag"));
		List<SubTexture> sprites = world.getTexturePack().findParticleTextures(down ? "minecraft:current_down" : "minecraft:bubble_column_up");
		addGeometry(x, y, z, down, sprites, world, chunk, geometry);
	}

	static void addGeometry(int x, int y, int z, boolean down, List<SubTexture> sprites, BlockContext world, RawChunk chunk, Geometry geometry) {
		if (sprites.isEmpty())
			return;
		long worldX = chunk.getChunkCoord().x * RawChunk.WIDTH + x;
		long worldZ = chunk.getChunkCoord().z * RawChunk.DEPTH + z;
		Random random = new Random(positionSeed(worldX, chunk.getWorldY(y), worldZ));
		float angle = world.getCameraAngleRad();
		float elevation = world.getCameraElevationRad();
		Vector3f right = new Vector3f((float) Math.sin(angle), 0, -(float) Math.cos(angle));
		Vector3f up = new Vector3f(-(float) Math.cos(angle) * (float) Math.sin(elevation),
				(float) Math.cos(elevation), -(float) Math.sin(angle) * (float) Math.sin(elevation));
		for (int i = 0; i < (down ? DOWN_SAMPLES : UP_SAMPLES); i++) {
			SubTexture sprite = sprites.get(random.nextInt(sprites.size()));
			float size = (0.1f + random.nextFloat() * 0.1f) * (0.2f + random.nextFloat() * 0.6f);
			Vector3f position = down ? new Vector3f(x + 0.5f, y + 0.8f, z)
					: i % 2 == 0 ? new Vector3f(x + 0.5f, y, z + 0.5f)
					: new Vector3f(x + random.nextFloat(), y + random.nextFloat(), z + random.nextFloat());
			Vector3f velocity = down ? new Vector3f(0, -0.05f, 0)
					: new Vector3f(jitter(random), 0.008f + jitter(random), jitter(random));
			int lifetime = down ? 30 + random.nextInt(60) : (int) (40 / (random.nextFloat() * 0.8f + 0.2f));
			int age = random.nextInt(lifetime);
			boolean alive = containsWater(world, chunk, position);
			for (int tick = 0; tick < age && alive; tick++) {
				if (down) {
					velocity.x = (velocity.x + 0.6f * (float) Math.cos(tick * 0.08f)) * 0.07f;
					velocity.z = (velocity.z + 0.6f * (float) Math.sin(tick * 0.08f)) * 0.07f;
				} else {
					velocity.y += 0.005f;
				}
				position.add(velocity);
				if (!down)
					velocity.mul(0.85f);
				alive = containsWater(world, chunk, position);
			}
			if (!alive)
				continue;
			int px = (int) Math.floor(position.x);
			int py = (int) Math.floor(position.y);
			int pz = (int) Math.floor(position.z);
			Colour4f light = world.getLightSample(chunk, px, py, pz, LightFace.Top, world.getNightLightAdjustment())
					.toColour(world.getLightStyle(), world.useOldGrayscaleLighting());
			Vector4f colour = new Vector4f(light.r, light.g, light.b, 1);
			Vector3f halfRight = new Vector3f(right).mul(size);
			Vector3f halfUp = new Vector3f(up).mul(size);
			Mesh mesh = geometry.getMesh(sprite.texture, Geometry.MeshType.AlphaTest);
			// Tectonicus treats clockwise faces as front-facing.
			mesh.addVertex(new Vector3f(position).sub(halfRight).add(halfUp), colour, sprite.u0, sprite.v0);
			mesh.addVertex(new Vector3f(position).add(halfRight).add(halfUp), colour, sprite.u1, sprite.v0);
			mesh.addVertex(new Vector3f(position).add(halfRight).sub(halfUp), colour, sprite.u1, sprite.v1);
			mesh.addVertex(new Vector3f(position).sub(halfRight).sub(halfUp), colour, sprite.u0, sprite.v1);
		}
	}

	private static boolean containsWater(BlockContext world, RawChunk chunk, Vector3f position) {
		int x = (int) Math.floor(position.x);
		int y = (int) Math.floor(position.y);
		int z = (int) Math.floor(position.z);
		if (y < 0 || y >= Minecraft.getChunkHeight())
			return false;
		BlockType type = world.getBlockType(chunk, x, y, z);
		if (type.isWater())
			return true;
		BlockStateWrapper block = world.getBlock(chunk, x, y, z);
		return Water.containsWater(block == null ? type.getName() : block.getBlockName(),
				world.getBlockState(chunk.getChunkCoord(), x, y, z), block);
	}

	private static float jitter(Random random) {
		return (random.nextFloat() * 2 - 1) * 0.02f;
	}

	private static long positionSeed(long x, int y, long z) {
		long seed = 0x9E3779B97F4A7C15L;
		seed = (seed ^ x) * 0xBF58476D1CE4E5B9L;
		seed = (seed ^ y) * 0x94D049BB133111EBL;
		seed = (seed ^ z) * 0x9E3779B97F4A7C15L;
		return seed ^ (seed >>> 31);
	}
}
