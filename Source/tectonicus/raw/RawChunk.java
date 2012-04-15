/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package tectonicus.raw;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTInputStream.Compression;
import org.jnbt.StringTag;
import org.jnbt.Tag;

import tectonicus.BlockIds;
import tectonicus.ChunkCoord;

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
	
	private ArrayList<RawSign> signs;
	
	private Map<String, Object> filterData = new HashMap<String, Object>();
	
	public RawChunk()
	{
		clear();
	}
	
	public RawChunk(File file) throws Exception
	{
		FileInputStream fileIn = new FileInputStream(file);
		init(fileIn, Compression.Gzip);
	}
	
	public RawChunk(InputStream in, Compression compression) throws Exception
	{
		init(in, compression);
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
		signs = new ArrayList<RawSign>();
		
		sections = new Section[MAX_SECTIONS];
	}
	
	private void init(InputStream in, Compression compression) throws Exception
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
						parseAnvilData(level);
					}
					else
					{
						// Parse as McRegion format
						parseMcRegionData(level);
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
									String id = idTag.getValue();
									if (id.equals("Sign"))
									{
										StringTag text1Tag = NbtUtil.getChild(entity, "Text1", StringTag.class);
										StringTag text2Tag = NbtUtil.getChild(entity, "Text2", StringTag.class);
										StringTag text3Tag = NbtUtil.getChild(entity, "Text3", StringTag.class);
										StringTag text4Tag = NbtUtil.getChild(entity, "Text4", StringTag.class);

										String text1 = text1Tag.getValue();
										if (text1 == null)
											text1 = "";
										
										String text2 = text2Tag.getValue();
										if (text2 == null)
											text2 = "";
										
										String text3 = text3Tag.getValue();
										if (text3 == null)
											text3 = "";
										
										String text4 = text4Tag.getValue();
										if (text4 == null)
											text4 = "";
										
										final int x = xTag.getValue();
										final int y = yTag.getValue();
										final int z = zTag.getValue();
										
										final int localX = x-(blockX*WIDTH);
										final int localY  = y-(blockY*HEIGHT);
										final int localZ = z-(blockZ*DEPTH);
										
										final int blockId = getBlockId(localX, localY, localZ);
										final int data = getBlockData(localX, localY, localZ);
										
										signs.add( new RawSign( blockId, data,
																x, y, z,
																localX, localY, localZ,
																text1, text2, text3, text4) );
									}
								//	else if (id.equals("Furnace"))
								//	{
								//		
								//	}
								//	else if (id.equals("MobSpawner"))
								//	{
								//		
								//	}
								//	else if (id.equals("Chest"))
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
	
	
	private void parseAnvilData(CompoundTag level)
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
			if (blocksTag != null)
			{
				for (int x=0; x<SECTION_WIDTH; x++)
				{
					for (int y=0; y<SECTION_HEIGHT; y++)
					{
						for (int z=0; z<SECTION_DEPTH; z++)
						{
							final int index = calcAnvilIndex(x, y, z);
							newSection.blockIds[x][y][z] = blocksTag.getValue()[index];
						}
					}
				}
			}
			
			ByteArrayTag addTag = NbtUtil.getChild(compound, "Add", ByteArrayTag.class);
			if (addTag != null)
			{
				for (int x=0; x<SECTION_WIDTH; x++)
				{
					for (int y=0; y<SECTION_HEIGHT; y++)
					{
						for (int z=0; z<SECTION_DEPTH; z++)
						{
							final int index = calcAnvilIndex(x, y, z);
							final int addValue = addTag.getValue()[index];
							newSection.blockIds[x][y][z] = newSection.blockIds[x][y][z] + (addValue << 8);
						}
					}
				}
			}
			
			ByteArrayTag dataTag = NbtUtil.getChild(compound, "Data", ByteArrayTag.class);
			if (dataTag != null)
			{
				for (int x=0; x<SECTION_WIDTH; x++)
				{
					for (int y=0; y<SECTION_HEIGHT; y++)
					{
						for (int z=0; z<SECTION_DEPTH; z++)
						{
							final byte half = getAnvil4Bit(dataTag, x, y, z);
							newSection.blockData[x][y][z] = half;
						}
					}
				}
			}
			
			ByteArrayTag skylightTag = NbtUtil.getChild(compound, "SkyLight", ByteArrayTag.class);
			if (skylightTag != null)
			{
				for (int x=0; x<SECTION_WIDTH; x++)
				{
					for (int y=0; y<SECTION_HEIGHT; y++)
					{
						for (int z=0; z<SECTION_DEPTH; z++)
						{
							final byte half = getAnvil4Bit(skylightTag, x, y, z);
							newSection.skylight[x][y][z] = half;
						}
					}
				}
			}
			
			ByteArrayTag blocklightTag = NbtUtil.getChild(compound, "BlockLight", ByteArrayTag.class);
			if (blocklightTag != null)
			{
				for (int x=0; x<SECTION_WIDTH; x++)
				{
					for (int y=0; y<SECTION_HEIGHT; y++)
					{
						for (int z=0; z<SECTION_DEPTH; z++)
						{
							final byte half = getAnvil4Bit(blocklightTag, x, y, z);
							newSection.blocklight[x][y][z] = half;
						}
					}
				}
			}
		}
		
		// Parse "Biomes" data (16x16)
		ByteArrayTag biomeDataTag = NbtUtil.getChild(level, "Biomes", ByteArrayTag.class);
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
	}
	
	private void setBlockData(final int x, final int y, final int z, final byte val)
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
		final int sectionY = y / MAX_SECTIONS;
		final int localY = y % SECTION_HEIGHT;
		
		Section s = sections[sectionY];
		if (s != null)
			return s.blockData[x][localY][z];
		else
			return 0;
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
		if (s != null)
			return s.skylight[x][localY][z];
		else
			return MAX_LIGHT-1;
	}
	
	private void setBlockLight(final int x, final int y, final int z, final byte val)
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
		if (s != null)
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

	public ArrayList<RawSign> getSigns()
	{
		return new ArrayList<RawSign>(signs);
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
				update(hashAlgorithm, s.skylight);
				update(hashAlgorithm, s.blocklight);
			}
			else
			{
				byte[][][] dummy = new byte[1][1][1];
				update(hashAlgorithm, dummy);
			}
		}
		
		for (RawSign sign : signs)
		{
			hashAlgorithm.update(Integer.toString(sign.x).getBytes());
			hashAlgorithm.update(Integer.toString(sign.y).getBytes());
			hashAlgorithm.update(Integer.toString(sign.z).getBytes());
			hashAlgorithm.update(sign.text1.getBytes());
			hashAlgorithm.update(sign.text2.getBytes());
			hashAlgorithm.update(sign.text3.getBytes());
			hashAlgorithm.update(sign.text4.getBytes());
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
		return biomes[x][z];
	}
	
	private static class Section
	{
		public int[][][] blockIds;
		public byte[][][] blockData;
		
		public byte[][][] skylight;
		public byte[][][] blocklight;
		
		public Section()
		{
			blockIds = new int[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
			blockData = new byte[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
			
			skylight = new byte[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
			blocklight = new byte[SECTION_WIDTH][SECTION_HEIGHT][SECTION_DEPTH];
		}
	}

}
