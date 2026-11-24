/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockregistry;

import lombok.Getter;
import lombok.Setter;
import tectonicus.BlockOcclusion;
import tectonicus.raw.BlockProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Getter
public class BlockStateWrapper {
	private final String blockName;
	private final PlantModelOffset modelOffset;
	private final boolean slab;
	private final boolean stairs;
	private final boolean nonOccludingPartialBlock;
	private final boolean leaves;
	private final List<BlockState> states = new ArrayList<>();
	private BlockProperties defaultProperties;
	@Setter
	private EmissiveBlock emissiveBlock;
	@Setter
	private boolean fullBlock = true;
	@Setter
	private boolean isTransparent = false;
	@Setter
	private boolean fullOpaqueBlock = true;

	public BlockStateWrapper(String blockName) {
		this.blockName = blockName;
		this.modelOffset = PlantModelOffset.forBlock(blockName);
		this.slab = isSlabName(blockName);
		this.stairs = isStairsName(blockName);
		// Vanilla trapdoors and hoppers have model geometry but an empty occlusion shape.
		this.nonOccludingPartialBlock = blockName.endsWith("_trapdoor") || blockName.equals("minecraft:hopper");
		this.leaves = blockName.contains("leaves");
	}

	public static boolean isSlabName(String blockName) {
		return blockName.endsWith("_slab");
	}

	public static boolean isStairsName(String blockName) {
		return blockName.endsWith("_stairs");
	}

	public boolean hasStateDependentFaceCoverage() {
		return slab || stairs;
	}
	
	public float getShadeBrightness() {
		// The beacon is a full cube despite its inset elements.
		if (blockName.equals("minecraft:beacon"))
			return 0.2f;
		// Glass does not shade neighbors, unlike ice.
		if (blockName.equals("minecraft:glass") || blockName.equals("minecraft:tinted_glass")
				|| blockName.startsWith("minecraft:") && blockName.endsWith("_stained_glass"))
			return 1.0f;
		return fullBlock || fullOpaqueBlock ? 0.2f : 1.0f;
	}

	public BlockOcclusion getOcclusion() {
		if (getShadeBrightness() == 1.0f)
			return BlockOcclusion.OPEN;
		return fullOpaqueBlock ? BlockOcclusion.OPAQUE : BlockOcclusion.TRANSLUCENT;
	}

	public void addState(BlockState state) {
		states.add(state);
	}

	public void setDefaultProperties(Map<String, String> defaultProperties) {
		Map<String, String> defaults = Map.copyOf(defaultProperties);
		this.defaultProperties = defaults.isEmpty() ? null : BlockProperties.immutableCopyOf(defaults);
	}

	public Map<String, String> getDefaultProperties() {
		BlockProperties defaults = defaultProperties;
		return defaults == null ? Map.of() : defaults.getProperties();
	}

	public BlockProperties getPropertiesWithDefaults(BlockProperties properties) {
		BlockProperties defaults = defaultProperties;
		if (defaults != null && (properties == null || properties.getProperties().isEmpty()))
			return defaults;

		return properties;
	}

	public int getLightEmission(BlockProperties properties) {
		if (emissiveBlock == null)
			return 0;
		Map<String, String> effectiveProperties = new HashMap<>(getDefaultProperties());
		if (properties != null)
			effectiveProperties.putAll(properties.getProperties());
		return emissiveBlock.getLightEmission(effectiveProperties);
	}

	/** Vanilla's full-bright flag, independent of emitted block light. */
	public boolean isEmissiveRendering(BlockProperties properties) {
		if (blockName.equals("minecraft:magma_block"))
			return true;
		if (!blockName.equals("minecraft:sculk_sensor") && !blockName.equals("minecraft:calibrated_sculk_sensor"))
			return false;
		String defaultPhase = getDefaultProperties().get("sculk_sensor_phase");
		String phase = properties != null ? properties.getProperties().getOrDefault("sculk_sensor_phase", defaultPhase) : defaultPhase;
		return "active".equals(phase);
	}

	public List<BlockStateModel> getModels(BlockProperties properties, int x, int y, int z) {
		List<BlockStateModel> models = new ArrayList<>();
		Random random = BlockState.createPositionRandom(blockName, properties, x, y, z);
		boolean hasMultipart = states.stream().anyMatch(BlockStateCase.class::isInstance);
		long multipartSeed = hasMultipart ? random.nextLong() : 0;
		Random variantRandom = hasMultipart
				? BlockState.createPositionRandom(blockName, properties, x, y, z)
				: random;
		for (BlockState state : states) {
			Random stateRandom = state instanceof BlockStateCase ? new Random(multipartSeed) : variantRandom;
			state.addModels(models, properties, stateRandom);
		}
		return models;
	}

	public List<BlockStateModel> getAllModels() {
		List<BlockStateModel> models = new ArrayList<>();
		for (BlockState state : states) {
			models.addAll(state.getModelsAndWeight().getModels());
		}
		return models;
	}
}
