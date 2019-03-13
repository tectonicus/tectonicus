/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringEscapeUtils;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import tectonicus.BlockIds;
import tectonicus.ChunkCoord;
import tectonicus.WorldStats;
import tectonicus.blockTypes.Banner.Pattern;
import tectonicus.util.FileUtils;

public class RawChunk
{
	public static final int WIDTH = 16;
	public static final int HEIGHT = 256;
	public static final int DEPTH = 16;
	
	public static final int MC_REGION_HEIGHT = 128;
	
	public static final int SECTION_WIDTH = 16;
	public static final int SECTION_HEIGHT = 16;
	public static final int SECTION_DEPTH = 16;
	
	public static final int MAX_LIGHT = 16;
	
	private static final int MAX_SECTIONS = HEIGHT / SECTION_HEIGHT;
	
	private byte[][] biomes;
	
	private Section[] sections;
	
	private int blockX, blockY, blockZ;
	
	private Map<String, SignEntity> signs;
	private Map<String, FlowerPotEntity> flowerPots;
	private Map<String, SkullEntity> skulls;
	private Map<String, BeaconEntity> beacons;
	private Map<String, BannerEntity> banners;
	private Map<String, BedEntity> beds;
	
	private List<PaintingEntity> paintings;
	private List<PaintingEntity> itemFrames;
	private List<ContainerEntity> chests;
	
	private Map<String, Object> filterData = new HashMap<>();
	
	public RawChunk()
	{
		clear();
	}
	
	public RawChunk(File file) throws Exception
	{
		FileInputStream fileIn = new FileInputStream(file);
		init(fileIn, Compression.Gzip, null);
	}
	
	public RawChunk(InputStream in, Compression compression, WorldStats worldStats) throws Exception
	{
		init(in, compression, worldStats);
	}
	
	public void setFilterMetadata(String id, Object data)
	{
		this.filterData.put(id, data);
	}
	public void removeFilterMetadata(String id)
	{
		this.filterData.remove(id);
	}
	public Object getFilterMetadata(String id)
	{
		return this.filterData.get(id);
	}
	
