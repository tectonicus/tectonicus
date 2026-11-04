/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.texture.ZipStack;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ItemIconCacheTest {
	@TempDir Path directory;
	private Path jar;
	private Path icons;
	private static final Set<String> ITEMS = Set.of("potion", "stone");

	@BeforeEach
	void setup() throws Exception {
		jar = archive("client.jar", "original");
		icons = Files.createDirectory(directory.resolve("Items"));
	}

	@Test
	void reusesBothLayersAndRepairsOnlyDamagedIcons() throws Exception {
		populate(cache("settings"));
		assertEquals(ITEMS, cache("settings").prepare(ITEMS));
		// prepare invalidates the manifest until a render finishes successfully.
		cache("settings").save(ITEMS, Set.of());
		Files.delete(icons.resolve("potion_overlay.png"));
		ItemIconCache cache = cache("settings");
		assertEquals(Set.of("stone"), cache.prepare(ITEMS));
		assertFalse(Files.exists(icons.resolve("potion.png")));
		Files.writeString(icons.resolve("potion.png"), "repaired");
		cache.save(ITEMS, Set.of());
		Files.writeString(icons.resolve("stone.png"), "corrupt");
		assertEquals(Set.of("potion"), cache("settings").prepare(ITEMS));
	}

	@ParameterizedTest
	@ValueSource(strings = {"jar", "pack", "mod", "settings", "disabled"})
	void regeneratesWhenInputsChange(String change) throws Exception {
		Path pack = archive("pack.zip", "pack");
		Path mod = archive("mod.jar", "mod");
		populate(cache(jar, pack, List.of(mod), "settings"));
		Path changed = archive("changed.zip", "different");
		ItemIconCache next = cache(change.equals("jar") ? changed : jar,
				change.equals("pack") ? changed : pack,
				List.of(change.equals("mod") ? changed : mod),
				change.equals("disabled") ? null : change.equals("settings") ? "new settings" : "settings");
		assertTrue(next.prepare(ITEMS).isEmpty());
		assertFalse(Files.exists(icons.resolve("potion_overlay.png")));
	}

	@Test
	void retriesFailedIconsEvenWhenTheyWrotePartialOutput() throws Exception {
		ItemIconCache cache = cache("settings");
		populate(cache);
		cache.save(ITEMS, Set.of("potion"));
		assertEquals(Set.of("stone"), cache("settings").prepare(ITEMS));
	}

	@Test
	void interruptedRenderDoesNotReuseUnfinishedOutputs() throws Exception {
		populate(cache("settings"));
		assertEquals(ITEMS, cache("settings").prepare(ITEMS));
		assertTrue(cache("settings").prepare(ITEMS).isEmpty());
	}

	@Test
	void malformedManifestRegeneratesIcons() throws Exception {
		populate(cache("settings"));
		Files.writeString(icons.resolve("item-icons.cache"), "bad=\\uXXXX");
		assertTrue(cache("settings").prepare(ITEMS).isEmpty());
	}

	private void populate(ItemIconCache cache) throws Exception {
		assertTrue(cache.prepare(ITEMS).isEmpty());
		Files.writeString(icons.resolve("potion.png"), "base");
		Files.writeString(icons.resolve("potion_overlay.png"), "overlay");
		Files.writeString(icons.resolve("stone.png"), "stone");
		cache.save(ITEMS, Set.of());
	}

	private ItemIconCache cache(String options) throws Exception {
		return cache(jar, null, List.of(), options);
	}

	private ItemIconCache cache(Path base, Path pack, List<Path> mods, String options) throws Exception {
		ZipStack resources = new ZipStack(base.toFile(), pack == null ? null : pack.toFile(), mods.stream().map(Path::toFile).toList());
		try {
			return new ItemIconCache(icons, resources, options);
		} finally {
			for (String name : List.of("base", "override")) {
				var field = ZipStack.class.getDeclaredField(name);
				field.setAccessible(true);
				FileSystem fs = (FileSystem) field.get(resources);
				if (fs != null)
					fs.close();
			}
		}
	}

	private Path archive(String name, String contents) throws Exception {
		Path path = directory.resolve(name);
		try (var out = new ZipOutputStream(Files.newOutputStream(path))) {
			out.putNextEntry(new ZipEntry("test"));
			out.write(contents.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			out.closeEntry();
		}
		return path;
	}
}
