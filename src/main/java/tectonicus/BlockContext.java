/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import tectonicus.blockregistry.BlockRegistry;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.cache.PlayerSkinCache;
import tectonicus.chunk.ChunkCoord;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.paintingregistry.PaintingRegistry;
import tectonicus.raw.Biome;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

public interface BlockContext {
	default boolean shouldCullSameBlockFaces() { return true; }
	default boolean isLegacyWater() { return true; }

	int getBlockId(ChunkCoord chunkCoord, int x, int y, int z);
	default int getBlockData(ChunkCoord chunkCoord, int x, int y, int z) { return 0; }

	BlockType getBlockType(ChunkCoord chunkCoord, int x, int y, int z);
	BlockType getBlockType(RawChunk rawChunk, int x, int y, int z);
	BlockStateWrapper getBlock(ChunkCoord chunkCoord, int x, int y, int z);
	BlockStateWrapper getBlock(RawChunk rawChunk, int x, int y, int z);
	BlockProperties getBlockState(ChunkCoord chunkCoord, int x, int y, int z);

	float getLight(ChunkCoord chunkCoord, final int x, final int y, final int z, LightFace face);
	float getLight(RawChunk rawChunk, final int x, final int y, final int z, LightFace face);
	float getLight(RawChunk rawChunk, final int x, final int y, final int z, LightFace face, float nightLightAdjustment);
	default LightSample getLightSample(RawChunk rawChunk, int x, int y, int z, LightFace face, float nightLightAdjustment) {
		return LightSample.ambient(getLight(rawChunk, x, y, z, face, nightLightAdjustment));
	}

	LightStyle getLightStyle();
	float getNightLightAdjustment();
	boolean isSmoothLit();
	boolean useOldGrayscaleLighting();

	TexturePack getTexturePack();
	default float getCameraAngleRad() { return (float)Math.PI / 4.0f; }
	default float getCameraElevationRad() { return (float)Math.PI / 4.0f; }

	Biome getBiome(ChunkCoord chunkCoord, int x, int y, int z);

	Colour4f getGrassColor(ChunkCoord chunkCoord, final int x, final int y, final int z);
	Colour4f getFoliageColor(ChunkCoord chunkCoord, final int x, final int y, final int z);
	Colour4f getDryFoliageColor(ChunkCoord chunkCoord, final int x, final int y, final int z);
	Colour4f getWaterColor(RawChunk rawChunk, final int x, final int y, final int z);

	PlayerSkinCache getPlayerSkinCache();

	BlockRegistry getModelRegistry();
	PaintingRegistry getPaintingRegistry();
}
