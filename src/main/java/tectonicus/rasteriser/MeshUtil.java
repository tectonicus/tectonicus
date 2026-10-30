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
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.blockregistry.BakedBlockStateModel;
import tectonicus.blockregistry.BakedBlockStateModel.BakedQuad;
import tectonicus.blockregistry.BlockModel;
import tectonicus.blockregistry.BlockModel.BlockElement.ElementFace;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.itemregistry.GuiGeometry;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.renderer.Geometry.MeshType;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;
import tectonicus.util.Util;

@UtilityClass
public class MeshUtil {
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

		if (geometry instanceof GuiGeometry) {
			// GUI meshes light the posed surface normals. World AO and cardinal shading
			// must not be baked into the input colours as well.
			for (BakedQuad quad : bakedModel.getQuads()) {
				ElementFace face = quad.getFace();
				Colour4f tint = face.isTinted() ? getTintColor(model.getName(), rawChunk, world, x, y, z)
						: new Colour4f(1, 1, 1, 1);
				MeshType type = face.isTranslucent() ? MeshType.Transparent
						: face.isTransparent() ? MeshType.AlphaTest : MeshType.Solid;
				Mesh mesh = geometry.getMesh(face.getTexture().texture, type);
				for (int vertex = 0; vertex < 4; vertex++)
					mesh.addVertex(new Vector3f(x + quad.getPosition(vertex, 0), y + quad.getPosition(vertex, 1), z + quad.getPosition(vertex, 2)),
							new Vector4f(tint.r, tint.g, tint.b, 1), quad.getUv(vertex, 0), quad.getUv(vertex, 1));
			}
			return;
		}

		// Empty item-model chunks have neither a name nor an ID and need no legacy lookup.
		boolean legacyBlock = rawChunk.getBlockName(x, y, z) == null && rawChunk.getBlockId(x, y, z) != 0;
		BlockStateWrapper selfBlock = legacyBlock ? null : world.getBlock(rawChunk, x, y, z);
		
		boolean blockEmitsLight = selfBlock != null
				&& selfBlock.getLightEmission(rawChunk.getBlockState(x, y, z)) > 0;
		boolean above = legacyBlock
				? world.getBlockType(rawChunk, x, y + 1, z).isSolid()
				: isFaceCovered(world, selfBlock, world.getBlock(rawChunk, x, y + 1, z));
		boolean below = legacyBlock
				? world.getBlockType(rawChunk, x, y - 1, z).isSolid()
				: isFaceCovered(world, selfBlock, world.getBlock(rawChunk, x, y - 1, z));
		boolean north = legacyBlock
				? world.getBlockType(rawChunk, x, y, z - 1).isSolid()
				: isFaceCovered(world, selfBlock, world.getBlock(rawChunk, x, y, z - 1));
		boolean south = legacyBlock
				? world.getBlockType(rawChunk, x, y, z + 1).isSolid()
				: isFaceCovered(world, selfBlock, world.getBlock(rawChunk, x, y, z + 1));
		boolean east = legacyBlock
				? world.getBlockType(rawChunk, x + 1, y, z).isSolid()
				: isFaceCovered(world, selfBlock, world.getBlock(rawChunk, x + 1, y, z));
		boolean west = legacyBlock
				? world.getBlockType(rawChunk, x - 1, y, z).isSolid()
				: isFaceCovered(world, selfBlock, world.getBlock(rawChunk, x - 1, y, z));

		if (above && north && south && east && west)
			return;

