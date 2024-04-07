/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.DoubleTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.NBTInputStream;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import tectonicus.configuration.Dimension;
import tectonicus.util.FileUtils;
import tectonicus.util.Vector3d;
import tectonicus.util.Vector3l;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;

@Log4j2
@Getter
public class Player
{
	@Setter
	private String name;
	private String UUID;
	@Setter
	private String skinURL;
	private Dimension dimension;
	private Vector3d position;
	/** Caution - may be null if the player hasn't built a bed yet! */
	private Vector3l spawnPosition;
	private Dimension spawnDimension;
	private float health; // 0-20
	private int food; // 0-20
	private int air; // 0-300
	private int xpLevel;
	private int xpTotal;
	
	private List<Item> inventory;

	public static final int MAX_HEALTH = 20;
	public static final int MAX_AIR = 300;
	private static final ObjectReader OBJECT_READER = FileUtils.getOBJECT_MAPPER().reader();
	
	public Player(Path playerFile) throws Exception
	{
		log.debug("Loading raw player from {}", playerFile);
		
		dimension = Dimension.OVERWORLD;
		spawnDimension = Dimension.OVERWORLD;
		position = new Vector3d();
		inventory = new ArrayList<>();
		
		UUID = playerFile.getFileName().toString();
		
		final int dotPos = UUID.lastIndexOf('.');
		if (UUID.contains("-"))
		{
			UUID = UUID.substring(0, dotPos).replace("-", "");
		}
		else
		{
			name = UUID = UUID.substring(0, dotPos);
		}
		
		skinURL = null;

		try(InputStream in = Files.newInputStream(playerFile); NBTInputStream nbtIn = new NBTInputStream(in))
		{
			Tag tag = nbtIn.readTag();
			if (tag instanceof CompoundTag)
			{
				CompoundTag root = (CompoundTag)tag;
				parse(root);
			}
		}
	}
	
	public Player(String playerName, CompoundTag tag)
	{
		this.name = playerName;

		parse(tag);
	}
	
	public Player(String name, String UUID, String skinURL)
	{
		this.name = name;
		this.UUID = UUID;
		this.skinURL = skinURL;
	}
	
	private void parse(CompoundTag root)
	{
		dimension = Dimension.OVERWORLD;
		position = new Vector3d();
		inventory = new ArrayList<>();

		ShortTag healthTag =  NbtUtil.getChild(root, "Health", ShortTag.class);
		if (healthTag != null) {
			health = healthTag.getValue();
		} else { // Health switched to FloatTag in MC 1.9
			health = NbtUtil.getFloat(root, "Health", 0);
		}
		air = NbtUtil.getShort(root, "Air", (short)0);
		food = NbtUtil.getInt(root, "foodLevel", 0);

		final int dimensionVal = NbtUtil.getInt(root, "Dimension", 0);
		if (dimensionVal == 0)
			dimension = Dimension.OVERWORLD;
		else if (dimensionVal == 1)
			dimension = Dimension.END;
		else if (dimensionVal == -1)
			dimension = Dimension.NETHER;

		ListTag posList = NbtUtil.getChild(root, "Pos", ListTag.class);
		if (posList != null)
		{
			DoubleTag xTag = NbtUtil.getChild(posList, 0, DoubleTag.class);
			DoubleTag yTag = NbtUtil.getChild(posList, 1, DoubleTag.class);
			DoubleTag zTag = NbtUtil.getChild(posList, 2, DoubleTag.class);

			if (xTag != null && yTag != null && zTag != null)
			{
				position.set(xTag.getValue(), yTag.getValue(), zTag.getValue());
			}
		}

		IntTag spawnXTag = NbtUtil.getChild(root, "SpawnX", IntTag.class);
		IntTag spawnYTag = NbtUtil.getChild(root, "SpawnY", IntTag.class);
		IntTag spawnZTag = NbtUtil.getChild(root, "SpawnZ", IntTag.class);
		if (spawnXTag != null && spawnYTag != null && spawnZTag != null)
		{
			spawnPosition = new Vector3l(spawnXTag.getValue(), spawnYTag.getValue(), spawnZTag.getValue());
		}

		StringTag spawnDimensionTag = NbtUtil.getChild(root, "SpawnDimension", StringTag.class);
		if (spawnDimensionTag != null) {
			spawnDimension = Dimension.byId(spawnDimensionTag.getValue());
		}

		xpLevel = NbtUtil.getInt(root, "XpLevel", 0);
		xpTotal = NbtUtil.getInt(root, "XpTotal", 0);
		
		// Parse inventory items (both inventory items and worn items)
		ListTag inventoryList = NbtUtil.getChild(root, "Inventory", ListTag.class);
		if (inventoryList != null)
		{
			for (Tag t : inventoryList.getValue())
			{
				if (t instanceof CompoundTag)
				{
					CompoundTag itemTag = (CompoundTag)t;

					StringTag idTag = NbtUtil.getChild(itemTag, "id", StringTag.class);
					ShortTag damageTag = NbtUtil.getChild(itemTag, "Damage", ShortTag.class);
					ByteTag countTag = NbtUtil.getChild(itemTag, "Count", ByteTag.class);
					ByteTag slotTag = NbtUtil.getChild(itemTag, "Slot", ByteTag.class);

					if (idTag != null && damageTag != null && countTag != null && slotTag != null)
					{
						inventory.add( new Item(idTag.getValue(), damageTag.getValue(), countTag.getValue(), slotTag.getValue(), null) );
					}
				}
			}
		}
	}


	public class RequestPlayerInfoTask implements Callable<Void>
	{
		@Override
		public Void call() throws Exception
		{
			if (Player.this.getUUID().equals(Player.this.getName()))
			{
				Player.this.setSkinURL("http://www.minecraft.net/skin/"+Player.this.getName()+".png");
			}
			else
			{
				String urlString = "https://sessionserver.mojang.com/session/minecraft/profile/"+Player.this.getUUID();
				URL url = new URL(urlString);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.addRequestProperty("Content-Type", "application/json");
				connection.setReadTimeout(15*1000);
				connection.connect();
				int responseCode = connection.getResponseCode();
				if (responseCode == 204)
					log.error("ERROR: Unrecognized UUID");
				else if (responseCode == 429) //Is this error still necessary?  It doesn't seem to occur anymore
					log.error("ERROR: Too many requests. You are only allowed to contact the Mojang session server once per minute per player.  Wait for a minute and try again.");

				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder builder = new StringBuilder();
				
				String line;
				while ((line = reader.readLine()) != null)
				{
					builder.append(line).append("\n");
				}
				reader.close();

				JsonNode node = OBJECT_READER.readTree(builder.toString());
				Player.this.setName(node.get("name").asText());
				JsonNode textures = node.get("properties").get(0);
				byte[] decoded = Base64.getDecoder().decode(textures.get("value").asText());
				node = OBJECT_READER.readTree(new String(decoded, StandardCharsets.UTF_8));
				boolean hasSkin = node.get("textures").has("SKIN");
				String textureUrl = null;
				if (hasSkin)
					textureUrl = node.get("textures").get("SKIN").get("url").asText();
				Player.this.setSkinURL(textureUrl);
			}
			log.debug("Loaded " + Player.this.getName());
			return null;
		}
	}
}
