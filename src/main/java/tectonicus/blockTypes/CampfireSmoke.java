/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.raw.RawChunk;
import tectonicus.rasteriser.SubMesh;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import java.util.Random;

/**
 * Adds a stable, camera-facing approximation of the smoke particles emitted by a lit campfire.
 */
public final class CampfireSmoke {
	private static final int TEXTURE_COUNT = 12;
	private static final int BLOCK_LEVEL_COUNT = 9;
	private static final int MIN_PARTICLES_PER_BLOCK = 3;
	private static final int MAX_PARTICLES_PER_BLOCK = 6;
	private static final float COLUMN_HEIGHT = 9.0f;
	private static final float FADE_START = 5.0f;
	private static final float PARTICLE_BASE_Y = 0.35f;
	private static final float MAX_OPACITY = 0.75f;
	
	private CampfireSmoke() {
	}
	
	public static void addGeometry(int x, int y, int z, BlockContext world, RawChunk rawChunk, Geometry geometry) {
		long worldX = rawChunk.getChunkCoord().x * RawChunk.WIDTH + x;
		int worldY = rawChunk.getWorldY(y);
		long worldZ = rawChunk.getChunkCoord().z * RawChunk.DEPTH + z;
		Random random = new Random(positionSeed(worldX, worldY, worldZ));
		
		float angle = world.getCameraAngleRad();
		float elevation = Math.max(0, Math.min((float) Math.PI / 2.0f, world.getCameraElevationRad()));
		float sinAngle = (float) Math.sin(angle);
		float cosAngle = (float) Math.cos(angle);
		float sinElevation = (float) Math.sin(elevation);
		float cosElevation = (float) Math.cos(elevation);
		Vector3f right = new Vector3f(sinAngle, 0, -cosAngle);
		Vector3f up = new Vector3f(-cosAngle * sinElevation, cosElevation, -sinAngle * sinElevation);
		
		for (int blockLevel = 0; blockLevel < BLOCK_LEVEL_COUNT; blockLevel++) {
			int particleCount = MIN_PARTICLES_PER_BLOCK
				+ random.nextInt(MAX_PARTICLES_PER_BLOCK - MIN_PARTICLES_PER_BLOCK + 1);
			for (int i = 0; i < particleCount; i++) {
				SubTexture texture = world.getTexturePack().findTexture(
					"assets/minecraft/textures/particle/big_smoke_" + random.nextInt(TEXTURE_COUNT) + ".png");
				if (texture == null || texture.texture == null)
					continue;
				
				// Each block contributes 3-6 puffs, but their vertical positions can
				// land anywhere in the full nine-block column rather than one block band.
				float height = PARTICLE_BASE_Y + random.nextFloat() * (COLUMN_HEIGHT - PARTICLE_BASE_Y);
				float width = 0.65f + random.nextFloat() * 0.65f;
				float centerX = x + random.nextFloat();
				float centerZ = z + random.nextFloat();
				float fade = height <= FADE_START ? 1.0f : 1.0f - (height - FADE_START) / (COLUMN_HEIGHT - FADE_START);
				float alpha = MAX_OPACITY * fade;
				if (alpha <= 0)
					continue;
				
				Vector3f center = new Vector3f(centerX, y + height, centerZ);
				Vector3f halfRight = new Vector3f(right).mul(width * 0.5f);
				Vector3f halfUp = new Vector3f(up).mul(width * 0.5f);
				Vector3f p0 = new Vector3f(center).sub(halfRight).sub(halfUp);
				Vector3f p1 = new Vector3f(center).add(halfRight).sub(halfUp);
				Vector3f p2 = new Vector3f(center).add(halfRight).add(halfUp);
				Vector3f p3 = new Vector3f(center).sub(halfRight).add(halfUp);
				
				SubMesh smoke = new SubMesh();
				smoke.addDoubleSidedQuad(p0, p1, p2, p3, new Vector4f(1, 1, 1, alpha), texture);
				smoke.pushTo(geometry.getMesh(texture.texture, Geometry.MeshType.Transparent), 0, 0, 0,
					SubMesh.Rotation.None, 0);
			}
		}
	}
	
	private static long positionSeed(long x, int y, long z) {
		long seed = 0x9E3779B97F4A7C15L;
		seed = (seed ^ x) * 0xBF58476D1CE4E5B9L;
		seed = (seed ^ y) * 0x94D049BB133111EBL;
		seed = (seed ^ z) * 0x9E3779B97F4A7C15L;
		return seed ^ (seed >>> 31);
	}
}
