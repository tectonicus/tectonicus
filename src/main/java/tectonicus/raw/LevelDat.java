/*
 * Copyright (c) 2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import lombok.Data;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import tectonicus.util.Vector3l;


@Data
public class LevelDat
{
	public static final int UNKNOWN_VERSION = 0;

	private String worldName;
	private boolean alpha;
	private Vector3l spawnPosition;
	private long sizeOnDisk;
	private Player player;
	private String version;
	private boolean snapshot;
	
	public LevelDat(Path datFile, String singlePlayerName)
	{	
		spawnPosition = new Vector3l();
		
		try(InputStream in = Files.newInputStream(datFile);
			NBTInputStream nbtIn = new NBTInputStream(in))
		{
			Tag tag = nbtIn.readTag();
			if (tag instanceof CompoundTag) {
				CompoundTag data = NbtUtil.getChild((CompoundTag)tag, "Data", CompoundTag.class);

				alpha = NbtUtil.getInt(data, "version", UNKNOWN_VERSION) == 0;

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

				Optional.ofNullable(NbtUtil.getChild(data, "Version", CompoundTag.class)).ifPresent(versionTag -> {
					version = NbtUtil.getString(versionTag, "Name", "");
					snapshot = NbtUtil.getByte(versionTag, "Snapshot", (byte) 0) == 1;
				});
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
}
