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
import tectonicus.blockregistry.BlockState;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

import java.util.List;
import java.util.Random;

/** Seeded static appearances for torches, lit candles and powered redstone circuits. */
public final class FlameParticles {
	private static final long SMOKE_SEED = 0x6A09E667F3BCC909L;
	private static final String[] HORIZONTAL_DIRECTIONS = {"north", "east", "south", "west"};
	// Predefined appearances approximate vanilla's range without simulating a particle's lifetime.
	// Offsets and sizes are in blocks; size is the billboard's half-width.
	private static final Appearance[] FLAMES = {
		new Appearance(-0.018f, 0.000f,  0.012f, 0.180f, 0, 0.05f, 1),
		new Appearance( 0.010f, 0.006f, -0.020f, 0.160f, 0, 0.15f, 1),
		new Appearance(-0.006f, 0.010f, -0.010f, 0.150f, 0, 0.25f, 1),
		new Appearance( 0.020f, 0.020f,  0.008f, 0.140f, 0, 0.40f, 1),
		new Appearance(-0.025f, 0.030f,  0.018f, 0.130f, 0, 0.55f, 1),
		new Appearance( 0.012f, 0.035f, -0.006f, 0.115f, 0, 0.65f, 1),
		new Appearance(-0.012f, 0.045f, -0.022f, 0.100f, 0, 0.80f, 1),
		new Appearance( 0.022f, 0.055f,  0.014f, 0.085f, 0, 0.90f, 1)
	};
	// Higher puffs use more dispersed sprites. Keep height, size and sprite stage together.
	private static final Appearance[] SMOKE = {
		new Appearance( 0.000f, 0.015f,  0.000f, 0.045f, 0, 0, 0.22f),
		new Appearance(-0.015f, 0.050f,  0.010f, 0.070f, 1, 0, 0.20f),
		new Appearance( 0.025f, 0.100f, -0.020f, 0.090f, 2, 0, 0.18f),
		new Appearance(-0.040f, 0.200f, -0.015f, 0.105f, 3, 0, 0.16f),
		new Appearance( 0.060f, 0.360f,  0.035f, 0.120f, 4, 0, 0.14f),
		new Appearance(-0.085f, 0.580f,  0.050f, 0.135f, 5, 0, 0.12f),
		new Appearance( 0.110f, 0.850f, -0.070f, 0.140f, 6, 0, 0.10f),
		new Appearance(-0.140f, 1.200f, -0.090f, 0.140f, 7, 0, 0.08f)
	};
	private static final Appearance[] DUST = {
		new Appearance(-0.060f, 0.010f,  0.030f, 0.070f, 0, 0, 0.80f),
		new Appearance( 0.040f, 0.030f, -0.050f, 0.090f, 1, 0, 0.72f),
		new Appearance(-0.030f, 0.060f, -0.020f, 0.100f, 2, 0, 0.88f),
		new Appearance( 0.070f, 0.090f,  0.040f, 0.110f, 3, 0, 0.65f),
		new Appearance(-0.090f, 0.120f,  0.060f, 0.120f, 4, 0, 0.76f),
		new Appearance( 0.050f, 0.160f, -0.080f, 0.105f, 5, 0, 0.70f),
		new Appearance(-0.110f, 0.210f, -0.040f, 0.090f, 6, 0, 0.82f),
		new Appearance( 0.100f, 0.260f,  0.070f, 0.075f, 7, 0, 0.60f)
	};
	// Vanilla CandleBlock particle offsets in model pixels, including the space above the wick.
	private static final int[][][] CANDLE_OFFSETS = {
		{{8, 8, 8}},
		{{6, 7, 8}, {10, 8, 7}},
		{{8, 5, 10}, {6, 7, 8}, {9, 8, 7}},
		{{7, 5, 9}, {10, 7, 9}, {6, 7, 6}, {9, 8, 6}}
	};

	private FlameParticles() {}

	public static void addGeometry(int x, int y, int z, String blockName, BlockProperties properties,
			BlockContext world, RawChunk chunk, Geometry geometry) {
		String particle = particleId(blockName, properties);
		if (particle == null)
			return;
		addGeometry(x, y, z, blockName, properties, world, chunk, geometry,
				world.getTexturePack().findParticleTextures(particle), particle.equals("minecraft:dust") ? List.of()
						: world.getTexturePack().findParticleTextures("minecraft:smoke"));
	}

