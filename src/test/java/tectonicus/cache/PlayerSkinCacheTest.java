/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.PlayerIconAssembler;
import tectonicus.configuration.Configuration;
import tectonicus.raw.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlayerSkinCacheTest {
	@Test
	void indexRoundTripsValuesThatNeedEscaping(@TempDir Path dir) throws Exception {
		Configuration config = new Configuration();
		config.setCacheDir(dir.toFile());
		Path skinDir = Files.createDirectories(dir.resolve("skinCache"));
		String url = "https://textures.example/skin?a=1&b=\"2\"<3>";
		Files.writeString(skinDir.resolve("skins.cache"), """
				<skinCache version="1">
					<entries>
						<entry playerName="Name&amp;&quot;" playerUUID="uuid-1" skinURL="%s" skinFile="uuid-1.png" fetchedTime="42" />
					</entries>
				</skinCache>
				""".formatted(url.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")));

		new PlayerSkinCache(config, null).destroy();
		PlayerSkinCache.CacheEntry entry = new PlayerSkinCache(config, null).getCacheEntry("uuid-1");

		assertNotNull(entry);
		assertEquals("Name&\"", entry.playerName);
		assertEquals(url, entry.skinURL);
		assertEquals("uuid-1.png", entry.skinFile);
		assertEquals(42, entry.fetchedTime);
		assertFalse(Files.exists(skinDir.resolve("skins.cache.tmp")));
	}

	@Test
	void skinFilesAreNamedByUuid() {
		assertEquals("0f3c-a1_b.png", PlayerSkinCache.skinFileName("0f3c-a1/b"));
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void reusesHeadSkinAcrossRendersWithMissingOrUnreadablePlayerIndex(boolean unreadableIndex, @TempDir Path dir) throws Exception {
		Configuration config = new Configuration();
		config.setCacheDir(dir.toFile());
		Path source = dir.resolve("source.png");
		BufferedImage original = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		original.setRGB(0, 0, 0xff124578);
		ImageIO.write(original, "png", source.toFile());
		String url = source.toUri().toString();

		PlayerSkinCache first = new PlayerSkinCache(config, null);
		try {
			first.prefetchHeadSkin(url);
			BufferedImage loaded = first.fetchHeadSkin(url);
			assertNotNull(loaded);
			loaded.setRGB(0, 0, 0);
			assertEquals(original.getRGB(0, 0), first.fetchHeadSkin(url).getRGB(0, 0));
		} finally {
			first.destroy();
		}

		Files.delete(source);
		Path index = dir.resolve("skinCache/skins.cache");
		if (unreadableIndex)
			Files.writeString(index, "<unexpected/>");
		else
			Files.delete(index);
		PlayerSkinCache second = new PlayerSkinCache(config, null);
		try {
			BufferedImage cached = second.fetchHeadSkin(url);
			assertNotNull(cached);
			assertEquals(original.getRGB(0, 0), cached.getRGB(0, 0));
		} finally {
			second.destroy();
		}
	}

	@Test
	void drainsPlayerIconsBeforePersistingTheirSkins(@TempDir Path dir) throws Exception {
		Configuration config = new Configuration();
		config.setCacheDir(dir.toFile());
		Path source = dir.resolve("source.png");
		BufferedImage original = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		original.setRGB(0, 0, 0xff124578);
		ImageIO.write(original, "png", source.toFile());
		Player player = new Player("Alex", "uuid-1", source.toUri().toString());
		Path icon = dir.resolve("Alex.png");
		PlayerSkinCache first = new PlayerSkinCache(config, null);
		try (PlayerIconAssembler icons = new PlayerIconAssembler(first)) {
			icons.writeIconAsync(player, icon.toFile());
		} finally {
			first.destroy();
		}
		assertNotNull(ImageIO.read(icon.toFile()));

		Files.delete(source);
		PlayerSkinCache second = new PlayerSkinCache(config, null);
		try {
			assertEquals(original.getRGB(0, 0), second.fetchSkin(player).getRGB(0, 0));
		} finally {
			second.destroy();
		}
	}
}
