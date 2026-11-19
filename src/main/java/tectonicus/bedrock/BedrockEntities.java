/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.DoubleTag;
import org.jnbt.FloatTag;
import org.jnbt.IntArrayTag;
import org.jnbt.ListTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import tectonicus.raw.RawChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Converts the Bedrock entities Tectonicus draws, paintings and armor stands, into Java entity NBT. */
final class BedrockEntities {
	/** Painting sizes in blocks, needed to find the block Java anchors a painting to from its centre. */
	private static final Map<String, int[]> PAINTING_SIZES = new HashMap<>();

	static {
		size(1, 1, "kebab", "aztec", "alban", "aztec2", "bomb", "plant", "wasteland", "meditative");
		size(2, 1, "pool", "courbet", "sunset", "sea", "creebet");
		size(1, 2, "wanderer", "graham", "prairie_ride");
		size(2, 2, "match", "bust", "stage", "void", "skull_and_roses", "wither", "baroque", "humble", "earth", "wind",
				"water", "fire");
		size(4, 2, "fighters", "changing", "finding", "lowmist", "passage");
		size(4, 3, "skeleton", "donkey_kong");
		size(4, 4, "pointer", "pigscene", "burning_skull", "orb", "unpacked");
		size(3, 3, "bouquet", "cavebird", "cotan", "endboss", "fern", "owlemons", "sunflowers", "tides", "dennis");
		size(3, 4, "backyard", "pond");
	}

	private static void size(int width, int height, String... names) {
		for (String name : names)
			PAINTING_SIZES.put(name, new int[] {width, height});
	}

	private BedrockEntities() {}

	static void read(RawChunk chunk, List<CompoundTag> actors, BedrockBlockMapper mapper) {
		List<Tag> java = new ArrayList<>();
		for (CompoundTag actor : actors) {
			try {
				CompoundTag converted = switch (BedrockChunkReader.string(actor, "identifier", "")) {
					case "minecraft:painting" -> painting(actor);
					case "minecraft:armor_stand" -> armorStand(actor, mapper);
					default -> null;
				};
				if (converted != null)
					java.add(converted);
			} catch (RuntimeException e) {
				// An entity Tectonicus cannot read is left out
			}
		}
		if (!java.isEmpty())
			chunk.addEntities(new ListTag("Entities", CompoundTag.class, java));
	}

	private static double[] position(CompoundTag actor) {
		if (!(actor.getValue().get("Pos") instanceof ListTag pos) || pos.getValue().size() < 3)
			return null;
		double[] xyz = new double[3];
		for (int i = 0; i < 3; i++)
			xyz[i] = ((Number) pos.getValue().get(i).getValue()).doubleValue();
		return xyz;
	}

	/**
	 * Bedrock saves a painting's centre, Java the block it hangs in front of. Java finds the centre by moving from that
	 * block's centre 0.46875 towards the wall, then half a block left and up for even widths and heights; this undoes it.
	 */
	static CompoundTag painting(CompoundTag actor) {
		double[] pos = position(actor);
		if (pos == null)
			return null;
		String variant = snakeCase(BedrockChunkReader.string(actor, "Motif", "Kebab"));
		int direction = Math.floorMod(BedrockItems.number(actor.getValue().get("Direction"), 0), 4); // 0 south, 1 west, 2 north, 3 east
		int[] facing = switch (direction) {
			case 0 -> new int[] {0, 1};
			case 1 -> new int[] {-1, 0};
			case 2 -> new int[] {0, -1};
			default -> new int[] {1, 0};
		};
		int[] left = {facing[1], -facing[0]}; // Counter-clockwise from the facing, seen from above
		int[] size = PAINTING_SIZES.getOrDefault(variant, new int[] {1, 1});
		double sideways = size[0] % 2 == 0 ? 0.5 : 0;
		double up = size[1] % 2 == 0 ? 0.5 : 0;
		int x = (int) Math.floor(pos[0] + facing[0] * 0.46875 - left[0] * sideways);
		int y = (int) Math.floor(pos[1] - up);
		int z = (int) Math.floor(pos[2] + facing[1] * 0.46875 - left[1] * sideways);

		Map<String, Tag> tag = new HashMap<>();
		tag.put("id", new StringTag("id", "minecraft:painting"));
		tag.put("variant", new StringTag("variant", "minecraft:" + variant));
		tag.put("facing", new ByteTag("facing", (byte) direction));
		tag.put("block_pos", new IntArrayTag("block_pos", new int[] {x, y, z}));
		return new CompoundTag("", tag);
	}

