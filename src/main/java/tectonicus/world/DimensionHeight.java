/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import lombok.extern.slf4j.Slf4j;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import tectonicus.SaveFormat;
import tectonicus.Version;
import tectonicus.configuration.Dimension;
import tectonicus.raw.RawChunk;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The vertical range of a dimension: its lowest block Y and the number of block rows above it.
 * Datapacks can give a dimension type its own min_y and height, so these are read from the world where possible.
 */
@Slf4j
public record DimensionHeight(int minY, int height) {
	public static final DimensionHeight MC_REGION = new DimensionHeight(0, RawChunk.MC_REGION_HEIGHT);
	public static final DimensionHeight CLASSIC = new DimensionHeight(0, 256);
	public static final DimensionHeight CAVES_AND_CLIFFS = new DimensionHeight(-64, 384);

	private static final ObjectMapper JSON = new ObjectMapper();

	/** Bedrock's fixed heights: the Nether is 128 blocks tall, and the Overworld grew to Y -64 to 319 in 1.18. */
	public static DimensionHeight bedrock(Dimension dimension, Version version) {
		if (dimension == Dimension.NETHER)
			return MC_REGION;
		if (dimension == Dimension.END)
			return CLASSIC;
		boolean modern = version == null || version == Version.VERSION_UNKNOWN
				|| version.getNumVersion() >= Version.VERSION_18.getNumVersion();
		return modern ? CAVES_AND_CLIFFS : CLASSIC;
	}

	public int minSectionY() {
		return Math.floorDiv(minY, RawChunk.SECTION_HEIGHT);
	}

	/**
	 * @param dimensionTypes level.dat's dimension id to dimension type map (a type id or an inline definition)
	 * @param dataPacks      enabled datapacks in load order, as level.dat lists them (for example "file/heights.zip")
	 */
	public static DimensionHeight resolve(Path worldDir, Map<String, Tag> dimensionTypes, List<String> dataPacks,
			String dimensionId, Dimension dimension, Version version, SaveFormat format) {
		if (format == SaveFormat.BEDROCK)
			return bedrock(dimension, version);
		if (format != SaveFormat.ANVIL)
			return MC_REGION;
		boolean modern = version != null && version != Version.VERSION_UNKNOWN
				&& version.getNumVersion() >= Version.VERSION_18.getNumVersion();
		DimensionHeight fallback = modern && dimension != Dimension.NETHER && dimension != Dimension.END ? CAVES_AND_CLIFFS : CLASSIC;

		Tag type = dimensionTypes == null || dimensionId == null ? null : dimensionTypes.get(dimensionId);
		DimensionHeight result = null;
		if (type instanceof CompoundTag inline) {
			result = fromNbt(inline);
		} else if (type instanceof StringTag typeId) {
			result = fromDataPacks(worldDir, dataPacks, typeId.getValue());
			if (result == null)
				result = vanilla(typeId.getValue(), modern);
		}
		if (result == null)
			return fallback;
		if (!result.equals(fallback))
			log.info("Dimension {} uses min_y {} and height {}", dimensionId, result.minY, result.height);
		return result;
	}

	static DimensionHeight vanilla(String typeId, boolean modern) {
		return switch (namespaced(typeId)) {
			case "minecraft:overworld", "minecraft:overworld_caves" -> modern ? CAVES_AND_CLIFFS : CLASSIC;
			case "minecraft:the_nether", "minecraft:the_end" -> CLASSIC;
			default -> null;
		};
	}

	static DimensionHeight fromDataPacks(Path worldDir, List<String> dataPacks, String typeId) {
		if (worldDir == null || dataPacks == null || dataPacks.isEmpty())
			return null;
		String id = namespaced(typeId);
		int colon = id.indexOf(':');
		String entry = "data/" + id.substring(0, colon) + "/dimension_type/" + id.substring(colon + 1) + ".json";
		// Later packs override earlier ones, so search from the end of the enabled list.
		for (int i = dataPacks.size() - 1; i >= 0; i--) {
			String pack = dataPacks.get(i);
			if (!pack.startsWith("file/"))
				continue;
			Path packPath = worldDir.resolve("datapacks").resolve(pack.substring("file/".length()));
			try {
				JsonNode json = readPackJson(packPath, entry);
				if (json != null) {
					DimensionHeight height = fromJson(json);
					if (height != null)
						return height;
					log.warn("Ignoring dimension type {} in {}: min_y and height must be multiples of 16", id, pack);
				}
			} catch (IOException | RuntimeException e) {
				log.warn("Could not read dimension type {} from datapack {}", id, pack, e);
			}
		}
		return null;
	}

	private static JsonNode readPackJson(Path pack, String entry) throws IOException {
		if (Files.isDirectory(pack)) {
			Path file = pack.resolve(entry);
			if (!Files.isRegularFile(file))
				return null;
			try (InputStream in = Files.newInputStream(file)) {
				return JSON.readTree(in);
			}
		}
		if (Files.isRegularFile(pack)) {
			try (ZipFile zip = new ZipFile(pack.toFile())) {
				ZipEntry zipEntry = zip.getEntry(entry);
				if (zipEntry == null)
					return null;
				try (InputStream in = zip.getInputStream(zipEntry)) {
					return JSON.readTree(in);
				}
			}
		}
		return null;
	}

	static DimensionHeight fromJson(JsonNode json) {
		if (json == null || !json.has("min_y") || !json.has("height"))
			return null;
		return of(json.get("min_y").asInt(), json.get("height").asInt());
	}

	private static DimensionHeight fromNbt(CompoundTag type) {
		if (!(type.getValue().get("min_y") instanceof IntTag minY) || !(type.getValue().get("height") instanceof IntTag height))
			return null;
		return of(minY.getValue(), height.getValue());
	}

	/** Minecraft requires both values to be multiples of 16 and the height to be positive. */
	static DimensionHeight of(int minY, int height) {
		if (height <= 0 || minY % RawChunk.SECTION_HEIGHT != 0 || height % RawChunk.SECTION_HEIGHT != 0)
			return null;
		return new DimensionHeight(minY, height);
	}

	private static String namespaced(String id) {
		return id.contains(":") ? id : "minecraft:" + id;
	}
}
