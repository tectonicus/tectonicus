/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jnbt.ByteArrayTag;
import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntArrayTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.LongArrayTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTInputStream.Compression;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import tectonicus.Block;
import tectonicus.BlockIds;
import tectonicus.ChunkCoord;
import tectonicus.ChunkData;
import tectonicus.Version;
import tectonicus.WorldStats;
import tectonicus.blockTypes.Banner.Pattern;
import tectonicus.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tectonicus.Version.VERSION_18;
import static tectonicus.Version.VERSION_UNKNOWN;

public class RawChunk {
	public static final int WIDTH = 16;
	//public static final int HEIGHT = 256;
	public static final int HEIGHT = 384;
	public static final int DEPTH = 16;
	//public static final int NEW_HEIGHT = 384;
	//TODO: Tile entities and entities may need y value adjustment

	public static final int MC_REGION_HEIGHT = 128;

	public static final int SECTION_WIDTH = 16;
	public static final int SECTION_HEIGHT = 16;
	public static final int SECTION_DEPTH = 16;

	public static final int MAX_LIGHT = 16;

	private static final int MAX_SECTIONS = HEIGHT / SECTION_HEIGHT;
	//public static final int NEW_MAX_SECTIONS = NEW_HEIGHT / SECTION_HEIGHT;

	private static final ObjectReader OBJECT_READER = FileUtils.getOBJECT_MAPPER().reader();
	private static final ObjectWriter OBJECT_WRITER = FileUtils.getOBJECT_MAPPER().writer();

	private static final String CHEST = "Chest";

	private int[][] biomes;
	private int[][][] biomes3d;

	private Section[] sections;

	private int chunkX, chunkY, chunkZ;
	private int minSectionY;

	private Map<String, SignEntity> signs;
	private Map<String, FlowerPotEntity> flowerPots;
	private Map<String, SkullEntity> skulls;
	private Map<String, BeaconEntity> beacons;
	private Map<String, BannerEntity> banners;
	private Map<String, BedEntity> beds;

	private List<PaintingEntity> paintings;
	private List<PaintingEntity> itemFrames;
	private List<ContainerEntity> chests;

	private final Map<String, Object> filterData = new HashMap<>();

	public RawChunk() {
		clear();
	}

	public RawChunk(File file) throws IOException {
		init(new ChunkData(Files.readAllBytes(file.toPath()), Compression.Gzip), null, VERSION_UNKNOWN);
	}

	public RawChunk(ChunkData chunkData, WorldStats worldStats, Version version) throws IOException {
		init(chunkData, worldStats, version);
	}

	public RawChunk(ChunkData chunkData, ChunkData entityChunkData, WorldStats worldStats, Version version) throws IOException {
		this(chunkData, worldStats, version);

		byte[] chunkBytes = entityChunkData.getBytes();
		try (InputStream in = new ByteArrayInputStream(chunkBytes, 0, chunkBytes.length);
			 NBTInputStream nbtIn = new NBTInputStream(in, entityChunkData.getCompressionType())) {
			Tag tag = nbtIn.readTag();
			if (tag instanceof CompoundTag) {
				parseEntities(NbtUtil.getChild((CompoundTag) tag, "Entities", ListTag.class), true);
			}
		}
	}

	public void setFilterMetadata(String id, Object data) {
		this.filterData.put(id, data);
	}

	public void removeFilterMetadata(String id) {
		this.filterData.remove(id);
	}

	public Object getFilterMetadata(String id) {
		return this.filterData.get(id);
	}

	private void clear() {
		signs = new HashMap<>();
		flowerPots = new HashMap<>();
		skulls = new HashMap<>();
		beacons = new HashMap<>();
		banners = new HashMap<>();
		beds = new HashMap<>();

		paintings = new ArrayList<>();
		itemFrames = new ArrayList<>();
		chests = new ArrayList<>();

		sections = new Section[MAX_SECTIONS];
	}

	private void init(ChunkData chunkData, WorldStats worldStats, Version version) throws IOException {
		clear();

		byte [] chunkBytes = chunkData.getBytes();
		try (InputStream in = new ByteArrayInputStream(chunkBytes, 0, chunkBytes.length);
			 NBTInputStream nbtIn = new NBTInputStream(in, chunkData.getCompressionType())) {
			Tag tag = nbtIn.readTag();
			if (tag instanceof CompoundTag) {
				CompoundTag root = (CompoundTag) tag;

				CompoundTag level = NbtUtil.getChild(root, "Level", CompoundTag.class);
				if (level != null) { //Chunk was generated by Minecraft 1.17 or lower
					parseChunkPosition(level);

					ListTag sectionsTag = NbtUtil.getChild(level, "Sections", ListTag.class);
					if (sectionsTag != null) {
						// Parse as anvil format
						parseAnvilData(level, sectionsTag, worldStats, version);
					} else {
						// Parse as McRegion format
						parseMcRegionData(level);
					}

					ListTag entitiesTag = NbtUtil.getChild(level, "Entities", ListTag.class);
					if (entitiesTag != null && !entitiesTag.getValue().isEmpty()) {
						parseEntities(entitiesTag, version.getNumVersion() >= VERSION_18.getNumVersion());
					}

					ListTag tileEntitiesTag = NbtUtil.getChild(level, "TileEntities", ListTag.class);
					if (tileEntitiesTag != null) {
						parseBlockEntities(tileEntitiesTag, version.getNumVersion() >= VERSION_18.getNumVersion());
					}
				}
				else { //Chunk was generated by Minecraft 1.18+
					parseChunkPosition(root);

					ListTag sectionsTag = NbtUtil.getChild(root, "sections", ListTag.class);
					parseAnvilDataNew(root, sectionsTag, worldStats);

					ListTag blockEntitiesTag = NbtUtil.getChild(root, "block_entities", ListTag.class);
					parseBlockEntities(blockEntitiesTag, true);
				}
			}
		}
	}

