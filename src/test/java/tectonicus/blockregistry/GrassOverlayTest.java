/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockregistry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.BlockContext;
import tectonicus.Minecraft;
import tectonicus.blockregistry.BlockModel.BlockElement;
import tectonicus.blockregistry.BlockModel.BlockElement.ElementFace;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.renderer.Geometry.MeshType;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;
import tectonicus.util.Vector3f;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GrassOverlayTest {
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
	@ValueSource(booleans = {false, true})
	void grassSidesKeepTheirTransparencyWhenTopSharesTheOverlayElement(boolean topWithOverlay) {
		BakedBlockStateModel baked = grass(topWithOverlay);
		Map<MeshType, Integer> vertices = renderVertices(baked);

		assertEquals(16, vertices.get(MeshType.AlphaTest), "Four side overlays must discard transparent pixels");
		assertEquals(24, vertices.get(MeshType.Solid), "Dirt and grass top remain opaque");
		assertFalse(vertices.containsKey(MeshType.Transparent));
		assertEquals(4, baked.getQuads().stream().filter(BakedBlockStateModel.BakedQuad::isGrassOverlay).count());
		for (var quad : baked.getQuads()) {
			if (quad.getSourceFace().equals("north"))
				assertEquals(quad.isGrassOverlay() ? -0.0001f : 0, quad.getPosition(0, 2), 0.000001f);
			if (quad.getSourceFace().equals("up")) {
				assertFalse(quad.isGrassOverlay());
				assertEquals(1, quad.getPosition(0, 1));
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void grassTopDecorationsKeepTheirTextureTransparency(boolean translucent) {
		List<BlockElement> elements = new ArrayList<>(grass(false).getSourceModel().getElements());
		ElementFace decoration = new ElementFace(new SubTexture(null, 0, 0, 1, 1), null, 0,
				false, translucent, true);
		elements.add(new BlockElement(new Vector3f(0, 16.2f, 0), new Vector3f(16, 16.2f, 16),
				new org.joml.Vector3f(8, 8, 8), 0, 0, 0, false, true,
				Map.of("up", decoration, "down", decoration)));
		BlockModel model = new BlockModel("block/soil/grass/grass_block_rock1", true, elements);

		Map<MeshType, Integer> vertices = renderVertices(new BakedBlockStateModel(model, 0, 0, 0, false));

		assertEquals(24, vertices.get(MeshType.Solid), "Only dirt and the opaque grass top use the solid pass");
		assertEquals(translucent ? 16 : 24, vertices.get(MeshType.AlphaTest));
		if (translucent)
			assertEquals(8, vertices.get(MeshType.Transparent));
		else
			assertFalse(vertices.containsKey(MeshType.Transparent));
	}

	private Map<MeshType, Integer> renderVertices(BakedBlockStateModel baked) {
		Map<MeshType, Integer> vertices = new EnumMap<>(MeshType.class);
		Mesh unused = mesh(MeshType.Solid, new EnumMap<>(MeshType.class));
		Rasteriser rasteriser = (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
				(proxy, method, args) -> unused);
		Geometry geometry = new Geometry(rasteriser) {
			@Override
			public Mesh getMesh(Texture texture, MeshType type) {
				return mesh(type, vertices);
			}
		};
		BlockStateWrapper grass = new BlockStateWrapper("minecraft:grass_block");
		BlockStateWrapper air = new BlockStateWrapper("minecraft:air");
		air.setFullBlock(false);
		air.setFullOpaqueBlock(false);
		BlockContext world = (BlockContext) Proxy.newProxyInstance(BlockContext.class.getClassLoader(), new Class<?>[]{BlockContext.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "getBlock" -> (int) args[1] == 0 && (int) args[2] == 0 && (int) args[3] == 0 ? grass : air;
					case "getLightStyle" -> LightStyle.Day;
					case "isSmoothLit", "shouldCullSameBlockFaces" -> false;
					case "getNightLightAdjustment" -> 0.0f;
					case "getLight" -> 1.0f;
					case "getGrassColor" -> new Colour4f(0.4f, 0.8f, 0.2f, 1);
					default -> throw new AssertionError("Unexpected world call: " + method.getName());
				});

		baked.createGeometry(0, 0, 0, world, new RawChunk(), geometry);
		return vertices;
	}

	@Test
	void opaqueFourSidedGrassElementsStayOnTheBlockBoundary() {
		ElementFace side = face(false, false);
		BlockModel model = new BlockModel("block/grass_block", true, List.of(element(Map.of(
				"north", side, "south", side, "east", side, "west", side))));
		for (var quad : new BakedBlockStateModel(model, 0, 0, 0, false).getQuads()) {
			assertFalse(quad.isGrassOverlay());
			if (quad.getSourceFace().equals("north"))
				assertEquals(0, quad.getPosition(0, 2));
		}
	}

	private BakedBlockStateModel grass(boolean topWithOverlay) {
		Map<String, ElementFace> body = new HashMap<>();
		Map<String, ElementFace> overlay = new HashMap<>();
		for (String side : List.of("north", "south", "east", "west")) {
			body.put(side, face(false, false));
			overlay.put(side, face(true, true));
		}
		body.put("down", face(false, false));
		(topWithOverlay ? overlay : body).put("up", face(true, false));
		BlockModel model = new BlockModel("block/grass_block", true, List.of(element(body), element(overlay)));
		return new BakedBlockStateModel(model, 0, 0, 0, false);
	}

	private BlockElement element(Map<String, ElementFace> faces) {
		return new BlockElement(new Vector3f(0, 0, 0), new Vector3f(16, 16, 16),
				new org.joml.Vector3f(8, 8, 8), 0, 0, 0, false, true, faces);
	}

	private ElementFace face(boolean tinted, boolean transparent) {
		return new ElementFace(new SubTexture(null, 0, 0, 1, 1), null, 0, tinted, false, transparent);
	}

	private Mesh mesh(MeshType type, Map<MeshType, Integer> vertices) {
		return (Mesh) Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class}, (proxy, method, args) -> {
			if (method.getName().equals("addVertex")) {
				vertices.merge(type, 1, Integer::sum);
				return null;
			}
			throw new AssertionError("Unexpected mesh call: " + method.getName());
		});
	}
}
