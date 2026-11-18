/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.overlay;

import lombok.extern.slf4j.Slf4j;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.Dimension;
import tectonicus.configuration.DimensionConfig;
import tectonicus.configuration.OverlayConfig;
import tectonicus.raw.WorldBorder;
import tectonicus.util.FileUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;

/**
 * Collects and writes the vector overlays of one dimension as Scripts/&lt;dimension&gt;/overlays.js.
 * Only the results are exported: the slime chunk overlay lists chunks, never the world seed.
 */
@Slf4j
public class OverlayExporter {
	public static final String FILE_NAME = "overlays.js";

	private final DimensionConfig map;
	private final OverlayConfig config;
	private final Long slimeSeed;
	private final TreeSet<ChunkCoord> slimeChunks = new TreeSet<>();

	public OverlayExporter(DimensionConfig map, Long worldSeed) {
		this.map = map;
		this.config = map.getOverlays();
		Long seed = null;
		if (config.getSlimeChunks().isEnabled()) {
			if (map.getDimension() != Dimension.OVERWORLD)
				log.info("Slime chunks exist only in the Overworld; skipping the slime chunk overlay for {}", map.getName());
			else if (worldSeed == null)
				log.warn("The world seed could not be read; skipping the slime chunk overlay for {}", map.getName());
			else
				seed = worldSeed;
		}
		slimeSeed = seed;
	}

	public boolean isEnabled() {
		return config.isEnabled();
	}

	/** Call for every chunk that is rendered in this dimension. Thread safe. */
	public void addRenderedChunk(ChunkCoord coord) {
		if (slimeSeed != null && isSlimeChunk(slimeSeed, (int) coord.x, (int) coord.z)) {
			synchronized (slimeChunks) {
				slimeChunks.add(new ChunkCoord(coord));
			}
		}
	}

	/** Java Edition's slime chunk test, including its int overflow. */
	public static boolean isSlimeChunk(long seed, int chunkX, int chunkZ) {
		Random random = new Random(seed
				+ (int) (chunkX * chunkX * 0x4c1906)
				+ (int) (chunkX * 0x5ac0db)
				+ (int) (chunkZ * chunkZ) * 0x4307a7L
				+ (int) (chunkZ * 0x5f24f) ^ 0x3ad8025fL);
		return random.nextInt(10) == 0;
	}

	public void write(File file, Path worldDir, Path dimensionDir) {
		try {
			Files.deleteIfExists(file.toPath());
		} catch (IOException e) {
			log.error("Exception: ", e);
		}
		if (!isEnabled())
			return;

		var mapper = FileUtils.getOBJECT_MAPPER();
		ObjectNode root = mapper.createObjectNode();
		root.put("y", config.getElevation());
		if (config.getChunkGrid().isEnabled())
			root.putObject("chunkGrid").put("visible", isShown(config.getChunkGrid()));
		if (config.getRegionGrid().isEnabled())
			root.putObject("regionGrid").put("visible", isShown(config.getRegionGrid()));
		if (slimeSeed != null) {
			ObjectNode slime = root.putObject("slimeChunks");
			slime.put("visible", isShown(config.getSlimeChunks()));
			// Flat [x0, z0, x1, z1, ...] keeps large worlds compact.
			ArrayNode chunks = slime.putArray("chunks");
			synchronized (slimeChunks) {
				for (ChunkCoord coord : slimeChunks)
					chunks.add(coord.x).add(coord.z);
			}
			log.info("Found {} slime chunks", slimeChunks.size());
		}
		if (config.getWorldBorder().isEnabled()) {
			Optional<WorldBorder> border = WorldBorder.load(worldDir, dimensionDir, map.getDimension());
			if (border.isPresent()) {
				WorldBorder b = border.get();
				root.putObject("worldBorder").put("visible", isShown(config.getWorldBorder()))
						.put("centerX", b.centerX()).put("centerZ", b.centerZ()).put("size", b.size());
			} else {
				log.info("No world border saved for {}; skipping the world border overlay", map.getName());
			}
		}
		if (config.getFeatures().isEnabled() && config.getFeaturesFile() != null) {
			try {
				ArrayNode features = OverlayFeatures.read(config.getFeaturesFile().toPath());
				ObjectNode node = root.putObject("features");
				node.put("visible", isShown(config.getFeatures()));
				node.set("items", features);
				log.info("Loaded {} overlay features from {}", features.size(), config.getFeaturesFile());
			} catch (IOException | JacksonException | IllegalArgumentException e) {
				log.error("Unable to read overlay file {}: {}", config.getFeaturesFile(), e.getMessage());
			}
		}

		String script = "var " + map.getJavascriptId() + "_overlays = " + mapper.writeValueAsString(root) + ";\n";
		try {
			Files.writeString(file.toPath(), script, StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Unable to write {}", file, e);
		}
	}

	private static boolean isShown(OverlayConfig.Visibility visibility) {
		return visibility == OverlayConfig.Visibility.SHOWN;
	}
}
