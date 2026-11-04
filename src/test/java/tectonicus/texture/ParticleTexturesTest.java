/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.texture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.Version;
import tectonicus.rasteriser.Texture;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ParticleTexturesTest {
	@TempDir Path directory;
	private final List<ZipStack> stacks = new ArrayList<>();
	private final SubTexture sprite = new SubTexture(new Texture() {
		@Override public int getWidth() { return 8; }
		@Override public int getHeight() { return 8; }
	}, 0, 0, 1, 1);

	@Test
	void resourcePackReplacesParticleDefinitionAndResolvesNamespacedSprites() throws Exception {
		Path base = zip("client.jar", Map.of("assets/minecraft/particles/current_down.json", "{\"textures\":[\"minecraft:bubble\"]}"));
		Path pack = zip("pack.zip", Map.of("assets/minecraft/particles/current_down.json", "{\"textures\":[\"custom:ring/one\",\"ring_two\"]}"));
		ZipStack stack = openStack(base, pack);
		List<String> requests = new ArrayList<>();
		List<SubTexture> sprites = TexturePack.loadParticleTextures(stack, "minecraft:current_down", path -> {
			requests.add(path);
			return sprite;
		});
		assertEquals(List.of("assets/custom/textures/particle/ring/one.png", "assets/minecraft/textures/particle/ring_two.png"), requests);
		assertEquals(List.of(sprite, sprite), sprites);
	}

	@Test
	void missingMalformedAndEmptyDefinitionsAreSafe() throws Exception {
		Path base = zip("client.jar", Map.of(
				"assets/minecraft/particles/broken.json", "{invalid",
				"assets/minecraft/particles/empty.json", "{\"textures\":[]}",
				"assets/minecraft/particles/no_textures.json", "{}"));
		ZipStack stack = openStack(base, null);
		for (String id : List.of("missing", "broken", "empty", "no_textures"))
			assertTrue(TexturePack.loadParticleTextures(stack, id, path -> {
				fail("No texture should be loaded for " + id);
				return null;
			}).isEmpty());
	}

	@Test
	void missingSpriteDoesNotDiscardOtherSprites() throws Exception {
		Path base = zip("client.jar", Map.of("assets/custom/particles/fizz.json", "{\"textures\":[\"missing\",\"bubble\"]}"));
		ZipStack stack = openStack(base, null);
		assertEquals(List.of(sprite), TexturePack.loadParticleTextures(stack, "custom:fizz",
				path -> path.endsWith("/bubble.png") ? sprite : null));
	}

	@ParameterizedTest
	@ValueSource(ints = {256, 512})
	void legacyColumnsUseTheBubbleCellOfTheParticleAtlasAtAnyResolution(int resolution) throws Exception {
		Path base = zip("client.jar", Map.of("assets/minecraft/textures/particle/particles.png", "atlas"));
		ZipStack stack = openStack(base, null);
		SubTexture atlas = new SubTexture(new Texture() {
			@Override public int getWidth() { return resolution; }
			@Override public int getHeight() { return resolution; }
		}, 0, 0, 1, 1);
		for (String id : List.of("minecraft:bubble_column_up", "minecraft:current_down")) {
			List<String> requests = new ArrayList<>();
			List<SubTexture> sprites = TexturePack.loadParticleTextures(stack, id, path -> {
				requests.add(path);
				return atlas;
			});
			assertEquals(List.of("assets/minecraft/textures/particle/particles.png"), requests);
			assertEquals(1, sprites.size());
			SubTexture bubble = sprites.getFirst();
			assertSame(atlas.texture, bubble.texture);
			assertEquals(0, bubble.u0);
			assertEquals(2 / 32.0f, bubble.v0);
			assertEquals(0.999f / 32, bubble.u1, 0.0000001f);
			assertEquals((2 + 0.999f) / 32, bubble.v1, 0.0000001f);
		}
	}

	@Test
	void legacyAtlasUsesResourcePackOverride() throws Exception {
		String atlasPath = "assets/minecraft/textures/particle/particles.png";
		Path base = zip("client.jar", Map.of(atlasPath, "vanilla"));
		Path pack = zip("pack.zip", Map.of(atlasPath, "custom"));
		ZipStack stack = openStack(base, pack);
		List<SubTexture> sprites = TexturePack.loadParticleTextures(stack, "minecraft:current_down", path -> {
			try (var stream = stack.getStream(path)) {
				assertEquals("custom", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
				return sprite;
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		});
		assertEquals(1, sprites.size());
	}

	@Test
	void modernDefinitionsTakePrecedenceAndDoNotFallBackToAnAtlas() throws Exception {
		Path base = zip("client.jar", Map.of(
				"assets/minecraft/textures/particle/particles.png", "atlas",
				"assets/minecraft/particles/bubble_column_up.json", "{\"textures\":[\"custom:bubble\"]}",
				"assets/minecraft/particles/current_down.json", "{\"textures\":[]}"));
		ZipStack stack = openStack(base, null);
		assertEquals(List.of(sprite), TexturePack.loadParticleTextures(stack, "minecraft:bubble_column_up", path -> {
			assertEquals("assets/custom/textures/particle/bubble.png", path);
			return sprite;
		}));
		assertTrue(TexturePack.loadParticleTextures(stack, "minecraft:current_down", path -> {
			fail("An intentionally empty definition must not restore legacy bubbles");
			return null;
		}).isEmpty());
	}

	@Test
	void missingLegacyAtlasAndUnrelatedParticleIdsAreSafe() throws Exception {
		Path base = zip("client.jar", Map.of());
		ZipStack stack = openStack(base, null);
		assertTrue(TexturePack.loadParticleTextures(stack, "minecraft:current_down", path -> null).isEmpty());
		for (String id : List.of("custom:current_down", "minecraft:splash"))
			assertTrue(TexturePack.loadParticleTextures(stack, id, path -> {
				fail("Only vanilla bubble-column particles have this legacy fallback");
				return null;
			}).isEmpty());
	}

	@ParameterizedTest
	@CsvSource({
		"VERSION_4,particles.png,16", "VERSION_5,particles.png,16",
		"VERSION_12,assets/minecraft/textures/particle/particles.png,16",
		"VERSION_13,assets/minecraft/textures/particle/particles.png,32"
	})
	void legacyFlamesUseTheCorrectAtlasCellAtAnyResolution(Version version, String path, int grid) throws Exception {
		Path base = zip("client.jar", Map.of(path, "atlas"));
		ZipStack stack = openStack(base, null);
		for (int scale : List.of(1, 4)) {
			SubTexture atlas = new SubTexture(new Texture() {
				@Override public int getWidth() { return grid * 8 * scale; }
				@Override public int getHeight() { return grid * 8 * scale; }
			}, 0, 0, 1, 1, version);
			List<SubTexture> sprites = TexturePack.loadParticleTextures(stack, "minecraft:flame", requested -> {
				assertEquals(path, requested.startsWith("/") ? requested.substring(1) : requested);
				return atlas;
			});
			assertEquals(1, sprites.size());
			SubTexture flame = sprites.getFirst();
			assertSame(atlas.texture, flame.texture);
			assertEquals(0, flame.u0);
			assertEquals(3f / grid, flame.v0);
			assertEquals(0.999f / grid, flame.u1, 0.0000001f);
			assertEquals(3.999f / grid, flame.v1, 0.0000001f);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"flame", "smoke", "dust"})
	void torchParticleDefinitionsOverrideTheLegacyAtlasEvenWhenEmpty(String particle) throws Exception {
		Path base = zip("client.jar", Map.of("assets/minecraft/textures/particle/particles.png", "atlas"));
		Path pack = zip("pack.zip", Map.of("assets/minecraft/particles/" + particle + ".json", "{\"textures\":[]}"));
		ZipStack stack = openStack(base, pack);
		assertTrue(TexturePack.loadParticleTextures(stack, "minecraft:" + particle, requested -> {
			fail("An empty particle definition must suppress the legacy atlas");
			return null;
		}).isEmpty());
	}

	@ParameterizedTest
	@CsvSource({"VERSION_4,particles.png,16", "VERSION_12,assets/minecraft/textures/particle/particles.png,16",
		"VERSION_13,assets/minecraft/textures/particle/particles.png,32"})
	void legacySmokeAndDustUseEightSpritesInAgeOrder(Version version, String path, int grid) throws Exception {
		Path base = zip("client.jar", Map.of(path, "atlas"));
		Path pack = zip("pack.zip", Map.of(path, "custom atlas"));
		ZipStack stack = openStack(base, pack);
		for (String particle : List.of("smoke", "dust")) {
			List<SubTexture> sprites = TexturePack.loadParticleTextures(stack, "minecraft:" + particle, requested -> {
				try (var stream = stack.getStream(requested)) {
					assertEquals("custom atlas", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
				} catch (Exception e) {
					throw new AssertionError(e);
				}
				return new SubTexture(sprite.texture, 0, 0, 1, 1, version);
			});
			assertEquals(8, sprites.size());
			for (int i = 0; i < 8; i++) {
				SubTexture frame = sprites.get(i);
				assertSame(sprite.texture, frame.texture);
				assertEquals((7f - i) / grid, frame.u0, 0.0000001f);
				assertEquals((7.999f - i) / grid, frame.u1, 0.0000001f);
				assertEquals(0, frame.v0);
				assertEquals(0.999f / grid, frame.v1, 0.0000001f);
			}
		}
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

	private ZipStack openStack(Path base, Path override) throws Exception {
		ZipStack stack = new ZipStack(base.toFile(), override == null ? null : override.toFile(), List.of());
		stacks.add(stack);
		return stack;
	}

	@AfterEach
	void closeStacks() throws Exception {
		// ZipStack owns its file systems but does not currently expose a close method.
		for (ZipStack stack : stacks) {
			for (String name : List.of("base", "override")) {
				var field = ZipStack.class.getDeclaredField(name);
				field.setAccessible(true);
				FileSystem fileSystem = (FileSystem) field.get(stack);
				if (fileSystem != null)
					fileSystem.close();
			}
		}
	}
}
