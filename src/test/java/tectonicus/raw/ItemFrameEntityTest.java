package tectonicus.raw;

import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntArrayTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream.Compression;
import org.jnbt.NBTOutputStream;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import org.junit.jupiter.api.Test;
import tectonicus.Version;
import tectonicus.chunk.ChunkData;
import tectonicus.world.WorldInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemFrameEntityTest {
	@Test
	void readsNumericMapIdAndSupportBlockPosition() throws IOException {
		Map<String, Tag> entity = oldPosition("ItemFrame");
		entity.put("Direction", new ByteTag("Direction", (byte) 0));
		entity.put("Dir", new ByteTag("Dir", (byte) 2));
		entity.put("Item", new CompoundTag("Item", Map.of("id", new ShortTag("id", (short) 358),
				"Damage", new ShortTag("Damage", (short) 0))));
		ItemFrameEntity frame = readEmbedded(entity, Version.VERSION_4).getItemFrames().getFirst();
		assertEquals(0, frame.getMapId());
		assertEquals(3, frame.getDirection());
		assertEquals(5, frame.getLocalZ());
		assertTrue(frame.isFilledMap());
	}

	@Test
	void readsTagMapIdAndLegacyHorizontalFacing() throws IOException {
		Map<String, Tag> entity = oldPosition("minecraft:item_frame");
		entity.put("Facing", new ByteTag("Facing", (byte) 1));
		entity.put("Item", filledMap(new CompoundTag("tag", Map.of("map", new IntTag("map", 91000)))));
		ItemFrameEntity frame = readEmbedded(entity, Version.VERSION_12).getItemFrames().getFirst();
		assertEquals(91000, frame.getMapId());
		assertEquals(4, frame.getDirection());
		assertEquals(0, frame.getLocalX());
	}

	@Test
	void readsComponentMapIdAndFloorFacingFromEntityRegion() throws IOException {
		var entity = modernFrame(1, 7);
		ItemFrameEntity frame = readSplit(entity).getItemFrames().getFirst();
		assertEquals(4, frame.getMapId());
		assertEquals(1, frame.getDirection());
		assertEquals(3, frame.getRotation());
		assertEquals(58, frame.getLocalY());
		assertEquals(15, frame.getLocalZ());
		assertTrue(frame.isGlow());
	}

	@Test
	void doesNotTreatAMapWithoutAnIdAsMapZero() throws IOException {
		var entity = modernFrame(2, 0);
		entity.put("Item", new CompoundTag("Item", Map.of("id", new StringTag("id", "minecraft:filled_map"))));
		ItemFrameEntity frame = readSplit(entity).getItemFrames().getFirst();
		assertTrue(frame.isFilledMap());
		assertNull(frame.getMapId());
	}

	@Test
	void chunkHashDependsOnFrameAndExternalMapPixels() throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		RawChunk chunk = readSplit(modernFrame(2, 0));
		byte[] initial = chunk.calculateHash(digest, id -> new byte[] {1});
		assertArrayEquals(initial, chunk.calculateHash(digest, id -> new byte[] {1}));
		assertFalse(MessageDigest.isEqual(initial, chunk.calculateHash(digest, id -> new byte[] {2})));
		assertFalse(MessageDigest.isEqual(initial, chunk.calculateHash(digest, id -> null)));
		assertFalse(MessageDigest.isEqual(initial, readSplit(modernFrame(2, 1)).calculateHash(digest, id -> new byte[] {1})));
	}

	@Test
	void readsHeldItemEighthTurnsAndInvisibility() throws IOException {
		var entity = modernFrame(3, 5);
		entity.put("Invisible", new ByteTag("Invisible", (byte) 1));
		entity.put("Item", new CompoundTag("Item", Map.of("id", new StringTag("id", "minecraft:leather_helmet"),
				"count", new IntTag("count", 1),
				"components", new CompoundTag("components", Map.of("minecraft:dyed_color", new IntTag("minecraft:dyed_color", 0x3366FF))))));
		ItemFrameEntity frame = readSplit(entity).getItemFrames().getFirst();
		assertEquals("minecraft:leather_helmet", frame.getItem().id);
		assertEquals(0x3366FF, frame.getItem().getComponent(DyedColorTag.class).color);
		assertEquals(5, frame.getItemRotation());
		assertEquals(1, frame.getRotation());
		assertTrue(frame.isInvisible());
		assertFalse(frame.isFilledMap());
	}

	@Test
	void mapsAreNotHeldItems() throws IOException {
		ItemFrameEntity frame = readSplit(modernFrame(2, 0)).getItemFrames().getFirst();
		assertNull(frame.getItem());
		assertFalse(frame.isInvisible());
	}

	@Test
	void convertsNumericItemsAndQuarterTurnsFromBefore18() throws IOException {
		Map<String, Tag> entity = oldPosition("ItemFrame");
		entity.put("Direction", new ByteTag("Direction", (byte) 0));
		entity.put("ItemRotation", new ByteTag("ItemRotation", (byte) 3));
		entity.put("Item", new CompoundTag("Item", Map.of("id", new ShortTag("id", (short) 35),
				"Damage", new ShortTag("Damage", (short) 14), "Count", new ByteTag("Count", (byte) 1))));
		ItemFrameEntity frame = readEmbedded(entity, Version.VERSION_4).getItemFrames().getFirst();
		assertEquals("minecraft:red_wool", frame.getItem().id);
		assertEquals(6, frame.getItemRotation());
	}

	@Test
	void convertsDamageVariantsSavedBefore113() throws IOException {
		Map<String, Tag> entity = oldPosition("minecraft:item_frame");
		entity.put("Facing", new ByteTag("Facing", (byte) 0));
		entity.put("ItemRotation", new ByteTag("ItemRotation", (byte) 3));
		entity.put("Item", new CompoundTag("Item", Map.of("id", new StringTag("id", "minecraft:log"),
				"Damage", new ShortTag("Damage", (short) 2), "Count", new ByteTag("Count", (byte) 1))));
		ItemFrameEntity frame = readEmbedded(entity, Version.VERSION_12).getItemFrames().getFirst();
		assertEquals("minecraft:birch_log", frame.getItem().id);
		assertEquals(3, frame.getItemRotation());
	}

	@Test
	void chunkHashDependsOnHeldItem() throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] sword = hashWithItem(digest, "minecraft:diamond_sword", 0, false);
		assertArrayEquals(sword, hashWithItem(digest, "minecraft:diamond_sword", 0, false));
		assertFalse(MessageDigest.isEqual(sword, hashWithItem(digest, "minecraft:iron_sword", 0, false)));
		assertFalse(MessageDigest.isEqual(sword, hashWithItem(digest, "minecraft:diamond_sword", 5, false)));
		assertFalse(MessageDigest.isEqual(sword, hashWithItem(digest, "minecraft:diamond_sword", 0, true)));
	}

	@Test
	void chunkHashDependsOnHeldPotion() throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		assertFalse(MessageDigest.isEqual(hashWithPotion(digest, "minecraft:swiftness"), hashWithPotion(digest, "minecraft:healing")));
	}

	private static byte[] hashWithPotion(MessageDigest digest, String potion) throws IOException {
		var entity = modernFrame(3, 0);
		entity.put("Item", new CompoundTag("Item", Map.of("id", new StringTag("id", "minecraft:potion"), "count", new IntTag("count", 1),
				"components", new CompoundTag("components", Map.of("minecraft:potion_contents",
						new CompoundTag("minecraft:potion_contents", Map.of("potion", new StringTag("potion", potion))))))));
		return readSplit(entity).calculateHash(digest, mapId -> null);
	}

	private static byte[] hashWithItem(MessageDigest digest, String id, int rotation, boolean invisible) throws IOException {
		var entity = modernFrame(3, rotation);
		entity.put("Invisible", new ByteTag("Invisible", (byte) (invisible ? 1 : 0)));
		entity.put("Item", new CompoundTag("Item", Map.of("id", new StringTag("id", id), "count", new IntTag("count", 1))));
		return readSplit(entity).calculateHash(digest, mapId -> null);
	}

	private static Map<String, Tag> oldPosition(String entityId) {
		return new HashMap<>(Map.of("id", new StringTag("id", entityId), "TileX", new IntTag("TileX", 0),
				"TileY", new IntTag("TileY", 64), "TileZ", new IntTag("TileZ", 4)));
	}

	private static Map<String, Tag> modernFrame(int facing, int rotation) {
		return new HashMap<>(Map.of("id", new StringTag("id", "minecraft:glow_item_frame"),
				"Facing", new ByteTag("Facing", (byte) facing), "ItemRotation", new ByteTag("ItemRotation", (byte) rotation),
				"block_pos", new IntArrayTag("block_pos", new int[] {0, -6, 15}),
				"Item", filledMap(new CompoundTag("components", Map.of("minecraft:map_id", new IntTag("minecraft:map_id", 4))))));
	}

	private static CompoundTag filledMap(CompoundTag data) {
		return new CompoundTag("Item", Map.of("id", new StringTag("id", "minecraft:filled_map"), data.getName(), data));
	}

	private static RawChunk readEmbedded(Map<String, Tag> entity, Version version) throws IOException {
		var level = new CompoundTag("Level", Map.of("Sections", emptyList("Sections"), "Entities", entities(entity)));
		return new RawChunk(data(Map.of("Level", level, "DataVersion", new IntTag("DataVersion", version.getDataVersion()))),
				null, new WorldInfo(version, false));
	}

	private static RawChunk readSplit(Map<String, Tag> entity) throws IOException {
		ChunkData blocks = data(Map.of("sections", emptyList("sections"), "block_entities", emptyList("block_entities"),
				"yPos", new IntTag("yPos", -4), "DataVersion", new IntTag("DataVersion", Version.VERSION_26_3.getDataVersion())));
		return new RawChunk(blocks, data(Map.of("Entities", entities(entity))), null, new WorldInfo(Version.VERSION_26_3, true));
	}

	private static ListTag emptyList(String name) {
		return new ListTag(name, CompoundTag.class, List.of());
	}

	private static ListTag entities(Map<String, Tag> entity) {
		return new ListTag("Entities", CompoundTag.class, List.of(new CompoundTag("", entity)));
	}

	private static ChunkData data(Map<String, Tag> tags) throws IOException {
		var bytes = new ByteArrayOutputStream();
		try (NBTOutputStream nbt = new NBTOutputStream(bytes)) {
			nbt.writeTag(new CompoundTag("", tags));
		}
		return new ChunkData(bytes.toByteArray(), Compression.Gzip);
	}
}
