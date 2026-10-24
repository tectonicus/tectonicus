/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.Version;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.Texture;
import tectonicus.raw.BlockProperties;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WaterTest extends FluidTestFixture {
	private final Texture overlay = texture(16, 16);
	private final Water water = new Water("Water", sprite(still), sprite(flowing), sprite(overlay), 0);

	@Test
	void sourcePoolUsesStillTextureBiomeTintAndTransparentMesh() {
		for (int x = 7; x <= 9; x++)
			for (int z = 7; z <= 9; z++)
				putWater(x, 8, z, 0, false);
		putSolid(8, 7, 8);
		render(8, 8, 8);
		assertTrue(water.isWater());
		assertFalse(water.isSolid());
		assertEquals(Set.of(Geometry.MeshType.Transparent), meshTypes);
		assertEquals(8, vertices.get(still).size());
		assertTrue(vertices.getOrDefault(flowing, List.of()).isEmpty());
		for (Vertex vertex : top(still)) {
			assertEquals(8 + 8 / 9.0f - 0.001f, vertex.position().y, 0.00001f);
			assertEquals(new Vector4f(0.15f, 0.3f, 0.6f, 0.95f), vertex.colour());
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
	void modernAndLegacyLevelsHaveMinecraftHeights(int level) {
		for (boolean legacy : new boolean[]{false, true}) {
			cells.clear();
			putWater(8, 8, 8, level, legacy);
			basin(8, 8, 8);
			render(8, 8, 8);
			assertEquals(8, vertices.values().stream().mapToInt(List::size).sum());
			float expected = 8 + (level >= 8 ? 8 : 8 - level) / 9.0f - 0.001f;
			for (Vertex vertex : top(still))
				assertEquals(expected, vertex.position().y, 0.00001f);
		}
	}

	@Test
	void slopingSurfaceUsesTheSameSourceWeightingAsLava() {
		putWater(7, 8, 8, 0, false);
		putWater(8, 8, 8, 2, false);
		putWater(9, 8, 8, 6, false);
		for (int x = 7; x <= 10; x++) {
			putSolid(x, 8, 7);
			putSolid(x, 8, 9);
		}
		render(8, 8, 8);
		assertEquals(8 + (10 * 8 / 9.0f + 6 / 9.0f) / 11 - 0.001f, top(flowing).get(0).position().y, 0.00001f);
		assertEquals(8 + 4 / 9.0f - 0.001f, top(flowing).get(1).position().y, 0.00001f);
	}

	@ParameterizedTest
	@CsvSource({"15,16,false", "15,16,true", "0,-1,false", "0,-1,true"})
	void surfacesJoinAcrossBothChunkEdges(int x, int neighbor, boolean legacy) {
		putWater(x, 8, 8, 2, legacy);
		putWater(neighbor, 8, 8, 6, legacy);
		render(x, 8, 8);
		List<Vertex> first = List.copyOf(top(flowing));
		render(neighbor, 8, 8);
		if (neighbor > x) {
			assertEquals(first.get(1).position().y, top(flowing).get(0).position().y, 0.00001f);
			assertEquals(first.get(2).position().y, top(flowing).get(3).position().y, 0.00001f);
		} else {
			assertEquals(first.get(0).position().y, top(flowing).get(1).position().y, 0.00001f);
			assertEquals(first.get(3).position().y, top(flowing).get(2).position().y, 0.00001f);
		}
	}

	@ParameterizedTest
	@CsvSource({"1,0,0.75,0.25", "-1,0,0.25,0.75", "0,1,0.25,0.25", "0,-1,0.75,0.75"})
	void currentRotatesTheStaticFlowTexture(int dx, int dz, float u, float v) {
		putWater(8, 8, 8, 0, false);
		putWater(8 + dx, 8, 8 + dz, 4, false);
		render(8, 8, 8);
		assertEquals(u, top(flowing).getFirst().u(), 0.00001f);
		assertEquals(v / 20, top(flowing).getFirst().v(), 0.00001f);
	}

	@Test
	void waterfallHasFullHeightSidesAndNoInternalFaces() {
		putWater(8, 8, 8, 8, false);
		putWater(8, 9, 8, 8, false);
		putWater(8, 7, 8, 8, false);
		render(8, 8, 8);
		assertFalse(vertices.containsKey(still));
		assertEquals(32, vertices.get(flowing).size());
		assertEquals(9, vertices.get(flowing).get(0).position().y);
		assertEquals(8, vertices.get(flowing).get(2).position().y);
	}

	@Test
	void legacyDepthOptionOnlyAddsFaintInternalTopFaces() {
		for (Position position : List.of(new Position(8, 8, 8), new Position(8, 9, 8), new Position(8, 7, 8),
				new Position(7, 8, 8), new Position(9, 8, 8), new Position(8, 8, 7), new Position(8, 8, 9)))
			putWater(position.x(), position.y(), position.z(), 0, false);
		render(8, 8, 8);
		assertEquals(0, vertices.values().stream().mapToInt(List::size).sum());
		legacyWater = true;
		render(8, 8, 8);
		assertEquals(4, vertices.get(still).size());
		for (Vertex vertex : top(still)) {
			assertEquals(0.3f, vertex.colour().w);
			assertEquals(8.999f, vertex.position().y, 0.00001f);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"minecraft:oak_stairs", "minecraft:kelp", "minecraft:kelp_plant", "minecraft:seagrass", "minecraft:tall_seagrass"})
	void waterloggedBlocksAndPlantsContainSourceWater(String name) {
		putWater(8, 8, 8, 6, false);
		basin(8, 8, 8);
		putSolid(7, 8, 7);
		putSolid(7, 8, 9);
		Map<String, String> properties = name.endsWith("stairs") ? Map.of("waterlogged", "true", "level", "7") : Map.of();
		cells.put(new Position(7, 8, 8), new Cell(new Air(name), null, new BlockProperties(properties), 5));
		render(8, 8, 8);
		assertEquals(8, vertices.get(flowing).size(), "Only the double-sided top should remain");
		assertEquals(8 + (10 * 8 / 9.0f + 2 / 9.0f) / 11 - 0.001f, top(flowing).getFirst().position().y, 0.00001f);
	}

	@Test
	void dryPicklesAndCoralDoNotHideWaterBoundaries() {
		putWater(8, 8, 8, 0, false);
		basin(8, 8, 8);
		cells.put(new Position(7, 8, 8), new Cell(new Air("minecraft:sea_pickle"), null, new BlockProperties(Map.of("waterlogged", "false")), 0));
		cells.put(new Position(9, 8, 8), new Cell(new Air("minecraft:dead_tube_coral"), null, new BlockProperties(Map.of("waterlogged", "false")), 0));
		render(8, 8, 8);
		assertEquals(16, vertices.get(flowing).size());
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void wetPickleClustersRenderSourceWaterAndJoinTheirNeighbors(int pickles) {
		for (int x = 7; x <= 9; x++)
			for (int z = 7; z <= 9; z++)
				putWater(x, 8, z, 0, false);
		putSolid(8, 7, 8);
		putPickles(8, new BlockProperties(Map.of("pickles", Integer.toString(pickles), "waterlogged", "true")));
		assertTrue(water.containsFluid(world, chunk, 8, 8, 8));
		render(8, 8, 8);
		assertEquals(8, vertices.get(still).size());
		assertFalse(vertices.containsKey(flowing), "No internal faces should separate wet pickles and water");
		for (Vertex vertex : top(still))
			assertEquals(8 + 8 / 9.0f - 0.001f, vertex.position().y, 0.00001f);
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void dryPickleClustersDoNotContainWater(int pickles) {
		putWater(8, 8, 8, 0, false);
		basin(8, 8, 8);
		putPickles(9, new BlockProperties(Map.of("pickles", Integer.toString(pickles), "waterlogged", "false")));
		assertFalse(water.containsFluid(world, chunk, 9, 8, 8));
		render(8, 8, 8);
		assertEquals(8, vertices.get(flowing).size(), "Water keeps its boundary against dry pickles");
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void pickleWaterloggingDefaultsAgreeWithChunkRendering(int pickles) {
		putWater(8, 8, 8, 0, false);
		basin(8, 8, 8);
		putPickles(9, new BlockProperties(Map.of("pickles", Integer.toString(pickles))));
		assertTrue(water.containsFluid(world, chunk, 9, 8, 8));
		render(8, 8, 8);
		assertFalse(vertices.containsKey(flowing), "A default wet pickle must not leave a hole in the water");
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void defaultPicklesRenderWaterWhenSavedPropertiesAreMissing(boolean emptyProperties) {
		for (int x = 7; x <= 9; x++)
			for (int z = 7; z <= 9; z++)
				putWater(x, 8, z, 0, false);
		putSolid(8, 7, 8);
		putPickles(8, emptyProperties ? new BlockProperties() : null);
		render(8, 8, 8);
		assertEquals(8 + 8 / 9.0f - 0.001f, top(still).getFirst().position().y, 0.00001f);
	}

	@Test
	void bubbleColumnsJoinOrdinaryWater() {
		putWater(8, 8, 8, 0, false);
		basin(8, 8, 8);
		Water bubble = new Water("Bubble Column", sprite(still), sprite(flowing), null, 0);
		putFluid(bubble, 7, 8, 8, 0, false);
		render(8, 8, 8);
		assertTrue(vertices.getOrDefault(flowing, List.of()).isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"minecraft:glass", "minecraft:blue_stained_glass", "minecraft:ice", "minecraft:oak_leaves"})
	void transparentBlocksUseTheSingleSidedOverlay(String name) {
		putWater(8, 8, 8, 0, false);
		basin(8, 8, 8);
		BlockStateWrapper model = new BlockStateWrapper(name);
		model.setFullOpaqueBlock(false);
		cells.put(new Position(7, 8, 8), new Cell(new Air(name), model, null, 0));
		render(8, 8, 8);
		assertEquals(4, vertices.get(overlay).size());
		assertFalse(vertices.containsKey(flowing));
	}

	@Test
	void lavaIsNotWaterAndDoesNotCreateALedgeCurrent() {
		putWater(8, 8, 8, 0, false);
		basin(8, 8, 8);
		putWater(9, 7, 8, 8, false);
		putFluid(new Lava("Lava", sprite(still), sprite(flowing), 0), 9, 8, 8, 0, false);
		render(8, 8, 8);
		assertEquals(0, top(still).getFirst().u());
		assertEquals(8, vertices.get(flowing).size(), "The water/lava boundary remains visible");
	}

	@Test
	void waterRespondsToWorldLighting() {
		putWater(8, 8, 8, 0, false);
		basin(8, 8, 8);
		defaultLight = LightSample.ambient(0);
		light.put(new Position(8, 9, 8), new LightSample(0.2f, 0.4f, 0.05f));
		lightStyle = LightStyle.Night;
		render(8, 8, 8);
		Vector4f colour = top(still).getFirst().colour();
		assertEquals(0.2f * (0.05f + 0.2f * 0.48f + 0.4f), colour.x, 0.00001f);
		assertEquals(0.4f * (0.05f + 0.2f * 0.48f + 0.4f * 0.847f), colour.y, 0.00001f);
		assertEquals(0.8f * (0.05f + 0.2f + 0.4f * 0.612f), colour.z, 0.00001f);
		light.clear();
		render(8, 8, 8);
		assertEquals(new Vector4f(0, 0, 0, 0.95f), top(still).getFirst().colour());
	}

	@Test
	void olderPrecoloredTexturesAreNotBiomeTinted() {
		putWater(8, 8, 8, 0, true);
		SubTexture texture = new SubTexture(still, 0, 0, 1, 1, Version.VERSION_12);
		render(new Water("Water", texture, 0), 8, 8, 8);
		assertEquals(new Vector4f(0.75f, 0.75f, 0.75f, 0.95f), top(still).getFirst().colour());
		assertEquals(0, top(still).getFirst().v());
	}

	@Test
	void allStandaloneDefaultWaterTexturesHaveFlowSprites() throws Exception {
		try (var paths = Files.list(Path.of("src/main/resources"))) {
			for (var path : paths.filter(p -> p.getFileName().toString().matches("defaultBlockConfig.*\\.xml")).toList()) {
				var nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile()).getElementsByTagName("Water");
				assertTrue(nodes.getLength() >= 2);
				for (int i = 0; i < nodes.getLength(); i++) {
					var element = (org.w3c.dom.Element) nodes.item(i);
					if (!element.getAttribute("texture").startsWith("terrain"))
						assertEquals("water_flow.png", element.getAttribute("flow"), path.toString());
				}
			}
		}
	}

	private void putPickles(int x, BlockProperties properties) {
		BlockStateWrapper model = new BlockStateWrapper("minecraft:sea_pickle");
		model.setFullBlock(false);
		model.setFullOpaqueBlock(false);
		model.setDefaultProperties(Map.of("pickles", "1", "waterlogged", "true"));
		cells.put(new Position(x, 8, 8), new Cell(new Air("minecraft:sea_pickle"), model, properties, 0));
		boolean wet = properties == null || !"false".equals(properties.get("waterlogged"));
		assertEquals(wet, Water.containsWater("minecraft:sea_pickle", properties, model), "Chunk water dispatch must use the same waterlogged state");
	}

	private void putWater(int x, int y, int z, int level, boolean legacy) {
		putFluid(water, x, y, z, level, legacy);
	}

	private void render(int x, int y, int z) { render(water, x, y, z); }
}
