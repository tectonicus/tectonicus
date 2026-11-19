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
import tectonicus.configuration.Configuration;

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
}
