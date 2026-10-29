/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockregistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.texture.ZipStack;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class BlockRegistryResourcePackTest {
	@TempDir Path directory;
	private final ObjectMapper mapper = new ObjectMapper();

	@ParameterizedTest
	@ValueSource(strings = {"shroom_b/inside/block", "minecraft:shroom_r/top/block", "block/test", "test"})
	void loadsModelsRelativeToModelsDirectory(String modelId) throws Exception {
		String path = modelId.replace("minecraft:", "");
		Path base = zip("client.jar", Map.of());
		Path pack = zip("pack.zip", Map.of(
				"assets/minecraft/models/" + path + ".json", "{\"parent\":\"templates/face\"}",
				"assets/minecraft/models/templates/face.json", "{\"elements\":[]}"));
		ZipStack stack = new ZipStack(base.toFile(), pack.toFile(), List.of());
		try {
			BlockRegistry registry = new BlockRegistry();
			var field = BlockRegistry.class.getDeclaredField("zips");
			field.setAccessible(true);
			field.set(registry, stack);

			BlockModel model = registry.loadModel(modelId);

			assertNotNull(model);
			assertEquals(path, model.getName());
			assertSame(model, registry.loadModel(modelId));
		} finally {
			for (String fieldName : List.of("base", "override")) {
				var field = ZipStack.class.getDeclaredField(fieldName);
				field.setAccessible(true);
				((FileSystem) field.get(stack)).close();
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"stone", "minecraft:stone", "custom/stone"})
	void fallsBackToBlockDirectoryForLegacyModelReferences(String modelId) throws Exception {
		String path = "block/" + modelId.replace("minecraft:", "");
		Path base = zip("client.jar", Map.of("assets/minecraft/models/" + path + ".json", "{\"elements\":[]}"));
		Path pack = zip("pack.zip", Map.of("assets/minecraft/models/" + path + ".json", "{\"ambientocclusion\":false,\"elements\":[]}"));
		withRegistry(base, pack, registry -> {
			BlockModel model = registry.loadModel(modelId);
			assertNotNull(model);
			assertEquals(path, model.getName());
			assertFalse(model.isAmbientlyOccluded(), "The legacy lookup still honors the resource-pack override");
			assertSame(model, registry.loadModel(path), "Legacy and explicit references share the cached model");
		});
	}

	@Test
	void prefersExactResourcePackPathOverLegacyFallbackInMinecraftJar() throws Exception {
		Path base = zip("client.jar", Map.of("assets/minecraft/models/block/custom/stone.json", "{\"elements\":[]}"));
		Path pack = zip("pack.zip", Map.of("assets/minecraft/models/custom/stone.json", "{\"ambientocclusion\":false,\"elements\":[]}"));
		withRegistry(base, pack, registry -> {
			BlockModel model = registry.loadModel("custom/stone");
			assertNotNull(model);
			assertEquals("custom/stone", model.getName());
			assertFalse(model.isAmbientlyOccluded());
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"bottom\":\"#top\",\"top\":\"block/spawner_top\",\"intop\":\"#inside\",\"inside\":\"block/spawner_inner\"}",
			"{\"inside\":\"block/spawner_inner\",\"top\":\"block/spawner_top\",\"intop\":\"#inside\",\"bottom\":\"#top\"}"
	})
	void resolvesSiblingAliasesRegardlessOfDeclarationOrder(String json) throws Exception {
		Map<String, ModelTexture> textures = BlockRegistry.populateTextureMap(Map.of(), mapper.readTree(json));
		Map<String, ModelTexture> resolved = BlockRegistry.resolveTextureMap(textures);

		assertEquals(new ModelTexture("block/spawner_top"), resolved.get("bottom"));
		assertEquals(new ModelTexture("block/spawner_inner"), resolved.get("intop"));
	}

	@Test
	void resolvesAliasChainsAfterInheritanceAndKeepsChildOverrides() throws Exception {
		Map<String, ModelTexture> textures = BlockRegistry.populateTextureMap(Map.of(), mapper.readTree("""
				{"side":"block/custom", "bottom":"#top"}
				"""));
		textures = BlockRegistry.populateTextureMap(textures, mapper.readTree("""
				{"top":"#side", "side":"block/parent", "particle":"#bottom"}
				"""));
		Map<String, ModelTexture> resolved = BlockRegistry.resolveTextureMap(textures);

		ModelTexture custom = new ModelTexture("block/custom");
		assertEquals(Map.of("side", custom, "bottom", custom, "top", custom, "particle", custom), resolved);
	}

	@Test
	void unresolvedAndCyclicAliasesDoNotPreventOtherTexturesFromLoading() throws Exception {
		Map<String, ModelTexture> textures = BlockRegistry.populateTextureMap(Map.of(), mapper.readTree("""
				{"a":"#b", "b":"#a", "self":"#self", "missing":"#absent", "side":"block/stone"}
				"""));
		Map<String, ModelTexture> resolved = BlockRegistry.resolveTextureMap(textures);

		assertEquals(Map.of("side", new ModelTexture("block/stone")), resolved);
	}

	@Test
	void supportsSpriteObjectsWhenResolvingAliases() throws Exception {
		Map<String, ModelTexture> textures = BlockRegistry.populateTextureMap(Map.of(), mapper.readTree("""
				{"bottom":"#top", "top":{"sprite":"minecraft:block/stone", "force_translucent":true}}
				"""));

		assertEquals(new ModelTexture("minecraft:block/stone", true), BlockRegistry.resolveTextureMap(textures).get("bottom"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"\"block/stone\"", "{\"sprite\":\"block/stone\"}",
			"{\"sprite\":\"block/stone\",\"force_translucent\":false}"})
	void childTextureOverridesReplaceParentTranslucency(String childTexture) throws Exception {
		Map<String, ModelTexture> textures = BlockRegistry.populateTextureMap(Map.of(), mapper.readTree(
				"{\"side\":" + childTexture + ",\"bottom\":\"#top\"}"));
		textures = BlockRegistry.populateTextureMap(textures, mapper.readTree("""
				{"side":{"sprite":"block/stone","force_translucent":true}, "top":"#side"}
				"""));

		assertEquals(new ModelTexture("block/stone", false), BlockRegistry.resolveTextureMap(textures).get("bottom"));
	}

	@Test
	void inheritedAliasesRetainChildTranslucency() throws Exception {
		Map<String, ModelTexture> textures = BlockRegistry.populateTextureMap(Map.of(), mapper.readTree("""
				{"side":{"sprite":"block/stone","force_translucent":true}, "bottom":"#top"}
				"""));
		textures = BlockRegistry.populateTextureMap(textures, mapper.readTree("""
				{"top":"#side", "side":"block/parent", "particle":"#bottom"}
				"""));

		ModelTexture forced = new ModelTexture("block/stone", true);
		assertEquals(Map.of("side", forced, "top", forced, "bottom", forced, "particle", forced),
				BlockRegistry.resolveTextureMap(textures));
	}

	private Path zip(String name, Map<String, String> entries) throws Exception {
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

	private void withRegistry(Path base, Path pack, RegistryCheck check) throws Exception {
		ZipStack stack = new ZipStack(base.toFile(), pack.toFile(), List.of());
		try {
			BlockRegistry registry = new BlockRegistry();
			var field = BlockRegistry.class.getDeclaredField("zips");
			field.setAccessible(true);
			field.set(registry, stack);
			check.run(registry);
		} finally {
			for (String name : List.of("base", "override")) {
				var field = ZipStack.class.getDeclaredField(name);
				field.setAccessible(true);
				((FileSystem) field.get(stack)).close();
			}
		}
	}

	@FunctionalInterface
	private interface RegistryCheck {
		void run(BlockRegistry registry) throws Exception;
	}
}
