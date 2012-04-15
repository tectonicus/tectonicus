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
package tectonicus.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.jnbt.Tag;

import tectonicus.ChunkCoord;
import tectonicus.raw.NbtUtil;
import tectonicus.raw.RawChunk;

import com.google.code.minecraftbiomeextractor.WorldProcessor;

public class BiomeData
{
	private static final int VERSION = 1;
	
	private ColourCoord[][] colourCoords;
	
	public BiomeData()
	{
		colourCoords = new ColourCoord[RawChunk.WIDTH][RawChunk.DEPTH];
		
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int z=0; z<RawChunk.DEPTH; z++)
			{
				colourCoords[x][z] = new ColourCoord((byte)0, (byte)0);
			}
		}
	}
	
	public BiomeData(WorldProcessor worldProcessor, ChunkCoord coord)
	{
		colourCoords = new ColourCoord[RawChunk.WIDTH][RawChunk.DEPTH];
		
		final long baseX = coord.x * RawChunk.WIDTH;
		final long baseZ = coord.z * RawChunk.HEIGHT;
		
		for (int x=0; x<RawChunk.WIDTH; x++)
		{
			for (int z=0; z<RawChunk.DEPTH; z++)
			{
				try
				{
					// TODO: This should accept longs?
					byte[] rawCoord = worldProcessor.getCoordsAtBlock( (int)(baseX + x), (int)(baseZ + z) );
				//	final char colX = (char)( (int)rawCoord[0]&0xFF );
				//	final char colY = (char)( (int)rawCoord[1]&0xFF );
				//	ColourCoord colourCoord = new ColourCoord(colX, colY);
					ColourCoord colourCoord = new ColourCoord(rawCoord[0], rawCoord[1]);
					
					colourCoords[x][z] = colourCoord;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
			/*	// TODO: These should accept longs not ints?
				Color grassColour = worldProcessor.getColorAtBlock((int)worldX, (int)worldZ, ColourType.GrassColour);
				Color foliageColour = worldProcessor.getColorAtBlock((int)worldX, (int)worldZ, ColourType.FoliageColour);
				
				grassColours[x][z] = grassColour.getRGB();
				foliageColours[x][z] = foliageColour.getRGB();
				
				grassColours[x][z] = new Color(11, 22, 33).getRGB();
			*/
			}
		}
	}
	
	public BiomeData(File file) throws Exception
	{
		FileInputStream fIn = null;
		NBTInputStream nbtIn = null;
		
		colourCoords = new ColourCoord[RawChunk.WIDTH][RawChunk.DEPTH];
		
		try
		{
			fIn = new FileInputStream(file);
			nbtIn = new NBTInputStream(fIn);
			
			Tag tag = nbtIn.readTag();
			if (tag instanceof CompoundTag)
			{
				CompoundTag root = (CompoundTag)tag;
				
				final int version = NbtUtil.getInt(root, "version", 0);
				if (version != VERSION)
				{
					throw new IOException("Biome cache file is wrong version");
				}
				
				Tag rawCoordsTag = root.getValue().get("colourCoords");
				if (rawCoordsTag instanceof ByteArrayTag)
				{
					ByteArrayTag coordsTag = (ByteArrayTag)rawCoordsTag;
					
					int pos = 0;
					for (int x=0; x<colourCoords.length; x++)
					{
						for (int z=0; z<colourCoords[0].length; z++)
						{
						//	final char xCoord = (char)( (int)coordsTag.getValue()[pos] + 127);
						//	final char zCoord = (char)( (int)coordsTag.getValue()[pos+1] + 127);
						//	colourCoords[x][z] = new ColourCoord(xCoord, zCoord);
							
							colourCoords[x][z] = new ColourCoord(coordsTag.getValue()[pos], coordsTag.getValue()[pos+1]);
							
							pos += 2;
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (nbtIn != null)
				nbtIn.close();
			if (fIn != null)
				fIn.close();
		}
	}
	
	public ColourCoord getColourCoord(final int x, final int z)
	{
		return new ColourCoord( colourCoords[x][z] );
	}
	
	public void write(File file)
	{
		FileOutputStream fOut = null;
		NBTOutputStream nbtOut = null;
		
		try
		{
			file.getParentFile().mkdirs();
			
			fOut = new FileOutputStream(file);
			nbtOut = new NBTOutputStream(fOut);
			
			
			IntTag versionTag = new IntTag("version", VERSION);
			
			int pos = 0;
			byte[] coordBytes = new byte[colourCoords.length * colourCoords[0].length * 2];
			for (int x=0; x<colourCoords.length; x++)
			{
				for (int z=0; z<colourCoords[0].length; z++)
				{
					ColourCoord coord = colourCoords[x][z];
					
				//	final byte byte1 = (byte)((int)coord.x - 127);
				//	final byte byte2 = (byte)((int)coord.y - 127);
				//	
				//	coordBytes[pos] = byte1;
				//	coordBytes[pos+1] = byte2;
					
					coordBytes[pos] = coord.x;
					coordBytes[pos+1] = coord.y;
					
					pos += 2;
				}
			}
			
			ByteArrayTag colourCoordsTag = new ByteArrayTag("colourCoords", coordBytes);
			
			Map<String, Tag> dataTags = new HashMap<String, Tag>();
			dataTags.put(versionTag.getName(), versionTag);
			dataTags.put(colourCoordsTag.getName(), colourCoordsTag);
			
			CompoundTag data = new CompoundTag("data", dataTags);
			
			nbtOut.writeTag(data);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (nbtOut != null)
					nbtOut.close();
			}
			catch (Exception e) {}
			try
			{
				if (fOut != null)
					fOut.close();
			}
			catch (Exception e) {}
		}
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof BiomeData))
			return false;
		
		BiomeData other = (BiomeData)obj;
		
		if (this.colourCoords.length != other.colourCoords.length)
			return false;
		
		if (this.colourCoords[0].length != other.colourCoords[0].length)
			return false;
		
		for (int x=0; x<colourCoords.length; x++)
		{
			for (int z=0; z<colourCoords[0].length; z++)
			{
				ColourCoord thisCoord = this.colourCoords[x][z];
				ColourCoord otherCoord = other.colourCoords[x][z];
				
				if (!thisCoord.equals(otherCoord))
				{
					return false;
				}
			}
		}
		
		return true;
	}
	
	public static class ColourCoord
	{
		// Remember byte is [-128, +127]
		
		private byte x, y;
		
		public ColourCoord() {}
		
		public ColourCoord(final byte x, final byte y)
		{
			this.x = x;
			this.y = y;
		}
		
		public ColourCoord(ColourCoord other)
		{
			this.x = other.x;
			this.y = other.y;
		}
		
		public int getX()
		{
		//	return (x + 128);
			return x;
		}
		
		public int getY()
		{
		//	return (y + 128);
			return y;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			ColourCoord other = (ColourCoord)obj;
			
			return this.x == other.x && this.y == other.y;
		}
	}
}
