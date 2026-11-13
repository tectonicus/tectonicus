/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.rasteriser;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.BlockContext;
import tectonicus.BlockOcclusion;
import tectonicus.BlockTypeRegistry;
import tectonicus.ItemRenderer.ItemContext;
import tectonicus.Minecraft;
import tectonicus.Version;
import tectonicus.blockTypes.Air;
import tectonicus.blockTypes.BlockUtil;
import tectonicus.blockTypes.GlassPane;
import tectonicus.blockTypes.Slab;
import tectonicus.blockTypes.SolidBlockType;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.MeshUtil.FaceDirection;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LegacyBlockLightingTest {
	private static final SubTexture TEXTURE = new SubTexture(null, 0.1f, 0.2f, 0.3f, 0.4f, Version.VERSION_4);
	private static final float FLAT_LIGHT = 0.4f;
	private static final float EPSILON = 0.000001f;
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

	@Test
	void cubeEdgeAndInteriorPathsBlendLightAcrossDiagonalChunkAtOneCorner() {
		RawChunk chunk = new RawChunk();
		chunk.setBlockId(15, 7, 15, 1);
		BlockContext flat = context(false, false, Map.of(), Map.of());
		Cell diagonal = new Cell(16, 8, 16);
		BlockContext smooth = context(true, false, Map.of(diagonal, LightSample.overworld(6, 10, 1)),
				Map.of(diagonal, BlockOcclusion.OPAQUE));
		SolidBlockType cube = new SolidBlockType("Stone", TEXTURE, new Colour4f(0.75f, 0.5f, 0.25f, 0.6f), false);
		BlockTypeRegistry registry = new BlockTypeRegistry();
		List<Vertex> original = new ArrayList<>(), edge = new ArrayList<>(), interior = new ArrayList<>();
		cube.addEdgeGeometry(15, 7, 15, flat, registry, chunk, geometry(original));
		cube.addEdgeGeometry(15, 7, 15, smooth, registry, chunk, geometry(edge));
		cube.addInteriorGeometry(15, 7, 15, smooth, registry, chunk, geometry(interior));

		assertEquals(24, original.size());
		assertEquals(edge, interior);
		assertGeometryEquals(original, edge);
		for (Vertex vertex : original)
			assertEquals(new Vector4f(0.75f * FLAT_LIGHT, 0.5f * FLAT_LIGHT, 0.25f * FLAT_LIGHT, 0.6f), vertex.colour);

		Colour4f uniform = LightSample.overworld(10, 2, 1).toColour(LightStyle.Day);
		Colour4f blended = LightSample.overworld(9, 4, 1).toColour(LightStyle.Day);
		assertColour(edge.get(0), uniform, 1, 0.75f, 0.5f, 0.25f, 0.6f);
		assertColour(edge.get(1), uniform, 1, 0.75f, 0.5f, 0.25f, 0.6f);
		assertColour(edge.get(2), blended, 0.8f, 0.75f, 0.5f, 0.25f, 0.6f);
		assertColour(edge.get(3), uniform, 1, 0.75f, 0.5f, 0.25f, 0.6f);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void slabInterpolatesHalfHeightAndHonorsGrayscale(boolean grayscale) {
		RawChunk chunk = new RawChunk();
		chunk.setBlockId(1, 7, 1, 44);
		Cell upperNeighbor = new Cell(2, 8, 1);
		BlockContext world = context(true, grayscale, Map.of(upperNeighbor, LightSample.overworld(14, 10, 1)),
				Map.of(upperNeighbor, BlockOcclusion.TRANSLUCENT));
		Slab slab = new Slab("Slab", TEXTURE, TEXTURE);
		List<Vertex> vertices = new ArrayList<>();
		slab.addEdgeGeometry(1, 7, 1, world, new BlockTypeRegistry(), chunk, geometry(vertices));

		// The top edge of the +X face is halfway between two samples, one with a shaded neighbor.
		assertEquals(new Vector3f(2, 7.5f, 2), vertices.get(12).position);
		Colour4f light = LightSample.overworld(10.5f, 3, 1).toColour(LightStyle.Day, grayscale);
		float shade = Chunk.getFaceFactor(LightStyle.Day, LightFace.NorthSouth);
		assertColour(vertices.get(12), light, 0.9f * shade, 1, 1, 1, 1);
	}

	@Test
	void rotatedLogFaceKeepsVertexOrderAndUvs() {
		RawChunk chunk = new RawChunk();
		BlockContext flat = context(false, false, Map.of(), Map.of());
		BlockContext smooth = context(true, false, Map.of(new Cell(1, 8, 1), LightSample.overworld(5, 4, 1)), Map.of());
		List<Vertex> original = new ArrayList<>(), updated = new ArrayList<>();
		BlockUtil.addTop(flat, chunk, mesh(original), 0, 7, 0, new Colour4f(1, 1, 1, 1), TEXTURE,
				LegacyBlockLighting.forBlock(flat, chunk, 0, 7, 0), Rotation.Clockwise);
		BlockUtil.addTop(smooth, chunk, mesh(updated), 0, 7, 0, new Colour4f(1, 1, 1, 1), TEXTURE,
				LegacyBlockLighting.forBlock(smooth, chunk, 0, 7, 0), Rotation.Clockwise);

		assertGeometryEquals(original, updated);
		assertEquals(new Vector3f(1, 8, 0), updated.getFirst().position);
		assertNotEquals(updated.get(0).colour, updated.get(1).colour);
	}

	@Test
	void isolatedGlassPaneUsesSmoothLightingAndKeepsItsPostGeometry() {
		RawChunk chunk = new RawChunk();
		chunk.setBlockId(1, 7, 1, 102);
		SubTexture texture = new SubTexture(null, TEXTURE.u0, TEXTURE.v0, TEXTURE.u1, TEXTURE.v1, Version.VERSION_12);
		GlassPane pane = new GlassPane("Glass Pane", texture);
		BlockContext flat = context(false, false, Map.of(), Map.of());
		BlockContext smooth = context(true, false, Map.of(new Cell(0, 8, 0), LightSample.overworld(3, 2, 1)), Map.of());
		List<Vertex> original = new ArrayList<>(), flatVertices = new ArrayList<>(), smoothVertices = new ArrayList<>();
		SubMesh post = new SubMesh();
		SubMesh.addBlock(post, 0.5f, 0, 0.5f, 0.125f, 1, 0.125f,
				new Vector4f(FLAT_LIGHT, FLAT_LIGHT, FLAT_LIGHT, 1), texture, texture, texture);
		post.pushTo(mesh(original), 1, 7, 1, Rotation.None, 0);
		pane.addEdgeGeometry(1, 7, 1, flat, new BlockTypeRegistry(), chunk, geometry(flatVertices));
		pane.addEdgeGeometry(1, 7, 1, smooth, new BlockTypeRegistry(), chunk, geometry(smoothVertices));

		assertEquals(24, smoothVertices.size());
		assertEquals(original, flatVertices);
		assertGeometryEquals(original, smoothVertices);
		assertNotEquals(smoothVertices.get(16).colour, smoothVertices.get(18).colour);
	}

	@Test
	void emitsExistingFlatGeometryForLuminousBlocksAndItemIcons() {
		RawChunk chunk = new RawChunk();
		chunk.setBlockId(1, 7, 1, 89);
		assertSame(LegacyBlockLighting.FLAT,
				LegacyBlockLighting.forBlock(context(true, false, Map.of(), Map.of()), chunk, 1, 7, 1));
		chunk.setBlockId(1, 7, 1, 1);
		assertSame(LegacyBlockLighting.FLAT,
				LegacyBlockLighting.forBlock(new ItemContext(null, null, null), chunk, 1, 7, 1));
	}

	@Test
	void insetFaceUsesItsOwnCenterWhenOutwardBlockIsOpaque() {
		RawChunk chunk = new RawChunk();
		Cell ownCell = new Cell(1, 7, 1), outward = new Cell(1, 8, 1);
		Map<Cell, LightSample> samples = Map.of(ownCell, LightSample.overworld(15, 10, 1),
				outward, LightSample.overworld(4, 1, 1));
		List<Vertex> blocked = new ArrayList<>(), open = new ArrayList<>();
		Vector3f[] points = {new Vector3f(1, 7.5f, 1), new Vector3f(2, 7.5f, 1),
				new Vector3f(2, 7.5f, 2), new Vector3f(1, 7.5f, 2)};
		LegacyBlockLighting.forBlock(context(true, false, samples, Map.of(outward, BlockOcclusion.OPAQUE)), chunk, 1, 7, 1)
				.addQuad(mesh(blocked), FaceDirection.UP, LightFace.Top, points[0], points[1], points[2], points[3],
						LegacyBlockLighting.WHITE, 0, TEXTURE);
		LegacyBlockLighting.forBlock(context(true, false, samples, Map.of()), chunk, 1, 7, 1)
				.addQuad(mesh(open), FaceDirection.UP, LightFace.Top, points[0], points[1], points[2], points[3],
						LegacyBlockLighting.WHITE, 0, TEXTURE);
		assertTrue(blocked.getFirst().colour.x > open.getFirst().colour.x);
	}

	private static BlockContext context(boolean smooth, boolean grayscale, Map<Cell, LightSample> lights,
			Map<Cell, BlockOcclusion> occlusion) {
		return proxy(BlockContext.class, (p, method, args) -> switch (method.getName()) {
			case "isLegacySmoothLit" -> smooth;
			case "getLightStyle" -> LightStyle.Day;
			case "getNightLightAdjustment" -> 0f;
			case "useOldGrayscaleLighting" -> grayscale;
			case "getBlockType" -> new Air();
			case "getLight" -> FLAT_LIGHT;
			case "getLightSample" -> lights.getOrDefault(cell(args), LightSample.overworld(10, 2, 1));
			case "getBlockOcclusion" -> occlusion.getOrDefault(cell(args), BlockOcclusion.OPEN);
			default -> throw new AssertionError("Unexpected world call " + method.getName());
		});
	}

	private static Cell cell(Object[] arguments) {
		return new Cell((int) arguments[1], (int) arguments[2], (int) arguments[3]);
	}

	private static Mesh mesh(List<Vertex> vertices) {
		return proxy(Mesh.class, (p, method, args) -> {
			if (!method.getName().equals("addVertex"))
				throw new AssertionError("Unexpected mesh call " + method.getName());
			vertices.add(new Vertex(new Vector3f((Vector3f) args[0]), new Vector4f((Vector4f) args[1]),
					(float) args[2], (float) args[3]));
			return null;
		});
	}

	private static Geometry geometry(List<Vertex> vertices) {
		return new Geometry(proxy(Rasteriser.class, (p, method, args) -> mesh(vertices)));
	}

	private static <T> T proxy(Class<T> type, InvocationHandler handler) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
	}

	private static void assertGeometryEquals(List<Vertex> expected, List<Vertex> actual) {
		assertEquals(expected.size(), actual.size());
		for (int i = 0; i < expected.size(); i++) {
			assertEquals(expected.get(i).position, actual.get(i).position);
			assertEquals(expected.get(i).u, actual.get(i).u);
			assertEquals(expected.get(i).v, actual.get(i).v);
		}
	}

	private static void assertColour(Vertex vertex, Colour4f light, float ao, float r, float g, float b, float alpha) {
		assertEquals(light.r * ao * r, vertex.colour.x, EPSILON);
		assertEquals(light.g * ao * g, vertex.colour.y, EPSILON);
		assertEquals(light.b * ao * b, vertex.colour.z, EPSILON);
		assertEquals(alpha, vertex.colour.w, EPSILON);
	}

	private record Vertex(Vector3f position, Vector4f colour, float u, float v) {}

	private record Cell(int x, int y, int z) {}
}