	/** Bedrock names paintings like Kebab and SkullAndRoses; Java like kebab and skull_and_roses. */
	static String snakeCase(String name) {
		return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
	}

	private static CompoundTag armorStand(CompoundTag actor, BedrockBlockMapper mapper) {
		double[] pos = position(actor);
		if (pos == null)
			return null;
		float yaw = 0;
		if (actor.getValue().get("Rotation") instanceof ListTag rotation && !rotation.getValue().isEmpty()
				&& rotation.getValue().getFirst() instanceof FloatTag f)
			yaw = f.getValue();

		Map<String, Tag> equipment = new HashMap<>();
		if (actor.getValue().get("Armor") instanceof ListTag armor) {
			String[] slots = {"head", "chest", "legs", "feet"};
			for (int i = 0; i < Math.min(4, armor.getValue().size()); i++)
				equip(equipment, slots[i], armor.getValue().get(i), mapper);
		}
		if (actor.getValue().get("Mainhand") instanceof ListTag hand && !hand.getValue().isEmpty())
			equip(equipment, "mainhand", hand.getValue().getFirst(), mapper);
		if (actor.getValue().get("Offhand") instanceof ListTag hand && !hand.getValue().isEmpty())
			equip(equipment, "offhand", hand.getValue().getFirst(), mapper);

		Map<String, Tag> tag = new HashMap<>();
		tag.put("id", new StringTag("id", "minecraft:armor_stand"));
		tag.put("Pos", new ListTag("Pos", DoubleTag.class, List.of(new DoubleTag("", pos[0]), new DoubleTag("", pos[1]),
				new DoubleTag("", pos[2]))));
		tag.put("Rotation", new ListTag("Rotation", FloatTag.class, List.of(new FloatTag("", yaw), new FloatTag("", 0))));
		tag.put("ShowArms", new ByteTag("ShowArms", (byte) 1)); // Bedrock armor stands always have arms
		tag.put("equipment", new CompoundTag("equipment", equipment));
		CompoundTag pose = pose(actor);
		if (pose != null)
			tag.put("Pose", pose);
		return new CompoundTag("", tag);
	}

	private static void equip(Map<String, Tag> equipment, String slot, Tag item, BedrockBlockMapper mapper) {
		if (item instanceof CompoundTag compound) {
			CompoundTag java = BedrockItems.toJava(compound, null, mapper);
			if (java != null)
				equipment.put(slot, new CompoundTag(slot, java.getValue()));
		}
	}

	/** Bedrock armor stands pick one of a set of poses; the arms of the few that differ much from the default. */
	private static CompoundTag pose(CompoundTag actor) {
		CompoundTag pose = BedrockChunkReader.compound(actor, "Pose");
		int index = pose == null ? 0 : BedrockItems.number(pose.getValue().get("PoseIndex"), 0);
		float[][] arms = switch (index) {
			case 1 -> new float[][] {{0, 0, 0}, {0, 0, 0}}; // No pose
			case 4 -> new float[][] {{-10, 0, -10}, {-110, 50, 0}}; // Brandish
			case 7 -> new float[][] {{-10, 0, -10}, {-110, 35, 0}}; // Salute
			case 9 -> new float[][] {{-90, 0, 0}, {-90, 0, 0}}; // Zombie
			default -> null;
		};
		if (arms == null)
			return null;
		return new CompoundTag("Pose", Map.of("LeftArm", angles("LeftArm", arms[0]), "RightArm", angles("RightArm", arms[1])));
	}

	private static ListTag angles(String name, float[] angles) {
		return new ListTag(name, FloatTag.class, List.of(new FloatTag("", angles[0]), new FloatTag("", angles[1]),
				new FloatTag("", angles[2])));
	}
}
