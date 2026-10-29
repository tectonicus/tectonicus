/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.util;

import org.jnbt.CompoundTag;
import org.jnbt.StringTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.PlayerIconAssembler;
import tectonicus.configuration.DimensionConfig;
import tectonicus.configuration.ParseUtil;
import tectonicus.raw.Player;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerExportDimensionTest {
	@TempDir
	Path directory;

	@Test
	void exportedPlayersStayInTheirExactDimension() throws Exception {
		List<String> ids = List.of("minecraft:overworld", "example:moon", "example:moon/caves");
		List<Player> players = ids.stream().map(id -> new Player(id.replace(':', '_').replace('/', '_'),
				new CompoundTag("", Map.of("Dimension", new StringTag("Dimension", id))))).toList();
		PlayerIconAssembler icons = new PlayerIconAssembler(null) {
			@Override public void writeIcon(Player player, File file) {}
		};
		for (int i = 0; i < ids.size(); i++) {
			DimensionConfig dimension = new DimensionConfig("Dimension" + i);
			dimension.setDimensionInfo(ParseUtil.parseDimension(ids.get(i)));
			Path output = directory.resolve("players" + i + ".js");
			OutputResourcesUtil.outputPlayers(output.toFile(), directory.resolve("Images").toFile(), dimension, players, icons);
			String script = Files.readString(output);
			for (int j = 0; j < players.size(); j++)
				assertEquals(i == j, script.contains("\"" + players.get(j).getName() + "\""));
		}
	}
}