	private void parseChunkPosition(CompoundTag tag) {
		chunkX = chunkY = chunkZ = 0;

		IntTag xPosTag = NbtUtil.getChild(tag, "xPos", IntTag.class);
		if (xPosTag != null)
			chunkX = xPosTag.getValue();

		IntTag zPosTag = NbtUtil.getChild(tag, "zPos", IntTag.class);
		if (zPosTag != null)
			chunkZ = zPosTag.getValue();

		//yPos is the minimum or lowest section position in the chunk (added in 1.18, with -4 as lowest)
		IntTag yPosTag = NbtUtil.getChild(tag, "yPos", IntTag.class);
		if (yPosTag != null)
			minSectionY = yPosTag.getValue();
	}

	private void parseEntities(ListTag entitiesTag, boolean is118) {
		for (Tag t : entitiesTag.getValue()) {
			if (t instanceof CompoundTag) {
				CompoundTag entity = (CompoundTag) t;

				StringTag idTag = NbtUtil.getChild(entity, "id", StringTag.class);
				boolean painting = idTag.getValue().endsWith("Painting") || idTag.getValue().equals("minecraft:painting");
				boolean itemFrame = idTag.getValue().equals("ItemFrame") || idTag.getValue().equals("minecraft:item_frame");
				if (painting || itemFrame) {
					IntTag xTag = NbtUtil.getChild(entity, "TileX", IntTag.class);
					IntTag yTag = NbtUtil.getChild(entity, "TileY", IntTag.class);
					IntTag zTag = NbtUtil.getChild(entity, "TileZ", IntTag.class);
					ByteTag oldDir = NbtUtil.getChild(entity, "Dir", ByteTag.class);
					ByteTag dir = NbtUtil.getChild(entity, "Direction", ByteTag.class);

					if (oldDir != null && dir == null) {
						dir = oldDir;
					}

					boolean is18 = false;
					if (dir == null) {
						dir = NbtUtil.getChild(entity, "Facing", ByteTag.class);
						is18 = true;
					}

					int direction = dir.getValue();  // Have to reverse 0 and 2 for the old Dir tag
					if (oldDir != null && direction == 0) {
						direction = 2;
					} else if (oldDir != null && direction == 2) {
						direction = 0;
					}

					int x = xTag.getValue();
					final int y = yTag.getValue();
					int z = zTag.getValue();

					if (is18 && direction == 0) {
						z = zTag.getValue() - 1;
					} else if (is18 && direction == 1) {
						x = xTag.getValue() + 1;
					} else if (is18 && direction == 2) {
						z = zTag.getValue() + 1;
					} else if (is18 && direction == 3) {
						x = xTag.getValue() - 1;
					}

					final int localX = x - (chunkX * WIDTH);
					int localY;
					if (is118) {
						localY = y + 64;
					} else {
						localY = y - (chunkY * HEIGHT);
					}
					final int localZ = z - (chunkZ * DEPTH);


					if (painting) {
						StringTag motiveTag = NbtUtil.getChild(entity, "Motive", StringTag.class);
						paintings.add(new PaintingEntity(x, y, z, localX, localY, localZ, motiveTag.getValue(), direction));
					} else {
						String item = "";
						Map<String, Tag> map = entity.getValue();
						CompoundTag itemTag = (CompoundTag) map.get("Item");
						if (itemTag != null) {
							ShortTag itemIdTag = NbtUtil.getChild(itemTag, "id", ShortTag.class);
							if (itemIdTag == null) {
								StringTag stringItemIdTag = NbtUtil.getChild(itemTag, "id", StringTag.class);
								item = stringItemIdTag.getValue();
							} else {
								if (itemIdTag.getValue() == 358)
									item = "minecraft:filled_map";
							}
						}

						itemFrames.add(new PaintingEntity(x, y, z, localX, localY, localZ, item, direction));
					}
				}
			}
		}
	}

