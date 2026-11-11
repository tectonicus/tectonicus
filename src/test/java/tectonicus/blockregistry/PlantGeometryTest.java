/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockregistry;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.BlockContext;
import tectonicus.Minecraft;
import tectonicus.blockregistry.BlockModel.BlockElement;
import tectonicus.blockregistry.BlockModel.BlockElement.ElementFace;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.LightStyle;
import tectonicus.itemregistry.GuiGeometry;
import tectonicus.itemregistry.GuiLighting;
import tectonicus.raw.RawChunk;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlantGeometryTest {
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
	@CsvSource({
			"0, 0, 0, 0, -0.25, -0.25",
			"-2, 2, 15, 0, 0.150000006, -0.216666669",
			"1874999, -1875000, 15, 1, -0.150000006, -0.0833333284"
	})
	void displacesResourcePackGeometryUsingWorldCoordinates(int chunkX, int chunkZ, int x, int z, float dx, float dz) {
		var vertices = render("minecraft:dandelion", chunkX, chunkZ, x, 3, z, false);
		assertEquals(4, vertices.size());
		assertEquals(x + 0.25f + dx, vertices.getFirst().x, 0.000002f);
		assertEquals(3.5f, vertices.getFirst().y);
		assertEquals(z + 0.25f + dz, vertices.getFirst().z, 0.000002f);
		assertEquals(0.5f, vertices.get(1).x - vertices.getFirst().x, 0.000002f);
	}

	@ParameterizedTest
	@ValueSource(strings = {"minecraft:sunflower", "minecraft:tall_grass", "minecraft:small_dripleaf", "minecraft:bamboo"})
	void stackedPlantPartsKeepTheSameDisplacement(String name) {
		Vector3f lower = render(name, -2, 2, 15, 3, 0, false).getFirst();
		Vector3f upper = render(name, -2, 2, 15, 4, 0, false).getFirst();
		assertEquals(lower.x, upper.x);
		assertEquals(lower.z, upper.z);
		assertEquals(1, upper.y - lower.y, 0.000001f);
		assertEquals(name.equals("minecraft:small_dripleaf") ? 3.4266667f : 3.5f, lower.y, 0.000001f);
	}

	@Test
	void shortGrassReceivesItsVerticalOffset() {
		assertEquals(3.3f, render("minecraft:short_grass", 0, 0, 0, 3, 0, false).getFirst().y, 0.000001f);
	}

	@Test
	void plantsWithoutOffsetsKeepTheirResourcePackPlacement() {
		assertEquals(new Vector3f(0.25f, 3.5f, 0.25f),
				render("minecraft:potted_dandelion", 0, 0, 0, 3, 0, false).getFirst());
	}

	@Test
	void inventoryGeometrySkipsPlantOffsets() {
		assertEquals(new Vector3f(0.25f, 3.5f, 0.25f),
				render("minecraft:dandelion", 0, 0, 0, 3, 0, true).getFirst());
	}

	private List<Vector3f> render(String name, int chunkX, int chunkZ, int x, int y, int z, boolean gui) {
		List<Vector3f> vertices = new ArrayList<>();
		Mesh mesh = (Mesh) Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class},
				(proxy, method, args) -> {
					if (method.getName().equals("addVertex"))
						vertices.add(new Vector3f((Vector3f) args[0]));
					else
						throw new AssertionError("Unexpected mesh call: " + method.getName());
					return null;
				});
		Rasteriser rasteriser = (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
				(proxy, method, args) -> mesh);
		Geometry geometry = gui
				? new GuiGeometry(rasteriser, new Matrix4f(), new GuiLighting(false, new Matrix4f()))
				: new Geometry(rasteriser);
		RawChunk rawChunk = new RawChunk() {
			@Override public ChunkCoord getChunkCoord() { return new ChunkCoord(chunkX, chunkZ); }
		};
		rawChunk.setBlockName(x, y, z, name);
		BlockStateWrapper plant = new BlockStateWrapper(name);
		plant.setFullBlock(false);
		plant.setFullOpaqueBlock(false);
		BlockStateWrapper air = new BlockStateWrapper("minecraft:air");
		air.setFullBlock(false);
		air.setFullOpaqueBlock(false);
		BlockContext world = (BlockContext) Proxy.newProxyInstance(BlockContext.class.getClassLoader(), new Class<?>[]{BlockContext.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "getBlock" -> (int) args[1] == x && (int) args[2] == y && (int) args[3] == z ? plant : air;
					case "getLightStyle" -> LightStyle.Day;
					case "isSmoothLit", "shouldCullSameBlockFaces", "useOldGrayscaleLighting" -> false;
					case "getLightSample" -> tectonicus.chunk.LightSample.ambient(1);
					case "getNightLightAdjustment" -> 0.0f;
					case "getLight" -> 1.0f;
					default -> throw new AssertionError("Unexpected world call: " + method.getName());
				});
		ElementFace face = new ElementFace(new SubTexture(null, 0, 0, 1, 1), null, 0, false, false, true);
		BlockElement element = new BlockElement(new tectonicus.util.Vector3f(4, 0, 4), new tectonicus.util.Vector3f(12, 8, 12),
				new Vector3f(8), 0, 0, 0, false, false, Map.of("up", face));
		// The block identity must drive offsets even when a pack replaces the model's name and shape.
		BlockModel model = new BlockModel("example:block/custom_shape", false, List.of(element));
		new BakedBlockStateModel(model, 0, 0, 0, false).createGeometry(x, y, z, world, rawChunk, geometry);
		return vertices;
	}
}
