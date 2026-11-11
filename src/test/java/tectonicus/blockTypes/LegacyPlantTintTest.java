/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.blockTypes;

import org.joml.Vector4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.Minecraft;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyPlantTintTest {
	private int previousHeight;

	@BeforeEach
	void setUp() {
		previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(16);
	}

	@AfterEach
	void restoreHeight() {
		Minecraft.setChunkHeight(previousHeight);
	}

	@ParameterizedTest
	@CsvSource({"lily,0,4", "stem,0,8", "stem,7,8"})
	void rendersWithPlantTintWithoutAnExtractionCache(String plant, int growth, int vertexCount) {
		RawChunk chunk = new RawChunk();
		chunk.setBlockData(8, 8, 8, (byte)growth);
		Colour4f tint = new Colour4f(0.2f, 0.4f, 0.8f);
		BlockContext world = (BlockContext)Proxy.newProxyInstance(BlockContext.class.getClassLoader(),
				new Class<?>[]{BlockContext.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getGrassColor" -> {
						assertEquals("stem", plant, "Lily pads must not request a biome tint");
						assertEquals(chunk.getChunkCoord(), args[0]);
						assertEquals(8, args[1]);
						assertEquals(8, args[2]);
						assertEquals(8, args[3]);
						yield tint;
					}
					case "getLight" -> 0.5f;
					case "getNightLightAdjustment" -> 0f;
					case "getBlockId" -> 86;
					default -> throw new AssertionError("Unexpected world call: " + method.getName());
				});
		List<Vector4f> colors = new ArrayList<>();
		Mesh mesh = (Mesh)Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class},
				(proxy, method, args) -> {
					if (!method.getName().equals("addVertex"))
						throw new AssertionError("Unexpected mesh call: " + method.getName());
					colors.add(new Vector4f((Vector4f)args[1]));
					return null;
				});
		Rasteriser rasteriser = (Rasteriser)Proxy.newProxyInstance(Rasteriser.class.getClassLoader(),
				new Class<?>[]{Rasteriser.class}, (proxy, method, args) -> {
					if (!method.getName().equals("createMesh"))
						throw new AssertionError("Unexpected rasteriser call: " + method.getName());
					return mesh;
				});
		Texture texture = new Texture() {
			public int getWidth() { return 16; }
			public int getHeight() { return 16; }
		};
		SubTexture sprite = new SubTexture(texture, 0, 0, 1, 1);
		BlockType type = plant.equals("lily") ? new Lilly("Lily Pad", sprite)
				: new FruitStem("Pumpkin Stem", 86, sprite, sprite);
		Geometry geometry = new Geometry(rasteriser);
		type.addEdgeGeometry(8, 8, 8, world, null, chunk, geometry);
		assertEquals(vertexCount, colors.size());
		Vector4f expected = plant.equals("lily")
				? new Vector4f(16 / 255.0f, 64 / 255.0f, 24 / 255.0f, 1)
				: new Vector4f(0.1f, 0.2f, 0.4f, 1);
		for (Vector4f color : colors)
			assertEquals(expected, color);
		assertEquals(0.2f, tint.r, "Lighting must not mutate the world's shared tint");
		assertEquals(0.4f, tint.g);
		assertEquals(0.8f, tint.b);

		colors.clear();
		tint.r = tint.g = tint.b = 1;
		type.addEdgeGeometry(8, 8, 8, world, null, chunk, geometry);
		assertEquals(vertexCount, colors.size());
		for (Vector4f color : colors)
			assertEquals(plant.equals("lily") ? expected : new Vector4f(0.5f, 0.5f, 0.5f, 1), color);
	}
}