	static void addGeometry(int x, int y, int z, String blockName, BlockProperties properties,
			BlockContext world, RawChunk chunk, Geometry geometry, List<SubTexture> sprites, List<SubTexture> smoke) {
		String particle = particleId(blockName, properties);
		if (particle == null)
			return;
		Random random = positionRandom(x, y, z, chunk);
		boolean dust = particle.equals("minecraft:dust");
		if (blockName.endsWith("candle_cake")) {
			addWick(new Vector3f(x + 0.5f, y + 1, z + 0.5f), true, false, sprites, smoke, random.nextLong(), world, chunk, geometry);
		} else if (blockName.endsWith("candle")) {
			for (int[] offset : CANDLE_OFFSETS[candleCount(properties) - 1])
				addWick(new Vector3f(x + offset[0] / 16f, y + offset[1] / 16f, z + offset[2] / 16f),
						true, false, sprites, smoke, random.nextLong(), world, chunk, geometry);
		} else if (blockName.equals("minecraft:repeater")) {
			int delay = Math.clamp(integer(properties, "delay", 1), 1, 4);
			float offset = (random.nextBoolean() ? delay * 2 - 1 : -5) / 16f;
			String facing = properties == null ? "north" : properties.getProperties().getOrDefault("facing", "north");
			addWick(along(new Vector3f(x + 0.5f, y + 0.4f, z + 0.5f), facing, offset),
					false, true, sprites, List.of(), random.nextLong(), world, chunk, geometry);
		} else {
			Vector3f origin = new Vector3f(x + 0.5f, y + 0.7f, z + 0.5f);
			if (blockName.endsWith("wall_torch")) {
				origin.y += 0.22f;
				String facing = properties == null ? "north" : properties.getProperties().getOrDefault("facing", "north");
				switch (facing) {
					case "east" -> origin.x -= 0.27f;
					case "west" -> origin.x += 0.27f;
					case "south" -> origin.z -= 0.27f;
					case "north" -> origin.z += 0.27f;
				}
			}
			addWick(origin, false, dust, sprites, smoke, random.nextLong(), world, chunk, geometry);
		}
	}

	public static void addLegacyCircuitGeometry(int x, int y, int z, int blockId, int data, BlockContext world, RawChunk chunk, Geometry geometry) {
		if (blockId != 94)
			return;
		addLegacyCircuitGeometry(x, y, z, blockId, data, world, chunk, geometry,
				world.getTexturePack().findParticleTextures("minecraft:dust"));
	}

	static void addLegacyCircuitGeometry(int x, int y, int z, int blockId, int data, BlockContext world, RawChunk chunk,
			Geometry geometry, List<SubTexture> sprites) {
		Random random = positionRandom(x, y, z, chunk);
		if (blockId == 94) {
			// Follow the legacy renderer's torch spacing, height and rotation.
			float offset = (random.nextBoolean() ? ((data >> 2) & 3) * 2 - 1 : -5) / 16f;
			String facing = HORIZONTAL_DIRECTIONS[((data & 3) + 2) & 3];
			addWick(along(new Vector3f(x + 0.5f, y + 0.7f, z + 0.5f), facing, offset),
					false, true, sprites, List.of(), random.nextLong(), world, chunk, geometry);
		}
	}

	public static void addLegacyTorchGeometry(int x, int y, int z, int blockId, int data, BlockContext world, RawChunk chunk, Geometry geometry) {
		if (blockId != 50 && blockId != 76)
			return;
		boolean dust = blockId == 76;
		// The legacy Torch renderer keeps the tip centered and raises wall torches by 0.4 blocks.
		Vector3f origin = new Vector3f(x + 0.5f, y + 0.7f + (data >= 1 && data <= 4 ? 0.4f : 0), z + 0.5f);
		addWick(origin, false, dust, world.getTexturePack().findParticleTextures(dust ? "minecraft:dust" : "minecraft:flame"),
				dust ? List.of() : world.getTexturePack().findParticleTextures("minecraft:smoke"),
				positionRandom(x, y, z, chunk).nextLong(), world, chunk, geometry);
	}

	private static Random positionRandom(int x, int y, int z, RawChunk chunk) {
		int worldX = (int) (chunk.getChunkCoord().x * RawChunk.WIDTH + x);
		int worldZ = (int) (chunk.getChunkCoord().z * RawChunk.DEPTH + z);
		return BlockState.createPositionRandom(worldX, chunk.getWorldY(y), worldZ);
	}

	static String particleId(String blockName, BlockProperties properties) {
		return switch (blockName) {
			case "minecraft:torch", "minecraft:wall_torch" -> "minecraft:flame";
			case "minecraft:soul_torch", "minecraft:soul_wall_torch" -> "minecraft:soul_fire_flame";
			case "minecraft:copper_torch", "minecraft:copper_wall_torch" -> "minecraft:copper_fire_flame";
			case "minecraft:redstone_torch", "minecraft:redstone_wall_torch" -> properties == null
					|| !"false".equals(properties.get("lit")) ? "minecraft:dust" : null;
			case "minecraft:repeater" -> properties != null && "true".equals(properties.get("powered")) ? "minecraft:dust" : null;
			default -> blockName.startsWith("minecraft:")
					&& (blockName.endsWith("candle") || blockName.endsWith("candle_cake"))
					&& properties != null && "true".equals(properties.get("lit"))
					&& !"true".equals(properties.get("waterlogged")) ? "minecraft:small_flame" : null;
		};
	}

