/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockregistry;

import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tectonicus.BlockContext;
import tectonicus.Minecraft;
import tectonicus.blockregistry.BlockModel.BlockElement;
import tectonicus.blockregistry.BlockModel.BlockElement.ElementFace;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlabCullingTest {
	private static final BlockStateWrapper AIR = block("minecraft:air", false);
	private static final BlockStateWrapper STONE = block("minecraft:stone", true);
	private static final BlockStateWrapper STONE_SLAB = block("minecraft:stone_slab", false);
	private static final BlockStateWrapper OAK_SLAB = block("example:oak_slab", false);
	private static final BlockStateWrapper STONE_STAIRS = block("minecraft:stone_stairs", false);
	private static final BlockStateWrapper LEAVES = block("minecraft:oak_leaves", false);
	private static final BlockStateWrapper TRAPDOOR = block("minecraft:oak_trapdoor", false);
	private static final BlockStateWrapper HOPPER = block("minecraft:hopper", false);
	private static final BlockProperties EAST_STAIRS = new BlockProperties(Map.of("facing", "east", "half", "bottom", "shape", "straight"));
	private static final Map<String, BlockStateWrapper> BLOCKS = Map.of(
			STONE.getBlockName(), STONE, STONE_SLAB.getBlockName(), STONE_SLAB, OAK_SLAB.getBlockName(), OAK_SLAB,
			STONE_STAIRS.getBlockName(), STONE_STAIRS, LEAVES.getBlockName(), LEAVES,
			TRAPDOOR.getBlockName(), TRAPDOOR, HOPPER.getBlockName(), HOPPER);
	private static final ChunkCoord ORIGIN = new ChunkCoord(0, 0);
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
	@CsvSource({"bottom,bottom,0", "top,top,0", "top,bottom,1", "top,double,0", "double,top,1", "double,double,0"})
	void comparesTheOccupiedHalvesAcrossTheSharedSide(String type, String neighborType, int expectedFaces) {
		Scene scene = new Scene();
		scene.put(3, 3, 3, STONE_SLAB, type);
		scene.put(4, 3, 3, STONE_SLAB, neighborType);
		assertEquals(expectedFaces, countFaces(scene.render(3, type), 0, 4));
	}

	@ParameterizedTest
	@CsvSource({"top,top,1,0", "top,bottom,0,0", "bottom,bottom,0,1", "bottom,top,0,0", "double,top,1,0", "bottom,double,0,1"})
	void checksWhetherStackedSlabsTouchAtTheBoundary(String lower, String upper, int expectedLowerTop, int expectedUpperBottom) {
		Scene scene = new Scene();
		scene.put(3, 3, 3, STONE_SLAB, lower);
		scene.put(3, 4, 3, STONE_SLAB, upper);
		assertEquals(expectedLowerTop, countFaces(scene.render(3, 3, lower), 1, 4));
		assertEquals(expectedUpperBottom, countFaces(scene.render(3, 4, upper), 1, 4));
	}

	@Test
	void matchingHalvesCullAcrossChunksAndNamespacesAndUseDefaultsForCompactStates() {
		Scene scene = new Scene();
		scene.put(15, 3, 3, STONE_SLAB, "bottom");
		scene.put(16, 3, 3, OAK_SLAB, null);
		assertEquals(0, countFaces(scene.render(15, "bottom"), 0, 16));
		scene.put(16, 3, 3, OAK_SLAB, "top");
		assertEquals(1, countFaces(scene.render(15, "bottom"), 0, 16));
	}

	@Test
	void stairsWithIdenticalStatesCanHaveDifferentCoverageAtTheirJoin() {
		Scene scene = new Scene();
		scene.putWithProperties(3, 3, 3, STONE_STAIRS, EAST_STAIRS);
		scene.putWithProperties(4, 3, 3, STONE_STAIRS, EAST_STAIRS);
		BakedBlockStateModel stairs = eastFacingStairModel();
		assertEquals(2, countFaces(scene.render(3, 3, stairs), 0, 4), "the high end has exposed area");
		assertEquals(0, countFaces(scene.render(4, 3, stairs), 0, 4), "the low end is fully covered");
	}

	@Test
	void comparesSlabAndStairCoverageAcrossAChunkBoundary() {
		Scene scene = new Scene();
		scene.putWithProperties(16, 3, 3, STONE_STAIRS, EAST_STAIRS);
		scene.put(15, 3, 3, STONE_SLAB, "bottom");
		assertEquals(0, countFaces(scene.render(15, "bottom"), 0, 16));
		scene.put(15, 3, 3, STONE_SLAB, "top");
		assertEquals(1, countFaces(scene.render(15, "top"), 0, 16));
	}

	@Test
	void keepsTheSideOfAClosedTrapdoorNextToAnotherTrapdoorAcrossChunks() {
		Scene scene = new Scene();
		scene.putWithProperties(15, 3, 3, TRAPDOOR, new BlockProperties(Map.of("half", "top", "open", "false")));
		BakedBlockStateModel top = cuboidModel(13, 16);
		for (String half : List.of("bottom", "top")) {
			scene.putWithProperties(16, 3, 3, TRAPDOOR, new BlockProperties(Map.of("half", half, "open", "false")));
			assertEquals(1, countFaces(scene.render(15, 3, top), 0, 16), half);
		}
	}

	@Test
	void keepsTheHopperSpoutNextToAHopperOrHalfSlabAndCullsItBehindAFullFace() {
		Scene scene = new Scene();
		scene.putWithProperties(3, 3, 3, HOPPER, new BlockProperties(Map.of("facing", "east")));
		scene.putWithProperties(4, 3, 3, HOPPER, new BlockProperties(Map.of("facing", "down")));
		SubTexture texture = new SubTexture(null, 0, 0, 1, 1);
		BlockElement spout = new BlockElement(new tectonicus.util.Vector3f(12, 4, 6), new tectonicus.util.Vector3f(16, 8, 10),
				new Vector3f(8), 0, 0, 0, false, false, Map.of("east", new ElementFace(texture, "east", 0, false, false, false)));
		BlockModel model = new BlockModel("minecraft:block/hopper_fixture", false, List.of(spout));
		model.setFullBlock(false);
		BakedBlockStateModel baked = new BakedBlockStateModel(model, 0, 0, 0, false);
		assertEquals(1, countFaces(scene.render(3, 3, baked), 0, 4));
		scene.put(4, 3, 3, STONE_SLAB, "bottom");
		assertEquals(1, countFaces(scene.render(3, 3, baked), 0, 4));
		scene.put(4, 3, 3, STONE_SLAB, "double");
		assertEquals(0, countFaces(scene.render(3, 3, baked), 0, 4));
		scene.put(4, 3, 3, STONE, null);
		assertEquals(0, countFaces(scene.render(3, 3, baked), 0, 4));
	}

	@Test
	void leavesKeepFacesNextToFullBlocksAndSlabs() {
		Scene scene = new Scene();
		scene.put(3, 3, 3, LEAVES, null);
		scene.put(4, 3, 3, STONE, null);
		scene.put(2, 3, 3, STONE_SLAB, "double");
		List<Vector3f> vertices = scene.render(3, "double");
		assertEquals(1, countFaces(vertices, 0, 3));
		assertEquals(1, countFaces(vertices, 0, 4));
	}

	private static BakedBlockStateModel eastFacingStairModel() {
		SubTexture texture = new SubTexture(null, 0, 0, 1, 1);
		ElementFace east = new ElementFace(texture, "east", 0, false, false, false);
		ElementFace west = new ElementFace(texture, "west", 0, false, false, false);
		BlockElement base = new BlockElement(new tectonicus.util.Vector3f(0, 0, 0), new tectonicus.util.Vector3f(16, 8, 16),
				new Vector3f(8), 0, 0, 0, false, false, Map.of("east", east, "west", west));
		BlockElement step = new BlockElement(new tectonicus.util.Vector3f(8, 8, 0), new tectonicus.util.Vector3f(16, 16, 16),
				new Vector3f(8), 0, 0, 0, false, false, Map.of("east", east));
		BlockModel model = new BlockModel("minecraft:block/stairs_fixture", false, List.of(base, step));
		model.setFullBlock(false);
		return new BakedBlockStateModel(model, 0, 0, 0, false);
	}

	@Test
	void keepsInsetFacesWhenEveryNeighborCoversTheBoundary() {
		Scene scene = new Scene();
		scene.put(3, 3, 3, STONE_SLAB, "top");
		scene.put(3, 4, 3, STONE_SLAB, "bottom");
		scene.put(3, 2, 3, STONE, null);
		scene.put(2, 3, 3, STONE, null);
		scene.put(4, 3, 3, STONE, null);
		scene.put(3, 3, 2, STONE, null);
		scene.put(3, 3, 4, STONE, null);
		List<Vector3f> vertices = scene.render(3, "top");
		assertEquals(4, vertices.size());
		assertEquals(1, countFaces(vertices, 1, 3.5f));
	}

	@Test
	void keepsAnExposedUndersideWhenASlabCoversTheTopOfACube() {
		Scene scene = new Scene();
		scene.put(3, 3, 3, STONE, null);
		scene.put(3, 4, 3, STONE_SLAB, "bottom");
		scene.put(2, 3, 3, STONE, null);
		scene.put(4, 3, 3, STONE, null);
		scene.put(3, 3, 2, STONE, null);
		scene.put(3, 3, 4, STONE, null);
		List<Vector3f> vertices = scene.render(3, "double");
		assertEquals(4, vertices.size());
		assertEquals(1, countFaces(vertices, 1, 3));
	}

	@Test
	void keepsTheUndersideOfAFullBlockAboveAHopper() {
		Scene scene = new Scene();
		scene.put(3, 3, 3, STONE, null);
		scene.put(3, 4, 3, STONE, null);
		scene.put(2, 3, 3, STONE, null);
		scene.put(4, 3, 3, STONE, null);
		scene.put(3, 3, 2, STONE, null);
		scene.put(3, 3, 4, STONE, null);
		scene.put(3, 2, 3, HOPPER, null);
		List<Vector3f> vertices = scene.render(3, "double");
		assertEquals(4, vertices.size());
		assertEquals(1, countFaces(vertices, 1, 3));
	}

	private static BlockStateWrapper block(String name, boolean opaque) {
		BlockStateWrapper block = new BlockStateWrapper(name);
		block.setFullBlock(opaque);
		block.setFullOpaqueBlock(opaque);
		if (block.isSlab())
			block.setDefaultProperties(Map.of("type", "bottom", "waterlogged", "false"));
		return block;
	}

	private static BakedBlockStateModel model(String type) {
		return cuboidModel(type.equals("top") ? 8 : 0, type.equals("bottom") ? 8 : 16);
	}

	private static BakedBlockStateModel cuboidModel(int bottom, int top) {
		Map<String, ElementFace> faces = new HashMap<>();
		SubTexture texture = new SubTexture(null, 0, 0, 1, 1);
		for (String direction : List.of("up", "down", "east", "west", "north", "south")) {
			boolean inset = direction.equals("up") && top < 16 || direction.equals("down") && bottom > 0;
			faces.put(direction, new ElementFace(texture, inset ? null : direction, 0, false, false, false));
		}
		BlockElement element = new BlockElement(new tectonicus.util.Vector3f(0, bottom, 0), new tectonicus.util.Vector3f(16, top, 16),
				new Vector3f(8), 0, 0, 0, false, false, faces);
		BlockModel model = new BlockModel("minecraft:block/cuboid_fixture", false, List.of(element));
		model.setFullBlock(bottom == 0 && top == 16);
		return new BakedBlockStateModel(model, 0, 0, 0, false);
	}

	private static int countFaces(List<Vector3f> vertices, int axis, float plane) {
		int count = 0;
		for (int offset = 0; offset < vertices.size(); offset += 4) {
			if (vertices.subList(offset, offset + 4).stream().allMatch(vertex -> vertex.get(axis) == plane))
				count++;
		}
		return count;
	}

	private static final class Scene {
		private final RawChunk origin = new RawChunk() {
			@Override public ChunkCoord getChunkCoord() { return ORIGIN; }
		};
		private final RawChunk east = new RawChunk();
		private int edgeLookups;

		private RawChunk chunk(int x) { return x < RawChunk.WIDTH ? origin : east; }

		private void put(int x, int y, int z, BlockStateWrapper block, String type) {
			putWithProperties(x, y, z, block, type == null ? null : new BlockProperties(Map.of("type", type)));
		}

		private void putWithProperties(int x, int y, int z, BlockStateWrapper block, BlockProperties properties) {
			chunk(x).setBlockName(x % RawChunk.WIDTH, y, z, block.getBlockName());
			chunk(x).setBlockState(x % RawChunk.WIDTH, y, z, properties);
		}

		private List<Vector3f> render(int x, String type) { return render(x, 3, type); }

		private List<Vector3f> render(int x, int y, String type) {
			return render(x, y, model(type));
		}

		private List<Vector3f> render(int x, int y, BakedBlockStateModel model) {
			List<Vector3f> vertices = new ArrayList<>();
			edgeLookups = 0;
			Mesh mesh = (Mesh) Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class},
					(proxy, method, args) -> {
						if (!method.getName().equals("addVertex")) throw new AssertionError(method.getName());
						vertices.add(new Vector3f((Vector3f) args[0]));
						return null;
					});
			Rasteriser rasteriser = (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
					(proxy, method, args) -> mesh);
			BlockContext world = (BlockContext) Proxy.newProxyInstance(BlockContext.class.getClassLoader(), new Class<?>[]{BlockContext.class},
					(proxy, method, args) -> switch (method.getName()) {
						case "getBlock" -> {
							int nx = (int) args[1];
							assertTrue(nx < RawChunk.WIDTH, "chunk-edge lookups should use the combined method");
							String name = origin.getBlockName(nx, (int) args[2], (int) args[3]);
							yield name == null ? AIR : BLOCKS.getOrDefault(name, AIR);
						}
						case "getBlockWithProperties" -> {
							edgeLookups++;
							assertEquals(ORIGIN, args[0]);
							assertEquals(RawChunk.WIDTH, args[1]);
							int ny = (int) args[2], nz = (int) args[3];
							RawChunk.BlockName entry = east.getNamedBlock(0, ny, nz);
							BlockStateWrapper block = entry == null ? AIR : BLOCKS.getOrDefault(entry.getName(), AIR);
							yield new BlockContext.BlockWithProperties(entry, block, east.getBlockState(0, ny, nz));
						}
						case "getLightStyle" -> LightStyle.None;
						case "shouldCullSameBlockFaces" -> true;
						case "isSmoothLit" -> false;
						case "getNightLightAdjustment" -> 0f;
						case "getLight" -> 1f;
						default -> throw new AssertionError(method.getName());
					});
			model.createGeometry(x, y, 3, world, origin, new Geometry(rasteriser));
			assertEquals(x == RawChunk.WIDTH - 1 ? 1 : 0, edgeLookups);
			return vertices;
		}
	}
}
