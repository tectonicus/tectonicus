/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3f;
import tectonicus.Minecraft;
import tectonicus.Portal;
import tectonicus.TileRenderer;
import tectonicus.configuration.Dimension;
import tectonicus.raw.LevelDat;
import tectonicus.raw.Player;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.OrthoCamera;
import tectonicus.util.JsonWriter;
import tectonicus.util.Vector2f;
import tectonicus.util.Vector3l;
import tectonicus.world.subset.CircularWorldSubset;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.List;

@Slf4j
@Builder
@Getter
public class WorldVectors {
	private Vector2f origin;
	private Vector2f xAxis;
	private Vector2f yAxis;
	private Vector2f zAxis;
	private Vector2f mapXUnit;
	private Vector2f mapYUnit;
	private int yOffset;

	public static WorldVectors calcWorldVectors(OrthoCamera camera) {
		// Calculate origin and axes needed for the js to convert from world to map coords

		// Vectors for world->map projection
		Vector2f originScreenPos = camera.projectf(new Vector3f(0, 0, 0));
		Vector2f p100 = camera.projectf(new Vector3f(1, 0, 0));
		Vector2f p010 = camera.projectf(new Vector3f(0, 1, 0));
		Vector2f p001 = camera.projectf(new Vector3f(0, 0, 1));

		// Vectors for map->world projection
		Vector3f base = camera.unproject(new Vector2f(0, 0));
		Vector3f mapXUnit = camera.unproject(new Vector2f(1, 0));
		Vector3f mapYUnit = camera.unproject(new Vector2f(0, 1));

		// For 1.18 and higher we need to offset the map markers
		int yOffset = 0;
		if (Minecraft.getChunkHeight() > 256) {
			yOffset = 64;
		}

		return WorldVectors.builder()
				.origin(new Vector2f(originScreenPos.x, originScreenPos.y))
				.xAxis(new Vector2f(p100.x - originScreenPos.x, p100.y - originScreenPos.y))
				.yAxis(new Vector2f(p010.x - originScreenPos.x, p010.y - originScreenPos.y))
				.zAxis(new Vector2f(p001.x - originScreenPos.x, p001.y - originScreenPos.y))
				.mapXUnit(new Vector2f(mapXUnit.x - base.x, mapXUnit.z - base.z))
				.mapYUnit(new Vector2f(mapYUnit.x - base.x, mapYUnit.z - base.z))
				.yOffset(yOffset).build();
	}

	public void outputWorldVectors(File vectorsFile, tectonicus.configuration.Map map, TileRenderer.TileCoordBounds bounds, World world,
								   final int numChunks, List<Portal> portals, int numZoomLevels, int tileWidth, int tileHeight) {
		String varNamePrefix = map.getId();
		LevelDat levelDat = world.getLevelDat();
		List<Player> players = world.getAllPlayers();
		int numPlayers = players.size();

		try {
			Files.deleteIfExists(vectorsFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Exporting world vectors to {}", vectorsFile.getAbsolutePath());

		final int scale = (int) Math.pow(2, numZoomLevels);

		final int surfaceAreaM = numChunks * RawChunk.WIDTH * RawChunk.DEPTH;
		final DecimalFormat formatter = new DecimalFormat("####.#");
		final String surfaceArea = formatter.format(surfaceAreaM / 1000.0f / 1000.0f);


		try (JsonWriter json = new JsonWriter(vectorsFile)) {
			json.startObject(varNamePrefix + "_worldVectors");

			// World name and size
			json.writeVariable("worldName", levelDat.getWorldName());
			json.writeVariable("worldSizeInBytes", levelDat.getSizeOnDisk());
			json.writeVariable("numChunks", numChunks);
			json.writeVariable("surfaceArea", surfaceArea);
			json.writeVariable("numPlayers", numPlayers);

			Vector3l spawnPos = levelDat.getSpawnPosition();
			if (map.getWorldSubset().containsBlock(spawnPos.x, spawnPos.z)) {
				// Spawn point
				json.writeWorldCoord("spawnPosition", levelDat.getSpawnPosition());
			}

			json.writeWorldCoord("startView", getStartView(map, world, portals));

			Vector2f scaledOrigin = new Vector2f();
			scaledOrigin.x = (this.origin.x / scale);
			scaledOrigin.y = (this.origin.y / scale);
			json.writeMapsPoint("origin", scaledOrigin);

			// Axes
			Vector2f scaledXAxis = new Vector2f(this.xAxis.x / scale, this.xAxis.y / scale);
			json.writeMapsPoint("xAxis", scaledXAxis);

			Vector2f scaledYAxis = new Vector2f(this.yAxis.x / scale, this.yAxis.y / scale);
			json.writeMapsPoint("yAxis", scaledYAxis);

			Vector2f scaledZAxis = new Vector2f(this.zAxis.x / scale, this.zAxis.y / scale);
			json.writeMapsPoint("zAxis", scaledZAxis);

			// Units
			Vector2f scaledMapXUnit = new Vector2f(this.mapXUnit.x * scale, this.mapXUnit.y * scale);
			json.writeMapsPoint("mapXUnit", scaledMapXUnit);

			Vector2f scaledMapYUnit = new Vector2f(this.mapYUnit.x * scale, this.mapYUnit.y * scale);
			json.writeMapsPoint("mapYUnit", scaledMapYUnit);

			// Min and max bounds
			final long mapXMin = (long) bounds.min.x * tileWidth;
			final long mapYMin = (long) bounds.min.y * tileHeight;

			final long mapXMax = (long) bounds.max.x * tileWidth + tileWidth;
			final long mapYMax = (long) bounds.max.y * tileHeight + tileHeight;

			final long mapWidth = (mapXMax - mapXMin);
			final long mapHeight = (mapYMax - mapYMin);

			json.writeMapsPoint("mapMin", mapXMin, mapYMin);

			json.writeMapsPoint("mapSize", +mapWidth, mapHeight);

			json.writeVariable("yOffset", this.yOffset);

			json.endObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Vector3l getStartView(tectonicus.configuration.Map map, World world, List<Portal> portals) {
		LevelDat levelDat = world.getLevelDat();

		Vector3l startView;
		if (map.getWorldSubset() instanceof CircularWorldSubset) {
			CircularWorldSubset subset = (CircularWorldSubset) map.getWorldSubset();
			startView = new Vector3l(subset.getOrigin().x, 64, subset.getOrigin().z);
		} else if (map.getOrigin() != null) {
			startView = map.getOrigin();
		} else {
			Vector3l spawnPosition = levelDat.getSpawnPosition();
			startView = levelDat.getSpawnPosition();

			if (map.getDimension() == Dimension.NETHER) {
				//For the Nether we try to find a portal, player or Respawn Anchor closest to overworld spawn and use that as the origin
				//otherwise we just use the overworld spawn position as the origin
				double prevDistance = 99999999999d;

				//Prefer using portals as origin
				if (!portals.isEmpty()) {
					for (Portal portal : portals) {
						double distance = Math.hypot(portal.getX() - (double) spawnPosition.x, portal.getZ() - (double) spawnPosition.z);
						if (distance < prevDistance) {
							startView = new Vector3l(portal.getX(), portal.getY(), portal.getZ());
						}
						prevDistance = distance;
					}
				} else { //If no portals then try players
					startView = world.getNetherOriginFromPlayers();
				}
			}
		}

		return startView;
	}
}
