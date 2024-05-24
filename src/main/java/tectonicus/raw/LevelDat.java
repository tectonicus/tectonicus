/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import tectonicus.util.Vector3l;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Getter
public class LevelDat
{
	public static final int UNKNOWN_VERSION = 0;

	private String worldName;
	private boolean alpha;
	private final Vector3l spawnPosition;
	private long sizeOnDisk;
	private Player singlePlayer;
	private String version;
	private boolean snapshot;
	private List<String> dataPacks = Collections.emptyList();
	
	public LevelDat(Path datFile, String singlePlayerName)
	{	
		spawnPosition = new Vector3l();
		
		try(InputStream in = Files.newInputStream(datFile);
			NBTInputStream nbtIn = new NBTInputStream(in))
		{
			Tag tag = nbtIn.readTag();
			if (tag instanceof CompoundTag) {
				CompoundTag data = NbtUtil.getChild((CompoundTag)tag, "Data", CompoundTag.class);
				
				Optional.ofNullable(NbtUtil.getChild(data, "DataPacks", CompoundTag.class)).ifPresent(dataPackTag -> {
					List<Tag> enabledTags = NbtUtil.getChild(dataPackTag, "Enabled", ListTag.class).getValue();
					dataPacks = enabledTags.stream().filter(t -> !t.getValue().equals("vanilla")).map(t -> (String)t.getValue()).collect(Collectors.toList());
					log.debug("Enabled data packs: {}", dataPacks);
				});

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
				
				CompoundTag playerTag = NbtUtil.getChild(data, "Player", CompoundTag.class);
				if (playerTag != null) {
					singlePlayer = new Player(singlePlayerName, playerTag);
				}

				Optional.ofNullable(NbtUtil.getChild(data, "Version", CompoundTag.class)).ifPresent(versionTag -> {
					version = NbtUtil.getString(versionTag, "Name", "");
					snapshot = NbtUtil.getByte(versionTag, "Snapshot", (byte) 0) == 1;
				});
			}
		} catch (IOException e) {
			log.error("Exception: ", e);
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