	private static int integer(BlockProperties properties, String key, int fallback) {
		try {
			return properties == null || properties.get(key) == null ? fallback : Integer.parseInt(properties.get(key));
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static Vector3f along(Vector3f position, String direction, float distance) {
		switch (direction) {
			case "north" -> position.z -= distance;
			case "east" -> position.x += distance;
			case "south" -> position.z += distance;
			case "west" -> position.x -= distance;
		}
		return position;
	}

	private static int candleCount(BlockProperties properties) {
		String count = properties.get("candles");
		return switch (count == null ? "1" : count) {
			case "2" -> 2;
			case "3" -> 3;
			case "4" -> 4;
			default -> 1;
		};
	}

	private static void addWick(Vector3f origin, boolean small, boolean dust, List<SubTexture> sprites, List<SubTexture> smoke,
			long seed, BlockContext world, RawChunk chunk, Geometry geometry) {
		// Separate streams keep flames unchanged when smoke or resource-pack sprites are missing.
		if (!sprites.isEmpty()) {
			Random random = new Random(seed);
			Appearance[] samples = dust ? DUST : FLAMES;
			Appearance appearance = samples[random.nextInt(samples.length)];
			SubTexture sprite = dust ? stagedSprite(sprites, appearance) : sprites.get(random.nextInt(sprites.size()));
			place(origin, appearance, small, dust, dust, sprite, world, chunk, geometry);
		}
		if (!dust && !smoke.isEmpty()) {
			Random random = new Random(seed ^ SMOKE_SEED);
			if (!small || random.nextFloat() < 0.3f) {
				Appearance appearance = SMOKE[random.nextInt(SMOKE.length)];
				place(origin, appearance, false, false, true, stagedSprite(smoke, appearance), world, chunk, geometry);
			}
		}
	}

	private static SubTexture stagedSprite(List<SubTexture> sprites, Appearance appearance) {
		return sprites.get(appearance.spriteStage() * (sprites.size() - 1) / 7);
	}

	private static void place(Vector3f origin, Appearance appearance, boolean small, boolean dust, boolean checkObstructions,
			SubTexture sprite, BlockContext world, RawChunk chunk, Geometry geometry) {
		Vector3f position = new Vector3f(origin).add(appearance.dx(), appearance.dy(), appearance.dz());
		if (checkObstructions && !clearPath(origin, position, world, chunk))
			return;
		emit(position, appearance, small, dust, sprite, world, chunk, geometry);
	}

	private static boolean clearPath(Vector3f origin, Vector3f position, BlockContext world, RawChunk chunk) {
		// Check the few blocks between wick and puff, rather than simulating collision each tick.
		for (int y = (int) Math.floor(origin.y); y <= (int) Math.floor(position.y); y++) {
			if (!SulfurSpringParticles.passable((int) Math.floor(origin.x), y, (int) Math.floor(origin.z), world, chunk)
					|| !SulfurSpringParticles.passable((int) Math.floor(position.x), y, (int) Math.floor(position.z), world, chunk))
				return false;
		}
		return true;
	}

	private static void emit(Vector3f position, Appearance appearance, boolean small, boolean dust, SubTexture sprite, BlockContext world, RawChunk chunk, Geometry geometry) {
		float size = appearance.size() * (small ? 0.5f : 1);
		LightSample sample = world.getLightSample(chunk, (int) Math.floor(position.x), (int) Math.floor(position.y),
				(int) Math.floor(position.z), LightFace.Top, world.getNightLightAdjustment());
		Colour4f light = new LightSample(sample.sky(), Math.min(1, sample.block() + appearance.emission()),
				sample.ambient(), sample.netherColour()).toColour(world.getLightStyle(), world.useOldGrayscaleLighting());
		if (sample.netherColour() != null && appearance.emission() > 0)
			light = new Colour4f(Math.min(1, light.r + appearance.emission()), Math.min(1, light.g + appearance.emission()),
					Math.min(1, light.b + appearance.emission()), 1);
		Vector4f colour = new Vector4f(light.r * appearance.brightness(),
				dust ? 0 : light.g * appearance.brightness(), dust ? 0 : light.b * appearance.brightness(), 1);
		float angle = world.getCameraAngleRad(), elevation = world.getCameraElevationRad();
		Vector3f right = new Vector3f((float) Math.sin(angle), 0, -(float) Math.cos(angle)).mul(size);
		Vector3f up = new Vector3f(-(float) (Math.cos(angle) * Math.sin(elevation)), (float) Math.cos(elevation),
				-(float) (Math.sin(angle) * Math.sin(elevation))).mul(size);
		Mesh mesh = geometry.getMesh(sprite.texture, Geometry.MeshType.AlphaTest);
		mesh.addVertex(new Vector3f(position).sub(right).add(up), colour, sprite.u0, sprite.v0);
		mesh.addVertex(new Vector3f(position).add(right).add(up), colour, sprite.u1, sprite.v0);
		mesh.addVertex(new Vector3f(position).add(right).sub(up), colour, sprite.u1, sprite.v1);
		mesh.addVertex(new Vector3f(position).sub(right).sub(up), colour, sprite.u0, sprite.v1);
	}

	private record Appearance(float dx, float dy, float dz, float size, int spriteStage, float emission, float brightness) {}
}
