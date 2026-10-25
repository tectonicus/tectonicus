/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;
import tectonicus.BlockContext;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import java.util.List;
import java.util.Random;

/** A deterministic snapshot of the spray from a continuous or currently erupting geyser. */
public final class GeyserParticles {
	// Average live populations: one base and two poof particles per tick, lasting about 22 ticks.
	private static final int BASE_SAMPLES = 22;
	private static final int POOF_SAMPLES = 44;

	private GeyserParticles() {}

	public static void addGeometry(int x, int y, int z, BlockProperties properties, BlockContext world, RawChunk chunk, Geometry geometry) {
		if (!active(properties) || !SulfurSpringParticles.sourceWater(x, y + 1, z, world, chunk))
			return;
		addGeometry(x, y, z, properties, world, chunk, geometry,
				world.getTexturePack().findParticleTextures("minecraft:geyser_base"),
				world.getTexturePack().findParticleTextures("minecraft:geyser_poof"),
				world.getTexturePack().findParticleTextures("minecraft:geyser_plume"));
	}

	static void addGeometry(int x, int y, int z, BlockProperties properties, BlockContext world, RawChunk chunk,
			Geometry geometry, List<SubTexture> base, List<SubTexture> poof, List<SubTexture> plume) {
		if (!active(properties) || !SulfurSpringParticles.sourceWater(x, y + 1, z, world, chunk))
			return;
		int surface = SulfurSpringParticles.findSurface(x, y, z, world, chunk);
		int depth = surface - y - 1;
		if (surface < 0 || depth < 1 || depth > 4)
			return;
		long seed = SulfurSpringParticles.positionSeed(chunk.getChunkCoord().x * RawChunk.WIDTH + x,
				chunk.getWorldY(y), chunk.getChunkCoord().z * RawChunk.DEPTH + z);
		addPuffs(x, surface, z, depth, 1.5f, BASE_SAMPLES, base, new Random(seed ^ 0x510E527FADE682D1L), world, chunk, geometry);
		addPuffs(x, surface, z, depth, 2, POOF_SAMPLES, poof, new Random(seed ^ 0x9B05688C2B3E6C1FL), world, chunk, geometry);
		if (!plume.isEmpty())
			addPlume(x, surface, z, depth, plume, new Random(seed ^ 0x1F83D9ABFB41BD6BL), world, chunk, geometry);
	}

	private static void addPuffs(int x, int surface, int z, int depth, float burst, int count, List<SubTexture> sprites,
			Random random, BlockContext world, RawChunk chunk, Geometry geometry) {
		if (sprites.isEmpty())
			return;
		for (int i = 0; i < count; i++) {
			Vector3f position = new Vector3f(x + 0.5f + jitter(random, 0.25f), surface + 0.2f + jitter(random, 0.25f),
					z + 0.5f + jitter(random, 0.25f));
			Vector3f velocity = initialVelocity(random).mul(burst + 0.25f * depth);
			velocity.y = Math.abs(velocity.y);
			int lifetime = (int) (25 * (0.8f + 0.2f * random.nextFloat()));
			int age = 1 + random.nextInt(lifetime - 1);
			float size = (0.1f + 0.1f * random.nextFloat()) * 0.75f * (3 + 0.125f * depth)
					* Math.min(1, age * 32.0f / lifetime);
			for (int tick = 0; tick < age; tick++) {
				Vector3f next = new Vector3f(position).add(velocity);
				if (!clearMotion(position, next, world, chunk))
					break;
				position.set(next);
				velocity.mul(0.725f);
			}
			emit(position, size, sprites, age, lifetime, world, chunk, geometry);
		}
	}

