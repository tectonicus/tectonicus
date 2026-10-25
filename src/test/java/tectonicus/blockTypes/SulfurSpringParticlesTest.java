/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.Texture;
import tectonicus.raw.BlockProperties;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SulfurSpringParticlesTest extends FluidTestFixture {
	private final Texture gas = texture(32, 32);
	private final Texture bubble = texture(8, 8);
	private final List<SubTexture> gasSprites = List.of(sprite(gas), sprite(gas), sprite(gas), sprite(gas),
			sprite(gas), sprite(gas), sprite(gas), sprite(gas));
	private final List<SubTexture> bubbleSprites = List.of(sprite(bubble));
	private final Water water = new Water("Water", sprite(still), 1);

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void gasReachesTheSurfaceOfOneToFourSourceBlocks(int depth) {
		pool(8, 4, 8, depth);
		renderSpring("wet");
		assertFalse(centers(gas).isEmpty());
		for (Vector3f position : centers(gas)) {
			assertTrue(position.y >= 4 + depth + 1 + 0.25f);
			assertTrue(Math.hypot(position.x - 8.5f, position.z - 8.5f) <= 3.5);
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {5, 6, 8})
	void deeperWaterKeepsBubblesWithoutSurfaceGas(int depth) {
		pool(8, 4, 8, depth);
		renderSpring("wet");
		assertTrue(centers(gas).isEmpty());
		assertFalse(centers(bubble).isEmpty());
		assertTrue(centers(bubble).stream().allMatch(p -> p.y >= 5 && p.y < 9));
	}

	@ParameterizedTest
	@CsvSource({"dry,false,false", "wet,true,true", "dormant,true,true", "erupting,false,true", "continuous,false,true", "unknown,false,false"})
	void savedStatesSelectAmbientGasAndBubbles(String state, boolean hasGas, boolean hasBubbles) {
		pool(8, 4, 8, 4);
		renderSpring(state);
		assertEquals(hasGas, !centers(gas).isEmpty());
		assertEquals(hasBubbles, !centers(bubble).isEmpty());
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 7, 8, 15})
	void flowingOrFallingWaterDoesNotActivateSulfur(int level) {
		pool(8, 4, 8, 4);
		putFluid(water, 8, 5, 8, level, false);
		renderSpring("wet");
		assertTrue(meshTypes.isEmpty());
	}

	@Test
	void lidBlocksSurfaceGas() {
		pool(8, 4, 8, 4);
		putSolid(8, 9, 8);
		renderSpring("wet");
		assertTrue(centers(gas).isEmpty());
		assertFalse(centers(bubble).isEmpty());
	}

	@Test
	void submergedWallBlocksGasFromReachingWaterBeyondIt() {
		pool(8, 4, 8, 2);
		for (int z = 3; z <= 13; z++)
			putSolid(9, 6, z);
		renderSpring("wet");
		assertFalse(centers(gas).isEmpty());
		assertTrue(centers(gas).stream().allMatch(p -> p.x < 10));
	}

	@Test
	void gasDoesNotSpreadOverDryLandOrFlowingWater() {
		pool(8, 4, 8, 2);
		for (int x = 3; x <= 13; x++)
			for (int z = 3; z <= 13; z++)
				if (x != 8 || z != 8)
					putFluid(water, x, 6, z, 3, false);
		renderSpring("wet");
		assertTrue(centers(gas).stream().allMatch(p -> p.x > 7.5f && p.x < 9.5f && p.z > 7.5f && p.z < 9.5f));
		cells.keySet().removeIf(p -> p.y() >= 5);
		renderSpring("wet");
		assertTrue(meshTypes.isEmpty());
	}

	@Test
	void effectsCrossChunkEdgesAndRemainStableWhenAssetsAreMissing() {
		pool(15, 4, 8, 4);
		renderAt(15, 4, 8, "wet", gasSprites, bubbleSprites);
		List<Vertex> first = List.copyOf(vertices.get(gas));
		assertTrue(centers(gas).stream().anyMatch(p -> p.x >= 16));
		renderAt(15, 4, 8, "wet", gasSprites, List.of());
		assertEquals(first, vertices.get(gas));
		renderAt(15, 4, 8, "wet", List.of(), bubbleSprites);
		assertFalse(centers(bubble).isEmpty());
		renderAt(15, 4, 8, "wet", List.of(), List.of());
		assertTrue(meshTypes.isEmpty());
	}

	@Test
	void particlesUseLightingAndTheirCorrectRenderPasses() {
		pool(8, 4, 8, 4);
		defaultLight = new LightSample(0.2f, 0.4f, 0.05f);
		lightStyle = LightStyle.Night;
		renderSpring("wet");
		assertEquals(Set.of(Geometry.MeshType.Transparent, Geometry.MeshType.AlphaTest), meshTypes);
		for (Texture texture : List.of(gas, bubble)) {
			assertFalse(centers(texture).isEmpty());
			for (Vertex vertex : vertices.get(texture)) {
				assertEquals(0.546f, vertex.colour().x, 0.00001f);
				assertEquals(0.4848f, vertex.colour().y, 0.00001f);
				assertEquals(0.4948f, vertex.colour().z, 0.00001f);
				assertTrue(vertex.colour().w > 0.5f && vertex.colour().w <= 1);
			}
		}
	}

	@ParameterizedTest
	@ValueSource(floats = {0, 0.7853982f, 1.5707964f})
	void gasAndBubblesFaceTheCameraClockwise(float elevation) {
		pool(8, 4, 8, 4);
		cameraAngle = 2.3f;
		cameraElevation = elevation;
		renderSpring("wet");
		Vector3f facing = new Vector3f((float) (Math.cos(cameraAngle) * Math.cos(elevation)),
				(float) Math.sin(elevation), (float) (Math.sin(cameraAngle) * Math.cos(elevation)));
		for (Texture texture : List.of(gas, bubble)) {
			List<Vertex> rendered = vertices.get(texture);
			for (int i = 0; i < rendered.size(); i += 4) {
				Vector3f edge1 = new Vector3f(rendered.get(i + 1).position()).sub(rendered.get(i).position());
				Vector3f edge2 = new Vector3f(rendered.get(i + 2).position()).sub(rendered.get(i + 1).position());
				assertEquals(-1, edge1.cross(edge2).normalize().dot(facing), 0.00001f);
			}
		}
	}

	@Test
	void missingStateDefaultsToDryAndWorldHeightStopsTheSurfaceSearch() {
		pool(8, 4, 8, 4);
		render(geometry -> SulfurSpringParticles.addGeometry(8, 4, 8, null, world, chunk, geometry, gasSprites, bubbleSprites));
		assertTrue(meshTypes.isEmpty());
		pool(8, 14, 8, 1);
		renderAt(8, 14, 8, "wet", gasSprites, bubbleSprites);
		assertTrue(centers(gas).isEmpty());
		assertTrue(centers(bubble).stream().allMatch(p -> p.y >= 15 && p.y < 16));
	}

	private void pool(int x, int y, int z, int depth) {
		for (int dx = -5; dx <= 5; dx++)
			for (int dz = -5; dz <= 5; dz++)
				for (int dy = 1; dy <= depth; dy++)
					cells.put(new Position(x + dx, y + dy, z + dz),
							new Cell(water, null, new BlockProperties(Map.of("level", "0")), 0));
	}

	private void renderSpring(String state) { renderAt(8, 4, 8, state, gasSprites, bubbleSprites); }

	private void renderAt(int x, int y, int z, String state, List<SubTexture> gas, List<SubTexture> bubbles) {
		render(geometry -> SulfurSpringParticles.addGeometry(x, y, z,
				new BlockProperties(Map.of("potent_sulfur_state", state)), world, chunk, geometry, gas, bubbles));
	}

	private List<Vector3f> centers(Texture texture) {
		List<Vertex> rendered = vertices.getOrDefault(texture, List.of());
		List<Vector3f> centers = new ArrayList<>();
		for (int i = 0; i < rendered.size(); i += 4)
			centers.add(new Vector3f(rendered.get(i).position()).add(rendered.get(i + 2).position()).mul(0.5f));
		return centers;
	}
}
