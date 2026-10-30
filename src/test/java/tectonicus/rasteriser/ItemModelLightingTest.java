/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.rasteriser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.BlockContext;
import tectonicus.ItemRenderer.ItemContext;
import tectonicus.Minecraft;
import tectonicus.itemregistry.GuiGeometry;
import tectonicus.itemregistry.GuiLighting;
import tectonicus.blockTypes.Air;
import tectonicus.blockregistry.BakedBlockStateModel;
import tectonicus.blockregistry.BlockRegistry;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.blockregistry.LightingModelFixture;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.util.Vector3f;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemModelLightingTest {
	private int previousHeight;

	@BeforeEach
	void setChunkHeight() {
		previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(16);
	}

	@AfterEach
	void restoreChunkHeight() {
		Minecraft.setChunkHeight(previousHeight);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void unnamedInventoryAndBedModelsRenderWithoutLegacyRegistry(int z) {
		BlockRegistry registry = new BlockRegistry();
		BlockStateWrapper air = new BlockStateWrapper("minecraft:air");
		air.setFullBlock(false);
		air.setFullOpaqueBlock(false);
		registry.getBlockStates().put("minecraft:air", air);
		ItemContext context = new ItemContext(null, null, registry);
		List<org.joml.Vector4f> colours = new ArrayList<>();
		Mesh mesh = (Mesh) Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class},
				(proxy, method, args) -> {
					if (!method.getName().equals("addVertex"))
						throw new AssertionError("Unexpected mesh call: " + method.getName());
					colours.add(new org.joml.Vector4f((org.joml.Vector4f) args[1]));
					return null;
				});
		Rasteriser rasteriser = (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
				(proxy, method, args) -> {
					if (!method.getName().equals("createMesh"))
						throw new AssertionError("Unexpected rasteriser call: " + method.getName());
					return mesh;
				});

		model().createGeometry(0, 0, z, context, new RawChunk(), new Geometry(rasteriser));

		assertEquals(4, colours.size());
		for (org.joml.Vector4f colour : colours)
			assertEquals(new org.joml.Vector4f(1, 1, 1, 1), colour);
	}

	@Test
	void guiModelsSkipWorldLightingOcclusionAndCardinalShading() {
		List<org.joml.Vector4f> colours = new ArrayList<>();
		Mesh mesh = (Mesh) Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class},
				(proxy, method, args) -> {
					if (method.getName().equals("addVertex"))
						colours.add(new org.joml.Vector4f((org.joml.Vector4f) args[1]));
					return null;
				});
		Rasteriser rasteriser = (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
				(proxy, method, args) -> method.getName().equals("createMesh") ? mesh : null);
		BlockContext world = (BlockContext) Proxy.newProxyInstance(BlockContext.class.getClassLoader(), new Class<?>[]{BlockContext.class},
				(proxy, method, args) -> { throw new AssertionError("GUI models must not query the world: " + method.getName()); });
		var model = new BakedBlockStateModel(LightingModelFixture.face("north", new Vector3f(0, 0, 0),
				new Vector3f(16, 16, 16)).getModel(), 0, 0, 0, false);
		var pose = new org.joml.Matrix4f().rotateXYZ((float) Math.toRadians(30), (float) Math.toRadians(225), 0);
		model.createGeometry(0, 0, 0, world, new RawChunk(), new GuiGeometry(rasteriser, pose,
				new GuiLighting(true, new org.joml.Matrix4f())));
		assertEquals(4, colours.size());
		for (var colour : colours) {
			assertEquals(0.4f, colour.x, 0.00001f);
			assertEquals(0.4f, colour.y, 0.00001f);
			assertEquals(0.4f, colour.z, 0.00001f);
			assertEquals(1, colour.w);
		}
	}

	@Test
	void numericLegacyBlocksStillUseLegacyCulling() {
		RawChunk rawChunk = new RawChunk();
		rawChunk.setBlockId(0, 0, 0, 218);
		AtomicInteger lookups = new AtomicInteger();
		Air solid = new Air() {
			@Override
			public boolean isSolid() { return true; }
		};
		BlockContext context = (BlockContext) Proxy.newProxyInstance(BlockContext.class.getClassLoader(), new Class<?>[]{BlockContext.class},
				(proxy, method, args) -> {
					if (!method.getName().equals("getBlockType"))
						throw new AssertionError("Unexpected context call: " + method.getName());
					lookups.incrementAndGet();
					return solid;
				});

		model().createGeometry(0, 0, 0, context, rawChunk, null);

		assertEquals(6, lookups.get());
	}

	private BakedBlockStateModel model() {
		return new BakedBlockStateModel(LightingModelFixture.face("up", new Vector3f(0, 0, 0),
				new Vector3f(16, 16, 16)).getModel(), 0, 0, 0, false);
	}
}
