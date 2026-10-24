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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.Minecraft;
import tectonicus.Version;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class FluidTestFixture {
	final Texture still = texture(16, 320);
	final Texture flowing = texture(32, 640);
	final Map<Position, Cell> cells = new HashMap<>();
	final Map<Texture, List<Vertex>> vertices = new HashMap<>();
	final EnumSet<Geometry.MeshType> meshTypes = EnumSet.noneOf(Geometry.MeshType.class);
	final Colour4f waterTint = new Colour4f(0.2f, 0.4f, 0.8f);
	final Map<Position, LightSample> light = new HashMap<>();
	LightSample defaultLight = LightSample.ambient(0.75f);
	LightStyle lightStyle = LightStyle.Day;
	boolean legacyWater;
	boolean grayscale;
	private int previousHeight;
	RawChunk chunk;
	BlockContext world;

	@BeforeEach
	void setUp() {
		previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(16);
		chunk = new RawChunk();
		world = (BlockContext) Proxy.newProxyInstance(BlockContext.class.getClassLoader(), new Class<?>[]{BlockContext.class},
				(proxy, method, args) -> {
					switch (method.getName()) {
						case "isLegacyWater": return legacyWater;
						case "getLightStyle": return lightStyle;
						case "getNightLightAdjustment": return 0.1f;
						case "useOldGrayscaleLighting": return grayscale;
					}
					Position position = new Position((int) args[1], (int) args[2], (int) args[3]);
					Cell cell = cells.get(position);
					return switch (method.getName()) {
						case "getBlockType" -> cell == null ? new Air() : cell.type;
						case "getBlock" -> cell == null ? null : cell.model;
						case "getBlockState" -> cell == null ? null : cell.properties;
						case "getBlockData" -> cell == null ? 0 : cell.data;
						case "getWaterColor" -> waterTint;
						case "getLightSample" -> light.getOrDefault(position, defaultLight);
						default -> throw new AssertionError("Unexpected context call: " + method.getName());
					};
				});
	}

	@AfterEach
	void restoreHeight() {
		Minecraft.setChunkHeight(previousHeight);
	}

	void basin(int x, int y, int z) {
		putSolid(x - 1, y, z);
		putSolid(x + 1, y, z);
		putSolid(x, y, z - 1);
		putSolid(x, y, z + 1);
		putSolid(x, y - 1, z);
	}

	void putSolid(int x, int y, int z) {
		cells.put(new Position(x, y, z), new Cell(new Air("minecraft:stone"), new BlockStateWrapper("minecraft:stone"), null, 0));
	}

	void putFluid(Fluid fluid, int x, int y, int z, int level, boolean legacy) {
		int data = legacy ? level : 0;
		cells.put(new Position(x, y, z), new Cell(fluid, null, legacy ? null : new BlockProperties(Map.of("level", Integer.toString(level))), data));
		if (x >= 0 && x < 16 && z >= 0 && z < 16)
			chunk.setBlockData(x, y, z, (byte) data);
	}

	List<Vertex> top(Texture texture) {
		return vertices.get(texture).subList(0, 4);
	}

	void render(Fluid type, int x, int y, int z) {
		vertices.clear();
		meshTypes.clear();
		Rasteriser rasteriser = (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
				(proxy, method, args) -> {
					if (!method.getName().equals("createMesh"))
						throw new AssertionError("Unexpected rasteriser call: " + method.getName());
					List<Vertex> meshVertices = new ArrayList<>();
					vertices.put((Texture) args[0], meshVertices);
					return Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class}, (p, m, a) -> {
						if (!m.getName().equals("addVertex"))
							throw new AssertionError("Unexpected mesh call: " + m.getName());
						meshVertices.add(new Vertex(new Vector3f((Vector3f) a[0]), new Vector4f((Vector4f) a[1]), (float) a[2], (float) a[3]));
						return null;
					});
				});
		type.addEdgeGeometry(x, y, z, world, null, chunk, new Geometry(rasteriser) {
			@Override
			public Mesh getMesh(Texture texture, MeshType type) {
				meshTypes.add(type);
				return super.getMesh(texture, type);
			}
		});
	}

	static SubTexture sprite(Texture texture) {
		return new SubTexture(texture, 0, 0, 1, 1, Version.VERSION_26_3);
	}

	static Texture texture(int width, int height) {
		return new Texture() {
			@Override public int getWidth() { return width; }
			@Override public int getHeight() { return height; }
		};
	}

	record Position(int x, int y, int z) {}
	record Cell(BlockType type, BlockStateWrapper model, BlockProperties properties, int data) {}
	record Vertex(Vector3f position, Vector4f colour, float u, float v) {}
}
