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
import tectonicus.BlockRegistryParser;
import tectonicus.Version;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.texture.SubTexture;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LavaTest extends FluidTestFixture {
	private final Lava lava = new Lava("Lava", sprite(still), sprite(flowing), 0);

	@Test
	void lavaIsNeitherSolidNorWater() {
		assertFalse(lava.isSolid());
		assertFalse(lava.isWater());
	}

	@Test
	void sourcePoolIsFlatAndUsesStillTexture() {
		for (int x = 7; x <= 9; x++)
			for (int z = 7; z <= 9; z++)
				putLava(x, 8, z, 0, false);
		render(8, 8, 8);
		for (Vertex vertex : top(still)) {
			assertEquals(8 + 8 / 9.0f - 0.001f, vertex.position().y, 0.00001f);
			assertEquals(new Vector4f(1), vertex.colour());
		}
		assertTrue(vertices.getOrDefault(flowing, List.of()).isEmpty(), "Internal lava faces must be culled");
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
	void modernAndLegacyLevelsHaveMinecraftHeights(int level) {
		for (boolean legacy : new boolean[]{false, true}) {
			cells.clear();
			putLava(8, 8, 8, level, legacy);
			basin(8, 8, 8);
			render(8, 8, 8);
			float expected = 8 + (level >= 8 ? 8 : 8 - level) / 9.0f - 0.001f;
			assertEquals(4, vertices.values().stream().mapToInt(List::size).sum());
			for (Vertex vertex : top(still))
				assertEquals(expected, vertex.position().y, 0.00001f);
		}
	}

	@Test
	void slopesAreWeightedTowardSourcesAndMeetNeighboringSurfaces() {
		putLava(7, 8, 8, 0, false);
		putLava(8, 8, 8, 2, false);
		putLava(9, 8, 8, 6, false);
		for (int x = 7; x <= 10; x++) {
			putSolid(x, 8, 7);
			putSolid(x, 8, 9);
		}
		render(8, 8, 8);
		List<Vertex> center = List.copyOf(top(flowing));
		assertEquals(8 + (10 * 8 / 9.0f + 6 / 9.0f) / 11 - 0.001f, center.get(0).position().y, 0.00001f);
		assertEquals(8 + 4 / 9.0f - 0.001f, center.get(1).position().y, 0.00001f);
		render(9, 8, 8);
		assertEquals(center.get(1).position().y, top(flowing).get(0).position().y, 0.00001f);
		assertEquals(center.get(2).position().y, top(flowing).get(3).position().y, 0.00001f);
	}

	@Test
	void fallingColumnHasFullHeightSidesAndNoInternalTop() {
		putLava(8, 8, 8, 8, false);
		putLava(8, 9, 8, 8, false);
		putLava(8, 7, 8, 8, false);
		render(8, 8, 8);
		assertFalse(vertices.containsKey(still));
		assertEquals(16, vertices.get(flowing).size());
		assertEquals(9, vertices.get(flowing).get(0).position().y);
		assertEquals(8, vertices.get(flowing).get(2).position().y);
	}

	@Test
	void nearbyColumnRaisesOnlyTheConnectedCorners() {
		putLava(8, 8, 8, 0, false);
		putLava(8, 8, 7, 0, false);
		putLava(8, 9, 7, 8, false);
		render(8, 8, 8);
		List<Vertex> top = top(still);
		assertEquals(8.999f, top.get(0).position().y, 0.00001f);
		assertEquals(8.999f, top.get(1).position().y, 0.00001f);
		assertTrue(top.get(2).position().y < 8.9f);
		assertTrue(top.get(3).position().y < 8.9f);
	}

	@Test
	void diagonalColumnCannotRaiseCornersThroughSolidWalls() {
		putLava(8, 8, 8, 0, false);
		basin(8, 8, 8);
		putLava(7, 8, 7, 0, false);
		putLava(7, 9, 7, 8, false);
		render(8, 8, 8);
		assertEquals(8 + 8 / 9.0f - 0.001f, top(still).getFirst().position().y, 0.00001f);
	}

	@ParameterizedTest
	@CsvSource({"1,0,0.75,0.25", "-1,0,0.25,0.75", "0,1,0.25,0.25", "0,-1,0.75,0.75"})
	void flowingTopRotatesTextureWithTheCurrent(int dx, int dz, float u, float v) {
		putLava(8, 8, 8, 0, false);
		putLava(8 + dx, 8, 8 + dz, 4, false);
		render(8, 8, 8);
		Vertex vertex = top(flowing).getFirst();
		assertEquals(u, vertex.u(), 0.00001f);
		assertEquals(v / 20, vertex.v(), 0.00001f);
	}

	@Test
	void ledgeFlowUsesLavaBelowTheEmptyNeighbor() {
		putLava(8, 8, 8, 0, false);
		putLava(9, 7, 8, 8, false);
		render(8, 8, 8);
		assertEquals(0.75f, top(flowing).getFirst().u(), 0.00001f);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void slopingSurfacesJoinAcrossChunkBoundaries(boolean legacy) {
		putLava(15, 8, 8, 2, legacy);
		putLava(16, 8, 8, 6, legacy);
		render(15, 8, 8);
		List<Vertex> left = List.copyOf(top(flowing));
		render(16, 8, 8);
		assertEquals(left.get(1).position().y, top(flowing).get(0).position().y, 0.00001f);
		assertEquals(left.get(2).position().y, top(flowing).get(3).position().y, 0.00001f);
	}

	@Test
	void modernOpaqueBlocksCullFacesButGlassAndWaterDoNot() {
		putLava(8, 8, 8, 0, false);
		basin(8, 8, 8);
		BlockStateWrapper glass = new BlockStateWrapper("minecraft:glass");
		glass.setFullOpaqueBlock(false);
		cells.put(new Position(7, 8, 8), new Cell(new Air("minecraft:glass"), glass, null, 0));
		cells.put(new Position(9, 8, 8), new Cell(new Air("Water") {
			@Override public boolean isWater() { return true; }
		}, null, null, 0));
		render(8, 8, 8);
		assertEquals(8, vertices.get(flowing).size());
		for (Vertex vertex : vertices.get(flowing)) {
			assertEquals(new Vector4f(0.6f, 0.6f, 0.6f, 1), vertex.colour());
			assertTrue(vertex.u() >= 0 && vertex.u() <= 0.5f);
			assertTrue(vertex.v() >= 0 && vertex.v() <= 0.025f);
		}
	}

	@Test
	void lowSurfaceRemainsVisibleUnderASolidCeiling() {
		putLava(8, 8, 8, 6, false);
		basin(8, 8, 8);
		putSolid(8, 9, 8);
		render(8, 8, 8);
		assertEquals(4, top(still).size());
	}

	@Test
	void animationFrameStaysWithinOneSquareOfEachStrip() {
		putLava(8, 8, 8, 0, false);
		Lava framed = new Lava("Lava", new SubTexture(still, 0, 0, 1, 1), new SubTexture(flowing, 0, 0, 1, 1), 3);
		render(framed, 8, 8, 8);
		for (List<Vertex> mesh : vertices.values())
			for (Vertex vertex : mesh)
				assertTrue(vertex.v() >= 0.1f && vertex.v() <= 0.15f);
	}

	@Test
	void terrainAtlasTileIsNotMistakenForAnAnimationStrip() {
		putLava(8, 8, 8, 0, true);
		SubTexture tile = new SubTexture(still, 0.9f, 0.9f, 0.95f, 0.95f, Version.VERSION_4);
		render(new Lava("Lava", tile, tile, 0), 8, 8, 8);
		for (Vertex vertex : top(still)) {
			assertTrue(vertex.u() >= 0.9f && vertex.u() <= 0.95f);
			assertTrue(vertex.v() >= 0.9f && vertex.v() <= 0.95f);
		}
	}

	@Test
	void allDefaultConfigsUseTheLavaRenderer() throws Exception {
		try (var paths = java.nio.file.Files.list(java.nio.file.Path.of("src/main/resources"))) {
			for (var path : paths.filter(p -> p.getFileName().toString().matches("defaultBlockConfig.*\\.xml")).toList()) {
				try (var in = BlockRegistryParser.openStream(path.getFileName().toString())) {
					var elements = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in).getDocumentElement().getChildNodes();
					int count = 0;
					for (int i = 0; i < elements.getLength(); i++) {
						if (elements.item(i) instanceof org.w3c.dom.Element element && element.getAttribute("name").equals("Lava")) {
							assertEquals("Lava", element.getTagName(), path.toString());
							if (!element.getAttribute("texture").startsWith("terrain"))
								assertEquals("lava_flow.png", element.getAttribute("flow"), path.toString());
							count++;
						}
					}
					assertEquals(2, count, path.toString());
				}
			}
		}
	}

	private void putLava(int x, int y, int z, int level, boolean legacy) {
		putFluid(lava, x, y, z, level, legacy);
	}

	private void render(int x, int y, int z) {
		render(lava, x, y, z);
	}
}
