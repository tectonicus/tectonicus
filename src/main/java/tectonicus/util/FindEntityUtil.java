/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
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
import tectonicus.cache.swap.HddObjectListWriter;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.filter.BeaconFilter;
import tectonicus.configuration.filter.ChestFilter;
import tectonicus.configuration.filter.PortalFilter;
import tectonicus.configuration.filter.SignFilter;
import tectonicus.configuration.filter.ViewFilter;
import tectonicus.raw.BeaconEntity;
import tectonicus.raw.BedEntity;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.ContainerEntity;
import tectonicus.raw.RawChunk;
import tectonicus.raw.SignEntity;
import tectonicus.world.Colors;
import tectonicus.world.Sign;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@UtilityClass
public class FindEntityUtil {
	public static void findSigns(RawChunk chunk, HddObjectListWriter<Sign> signs, SignFilter filter) {
                for (SignEntity s : chunk.getSigns().values()) {
                        if (filter.passesFilter(s)) {
                                Sign sign = new Sign(s);
                                signs.add(sign);
                        }
                }
	}

	public static void findPortals(RawChunk chunk, HddObjectListWriter<Portal> portals, PortalFilter filter) {
		for (int x = 0; x < RawChunk.WIDTH; x++) {
			for (int y = 1; y < Minecraft.getChunkHeight() - 1; y++) {
				for (int z = 0; z < RawChunk.DEPTH; z++) {
					boolean isPortal;
					boolean aboveIsPortal;
					int tempY = y;

					// Try using block name first (MC >= 1.13)
					String id = chunk.getBlockName(x, y, z);
					if (id != null) {
						String netherPortalName = Block.NETHER_PORTAL.getName();
						String above = chunk.getBlockName(x, y + 1, z);
						isPortal = id.equals(netherPortalName);
						aboveIsPortal = (above != null && above.equals(netherPortalName));

						if (isPortal && !aboveIsPortal) {
							String below = chunk.getBlockName(x, tempY - 1, z);
							while (below != null && below.equals(netherPortalName)) {
								tempY -= 1;
								below = chunk.getBlockName(x, tempY - 1, z);
							}
						}
					} else {
						// Fall back to block ID (MC < 1.13)
						final int idInt = chunk.getBlockId(x, y, z);
						final int above = chunk.getBlockId(x, y + 1, z);
						isPortal = (idInt == BlockIds.PORTAL);
						aboveIsPortal = (above == BlockIds.PORTAL);

						if (isPortal && !aboveIsPortal) {
							int below = chunk.getBlockId(x, tempY - 1, z);
							while (below == BlockIds.PORTAL) {
								tempY -= 1;
								below = chunk.getBlockId(x, tempY - 1, z);
							}
						}
					}

					if (isPortal && !aboveIsPortal) { //Find vertical center portal blocks
						ChunkCoord coord = chunk.getChunkCoord();

						// Calculate Y position
						long finalY = (y + tempY + 1) / 2L;

						// For 1.18 and higher we need to subtract 64 from the y value to get the actual y value
						if (Minecraft.getChunkHeight() > 256) {
							finalY -= 64L;
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
	}

	public static void findViews(RawChunk chunk, HddObjectListWriter<Sign> views, ViewFilter filter) {
                for (SignEntity s : chunk.getSigns().values()) {
                        if (filter.passesFilter(s)) {
                                Sign sign = new Sign(s);
                                views.add(sign);
                        }
                }
	}

	public static void findChests(RawChunk chunk, ChestFilter filter, ConcurrentLinkedQueue<ContainerEntity> chests) {
                for (ContainerEntity entity : chunk.getChests()) {
                        if (filter.passesFilter(entity.isUnopenedContainer())) {
                                chests.add(entity);
                        }
                }
	}

	public static void findBeds(RawChunk chunk, Queue<BedEntity> beds) {
		for (int y = 0; y < Minecraft.getChunkHeight(); y++) {
			for (int x = 0; x < RawChunk.WIDTH; x++) {
				for (int z = 0; z < RawChunk.DEPTH; z++) {
					final String blockName = chunk.getBlockName(x, y, z);
					if (blockName != null && blockName.contains("_bed")) {
						final BlockProperties blockProperties = chunk.getBlockState(x, y, z);
						if (blockProperties.containsKey("part") && blockProperties.get("part").equals("head")) {
							ChunkCoord coord = chunk.getChunkCoord();
							beds.add(new BedEntity((int) (coord.x * RawChunk.WIDTH + x), (y - 64), (int) (coord.z * RawChunk.DEPTH + z), x, y, z,
									Colors.byName(blockName.replace("minecraft:", "").replace("_bed", "")).getId()));
						}
					}
				}
			}
		}
	}
	
	public static void findBeacons(RawChunk chunk, Queue<BeaconEntity> beacons, BeaconFilter filter) {
		for (BeaconEntity beaconEntity: chunk.getBeacons().values()) {
			if (filter.passesFilter(beaconEntity.getLevels()>0)) {
				beacons.add(beaconEntity);
			}
		}
	}
}
