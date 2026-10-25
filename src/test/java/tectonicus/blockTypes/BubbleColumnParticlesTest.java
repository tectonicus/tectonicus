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

class BubbleColumnParticlesTest extends FluidTestFixture {
	private final Texture bubble = texture(8, 8);
	private final SubTexture sprite = sprite(bubble);

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void repeatedRendersKeepTheSameParticles(boolean down) {
		fillWater();
		renderParticles(down);
		List<Vertex> first = List.copyOf(vertices.get(bubble));
		assertFalse(first.isEmpty());
		renderParticles(down);
		assertEquals(first, vertices.get(bubble));
		assertEquals(Set.of(Geometry.MeshType.AlphaTest), meshTypes);
	}

	@Test
	void directionsProduceRisingAndDescendingDistributions() {
		fillWater();
		renderParticles(false);
		List<Vector3f> up = centers();
		renderParticles(true);
		List<Vector3f> down = centers();
		assertTrue(up.stream().mapToDouble(p -> p.y).average().orElseThrow() > 9);
		assertTrue(down.stream().mapToDouble(p -> p.y).average().orElseThrow() < 8);
		assertTrue(down.stream().allMatch(p -> p.y <= 8.8f));
		assertNotEquals(up, down);
	}

	@ParameterizedTest
	@ValueSource(floats = {0, 0.7853982f, 1.5707964f})
	void quadsFaceTheCameraClockwiseWithVanillaSizesAndSpriteUvs(float elevation) {
		fillWater();
		cameraAngle = 2.3f;
		cameraElevation = elevation;
		renderParticles(false);
		List<Vertex> rendered = vertices.get(bubble);
		Vector3f facing = new Vector3f((float) (Math.cos(cameraAngle) * Math.cos(elevation)),
				(float) Math.sin(elevation), (float) (Math.sin(cameraAngle) * Math.cos(elevation)));
		for (int i = 0; i < rendered.size(); i += 4) {
			Vector3f edge1 = new Vector3f(rendered.get(i + 1).position()).sub(rendered.get(i).position());
			Vector3f edge2 = new Vector3f(rendered.get(i + 2).position()).sub(rendered.get(i + 1).position());
			assertTrue(edge1.length() >= 0.04f && edge1.length() <= 0.32f);
			assertEquals(edge1.length(), edge2.length(), 0.00001f);
			assertEquals(-1, edge1.cross(edge2).normalize().dot(facing), 0.00001f);
			assertEquals(0, rendered.get(i).u());
			assertEquals(0, rendered.get(i).v());
			assertEquals(1, rendered.get(i + 1).u());
			assertEquals(0, rendered.get(i + 1).v());
			assertEquals(1, rendered.get(i + 2).u());
			assertEquals(1, rendered.get(i + 2).v());
		}
	}

	@Test
	void particlesUseWorldLightingWithoutWaterTintOrFading() {
		fillWater();
		defaultLight = new LightSample(0.2f, 0.4f, 0.05f);
		lightStyle = LightStyle.Night;
		renderParticles(false);
		for (Vertex vertex : vertices.get(bubble)) {
			assertEquals(0.546f, vertex.colour().x, 0.00001f);
			assertEquals(0.4848f, vertex.colour().y, 0.00001f);
			assertEquals(0.4948f, vertex.colour().z, 0.00001f);
			assertEquals(1, vertex.colour().w);
		}
	}

	@Test
	void risingParticlesCannotCrossADryLayerEvenIfWaterExistsAboveIt() {
		fillWater();
		cells.keySet().removeIf(p -> p.y() == 9);
		renderParticles(false);
		assertTrue(centers().stream().allMatch(p -> p.y < 9));
	}

	@Test
	void waterloggedCellsAllowParticlesToContinueIntoOrdinaryWater() {
		fillWater();
		for (int x = 0; x < 16; x++)
			for (int z = 0; z < 16; z++)
				cells.put(new Position(x, 9, z), new Cell(new Air("minecraft:oak_slab"), null,
						new BlockProperties(Map.of("waterlogged", "true")), 0));
		renderParticles(false);
		assertTrue(centers().stream().anyMatch(p -> p.y >= 10));
	}

	@Test
	void descendingSpiralsCanCrossChunkBoundaries() {
		fillWater();
		List<Vector3f> positions = new ArrayList<>();
		for (int z = 1; z < 15; z++) {
			int sourceZ = z;
			render(geometry -> BubbleColumnParticles.addGeometry(15, 8, sourceZ, true, List.of(sprite), world, chunk, geometry));
			positions.addAll(centers());
		}
		assertTrue(positions.stream().anyMatch(p -> p.x >= 16), "Neighbor water must remain available across a chunk edge");
		cells.keySet().removeIf(p -> p.x() >= 16);
		render(geometry -> BubbleColumnParticles.addGeometry(15, 8, 8, true, List.of(sprite), world, chunk, geometry));
		assertTrue(centers().stream().allMatch(p -> p.x < 16));
	}

	@Test
	void missingSpritesAndDryCellsEmitNoGeometry() {
		renderParticles(false);
		assertTrue(meshTypes.isEmpty());
		fillWater();
		render(geometry -> BubbleColumnParticles.addGeometry(8, 8, 8, false, List.of(), world, chunk, geometry));
		assertTrue(meshTypes.isEmpty());
	}

	private void fillWater() {
		Water water = new Water("Water", sprite(still), 1);
		Cell cell = new Cell(water, null, null, 0);
		for (int x = -2; x < 19; x++)
			for (int y = 0; y < 16; y++)
				for (int z = -2; z < 19; z++)
					cells.put(new Position(x, y, z), cell);
	}

	private void renderParticles(boolean down) {
		render(geometry -> BubbleColumnParticles.addGeometry(8, 8, 8, down, List.of(sprite), world, chunk, geometry));
	}

	private List<Vector3f> centers() {
		List<Vertex> rendered = vertices.getOrDefault(bubble, List.of());
		List<Vector3f> centers = new ArrayList<>();
		for (int i = 0; i < rendered.size(); i += 4)
			centers.add(new Vector3f(rendered.get(i).position()).add(rendered.get(i + 2).position()).mul(0.5f));
		return centers;
	}
}