		boolean selfFull = model.isFullBlock() || selfBlock != null && selfBlock.isFullBlock();
		LightStyle lightStyle = world.getLightStyle();
		boolean smoothLit = world.isSmoothLit();
		float nightLightAdjustment = world.getNightLightAdjustment();
		boolean grayscale = smoothLit && world.useOldGrayscaleLighting();
		boolean useAo = !blockEmitsLight && smoothLit && model.isAmbientlyOccluded();
		for (BakedQuad quad : bakedModel.getQuads()) {
			ElementFace face = quad.getFace();
			FaceDirection direction = faceDirection(quad.getDirection());
			FaceDirection lightDirection = quad.getShadeDirectionOverride() != null
					? faceDirection(quad.getShadeDirectionOverride())
					: direction;
			FaceDirection cullDirection = quad.getCullDirection() == null ? null : faceDirection(quad.getCullDirection());
			if (cullDirection != null && isDirectionCovered(cullDirection, above, below, north, south, east, west))
				continue;

			Colour4f flatLight = null;
			if (!smoothLit) {
				float light = getFlatLight(world, rawChunk, x, y, z, quad, selfFull, nightLightAdjustment);
				flatLight = new Colour4f(light, light, light, 1);
			}
			Colour4f tint = face.isTinted()
					? getTintColor(model.getName(), rawChunk, world, x, y, z)
					: new Colour4f(1, 1, 1, 1);

			MeshType meshType = face.isTranslucent() ? MeshType.Transparent
					: face.isTransparent() ? MeshType.AlphaTest : MeshType.Solid;
			Mesh mesh = geometry.getMesh(face.getTexture().texture, meshType);
			LightFace smoothLightFace = quad.isShaded() ? lightDirection.lightFace : LightFace.Top;
			boolean faceAo = useAo && direction != FaceDirection.DOWN;
			for (int vertex = 0; vertex < 4; vertex++) {
				Colour4f vertexLight = flatLight;
				if (smoothLit) {
					// Smooth lighting interpolates the light field around each quad corner.
					// This is separate from model AO: models with ambientocclusion=false
					// still receive smooth sky/block lighting, just without corner occlusion.
					LightSample lightSample = getSmoothVertexLight(world, rawChunk, x, y, z, quad, vertex,
							LightFace.Top, selfFull, nightLightAdjustment);
					vertexLight = getShadedLightColour(lightSample, lightStyle, smoothLightFace, grayscale);
				}
				float ao = faceAo
						? getBakedAo(world, rawChunk, x, y, z, quad, vertex, selfFull) : 1.0f;
				Vector4f vertexColour = new Vector4f(vertexLight.r * tint.r * ao, vertexLight.g * tint.g * ao,
						vertexLight.b * tint.b * ao, 1);
				mesh.addVertex(new Vector3f(x + quad.getPosition(vertex, 0), y + quad.getPosition(vertex, 1), z + quad.getPosition(vertex, 2)),
						vertexColour, quad.getUv(vertex, 0), quad.getUv(vertex, 1));
			}
		}
	}

	static float getFlatLight(BlockContext world, RawChunk rawChunk, int x, int y, int z,
			BakedQuad quad, boolean fullBlock, float nightLightAdjustment) {
		FaceDirection surface = faceDirection(quad.getDirection());
		FaceDirection lightDirection = quad.getShadeDirectionOverride() == null
				? surface : faceDirection(quad.getShadeDirectionOverride());
		LightFace lightFace = quad.isShaded() ? lightDirection.lightFace : LightFace.Top;
		// Partial shaded models use their cell's light with the normal face factor.
		if (!fullBlock && quad.isShaded())
			return Chunk.getLight(world.getLightStyle(), lightFace, rawChunk, x, y, z, nightLightAdjustment);

		// shade=false keeps world light but uses no directional darkening.
		boolean outside = fullBlock || isFaceOnBlockBoundary(quad, surface);
		FaceDirection sampleDirection = quad.isShaded() ? lightDirection : surface;
		return world.getLight(rawChunk, x + (outside ? sampleDirection.dx : 0),
				y + (outside ? sampleDirection.dy : 0), z + (outside ? sampleDirection.dz : 0), lightFace, nightLightAdjustment);
	}

	static Colour4f getShadedLightColour(LightSample sample, LightStyle style, LightFace face, boolean grayscale) {
		Colour4f colour = sample.toColour(style, grayscale);
		// Cardinal shading follows light conversion, so saturation cannot erase it.
		float shade = Chunk.getFaceFactor(style, face);
		colour.r *= shade;
		colour.g *= shade;
		colour.b *= shade;
		return colour;
	}

	static LightSample getSmoothVertexLight(BlockContext world, RawChunk rawChunk, int x, int y, int z,
			BakedQuad quad, int vertex, LightFace lightFace, boolean fullBlock, float nightLightAdjustment) {
		LightStyle lightStyle = world.getLightStyle();
		FaceDirection surface = faceDirection(quad.getDirection());
		boolean outside = fullBlock || isFaceOnBlockBoundary(quad, surface);
		int baseX = x + (outside ? surface.dx : 0), baseY = y + (outside ? surface.dy : 0), baseZ = z + (outside ? surface.dz : 0);
		// Minecraft uses the outward center light for inset faces too when that
		// neighbor is not solid-rendering. Light blocking and render opacity differ:
		// tinted glass uses its saved light value and still participates in blending.
		boolean outwardCenter = outside || !world.getBlock(rawChunk, x + surface.dx, y + surface.dy, z + surface.dz).isFullOpaqueBlock();
		LightSample center = world.getLightSample(rawChunk, x + (outwardCenter ? surface.dx : 0),
				y + (outwardCenter ? surface.dy : 0), z + (outwardCenter ? surface.dz : 0), lightFace, nightLightAdjustment);
		float weightA = getVertexWeight(quad, vertex, surface.tangentA, fullBlock);
		float weightB = getVertexWeight(quad, vertex, surface.tangentB, fullBlock);
		LightSample total = LightSample.ambient(0);
		for (int a = 0; a <= 1; a++) {
			for (int b = 0; b <= 1; b++) {
				float weight = (a == 1 ? weightA : 1 - weightA) * (b == 1 ? weightB : 1 - weightB);
				if (weight == 0)
					continue;
				int offsetA = a == 1 ? 1 : -1, offsetB = b == 1 ? 1 : -1;
				int sideAX = baseX + surface.offsetX(offsetA, 0), sideAY = baseY + surface.offsetY(offsetA, 0), sideAZ = baseZ + surface.offsetZ(offsetA, 0);
				int sideBX = baseX + surface.offsetX(0, offsetB), sideBY = baseY + surface.offsetY(0, offsetB), sideBZ = baseZ + surface.offsetZ(0, offsetB);
				LightSample sideA = world.getLightSample(rawChunk, sideAX, sideAY, sideAZ, lightFace, nightLightAdjustment);
				LightSample sideB = world.getLightSample(rawChunk, sideBX, sideBY, sideBZ, lightFace, nightLightAdjustment);
				LightSample corner = world.getLightSample(rawChunk, baseX + surface.offsetX(offsetA, offsetB),
						baseY + surface.offsetY(offsetA, offsetB), baseZ + surface.offsetZ(offsetA, offsetB), lightFace, nightLightAdjustment);
				LightSample blended = center.add(Chunk.applySmoothLightFallback(sideA, center, lightFace, lightStyle))
						.add(Chunk.applySmoothLightFallback(sideB, center, lightFace, lightStyle))
						.add(Chunk.applySmoothLightFallback(corner, center, lightFace, lightStyle));
				total = total.add(blended.scale(weight * 0.25f));
			}
		}
		return total;
	}

	private float getVertexWeight(BakedQuad quad, int vertex, int axis, boolean fullBlock) {
		float weight = Util.clamp(quad.getPosition(vertex, axis), 0, 1);
		return fullBlock ? (weight >= 0.5f ? 1 : 0) : weight;
	}

	static float getBakedAo(BlockContext world, RawChunk rawChunk, int x, int y, int z,
			BakedQuad quad, int vertex, boolean fullBlock) {
		FaceDirection face = faceDirection(quad.getDirection());
		// Keep AO and light sampling on the same layer for partial faces.
		boolean outside = fullBlock || isFaceOnBlockBoundary(quad, face);
		float weightA = getVertexWeight(quad, vertex, face.tangentA, fullBlock);
		float weightB = getVertexWeight(quad, vertex, face.tangentB, fullBlock);
		int baseX = x + (outside ? face.dx : 0), baseY = y + (outside ? face.dy : 0), baseZ = z + (outside ? face.dz : 0);
		float centerShade = world.getBlock(rawChunk, baseX, baseY, baseZ).getShadeBrightness();
		float shade = 0;
		// Partial vertices interpolate the four block-face corners instead of
		// snapping every small rectangle to a whole-block corner.
		for (int a = 0; a <= 1; a++) {
			for (int b = 0; b <= 1; b++) {
				float weight = (a == 1 ? weightA : 1 - weightA) * (b == 1 ? weightB : 1 - weightB);
				if (weight == 0)
					continue;
				int offsetA = a == 1 ? 1 : -1, offsetB = b == 1 ? 1 : -1;
				int sideAX = baseX + face.offsetX(offsetA, 0), sideAY = baseY + face.offsetY(offsetA, 0), sideAZ = baseZ + face.offsetZ(offsetA, 0);
				int sideBX = baseX + face.offsetX(0, offsetB), sideBY = baseY + face.offsetY(0, offsetB), sideBZ = baseZ + face.offsetZ(0, offsetB);
				int cornerX = baseX + face.offsetX(offsetA, offsetB), cornerY = baseY + face.offsetY(offsetA, offsetB), cornerZ = baseZ + face.offsetZ(offsetA, offsetB);
				shade += weight * (centerShade
						+ world.getBlock(rawChunk, sideAX, sideAY, sideAZ).getShadeBrightness()
						+ world.getBlock(rawChunk, sideBX, sideBY, sideBZ).getShadeBrightness()
						+ world.getBlock(rawChunk, cornerX, cornerY, cornerZ).getShadeBrightness()) * 0.25f;
			}
		}
		return Util.clamp(shade, 0.2f, 1.0f);
	}

	private boolean isFaceOnBlockBoundary(BakedQuad quad, FaceDirection face) {
		float plane = quad.getPosition(0, face.axis);
		boolean boundary = face.dx + face.dy + face.dz > 0 ? plane > 0.9999f : plane < 0.0001f;
		for (int i = 1; i < 4; i++)
			boundary &= Math.abs(quad.getPosition(i, face.axis) - plane) < 0.0001f;
		return boundary;
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

	private Colour4f getTintColor(String modelName, RawChunk rawChunk, BlockContext world, int x, int y, int z) {
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

	private enum FaceDirection {
		UP(0, 1, 0, 0, 2, LightFace.Top),
		DOWN(0, -1, 0, 0, 2, LightFace.Bottom),
		NORTH(0, 0, -1, 0, 1, LightFace.NorthSouth),
		SOUTH(0, 0, 1, 0, 1, LightFace.NorthSouth),
		EAST(1, 0, 0, 1, 2, LightFace.EastWest),
		WEST(-1, 0, 0, 1, 2, LightFace.EastWest);

		private final int dx, dy, dz;
		private final int axis, tangentA, tangentB;
		private final LightFace lightFace;

		FaceDirection(int dx, int dy, int dz, int tangentA, int tangentB, LightFace lightFace) {
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			this.axis = dx != 0 ? 0 : dy != 0 ? 1 : 2;
			this.tangentA = tangentA;
			this.tangentB = tangentB;
			this.lightFace = lightFace;
		}

		int offsetX(int a, int b) { return (tangentA == 0 ? a : 0) + (tangentB == 0 ? b : 0); }
		int offsetY(int a, int b) { return (tangentA == 1 ? a : 0) + (tangentB == 1 ? b : 0); }
		int offsetZ(int a, int b) { return (tangentA == 2 ? a : 0) + (tangentB == 2 ? b : 0); }
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
