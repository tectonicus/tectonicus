/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.chunk.ChunkCoord;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.Texture;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class FlameParticlesTest extends FluidTestFixture {
	private final Texture flame = texture(8, 8);
	private final SubTexture sprite = new SubTexture(flame, 0.125f, 0.25f, 0.625f, 0.75f);
	private final Texture smokeTexture = texture(8, 8);
	private final List<SubTexture> smoke = IntStream.range(0, 8)
			.mapToObj(i -> new SubTexture(smokeTexture, i / 8f, 0, (i + 1) / 8f, 1)).toList();

	@ParameterizedTest
	@CsvSource({
		"torch,flame", "wall_torch,flame", "soul_torch,soul_fire_flame", "soul_wall_torch,soul_fire_flame",
		"copper_torch,copper_fire_flame", "copper_wall_torch,copper_fire_flame",
		"candle,small_flame", "red_candle,small_flame", "candle_cake,small_flame", "black_candle_cake,small_flame",
		"redstone_torch,dust", "redstone_wall_torch,dust"
	})
	void selectsTheVanillaParticleForEachEmitter(String block, String particle) {
		assertEquals("minecraft:" + particle, FlameParticles.particleId("minecraft:" + block, properties("true", "1")));
	}

	@Test
	void unlitAndWaterloggedCandlesAndUnlitRedstoneTorchesEmitNothing() {
		for (String block : List.of("redstone_torch", "redstone_wall_torch")) {
			renderFlame(block, properties("false", "1"));
			assertTrue(meshTypes.isEmpty());
		}
		for (String block : List.of("candle", "blue_candle", "candle_cake", "white_candle_cake")) {
			for (BlockProperties state : new BlockProperties[]{null, new BlockProperties(), properties("false", "4"),
					new BlockProperties(Map.of("lit", "true", "waterlogged", "true"))}) {
				renderFlame(block, state);
				assertTrue(meshTypes.isEmpty(), block);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"lantern", "stone", "redstone_wire"})
	void unsupportedBlocksEmitNothingEvenWhenPowered(String block) {
		renderWithSmoke(8, block, new BlockProperties(Map.of("lit", "true", "power", "15")));
		assertTrue(meshTypes.isEmpty());
	}

	@ParameterizedTest
	@CsvSource({"north,0.5,0.77", "south,0.5,0.23", "east,0.23,0.5", "west,0.77,0.5"})
	void wallFlamesFollowTheTorchFacing(String facing, float expectedX, float expectedZ) {
		for (String block : List.of("wall_torch", "soul_wall_torch", "copper_wall_torch")) {
			renderFlame(block, new BlockProperties(Map.of("facing", facing)));
			assertEquals(1, centers().size());
			assertNearWick(new Vector3f(8 + expectedX, 8.92f, 8 + expectedZ), centers().getFirst());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"torch", "soul_torch", "copper_torch"})
	void floorFlamesStayAtTheTorchTip(String block) {
		renderFlame(block, null);
		assertNearWick(new Vector3f(8.5f, 8.7f, 8.5f), centers().getFirst());
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void eachCandleHasOneSmallFlameAboveItsWick(int count) {
		int[][][] expectedPixels = {
			{{8, 8, 8}}, {{6, 7, 8}, {10, 8, 7}},
			{{8, 5, 10}, {6, 7, 8}, {9, 8, 7}}, {{7, 5, 9}, {10, 7, 9}, {6, 7, 6}, {9, 8, 6}}
		};
		renderFlame("candle", properties("true", Integer.toString(count)));
		List<Vector3f> centers = centers();
		assertEquals(count, centers.size());
		for (int i = 0; i < count; i++) {
			int[] pixel = expectedPixels[count - 1][i];
			assertNearWick(new Vector3f(8 + pixel[0] / 16f, 8 + pixel[1] / 16f, 8 + pixel[2] / 16f), centers.get(i));
			float width = quadWidth(vertices.get(flame), i);
			assertTrue(width >= 0.05f - 0.00001f && width <= 0.2f + 0.00001f);
		}
	}

	@Test
	void cakeFlameIsRaisedAndMissingCandleCountDefaultsToOne() {
		renderFlame("pink_candle_cake", properties("true", "4"));
		assertEquals(1, centers().size());
		assertNearWick(new Vector3f(8.5f, 9, 8.5f), centers().getFirst());
		renderFlame("candle", new BlockProperties(Map.of("lit", "true")));
		assertEquals(1, centers().size());
		assertNearWick(new Vector3f(8.5f, 8.5f, 8.5f), centers().getFirst());
	}

	@ParameterizedTest
	@ValueSource(floats = {0, 0.7853982f, 1.5707964f})
	void flamesFaceTheCameraWithUprightUvsAndWorldLighting(float elevation) {
		cameraAngle = 2.3f;
		cameraElevation = elevation;
		renderFlame("torch", null);
		List<Vertex> rendered = List.copyOf(vertices.get(flame));
		assertEquals(4, rendered.size());
		assertEquals(Set.of(Geometry.MeshType.AlphaTest), meshTypes);
		Vector3f edge1 = new Vector3f(rendered.get(1).position()).sub(rendered.get(0).position());
		Vector3f edge2 = new Vector3f(rendered.get(2).position()).sub(rendered.get(1).position());
		assertTrue(edge1.length() >= 0.1f - 0.00001f && edge1.length() <= 0.4f + 0.00001f);
		assertEquals(edge1.length(), edge2.length(), 0.00001f);
		Vector3f facing = new Vector3f((float) (Math.cos(cameraAngle) * Math.cos(elevation)),
				(float) Math.sin(elevation), (float) (Math.sin(cameraAngle) * Math.cos(elevation)));
		assertEquals(-1, edge1.cross(edge2).normalize().dot(facing), 0.00001f);
		float leftU = rendered.get(0).u(), rightU = rendered.get(1).u();
		assertEquals(sprite.u0, leftU);
		assertEquals(sprite.u1, rightU);
		assertEquals(sprite.v0, rendered.get(0).v());
		assertEquals(sprite.v0, rendered.get(1).v());
		assertEquals(rightU, rendered.get(2).u());
		assertEquals(sprite.v1, rendered.get(2).v());
		assertEquals(leftU, rendered.get(3).u());
		assertEquals(sprite.v1, rendered.get(3).v());
		assertTrue(rendered.stream().allMatch(v -> v.colour().x >= 0.75f && v.colour().w == 1));
	}

	@Test
	void missingParticleSpritesEmitNoGeometry() {
		render(geometry -> FlameParticles.addGeometry(8, 8, 8, "minecraft:torch", null, world, chunk, geometry, List.of(), List.of()));
		assertTrue(meshTypes.isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"torch", "soul_torch", "copper_torch", "candle", "candle_cake"})
	void nearbyBlocksHaveDifferentStableSizesWithoutMirroring(String block) {
		Set<Float> widths = new HashSet<>();
		Set<Float> leftUvs = new HashSet<>();
		BlockProperties state = properties("true", "1");
		renderFlame(block, state);
		List<Vertex> first = List.copyOf(vertices.get(flame));
		for (int x = 0; x < 16; x++) {
			renderFlameAt(x, 8, 8, block, state, List.of(sprite));
			List<Vertex> rendered = vertices.get(flame);
			widths.add(quadWidth(rendered, 0));
			leftUvs.add(rendered.getFirst().u());
		}
		assertTrue(widths.size() >= 4);
		assertEquals(Set.of(sprite.u0), leftUvs);
		renderFlame(block, state);
		assertEquals(first, vertices.get(flame), "Other renders must not advance this block's random state");
	}

	@Test
	void candlesInTheSameClusterVaryIndependentlyAndRepeatExactly() {
		renderFlame("candle", properties("true", "4"));
		List<Vertex> first = List.copyOf(vertices.get(flame));
		Set<Float> widths = new HashSet<>();
		for (int i = 0; i < 4; i++)
			widths.add(quadWidth(first, i));
		assertTrue(widths.size() > 1);
		renderFlame("candle", properties("true", "4"));
		assertEquals(first, vertices.get(flame));
	}

	@Test
	void variationUsesWorldCoordinatesIncludingChunkOriginAndWorldHeight() {
		renderFlame("torch", null);
		List<Vertex> first = List.copyOf(vertices.get(flame));
		Set<List<Vertex>> chunkAppearances = new HashSet<>();
		for (int x = 0; x < 16; x++) {
			chunk = chunkAt(x, 0, 0);
			renderFlame("torch", null);
			chunkAppearances.add(List.copyOf(vertices.get(flame)));
		}
		assertTrue(chunkAppearances.size() >= 4, "Same local position must vary between chunks");
		chunk = chunkAt(1, 0, 0);
		renderFlameAt(-8, 8, 8, "torch", null, List.of(sprite));
		assertTranslated(first, vertices.get(flame), new Vector3f(-16, 0, 0));
		Set<List<Vertex>> heightAppearances = new HashSet<>();
		for (int section = 0; section < 16; section++) {
			chunk = chunkAt(0, 0, -16 * section);
			renderFlame("torch", null);
			heightAppearances.add(List.copyOf(vertices.get(flame)));
		}
		assertTrue(heightAppearances.size() >= 4, "Same local height must vary between world heights");
		chunk = chunkAt(0, 0, -64);
		renderFlameAt(8, 72, 8, "torch", null, List.of(sprite));
		assertTranslated(first, vertices.get(flame), new Vector3f(0, 64, 0));
	}

	@Test
	void resourcePackSpritesAreSelectedDeterministicallyAcrossBlocks() {
		List<SubTexture> sprites = List.of(new SubTexture(flame, 0, 0, 0.25f, 1),
				new SubTexture(flame, 0.25f, 0, 0.5f, 1), new SubTexture(flame, 0.5f, 0, 0.75f, 1));
		Set<Float> selected = new HashSet<>();
		for (int x = 0; x < 16; x++) {
			renderFlameAt(x, 8, 8, "torch", null, sprites);
			List<Vertex> first = List.copyOf(vertices.get(flame));
			selected.add(Math.min(first.get(0).u(), first.get(1).u()));
			renderFlameAt(x, 8, 8, "torch", null, sprites);
			assertEquals(first, vertices.get(flame));
		}
		assertEquals(Set.of(0f, 0.25f, 0.5f), selected);
	}

	@Test
	void candleFlamesUseHalfTheTorchSizeWithTheSameStableOffset() {
		renderFlame("torch", null);
		List<Vertex> torch = List.copyOf(vertices.get(flame));
		Vector3f torchCenter = centers().getFirst();
		renderFlame("candle", properties("true", "1"));
		List<Vertex> candle = vertices.get(flame);
		assertEquals(torch.get(0).position().distance(torch.get(1).position()) / 2,
				candle.get(0).position().distance(candle.get(1).position()), 0.00001f);
		assertPosition(new Vector3f(torchCenter).add(0, -0.2f, 0), centers().getFirst());
	}

	@Test
	void smokeSamplesKeepHeightSizeAndSpriteStageCoordinated() {
		Map<Float, SmokeAppearance> appearances = new TreeMap<>();
		boolean rises = false;
		for (int x = 0; x < 64; x++) {
			renderWithSmoke(x, "torch", null);
			List<Vertex> rendered = vertices.getOrDefault(smokeTexture, List.of());
			if (rendered.isEmpty())
				continue;
			assertEquals(4, rendered.size());
			Vector3f center = new Vector3f(rendered.get(0).position()).add(rendered.get(2).position()).mul(0.5f);
			rises |= center.y > 9;
			assertTrue(center.y >= 8.7f);
			appearances.put(rendered.get(0).u(), new SmokeAppearance(center.y, quadWidth(rendered, 0), rendered.get(0).colour().x));
			assertTrue(rendered.stream().allMatch(v -> v.colour().x <= 0.3f && v.colour().x == v.colour().y && v.colour().y == v.colour().z));
		}
		assertTrue(rises);
		assertEquals(8, appearances.size());
		SmokeAppearance previous = null;
		for (SmokeAppearance appearance : appearances.values()) {
			if (previous != null) {
				assertTrue(appearance.height() > previous.height(), "More dispersed sprites belong higher above the wick");
				assertTrue(appearance.width() >= previous.width() - 0.00001f, "Higher smoke should spread out");
				assertTrue(appearance.brightness() <= previous.brightness(), "Higher smoke should become less dense");
			}
			previous = appearance;
		}
	}

	@Test
	void candlesEmitSmokeAtAMinorityOfWicks() {
		int smokingWicks = 0;
		for (int x = 0; x < 16; x++) {
			renderWithSmoke(x, "candle", properties("true", "4"));
			smokingWicks += vertices.getOrDefault(smokeTexture, List.of()).size() / 4;
		}
		assertTrue(smokingWicks >= 8 && smokingWicks <= 30, "A minority of the 64 wicks should emit smoke");
	}

	@Test
	void missingTexturesDoNotChangeTheOtherParticleType() {
		renderWithSmoke(8, "torch", null);
		List<Vertex> flames = List.copyOf(vertices.get(flame));
		List<Vertex> smokeVertices = List.copyOf(vertices.get(smokeTexture));
		renderFlame("torch", null);
		assertEquals(flames, vertices.get(flame));
		render(geometry -> FlameParticles.addGeometry(8, 8, 8, "minecraft:torch", null, world, chunk, geometry, List.of(), smoke));
		assertEquals(smokeVertices, vertices.get(smokeTexture));
		assertFalse(vertices.containsKey(flame));
	}

	@Test
	void smokeDoesNotPassThroughAFullBlockCeiling() {
		boolean visibleSmoke = false;
		for (int x = 0; x < 16; x++)
			for (int z = 0; z < 16; z++)
				putSolid(x, 9, z);
		for (int x = 0; x < 16; x++) {
			renderWithSmoke(x, "torch", null);
			List<Vertex> rendered = vertices.getOrDefault(smokeTexture, List.of());
			if (!rendered.isEmpty()) {
				visibleSmoke = true;
				assertTrue(new Vector3f(rendered.get(0).position()).add(rendered.get(2).position()).mul(0.5f).y < 9);
			}
		}
		assertTrue(visibleSmoke, "The ceiling should only hide particles above it");
	}

	@ParameterizedTest
	@ValueSource(strings = {"redstone_torch", "redstone_wall_torch"})
	void redstoneTorchesEmitLitRedDustWithoutSmoke(String block) {
		renderWithSmoke(8, block, new BlockProperties(Map.of("lit", "true", "facing", "east")));
		assertFalse(vertices.containsKey(smokeTexture));
		assertEquals(4, vertices.get(flame).size());
		for (Vertex vertex : vertices.get(flame)) {
			assertTrue(vertex.colour().x > 0 && vertex.colour().x < 1);
			assertEquals(0, vertex.colour().y);
			assertEquals(0, vertex.colour().z);
		}
		renderWithSmoke(8, block, properties("false", "1"));
		assertTrue(meshTypes.isEmpty());
	}

	@Test
	void flamesAreEmissiveWhileSmokeAndDustUseSurroundingLight() {
		defaultLight = LightSample.ambient(0);
		lightStyle = LightStyle.Night;
		renderWithSmoke(8, "torch", null);
		assertTrue(vertices.get(flame).getFirst().colour().x > 0);
		for (Vertex vertex : vertices.get(smokeTexture))
			assertEquals(new Vector4f(0, 0, 0, 1), vertex.colour());
		renderFlame("redstone_torch", properties("true", "1"));
		assertEquals(new Vector4f(0, 0, 0, 1), vertices.get(flame).getFirst().colour());
	}

	private void renderWithSmoke(int x, String block, BlockProperties properties) {
		render(geometry -> FlameParticles.addGeometry(x, 8, 8, "minecraft:" + block, properties, world, chunk, geometry, List.of(sprite), smoke));
	}

	private void assertNearWick(Vector3f expected, Vector3f actual) {
		assertEquals(expected.x, actual.x, 0.12f);
		assertEquals(expected.y, actual.y, 0.12f);
		assertEquals(expected.z, actual.z, 0.12f);
	}

	private RawChunk chunkAt(int chunkX, int chunkZ, int yOffset) {
		return new RawChunk() {
			@Override public ChunkCoord getChunkCoord() { return new ChunkCoord(chunkX, chunkZ); }
			@Override public int getWorldY(int y) { return y + yOffset; }
		};
	}

	private void assertTranslated(List<Vertex> expected, List<Vertex> actual, Vector3f translation) {
		assertEquals(expected.size(), actual.size());
		for (int i = 0; i < expected.size(); i++) {
			assertPosition(new Vector3f(expected.get(i).position()).add(translation), actual.get(i).position());
			assertEquals(expected.get(i).u(), actual.get(i).u());
			assertEquals(expected.get(i).v(), actual.get(i).v());
		}
	}

	private void renderFlame(String block, BlockProperties properties) {
		renderFlameAt(8, 8, 8, block, properties, List.of(sprite));
	}

	private void renderFlameAt(int x, int y, int z, String block, BlockProperties properties, List<SubTexture> sprites) {
		render(geometry -> FlameParticles.addGeometry(x, y, z, "minecraft:" + block, properties, world, chunk, geometry, sprites, List.of()));
	}

	private BlockProperties properties(String lit, String count) {
		return new BlockProperties(Map.of("lit", lit, "candles", count));
	}

	private List<Vector3f> centers() {
		List<Vertex> rendered = vertices.get(flame);
		return IntStream.range(0, rendered.size() / 4)
				.mapToObj(i -> new Vector3f(rendered.get(i * 4).position()).add(rendered.get(i * 4 + 2).position()).mul(0.5f)).toList();
	}

	private void assertPosition(Vector3f expected, Vector3f actual) {
		assertEquals(expected.x, actual.x, 0.00001f);
		assertEquals(expected.y, actual.y, 0.00001f);
		assertEquals(expected.z, actual.z, 0.00001f);
	}

	private float quadWidth(List<Vertex> rendered, int particle) {
		return rendered.get(particle * 4).position().distance(rendered.get(particle * 4 + 1).position());
	}

	private record SmokeAppearance(float height, float width, float brightness) {}
}