	private void clear()
	{
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
	
	private void init(InputStream in, Compression compression, WorldStats worldStats) throws Exception
	{
		clear();

		NBTInputStream nbtIn = null;
		try
		{
			nbtIn = new NBTInputStream(in, compression);
			
			Tag tag = nbtIn.readTag();
			if (tag instanceof CompoundTag)
			{
				CompoundTag root = (CompoundTag)tag;
				
				CompoundTag level = NbtUtil.getChild(root, "Level", CompoundTag.class);
				if (level != null)
				{
					blockX = blockY = blockZ = 0;
					
					IntTag xPosTag = NbtUtil.getChild(level, "xPos", IntTag.class);
					if (xPosTag != null)
						blockX = xPosTag.getValue().intValue();
					
					IntTag zPosTag = NbtUtil.getChild(level, "zPos", IntTag.class);
					if (zPosTag != null)
						blockZ = zPosTag.getValue().intValue();
					
					ListTag sections = NbtUtil.getChild(level, "Sections", ListTag.class);
					if (sections != null)
					{
						// Parse as anvil format
						parseAnvilData(level, worldStats);
					}
					else
					{
						// Parse as McRegion format
						parseMcRegionData(level);
					}
					
					ListTag entitiesTag = NbtUtil.getChild(level, "Entities", ListTag.class);
					if (entitiesTag != null)
					{
						for (Tag t : entitiesTag.getValue())
						{
							if (t instanceof CompoundTag)
							{
								CompoundTag entity = (CompoundTag)t;
								
								StringTag idTag = NbtUtil.getChild(entity, "id", StringTag.class);
								boolean painting = idTag.getValue().endsWith("Painting") || idTag.getValue().equals("minecraft:painting");
								boolean itemFrame = idTag.getValue().equals("ItemFrame") || idTag.getValue().equals("minecraft:item_frame");
								if (painting || itemFrame)
								{
									IntTag xTag = NbtUtil.getChild(entity, "TileX", IntTag.class);
									IntTag yTag = NbtUtil.getChild(entity, "TileY", IntTag.class);
									IntTag zTag = NbtUtil.getChild(entity, "TileZ", IntTag.class);
									ByteTag oldDir = NbtUtil.getChild(entity, "Dir", ByteTag.class);
									ByteTag dir = NbtUtil.getChild(entity, "Direction", ByteTag.class);
									
									if (oldDir != null && dir == null){
										dir = oldDir;
									}

									boolean is18 = false;
									if (dir == null){
										dir = NbtUtil.getChild(entity, "Facing", ByteTag.class);
										is18 = true;
									}
									
									int direction = dir.getValue();  // Have to reverse 0 and 2 for the old Dir tag
									if (oldDir != null && direction == 0){
										direction = 2;
									}
									else if (oldDir != null && direction == 2){
										direction = 0;
									}
									
									int x = xTag.getValue();
									final int y = yTag.getValue();
									int z = zTag.getValue();
									
									if (is18 && direction == 0){
										z = zTag.getValue() - 1;
									}
									else if (is18 && direction == 1){
										x = xTag.getValue() + 1;
									}
									else if (is18 && direction == 2){
										z = zTag.getValue() + 1;
									}
									else if (is18 && direction == 3){
										x = xTag.getValue() - 1;
									}
									
									final int localX = x-(blockX*WIDTH);
									final int localY  = y-(blockY*HEIGHT);
									final int localZ = z-(blockZ*DEPTH);

									
									if (painting)
									{
										StringTag motiveTag = NbtUtil.getChild(entity, "Motive", StringTag.class);
										paintings.add(new PaintingEntity(x, y, z, localX, localY, localZ, motiveTag.getValue(), direction));
									}
									else if (itemFrame)
									{
										String item = "";
										Map<String, Tag> map = entity.getValue();
										CompoundTag itemTag = (CompoundTag) map.get("Item");
										if(itemTag != null)
										{
											ShortTag itemIdTag = NbtUtil.getChild(itemTag, "id", ShortTag.class);
											if (itemIdTag == null)
											{
												StringTag stringItemIdTag = NbtUtil.getChild(itemTag, "id", StringTag.class);
												item = stringItemIdTag.getValue();
											}
											else
											{
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
					
					ListTag tileEntitiesTag = NbtUtil.getChild(level, "TileEntities", ListTag.class);
					if (tileEntitiesTag != null)
					{
						for (Tag t : tileEntitiesTag.getValue())
						{
							if (t instanceof CompoundTag)
							{								
								CompoundTag entity = (CompoundTag)t;
								
								StringTag idTag = NbtUtil.getChild(entity, "id", StringTag.class);
								IntTag xTag = NbtUtil.getChild(entity, "x", IntTag.class);
								IntTag yTag = NbtUtil.getChild(entity, "y", IntTag.class);
								IntTag zTag = NbtUtil.getChild(entity, "z", IntTag.class);
								
								if (idTag != null && xTag != null && yTag != null && zTag != null)
								{
									final String id = idTag.getValue();
									
									final int x = xTag.getValue();
									final int y = yTag.getValue();
									final int z = zTag.getValue();
									
									final int localX = x-(blockX*WIDTH);
									final int localY  = y-(blockY*HEIGHT);
									final int localZ = z-(blockZ*DEPTH);
									
									if (id.equals("Sign") || id.equals("minecraft:sign"))
									{
										List<String> textLines = new ArrayList<String>();

										for (int i=1; i<=4; i++)
										{
											String text = NbtUtil.getChild(entity, "Text"+i, StringTag.class).getValue();

											if (!StringUtils.isEmpty(text) && FileUtils.isJSONValid(text))  // 1.9 sign text
											{
												textLines.add(textFromJSON(text));
											}
											else if (!StringUtils.isEmpty(text) && text.charAt(0) == '"' && text.charAt(text.length()-1) == '"' && text.length()>2) // 1.8 or older sign text
											{
												text = text.replaceAll("^\"|\"$", "");  //This removes begin and end double quotes
												text = StringEscapeUtils.unescapeJava(text);
												Gson gson = new GsonBuilder().create();
										        textLines.add(gson.toJson(text).replaceAll("^\"|\"$", ""));
											}
											else if (!StringUtils.isBlank(text)) // 1.7 or older sign text
											{
												text = text.replaceAll("^\"|\"$", "");
												Gson gson = new GsonBuilder().create();
												textLines.add(gson.toJson(text).replaceAll("^\"|\"$", ""));
											}
											else
											{
												textLines.add("");
											}
										}
										
										final int data = getBlockData(localX, localY, localZ);
										
										signs.put(createKey(localX, localY, localZ), new SignEntity(x, y, z, localX, localY, localZ,
																textLines.get(0), textLines.get(1), textLines.get(2), textLines.get(3), data) );
									}
									else if (id.equals("FlowerPot") || id.equals("minecraft:flower_pot"))
									{
										IntTag dataTag = NbtUtil.getChild(entity, "Data", IntTag.class);
										IntTag itemTag = NbtUtil.getChild(entity, "Item", IntTag.class);
										final int item;
										if(itemTag == null)
										{
											StringTag stringIdTag = NbtUtil.getChild(entity, "Item", StringTag.class);
											if (stringIdTag.getValue().equals("minecraft:sapling"))
												item = 6;
											else if (stringIdTag.getValue().equals("minecraft:red_flower"))
												item = 38;
											else
												item = 0;
										}
										else
										{
											item = itemTag.getValue();
										}
										
										flowerPots.put(createKey(localX, localY, localZ), new FlowerPotEntity(x, y, z, localX, localY, localZ, item, dataTag.getValue()));
									}
									else if (id.equals("Skull") || id.equals("minecraft:skull"))
									{
										ByteTag skullType = NbtUtil.getChild(entity, "SkullType", ByteTag.class);
										ByteTag rot = NbtUtil.getChild(entity, "Rot", ByteTag.class);
										
										StringTag nameTag = null;
										StringTag playerId = null;
										String name = "";
										String UUID = "";
										String textureURL = "";
										StringTag extraType = NbtUtil.getChild(entity, "ExtraType", StringTag.class);
										CompoundTag owner = NbtUtil.getChild(entity, "Owner", CompoundTag.class);
										if(owner != null)
										{
											nameTag = NbtUtil.getChild(owner, "Name", StringTag.class);
											name = nameTag.getValue();
											playerId = NbtUtil.getChild(owner, "Id", StringTag.class);
											UUID = playerId.getValue().replace("-", "");
											
											// Get skin URL
											CompoundTag properties = NbtUtil.getChild(owner, "Properties", CompoundTag.class);
											ListTag textures = NbtUtil.getChild(properties, "textures", ListTag.class);
											CompoundTag tex = NbtUtil.getChild(textures, 0, CompoundTag.class);
											StringTag value = NbtUtil.getChild(tex, "Value", StringTag.class);
											byte[] decoded = DatatypeConverter.parseBase64Binary(value.getValue());
								            JsonObject obj = new JsonParser().parse(new String(decoded, "UTF-8")).getAsJsonObject();
								            textureURL = obj.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
										}
										else if (extraType != null && !(extraType.getValue().equals("")))
										{
											name = UUID = extraType.getValue();
											textureURL = "http://www.minecraft.net/skin/"+extraType.getValue()+".png";
										}
										
										skulls.put(createKey(localX, localY, localZ), new SkullEntity(x, y, z, localX, localY, localZ, skullType.getValue(), rot.getValue(), name, UUID, textureURL));
									}
									else if (id.equals("Beacon") || id.equals("minecraft:beacon"))
									{
										IntTag levels = NbtUtil.getChild(entity, "Levels", IntTag.class);
										
										beacons.put(createKey(localX, localY, localZ), new BeaconEntity(x, y, z, localX, localY, localZ, levels.getValue()));
									}
									else if (id.equals("Banner") || id.equals("minecraft:banner"))
									{
										IntTag base = NbtUtil.getChild(entity, "Base", IntTag.class);
										int baseVal = 0;

										if (base != null)
											baseVal = base.getValue();

										ListTag patternList = NbtUtil.getChild(entity, "Patterns", ListTag.class);
										
										List<Pattern> patterns = new ArrayList<Pattern>();
										
										int numPatterns = 0;
										if (patternList != null)
											numPatterns = patternList.getValue().size();
										if (numPatterns > 0)
										{
											for(int i=0; i<numPatterns; i++)
											{
												CompoundTag p = NbtUtil.getChild(patternList, i, CompoundTag.class);
												StringTag pattern = NbtUtil.getChild(p, "Pattern", StringTag.class);
												IntTag color = NbtUtil.getChild(p, "Color", IntTag.class);
												patterns.add(new Pattern(pattern.getValue(), color.getValue()));
											}
										}
										banners.put(createKey(localX, localY, localZ), new BannerEntity(x, y, z, localX, localY, localZ, baseVal, patterns));
									}
									else if (id.equals("Chest") || id.equals("minecraft:chest") || id.equals("minecraft:shulker_box"))
									{
										final StringTag customName = NbtUtil.getChild(entity, "CustomName", StringTag.class);
										String name = "Chest";
										if (customName != null)
											name = customName.getValue();
										
										final StringTag lock = NbtUtil.getChild(entity, "Lock", StringTag.class);
										String lockStr = "";
										if (lock != null)
											lockStr = lock.getValue();
										
										final StringTag lootTable = NbtUtil.getChild(entity, "LootTable", StringTag.class);
										
										boolean unopenedChest = false;
										if (lootTable != null)
											unopenedChest = true;
										
										if (id.equals("Chest") || id.equals("minecraft:chest"))
										{
											chests.add(new ContainerEntity(x, y, z, localX, localY, localZ, name, lockStr, unopenedChest));
										}
//										else if (id.equals("EnderChest") || id.equals("minecraft:ender_chest"))
//										{
//											
//										}
										else if (id.equals("minecraft:shulker_box"))
										{
											
										}
									}
									else if (id.equals("minecraft:bed"))
									{
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
					
				//	LongTag lastUpdateTag =
				//		NbtUtil.getChild(level, "LastUpdate", LongTag.class);
					
				//	ByteTag terrainPopulatedTag = 
				//		NbtUtil.getChild(level, "TerrainPopulated", ByteTag.class);
				}
			}
		}
		finally
		{
			if (nbtIn != null)
				nbtIn.close();
			if (in != null)
				in.close();
		}
		
		/* Old debug: put bricks in the corner of every chunk
		for (int y=0; y<HEIGHT; y++)
		{
			if (blockIds[0][y][0] != BlockIds.AIR)
			{
				if (signs.size() > 0)
					blockIds[0][y][0] = BlockIds.DIAMOND_BLOCK;
				else
					blockIds[0][y][0] = BlockIds.BRICK;
			}
		}
		*/
	}
	
	private String createKey(int x, int y, int z)
	{
		return "x" + x + "y" + y + "z" + z;
	}
	
	private void parseAnvilData(CompoundTag level, WorldStats worldStats)
	{
		ListTag sectionsList = NbtUtil.getChild(level, "Sections", ListTag.class);
		// sections shouldn't be null here
		
		List<Tag> list = sectionsList.getValue();
		for (Tag t : list)
		{
			if (!(t instanceof CompoundTag))
				continue;
				
			CompoundTag compound = (CompoundTag)t;
			
			final int sectionY = NbtUtil.getByte(compound, "Y", (byte)0);
			
			if (sectionY < 0 || sectionY >= MAX_SECTIONS)
				continue;
			
			Section newSection = new Section();
			sections[sectionY] = newSection;
			
			ByteArrayTag blocksTag = NbtUtil.getChild(compound, "Blocks", ByteArrayTag.class);
			ByteArrayTag addTag = NbtUtil.getChild(compound, "Add", ByteArrayTag.class);
			ByteArrayTag dataTag = NbtUtil.getChild(compound, "Data", ByteArrayTag.class);
			ByteArrayTag skylightTag = NbtUtil.getChild(compound, "SkyLight", ByteArrayTag.class);
			ByteArrayTag blocklightTag = NbtUtil.getChild(compound, "BlockLight", ByteArrayTag.class);

			// 1.13+ block data
			LongArrayTag blockStatesTag = NbtUtil.getChild(compound, "BlockStates", LongArrayTag.class);
			ListTag paletteTag = NbtUtil.getChild(compound, "Palette", ListTag.class);
			int bitsPerBlock = 0;
			int blockBitMask = 0;
			List<Tag> paletteList = null;

			if (blockStatesTag != null)
			{
				bitsPerBlock = blockStatesTag.getValue().length * 64 / (SECTION_WIDTH * SECTION_HEIGHT * SECTION_DEPTH);
				blockBitMask = (1 << bitsPerBlock) - 1;

				paletteList = paletteTag.getValue();
			}

			for (int x=0; x<SECTION_WIDTH; x++)
			{
				for (int y=0; y<SECTION_HEIGHT; y++)
				{
					for (int z=0; z<SECTION_DEPTH; z++)
					{
						final int index = calcAnvilIndex(x, y, z);

						if (blocksTag != null)
						{
							int id = blocksTag.getValue()[index] & 0xFF;
							newSection.blockIds[x][y][z] = id;

							if (addTag != null)
							{
								id = id | (getAnvil4Bit(addTag, x, y, z) << 8);
								newSection.blockIds[x][y][z] = id;
							}

							final byte data = getAnvil4Bit(dataTag, x, y, z);
							newSection.blockData[x][y][z] = data;

							if (worldStats != null)
								worldStats.incBlockId(id, data);
						}
						else
						{
							// 1.13+ format
							int bitIndex = index * bitsPerBlock;
							int longIndex = bitIndex / 64;
							int bitOffset = bitIndex % 64;

							long paletteIndex = (blockStatesTag.getValue()[longIndex] >>> bitOffset) & blockBitMask;

							// overflow
							if (bitOffset + bitsPerBlock > 64)
							{
								int carryBits = bitOffset + bitsPerBlock - 64;
								int carryMask = (1 << carryBits) - 1;
								int carryShift = (bitsPerBlock - carryBits);

								paletteIndex |= (blockStatesTag.getValue()[longIndex + 1] & carryMask) << carryShift;
							}

							CompoundTag paletteEntry = (CompoundTag)paletteList.get((int)paletteIndex);

							String blockName = NbtUtil.getChild(paletteEntry, "Name", StringTag.class).getValue();
							CompoundTag properties = NbtUtil.getChild(paletteEntry, "Properties", CompoundTag.class);
							BlockProperties blockState = NbtUtil.getProperties(properties);

							newSection.blockNames[x][y][z] = blockName;
							newSection.blockStates[x][y][z] = blockState;
						}

						newSection.skylight[x][y][z] = getAnvil4Bit(skylightTag, x, y, z);
						newSection.blocklight[x][y][z] = getAnvil4Bit(blocklightTag, x, y, z);
					}
				}
			}
		}
		
		// Parse "Biomes" data (16x16)
		ByteArrayTag biomeDataTag = NbtUtil.getChild(level, "Biomes", ByteArrayTag.class);
		IntArrayTag intBiomeDataTag = NbtUtil.getChild(level, "Biomes", IntArrayTag.class);

		if (biomeDataTag != null)
		{
			biomes = new byte[SECTION_WIDTH][SECTION_DEPTH];
			
			for (int x=0; x<SECTION_WIDTH; x++)
			{
				for (int z=0; z<SECTION_DEPTH; z++)
				{
					final int index = x * SECTION_WIDTH + z;
					biomes[x][z] = biomeDataTag.getValue()[index];
				}
			}
		}
		else if (intBiomeDataTag != null && intBiomeDataTag.getValue().length > 0)
		{
			// 1.13+
			biomes = new byte[SECTION_WIDTH][SECTION_DEPTH]; //TODO: use int

			for (int x=0; x<SECTION_WIDTH; x++)
			{
				for (int z=0; z<SECTION_DEPTH; z++)
				{
					final int index = x * SECTION_WIDTH + z;
					biomes[x][z] = (byte)intBiomeDataTag.getValue()[index];
				}
			}
		}
	}
	
	private void parseMcRegionData(CompoundTag level)
	{
		// McRegion chunks are only 128 high, so just create the lower half of the sections
		for (int i=0; i<8; i++)
		{
			sections[i] = new Section();
		}
		
		ByteArrayTag blocks = NbtUtil.getChild(level, "Blocks", ByteArrayTag.class);
		if (blocks != null)
		{
			for (int x=0; x<WIDTH; x++)
			{
				for (int y=0; y<MC_REGION_HEIGHT; y++)
				{
					for (int z=0; z<DEPTH; z++)
					{
						final int index = calcIndex(x, y, z);
						final byte blockId = blocks.getValue()[index];
						setBlockId(x, y, z, blockId);
					}
				}
			}
		}
		
		ByteArrayTag dataTag = NbtUtil.getChild(level, "Data", ByteArrayTag.class);
		if (dataTag != null)
		{
			for (int x=0; x<WIDTH; x++)
			{
				for (int y=0; y<MC_REGION_HEIGHT; y++)
				{
					for (int z=0; z<DEPTH; z++)
					{
						final byte half = get4Bit(dataTag, x, y, z);
						setBlockData(x, y, z, half);
					}
				}
			}
		}
		
		ByteArrayTag skylightTag = NbtUtil.getChild(level, "SkyLight", ByteArrayTag.class);
		if (skylightTag != null)
		{
			for (int x=0; x<WIDTH; x++)
			{
				for (int y=0; y<MC_REGION_HEIGHT; y++)
				{
					for (int z=0; z<DEPTH; z++)
					{
						final byte half = get4Bit(skylightTag, x, y, z);
						setSkyLight(x, y, z, half);
					}
				}
			}
		}
		
		ByteArrayTag blockLightTag = NbtUtil.getChild(level, "BlockLight", ByteArrayTag.class);
		if (blockLightTag != null)
		{
			for (int x=0; x<WIDTH; x++)
			{
				for (int y=0; y<MC_REGION_HEIGHT; y++)
				{
					for (int z=0; z<DEPTH; z++)
					{
						final byte half = get4Bit(blockLightTag, x, y, z);
						setBlockLight(x, y, z, half);
					}
				}
			}
		}
	}
	
	private static final int calcIndex(final int x, final int y, final int z)
	{
		// y + ( z * ChunkSizeY(=128) + ( x * ChunkSizeY(=128) * ChunkSizeZ(=16) ) ) ];
		return y + (z * MC_REGION_HEIGHT) + (x * MC_REGION_HEIGHT * DEPTH);
	}
	
	private static final int calcAnvilIndex(final int x, final int y, final int z)
	{
		// Note that the old format is XZY ((x * 16 + z) * 128 + y)
		// and the new format is       YZX ((y * 16 + z) * 16 + x)
		
		return x + (z * SECTION_HEIGHT) + (y * SECTION_HEIGHT * SECTION_DEPTH);
	}
	
	private static final int calc4BitIndex(final int x, final int y, final int z)
	{
		// Math.floor is bloody slow!
		// Since calcIndex should always be +ive, we can just cast to int and get the same result 
		return (int)(calcIndex(x, y, z) / 2);
	}
	
	private static final int calcAnvil4BitIndex(final int x, final int y, final int z)
	{
		// Math.floor is bloody slow!
		// Since calcIndex should always be +ive, we can just cast to int and get the same result 
		return (int)(calcAnvilIndex(x, y, z) / 2);
	}
	
	private static byte getAnvil4Bit(ByteArrayTag tag, final int x, final int y, final int z)
	{
		final int index = calcAnvil4BitIndex(x, y, z);
		if (index == 2048)
			System.out.println();;
		
		final int doublet = tag.getValue()[index];
		
		// Upper or lower half?
		final boolean isUpper = (x % 2 == 1);
		
		byte half;
		if (isUpper)
		{
			half = (byte)((doublet >> 4) & 0xF);
		}
		else
		{
			half = (byte)(doublet & 0xF);
		}
		
		return half;
	}
	
	private static byte get4Bit(ByteArrayTag tag, final int x, final int y, final int z)
	{
		final int index = calc4BitIndex(x, y, z);
		final int doublet = tag.getValue()[index];
		
		// Upper or lower half?
		final boolean isUpper = (y % 2 == 1);
		
		byte half;
		if (isUpper)
		{
			half = (byte)((doublet >> 4) & 0xF);
		}
		else
		{
			half = (byte)(doublet & 0xF);
		}
		
		return half;
	}
	
	public int getBlockId(final int x, final int y, final int z)
	{
		if (y < 0 || y >= RawChunk.HEIGHT || x < 0 || x > RawChunk.WIDTH || z < 0 || z > RawChunk.DEPTH)
			return 0;
		
		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;
		
		Section s = sections[sectionY];
		if (s != null)
			return s.blockIds[x][localY][z];
		else
			return BlockIds.AIR;
	}
	
	public void setBlockId(final int x, final int y, final int z, final int blockId)
	{
		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;
		
		Section s = sections[sectionY];
		if (s == null)
		{
			s = new Section();
			sections[sectionY] = s;
		}
		
		s.blockIds[x][localY][z] = blockId;
		s.blockNames[x][localY][z] = null;
	}
	
	public void setBlockData(final int x, final int y, final int z, final byte val)
	{
		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;
		
		Section s = sections[sectionY];
		if (s == null)
		{
			s = new Section();
			sections[sectionY] = s;
		}
		
		s.blockData[x][localY][z] = val;
	}
	
	public int getBlockData(final int x, final int y, final int z)
	{
		if (y < 0 || y >= RawChunk.HEIGHT || x < 0 || x > RawChunk.WIDTH || z < 0 || z > RawChunk.DEPTH)
			return 0;
		
		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;
		
		Section s = sections[sectionY];
		if (s != null && x >= 0 && x <= 15 && z >= 0 && z <= 15)  //TODO:  Fix this (workaround for painting and stair problems)
			return s.blockData[x][localY][z];
		else
			return 0;
	}

	public String getBlockName(final int x, final int y, final int z)
	{
		if (y < 0 || y >= RawChunk.HEIGHT || x < 0 || x > RawChunk.WIDTH || z < 0 || z > RawChunk.DEPTH)
			return null;

		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s != null)
			return s.blockNames[x][localY][z];
		else
			return null;
	}

	public void setBlockName(final int x, final int y, final int z, final String blockName)
	{
		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s == null)
		{
			s = new Section();
			sections[sectionY] = s;
		}

		s.blockNames[x][localY][z] = blockName;
	}

	public Map<String, String> getBlockState(final int x, final int y, final int z)
	{
		if (y < 0 || y >= RawChunk.HEIGHT || x < 0 || x > RawChunk.WIDTH || z < 0 || z > RawChunk.DEPTH)
			return null;

		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s != null)
			return s.blockStates[x][localY][z].getProperties();
		else
			return null;
	}

	public void setBlockState(final int x, final int y, final int z, final BlockProperties blockState)
	{
		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;

		Section s = sections[sectionY];
		if (s == null)
		{
			s = new Section();
			sections[sectionY] = s;
		}

		s.blockStates[x][localY][z] = blockState;
	}

	public void setSkyLight(final int x, final int y, final int z, final byte val)
	{
		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;
		
		Section s = sections[sectionY];
		if (s == null)
		{
			s = new Section();
			sections[sectionY] = s;
		}
		
		s.skylight[x][localY][z] = val;
	}
	
	public byte getSkyLight(final int x, final int y, final int z)
	{
		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;
		
		Section s = sections[sectionY];
		if (s != null && x >= 0 && localY >= 0 && z >= 0)  //TODO: Fix this (workaround for painting and stair problems)
			return s.skylight[x][localY][z];
		else
			return MAX_LIGHT-1;
	}
	
	public void setBlockLight(final int x, final int y, final int z, final byte val)
	{
		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;
		
		Section s = sections[sectionY];
		if (s == null)
		{
			s = new Section();
			sections[sectionY] = s;
		}
		
		s.blocklight[x][localY][z] = val;
	}
	
	public byte getBlockLight(final int x, final int y, final int z)
	{
		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;
		
		Section s = sections[sectionY];
		if (s != null && x >= 0 && localY >= 0 && z >= 0)  //TODO: Fix this (workaround for painting and stair problems)
			return s.blocklight[x][localY][z];
		else
			return 0;
	}
	
	public int getBlockIdClamped(final int x, final int y, final int z, final int defaultId)
	{
		if (x < 0 || x >= WIDTH)
			return defaultId;
		if (y < 0 || y >= HEIGHT)
			return defaultId;
		if (z < 0 || z >= DEPTH)
			return defaultId;
		
		return getBlockId(x, y, z);
	}
	
	public int getBlockX() { return blockX; }
	public int getBlockY() { return blockY; }
	public int getBlockZ() { return blockZ; }
	
	public ChunkCoord getChunkCoord() { return new ChunkCoord(blockX, blockZ); }
	
	public long getMemorySize()
	{
		int blockIdTotal = 0;
		int skyLightTotal = 0;
		int blockLightTotal = 0;
		int blockDataTotal = 0;
		for (Section s : sections)
		{
			if (s != null)
			{
				blockIdTotal += s.blockIds.length * s.blockIds[0].length * s.blockIds[0][0].length;
				skyLightTotal += s.skylight.length * s.skylight[0].length * s.skylight[0][0].length;
				blockLightTotal += s.blocklight.length * s.blocklight[0].length * s.blocklight[0][0].length;
				blockDataTotal += s.blockData.length * s.blockData[0].length * s.blockData[0][0].length;
			}
		}
		
		return blockIdTotal + blockDataTotal + skyLightTotal + blockLightTotal;
	}

	public Map<String, SignEntity> getSigns()
	{
		return Collections.unmodifiableMap(signs);
	}
	
	public void setSigns(Map<String, SignEntity> signs)
	{
		this.signs = signs;
	}
	
	public Map<String, FlowerPotEntity> getFlowerPots()
	{
		return Collections.unmodifiableMap(flowerPots);
	}
	
	public List<PaintingEntity> getPaintings()
	{
		return Collections.unmodifiableList(paintings);
	}
	
	public Map<String, SkullEntity> getSkulls()
	{
		return Collections.unmodifiableMap(skulls);
	}
	
	public Map<String, BeaconEntity> getBeacons()
	{
		return Collections.unmodifiableMap(beacons);
	}
	
	public Map<String, BannerEntity> getBanners()
	{
		return Collections.unmodifiableMap(banners);
	}
	
	public List<PaintingEntity> getItemFrames()
	{
		return Collections.unmodifiableList(itemFrames);
	}
	
	public List<ContainerEntity> getChests()
	{
		return Collections.unmodifiableList(chests);
	}
	
	public Map<String, BedEntity> getBeds()
	{
		return Collections.unmodifiableMap(beds);
	}
	
	public void setBeds(Map<String, BedEntity> beds)
	{
		this.beds = beds;
	}

	public byte[] calculateHash(MessageDigest hashAlgorithm)
	{
		hashAlgorithm.reset();
		
		for (Section s : sections)
		{
			if (s != null)
			{
				update(hashAlgorithm, s.blockIds);
				update(hashAlgorithm, s.blockData);
				update(hashAlgorithm, s.blockNames);

				update(hashAlgorithm, s.skylight);
				update(hashAlgorithm, s.blocklight);
			}
			else
			{
				byte[][][] dummy = new byte[1][1][1];
				update(hashAlgorithm, dummy);
			}
		}
		
		for (String key : signs.keySet())
		{
			SignEntity sign = signs.get(key);
			
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
	
	private static void update(MessageDigest hashAlgorithm, int[][][] data)
	{
		for (int x=0; x<data.length; x++)
		{
			for (int y=0; y<data[0].length; y++)
			{
				for (int z=0; y<data[0][0].length; y++)
				{
					final int val = data[x][y][z];
					
					hashAlgorithm.update((byte)((val)       & 0xFF));
					hashAlgorithm.update((byte)((val >>  8) & 0xFF));
					hashAlgorithm.update((byte)((val >> 16) & 0xFF));
					hashAlgorithm.update((byte)((val >> 24) & 0xFF));
				}
			}
		}
	}

	private static void update(MessageDigest hashAlgorithm, String[][][] data)
	{
		for (int x=0; x<data.length; x++)
		{
			for (int y=0; y<data[0].length; y++)
			{
				for (int z=0; y<data[0][0].length; y++)
				{
				    if (data[x][y][z] != null)
					    hashAlgorithm.update(data[x][y][z].getBytes());
				}
			}
		}
	}

	private static void update(MessageDigest hashAlgorithm, byte[][][] data)
	{
		for (int x=0; x<data.length; x++)
		{
			for (int y=0; y<data[0].length; y++)
			{
				hashAlgorithm.update(data[x][y]);
			}
		}
	}
	
	public int getBiomeId(final int x, final int y, final int z)
	{
		if(biomes != null)
			return biomes[x][z];
		else
			return BiomeIds.UNKNOWN;
	}
	
	private static class Section
	{
		public int[][][] blockIds;
		public byte[][][] blockData;
		public String[][][] blockNames;
		public BlockProperties[][][] blockStates;

		public byte[][][] skylight;
		public byte[][][] blocklight;
		
		public Section()
		{
			blockIds = new int[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
			blockData = new byte[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
			blockNames = new String[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
			blockStates = new BlockProperties[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];

			skylight = new byte[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
			blocklight = new byte[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
		}
	}
	
	private static String textFromJSON(String rawMessage){
		String result="";
		String searchString = "\"text\":\"";
		int pos=0;
		int left=0;
		int right=0;
		while(pos != -1){
			pos=rawMessage.indexOf(searchString, pos);
			left=pos+8;
			if(pos != -1){
				int nBackslash=0;
				// Find right delimiting ". Problem: \\\" is escaped, \\\\" is not.
				for(int i=left; i<rawMessage.length(); i++){ 
					if(rawMessage.charAt(i)=='\\'){
						nBackslash++;
					}
					else if(rawMessage.charAt(i)=='"' && nBackslash % 2 == 0){
						right=i;
						break;
					}
					else{
						nBackslash=0;
					}
	
				}

				result=result+rawMessage.substring(left, right);
				pos=left;
			}
			
		}
		
		
		return result;
	}

}