	private static void addPlume(int x, int surface, int z, int depth, List<SubTexture> sprites, Random random,
			BlockContext world, RawChunk chunk, Geometry geometry) {
		int height = 5 * depth;
		int emissionRate = depth + 2;
		int initialLifetime = height * 5;
		int candidates = emissionRate * initialLifetime;
		float propulsion = height * 1.45f * (depth == 1 ? 1.5f : 1);
		for (int i = 0; i < candidates; i++) {
			Vector3f position = new Vector3f(x + 0.5f + jitter(random, 0.1f), surface + random.nextFloat(), z + 0.5f + jitter(random, 0.1f));
			float startY = position.y;
			Vector3f velocity = initialVelocity(random);
			velocity.y = 0;
			float sprayX = jitter(random, 0.1f), sprayZ = jitter(random, 0.1f);
			float baseSize = (0.1f + 0.1f * random.nextFloat()) * 0.75f;
			float minSize = baseSize * (2 + height / 8.0f);
			float maxSize = baseSize * (3 + height / 8.0f);
			int lifetime = initialLifetime;
			// Sample every emission tick so the static column retains vanilla's continuous density.
			int age = i / emissionRate;
			float gravity = -propulsion, friction = 1, progress = 0;
			boolean done = false;
			for (int tick = 1; tick <= age && tick < lifetime; tick++) {
				float previousY = position.y;
				velocity.y -= 0.04f * gravity;
				Vector3f next = new Vector3f(position).add(velocity);
				if (clearMotion(position, next, world, chunk))
					position.set(next);
				else
					velocity.y = 0;
				velocity.mul(friction);
				if (!done && (velocity.y < 0 || position.y > startY + height - 1 || position.y == previousY)) {
					lifetime = Math.min(lifetime, tick + 5);
					friction = 0;
					done = true;
				}
				progress = Math.clamp((position.y - startY) / (height - 1), 0, 1);
				gravity = propulsion * progress * progress * progress * 0.12f;
				velocity.x = progress * sprayX;
				velocity.z = progress * sprayZ;
			}
			if (age < lifetime)
				emit(position, minSize + progress * (maxSize - minSize), sprites, age, lifetime, world, chunk, geometry);
		}
	}

	private static void emit(Vector3f position, float size, List<SubTexture> sprites, int age, int lifetime,
			BlockContext world, RawChunk chunk, Geometry geometry) {
		// Keep the entire billboard below full-block ceilings and outside walls.
		float angle = world.getCameraAngleRad(), elevation = world.getCameraElevationRad();
		Vector3f right = new Vector3f((float) Math.sin(angle), 0, -(float) Math.cos(angle)).mul(size);
		Vector3f up = new Vector3f(-(float) (Math.cos(angle) * Math.sin(elevation)), (float) Math.cos(elevation),
				-(float) (Math.sin(angle) * Math.sin(elevation))).mul(size);
		for (int r : new int[]{-1, 1})
			for (int u : new int[]{-1, 1}) {
				Vector3f corner = new Vector3f(position).fma(r, right).fma(u, up);
				if (!SulfurSpringParticles.passable(floor(corner.x), floor(corner.y), floor(corner.z), world, chunk))
					return;
			}
		SubTexture sprite = sprites.get(Math.min(sprites.size() - 1, age * (sprites.size() - 1) / lifetime));
		SulfurSpringParticles.addParticle(position, size, 1, sprite, Geometry.MeshType.AlphaTest, world, chunk, geometry);
	}

	private static boolean clearMotion(Vector3f from, Vector3f to, BlockContext world, RawChunk chunk) {
		int steps = Math.max(1, (int) Math.ceil(from.distance(to) * 8));
		for (int i = 0; i <= steps; i++) {
			Vector3f point = new Vector3f(from).lerp(to, (float) i / steps);
			if (!SulfurSpringParticles.passable(floor(point.x), floor(point.y), floor(point.z), world, chunk))
				return false;
		}
		return true;
	}

	private static Vector3f initialVelocity(Random random) {
		Vector3f velocity = new Vector3f(jitter(random, 0.4f), jitter(random, 0.4f), jitter(random, 0.4f));
		if (velocity.lengthSquared() > 0)
			velocity.normalize().mul((random.nextFloat() + random.nextFloat() + 1) * 0.15f * 0.4f);
		velocity.y += 0.1f;
		return velocity;
	}

	private static boolean active(BlockProperties properties) {
		String state = properties == null ? null : properties.get("potent_sulfur_state");
		return "continuous".equals(state) || "erupting".equals(state);
	}

	private static int floor(float value) { return (int) Math.floor(value); }

	private static float jitter(Random random, float amount) { return (random.nextFloat() * 2 - 1) * amount; }
}
