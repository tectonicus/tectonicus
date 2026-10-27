/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus;

import org.joml.Vector2d;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tectonicus.raw.RawChunk;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.chunk.ChunkCoord;
import tectonicus.renderer.OrthoCamera;

import static org.junit.jupiter.api.Assertions.*;

class TileRendererPrecisionTest {
	private int previousHeight;

	@BeforeEach
	void setHeight() {
		previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(384);
	}

	@AfterEach
	void restoreHeight() {
		Minecraft.setChunkHeight(previousHeight);
	}

	@ParameterizedTest
	@ValueSource(longs = {-1874999, 1874999})
	void findsTilesWhenProjectedPixelsExceedTheIntegerRange(long chunkCoord) {
		OrthoCamera camera = new OrthoCamera(null, 512, 512);
		camera.lookAtTile(0, 0, 8, (float)Math.toRadians(135), (float)Math.toRadians(45));
		ChunkCoord chunk = new ChunkCoord(chunkCoord, chunkCoord);
		Vector2d center = camera.projectd(chunkCoord * 16 + 8, 64, chunkCoord * 16 + 8);
		assertTrue(Math.abs(center.x) > Integer.MAX_VALUE);
		TileRenderer.TileCoordBounds tiles = TileRenderer.getTileBoundsForChunk(camera, chunk, 512, 512);
		int x = (int)Math.floor(center.x / 512), y = (int)Math.floor(center.y / 512);
		assertTrue(tiles.min.x <= x && tiles.max.x >= x);
		assertTrue(tiles.min.y <= y && tiles.max.y >= y);
		assertTrue(tiles.max.x - tiles.min.x < 10);
		assertTrue(tiles.max.y - tiles.min.y < 100);
	}

	@Test
	void occupiedHeightBoundsReduceFlatWorldCandidatesWithoutDroppingTheirCorners() {
		OrthoCamera camera = new OrthoCamera(null, 512, 512);
		camera.lookAtTile(0, 0, 10, (float)Math.toRadians(225), (float)Math.toRadians(45));
		ChunkCoord chunk = new ChunkCoord(-7, 3);
		RawChunk raw = new RawChunk();
		raw.setBlockName(0, 64, 0, "minecraft:stone");
		int[] height = raw.getRenderHeightBounds();
		var tight = TileRenderer.getTileBoundsForChunk(camera, chunk, 512, 512, height[0], height[1]);
		var full = TileRenderer.getTileBoundsForChunk(camera, chunk, 512, 512);
		assertTrue(tight.max.y - tight.min.y < full.max.y - full.min.y);
		for (int x : new int[] {0, 16}) {
			for (int y : height) {
				for (int z : new int[] {0, 16}) {
					Vector2d point = camera.projectd(chunk.x * 16 + x, y, chunk.z * 16 + z);
					assertTrue(tight.min.x <= Math.floor(point.x / 512) && tight.max.x >= Math.floor(point.x / 512));
					assertTrue(tight.min.y <= Math.floor(point.y / 512) && tight.max.y >= Math.floor(point.y / 512));
				}
			}
		}
	}
}