	private void parseBlockEntities(ListTag blockEntitiesTag, boolean is118) throws JsonProcessingException {
		for (Tag t : blockEntitiesTag.getValue()) {
			if (t instanceof CompoundTag) {
				CompoundTag entity = (CompoundTag) t;

				StringTag idTag = NbtUtil.getChild(entity, "id", StringTag.class);
				IntTag xTag = NbtUtil.getChild(entity, "x", IntTag.class);
				IntTag yTag = NbtUtil.getChild(entity, "y", IntTag.class);
				IntTag zTag = NbtUtil.getChild(entity, "z", IntTag.class);

				if (idTag != null && xTag != null && yTag != null && zTag != null) {
					final String id = idTag.getValue();

					final int x = xTag.getValue();
					final int y = yTag.getValue();
					final int z = zTag.getValue();

					final int localX = x - (chunkX * WIDTH);
					int localY;
					if (is118) {
						localY = y + 64;
					} else {
						localY = y - (chunkY * HEIGHT);
					}
					final int localZ = z - (chunkZ * DEPTH);

					if (id.equals("Sign") || id.equals("minecraft:sign")) {
						List<String> textLines = new ArrayList<>();

						for (int i = 1; i <= 4; i++) {
							String text = NbtUtil.getChild(entity, "Text" + i, StringTag.class).getValue();

							if (!StringUtils.isEmpty(text) && FileUtils.isJSONValid(text))  // 1.9 sign text
							{
								textLines.add(textFromJSON(text));
							} else if (!StringUtils.isBlank(text)) // 1.8 or older sign text
							{
								text = text.replaceAll("^\"|\"$", ""); //This removes begin and end double quotes
								textLines.add(OBJECT_WRITER.writeValueAsString(text).replaceAll("^\"|\"$", ""));
							} else {
								textLines.add("");
							}
						}

						StringTag colorTag = NbtUtil.getChild(entity, "Color", StringTag.class);
						String color = "black";
						if (colorTag != null) {
							color = colorTag.getValue();
						}

						final int data = getBlockData(localX, localY, localZ);

						signs.put(createKey(localX, localY, localZ), new SignEntity(x, y, z, localX, localY, localZ,
								textLines.get(0), textLines.get(1), textLines.get(2), textLines.get(3), data, color));
					} else if (id.equals("FlowerPot") || id.equals("minecraft:flower_pot")) {
						IntTag dataTag = NbtUtil.getChild(entity, "Data", IntTag.class);
						IntTag itemTag = NbtUtil.getChild(entity, "Item", IntTag.class);
						final int item;
						if (itemTag == null) {
							StringTag stringIdTag = NbtUtil.getChild(entity, "Item", StringTag.class);
							if (stringIdTag.getValue().equals("minecraft:sapling"))
								item = 6;
							else if (stringIdTag.getValue().equals("minecraft:red_flower"))
								item = 38;
							else
								item = 0;
						} else {
							item = itemTag.getValue();
						}

						flowerPots.put(createKey(localX, localY, localZ), new FlowerPotEntity(x, y, z, localX, localY, localZ, item, dataTag.getValue()));
					} else if (id.equals("Skull") || id.equals("minecraft:skull")) {
						ByteTag skullType = NbtUtil.getChild(entity, "SkullType", ByteTag.class);
						ByteTag rot = NbtUtil.getChild(entity, "Rot", ByteTag.class);

						StringTag nameTag;
						StringTag playerId;
						String name = "";
						String uuid = "";
						String textureURL = "";
						StringTag extraType = NbtUtil.getChild(entity, "ExtraType", StringTag.class);
						CompoundTag owner = NbtUtil.getChild(entity, "Owner", CompoundTag.class);
						if (owner == null) {  //1.16
							owner = NbtUtil.getChild(entity, "SkullOwner", CompoundTag.class);
						}
						if (owner != null) {
							nameTag = NbtUtil.getChild(owner, "Name", StringTag.class);
							if (nameTag != null) {
								name = nameTag.getValue();
							}
							playerId = NbtUtil.getChild(owner, "Id", StringTag.class);
							if (playerId == null) { //1.16
								IntArrayTag idArrayTag = NbtUtil.getChild(owner, "Id", IntArrayTag.class);
								if (idArrayTag != null) {
									int[] integerIdArray = idArrayTag.getValue();
									StringBuilder sb = new StringBuilder();
									for (int integerId : integerIdArray) {
										sb.append(Integer.toHexString(integerId));
									}
									uuid = sb.toString();
								}
							} else {
								uuid = playerId.getValue().replace("-", "");
							}

							// Get skin URL
							CompoundTag properties = NbtUtil.getChild(owner, "Properties", CompoundTag.class);
							ListTag textures = NbtUtil.getChild(properties, "textures", ListTag.class);
							CompoundTag tex = NbtUtil.getChild(textures, 0, CompoundTag.class);
							StringTag value = NbtUtil.getChild(tex, "Value", StringTag.class);
							byte[] decoded = Base64.getDecoder().decode(value.getValue());
							JsonNode node = OBJECT_READER.readTree(new String(decoded, StandardCharsets.UTF_8));
							textureURL = node.get("textures").get("SKIN").get("url").asText();
						} else if (extraType != null && !(extraType.getValue().equals(""))) {
							name = uuid = extraType.getValue();
							textureURL = "http://www.minecraft.net/skin/" + extraType.getValue() + ".png";
						}

						if (skullType != null) {
							skulls.put(createKey(localX, localY, localZ), new SkullEntity(x, y, z, localX, localY, localZ, skullType.getValue(), rot.getValue(), name, uuid, textureURL));
						} else { // 1.13+ no longer has SkullType or Rot tags
							skulls.put(createKey(localX, localY, localZ), new SkullEntity(x, y, z, localX, localY, localZ, name, uuid, textureURL));
						}
					} else if (id.equals("Beacon") || id.equals("minecraft:beacon")) {
						IntTag levels = NbtUtil.getChild(entity, "Levels", IntTag.class);

						beacons.put(createKey(localX, localY, localZ), new BeaconEntity(x, y, z, localX, localY, localZ, levels.getValue()));
					} else if (id.equals("Banner") || id.equals("minecraft:banner")) {
						IntTag base = NbtUtil.getChild(entity, "Base", IntTag.class);
						int baseVal = 0;

						if (base != null)
							baseVal = base.getValue();

						ListTag patternList = NbtUtil.getChild(entity, "Patterns", ListTag.class);

						List<Pattern> patterns = new ArrayList<>();

						int numPatterns = 0;
						if (patternList != null)
							numPatterns = patternList.getValue().size();
						if (numPatterns > 0) {
							for (int i = 0; i < numPatterns; i++) {
								CompoundTag p = NbtUtil.getChild(patternList, i, CompoundTag.class);
								StringTag pattern = NbtUtil.getChild(p, "Pattern", StringTag.class);
								IntTag color = NbtUtil.getChild(p, "Color", IntTag.class);
								patterns.add(new Pattern(pattern.getValue(), color.getValue()));
							}
						}
						banners.put(createKey(localX, localY, localZ), new BannerEntity(x, y, z, localX, localY, localZ, baseVal, patterns));
					} else if (id.equals(CHEST) || id.equals("minecraft:chest") || id.equals("minecraft:shulker_box")) {
						final StringTag customName = NbtUtil.getChild(entity, "CustomName", StringTag.class);
						String name = CHEST;
						if (customName != null)
							name = customName.getValue();

						final StringTag lock = NbtUtil.getChild(entity, "Lock", StringTag.class);
						String lockStr = "";
						if (lock != null)
							lockStr = lock.getValue();

						final StringTag lootTable = NbtUtil.getChild(entity, "LootTable", StringTag.class);

						boolean unopenedChest = lootTable != null;

						if (id.equals(CHEST) || id.equals("minecraft:chest")) {
							chests.add(new ContainerEntity(x, y, z, localX, localY, localZ, name, lockStr, unopenedChest));
						}
//										else if (id.equals("EnderChest") || id.equals("minecraft:ender_chest"))  //TODO: Handle Ender chests
//										{
//
//										}
//						else if (id.equals("minecraft:shulker_box")) {
//
//						}
					} else if (id.equals("minecraft:bed")) {
						final IntTag color = NbtUtil.getChild(entity, "color", IntTag.class);
						int colorVal = 0;
						if (color != null)
							colorVal = color.getValue();
						beds.put(createKey(localX, localY, localZ), new BedEntity(x, y, z, localX, localY, localZ, colorVal));
					}
					//	else if (id.equals("Furnace"))
					//	{
					//
					//	}
					//	else if (id.equals("MobSpawner"))
					//	{
					//
					//	}
				}
			}
		}
	}

