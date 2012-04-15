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

import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;

import tectonicus.util.Vector3l;

public class LevelDat
{
	public static final int UNKNOWN_VERSION = 0;
	
	private String worldName;
	
	private int version;
	
	private Vector3l spawnPosition;
	
	private long sizeOnDisk;
	
	private Player player;
	
	public LevelDat(File datFile, String singlePlayerName) throws Exception
	{
		worldName = "";
		
		spawnPosition = new Vector3l();
		
		InputStream in = null;
		NBTInputStream nbtIn = null;
		try
		{
			in = new FileInputStream(datFile);
			nbtIn = new NBTInputStream(in);
			
			Tag tag = nbtIn.readTag();
			if (tag instanceof CompoundTag)
			{
				CompoundTag data = NbtUtil.getChild((CompoundTag)tag, "Data", CompoundTag.class);
				
				version = NbtUtil.getInt(data, "version", UNKNOWN_VERSION);
				
				IntTag spawnXTag = NbtUtil.getChild(data, "SpawnX", IntTag.class);
				IntTag spawnYTag = NbtUtil.getChild(data, "SpawnY", IntTag.class);
				IntTag spawnZTag = NbtUtil.getChild(data, "SpawnZ", IntTag.class);
				
				if (spawnXTag != null && spawnYTag != null && spawnZTag != null)
				{
					spawnPosition.x = spawnXTag.getValue();
					spawnPosition.y = spawnYTag.getValue();
					spawnPosition.z = spawnZTag.getValue();
				}
				
				sizeOnDisk = NbtUtil.getLong(data, "SizeOnDisk", 0);
				
				worldName = NbtUtil.getString(data, "LevelName", "");
				
				CompoundTag playerTag = NbtUtil.getChild((CompoundTag)data, "Player", CompoundTag.class);
				if (playerTag != null)
				{
					try
					{
						player = new Player(singlePlayerName, playerTag);
					}
					catch (Exception e)
					{
						System.err.println("Couldn't parse single player from level.dat: "+e);
						e.printStackTrace();
					}
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
	}
	
	public int getVersion()
	{
		return version;
	}
	
	public String getWorldName()
	{
		return worldName;
	}
	
	public Vector3l getSpawnPosition()
	{
		return new Vector3l(spawnPosition);
	}
	
	public long getSizeOnDisk()
	{
		return sizeOnDisk;
	}
	
	public Player getSinglePlayer()
	{
		return player;
	}
}
