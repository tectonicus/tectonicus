/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */

package tectonicus.texture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZipStackTest {
	private static final String STONE = "assets/minecraft/textures/block/stone.png";
	private static final String DIRT = "assets/minecraft/textures/block/dirt.png";

	@TempDir
	Path directory;

	@Test
	void highestPriorityPackWinsPerFile() throws Exception {
		Path jar = zip("client.jar", Map.of(STONE, "vanilla stone", DIRT, "vanilla dirt"));
		Path low = zip("low.zip", Map.of(STONE, "low stone", DIRT, "low dirt"));
		Path high = zip("high.zip", Map.of(STONE, "high stone"));
		try (ZipStack stack = new ZipStack(jar.toFile(), List.of(high.toFile(), low.toFile()), List.of())) {
			assertEquals("high stone", read(stack, STONE));
			assertEquals("low dirt", read(stack, DIRT));
			assertEquals("vanilla stone", read(stack.getBaseStream(STONE)));
			assertTrue(stack.hasPackFile(DIRT));
			assertEquals(3, stack.getLayers(STONE).size());
			assertEquals(List.of(high.toFile(), low.toFile()), stack.getResourcePackFiles());
		}
	}

	@Test
	void folderPacksAreSupported() throws Exception {
		Path jar = zip("client.jar", Map.of(STONE, "vanilla stone"));
		Path folder = directory.resolve("folder pack");
		Files.createDirectories(folder.resolve(STONE).getParent());
		Files.writeString(folder.resolve(STONE), "folder stone");
		Files.writeString(folder.resolve("assets/minecraft/textures/block/new.png"), "new");
		try (ZipStack stack = new ZipStack(jar.toFile(), List.of(folder.toFile()), List.of())) {
			assertEquals("folder stone", read(stack, STONE));
			assertEquals("folder stone", read(stack, "/" + STONE));
			assertEquals(List.of("assets/minecraft/textures/block/new.png", "assets/minecraft/textures/block/stone.png"),
					stack.listFilesInDirectory("assets/minecraft/textures/block").stream().sorted().toList());
		}
	}

	@Test
	void missingPacksAreSkipped() throws Exception {
		Path jar = zip("client.jar", Map.of(STONE, "vanilla stone"));
		try (ZipStack stack = new ZipStack(jar.toFile(), List.of(directory.resolve("missing.zip").toFile()), null)) {
			assertTrue(stack.getResourcePackFiles().isEmpty());
			assertEquals("vanilla stone", read(stack, STONE));
		}
	}

	@Test
	void filterHidesFilesInLowerLayersOnly() throws Exception {
		Path jar = zip("client.jar", Map.of(STONE, "vanilla stone", DIRT, "vanilla dirt"));
		Path low = zip("low.zip", Map.of(STONE, "low stone"));
		Path high = zip("high.zip", Map.of(
				"pack.mcmeta", "{\"pack\":{\"pack_format\":34},\"filter\":{\"block\":[{\"namespace\":\"minecraft\",\"path\":\"block/stone\"}]}}"));
		Path top = zip("top.zip", Map.of(STONE, "top stone"));
		try (ZipStack stack = new ZipStack(jar.toFile(), List.of(high.toFile(), low.toFile()), List.of())) {
			assertNull(stack.getStream(STONE), "The filter hides stone in the pack below and in the Minecraft jar");
			assertFalse(stack.hasFile(STONE));
			assertEquals("vanilla dirt", read(stack, DIRT));
			assertFalse(stack.listFilesInDirectory("assets/minecraft/textures/block").contains(STONE));
		}
		try (ZipStack stack = new ZipStack(jar.toFile(), List.of(top.toFile(), high.toFile(), low.toFile()), List.of())) {
			assertEquals("top stone", read(stack, STONE), "A pack above the filter is not affected");
		}
	}

	@Test
	void overlaysApplyWhenGameFormatMatches() throws Exception {
		Path jar = zip("client.jar", Map.of(STONE, "vanilla stone",
				"version.json", "{\"name\":\"1.21.4\",\"pack_version\":{\"resource\":46,\"data\":61}}"));
		Path pack = zip("pack.zip", Map.of(
				"pack.mcmeta", """
						{"pack":{"pack_format":34},"overlays":{"entries":[
						 {"directory":"old","formats":[1,20]},
						 {"directory":"current","formats":{"min_inclusive":40,"max_inclusive":50}},
						 {"directory":"later","formats":[45,46]},
						 {"directory":"future","min_format":[70,0],"max_format":80}
						]}}""",
				STONE, "root stone",
				DIRT, "root dirt",
				"old/" + DIRT, "old dirt",
				"current/" + STONE, "current stone",
				"current/" + DIRT, "current dirt",
				"later/" + STONE, "later stone",
				"future/" + STONE, "future stone"));
		try (ZipStack stack = new ZipStack(jar.toFile(), List.of(pack.toFile()), List.of())) {
			assertEquals("later stone", read(stack, STONE), "Later overlay entries take priority");
			assertEquals("current dirt", read(stack, DIRT));
		}
	}

	@Test
	void overlayRangesUseMajorAndMinorFormats() {
		var mapper = tectonicus.util.FileUtils.getOBJECT_MAPPER();
		int[][] range = ResourcePack.overlayRange(mapper.readTree("{\"min_format\":[69,1],\"max_format\":70}"));
		assertArrayEquals(new int[] {69, 1}, range[0]);
		assertArrayEquals(new int[] {70, Integer.MAX_VALUE}, range[1]);
		range = ResourcePack.overlayRange(mapper.readTree("{\"formats\":42}"));
		assertArrayEquals(new int[] {42, 0}, range[0]);
		assertArrayEquals(new int[] {42, Integer.MAX_VALUE}, range[1]);
		assertNull(ResourcePack.overlayRange(mapper.readTree("{\"directory\":\"x\"}")));
	}

	@Test
	void metadataComesFromTheLayerThatProvidesTheTexture() throws Exception {
		Path jar = zip("client.jar", Map.of(STONE, "vanilla stone", STONE + ".mcmeta", "vanilla animation"));
		Path pack = zip("pack.zip", Map.of(STONE, "pack stone"));
		try (ZipStack stack = new ZipStack(jar.toFile(), List.of(pack.toFile()), List.of())) {
			assertNull(stack.getSiblingStream(STONE, STONE + ".mcmeta"), "A pack's still texture must not use vanilla animation");
			assertEquals("vanilla animation", read(stack, STONE + ".mcmeta"));
		}
	}

	@Test
	void versionDetectionIgnoresResourcePacks() throws Exception {
		Path jar = zip("client.jar", Map.of("pack.mcmeta", "{\"pack\":{\"pack_format\":4}}"));
		Path pack = zip("pack.zip", Map.of("pack.mcmeta", "{\"pack\":{\"pack_format\":3}}", "version.json", "{\"name\":\"9.9\"}"));
		try (ZipStack stack = new ZipStack(jar.toFile(), List.of(pack.toFile()), List.of())) {
			assertEquals("", tectonicus.Minecraft.getVersionJson(stack).getName());
			assertTrue(read(stack.getBaseStream("pack.mcmeta")).contains("4"));
		}
	}

	private static String read(ZipStack stack, String path) throws IOException {
		return read(stack.getStream(path));
	}

	private static String read(InputStream in) throws IOException {
		assertNotNull(in);
		try (in) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private Path zip(String name, Map<String, String> entries) throws IOException {
		Path path = directory.resolve(name);
		try (ZipOutputStream stream = new ZipOutputStream(Files.newOutputStream(path))) {
			for (var entry : entries.entrySet()) {
				stream.putNextEntry(new ZipEntry(entry.getKey()));
				stream.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
				stream.closeEntry();
			}
		}
		return path;
	}
}
