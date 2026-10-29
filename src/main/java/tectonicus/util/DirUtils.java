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
import tectonicus.configuration.DimensionInfo;
import tectonicus.configuration.LayerConfig;
import tectonicus.configuration.WorldConfig;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public class DirUtils
{
	public static File getWorldImagesDir(File exportDir, WorldConfig world) {
		return new File(exportDir, world.getId() + "/Images");
	}

	public static File getLayerDir(File exportDir, LayerConfig layer)
	{
		File worldDir = new File(exportDir, layer.getWorldId());
		return new File(worldDir, layer.getDirectoryName());
	}
	
	public static File getZoomDir(File exportDir, LayerConfig layer, final int zoomLevel)
	{
		File layerDir = getLayerDir(exportDir, layer);
		return new File(layerDir, "Zoom"+zoomLevel);
	}

	public static Path getDimensionDir(Path worldDir, DimensionInfo dimensionInfo) {
		String[] dimensionParts = dimensionInfo.name().contains(":")
				? dimensionInfo.name().split(":", 2) : dimensionInfo.dimension().getId().split(":", 2);
		String namespace = dimensionParts[0];
		String path = dimensionParts[1];
		if (path.equals("other") && !dimensionInfo.name().contains(":")) { // Legacy custom dimension paths
			path = dimensionInfo.name();
		}
		Path dimensionDir = Paths.get(worldDir.toString(),"dimensions", namespace, path);
		if (!Files.exists(dimensionDir) && dimensionInfo.dimension() != tectonicus.configuration.Dimension.OTHER) {
			dimensionDir = switch (dimensionInfo.dimension()) {
				case NETHER -> worldDir.resolve("DIM-1");
				case END -> worldDir.resolve("DIM1");
				default -> worldDir;
			};
		} else if (!Files.exists(dimensionDir)) {
			throw new IllegalStateException("No directory found for dimension: " + dimensionInfo.name());
		}

		return dimensionDir;
	}
}
