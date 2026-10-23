/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser;

import lombok.experimental.UtilityClass;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.Version;
import tectonicus.blockregistry.BakedBlockStateModel;
import tectonicus.blockregistry.BakedBlockStateModel.BakedQuad;
import tectonicus.blockregistry.BlockModel;
import tectonicus.blockregistry.BlockModel.BlockElement;
import tectonicus.blockregistry.BlockModel.BlockElement.ElementFace;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.renderer.Geometry.MeshType;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;
import tectonicus.util.Util;

import java.util.Set;

@UtilityClass
public class MeshUtil {
	private static final float BOOKSHELF_BOOK_WORLD_LIGHT_WEIGHT = 0.25f;

	public void addQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, SubTexture texture) {
		mesh.addVertex(p0, colour, texture.u0, texture.v0);
		mesh.addVertex(p1, colour, texture.u1, texture.v0);
		mesh.addVertex(p2, colour, texture.u1, texture.v1);
		mesh.addVertex(p3, colour, texture.u0, texture.v1);
	}
	
	public void addDoubleSidedQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, SubTexture texture) {
		// Clockwise
		mesh.addVertex(p0, colour, texture.u0, texture.v0);
		mesh.addVertex(p1, colour, texture.u1, texture.v0);
		mesh.addVertex(p2, colour, texture.u1, texture.v1);
		mesh.addVertex(p3, colour, texture.u0, texture.v1);
		
		// Anticlockwise
		mesh.addVertex(p0, colour, texture.u0, texture.v0);
		mesh.addVertex(p3, colour, texture.u0, texture.v1);
		mesh.addVertex(p2, colour, texture.u1, texture.v1);
		mesh.addVertex(p1, colour, texture.u1, texture.v0);
	}
	
	public void addDoubleSidedQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, Vector2f uv0, Vector2f uv1, Vector2f uv2, Vector2f uv3)
	{
		// Clockwise
		mesh.addVertex(p0, colour, uv0.x, uv0.y);
		mesh.addVertex(p1, colour, uv1.x, uv1.y);
		mesh.addVertex(p2, colour, uv2.x, uv2.y);
		mesh.addVertex(p3, colour, uv3.x, uv3.y);
		
		// Anticlockwise
		mesh.addVertex(p0, colour, uv0.x, uv0.y);
		mesh.addVertex(p3, colour, uv3.x, uv3.y);
		mesh.addVertex(p2, colour, uv2.x, uv2.y);
		mesh.addVertex(p1, colour, uv1.x, uv1.y);
	}

	public void addQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, Vector2f uv0, Vector2f uv1, Vector2f uv2, Vector2f uv3)
	{
		mesh.addVertex(p0, colour, uv0.x, uv0.y);
		mesh.addVertex(p1, colour, uv1.x, uv1.y);
		mesh.addVertex(p2, colour, uv2.x, uv2.y);
		mesh.addVertex(p3, colour, uv3.x, uv3.y);
	}

	/** Emits geometry that was transformed and UV-mapped during blockstate model baking. */
	public void addBakedBlock(BlockContext world, RawChunk rawChunk, int x, int y, int z, BakedBlockStateModel bakedModel, Geometry geometry) {
		BlockModel model = bakedModel.getSourceModel();
		if (model == null || bakedModel.getQuads().isEmpty())
			return;

		boolean oldBlockStates = world.getTexturePack().getVersion().getNumVersion() <= Version.VERSION_12.getNumVersion();
		BlockStateWrapper selfBlock = oldBlockStates ? null : world.getBlock(rawChunk, x, y, z);
		
		boolean blockEmitsLight = !oldBlockStates && selfBlock != null
				&& selfBlock.getLightEmission(rawChunk.getBlockState(x, y, z)) > 0;
		boolean above = oldBlockStates
				? world.getBlockType(rawChunk, x, y + 1, z).isSolid()
				: isFaceCovered(world, selfBlock, world.getBlock(rawChunk, x, y + 1, z));
		boolean below = oldBlockStates
				? world.getBlockType(rawChunk, x, y - 1, z).isSolid()
				: isFaceCovered(world, selfBlock, world.getBlock(rawChunk, x, y - 1, z));
		boolean north = oldBlockStates
				? world.getBlockType(rawChunk, x, y, z - 1).isSolid()
				: isFaceCovered(world, selfBlock, world.getBlock(rawChunk, x, y, z - 1));
		boolean south = oldBlockStates
				? world.getBlockType(rawChunk, x, y, z + 1).isSolid()
				: isFaceCovered(world, selfBlock, world.getBlock(rawChunk, x, y, z + 1));
		boolean east = oldBlockStates
				? world.getBlockType(rawChunk, x + 1, y, z).isSolid()
				: isFaceCovered(world, selfBlock, world.getBlock(rawChunk, x + 1, y, z));
		boolean west = oldBlockStates
				? world.getBlockType(rawChunk, x - 1, y, z).isSolid()
				: isFaceCovered(world, selfBlock, world.getBlock(rawChunk, x - 1, y, z));

		if (above && north && south && east && west)
			return;

		boolean selfFull = oldBlockStates || selfBlock.isFullBlock();
		if (!selfFull && model.isFullBlock())
			selfFull = true;
		boolean samplePartialModelFromCell = !selfFull
				&& !selfBlock.getBlockName().contains("stair")
				&& !selfBlock.getBlockName().contains("slab");
		
		float nightLightAdjustment = world.getNightLightAdjustment();
		
		float topLight = world.getLight(rawChunk, x, y + 1, z, LightFace.Top, nightLightAdjustment);
		float bottomLight = world.getLight(rawChunk, x, y - 1, z, LightFace.Bottom, nightLightAdjustment);
		float northLight = world.getLight(rawChunk, x, y, z - 1, LightFace.NorthSouth, nightLightAdjustment);
		float southLight = world.getLight(rawChunk, x, y, z + 1, LightFace.NorthSouth, nightLightAdjustment);
		float eastLight = world.getLight(rawChunk, x + 1, y, z, LightFace.EastWest, nightLightAdjustment);
		float westLight = world.getLight(rawChunk, x - 1, y, z, LightFace.EastWest, nightLightAdjustment);
		// Use the same adjacent-cell face samples for full cubes and partial models.
		// Stairs and slabs keep their shape-aware average because their faces occupy
		// only part of the block and their surrounding cells otherwise overstate light.
		if (!selfFull) {
			String blockName = selfBlock.getBlockName();
			if (blockName.contains("stair") || blockName.contains("slab")) {
				BlockStateWrapper aboveBlock = world.getBlock(rawChunk, x, y + 1, z);
				BlockStateWrapper belowBlock = world.getBlock(rawChunk, x, y - 1, z);
				BlockStateWrapper northBlock = world.getBlock(rawChunk, x, y, z - 1);
				BlockStateWrapper southBlock = world.getBlock(rawChunk, x, y, z + 1);
				BlockStateWrapper eastBlock = world.getBlock(rawChunk, x + 1, y, z);
				BlockStateWrapper westBlock = world.getBlock(rawChunk, x - 1, y, z);
				float total = 0;
				int count = 0;
				if (!above && !isStairOrSlab(aboveBlock)) { total += topLight; count++; }
				if (!below && !isStairOrSlab(belowBlock)) { total += bottomLight; count++; }
				if (!north && !isStairOrSlab(northBlock)) { total += northLight; count++; }
				if (!south && !isStairOrSlab(southBlock)) { total += southLight; count++; }
				if (!east && !isStairOrSlab(eastBlock)) { total += eastLight; count++; }
				if (!west && !isStairOrSlab(westBlock)) { total += westLight; count++; }
				float average = count == 0 ? 0 : Util.clamp(total / count, 0, 1);
				topLight = bottomLight = northLight = southLight = eastLight = westLight = average;
			} else {
				topLight = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z, nightLightAdjustment);
				northLight = southLight = Chunk.getLight(world.getLightStyle(), LightFace.NorthSouth, rawChunk, x, y, z, nightLightAdjustment);
				eastLight = westLight = Chunk.getLight(world.getLightStyle(), LightFace.EastWest, rawChunk, x, y, z, nightLightAdjustment);
				bottomLight = Chunk.getLight(world.getLightStyle(), LightFace.Bottom, rawChunk, x, y, z, nightLightAdjustment);
			}
		}

		Matrix4f rotation = new Matrix4f()
				.rotateZ((float) Math.toRadians(-bakedModel.getZRotation()))
				.rotateY((float) Math.toRadians(-bakedModel.getYRotation()))
				.rotateX((float) Math.toRadians(-bakedModel.getXRotation()));
		boolean bookshelfBookOverlay = model.getName().contains("chiseled_bookshelf_occupied_slot_");
		for (BakedQuad quad : bakedModel.getQuads()) {
			ElementFace face = quad.getFace();
			FaceDirection direction = rotatedFaceDirection(rotation, quad.getSourceFace());
			FaceDirection lightDirection = quad.getShadeDirectionOverride() != null
					? faceDirection(quad.getShadeDirectionOverride())
					: direction;
			FaceDirection cullDirection = quad.getCullDirection() == null ? null : faceDirection(quad.getCullDirection());
			if (cullDirection != null && isDirectionCovered(cullDirection, above, below, north, south, east, west))
				continue;

			// shade=false disables directional face shading, not the block's world lighting.
			float light;
			if (bookshelfBookOverlay) {
				float worldLight = world.getLight(rawChunk, x, y, z, LightFace.Top, nightLightAdjustment);
				light = 1.0f - BOOKSHELF_BOOK_WORLD_LIGHT_WEIGHT * (1.0f - worldLight);
			} else if (!quad.isShaded()) {
				int sampleX = x, sampleY = y, sampleZ = z;
				if (!samplePartialModelFromCell) {
					switch (direction) {
						case UP -> sampleY++;
						case DOWN -> sampleY--;
						case NORTH -> sampleZ--;
						case SOUTH -> sampleZ++;
						case EAST -> sampleX++;
						case WEST -> sampleX--;
					}
				}
				// Keep shade=false faces free of cardinal darkening, but sample the light
				// across their actual surface so exposed tops still see full skylight.
				light = world.getLight(rawChunk, sampleX, sampleY, sampleZ, LightFace.Top, nightLightAdjustment);
			} else {
				light = getDirectionLight(lightDirection, topLight, bottomLight, northLight, southLight, eastLight, westLight);
			}
			Colour4f tint = face.isTinted()
					? getTintColor(model.getName(), rawChunk, world, x, y, z, null)
					: new Colour4f(1, 1, 1, 1);

			MeshType meshType = model.getName().contains("grass_block") && !quad.isGrassOverlay()
					? MeshType.Solid
					: face.isTranslucent() ? MeshType.Transparent
					: face.isTransparent() ? MeshType.AlphaTest : MeshType.Solid;
			Mesh mesh = geometry.getMesh(face.getTexture().texture, meshType);
			for (int vertex = 0; vertex < 4; vertex++) {
				Colour4f vertexLight = new Colour4f(light, light, light, 1);
				if (world.isSmoothLit() && world.getLightStyle() == LightStyle.Night && !bookshelfBookOverlay) {
					// Smooth lighting interpolates the light field around each quad corner.
					// This is separate from model AO: models with ambientocclusion=false
					// still receive smooth sky/block lighting, just without corner occlusion.
					FaceDirection lightFace = quad.isShaded() ? lightDirection : FaceDirection.UP;
					LightSample lightSample = getSmoothVertexLight(world, rawChunk, x, y, z, direction, lightFace, samplePartialModelFromCell,
							quad.getPosition(vertex, 0), quad.getPosition(vertex, 1), quad.getPosition(vertex, 2),
							nightLightAdjustment);
					vertexLight = lightSample.toColour(world.getLightStyle(), world.useOldGrayscaleLighting());
				}
				// Chiseled bookshelf books are single-face overlays on the front plane.
				// The baked AO approximation treats their small rectangles as block corners,
				// which darkens the book art much more than the surrounding bookshelf face.
				float ao = selfFull && !bookshelfBookOverlay && !blockEmitsLight && world.isSmoothLit()
						&& model.isAmbientlyOccluded() && direction != FaceDirection.DOWN
						? getBakedAo(world, rawChunk, x, y, z, direction, quad, vertex, nightLightAdjustment) : 1.0f;
				Vector4f vertexColour = new Vector4f(vertexLight.r * tint.r * ao, vertexLight.g * tint.g * ao,
						vertexLight.b * tint.b * ao, 1);
				mesh.addVertex(new Vector3f(x + quad.getPosition(vertex, 0), y + quad.getPosition(vertex, 1), z + quad.getPosition(vertex, 2)),
						vertexColour, quad.getUv(vertex, 0), quad.getUv(vertex, 1));
			}
		}
	}

	private LightSample getSmoothVertexLight(BlockContext world, RawChunk rawChunk, int x, int y, int z,
			FaceDirection surface, FaceDirection shadeDirection, boolean sampleFromCell,
			float vertexX, float vertexY, float vertexZ,
			float nightLightAdjustment) {
		int nx = 0, ny = 0, nz = 0;
		int tangentAX, tangentAY, tangentAZ, tangentBX, tangentBY, tangentBZ;
		switch (surface) {
			case UP -> { ny = 1; tangentAX = 1; tangentAY = 0; tangentAZ = 0; tangentBX = 0; tangentBY = 0; tangentBZ = 1; }
			case DOWN -> { ny = -1; tangentAX = 1; tangentAY = 0; tangentAZ = 0; tangentBX = 0; tangentBY = 0; tangentBZ = 1; }
			case NORTH -> { nz = -1; tangentAX = 1; tangentAY = 0; tangentAZ = 0; tangentBX = 0; tangentBY = 1; tangentBZ = 0; }
			case SOUTH -> { nz = 1; tangentAX = 1; tangentAY = 0; tangentAZ = 0; tangentBX = 0; tangentBY = 1; tangentBZ = 0; }
			case EAST -> { nx = 1; tangentAX = 0; tangentAY = 1; tangentAZ = 0; tangentBX = 0; tangentBY = 0; tangentBZ = 1; }
			case WEST -> { nx = -1; tangentAX = 0; tangentAY = 1; tangentAZ = 0; tangentBX = 0; tangentBY = 0; tangentBZ = 1; }
			default -> throw new IllegalStateException("Unexpected face direction: " + surface);
		}
		if (sampleFromCell) {
			nx = ny = nz = 0;
		}
		int offsetA = vertexCoordinate(vertexX, vertexY, vertexZ, tangentAX, tangentAY, tangentAZ) >= 0.5f ? 1 : -1;
		int offsetB = vertexCoordinate(vertexX, vertexY, vertexZ, tangentBX, tangentBY, tangentBZ) >= 0.5f ? 1 : -1;
		LightFace lightFace = toLightFace(shadeDirection);
		LightSample total = world.getLightSample(rawChunk, x, y, z, lightFace, nightLightAdjustment);
		for (int a = 0; a <= 1; a++) {
			for (int b = 0; b <= 1; b++) {
				total = total.add(world.getLightSample(rawChunk,
						x + nx + (a == 1 ? offsetA * tangentAX : 0) + (b == 1 ? offsetB * tangentBX : 0),
						y + ny + (a == 1 ? offsetA * tangentAY : 0) + (b == 1 ? offsetB * tangentBY : 0),
						z + nz + (a == 1 ? offsetA * tangentAZ : 0) + (b == 1 ? offsetB * tangentBZ : 0),
						lightFace, nightLightAdjustment));
			}
		}
		return total.scale(0.2f);
	}

	private float vertexCoordinate(float x, float y, float z, int axisX, int axisY, int axisZ) {
		return x * axisX + y * axisY + z * axisZ;
	}

	private LightFace toLightFace(FaceDirection direction) {
		return switch (direction) {
			case UP -> LightFace.Top;
			case DOWN -> LightFace.Bottom;
			case NORTH, SOUTH -> LightFace.NorthSouth;
			case EAST, WEST -> LightFace.EastWest;
		};
	}

	private boolean isStairOrSlab(BlockStateWrapper block) {
		return block.getBlockName().contains("stair") || block.getBlockName().contains("slab");
	}

	private float getBakedAo(BlockContext world, RawChunk rawChunk, int x, int y, int z, FaceDirection face,
			BakedQuad quad, int vertex, float nightLightAdjustment) {
		int nx = 0, ny = 0, nz = 0;
		switch (face) {
			case UP -> ny = 1;
			case DOWN -> ny = -1;
			case NORTH -> nz = -1;
			case SOUTH -> nz = 1;
			case EAST -> nx = 1;
			case WEST -> nx = -1;
		}
		int px = quad.getPosition(vertex, 0) >= 0.5f ? 1 : -1;
		int py = quad.getPosition(vertex, 1) >= 0.5f ? 1 : -1;
		int pz = quad.getPosition(vertex, 2) >= 0.5f ? 1 : -1;
		int[] tangents = switch (face) {
			case UP, DOWN -> new int[]{px, 0, 0, 0, 0, pz};
			case NORTH, SOUTH -> new int[]{px, 0, 0, 0, py, 0};
			case EAST, WEST -> new int[]{0, py, 0, 0, 0, pz};
		};
		float a = world.getLight(rawChunk, x + nx + tangents[0], y + ny + tangents[1], z + nz + tangents[2], LightFace.Top,
				world.getLightStyle() == LightStyle.Night ? 0 : nightLightAdjustment);
		float b = world.getLight(rawChunk, x + nx + tangents[3], y + ny + tangents[4], z + nz + tangents[5], LightFace.Top,
				world.getLightStyle() == LightStyle.Night ? 0 : nightLightAdjustment);
		float c = world.getLight(rawChunk, x + nx + tangents[0] + tangents[3], y + ny + tangents[1] + tangents[4],
				z + nz + tangents[2] + tangents[5], LightFace.Top,
				world.getLightStyle() == LightStyle.Night ? 0 : nightLightAdjustment);
		if (world.getLightStyle() != LightStyle.Night)
			return Util.clamp((a + b + c) / 3.0f, 0, 1);

		float localLight = world.getLight(rawChunk, x, y, z, LightFace.Top, 0);
		if (localLight <= 0.0001f)
			return 1.0f;

		// Use neighboring samples as an attenuation ratio, not as another absolute
		// brightness multiplier. This keeps global night light from being applied
		// twice to AO-enabled models while still darkening locally occluded corners.
		return Util.clamp(((a + b + c) / 3.0f) / localLight, 0, 1);
	}

	private FaceDirection rotatedFaceDirection(Matrix4f rotation, String face) {
		Vector3f direction = rotation.transformDirection(faceNormal(face));
		float absX = Math.abs(direction.x), absY = Math.abs(direction.y), absZ = Math.abs(direction.z);
		if (absY >= absX && absY >= absZ) return direction.y >= 0 ? FaceDirection.UP : FaceDirection.DOWN;
		if (absX >= absZ) return direction.x >= 0 ? FaceDirection.EAST : FaceDirection.WEST;
		return direction.z >= 0 ? FaceDirection.SOUTH : FaceDirection.NORTH;
	}

	private Vector3f faceNormal(String face) {
		return switch (face) {
			case "up" -> new Vector3f(0, 1, 0);
			case "down" -> new Vector3f(0, -1, 0);
			case "north" -> new Vector3f(0, 0, -1);
			case "south" -> new Vector3f(0, 0, 1);
			case "east" -> new Vector3f(1, 0, 0);
			default -> new Vector3f(-1, 0, 0);
		};
	}

	private FaceDirection faceDirection(String face) {
		return switch (face) {
			case "up" -> FaceDirection.UP;
			case "down" -> FaceDirection.DOWN;
			case "north" -> FaceDirection.NORTH;
			case "south" -> FaceDirection.SOUTH;
			case "east" -> FaceDirection.EAST;
			default -> FaceDirection.WEST;
		};
	}

	private Colour4f getTintColor(String modelName, RawChunk rawChunk, BlockContext world, int x, int y, int z, BlockElement element) {
		// The vanilla stonecutter model has tintindex on its saw, but the saw should keep its texture color.
		if (modelName.contains("stonecutter"))
			return new Colour4f(1, 1, 1, 1);

		Colour4f tintColor;
		Colour4f fixedTint = FixedBlockTint.find(modelName);

		if (modelName.contains("cauldron")) {
			tintColor = world.getWaterColor(rawChunk, x, y, z);
		} else if (modelName.contains("redstone")) {
			final float power = (Integer.parseInt(rawChunk.getBlockState(x, y, z).get("power")) / 16.0f);
			tintColor = new Colour4f(Util.clamp(power + 0.25f, 0, 1), 0.2f * power, 0.2f * power, 1);
		} else if (modelName.contains("stem") && !modelName.contains("mushroom")) {
			BlockProperties stemProperties = rawChunk.getBlockState(x, y, z);
			int age;
			if (stemProperties.containsKey("age")) {
				age = Integer.parseInt(rawChunk.getBlockState(x, y, z).get("age"));
			} else {
				age = 7;
			}
			tintColor = new Colour4f(age*32/255f, (255-age*8)/255f, age*4/255f);
		} else if (fixedTint != null) {
			tintColor = fixedTint;
		} else if (modelName.contains("oak_leaves") || modelName.contains("jungle_leaves") || modelName.contains("acacia_leaves") || modelName.contains("vines")) {
			tintColor = world.getFoliageColor(rawChunk.getChunkCoord(), x, y, z);
		} else if (modelName.contains("leaf_litter")) {
			tintColor = world.getDryFoliageColor(rawChunk.getChunkCoord(), x, y, z);
		} else {
			tintColor = world.getGrassColor(rawChunk.getChunkCoord(), x, y, z);
		}

		return tintColor;
	}

	private boolean isDirectionCovered(FaceDirection direction, boolean above, boolean below, boolean north, boolean south,
										boolean east, boolean west) {
		return switch (direction) {
			case UP -> above;
			case DOWN -> below;
			case NORTH -> north;
			case SOUTH -> south;
			case EAST -> east;
			case WEST -> west;
		};
	}

	private float getDirectionLight(FaceDirection direction, float top, float bottom, float north, float south, float east, float west) {
		return switch (direction) {
			case UP -> top;
			case DOWN -> bottom;
			case NORTH -> north;
			case SOUTH -> south;
			case EAST -> east;
			case WEST -> west;
		};
	}

	private enum FaceDirection {
		UP, DOWN, NORTH, SOUTH, EAST, WEST
	}

	private boolean isFaceCovered(BlockContext world, BlockStateWrapper selfBlock, BlockStateWrapper neighborBlock)
	{
		//Ignore snow because it can have adjacent snow blocks with different heights, we could look up the snow layer height
		//but I don't know if it's worth it
		if (selfBlock.getBlockName().equals("minecraft:snow")
				&& neighborBlock.getBlockName().equals("minecraft:snow") || selfBlock.getBlockName().contains("leaves"))
			return false;

		return neighborBlock.isFullOpaqueBlock()
				|| world.shouldCullSameBlockFaces()
					&& selfBlock.getBlockName().equals(neighborBlock.getBlockName());
	}
}
