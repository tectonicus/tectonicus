/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.Minecraft;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.Texture;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GeyserParticlesTest extends FluidTestFixture {
	private final Texture base = texture(32, 32), poof = texture(32, 32), plume = texture(32, 32);
	private final List<SubTexture> baseSprites = List.of(sprite(base));
	private final List<SubTexture> poofSprites = List.of(sprite(poof));
	private final List<SubTexture> plumeSprites = List.of(sprite(plume));
	private final Water water = new Water("Water", sprite(still), 1);

	@BeforeEach
	void allowTallPlumes() {
		Minecraft.setChunkHeight(64);
		chunk = new RawChunk();
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void continuousGeysersEmitAllThreeParticleFamilies(int depth) {
		pool(8, depth);
		renderGeyser("continuous");
		for (Texture texture : List.of(base, poof, plume))
			assertFalse(vertices.getOrDefault(texture, List.of()).isEmpty());
		float top = vertices.get(plume).stream().map(v -> v.position().y).max(Float::compare).orElseThrow();
		assertTrue(top > 5 + depth + depth * 3);
		assertTrue(top < 5 + depth + depth * 5 + 3);
		assertEquals(Set.of(Geometry.MeshType.AlphaTest), meshTypes);
	}

	@Test
	void moreWaterProducesATallerPlume() {
		float previous = 0;
		for (int depth = 1; depth <= 4; depth++) {
			cells.clear();
			pool(8, depth);
			renderGeyser("continuous");
			float height = vertices.get(plume).stream().map(v -> v.position().y).max(Float::compare).orElseThrow() - (5 + depth);
			assertTrue(height > previous);
			previous = height;
		}
	}

	@ParameterizedTest
	@CsvSource({"dry,false", "wet,false", "dormant,false", "continuous,true", "erupting,true", "unknown,false"})
	void onlySavedActiveStatesProduceAGeyser(String state, boolean expected) {
		pool(8, 2);
		renderGeyser(state);
		assertEquals(expected, !meshTypes.isEmpty());
	}

	@ParameterizedTest
	@ValueSource(ints = {5, 6})
	void deepWaterDoesNotProduceASurfaceGeyser(int depth) {
		pool(8, depth);
		renderGeyser("continuous");
		assertTrue(meshTypes.isEmpty());
	}

	@Test
	void flowingWaterMissingWaterAndCappedWaterDoNotErupt() {
		pool(8, 2);
		putFluid(water, 8, 5, 8, 1, false);
		renderGeyser("continuous");
		assertTrue(meshTypes.isEmpty());
		pool(8, 2);
		putSolid(8, 7, 8);
		renderGeyser("continuous");
		assertTrue(meshTypes.isEmpty());
		cells.clear();
		renderGeyser("continuous");
		assertTrue(meshTypes.isEmpty());
	}

	@Test
	void roofStopsParticlesAndTheirBillboardsBelowTheCeiling() {
		pool(8, 4);
		int roof = 13;
		for (int x = 2; x <= 14; x++)
			for (int z = 2; z <= 14; z++)
				putSolid(x, roof, z);
		renderGeyser("continuous");
		assertFalse(vertices.getOrDefault(plume, List.of()).isEmpty());
		for (Texture texture : List.of(base, poof, plume))
			assertTrue(vertices.getOrDefault(texture, List.of()).stream().allMatch(v -> v.position().y < roof));
	}

	@Test
	void missingAssetFamiliesDoNotMoveTheRemainingParticles() {
		pool(8, 3);
		renderGeyser("continuous");
		List<Vertex> first = List.copyOf(vertices.get(plume));
		renderAt(8, "continuous", List.of(), List.of(), plumeSprites);
		assertEquals(first, vertices.get(plume));
		renderAt(8, "continuous", baseSprites, poofSprites, List.of());
		assertFalse(vertices.get(base).isEmpty());
		assertFalse(vertices.get(poof).isEmpty());
		renderAt(8, "continuous", List.of(), List.of(), List.of());
		assertTrue(meshTypes.isEmpty());
	}

	@Test
	void plumeCanSpreadAcrossAChunkEdge() {
		pool(15, 4);
		renderAt(15, "continuous", baseSprites, poofSprites, plumeSprites);
		assertTrue(vertices.get(plume).stream().anyMatch(v -> v.position().x >= 16));
	}

	@ParameterizedTest
	@ValueSource(floats = {0, 0.7853982f, 1.5707964f})
	void particlesUseClockwiseBillboardsAndWorldLighting(float elevation) {
		pool(8, 3);
		cameraAngle = 2.3f;
		cameraElevation = elevation;
		defaultLight = new LightSample(0.2f, 0.4f, 0.05f);
		lightStyle = LightStyle.Night;
		renderGeyser("continuous");
		Vector3f facing = new Vector3f((float) (Math.cos(cameraAngle) * Math.cos(elevation)),
				(float) Math.sin(elevation), (float) (Math.sin(cameraAngle) * Math.cos(elevation)));
		for (Texture texture : List.of(base, poof, plume)) {
			List<Vertex> rendered = vertices.get(texture);
			for (int i = 0; i < rendered.size(); i += 4) {
				Vector3f edge1 = new Vector3f(rendered.get(i + 1).position()).sub(rendered.get(i).position());
				Vector3f edge2 = new Vector3f(rendered.get(i + 2).position()).sub(rendered.get(i + 1).position());
				assertEquals(-1, edge1.cross(edge2).normalize().dot(facing), 0.00001f);
				assertEquals(0.546f, rendered.get(i).colour().x, 0.00001f);
				assertEquals(0.4848f, rendered.get(i).colour().y, 0.00001f);
				assertEquals(0.4948f, rendered.get(i).colour().z, 0.00001f);
				assertEquals(1, rendered.get(i).colour().w);
			}
		}
	}

	private void pool(int x, int depth) {
		for (int dx = -3; dx <= 3; dx++)
			for (int dz = -3; dz <= 3; dz++)
				for (int y = 5; y < 5 + depth; y++)
					cells.put(new Position(x + dx, y, 8 + dz), new Cell(water, null, new BlockProperties(Map.of("level", "0")), 0));
	}

	private void renderGeyser(String state) { renderAt(8, state, baseSprites, poofSprites, plumeSprites); }

	private void renderAt(int x, String state, List<SubTexture> base, List<SubTexture> poof, List<SubTexture> plume) {
		render(geometry -> GeyserParticles.addGeometry(x, 4, 8, new BlockProperties(Map.of("potent_sulfur_state", state)),
				world, chunk, geometry, base, poof, plume));
	}
}
