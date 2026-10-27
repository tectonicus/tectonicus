/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.util;

import lombok.experimental.UtilityClass;
import tectonicus.TileCoord;
import tectonicus.TileRenderer;
import tectonicus.cache.swap.HddTileList;
import tectonicus.configuration.ImageFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

@UtilityClass
public class TileOutput {
	public static void removeObsolete(Path baseDir, HddTileList visible, ImageFormat format, HddTileList removed) throws IOException {
		Pattern name = Pattern.compile("tile_(-?\\d+)_(-?\\d+)\\." + Pattern.quote(format.getExtension()));
		try (var paths = Files.walk(baseDir, 3)) {
			var iterator = paths.iterator();
			while (iterator.hasNext()) {
				Path path = iterator.next();
				var match = name.matcher(path.getFileName().toString());
				if (!match.matches() || !Files.isRegularFile(path))
					continue;
				TileCoord tile = new TileCoord(Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2)));
				if (!visible.contains(tile) && path.equals(TileRenderer.getImageFile(baseDir.toFile(), tile.x, tile.y, format).toPath())) {
					Files.delete(path);
					removed.add(tile);
				}
			}
		}
	}
}
