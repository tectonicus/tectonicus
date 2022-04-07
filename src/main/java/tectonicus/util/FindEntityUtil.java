/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import lombok.experimental.UtilityClass;
import tectonicus.Block;
import tectonicus.BlockIds;
import tectonicus.Minecraft;
import tectonicus.Portal;
import tectonicus.WorldStats;
import tectonicus.cache.swap.HddObjectListWriter;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.ChestFilter;
import tectonicus.configuration.PortalFilter;
import tectonicus.configuration.SignFilter;
import tectonicus.configuration.ViewFilter;
import tectonicus.raw.ContainerEntity;
import tectonicus.raw.RawChunk;
import tectonicus.raw.SignEntity;
import tectonicus.world.Sign;

import java.util.List;

@UtilityClass
public class FindEntityUtil {
	public static void findSigns(RawChunk chunk, HddObjectListWriter<Sign> signs, SignFilter filter) {
		try {
			for (SignEntity s : chunk.getSigns().values()) {
				if (filter.passesFilter(s)) {
					Sign sign = new Sign(s);
					signs.add(sign);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void findPortalsOld(RawChunk chunk, HddObjectListWriter<Portal> portals, PortalFilter filter, WorldStats stats) {
		try {
			for (int x = 0; x < RawChunk.WIDTH; x++) {
				for (int y = 1; y < Minecraft.getChunkHeight() - 1; y++) {
					for (int z = 0; z < RawChunk.DEPTH; z++) {
						final int id = chunk.getBlockId(x, y, z);
						final int above = chunk.getBlockId(x, y + 1, z);
						int below = chunk.getBlockId(x, y - 1, z);

						if (id == BlockIds.PORTAL && above != BlockIds.PORTAL) //Find vertical center portal blocks
						{
							ChunkCoord coord = chunk.getChunkCoord();

							int tempY = y;
							while (below == BlockIds.PORTAL) {
								tempY -= 1;
								below = chunk.getBlockId(x, tempY, z);
							}

							Vector3l pos = new Vector3l(coord.x * RawChunk.WIDTH + x,
									y - Math.round((y - (tempY + 1)) / 2),
									coord.z * RawChunk.DEPTH + z);

							if (filter.passesFilter()) {
								portals.add(new Portal(pos.x, pos.y, pos.z));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void findPortals(RawChunk chunk, HddObjectListWriter<Portal> portals, PortalFilter filter, WorldStats stats) {
		try {
			String netherPortalName = Block.NETHER_PORTAL.getName();

			for (int x = 0; x < RawChunk.WIDTH; x++) {
				for (int y = 1; y < Minecraft.getChunkHeight() - 1; y++) {
					for (int z = 0; z < RawChunk.DEPTH; z++) {
						String id = chunk.getBlockName(x, y, z);
						String above = chunk.getBlockName(x, y + 1, z);
						String below = chunk.getBlockName(x, y - 1, z);

						if (id != null && id.equals(netherPortalName) && !above.equals(netherPortalName)) //Find vertical center portal blocks
						{
							ChunkCoord coord = chunk.getChunkCoord();

							int tempY = y;
							while (below.equals(netherPortalName)) {
								tempY -= 1;
								below = chunk.getBlockName(x, tempY, z);
							}

							// For 1.18 and higher we need to subtract 64 from the y value to get the actual y value
							long finalY;
							if (Minecraft.getChunkHeight() > 256) {
								finalY = (y - (y - (tempY + 1)) / 2) - 64L;
							} else {
								finalY = y - (y - (tempY + 1)) / 2L;
							}

							Vector3l pos = new Vector3l(coord.x * RawChunk.WIDTH + x,
									finalY,
									coord.z * RawChunk.DEPTH + z);

							if (filter.passesFilter()) {
								portals.add(new Portal(pos.x, pos.y, pos.z));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void findViews(RawChunk chunk, HddObjectListWriter<Sign> views, ViewFilter filter) {
		try {
			for (SignEntity s : chunk.getSigns().values()) {
				if (filter.passesFilter(s)) {
					Sign sign = new Sign(s);
					views.add(sign);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void findChests(RawChunk chunk, ChestFilter filter, List<ContainerEntity> chests) {
		try {
			for (ContainerEntity entity : chunk.getChests()) {
				if (filter.passesFilter(entity.isUnopenedContainer())) {
					chests.add(entity);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
