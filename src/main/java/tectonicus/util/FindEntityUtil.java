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
import lombok.extern.slf4j.Slf4j;
import tectonicus.Block;
import tectonicus.BlockIds;
import tectonicus.Minecraft;
import tectonicus.PortalColumn;
import tectonicus.PortalColumn.Axis;
import tectonicus.cache.swap.HddObjectListWriter;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.filter.BeaconFilter;
import tectonicus.configuration.filter.ContainerFilter;
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
import tectonicus.world.subset.WorldSubset;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

@Slf4j
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

	public static void findPortals(RawChunk chunk, HddObjectListWriter<PortalColumn> portals, PortalFilter filter, WorldSubset subset) {
		if (!filter.passesFilter() || !containsPortalBlock(chunk))
			return;
		scanPortalColumns(chunk, subset.visibility(chunk.getChunkCoord()), portals::add);
	}

	/** Complete portal columns, independent of render filters and subsets. */
	public static List<PortalColumn> findPortalColumns(RawChunk chunk) {
		List<PortalColumn> columns = new ArrayList<>();
		if (containsPortalBlock(chunk))
			scanPortalColumns(chunk, (x, y, z) -> true, columns::add);
		return columns;
	}

	/**
	 * Whether the chunk has a nether portal block: by name in 1.13+ chunks, by id in older ones. Almost no chunk has
	 * one, so this is checked before scanning every column.
	 */
	private static boolean containsPortalBlock(RawChunk chunk) {
		return chunk.containsBlock(block -> Block.NETHER_PORTAL.getName().equals(block.getName()))
				|| chunk.containsBlockId(BlockIds.PORTAL);
	}

	private static void scanPortalColumns(RawChunk chunk, RawChunk.BlockVisibility visibility, Consumer<PortalColumn> columns) {
		ChunkCoord coord = chunk.getChunkCoord();
		int height = Minecraft.getChunkHeight();
		int worldMinY = chunk.getWorldY(0);
		for (int x = 0; x < RawChunk.WIDTH; x++) {
			for (int z = 0; z < RawChunk.DEPTH; z++) {
				Axis runAxis = null;
				int runStart = 0;
				for (int y = 0; y <= height; y++) {
					int worldY = worldMinY + y;
					Axis axis = y < height ? portalAxis(chunk, x, y, z) : null;
					if (axis != null && !visibility.contains(x, worldY, z))
						axis = null;
					if (axis == Axis.UNKNOWN && runAxis != null)
						axis = runAxis;
					if (runAxis == Axis.UNKNOWN && axis != null)
						runAxis = axis;
					if (axis != runAxis) {
						if (runAxis != null) {
							columns.accept(new PortalColumn(coord.x * RawChunk.WIDTH + x,
									coord.z * RawChunk.DEPTH + z, runStart, worldY - 1, runAxis));
						}
						runStart = worldY;
						runAxis = axis;
					}
				}
			}
		}
	}

	private static Axis portalAxis(RawChunk chunk, int x, int y, int z) {
		String name = chunk.getBlockName(x, y, z);
		if (Block.NETHER_PORTAL.getName().equals(name)) {
			BlockProperties state = chunk.getBlockState(x, y, z);
			String axis = state == null ? null : state.get("axis");
			if ("x".equals(axis))
				return Axis.X;
			if ("z".equals(axis))
				return Axis.Z;
			return Axis.UNKNOWN;
		}
		if (name == null && chunk.getBlockId(x, y, z) == BlockIds.PORTAL) {
			return switch (chunk.getBlockData(x, y, z)) {
				case 1 -> Axis.X;
				case 2 -> Axis.Z;
				default -> Axis.UNKNOWN;
			};
		}
		return null;
	}

	public static void findViews(RawChunk chunk, HddObjectListWriter<Sign> views, ViewFilter filter) {
                for (SignEntity s : chunk.getSigns().values()) {
                        if (filter.passesFilter(s)) {
                                if (!s.hasDirection())
                                        log.warn("View sign at {} {} {} has no readable direction in the save; the view will face north (-Z)",
                                                        s.getX(), s.getY(), s.getZ());
                                Sign sign = new Sign(s);
                                views.add(sign);
                        }
                }
	}

	public static void findContainers(RawChunk chunk, ContainerFilter filter, ConcurrentLinkedQueue<ContainerEntity> containers) {
                for (ContainerEntity entity : chunk.getContainers()) {
                        if (filter.passesFilter(entity)) {
                                containers.add(entity);
                        }
                }
	}

	public static void findBeds(RawChunk chunk, Queue<BedEntity> beds) {
		chunk.forEachBlock(RawChunk.BlockName::isBed, (x, y, z) -> {
			final BlockProperties blockProperties = chunk.getBlockState(x, y, z);
			// A bed is two blocks; count only its head.
			if (blockProperties != null && "head".equals(blockProperties.get("part"))) {
				final String blockName = chunk.getBlockName(x, y, z);
				ChunkCoord coord = chunk.getChunkCoord();
				beds.add(new BedEntity((int) (coord.x * RawChunk.WIDTH + x), chunk.getWorldY(y), (int) (coord.z * RawChunk.DEPTH + z), x, y, z,
						Colors.byName(blockName.replace("minecraft:", "").replace("_bed", "")).getId()));
			}
		});
	}
	
	public static void findBeacons(RawChunk chunk, Queue<BeaconEntity> beacons, BeaconFilter filter,
			ToIntFunction<BeaconEntity> resolveLevels) {
		if (!filter.passesFilter(true))
			return;
		for (BeaconEntity beaconEntity: chunk.getBeacons().values()) {
			int levels = resolveLevels.applyAsInt(beaconEntity);
			if (filter.passesFilter(levels > 0)) {
				beacons.add(new BeaconEntity(beaconEntity.getX(), beaconEntity.getY(), beaconEntity.getZ(),
						beaconEntity.getLocalX(), beaconEntity.getLocalY(), beaconEntity.getLocalZ(), levels,
						beaconEntity.getPrimaryEffect(), beaconEntity.getSecondaryEffect()));
			}
		}
	}
}
