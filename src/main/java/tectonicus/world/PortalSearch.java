 /*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world;

import tectonicus.SaveFormat;
import tectonicus.configuration.Dimension;

import java.util.Locale;
import java.util.regex.Pattern;

public record PortalSearch(Mode mode, int radius, Integer maxY) {
	private static final int PORTAL_POINTS_OF_INTEREST = 2203; // 19w36a
	private static final int EXPANDED_CHUNK_SEARCH = 2506; // 20w07a
	private static final int BLOCK_SEARCH_BOUNDS = 2570; // 20w28a
	private static final Pattern RELEASE = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})(?:\\.(\\d{1,2}))?(?:[- .].*)?");
	private static final Pattern SNAPSHOT = Pattern.compile("\\d{2}w\\d{2}[a-z]");

	public enum Mode { COLUMN_CENTERS, COLUMN_BLOCKS, CHUNKS, BLOCKS }

	public static PortalSearch forSave(int dataVersion, String versionName, SaveFormat format, Dimension dimension) {
		if (dataVersion > 0)
			return forDataVersion(dataVersion, format, dimension);

		if (versionName != null) {
			PortalSearch search = forVersionName(versionName.strip().toLowerCase(Locale.ROOT), format, dimension);
			if (search != null)
				return search;
		}

		if (format == SaveFormat.ANVIL || format == SaveFormat.MC_REGION)
			return legacy(Mode.COLUMN_CENTERS, format, dimension);
		return new PortalSearch(Mode.COLUMN_CENTERS, 32, 127);
	}

	private static PortalSearch forDataVersion(int dataVersion, SaveFormat format, Dimension dimension) {
		if (dataVersion >= BLOCK_SEARCH_BOUNDS)
			return new PortalSearch(Mode.BLOCKS, dimension == Dimension.NETHER ? 16 : 128, null);
		if (dataVersion >= EXPANDED_CHUNK_SEARCH)
			return new PortalSearch(Mode.CHUNKS, 9, null);
		if (dataVersion >= PORTAL_POINTS_OF_INTEREST)
			return new PortalSearch(Mode.CHUNKS, 8, null);
		return legacy(Mode.COLUMN_BLOCKS, format, dimension);
	}

	private static PortalSearch forVersionName(String name, SaveFormat format, Dimension dimension) {
		if (SNAPSHOT.matcher(name).matches()) {
			if (name.compareTo("20w28a") >= 0)
				return forDataVersion(BLOCK_SEARCH_BOUNDS, format, dimension);
			if (name.compareTo("20w07a") >= 0)
				return forDataVersion(EXPANDED_CHUNK_SEARCH, format, dimension);
			if (name.compareTo("19w36a") >= 0)
				return forDataVersion(PORTAL_POINTS_OF_INTEREST, format, dimension);
			return legacy(name.compareTo("14w02a") >= 0 ? Mode.COLUMN_BLOCKS : Mode.COLUMN_CENTERS, format, dimension);
		}
		var release = RELEASE.matcher(name);
		if (!release.matches())
			return null;
		int major = Integer.parseInt(release.group(1));
		int minor = Integer.parseInt(release.group(2));
		int patch = release.group(3) == null ? 0 : Integer.parseInt(release.group(3));
		if (major >= 26 || major == 1 && (minor > 16 || minor == 16 && patch >= 2))
			return forDataVersion(BLOCK_SEARCH_BOUNDS, format, dimension);
		if (major != 1)
			return null;
		if (minor == 16)
			return forDataVersion(EXPANDED_CHUNK_SEARCH, format, dimension);
		if (minor == 15)
			return forDataVersion(PORTAL_POINTS_OF_INTEREST, format, dimension);
		return legacy(minor >= 8 ? Mode.COLUMN_BLOCKS : Mode.COLUMN_CENTERS, format, dimension);
	}

	private static PortalSearch legacy(Mode mode, SaveFormat format, Dimension dimension) {
		int maxY = dimension == Dimension.NETHER || format == SaveFormat.MC_REGION || format == SaveFormat.ALPHA ? 127 : 255;
		return new PortalSearch(mode, 128, maxY);
	}
}
