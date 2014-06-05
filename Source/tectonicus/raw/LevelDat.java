/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
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
