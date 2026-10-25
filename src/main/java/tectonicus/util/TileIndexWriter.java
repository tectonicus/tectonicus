/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.
 */

package tectonicus.util;

import lombok.experimental.UtilityClass;
import tectonicus.TileRenderer;
import tectonicus.configuration.ImageFormat;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

@UtilityClass
public class TileIndexWriter {

	/** Writes zoom -> y -> inclusive x ranges, using files actually present after rendering. */
	public static void write(Writer writer, Path layerDir, int maxZoom, ImageFormat format) throws IOException {
		Pattern tileName = Pattern.compile("tile_(-?\\d+)_(-?\\d+)\\." + Pattern.quote(format.getExtension()));
		writer.write("[");
		for (int zoom = 0; zoom <= maxZoom; zoom++) {
			if (zoom > 0)
				writer.write(",");
			Map<Integer, SortedSet<Integer>> rows = new TreeMap<>();
			Path zoomDir = layerDir.resolve("Zoom" + zoom);
			if (Files.isDirectory(zoomDir)) {
				try (var paths = Files.walk(zoomDir, 3)) {
					var iterator = paths.iterator();
					while (iterator.hasNext()) {
						Path path = iterator.next();
						var match = tileName.matcher(path.getFileName().toString());
						if (!match.matches() || !Files.isRegularFile(path))
							continue;
						int x = Integer.parseInt(match.group(1));
						int y = Integer.parseInt(match.group(2));
						// Match the signed remainder buckets used by both the renderer and viewer.
						Path expected = TileRenderer.getImageFile(zoomDir.toFile(), x, y, format).toPath();
						if (path.equals(expected))
							rows.computeIfAbsent(y, key -> new TreeSet<>()).add(x);
					}
				}
			}

			writer.write("{");
			boolean firstRow = true;
			for (var row : rows.entrySet()) {
				if (!firstRow)
					writer.write(",");
				firstRow = false;
				writer.write("\"" + row.getKey() + "\":[");
				var xs = row.getValue().iterator();
				int start = xs.next();
				int end = start;
				while (xs.hasNext()) {
					int x = xs.next();
					if ((long) x != (long) end + 1) {
						writer.write(start + "," + end + ",");
						start = x;
					}
					end = x;
				}
				writer.write(start + "," + end + "]");
			}
			writer.write("}");
		}
		writer.write("]");
	}
}
