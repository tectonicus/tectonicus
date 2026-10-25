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

/** A stable snapshot of potent sulfur's surface gas and underwater bubbles. */
public final class SulfurSpringParticles {
	private static final int GAS_SAMPLES = 24;
	private static final int BUBBLE_SAMPLES = 8;
	private static final int MAX_WATER_DEPTH = 4;
	private static final float GAS_RADIUS = 3;

	private SulfurSpringParticles() {}

	public static void addGeometry(int x, int y, int z, BlockProperties properties, BlockContext world, RawChunk chunk, Geometry geometry) {
		String state = state(properties);
		if (!active(state) || !sourceWater(x, y + 1, z, world, chunk))
			return;
		List<SubTexture> gas = gasState(state) ? world.getTexturePack().findParticleTextures("minecraft:noxious_gas") : List.of();
		List<SubTexture> bubbles = world.getTexturePack().findParticleTextures("minecraft:sulfur_bubbles");
		addGeometry(x, y, z, properties, world, chunk, geometry, gas, bubbles);
		if (state.equals("continuous") || state.equals("erupting"))
			GeyserParticles.addGeometry(x, y, z, properties, world, chunk, geometry);
	}

	static void addGeometry(int x, int y, int z, BlockProperties properties, BlockContext world, RawChunk chunk,
			Geometry geometry, List<SubTexture> gas, List<SubTexture> bubbles) {
		String state = state(properties);
		if (!active(state) || !sourceWater(x, y + 1, z, world, chunk))
			return;
		long seed = positionSeed(chunk.getChunkCoord().x * RawChunk.WIDTH + x, chunk.getWorldY(y),
				chunk.getChunkCoord().z * RawChunk.DEPTH + z);
		if (!bubbles.isEmpty())
			addBubbles(x, y, z, new Random(seed), world, chunk, geometry, bubbles);
		if (gas.isEmpty() || !gasState(state))
			return;
		int surface = findSurface(x, y, z, world, chunk);
		if (surface < 0)
			return;
		Random random = new Random(seed ^ 0x6A09E667F3BCC909L);
		for (int i = 0; i < GAS_SAMPLES; i++) {
			float dx = random.nextFloat() - 0.5f;
			float dz = random.nextFloat() - 0.5f;
			float radius = random.nextFloat() * GAS_RADIUS;
			float length = (float) Math.sqrt(dx * dx + dz * dz);
			if (length == 0 || radius * radius + 0.25f * 0.25f > GAS_RADIUS * GAS_RADIUS)
				continue;
			Vector3f position = new Vector3f(x + 0.5f + dx / length * radius, surface + 0.25f, z + 0.5f + dz / length * radius);
			int px = floor(position.x);
			int pz = floor(position.z);
			if (!passable(px, surface, pz, world, chunk) || !sourceWater(px, surface - 1, pz, world, chunk)
					|| !clearPath(x + 0.5f, z + 0.5f, position.x, position.z, surface - 1, world, chunk))
				continue;
			int lifetime = (int) (6 / (random.nextFloat() * 0.5f + 0.5f) * 3);
			int age = 1 + random.nextInt(lifetime - 1);
			float size = (0.1f + random.nextFloat() * 0.1f) * 2.25f * Math.min(1, age * 32.0f / lifetime);
			float alpha = age <= lifetime / 2.0f ? 1 : (lifetime - (age - lifetime / 2.0f)) / lifetime;
			Vector3f velocity = new Vector3f(jitter(random, 0.01f), 0.01f + jitter(random, 0.01f), jitter(random, 0.01f));
			for (int tick = 0; tick < age; tick++) {
				velocity.y += 0.0008f;
				Vector3f next = new Vector3f(position).add(velocity);
				if (!passable(floor(next.x), floor(next.y), floor(next.z), world, chunk))
					break;
				position.set(next);
				velocity.mul(0.96f);
			}
			SubTexture sprite = gas.get(age * (gas.size() - 1) / lifetime);
			addParticle(position, size, alpha, sprite, Geometry.MeshType.Transparent, world, chunk, geometry);
		}
	}

	private static void addBubbles(int x, int y, int z, Random random, BlockContext world, RawChunk chunk, Geometry geometry, List<SubTexture> sprites) {
		for (int i = 0; i < BUBBLE_SAMPLES; i++) {
			SubTexture sprite = sprites.get(random.nextInt(sprites.size()));
			Vector3f position = new Vector3f(x + random.nextFloat(), y + 1 + random.nextFloat(), z + random.nextFloat());
			float startY = position.y;
			float sizeStart = 0.02f + random.nextFloat() * 0.02f;
			Vector3f velocity = new Vector3f(jitter(random, 0.02f), 0, jitter(random, 0.02f));
			int age = random.nextInt(300);
			boolean alive = true;
			for (int tick = 0; tick < age && alive; tick++) {
				float previousY = position.y;
				velocity.y += 0.0016f;
				position.add(velocity);
				velocity.mul(0.85f);
				alive = position.y < startY + 3 && position.y > previousY
						&& sourceWater(floor(position.x), floor(position.y), floor(position.z), world, chunk);
				velocity.x += jitter(random, 0.0015f);
				velocity.z += jitter(random, 0.0015f);
				position.add(velocity.x, 0, velocity.z);
				alive &= sourceWater(floor(position.x), floor(position.y), floor(position.z), world, chunk);
			}
			if (alive) {
				float size = sizeStart + (0.15f - sizeStart) * (position.y - startY) / 3;
				addParticle(position, size, 1, sprite, Geometry.MeshType.AlphaTest, world, chunk, geometry);
			}
		}
	}

