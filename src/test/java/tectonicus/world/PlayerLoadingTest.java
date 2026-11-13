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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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

	@Test
	void loadsPlayerHeadTexturesFromTheirUrlsWithoutAccountProfiles() throws Exception {
		Configuration config = new Configuration();
		config.setCacheDir(directory.toFile());
		PlayerSkinCache cache = new PlayerSkinCache(config, null);
		var skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Path firstTexture = directory.resolve("first.png");
		Path secondTexture = directory.resolve("second.png");
		skin.setRGB(0, 0, 0xffff0000);
		ImageIO.write(skin, "png", firstTexture.toFile());
		skin.setRGB(0, 0, 0xff00ff00);
		ImageIO.write(skin, "png", secondTexture.toFile());

		var noName = new Player("", "uuid", firstTexture.toUri().toString());
		var noUuid = new Player("Alex", "", secondTexture.toUri().toString());
		var noIdentity = new Player("", "", secondTexture.toUri().toString());
		assertEquals(0xffff0000, cache.fetchSkin(noName).getRGB(0, 0));
		assertEquals(0xff00ff00, cache.fetchSkin(noUuid).getRGB(0, 0));
		assertEquals(0xff00ff00, cache.fetchSkin(noIdentity).getRGB(0, 0));
		assertNull(cache.getCacheEntry("uuid"));
		assertNull(cache.getCacheEntry(""));
	}

	@Test
	void exportsPlayerWithUnresolvedProfileAndIgnoresOldNullCacheEntry() throws Exception {
		Configuration config = new Configuration();
		config.setCacheDir(directory.toFile());
		Path skinCache = Files.createDirectory(directory.resolve("skinCache"));
		Files.writeString(skinCache.resolve("skins.cache"), """
				<skinCache version="1"><entries>
				    <entry playerName="null" playerUUID="abc" fetchedTime="1" skinURL="null" skinFile="Tectonicus_Default_Player_Skin.png"/>
				</entries></skinCache>
				""");
		PlayerSkinCache cache = new PlayerSkinCache(config, null);
		assertNull(cache.getCacheEntry("abc"));
		Player player = new Player(null, "abc", null);
		assertNull(cache.fetchSkin(player));
		Path output = directory.resolve("players.js");
		OutputResourcesUtil.outputPlayers(output.toFile(), directory.resolve("Images").toFile(),
				new DimensionConfig("Map"), List.of(player), new PlayerIconAssembler(cache));
		String script = Files.readString(output);
		assertTrue(script.contains("name: \"Player\""));
		assertTrue(script.contains("defaultPlayerName: true"));
		assertFalse(script.contains("\"null\""));
		assertNull(cache.getCacheEntry("abc"));
	}
}
