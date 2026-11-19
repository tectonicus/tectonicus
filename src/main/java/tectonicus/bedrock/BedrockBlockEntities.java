/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import lombok.extern.slf4j.Slf4j;
import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.FloatTag;
import org.jnbt.IntArrayTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import tectonicus.bedrock.BedrockBlockMapper.JavaState;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.ContainerKind;
import tectonicus.raw.RawChunk;
import tectonicus.world.Colors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Converts Bedrock block entities into Java ones. Some carry what Java keeps in the block state, so they also change
 * the block: bed and banner colours, the kind and rotation of heads, a flower pot's plant and a shulker box's facing.
 * Item frames are blocks with a block entity in Bedrock and entities in Java, so they become air and an entity.
 */
@Slf4j
final class BedrockBlockEntities {
	private static final String[] HEADS = {"skeleton_skull", "wither_skeleton_skull", "zombie_head", "player_head",
			"creeper_head", "dragon_head", "piglin_head"};
	private static final Set<String> BEACON_BASES = Set.of("minecraft:iron_block", "minecraft:gold_block",
			"minecraft:diamond_block", "minecraft:emerald_block", "minecraft:netherite_block");
	/** Bedrock's banner pattern codes, which Java used before 1.20.5, and Java's pattern ids. */
	private static final Map<String, String> PATTERNS = Map.ofEntries(Map.entry("bo", "border"), Map.entry("bri", "bricks"),
			Map.entry("mc", "circle"), Map.entry("cre", "creeper"), Map.entry("cr", "cross"), Map.entry("cbo", "curly_border"),
			Map.entry("ld", "diagonal_left"), Map.entry("rd", "diagonal_right"), Map.entry("lud", "diagonal_up_left"),
			Map.entry("rud", "diagonal_up_right"), Map.entry("flo", "flower"), Map.entry("gra", "gradient"),
			Map.entry("gru", "gradient_up"), Map.entry("hh", "half_horizontal"), Map.entry("hhb", "half_horizontal_bottom"),
			Map.entry("vh", "half_vertical"), Map.entry("vhr", "half_vertical_right"), Map.entry("moj", "mojang"),
			Map.entry("mr", "rhombus"), Map.entry("sku", "skull"), Map.entry("ss", "small_stripes"),
			Map.entry("bl", "square_bottom_left"), Map.entry("br", "square_bottom_right"), Map.entry("tl", "square_top_left"),
			Map.entry("tr", "square_top_right"), Map.entry("sc", "straight_cross"), Map.entry("bs", "stripe_bottom"),
			Map.entry("cs", "stripe_center"), Map.entry("dls", "stripe_downleft"), Map.entry("drs", "stripe_downright"),
			Map.entry("ls", "stripe_left"), Map.entry("ms", "stripe_middle"), Map.entry("rs", "stripe_right"),
			Map.entry("ts", "stripe_top"), Map.entry("bt", "triangle_bottom"), Map.entry("tt", "triangle_top"),
			Map.entry("bts", "triangles_bottom"), Map.entry("tts", "triangles_top"), Map.entry("glb", "globe"),
			Map.entry("pig", "piglin"), Map.entry("flw", "flow"), Map.entry("gus", "guster"));
	/** The ominous banner, which Bedrock saves as a type rather than as patterns. */
	private static final String[][] OMINOUS = {{"rhombus", "cyan"}, {"stripe_bottom", "light_gray"},
			{"stripe_center", "gray"}, {"border", "light_gray"}, {"stripe_middle", "black"}, {"half_horizontal", "light_gray"},
			{"circle", "light_gray"}, {"border", "black"}};

	private final RawChunk chunk;
	private final Neighbourhood blocks;
	private final BedrockBlockMapper mapper;
	private final int minY;
	private final List<Tag> blockEntities = new ArrayList<>();
	private final List<Tag> entities = new ArrayList<>();
	private final Set<Long> frames = new HashSet<>();