	static int findSurface(int x, int y, int z, BlockContext world, RawChunk chunk) {
		for (int offset = 1; offset <= MAX_WATER_DEPTH + 1; offset++) {
			int py = y + offset;
			if (!passable(x, py, z, world, chunk))
				return -1;
			if (!sourceWater(x, py, z, world, chunk))
				return py;
		}
		return -1;
	}

	/** The vanilla sight ray stays within the top water layer; traverse its X/Z cells. */
	private static boolean clearPath(float startX, float startZ, float endX, float endZ, int y, BlockContext world, RawChunk chunk) {
		int x = floor(startX), z = floor(startZ);
		int targetX = floor(endX), targetZ = floor(endZ);
		double dx = endX - startX, dz = endZ - startZ;
		int stepX = Double.compare(dx, 0), stepZ = Double.compare(dz, 0);
		double deltaX = dx == 0 ? Double.POSITIVE_INFINITY : 1 / Math.abs(dx);
		double deltaZ = dz == 0 ? Double.POSITIVE_INFINITY : 1 / Math.abs(dz);
		double nextX = dx == 0 ? Double.POSITIVE_INFINITY : (dx > 0 ? x + 1 - startX : startX - x) / Math.abs(dx);
		double nextZ = dz == 0 ? Double.POSITIVE_INFINITY : (dz > 0 ? z + 1 - startZ : startZ - z) / Math.abs(dz);
		while (true) {
			if (!passable(x, y, z, world, chunk))
				return false;
			if (x == targetX && z == targetZ)
				return true;
			if (nextX <= nextZ) {
				x += stepX;
				nextX += deltaX;
			} else {
				z += stepZ;
				nextZ += deltaZ;
			}
		}
	}

	static boolean sourceWater(int x, int y, int z, BlockContext world, RawChunk chunk) {
		if (y < 0 || y >= Minecraft.getChunkHeight())
			return false;
		BlockType type = world.getBlockType(chunk, x, y, z);
		BlockStateWrapper model = world.getBlock(chunk, x, y, z);
		String name = model == null ? type.getName() : model.getBlockName();
		BlockProperties properties = world.getBlockState(chunk.getChunkCoord(), x, y, z);
		if (type.isWater() || name.equals("minecraft:water") || name.equals("minecraft:bubble_column")) {
			String level = properties == null ? null : properties.get("level");
			if (level == null && model != null)
				level = model.getDefaultProperties().get("level");
			return level == null || level.equals("0");
		}
		return Water.containsWater(name, properties, model);
	}

	static boolean passable(int x, int y, int z, BlockContext world, RawChunk chunk) {
		if (y < 0 || y >= Minecraft.getChunkHeight())
			return false;
		BlockType type = world.getBlockType(chunk, x, y, z);
		BlockStateWrapper model = world.getBlock(chunk, x, y, z);
		String name = model == null ? type.getName() : model.getBlockName();
		if (type.isWater() || name.equals("Air") || name.equals("minecraft:air") || name.equals("minecraft:cave_air")
				|| name.equals("minecraft:void_air") || name.equals("minecraft:water") || name.equals("minecraft:bubble_column")
				|| name.equals("minecraft:kelp") || name.equals("minecraft:kelp_plant")
				|| name.equals("minecraft:seagrass") || name.equals("minecraft:tall_seagrass"))
			return true;
		// Approximate collision using the model registry's full-block coverage.
		return !type.isSolid() && model != null && !model.isFullBlock() && !model.isFullOpaqueBlock();
	}

	static void addParticle(Vector3f position, float size, float alpha, SubTexture sprite, Geometry.MeshType type,
			BlockContext world, RawChunk chunk, Geometry geometry) {
		float angle = world.getCameraAngleRad(), elevation = world.getCameraElevationRad();
		Vector3f right = new Vector3f((float) Math.sin(angle), 0, -(float) Math.cos(angle)).mul(size);
		Vector3f up = new Vector3f(-(float) (Math.cos(angle) * Math.sin(elevation)), (float) Math.cos(elevation),
				-(float) (Math.sin(angle) * Math.sin(elevation))).mul(size);
		Colour4f light = world.getLightSample(chunk, floor(position.x), floor(position.y), floor(position.z),
				LightFace.Top, world.getNightLightAdjustment()).toColour(world.getLightStyle(), world.useOldGrayscaleLighting());
		Vector4f colour = new Vector4f(light.r, light.g, light.b, alpha);
		Mesh mesh = geometry.getMesh(sprite.texture, type);
		// Clockwise front faces, with the texture's top edge facing up.
		mesh.addVertex(new Vector3f(position).sub(right).add(up), colour, sprite.u0, sprite.v0);
		mesh.addVertex(new Vector3f(position).add(right).add(up), colour, sprite.u1, sprite.v0);
		mesh.addVertex(new Vector3f(position).add(right).sub(up), colour, sprite.u1, sprite.v1);
		mesh.addVertex(new Vector3f(position).sub(right).sub(up), colour, sprite.u0, sprite.v1);
	}

	private static String state(BlockProperties properties) {
		return properties == null ? "dry" : properties.getProperties().getOrDefault("potent_sulfur_state", "dry");
	}

	private static boolean active(String state) {
		return gasState(state) || state.equals("erupting") || state.equals("continuous");
	}

	private static boolean gasState(String state) {
		return state.equals("wet") || state.equals("dormant");
	}

	private static int floor(float value) { return (int) Math.floor(value); }

	private static float jitter(Random random, float amount) { return (random.nextFloat() * 2 - 1) * amount; }

	static long positionSeed(long x, int y, long z) {
		long seed = (0x9E3779B97F4A7C15L ^ x) * 0xBF58476D1CE4E5B9L;
		seed = (seed ^ y) * 0x94D049BB133111EBL;
		seed = (seed ^ z) * 0x9E3779B97F4A7C15L;
		return seed ^ (seed >>> 31);
	}
}
