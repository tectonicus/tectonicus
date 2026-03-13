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
import tectonicus.configuration.Dimension;
import tectonicus.configuration.Layer;
import tectonicus.configuration.Map;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public class DirUtils
{
	public static File getMapDir(File exportDir, Map map)
	{
		return new File(exportDir, map.getId());
	}
	
	public static File getLayerDir(File exportDir, Layer layer)
	{
		File mapDir = new File(exportDir, layer.getMapId());
		return new File(mapDir, layer.getId());
	}
	
	public static File getZoomDir(File exportDir, Layer layer, final int zoomLevel)
	{
		File layerDir = getLayerDir(exportDir, layer);
		return new File(layerDir, "Zoom"+zoomLevel);
	}

	public static Path getDimensionDir(Path worldDir, Dimension dimension) {
		String[] dimensionParts = dimension.getId().split(":");
		String namespace = dimensionParts[0];
		String path = dimensionParts[1];
		Path dimensionDir = Paths.get(worldDir.toString(),"dimensions", namespace, path);
		if (!Files.exists(dimensionDir)) { //fall back to old directory structure if new one doesn't exist
			dimensionDir = switch (dimension) {
				case NETHER -> worldDir.resolve("DIM-1");
				case END -> worldDir.resolve("DIM1");
				default -> worldDir;
			};
		}

		return dimensionDir;
	}
}
