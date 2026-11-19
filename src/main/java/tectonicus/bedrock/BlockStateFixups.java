/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import tectonicus.bedrock.BedrockBlockMapper.JavaState;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fills in the Java block states that Bedrock leaves to the game: fence, pane, bar, chorus and tripwire connections,
 * redstone wire sides, stair shapes, the halves of doors and tall plants, and snowy grass. Each comes from the
 * neighbouring blocks, so blocks at a chunk's edge look into the chunks around it.
 */
final class BlockStateFixups {
	private static final String[] SIDES = {"north", "east", "south", "west"};
	private static final int[][] OFFSETS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}; // By SIDES
	private static final Set<String> TALL_PLANTS = Set.of("minecraft:sunflower", "minecraft:lilac", "minecraft:rose_bush",
			"minecraft:peony", "minecraft:tall_grass", "minecraft:large_fern", "minecraft:pitcher_plant",
			"minecraft:tall_seagrass", "minecraft:small_dripleaf");
	private static final Set<String> SNOWY = Set.of("minecraft:grass_block", "minecraft:podzol", "minecraft:mycelium");
	private static final Set<String> SNOW = Set.of("minecraft:snow", "minecraft:snow_block", "minecraft:powder_snow");
	/** Full blocks that fences, panes and walls still do not attach to. */
	private static final Set<String> NO_ATTACH = Set.of("minecraft:barrier", "minecraft:pumpkin", "minecraft:carved_pumpkin",
			"minecraft:jack_o_lantern", "minecraft:melon");
	private static final Set<String> WIRE_POWER = Set.of("minecraft:redstone_block", "minecraft:redstone_torch",
			"minecraft:redstone_wall_torch", "minecraft:lever", "minecraft:target", "minecraft:daylight_detector",
			"minecraft:trapped_chest", "minecraft:lectern", "minecraft:sculk_sensor", "minecraft:calibrated_sculk_sensor",
			"minecraft:tripwire_hook", "minecraft:comparator", "minecraft:redstone_wire", "minecraft:copper_bulb");
	private static final Map<String, BlockProperties> INTERNED = new ConcurrentHashMap<>();

	private final RawChunk chunk;
	private final Neighbourhood blocks;

	private BlockStateFixups(RawChunk chunk, Neighbourhood blocks) {
		this.chunk = chunk;
		this.blocks = blocks;
	}

	static void apply(RawChunk chunk, Neighbourhood blocks) {
		new BlockStateFixups(chunk, blocks).apply();
	}

	private void apply() {
		JavaState[][] sections = blocks.centre().sections();
		for (int section = 0; section < sections.length; section++) {
			JavaState[] states = sections[section];
			if (states == null)
				continue;
			for (int x = 0; x < 16; x++)
				for (int z = 0; z < 16; z++)
					for (int dy = 0; dy < 16; dy++) {
						JavaState state = states[RawChunk.sectionIndex(x, dy, z)];
						if (!state.isAir())
							fix(state, x, section * 16 + dy, z);
					}
		}
	}

	private void fix(JavaState state, int x, int y, int z) {
		String name = state.name();
		if (isFence(name)) {
			connectSides(state, x, y, z, side -> attachesToFence(name, side));
		} else if (isPane(name)) {
			connectSides(state, x, y, z, this::attachesToPane);
		} else if (name.endsWith("_stairs")) {
			set(x, y, z, state, Map.of("shape", stairShape(state, x, y, z)));
		} else if (name.equals("minecraft:redstone_wire")) {
			wire(state, x, y, z);
		} else if (name.endsWith("_door")) {
			door(state, x, y, z);
		} else if (TALL_PLANTS.contains(name)) {
			tallPlant(state, x, y, z);
		} else if (SNOWY.contains(name)) {
			set(x, y, z, state, Map.of("snowy", Boolean.toString(SNOW.contains(blocks.name(x, y + 1, z)))));
		} else if (name.equals("minecraft:chorus_plant")) {
			chorus(state, x, y, z);
		} else if (name.equals("minecraft:tripwire")) {
			connectSides(state, x, y, z, side -> side.name().equals("minecraft:tripwire") || side.name().equals("minecraft:tripwire_hook"));
		}
	}

	/** A neighbouring block and the side of the block being fixed that it is on. */
	private record Side(JavaState state, String direction) {
		String name() {
			return state == null ? "minecraft:air" : state.name();
		}
	}

	private interface Attaches {
		boolean test(Side side);
	}

	private void connectSides(JavaState state, int x, int y, int z, Attaches attaches) {
		Map<String, String> changes = new TreeMap<>();
		for (int i = 0; i < 4; i++) {
			Side side = new Side(blocks.get(x + OFFSETS[i][0], y, z + OFFSETS[i][1]), SIDES[i]);
			changes.put(SIDES[i], Boolean.toString(attaches.test(side)));
		}
		set(x, y, z, state, changes);
	}

	private static boolean isFence(String name) {
		return name.endsWith("_fence");
	}

	private static boolean isPane(String name) {
		return name.endsWith("glass_pane") || name.equals("minecraft:iron_bars") || name.endsWith("copper_bars");
	}

	private static boolean isWall(String name) {
		return name.endsWith("_wall");
	}

	/** Whether a block is a full, solid cube, the kind fences and panes attach to. */
	private static boolean isSturdy(JavaState state) {
		return state != null && state.opacity() >= 15 && !NO_ATTACH.contains(state.name()) && !state.name().endsWith("shulker_box")
				&& !state.name().endsWith("_leaves");
	}

	private boolean attachesToFence(String fence, Side side) {
		String name = side.name();
		if (isFence(name))
			return name.equals("minecraft:nether_brick_fence") == fence.equals("minecraft:nether_brick_fence");
		if (name.endsWith("_fence_gate"))
			return gateAllows(side);
		return isSturdy(side.state());
	}

	private boolean attachesToPane(Side side) {
		String name = side.name();
		return isPane(name) || isWall(name) || isSturdy(side.state());
	}

	/** A fence gate joins the fence on the sides it opens between. */
	private static boolean gateAllows(Side side) {
		String facing = side.state().get("facing");
		boolean gateAlongX = "east".equals(facing) || "west".equals(facing);
		boolean sideAlongX = side.direction().equals("east") || side.direction().equals("west");
		return gateAlongX != sideAlongX;
	}

	/** Java's stair shape rule: a stair turns a corner to meet another stair of the same half behind or in front. */
	private String stairShape(JavaState state, int x, int y, int z) {
		String facing = state.get("facing");
		String half = state.get("half");
		if (facing == null || half == null)
			return "straight";
		int[] forward = offset(facing);
		JavaState back = blocks.get(x + forward[0], y, z + forward[1]);
		if (isStairs(back) && half.equals(back.get("half"))) {
			String backFacing = back.get("facing");
			if (backFacing != null && axis(backFacing) != axis(facing) && canTakeShape(state, x, y, z, opposite(backFacing)))
				return backFacing.equals(counterClockwise(facing)) ? "outer_left" : "outer_right";
		}
		JavaState front = blocks.get(x - forward[0], y, z - forward[1]);
		if (isStairs(front) && half.equals(front.get("half"))) {
			String frontFacing = front.get("facing");
			if (frontFacing != null && axis(frontFacing) != axis(facing) && canTakeShape(state, x, y, z, frontFacing))
				return frontFacing.equals(counterClockwise(facing)) ? "inner_left" : "inner_right";
		}
		return "straight";
	}

	private boolean canTakeShape(JavaState state, int x, int y, int z, String direction) {
		int[] offset = offset(direction);
		JavaState neighbour = blocks.get(x + offset[0], y, z + offset[1]);
		return !isStairs(neighbour) || !state.get("facing").equals(neighbour.get("facing"))
				|| !state.get("half").equals(neighbour.get("half"));
	}

	private static boolean isStairs(JavaState state) {
		return state != null && state.name().endsWith("_stairs");
	}

	private void wire(JavaState state, int x, int y, int z) {
		boolean solidAbove = isSturdy(blocks.get(x, y + 1, z));
		String[] values = new String[4];
		int connections = 0;
		for (int i = 0; i < 4; i++) {
			int nx = x + OFFSETS[i][0];
			int nz = z + OFFSETS[i][1];
			JavaState neighbour = blocks.get(nx, y, nz);
			String value = "none";
			if (wireAttaches(neighbour, SIDES[i])) {
				value = "side";
			} else if (!solidAbove && isSturdy(neighbour) && isWire(blocks.get(nx, y + 1, nz))) {
				value = "up";
			} else if (!isSturdy(neighbour) && isWire(blocks.get(nx, y - 1, nz))) {
				value = "side";
			}
			values[i] = value;
			if (!value.equals("none"))
				connections++;
		}
		if (connections == 0) {
			// Unconnected wire shows as a cross, as Java places it
			java.util.Arrays.fill(values, "side");
		} else if (connections == 1) {
			// A line of wire runs straight through its one connection
			for (int i = 0; i < 4; i++)
				if (!values[i].equals("none") && values[(i + 2) % 4].equals("none"))
					values[(i + 2) % 4] = "side";
		}
		Map<String, String> changes = new TreeMap<>();
		for (int i = 0; i < 4; i++)
			changes.put(SIDES[i], values[i]);
		set(x, y, z, state, changes);
	}

	private static boolean isWire(JavaState state) {
		return state != null && state.name().equals("minecraft:redstone_wire");
	}

	private static boolean wireAttaches(JavaState state, String side) {
		if (state == null)
			return false;
		String name = state.name();
		if (WIRE_POWER.contains(name) || name.endsWith("_button") || name.endsWith("_pressure_plate"))
			return true;
		if (name.equals("minecraft:repeater"))
			return axis(state.get("facing")) == axis(side);
		if (name.equals("minecraft:observer"))
			return side.equals(state.get("facing"));
		return false;
	}

	/** Java keeps the facing and open state on both halves of a door and the hinge on both; Bedrock may not. */
	private void door(JavaState state, int x, int y, int z) {
		boolean upper = "upper".equals(state.get("half"));
		JavaState other = blocks.get(x, upper ? y - 1 : y + 1, z);
		if (other == null || !other.name().equals(state.name()))
			return;
		Map<String, String> changes = new TreeMap<>();
		JavaState lower = upper ? other : state;
		JavaState top = upper ? state : other;
		copy(lower, "facing", changes);
		copy(lower, "open", changes);
		copy(top, "hinge", changes);
		set(x, y, z, state, changes);
	}

	private static void copy(JavaState from, String property, Map<String, String> into) {
		String value = from.get(property);
		if (value != null)
			into.put(property, value);
	}

	/** The upper half of a tall plant is the same plant as its lower half. */
	private void tallPlant(JavaState state, int x, int y, int z) {
		if (!"upper".equals(state.get("half")))
			return;
		JavaState below = blocks.get(x, y - 1, z);
		if (below != null && TALL_PLANTS.contains(below.name()) && !below.name().equals(state.name()) && "lower".equals(below.get("half")))
			chunk.setBlockName(x, y, z, below.name());
	}

	private void chorus(JavaState state, int x, int y, int z) {
		Map<String, String> changes = new TreeMap<>();
		for (int i = 0; i < 4; i++)
			changes.put(SIDES[i], Boolean.toString(isChorus(blocks.get(x + OFFSETS[i][0], y, z + OFFSETS[i][1]))));
		changes.put("up", Boolean.toString(isChorus(blocks.get(x, y + 1, z))));
		JavaState below = blocks.get(x, y - 1, z);
		changes.put("down", Boolean.toString(isChorus(below) || below != null && below.name().equals("minecraft:end_stone")));
		set(x, y, z, state, changes);
	}

	private static boolean isChorus(JavaState state) {
		return state != null && (state.name().equals("minecraft:chorus_plant") || state.name().equals("minecraft:chorus_flower"));
	}

	/** Writes changed properties, sharing equal property sets between blocks. */
	private void set(int x, int y, int z, JavaState state, Map<String, String> changes) {
		boolean changed = false;
		for (Map.Entry<String, String> change : changes.entrySet()) {
			if (!change.getValue().equals(state.get(change.getKey()))) {
				changed = true;
				break;
			}
		}
		if (!changed)
			return;
		Map<String, String> properties = new TreeMap<>(state.properties().getProperties());
		properties.putAll(changes);
		BlockProperties merged = INTERNED.computeIfAbsent(properties.toString(), k -> new BlockProperties(properties));
		chunk.setBlockState(x, y, z, merged);
	}

	private static int[] offset(String direction) {
		return switch (direction) {
			case "north" -> OFFSETS[0];
			case "east" -> OFFSETS[1];
			case "south" -> OFFSETS[2];
			default -> OFFSETS[3];
		};
	}

	private static int axis(String direction) {
		return "east".equals(direction) || "west".equals(direction) ? 0 : 1;
	}

	private static String opposite(String direction) {
		return switch (direction) {
			case "north" -> "south";
			case "east" -> "west";
			case "south" -> "north";
			default -> "east";
		};
	}

	private static String counterClockwise(String direction) {
		return switch (direction) {
			case "north" -> "west";
			case "west" -> "south";
			case "south" -> "east";
			default -> "north";
		};
	}
}