	private String createKey(int x, int y, int z) {
		return "x" + x + "y" + y + "z" + z;
	}

	private void parseAnvilDataNew(CompoundTag tag, ListTag sectionsTag, WorldStats worldStats) {
		for (Tag t : sectionsTag.getValue()) {
			CompoundTag compound = (CompoundTag) t;

			final int sectionY = NbtUtil.getByte(compound, "Y", (byte) 0) + 4;

			if (sectionY < minSectionY || sectionY >= MAX_SECTIONS)
				continue;

			ByteArrayTag skylightTag = NbtUtil.getChild(compound, "SkyLight", ByteArrayTag.class);
			ByteArrayTag blocklightTag = NbtUtil.getChild(compound, "BlockLight", ByteArrayTag.class);

			CompoundTag blockStatesContainer = NbtUtil.getChild(compound, "block_states", CompoundTag.class);
			ListTag blockPaletteTag = NbtUtil.getChild(blockStatesContainer, "palette", ListTag.class);
			LongArrayTag blockDataTag = NbtUtil.getChild(blockStatesContainer, "data", LongArrayTag.class);

			CompoundTag biomesContainer = NbtUtil.getChild(compound, "biomes", CompoundTag.class);
			ListTag biomePaletteTag = NbtUtil.getChild(biomesContainer, "palette", ListTag.class);
			LongArrayTag biomeDataTag = NbtUtil.getChild(biomesContainer, "data", LongArrayTag.class);


			List<BlockState> blockStatesPalette = parseBlockStates(blockPaletteTag);

			Section newSection = new Section();
			sections[sectionY] = newSection;

			int bitsPerBlock = 0;
			int blockBitMask = 0;
			int blocksPerLong = 0;
			boolean packedBits = false;
			BlockState singleBlockState = null;

			if (blockDataTag != null) {
				int sectionVolume = SECTION_WIDTH * SECTION_HEIGHT * SECTION_DEPTH;
				int dataTagLength = blockDataTag.getValue().length;
				bitsPerBlock = dataTagLength * 64 / sectionVolume;
				blockBitMask = (1 << bitsPerBlock) - 1;

				if (Math.ceil(sectionVolume / (64f / bitsPerBlock)) == dataTagLength) {
					packedBits = true;
				}
				blocksPerLong = 64 / bitsPerBlock;
			} else {
				//If the data tag doesn't exist that means there is only one block in the palette
				singleBlockState = blockStatesPalette.get(0);
			}

			for (int x = 0; x < SECTION_WIDTH; x++) {
				for (int y = 0; y < SECTION_HEIGHT; y++) {
					if (blockDataTag == null && singleBlockState != null) {
						//If no data tag the section should be filled with the only block in the palette
						Arrays.fill(newSection.blockNames[x][y], singleBlockState.name);
						Arrays.fill(newSection.blockStates[x][y], singleBlockState.properties);
					}
					for (int z = 0; z < SECTION_DEPTH; z++) {
						if (blockDataTag != null && blockDataTag.getValue() != null) {
							final int index = calcAnvilIndex(x, y, z);
							int longIndex;
							int bitOffset;
							if (packedBits) {  // 1.13-1.15 format or 1.16+ when bitsPerBlock is power of 2
								int bitIndex = index * bitsPerBlock;
								longIndex = bitIndex / 64;
								bitOffset = bitIndex % 64;
							} else { // 1.16+ format when bitsPerBlock is not a power of 2
								longIndex = index / blocksPerLong;
								bitOffset = (index % blocksPerLong) * bitsPerBlock;
							}

							long paletteIndex = (blockDataTag.getValue()[longIndex] >>> bitOffset) & blockBitMask;

							BlockState blockState = blockStatesPalette.get((int) paletteIndex);
							newSection.blockNames[x][y][z] = blockState.name;
							newSection.blockStates[x][y][z] = blockState.properties;

							//TODO: add back the block counting code here
//							if (worldStats != null)
//								worldStats.incBlockId(id, data);
						}

						if (skylightTag != null) {
							newSection.skylight[x][y][z] = getAnvil4Bit(skylightTag, x, y, z);
						}

						if (blocklightTag != null) {
							newSection.blocklight[x][y][z] = getAnvil4Bit(blocklightTag, x, y, z);
						}
					}
				}
			}


			// one string id per 4x4x4 volume within the section
			List<Tag> palette = biomePaletteTag.getValue();
			if (biomeDataTag != null) {
				/* The code for dealing with the new biomes palette comes from piegamesde in an Overviewer Github issue
				*  https://github.com/overviewer/Minecraft-Overviewer/issues/2022 */
				long[] data = biomeDataTag.getValue();
				int paletteSize = palette.size();
				int bitsPerEntry = Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1);
				int entriesPerLong = Math.floorDiv(64, bitsPerEntry);
				int mask = (1 << bitsPerEntry) - 1;

				int index = 0;
				for (long l : data) {
					for (int i = 0; i < entriesPerLong && index < 64; i++) {
						newSection.biomeIds[index++] = (String) palette.get((int) (l & mask)).getValue();
						l >>= bitsPerEntry;
					}
				}
			} else { //only a single entry in the biome palette
				Arrays.fill(newSection.biomeIds, palette.get(0).getValue());
			}
		}
	}

	private void parseAnvilData(CompoundTag tag, ListTag sectionsTag, WorldStats worldStats, Version version) {
		for (Tag t : sectionsTag.getValue()) {
			if (!(t instanceof CompoundTag))
				continue;

			CompoundTag compound = (CompoundTag) t;

			int sectionY = NbtUtil.getByte(compound, "Y", (byte) 0);

			/* In order to get block checking between 1.17 and 1.18 chunks to work and to
			   have chunks line up correctly when rendering we need to offset 1.17 or older chunks */
			if (version.getNumVersion() >= VERSION_18.getNumVersion()) {
				sectionY += 4;
			}

			if (sectionY < 0 || sectionY >= MAX_SECTIONS)
				continue;

			ByteArrayTag blocksTag = NbtUtil.getChild(compound, "Blocks", ByteArrayTag.class);
			ByteArrayTag addTag = NbtUtil.getChild(compound, "Add", ByteArrayTag.class);
			ByteArrayTag dataTag = NbtUtil.getChild(compound, "Data", ByteArrayTag.class);
			ByteArrayTag skylightTag = NbtUtil.getChild(compound, "SkyLight", ByteArrayTag.class);
			ByteArrayTag blocklightTag = NbtUtil.getChild(compound, "BlockLight", ByteArrayTag.class);

			// 1.13+ block data
			LongArrayTag blockStatesTag = NbtUtil.getChild(compound, "BlockStates", LongArrayTag.class);
			ListTag paletteTag = NbtUtil.getChild(compound, "Palette", ListTag.class);

			Section newSection = null;
			if (blocksTag != null || blockStatesTag != null) {
				newSection = new Section();
				sections[sectionY] = newSection;
			}

			int bitsPerBlock = 0;
			int blockBitMask = 0;
			int blocksPerLong = 0;
			List<BlockState> blockStatesPalette = null;
			boolean packedBits = false;

			if (blockStatesTag != null) {
				int sectionVolume = SECTION_WIDTH * SECTION_HEIGHT * SECTION_DEPTH;
				int blockStatesTagLength = blockStatesTag.getValue().length;
				bitsPerBlock = blockStatesTagLength * 64 / sectionVolume;
				blockBitMask = (1 << bitsPerBlock) - 1;

				if (Math.ceil(sectionVolume / (64f / bitsPerBlock)) == blockStatesTagLength) {
					packedBits = true;
				}
				blocksPerLong = 64 / bitsPerBlock;

				blockStatesPalette = parseBlockStates(paletteTag);
			}

			for (int x = 0; x < SECTION_WIDTH; x++) {
				for (int y = 0; y < SECTION_HEIGHT; y++) {
					for (int z = 0; z < SECTION_DEPTH; z++) {
						final int index = calcAnvilIndex(x, y, z);

						if (blocksTag != null) {
							int id = blocksTag.getValue()[index] & 0xFF;
							newSection.blockIds[x][y][z] = id;

							if (addTag != null) {
								id = id | (getAnvil4Bit(addTag, x, y, z) << 8);
								newSection.blockIds[x][y][z] = id;
							}

							final byte data = getAnvil4Bit(dataTag, x, y, z);
							newSection.blockData[x][y][z] = data;

							if (worldStats != null)
								worldStats.incBlockId(id, data);
						} else {
							if (blockStatesTag != null && blockStatesTag.getValue() != null) {
								int longIndex;
								int bitOffset;
								if (packedBits) {  // 1.13-1.15 format or 1.16+ when bitsPerBlock is power of 2
									int bitIndex = index * bitsPerBlock;
									longIndex = bitIndex / 64;
									bitOffset = bitIndex % 64;
								} else { // 1.16+ format when bitsPerBlock is not a power of 2
									longIndex = index / blocksPerLong;
									bitOffset = (index % blocksPerLong) * bitsPerBlock;
								}

								long paletteIndex = (blockStatesTag.getValue()[longIndex] >>> bitOffset) & blockBitMask;

								// overflow, only for 1.13-1.15
								if (packedBits && bitOffset + bitsPerBlock > 64) {
									int carryBits = bitOffset + bitsPerBlock - 64;
									int carryMask = (1 << carryBits) - 1;
									int carryShift = (bitsPerBlock - carryBits);

									paletteIndex |= (blockStatesTag.getValue()[longIndex + 1] & carryMask) << carryShift;
								}

								BlockState blockState = blockStatesPalette.get((int) paletteIndex);
								newSection.blockNames[x][y][z] = blockState.name;
								newSection.blockStates[x][y][z] = blockState.properties;
							}
						}

						if (skylightTag != null && newSection != null) {
							newSection.skylight[x][y][z] = getAnvil4Bit(skylightTag, x, y, z);
						}
						if (blocklightTag != null && newSection != null) {
							newSection.blocklight[x][y][z] = getAnvil4Bit(blocklightTag, x, y, z);
						}
					}
				}
			}
		}

		// Parse "Biomes" data (16x16) or 1.15+ (one int is biome id for 4x4x4 volume)
		ByteArrayTag biomeDataTag = NbtUtil.getChild(tag, "Biomes", ByteArrayTag.class);
		IntArrayTag intBiomeDataTag = NbtUtil.getChild(tag, "Biomes", IntArrayTag.class);

		if (biomeDataTag != null || (intBiomeDataTag != null && intBiomeDataTag.getValue().length == 256)) {
			biomes = new int[SECTION_WIDTH][SECTION_DEPTH];

			for (int x = 0; x < SECTION_WIDTH; x++) {
				for (int z = 0; z < SECTION_DEPTH; z++) {
					final int index = z * SECTION_WIDTH + x;
					if (biomeDataTag != null) {
						biomes[x][z] = biomeDataTag.getValue()[index];
					} else {
						biomes[x][z] = intBiomeDataTag.getValue()[index];
					}
				}
			}
		} else if (intBiomeDataTag != null && intBiomeDataTag.getValue().length == 1024) {
			int width = 4;
			int height = 64;
			int depth = 4;

			// 1.15+
			biomes3d = new int[width][height][depth];

			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					for (int z = 0; z < depth; z++) {
						final int index = (z * width + x) + y * SECTION_HEIGHT;
						biomes3d[x][y][z] = intBiomeDataTag.getValue()[index];
					}
				}
			}
		}
	}

	private List<BlockState> parseBlockStates(ListTag paletteTag) {
		List<BlockState> blockStatesPalette = new ArrayList<>();
		for (Tag paletteEntry : paletteTag.getValue()) {
			String name = NbtUtil.getChild((CompoundTag) paletteEntry, "Name", StringTag.class).getValue();
			BlockProperties properties = NbtUtil.getProperties(NbtUtil.getChild((CompoundTag) paletteEntry, "Properties", CompoundTag.class));
			blockStatesPalette.add(new BlockState(name, properties));
		}

		return blockStatesPalette;
	}

	private void parseMcRegionData(CompoundTag level) {
		// McRegion chunks are only 128 high, so just create the lower half of the sections
		for (int i = 0; i < 8; i++) {
			sections[i] = new Section();
		}

		ByteArrayTag blocks = NbtUtil.getChild(level, "Blocks", ByteArrayTag.class);
		if (blocks != null) {
			for (int x = 0; x < WIDTH; x++) {
				for (int y = 0; y < MC_REGION_HEIGHT; y++) {
					for (int z = 0; z < DEPTH; z++) {
						final int index = calcIndex(x, y, z);
						final byte blockId = blocks.getValue()[index];
						setBlockId(x, y, z, blockId);
					}
				}
			}
		}

		ByteArrayTag dataTag = NbtUtil.getChild(level, "Data", ByteArrayTag.class);
		if (dataTag != null) {
			for (int x = 0; x < WIDTH; x++) {
				for (int y = 0; y < MC_REGION_HEIGHT; y++) {
					for (int z = 0; z < DEPTH; z++) {
						final byte half = get4Bit(dataTag, x, y, z);
						setBlockData(x, y, z, half);
					}
				}
			}
		}

		ByteArrayTag skylightTag = NbtUtil.getChild(level, "SkyLight", ByteArrayTag.class);
		if (skylightTag != null) {
			for (int x = 0; x < WIDTH; x++) {
				for (int y = 0; y < MC_REGION_HEIGHT; y++) {
					for (int z = 0; z < DEPTH; z++) {
						final byte half = get4Bit(skylightTag, x, y, z);
						setSkyLight(x, y, z, half);
					}
				}
			}
		}

		ByteArrayTag blockLightTag = NbtUtil.getChild(level, "BlockLight", ByteArrayTag.class);
		if (blockLightTag != null) {
			for (int x = 0; x < WIDTH; x++) {
				for (int y = 0; y < MC_REGION_HEIGHT; y++) {
					for (int z = 0; z < DEPTH; z++) {
						final byte half = get4Bit(blockLightTag, x, y, z);
						setBlockLight(x, y, z, half);
					}
				}
			}
		}
	}

	private static int calcIndex(final int x, final int y, final int z) {
		// y + ( z * ChunkSizeY(=128) + ( x * ChunkSizeY(=128) * ChunkSizeZ(=16) ) ) ];
		return y + (z * MC_REGION_HEIGHT) + (x * MC_REGION_HEIGHT * DEPTH);
	}

	private static int calcAnvilIndex(final int x, final int y, final int z) {
		// Note that the old format is XZY ((x * 16 + z) * 128 + y)
		// and the new format is       YZX ((y * 16 + z) * 16 + x)
		return x + (z * SECTION_HEIGHT) + (y * SECTION_HEIGHT * SECTION_DEPTH);
	}

	private static byte getAnvil4Bit(ByteArrayTag tag, final int x, final int y, final int z) {
		final int index = calcAnvilIndex(x, y, z) / 2; //4 bit index

		// Upper or lower half?
		final boolean isUpper = x % 2 == 1;

		return getHalf(tag, index, isUpper);
	}

	private static byte get4Bit(ByteArrayTag tag, final int x, final int y, final int z) {
		final int index = calcIndex(x, y, z) / 2; //4 bit index

		// Upper or lower half?
		final boolean isUpper = y % 2 == 1;

		return getHalf(tag, index, isUpper);
	}

	private static byte getHalf(ByteArrayTag tag, int index, boolean isUpper) {
		final int doublet = tag.getValue()[index];

		byte half;
		if (isUpper) {
			half = (byte) ((doublet >> 4) & 0xF);
		} else {
			half = (byte) (doublet & 0xF);
		}

		return half;
	}

	public int getBlockId(final int x, final int y, final int z) {
		if (y < 0 || y >= RawChunk.HEIGHT || x < 0 || x > RawChunk.WIDTH || z < 0 || z > RawChunk.DEPTH)
			return 0;

		final int sectionY = y / SECTION_HEIGHT;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s != null)
			return s.blockIds[x][localY][z];
		else
			return BlockIds.AIR;
	}

	public void setBlockId(final int x, final int y, final int z, final int blockId) {
		final int sectionY = y / SECTION_HEIGHT;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s == null) {
			s = new Section();
			sections[sectionY] = s;
		}

		s.blockIds[x][localY][z] = blockId;
		s.blockNames[x][localY][z] = null;
	}

	public void setBlockData(final int x, final int y, final int z, final byte val) {
		final int sectionY = y / SECTION_HEIGHT;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s == null) {
			s = new Section();
			sections[sectionY] = s;
		}

		s.blockData[x][localY][z] = val;
	}

	public int getBlockData(final int x, final int y, final int z) {
		if (y < 0 || y >= RawChunk.HEIGHT || x < 0 || x > RawChunk.WIDTH || z < 0 || z > RawChunk.DEPTH)
			return 0;

		final int sectionY = y / SECTION_HEIGHT;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s != null && x >= 0 && x <= 15 && z >= 0 && z <= 15)  //TODO:  Fix this (workaround for painting and stair problems)
			return s.blockData[x][localY][z];
		else
			return 0;
	}

	public String getBlockName(final int x, final int y, final int z) {
		final int sectionY = y / SECTION_HEIGHT;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s != null) {
			String blockName = s.blockNames[x][localY][z];
			if (blockName != null) {
				return blockName;
			} else {
				return Block.AIR.getName();
			}
		} else {
			return Block.AIR.getName();
		}
	}

	public void setBlockName(final int x, final int y, final int z, final String blockName) {
		final int sectionY = y / SECTION_HEIGHT;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s == null) {
			s = new Section();
			sections[sectionY] = s;
		}

		s.blockNames[x][localY][z] = blockName;
	}

	public BlockProperties getBlockState(final int x, final int y, final int z) {
		if (y < 0 || y >= RawChunk.HEIGHT || x < 0 || x > RawChunk.WIDTH || z < 0 || z > RawChunk.DEPTH)
			return null;

		final int sectionY = y / SECTION_HEIGHT;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s != null)
			return s.blockStates[x][localY][z];
		else
			return null;
	}

	public void setBlockState(final int x, final int y, final int z, final BlockProperties blockState) {
		final int sectionY = y / SECTION_HEIGHT;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s == null) {
			s = new Section();
			sections[sectionY] = s;
		}

		s.blockStates[x][localY][z] = blockState;
	}

	public void setSkyLight(final int x, final int y, final int z, final byte val) {
		final int sectionY = y / SECTION_HEIGHT;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s == null) {
			s = new Section();
			sections[sectionY] = s;
		}

		s.skylight[x][localY][z] = val;
	}

	public byte getSkyLight(final int x, final int y, final int z) {
		final int sectionY = y / SECTION_HEIGHT;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s != null && x >= 0 && localY >= 0 && z >= 0)  //TODO: Fix this (workaround for painting and stair problems)
			return s.skylight[x][localY][z];
		else
			return MAX_LIGHT - 1;
	}

	public void setBlockLight(final int x, final int y, final int z, final byte val) {
		final int sectionY = y / SECTION_HEIGHT;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s == null) {
			s = new Section();
			sections[sectionY] = s;
		}

		s.blocklight[x][localY][z] = val;
	}

	public byte getBlockLight(final int x, final int y, final int z) {
		final int sectionY = y / SECTION_HEIGHT;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s != null && x >= 0 && localY >= 0 && z >= 0)  //TODO: Fix this (workaround for painting and stair problems)
			return s.blocklight[x][localY][z];
		else
			return 0;
	}

	public int getBlockIdClamped(final int x, final int y, final int z, final int defaultId) {
		if (x < 0 || x >= WIDTH)
			return defaultId;
		if (y < 0 || y >= HEIGHT)
			return defaultId;
		if (z < 0 || z >= DEPTH)
			return defaultId;

		return getBlockId(x, y, z);
	}

	public ChunkCoord getChunkCoord() {
		return new ChunkCoord(chunkX, chunkZ);
	}

	public long getMemorySize() {
		int blockIdTotal = 0;
		int skyLightTotal = 0;
		int blockLightTotal = 0;
		int blockDataTotal = 0;
		for (Section s : sections) {
			if (s != null) {
				blockIdTotal += s.blockIds.length * s.blockIds[0].length * s.blockIds[0][0].length;
				skyLightTotal += s.skylight.length * s.skylight[0].length * s.skylight[0][0].length;
				blockLightTotal += s.blocklight.length * s.blocklight[0].length * s.blocklight[0][0].length;
				blockDataTotal += s.blockData.length * s.blockData[0].length * s.blockData[0][0].length;
			}
		}

		return (long) blockIdTotal + blockDataTotal + skyLightTotal + blockLightTotal;
	}

	public Map<String, SignEntity> getSigns() {
		return Collections.unmodifiableMap(signs);
	}

	public void setSigns(Map<String, SignEntity> signs) {
		this.signs = signs;
	}

	public Map<String, FlowerPotEntity> getFlowerPots() {
		return Collections.unmodifiableMap(flowerPots);
	}

	public List<PaintingEntity> getPaintings() {
		return Collections.unmodifiableList(paintings);
	}

	public Map<String, SkullEntity> getSkulls() {
		return Collections.unmodifiableMap(skulls);
	}

	public Map<String, BeaconEntity> getBeacons() {
		return Collections.unmodifiableMap(beacons);
	}

	public Map<String, BannerEntity> getBanners() {
		return Collections.unmodifiableMap(banners);
	}

	public List<PaintingEntity> getItemFrames() {
		return Collections.unmodifiableList(itemFrames);
	}

	public List<ContainerEntity> getChests() {
		return Collections.unmodifiableList(chests);
	}

	public Map<String, BedEntity> getBeds() {
		return Collections.unmodifiableMap(beds);
	}

	public void setBeds(Map<String, BedEntity> beds) {
		this.beds = beds;
	}

	public byte[] calculateHash(MessageDigest hashAlgorithm) {
		hashAlgorithm.reset();

		for (Section s : sections) {
			if (s != null) {
				update(hashAlgorithm, s.blockIds);
				update(hashAlgorithm, s.blockData);
				update(hashAlgorithm, s.blockNames);

				update(hashAlgorithm, s.skylight);
				update(hashAlgorithm, s.blocklight);
			} else {
				byte[][][] dummy = new byte[1][1][1];
				update(hashAlgorithm, dummy);
			}
		}

		for (SignEntity sign : signs.values()) {
			hashAlgorithm.update(Integer.toString(sign.getX()).getBytes());
			hashAlgorithm.update(Integer.toString(sign.getY()).getBytes());
			hashAlgorithm.update(Integer.toString(sign.getZ()).getBytes());
			hashAlgorithm.update(sign.getText1().getBytes());
			hashAlgorithm.update(sign.getText2().getBytes());
			hashAlgorithm.update(sign.getText3().getBytes());
			hashAlgorithm.update(sign.getText4().getBytes());
		}

		return hashAlgorithm.digest();
	}

	private static void update(MessageDigest hashAlgorithm, int[][][] data) {
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[0].length; y++) {
				for (int z = 0; y < data[0][0].length; y++) {
					final int val = data[x][y][z];

					hashAlgorithm.update((byte) ((val) & 0xFF));
					hashAlgorithm.update((byte) ((val >> 8) & 0xFF));
					hashAlgorithm.update((byte) ((val >> 16) & 0xFF));
					hashAlgorithm.update((byte) ((val >> 24) & 0xFF));
				}
			}
		}
	}

	private static void update(MessageDigest hashAlgorithm, String[][][] data) {
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[0].length; y++) {
				for (int z = 0; y < data[0][0].length; y++) {
					if (data[x][y][z] != null)
						hashAlgorithm.update(data[x][y][z].getBytes());
				}
			}
		}
	}

	private static void update(MessageDigest hashAlgorithm, byte[][][] data) {
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[0].length; y++) {
				hashAlgorithm.update(data[x][y]);
			}
		}
	}

	public Biome getBiome(final int x, final int y, final int z) {
		if (biomes3d != null) { //1.16-1.17
			int xIndex = Math.floorDiv(x, 4);
			int yIndex = Math.floorDiv(y, 4);
			int zIndex = Math.floorDiv(z, 4);

			return BiomesOld.byId(biomes3d[xIndex][yIndex][zIndex]);
		} else if (biomes != null) { //1.15 and older
			return BiomesOld.byId(biomes[x][z]);
		} else { //1.18+
			final int sectionY = y / SECTION_HEIGHT;
			final int localY = y % SECTION_HEIGHT;

			Section s = sections[sectionY];
			if (s != null) {
				int xIndex = Math.floorDiv(x, 4);
				int yIndex = Math.floorDiv(localY, 4);
				int zIndex = Math.floorDiv(z, 4);

				return Biomes.byId(s.biomeIds[(yIndex * 4 + zIndex) * 4 + xIndex].substring(10));
			} else {
				return Biomes.THE_VOID;
			}
		}
	}

	private static class Section {
		public int[][][] blockIds;
		public byte[][][] blockData;
		public String[][][] blockNames;
		public BlockProperties[][][] blockStates;

		private final String[] biomeIds;

		public byte[][][] skylight;
		public byte[][][] blocklight;

		public Section() {
			blockIds = new int[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
			blockData = new byte[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
			blockNames = new String[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
			blockStates = new BlockProperties[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
			biomeIds = new String[64];

			skylight = new byte[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
			blocklight = new byte[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
		}
	}

	//TODO: for versions newer than 1.13 can we switch to using sections without the blockId and blockData arrays?
//	private static class SectionNew {
//		public int[][][] blockIds;
//		public byte[][][] blockData;
//		public String[][][] blockNames;
//		public BlockProperties[][][] blockStates;
//
//		@Getter
//		private final String[] biomeIds;
//
//		public byte[][][] skylight;
//		public byte[][][] blocklight;
//
//		public Section118() {
//			blockIds = new int[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
//			blockData = new byte[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
//			blockNames = new String[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
//			blockStates = new BlockProperties[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
//			biomeIds = new String[64];
//
//			skylight = new byte[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
//			blocklight = new byte[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
//		}
//	}

	@RequiredArgsConstructor
	private static class BlockState {
		private final String name;
		private final BlockProperties properties;
	}

	private static String textFromJSON(String rawMessage) {
		StringBuilder result = new StringBuilder();
		String searchString = "\"text\":\"";
		int pos = 0;
		int left;
		int right = 0;
		while (pos != -1) {
			pos = rawMessage.indexOf(searchString, pos);
			left = pos + 8;
			if (pos != -1) {
				int nBackslash = 0;
				// Find right delimiting ". Problem: \\\" is escaped, \\\\" is not.
				for (int i = left; i < rawMessage.length(); i++) {
					if (rawMessage.charAt(i) == '\\') {
						nBackslash++;
					} else if (rawMessage.charAt(i) == '"' && nBackslash % 2 == 0) {
						right = i;
						break;
					} else {
						nBackslash = 0;
					}
				}

				result.append(rawMessage.substring(left, right));
				pos = left;
			}
		}

		return result.toString();
	}
}
