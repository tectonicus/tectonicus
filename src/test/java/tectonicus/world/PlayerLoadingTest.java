/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.world;

import org.jnbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.PlayerIconAssembler;
import tectonicus.cache.PlayerSkinCache;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.DimensionConfig;
import tectonicus.raw.Player;
import tectonicus.util.OutputResourcesUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerLoadingTest {
	@TempDir Path directory;

	@Test
	void exportsUnnamedLegacyPlayerWithoutProfileRequestsOrNullNames() throws Exception {
		Player player = new Player("", new CompoundTag("", Map.of()));
		// No cache is needed: an unknown account must never trigger a profile lookup.
		List<Player> players = World.loadPlayers(directory.toFile(), null, player);
		assertEquals(List.of(player), players);
		Path output = directory.resolve("players.js");
		PlayerIconAssembler icons = new PlayerIconAssembler(null) {
			@Override public void writeIcon(Player exported, File file) {}
		};
		OutputResourcesUtil.outputPlayers(output.toFile(), directory.resolve("Images").toFile(),
				new DimensionConfig("Map"), players, icons);
		String script = Files.readString(output);
		assertTrue(script.contains("name: \"Player\""));
		assertFalse(script.contains("\"null\""));
	}

	@Test
	void emptyPlayerDirectoryStillLoadsEmbeddedSinglePlayer() throws Exception {
		Files.createDirectories(directory.resolve("players"));
		Player player = new Player("", new CompoundTag("", Map.of()));
		assertEquals(List.of(player), World.loadPlayers(directory.toFile(), null, player));
	}

	@Test
	void savesWithoutAnyPlayerDataReturnAnEmptyList() {
		assertTrue(World.loadPlayers(directory.toFile(), null, null).isEmpty());
	}

	@Test
	void unknownAccountUsesDefaultSkinWithoutCachingANullIdentity() throws Exception {
		Configuration config = new Configuration();
		config.setCacheDir(directory.toFile());
		PlayerSkinCache cache = new PlayerSkinCache(config, null);
		assertNull(cache.fetchSkin(new Player("", new CompoundTag("", Map.of()))));
		cache.destroy();
		String index = Files.readString(directory.resolve("skinCache/skins.cache"));
		assertFalse(index.contains("<entry "));
	}
}
