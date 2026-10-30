/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */
package tectonicus.itemregistry;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

class ItemGuiTransformTest {
	@TempDir Path directory;

	@Test void resolvesLegacyItemAndModernBlockParentChains() throws Exception {
		withModels(Map.of(
				"item/oak_stairs", "{\"parent\":\"block/oak_stairs\"}",
				"block/oak_stairs", "{\"parent\":\"minecraft:block/stairs\"}",
				"block/stairs", "{\"parent\":\"block/block\",\"display\":{\"gui\":{\"rotation\":[30,135,0]}}}",
				"block/block", "{\"display\":{\"gui\":{\"rotation\":[30,225,0]}}}"), zips -> {
			Matrix4f legacy = ItemGuiTransform.load(zips, "item/oak_stairs");
			assertEquals(legacy, ItemGuiTransform.load(zips, "minecraft:block/oak_stairs"));
			// Stairs must expose west and north; the old fixed camera exposed east and north.
			Vector3f towardViewer = legacy.invert(new Matrix4f()).transformDirection(new Vector3f(0, 0, 1));
			assertTrue(towardViewer.x < 0 && towardViewer.y > 0 && towardViewer.z < 0);
		});
	}

	@Test void childGuiReplacesParentEntryRatherThanAccumulatingTransforms() throws Exception {
		withModels(Map.of(
				"item/child", "{\"parent\":\"item/parent\",\"display\":{\"gui\":{\"translation\":[16,0,0]}}}",
				"item/parent", "{\"display\":{\"gui\":{\"rotation\":[30,45,0],\"scale\":[0.25,0.25,0.25]}}}"), zips -> {
			Matrix4f pose = ItemGuiTransform.load(zips, "item/child");
			assertEquals(new Vector3f(1, 0, 0), pose.transformPosition(new Vector3f(0.5f)));
			assertEquals(new Vector3f(1, 0, 0), pose.transformDirection(new Vector3f(1, 0, 0)));
		});
	}

	@Test void appliesScaleThenXyzRotationThenTranslationAroundModelCenter() {
		Matrix4f pose = ItemGuiTransform.fromJson(new ObjectMapper().readTree("""
				{"rotation":[0,0,90],"translation":[0,8,0],"scale":[2,1,1]}
				"""));
		assertTrue(new Vector3f(0, 1.5f, 0).equals(pose.transformPosition(new Vector3f(1, 0.5f, 0.5f)), 0.00001f));
	}

	@Test void missingGuiUsesIdentityAndCyclicParentsTerminate() throws Exception {
		withModels(Map.of("item/a", "{\"parent\":\"item/b\"}", "item/b", "{\"parent\":\"item/a\"}"), zips -> {
			assertEquals(new Vector3f(), ItemGuiTransform.load(zips, "item/a").transformPosition(new Vector3f(0.5f)));
		});
	}

	@Test void guiLightIsInheritedIndependentlyOfTheDisplayPose() throws Exception {
		withModels(Map.of(
				"item/parent", "{\"gui_light\":\"front\"}",
				"item/child", "{\"parent\":\"item/parent\",\"display\":{\"gui\":{\"rotation\":[30,45,0]}}}",
				"item/override", "{\"parent\":\"item/child\",\"gui_light\":\"side\"}",
				"item/legacy", "{\"parent\":\"builtin/entity\"}"), zips -> {
			assertFalse(ItemGuiTransform.isSideLit(zips, "item/child"));
			assertTrue(ItemGuiTransform.isSideLit(zips, "item/override"));
			assertTrue(ItemGuiTransform.isSideLit(zips, "item/legacy"), "1.13 models omit gui_light");
		});
	}

	private void withModels(Map<String, String> models, CheckedConsumer test) throws Exception {
		Path jar = directory.resolve("client.jar");
		try (var out = new ZipOutputStream(Files.newOutputStream(jar))) {
			for (var entry : models.entrySet()) {
				out.putNextEntry(new ZipEntry("assets/minecraft/models/" + entry.getKey() + ".json"));
				out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
				out.closeEntry();
			}
		}
		ZipStack zips = new ZipStack(jar.toFile(), null, List.of());
		try { test.accept(zips); }
		finally {
			var base = ZipStack.class.getDeclaredField("base");
			base.setAccessible(true);
			((FileSystem) base.get(zips)).close();
		}
	}

	private interface CheckedConsumer { void accept(ZipStack zips) throws Exception; }
}
