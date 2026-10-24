/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.BlockType;
import tectonicus.Version;
import tectonicus.blockregistry.BlockStateWrapper;
import tectonicus.chunk.LightSample;
import tectonicus.configuration.LightFace;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

public class Water extends Fluid {
	private final Version texturePackVersion;
	private final SubTexture overlayTexture;

	public Water(String name, SubTexture stillTexture, int frame) {
		this(name, stillTexture, stillTexture, null, frame);
	}

	public Water(String name, SubTexture stillTexture, SubTexture flowTexture, SubTexture overlayTexture, int frame) {
		super(name, stillTexture, flowTexture, frame);
		this.texturePackVersion = stillTexture.texturePackVersion;
		this.overlayTexture = overlayTexture == null ? null : animationFrame(overlayTexture, frame);
	}

	@Override
	public boolean isWater() { return true; }

	@Override
	protected Geometry.MeshType getMeshType() { return Geometry.MeshType.Transparent; }

	@Override
	protected boolean renderInternalTop(BlockContext world) { return world.isLegacyWater(); }

	@Override
	protected boolean containsFluid(BlockContext world, RawChunk chunk, int x, int y, int z) {
		BlockType type = world.getBlockType(chunk, x, y, z);
		if (type.isWater())
			return true;
		BlockProperties properties = world.getBlockState(chunk.getChunkCoord(), x, y, z);
		BlockStateWrapper block = world.getBlock(chunk, x, y, z);
		return containsWater(block == null ? type.getName() : block.getBlockName(), properties, block);
	}

	/** Shared by chunk dispatch and neighboring-fluid checks, including state defaults. */
	public static boolean containsWater(String name, BlockProperties properties, BlockStateWrapper block) {
		String waterlogged = properties == null ? null : properties.get("waterlogged");
		if (waterlogged == null && block != null)
			waterlogged = block.getDefaultProperties().get("waterlogged");
		return "true".equals(waterlogged) || name.equals("minecraft:kelp") || name.equals("minecraft:kelp_plant")
				|| name.equals("minecraft:seagrass") || name.equals("minecraft:tall_seagrass");
	}

	@Override
	protected float ownHeight(BlockContext world, RawChunk chunk, int x, int y, int z) {
		// Waterlogged blocks and aquatic plants contain source water, independently
		// of their block's own metadata (for example a stair's orientation).
		return world.getBlockType(chunk, x, y, z).isWater() ? super.ownHeight(world, chunk, x, y, z) : 8 / 9.0f;
	}

	@Override
	protected SubTexture sideTexture(BlockContext world, RawChunk chunk, int x, int y, int z) {
		if (overlayTexture == null)
			return flowTexture;
		BlockStateWrapper block = world.getBlock(chunk, x, y, z);
		BlockType type = world.getBlockType(chunk, x, y, z);
		String name = block == null ? type.getName() : block.getBlockName();
		boolean overlay = type instanceof Glass || type instanceof Ice || type instanceof Leaves
				|| name.equals("minecraft:glass") || name.equals("minecraft:tinted_glass") || name.endsWith("_stained_glass")
				|| name.equals("minecraft:ice") || name.equals("minecraft:frosted_ice") || name.endsWith("_leaves");
		return overlay ? overlayTexture : flowTexture;
	}

	@Override
	protected Vector4f colour(BlockContext world, RawChunk chunk, int x, int y, int z, LightFace face, boolean internal) {
		Colour4f tint = texturePackVersion != null && texturePackVersion.getNumVersion() >= Version.VERSION_13.getNumVersion()
				? world.getWaterColor(chunk, x, y, z) : new Colour4f();
		int sampleY = face == LightFace.Bottom ? y - 1 : y;
		float adjustment = world.getNightLightAdjustment();
		LightSample self = world.getLightSample(chunk, x, sampleY, z, LightFace.Top, adjustment);
		LightSample above = world.getLightSample(chunk, x, sampleY + 1, z, LightFace.Top, adjustment);
		// Minecraft fluids combine the strongest sky and block light in the cell
		// and the cell above, then apply the directional face shading.
		Colour4f light = new LightSample(Math.max(self.sky(), above.sky()), Math.max(self.block(), above.block()),
				Math.max(self.ambient(), above.ambient())).toColour(world.getLightStyle(), world.useOldGrayscaleLighting());
		float shade = shade(face);
		return new Vector4f(tint.r * light.r * shade, tint.g * light.g * shade, tint.b * light.b * shade, internal ? 0.3f : 0.95f);
	}
}