	BedrockBlockEntities(RawChunk chunk, Neighbourhood blocks, BedrockBlockMapper mapper) {
		this.chunk = chunk;
		this.blocks = blocks;
		this.mapper = mapper;
		this.minY = chunk.getWorldY(0);
	}

	/** Converts one block entity at world position x, y, z. */
	void convert(String id, CompoundTag entity, int x, int y, int z) {
		int lx = (int) (x - chunk.getChunkCoord().x * 16);
		int ly = y - minY;
		int lz = (int) (z - chunk.getChunkCoord().z * 16);
		if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16 || ly < 0 || ly >= chunk.getSectionCount() * 16)
			return;
		switch (id) {
			case "Banner" -> banner(entity, x, y, z, lx, ly, lz);
			case "Bed" -> bed(entity, x, y, z, lx, ly, lz);
			case "Skull" -> skull(entity, lx, ly, lz);
			case "FlowerPot" -> flowerPot(entity, lx, ly, lz);
			case "ItemFrame", "GlowItemFrame" -> itemFrame(entity, x, y, z, lx, ly, lz);
			case "DecoratedPot" -> decoratedPot(entity, x, y, z);
			case "Beacon" -> beacon(entity, x, y, z, lx, ly, lz);
			case "ShulkerBox" -> {
				Tag facing = entity.getValue().get("facing");
				if (facing != null)
					setProperty(lx, ly, lz, "facing", BedrockBlockMapper.FACINGS[Math.floorMod(BedrockItems.number(facing, 1), 6)]);
				container("minecraft:shulker_box", entity, x, y, z);
			}
			case "Chest" -> container("minecraft:chest", entity, x, y, z);
			case "Barrel" -> container("minecraft:barrel", entity, x, y, z);
			case "EnderChest" -> container("minecraft:ender_chest", entity, x, y, z);
			default -> {
				// Other block entities change nothing Tectonicus draws
			}
		}
	}

	/** Turns item frames without a block entity into empty frames, and adds everything to the chunk. */
	void finish() {
		JavaState[][] sections = blocks.centre().sections();
		for (int section = 0; section < sections.length; section++) {
			JavaState[] states = sections[section];
			if (states == null)
				continue;
			for (int x = 0; x < 16; x++)
				for (int z = 0; z < 16; z++)
					for (int dy = 0; dy < 16; dy++) {
						int y = section * 16 + dy;
						if (isFrame(states[RawChunk.sectionIndex(x, dy, z)].name()) && !frames.contains(key(x, y, z)))
							itemFrame(null, (int) chunk.getChunkCoord().x * 16 + x, y + minY, (int) chunk.getChunkCoord().z * 16 + z, x, y, z);
					}
		}
		if (!blockEntities.isEmpty())
			chunk.addBlockEntities(new ListTag("block_entities", CompoundTag.class, blockEntities));
		if (!entities.isEmpty())
			chunk.addEntities(new ListTag("Entities", CompoundTag.class, entities));
	}

	private static boolean isFrame(String name) {
		return name.equals(BedrockBlockMapper.ITEM_FRAME) || name.equals(BedrockBlockMapper.GLOW_ITEM_FRAME);
	}

	private static long key(int x, int y, int z) {
		return ((long) y << 8) | (x << 4) | z;
	}

	private Map<String, Tag> javaBlockEntity(String id, int x, int y, int z) {
		Map<String, Tag> tag = new HashMap<>();
		tag.put("id", new StringTag("id", id));
		tag.put("x", new IntTag("x", x));
		tag.put("y", new IntTag("y", y));
		tag.put("z", new IntTag("z", z));
		return tag;
	}

	private void banner(CompoundTag entity, int x, int y, int z, int lx, int ly, int lz) {
		boolean ominous = BedrockItems.number(entity.getValue().get("Type"), 0) == 1;
		String color = ominous ? "white" : BedrockItems.legacyColor(BedrockItems.number(entity.getValue().get("Base"), 0));
		String name = chunk.getBlockName(lx, ly, lz);
		if (name != null && name.endsWith("_banner"))
			chunk.setBlockName(lx, ly, lz, "minecraft:" + color + (name.endsWith("_wall_banner") ? "_wall_banner" : "_banner"));

		List<Tag> patterns = new ArrayList<>();
		if (ominous) {
			for (String[] layer : OMINOUS)
				patterns.add(pattern(layer[0], layer[1]));
		} else if (entity.getValue().get("Patterns") instanceof ListTag list) {
			for (Tag layer : list.getValue()) {
				if (!(layer instanceof CompoundTag compound))
					continue;
				String pattern = PATTERNS.get(BedrockChunkReader.string(compound, "Pattern", ""));
				if (pattern != null)
					patterns.add(pattern(pattern, BedrockItems.legacyColor(BedrockItems.number(compound.getValue().get("Color"), 0))));
			}
		}
		Map<String, Tag> tag = javaBlockEntity("minecraft:banner", x, y, z);
		tag.put("patterns", new ListTag("patterns", CompoundTag.class, patterns));
		blockEntities.add(new CompoundTag("", tag));
	}

	private static CompoundTag pattern(String pattern, String color) {
		return new CompoundTag("", Map.of("Pattern", new StringTag("Pattern", "minecraft:" + pattern),
				"color", new StringTag("color", color)));
	}

	private void bed(CompoundTag entity, int x, int y, int z, int lx, int ly, int lz) {
		int color = Math.floorMod(BedrockItems.number(entity.getValue().get("color"), 14), 16);
		String name = chunk.getBlockName(lx, ly, lz);
		if (name != null && name.endsWith("_bed"))
			chunk.setBlockName(lx, ly, lz, "minecraft:" + ContainerKind.colorName(Colors.byId(color)) + "_bed");
		Map<String, Tag> tag = javaBlockEntity("minecraft:bed", x, y, z);
		tag.put("color", new IntTag("color", color));
		blockEntities.add(new CompoundTag("", tag));
	}

	private void skull(CompoundTag entity, int lx, int ly, int lz) {
		String name = chunk.getBlockName(lx, ly, lz);
		BlockProperties state = chunk.getBlockState(lx, ly, lz);
		if (name == null || state == null)
			return;
		boolean wall = name.contains("_wall_");
		Tag type = entity.getValue().get("SkullType");
		if (type != null) {
			String head = HEADS[Math.floorMod(BedrockItems.number(type, 0), HEADS.length)];
			if (wall)
				head = head.endsWith("_skull") ? head.replace("_skull", "_wall_skull") : head.replace("_head", "_wall_head");
			chunk.setBlockName(lx, ly, lz, "minecraft:" + head);
		}
		if (!wall && entity.getValue().get("Rotation") instanceof FloatTag rotation)
			setProperty(lx, ly, lz, "rotation", Integer.toString(Math.floorMod(Math.round(rotation.getValue() / 22.5f), 16)));
	}

	private void flowerPot(CompoundTag entity, int lx, int ly, int lz) {
		CompoundTag plant = BedrockChunkReader.compound(entity, "PlantBlock");
		if (plant == null)
			return;
		String name = mapper.map(plant).name();
		String potted = switch (name) {
			case "minecraft:azalea" -> "minecraft:potted_azalea_bush";
			case "minecraft:flowering_azalea" -> "minecraft:potted_flowering_azalea_bush";
			default -> "minecraft:potted_" + name.substring(name.indexOf(':') + 1);
		};
		chunk.setBlockName(lx, ly, lz, potted);
		chunk.setBlockState(lx, ly, lz, new BlockProperties());
	}

	private void itemFrame(CompoundTag entity, int x, int y, int z, int lx, int ly, int lz) {
		String name = chunk.getBlockName(lx, ly, lz);
		if (name == null || !isFrame(name))
			return;
		BlockProperties state = chunk.getBlockState(lx, ly, lz);
		String facing = state == null ? "north" : state.get("facing");
		int direction = 2;
		for (int i = 0; i < BedrockBlockMapper.FACINGS.length; i++)
			if (BedrockBlockMapper.FACINGS[i].equals(facing))
				direction = i;
		frames.add(key(lx, ly, lz));
		chunk.setBlockName(lx, ly, lz, "minecraft:air");
		chunk.setBlockState(lx, ly, lz, new BlockProperties());

		Map<String, Tag> tag = new HashMap<>();
		tag.put("id", new StringTag("id", name.equals(BedrockBlockMapper.GLOW_ITEM_FRAME) ? "minecraft:glow_item_frame" : "minecraft:item_frame"));
		tag.put("Facing", new ByteTag("Facing", (byte) direction));
		tag.put("block_pos", new IntArrayTag("block_pos", new int[] {x, y, z}));
		if (entity != null) {
			CompoundTag item = BedrockItems.toJava(BedrockChunkReader.compound(entity, "Item"), null, mapper);
			if (item != null)
				tag.put("Item", new CompoundTag("Item", item.getValue()));
			float rotation = entity.getValue().get("ItemRotation") instanceof FloatTag f ? f.getValue() : 0;
			tag.put("ItemRotation", new ByteTag("ItemRotation", (byte) Math.floorMod(Math.round(rotation / 45f), 8)));
		}
		entities.add(new CompoundTag("", tag));
	}

	private void decoratedPot(CompoundTag entity, int x, int y, int z) {
		Map<String, Tag> tag = javaBlockEntity("minecraft:decorated_pot", x, y, z);
		if (entity.getValue().get("sherds") instanceof ListTag sherds) {
			List<Tag> java = new ArrayList<>();
			for (Tag sherd : sherds.getValue())
				if (sherd instanceof StringTag s)
					java.add(new StringTag("", s.getValue().isEmpty() ? "minecraft:brick" : s.getValue()));
			tag.put("sherds", new ListTag("sherds", StringTag.class, java));
		}
		blockEntities.add(new CompoundTag("", tag));
	}

	private void beacon(CompoundTag entity, int x, int y, int z, int lx, int ly, int lz) {
		Map<String, Tag> tag = javaBlockEntity("minecraft:beacon", x, y, z);
		tag.put("Primary", new IntTag("Primary", BedrockItems.number(entity.getValue().get("primary"), 0)));
		tag.put("Secondary", new IntTag("Secondary", BedrockItems.number(entity.getValue().get("secondary"), 0)));
		tag.put("Levels", new IntTag("Levels", beaconLevels(lx, ly, lz)));
		blockEntities.add(new CompoundTag("", tag));
	}

	/** Bedrock does not save how many pyramid levels power a beacon, so count them as Java does. */
	private int beaconLevels(int x, int y, int z) {
		int levels = 0;
		for (int level = 1; level <= 4; level++) {
			int below = y - level;
			if (below < 0)
				break;
			for (int dx = -level; dx <= level; dx++)
				for (int dz = -level; dz <= level; dz++)
					if (!BEACON_BASES.contains(blocks.name(x + dx, below, z + dz)))
						return levels;
			levels = level;
		}
		return levels;
	}

	private void container(String id, CompoundTag entity, int x, int y, int z) {
		Map<String, Tag> tag = javaBlockEntity(id, x, y, z);
		tag.put("Items", BedrockItems.toJava(entity.getValue().get("Items") instanceof ListTag items ? items : null, mapper));
		String name = BedrockChunkReader.string(entity, "CustomName", null);
		if (name != null && !name.isEmpty())
			tag.put("CustomName", new StringTag("CustomName", name));
		String lootTable = BedrockChunkReader.string(entity, "LootTable", null);
		if (lootTable != null)
			tag.put("LootTable", new StringTag("LootTable", lootTable));
		blockEntities.add(new CompoundTag("", tag));
	}

	private void setProperty(int lx, int ly, int lz, String key, String value) {
		BlockProperties state = chunk.getBlockState(lx, ly, lz);
		Map<String, String> properties = new TreeMap<>(state == null ? Map.of() : state.getProperties());
		properties.put(key, value);
		chunk.setBlockState(lx, ly, lz, new BlockProperties(properties));
	}
}
