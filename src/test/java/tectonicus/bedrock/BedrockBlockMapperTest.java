/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import org.tectonicus.jnbt.ByteTag;
import org.tectonicus.jnbt.CompoundTag;
import org.tectonicus.jnbt.IntTag;
import org.tectonicus.jnbt.ShortTag;
import org.tectonicus.jnbt.StringTag;
import org.tectonicus.jnbt.Tag;
import org.junit.jupiter.api.Test;
import tectonicus.bedrock.BedrockBlockMapper.JavaState;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockBlockMapperTest {
	private final BedrockBlockMapper mapper = BedrockBlockMapper.get();

	private static CompoundTag entry(String name, Tag... states) {
		Map<String, Tag> stateMap = new HashMap<>();
		for (Tag state : states)
			stateMap.put(state.getName(), state);
		return new CompoundTag("", Map.of("name", new StringTag("name", name), "states", new CompoundTag("states", stateMap),
				"version", new IntTag("version", 18168865)));
	}

	@Test
	void keysSortStatesAndPrintBytesAsNumbers() {
		assertEquals("minecraft:oak_log[pillar_axis=y]", BedrockBlockMapper.key(entry("minecraft:oak_log", new StringTag("pillar_axis", "y"))));
		assertEquals("minecraft:x[a=1,b=2]", BedrockBlockMapper.key(entry("x", new IntTag("b", 2), new ByteTag("a", (byte) 1))));
	}

	@Test
	void mapsCurrentNames() {
		JavaState log = mapper.map(entry("minecraft:oak_log", new StringTag("pillar_axis", "x")));
		assertEquals("minecraft:oak_log", log.name());
		assertEquals("x", log.properties().get("axis"));

		JavaState door = mapper.map("minecraft:wooden_door[direction=0,door_hinge_bit=0,open_bit=0,upper_block_bit=1]");
		assertEquals("minecraft:oak_door", door.name());
		assertEquals("upper", door.properties().get("half"));
	}

	private JavaState legacy(String name, int val) {
		return mapper.map(new CompoundTag("", Map.of("name", new StringTag("name", name), "val", new ShortTag("val", (short) val))));
	}

	@Test
	void mapsDataValuesFromBefore113() {
		assertEquals("minecraft:oak_fence", legacy("minecraft:fence", 0).name());
		assertEquals("minecraft:dark_oak_fence", legacy("minecraft:fence", 5).name());
		assertEquals("minecraft:cobblestone_wall", legacy("minecraft:cobblestone_wall", 0).name());
		assertEquals("minecraft:mossy_cobblestone_wall", legacy("minecraft:cobblestone_wall", 1).name());
		assertEquals("minecraft:red_wool", legacy("minecraft:wool", 14).name());
		JavaState log = legacy("minecraft:log", 6);
		assertEquals("minecraft:birch_log", log.name());
		assertEquals("x", log.get("axis"));
		assertEquals("top", legacy("minecraft:oak_stairs", 4).get("half"));
		assertEquals("0", legacy("minecraft:water", 0).get("level"));
		assertEquals("minecraft:stone", legacy("minecraft:stone", 0).name());
		assertEquals("minecraft:oak_fence", legacy("minecraft:fence", 9).name()); // Unused data value
	}

	@Test
	void mapsNamesFromOlderBedrockVersions() {
		assertEquals("minecraft:granite", mapper.map("minecraft:stone[stone_type=granite]").name());
		assertEquals("minecraft:red_wool", mapper.map("minecraft:wool[color=red]").name());
		assertEquals("minecraft:grass_block", mapper.map("minecraft:grass[]").name());
	}

	@Test
	void fallsBackToTheClosestStateOfTheSameBlock() {
		// An extra state Bedrock may add in a later version should not lose the block.
		JavaState log = mapper.map("minecraft:oak_log[pillar_axis=z,some_future_state=1]");
		assertEquals("minecraft:oak_log", log.name());
		assertEquals("z", log.properties().get("axis"));
	}

	@Test
	void keepsUnknownBlocksUnderTheirOwnName() {
		JavaState unknown = mapper.map("example:custom_block[]");
		assertEquals("example:custom_block", unknown.name());
		assertTrue(unknown.properties().getProperties().isEmpty());
	}

	@Test
	void reportsWaterloggingAndSingleChests() {
		assertTrue(mapper.map("minecraft:oak_fence[]").waterloggable());
		assertFalse(mapper.map("minecraft:stone[]").waterloggable());
		assertEquals("single", mapper.map("minecraft:chest[minecraft:cardinal_direction=north]").properties().get("type"));
		assertEquals("true", mapper.map("minecraft:oak_fence[]").withProperty("waterlogged", "true").properties().get("waterlogged"));
	}

	@Test
	void mapsBiomeIds() {
		assertEquals("minecraft:plains", BedrockBiomes.javaId(1));
		assertEquals("minecraft:swamp", BedrockBiomes.javaId(6));
		assertEquals("minecraft:nether_wastes", BedrockBiomes.javaId(8));
		assertEquals("minecraft:eroded_badlands", BedrockBiomes.javaId(165));
		assertEquals("minecraft:cherry_grove", BedrockBiomes.javaId(192));
		assertEquals("minecraft:plains", BedrockBiomes.javaId(9999));
	}

	@Test
	void mapsBedrockVersionsToJavaVersions() {
		assertEquals("1.21", level(1, 16, 210).javaVersion());
		assertEquals("1.21", level(1, 20, 80).javaVersion());
		assertEquals("1.21", level(1, 21, 50).javaVersion());
		assertEquals("1.21.10", level(1, 21, 120).javaVersion());
		assertEquals("26.1", level(1, 26, 0).javaVersion());
		assertEquals("26.2", level(1, 26, 20).javaVersion());
		assertFalse(level(1, 17, 40).hasCavesAndCliffsHeight());
		assertTrue(level(1, 18, 0).hasCavesAndCliffsHeight());
	}

	private static BedrockLevel level(int... version) {
		return new BedrockLevel("", 0, 64, 0, null, version);
	}
}
