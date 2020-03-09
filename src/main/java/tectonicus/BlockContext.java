/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import tectonicus.blockTypes.BlockRegistry;
import tectonicus.cache.PlayerSkinCache;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

public interface BlockContext
{
	int getBlockId(ChunkCoord chunkCoord, int x, int y, int z);
	
	BlockType getBlockType(ChunkCoord chunkCoord, int x, int y, int z);
	
	float getLight(ChunkCoord chunkCoord, final int x, final int y, final int z, LightFace face);
	
	LightStyle getLightStyle();
	
	TexturePack getTexturePack();

	int getBiomeId(ChunkCoord chunkCoord, int x, int y, int z);
	
	Colour4f getGrassColour(ChunkCoord chunkCoord, final int x, final int y, final int z);
	Colour4f getWaterColor(ChunkCoord chunkCoord, final int x, final int y, final int z);
	
	PlayerSkinCache getPlayerSkinCache();
	
	BlockRegistry getModelRegistry();
}
