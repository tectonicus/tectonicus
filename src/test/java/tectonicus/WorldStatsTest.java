/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldStatsTest {
	@TempDir Path directory;

	@ParameterizedTest
	@EnumSource(value = Version.class, names = {"VERSION_12", "VERSION_13", "VERSION_26_4", "VERSION_UNKNOWN"})
	void exportsWorldVersionForMarkerTeleportCommands(Version version) throws Exception {
		Path output = directory.resolve("worldStats.js");
		new WorldStats().outputWorldStats(output.toFile(), "Overworld", version);
		int expected = version == Version.VERSION_UNKNOWN ? 0 : version.getNumVersion();
		assertTrue(Files.readString(output).contains("minecraftVersion: " + expected));
	}
}
