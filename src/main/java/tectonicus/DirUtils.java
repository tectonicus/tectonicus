/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.experimental.UtilityClass;
import tectonicus.configuration.Dimension;
import tectonicus.configuration.Layer;
import tectonicus.configuration.Map;

import java.io.File;

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

	public static File getDimensionDir(File worldDir, Dimension dimension) {
		File dimensionDir;

		if (dimension == Dimension.OVERWORLD) {
			dimensionDir = worldDir;
		} else if (dimension == Dimension.NETHER) {
			dimensionDir = new File(worldDir, "DIM-1");
		} else if (dimension == Dimension.END) {
			dimensionDir = new File(worldDir, "DIM1");
		} else {
			dimensionDir = worldDir;
		}

		return dimensionDir;
	}
}
