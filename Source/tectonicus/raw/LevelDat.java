/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import tectonicus.util.Vector3l;
import xyz.nickr.nbt.NBTCodec;
import xyz.nickr.nbt.NBTCompression;
import xyz.nickr.nbt.tags.CompoundTag;

public class LevelDat
{
	private String worldName = "";
	
	private int version = 0;
	
	private Vector3l spawnPosition;
	
	private long sizeOnDisk;
	
	private Player player;
	
	public LevelDat(Path datFile, String singlePlayerName) throws Exception
	{	
		spawnPosition = new Vector3l();
		
		try(InputStream in = Files.newInputStream(datFile))
		{			
			NBTCodec codec = new NBTCodec(ByteOrder.BIG_ENDIAN);
			CompoundTag tag = codec.decode(in, NBTCompression.GZIP).getAsCompoundTag();
			
			CompoundTag data = tag.getAsCompoundTag("Data");

			version = data.getAsNumber("version").intValue();

			spawnPosition.x = data.getAsNumber("SpawnX").intValue();
			spawnPosition.y = data.getAsNumber("SpawnY").intValue();
			spawnPosition.z = data.getAsNumber("SpawnZ").intValue();
			
			sizeOnDisk = data.getAsNumber("SizeOnDisk").longValue();
			
			worldName = data.getAsString("LevelName");
			
			CompoundTag playerTag = data.getAsCompoundTag("Player");
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
	
	public void setSpawnPosition(long x, long y, long z)
	{
		spawnPosition.x = x;
		if (y != 0)
			spawnPosition.y = y;
		spawnPosition.z = z;
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
