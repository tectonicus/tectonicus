/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.rasteriser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.BlockContext;
import tectonicus.blockregistry.BakedBlockStateModel.BakedQuad;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.blockregistry.LightingModelFixture;
import tectonicus.blockregistry.EmissiveBlock;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.util.Vector3f;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeshUtilLightingTest {
	private static final BlockStateWrapper AIR = block("minecraft:air", false);
	private static final BlockStateWrapper SOLID = block("minecraft:stone", true);
	private static final BlockStateWrapper SLAB = block("minecraft:oak_slab", false);

	@ParameterizedTest
	@ValueSource(floats = {0, 0.025f, 0.05f, 0.1f, 0.15f, 0.3f})
	void partialAoDoesNotDependOnNightAdjustment(float adjustment) {
		BakedQuad quad = top(0, 0, 16, 8, 16);
		Map<Cell, BlockStateWrapper> blocks = Map.of(new Cell(0, 0, 0), SLAB,
				new Cell(1, 0, 0), SOLID, new Cell(1, 0, 1), SOLID);
		float night = ao(world(blocks, LightStyle.Night, adjustment), quad, 2, false);
		float day = ao(world(blocks, LightStyle.Day, adjustment), quad, 2, false);

		assertEquals(0.6f, night, 0.000001f);
		assertEquals(day, night);
	}

	@Test
	void exposedPartialFacesHaveNoOcclusion() {
		BlockContext world = world(Map.of(new Cell(0, 0, 0), SLAB), LightStyle.Night, 0);
		BakedQuad quad = top(4, 4, 12, 8, 12);
		for (int vertex = 0; vertex < 4; vertex++)
			assertEquals(1, ao(world, quad, vertex, false), 0.000001f);
	}

	@Test
	void insetFacesSampleTheirOwnLayer() {
		BakedQuad quad = top(0, 0, 16, 8, 16);
		BlockContext world = world(Map.of(new Cell(0, 0, 0), SLAB, new Cell(1, 0, 0), SOLID), LightStyle.Night, 0);

		assertEquals(0.8f, ao(world, quad, 2, false), 0.000001f);
	}

	@Test
	void boundaryFacesSampleOutsideTheBlock() {
		BakedQuad quad = top(0, 0, 16, 16, 8);
		BlockContext inside = world(Map.of(new Cell(0, 0, 0), SLAB, new Cell(1, 0, 0), SOLID), LightStyle.Night, 0);
		BlockContext outside = world(Map.of(new Cell(0, 0, 0), SLAB, new Cell(1, 1, 0), SOLID), LightStyle.Night, 0);

		assertEquals(1, ao(inside, quad, 1, false), 0.000001f);
		assertEquals(0.8f, ao(outside, quad, 1, false), 0.000001f);
	}

	@Test
	void smallFacesInterpolateInsteadOfSnappingToCubeCorners() {
		BakedQuad quad = top(4, 4, 12, 8, 12);
		BlockContext world = world(Map.of(new Cell(0, 0, 0), SLAB, new Cell(1, 0, 0), SOLID), LightStyle.Night, 0);

		assertEquals(0.95f, ao(world, quad, 0, false), 0.000001f);
		assertEquals(0.85f, ao(world, quad, 1, false), 0.000001f);
	}

	@Test
	void fullCubeKeepsItsFourSampleShade() {
		BakedQuad quad = top(0, 0, 16, 16, 16);
		BlockContext world = world(Map.of(new Cell(0, 0, 0), SOLID, new Cell(1, 1, 1), SOLID), LightStyle.Night, 0);

		assertEquals(0.8f, ao(world, quad, 2, true), 0.000001f);
		assertEquals(1, ao(world, quad, 0, true), 0.000001f);
	}

	@ParameterizedTest
	@ValueSource(strings = {"minecraft:glass", "minecraft:red_stained_glass", "minecraft:blue_stained_glass", "minecraft:tinted_glass"})
	void glassDoesNotDarkenBlocksUnderneathOrAroundIt(String name) {
		BlockStateWrapper glass = transparentBlock(name);
		BakedQuad full = top(0, 0, 16, 16, 16);
		BlockContext around = world(Map.of(new Cell(0, 1, 0), glass, new Cell(1, 1, 0), glass,
				new Cell(0, 1, 1), glass, new Cell(1, 1, 1), glass), LightStyle.Night, 0.1f);
		assertEquals(1, ao(around, full, 2, true), 0.000001f);

		BakedQuad partial = top(0, 0, 16, 8, 16);
		BlockContext beside = world(Map.of(new Cell(0, 0, 0), SLAB, new Cell(1, 0, 0), glass,
				new Cell(0, 0, 1), glass, new Cell(1, 0, 1), glass), LightStyle.Night, 0.1f);
		assertEquals(1, ao(beside, partial, 2, false), 0.000001f);
	}

	@Test
	void iceStillDarkensTheBlockUnderneath() {
		BlockContext world = world(Map.of(new Cell(0, 1, 0), transparentBlock("minecraft:ice")), LightStyle.Night, 0.1f);
		assertEquals(0.8f, ao(world, top(0, 0, 16, 16, 16), 2, true), 0.000001f);
	}

	@Test
	void opaqueMultipartBodiesCastAoEvenWhenTheirOverlaysArePartial() {
		BlockStateWrapper bookshelf = block("minecraft:chiseled_bookshelf", false);
		bookshelf.setFullOpaqueBlock(true);
		BlockContext world = world(Map.of(new Cell(0, 1, 0), bookshelf), LightStyle.Day, 0.1f);

		assertEquals(0.8f, ao(world, top(0, 0, 16, 16, 16), 2, true), 0.000001f);
	}

	@ParameterizedTest
	@CsvSource({"0,1,0", "1,1,0", "0,1,1", "1,1,1"})
	void beaconsCastAoDespitePartialVisualGeometryAndLightEmission(int x, int y, int z) {
		BlockStateWrapper beacon = block("minecraft:beacon", false);
		beacon.setTransparent(true);
		beacon.setEmissiveBlock(EmissiveBlock.forBlockName(beacon.getBlockName()));
		BakedQuad quad = top(0, 0, 16, 16, 16);

		for (LightStyle style : new LightStyle[]{LightStyle.Day, LightStyle.Night}) {
			BlockContext world = world(Map.of(new Cell(x, y, z), beacon), style, 0.1f);
			assertEquals(0.8f, ao(world, quad, 2, true), 0.000001f);
		}
	}

	@Test
	void glassCanStillReceiveAoFromSolidNeighbors() {
		BlockContext world = world(Map.of(new Cell(0, 0, 0), transparentBlock("minecraft:glass"),
				new Cell(1, 1, 1), SOLID), LightStyle.Night, 0.1f);
		assertEquals(0.8f, ao(world, top(0, 0, 16, 16, 16), 2, true), 0.000001f);
	}

	@ParameterizedTest
	@CsvSource({
			"up, 1, 0, 0, 1", "down, 1, 0, 0, 1",
			"north, 1, 0, 0, 0", "south, 1, 0, 0, 1",
			"east, 0, 1, 0, 0", "west, 0, 1, 0, 1"
	})
	void insetInterpolationWorksOnEveryFace(String direction, int x, int y, int z, int vertex) {
		BakedQuad quad = LightingModelFixture.face(direction, new Vector3f(4, 4, 4), new Vector3f(12, 12, 12));
		BlockContext world = world(Map.of(new Cell(0, 0, 0), SLAB, new Cell(x, y, z), SOLID), LightStyle.Night, 0);

		assertEquals(0.85f, ao(world, quad, vertex, false), 0.000001f);
	}

	@ParameterizedTest
	@CsvSource({
			"up, 1, 1, 0, 1", "down, 1, -1, 0, 1",
			"north, 1, 0, -1, 0", "south, 1, 0, 1, 1",
			"east, 1, 1, 0, 0", "west, -1, 1, 0, 1"
	})
	void boundarySamplingWorksOnEveryFace(String direction, int x, int y, int z, int vertex) {
		BakedQuad quad = LightingModelFixture.face(direction, new Vector3f(0, 0, 0), new Vector3f(16, 16, 16));
		BlockContext world = world(Map.of(new Cell(0, 0, 0), SLAB, new Cell(x, y, z), SOLID), LightStyle.Night, 0);

		assertEquals(0.8f, ao(world, quad, vertex, false), 0.000001f);
		assertEquals(0.8f, ao(world, quad, vertex, true), 0.000001f);
	}

	@Test
	void enclosedPartialCornersCannotBecomeBlack() {
		BakedQuad quad = top(0, 0, 16, 8, 16);
		BlockContext world = world(Map.of(new Cell(0, 0, 0), SLAB, new Cell(1, 0, 0), SOLID,
				new Cell(0, 0, 1), SOLID, new Cell(1, 0, 1), SOLID), LightStyle.Night, 0);
		float shade = ao(world, quad, 2, false);

		assertEquals(0.4f, shade, 0.000001f);
		assertTrue(shade > 0);
	}

	@ParameterizedTest
	@CsvSource({
			"up, 0, 1, 0, Top", "down, 0, -1, 0, Bottom",
			"north, 0, 0, -1, NorthSouth", "south, 0, 0, 1, NorthSouth",
			"east, 1, 0, 0, EastWest", "west, -1, 0, 0, EastWest"
	})
	void partialBoundaryFacesGetTheSameNightLightAsFullCubes(String direction, int x, int y, int z, LightFace face) {
		BakedQuad quad = LightingModelFixture.face(direction, new Vector3f(0, 0, 0), new Vector3f(16, 16, 16));
		LightSample lit = new LightSample(0.225f, 0.35f, 0.1f);
		BlockContext world = lightingWorld(Map.of(), Map.of(new Cell(x, y, z), lit), new LightSample(0, 0, 0.1f));
		for (int vertex = 0; vertex < 4; vertex++) {
			LightSample full = smoothLight(world, quad, vertex, face, true);
			LightSample partial = smoothLight(world, quad, vertex, face, false);
			assertLightEquals(lit, full);
			assertLightEquals(full, partial);
		}
	}

	@Test
	void insetFacesUseOpenNeighborLightAndReplaceMissingSamples() {
		BakedQuad quad = top(0, 0, 16, 8, 16);
		LightSample lit = new LightSample(0.225f, 0.35f, 0.1f);
		BlockContext world = lightingWorld(Map.of(new Cell(0, 0, 0), SLAB),
				Map.of(new Cell(0, 1, 0), lit), new LightSample(0, 0, 0.1f));

		assertLightEquals(lit, smoothLight(world, quad, 2, LightFace.Top, false));
	}

	@Test
	void insetFacesDoNotBorrowCenterLightThroughSolidNeighbors() {
		BakedQuad quad = top(0, 0, 16, 8, 16);
		LightSample dark = new LightSample(0, 0, 0.1f);
		BlockContext world = lightingWorld(Map.of(new Cell(0, 0, 0), SLAB, new Cell(0, 1, 0), SOLID),
				Map.of(new Cell(0, 1, 0), new LightSample(0.225f, 0.35f, 0.1f)), dark);

		assertLightEquals(dark, smoothLight(world, quad, 2, LightFace.Top, false));
	}

	@ParameterizedTest
	@ValueSource(floats = {0, 0.05f, 0.1f, 0.15f})
	void tintedGlassBlendsSideLightLikeOtherGlass(float ambient) {
		BakedQuad quad = top(0, 0, 16, 16, 16);
		BlockStateWrapper tintedGlass = transparentBlock("minecraft:tinted_glass");
		LightSample dark = new LightSample(0, 0, ambient);
		BlockContext world = lightingWorld(Map.of(new Cell(0, 1, 0), tintedGlass),
				Map.of(new Cell(0, 1, 0), dark), new LightSample(0.225f, 0.35f, ambient));

		for (int vertex = 0; vertex < 4; vertex++) {
			LightSample blended = new LightSample(0.225f * 0.75f, 0.35f * 0.75f, ambient);
			assertLightEquals(blended, smoothLight(world, quad, vertex, LightFace.Top, true));
			assertLightEquals(blended, smoothLight(world, quad, vertex, LightFace.Top, false));
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"minecraft:glass", "minecraft:red_stained_glass"})
	void regularAndStainedGlassStillAllowSmoothSideLighting(String name) {
		BakedQuad quad = top(0, 0, 16, 16, 16);
		LightSample dark = new LightSample(0, 0, 0.1f);
		BlockContext world = lightingWorld(Map.of(new Cell(0, 1, 0), transparentBlock(name)),
				Map.of(new Cell(0, 1, 0), dark), new LightSample(0.2f, 0.4f, 0.1f));

		assertLightEquals(new LightSample(0.15f, 0.3f, 0.1f), smoothLight(world, quad, 2, LightFace.Top, true));
	}

	@Test
	void tintedGlassBlendsAvailableBlockAndSkyLight() {
		BakedQuad quad = top(0, 0, 16, 16, 16);
		LightSample interfaceLight = new LightSample(0, 0.2f, 0.1f);
		BlockContext world = lightingWorld(Map.of(new Cell(0, 1, 0), transparentBlock("minecraft:tinted_glass")),
				Map.of(new Cell(0, 1, 0), interfaceLight), new LightSample(0.225f, 0.5f, 0.1f));

		assertLightEquals(new LightSample(0.225f * 0.75f, 0.425f, 0.1f), smoothLight(world, quad, 2, LightFace.Top, true));
	}

	@Test
	void insetFacesUseTheSavedLightOfNonSolidRenderingTintedGlass() {
		BakedQuad quad = top(0, 0, 16, 8, 16);
		LightSample dark = new LightSample(0, 0, 0.1f);
		BlockContext world = lightingWorld(Map.of(new Cell(0, 0, 0), SLAB,
				new Cell(0, 1, 0), transparentBlock("minecraft:tinted_glass")),
				Map.of(new Cell(0, 0, 0), new LightSample(0.225f, 0.35f, 0.1f), new Cell(0, 1, 0), dark), dark);

		assertLightEquals(dark, smoothLight(world, quad, 2, LightFace.Top, false));
	}

	@ParameterizedTest
	@CsvSource({"Day,0", "Day,1", "Day,2", "Day,3", "Night,0", "Night,1", "Night,2", "Night,3"})
	void largerTintedGlassPatchesDarkenNeighborSamplesInBothDayAndNight(LightStyle style, int coveredNeighbors) {
		BakedQuad quad = top(0, 0, 16, 16, 16);
		BlockStateWrapper glass = transparentBlock("minecraft:tinted_glass");
		float ambient = style == LightStyle.Day ? 0.3f : 0.1f;
		float sky = style == LightStyle.Day ? 0.7f : 0.225f;
		LightSample dark = new LightSample(0, 0, ambient);
		Map<Cell, BlockStateWrapper> blocks = new HashMap<>();
		Map<Cell, LightSample> lights = new HashMap<>();
		blocks.put(new Cell(0, 1, 0), glass);
		lights.put(new Cell(0, 1, 0), dark);
		Cell[] neighbors = {new Cell(1, 1, 0), new Cell(0, 1, 1), new Cell(1, 1, 1)};
		for (int i = 0; i < coveredNeighbors; i++) {
			blocks.put(neighbors[i], glass);
			lights.put(neighbors[i], dark);
		}
		BlockContext world = lightingWorld(blocks, lights, new LightSample(sky, 0, ambient), style);
		assertLightEquals(new LightSample(sky * (3 - coveredNeighbors) * 0.25f, 0, ambient),
				smoothLight(world, quad, 2, LightFace.Top, true));
		assertEquals(1, ao(world, quad, 2, true), 0.000001f);
	}

	@ParameterizedTest
	@CsvSource({"Day,Top,1", "Day,Bottom,0.5", "Day,NorthSouth,0.85", "Day,EastWest,0.7",
			"Night,Top,1", "Night,Bottom,0.5", "Night,NorthSouth,0.95", "Night,EastWest,0.9"})
	void brightSmoothLightingKeepsDirectionalShading(LightStyle style, LightFace face, float expectedShade) {
		for (boolean grayscale : new boolean[]{false, true}) {
			var colour = MeshUtil.getShadedLightColour(new LightSample(2, 2, 2), style, face, grayscale);
			assertEquals(expectedShade, colour.r, 0.000001f);
			assertEquals(expectedShade, colour.g, 0.000001f);
			assertEquals(expectedShade, colour.b, 0.000001f);
		}
	}

	@ParameterizedTest
	@CsvSource({"up,0,1,0,Top", "down,0,-1,0,Bottom", "north,0,0,-1,NorthSouth",
			"south,0,0,1,NorthSouth", "east,1,0,0,EastWest", "west,-1,0,0,EastWest"})
	void flatFullFacesSampleTheirDirectionalNeighbor(String direction, int dx, int dy, int dz, LightFace face) {
		BakedQuad quad = LightingModelFixture.face(direction, new Vector3f(0, 0, 0), new Vector3f(16, 16, 16));
		for (LightStyle style : LightStyle.values()) {
			assertEquals(flatLightAt(dx, dy, dz, face, style),
					MeshUtil.getFlatLight(flatWorld(style), null, 0, 0, 0, quad, true, 0.1f), 0.000001f);
		}
	}

	@ParameterizedTest
	@CsvSource({"up,Top", "down,Bottom", "north,NorthSouth", "south,NorthSouth", "east,EastWest", "west,EastWest"})
	void flatPartialFacesRetainCellLightAndDirectionalShading(String direction, LightFace face) {
		BakedQuad quad = LightingModelFixture.face(direction, new Vector3f(4, 4, 4), new Vector3f(12, 12, 12));
		for (LightStyle style : LightStyle.values()) {
			float cellLight = 0.6f;
			assertEquals(cellLight * Chunk.getFaceFactor(style, face),
					MeshUtil.getFlatLight(flatWorld(style), null, 0, 0, 0, quad, false, 0.1f), 0.000001f);
		}
	}

	@ParameterizedTest
	@CsvSource({"up,0,1,0", "down,0,-1,0", "north,0,0,-1", "south,0,0,1", "east,1,0,0", "west,-1,0,0"})
	void unshadedPartialFacesUseTheirSurfaceWithoutCardinalDarkening(String direction, int dx, int dy, int dz) {
		BlockContext world = flatWorld(LightStyle.Day);
		BakedQuad inset = LightingModelFixture.face(direction, new Vector3f(4, 4, 4), new Vector3f(12, 12, 12), false, null);
		BakedQuad boundary = LightingModelFixture.face(direction, new Vector3f(0, 0, 0), new Vector3f(16, 16, 16), false, null);

		assertEquals(flatLightAt(0, 0, 0, LightFace.Top, LightStyle.Day),
				MeshUtil.getFlatLight(world, null, 0, 0, 0, inset, false, 0.1f), 0.000001f);
		assertEquals(flatLightAt(dx, dy, dz, LightFace.Top, LightStyle.Day),
				MeshUtil.getFlatLight(world, null, 0, 0, 0, boundary, false, 0.1f), 0.000001f);
	}

	@Test
	void flatLightingPreservesShadeDirectionOverrides() {
		BakedQuad quad = LightingModelFixture.face("east", new Vector3f(0, 0, 0), new Vector3f(16, 16, 16), true, "up");
		assertEquals(flatLightAt(0, 1, 0, LightFace.Top, LightStyle.Day),
				MeshUtil.getFlatLight(flatWorld(LightStyle.Day), null, 0, 0, 0, quad, true, 0.1f), 0.000001f);
	}

	private float flatLightAt(int x, int y, int z, LightFace face, LightStyle style) {
		return (0.6f + x * 0.03f + y * 0.02f + z * 0.01f) * Chunk.getFaceFactor(style, face);
	}

	private BlockContext flatWorld(LightStyle style) {
		return (BlockContext) Proxy.newProxyInstance(BlockContext.class.getClassLoader(), new Class<?>[]{BlockContext.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "getLightStyle" -> style;
					case "getLight" -> flatLightAt((int) args[1], (int) args[2], (int) args[3], (LightFace) args[4], style);
					default -> throw new AssertionError("Unexpected flat-light query: " + method.getName());
				});
	}

	@Test
	void smallFacesInterpolateNightLightingByVertexPosition() {
		BakedQuad quad = top(4, 4, 12, 8, 12);
		Map<Cell, LightSample> lights = new HashMap<>();
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++)
				lights.put(new Cell(x, 0, z), new LightSample(0.2f + 0.1f * x, 0, 0.1f));
		}
		BlockContext world = lightingWorld(Map.of(new Cell(0, 0, 0), SLAB, new Cell(0, 1, 0), SOLID),
				lights, new LightSample(0, 0, 0.1f));

		assertLightEquals(new LightSample(0.175f, 0, 0.1f), smoothLight(world, quad, 0, LightFace.Top, false));
		assertLightEquals(new LightSample(0.225f, 0, 0.1f), smoothLight(world, quad, 1, LightFace.Top, false));
	}

	private LightSample smoothLight(BlockContext world, BakedQuad quad, int vertex, LightFace face, boolean fullBlock) {
		return MeshUtil.getSmoothVertexLight(world, null, 0, 0, 0, quad, vertex, face, fullBlock, 0.1f);
	}

	private void assertLightEquals(LightSample expected, LightSample actual) {
		assertEquals(expected.sky(), actual.sky(), 0.000001f);
		assertEquals(expected.block(), actual.block(), 0.000001f);
		assertEquals(expected.ambient(), actual.ambient(), 0.000001f);
	}

	private BlockContext lightingWorld(Map<Cell, BlockStateWrapper> blocks, Map<Cell, LightSample> lights, LightSample defaultLight) {
		return lightingWorld(blocks, lights, defaultLight, LightStyle.Night);
	}

	private BlockContext lightingWorld(Map<Cell, BlockStateWrapper> blocks, Map<Cell, LightSample> lights, LightSample defaultLight, LightStyle style) {
		return (BlockContext) Proxy.newProxyInstance(BlockContext.class.getClassLoader(), new Class<?>[]{BlockContext.class},
				(proxy, method, args) -> {
					if (method.getName().equals("getLightStyle"))
						return style;
					Cell cell = new Cell((int) args[1], (int) args[2], (int) args[3]);
					return switch (method.getName()) {
						case "getBlock" -> blocks.getOrDefault(cell, AIR);
						case "getLightSample" -> lights.getOrDefault(cell, defaultLight);
						default -> throw new AssertionError("Unexpected lighting query: " + method.getName());
					};
				});
	}

	private float ao(BlockContext world, BakedQuad quad, int vertex, boolean fullBlock) {
		return MeshUtil.getBakedAo(world, null, 0, 0, 0, quad, vertex, fullBlock);
	}

	private BakedQuad top(int minX, int minZ, int maxX, int maxY, int maxZ) {
		return LightingModelFixture.face("up", new Vector3f(minX, 0, minZ), new Vector3f(maxX, maxY, maxZ));
	}

	private BlockContext world(Map<Cell, BlockStateWrapper> blocks, LightStyle style, float adjustment) {
		return (BlockContext) Proxy.newProxyInstance(BlockContext.class.getClassLoader(), new Class<?>[]{BlockContext.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "getBlock" -> blocks.getOrDefault(new Cell((int) args[1], (int) args[2], (int) args[3]), AIR);
					case "getLightStyle" -> style;
					case "getNightLightAdjustment" -> adjustment;
					default -> throw new AssertionError("AO must not query light: " + method.getName());
				});
	}

	private static BlockStateWrapper block(String name, boolean fullBlock) {
		BlockStateWrapper block = new BlockStateWrapper(name);
		block.setFullBlock(fullBlock);
		block.setFullOpaqueBlock(fullBlock);
		return block;
	}

	private static BlockStateWrapper transparentBlock(String name) {
		BlockStateWrapper block = block(name, true);
		block.setTransparent(true);
		block.setFullOpaqueBlock(false);
		return block;
	}

	private record Cell(int x, int y, int z) {}
}
