/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.xml.bind.DatatypeConverter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import tectonicus.configuration.Configuration.Dimension;
import tectonicus.util.Vector3d;
import tectonicus.util.Vector3l;
import xyz.nickr.nbt.NBTCodec;
import xyz.nickr.nbt.NBTCompression;
import xyz.nickr.nbt.tags.CompoundTag;
import xyz.nickr.nbt.tags.ListTag;
import xyz.nickr.nbt.tags.NBTTag;

public class Player
{
	public static final int MAX_HEALTH = 20;
	public static final int MAX_AIR = 300;
	
	private String name;
	private String UUID;
	private String skinURL;
	
	private Dimension dimension;
	
	private Vector3d position;
	
	private Vector3l spawnPos;
	
	private int health; // 0-20
	private int food; // 0-20
	private int air; // 0-300
	
	private int xpLevel;
	private int xpTotal;
	
	private ArrayList<Item> inventory;
	
	public Player(Path playerFile) throws Exception
	{
		System.out.println("Loading raw player from "+playerFile);
		
		dimension = Dimension.Terra;
		position = new Vector3d();
		inventory = new ArrayList<Item>();
		
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

		try(InputStream in = Files.newInputStream(playerFile))
		{
			NBTCodec codec = new NBTCodec(ByteOrder.BIG_ENDIAN);
			CompoundTag root = codec.decode(in, NBTCompression.GZIP).getAsCompoundTag();

			parse(root);
		}
	}
	
	public Player(String playerName, CompoundTag tag)
	{
		this.name = playerName;
		
		try {
			parse(tag);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Player(String name, String UUID, String skinURL)
	{
		this.name = name;
		this.UUID = UUID;
		this.skinURL = skinURL;
	}
	
	private void parse(CompoundTag root) throws Exception
	{
		dimension = Dimension.Terra;
		position = new Vector3d();
		inventory = new ArrayList<Item>();	
		
		health = root.getAsNumber("Health").shortValue();
		air = root.getAsNumber("Air").shortValue();
		food = root.getAsNumber("foodLevel").intValue();
		
		final int dimensionVal = root.getAsNumber("Dimension").intValue();
		if (dimensionVal == 0)
			dimension = Dimension.Terra;
		else if (dimensionVal == 1)
			dimension = Dimension.Ender;
		else if (dimensionVal == -1)
			dimension = Dimension.Nether;
		
		ListTag posList = root.getAsListTag("Pos");
		if (posList != null)
		{
			double x = posList.get(0).getAsNumber().doubleValue();
			double y = posList.get(1).getAsNumber().doubleValue();
			double z = posList.get(2).getAsNumber().doubleValue();
			
			position.set(x, y, z);
		}
		
		int spawnX = root.getAsNumber("SpawnX").intValue();
		int spawnY = root.getAsNumber("SpawnY").intValue();
		int spawnZ = root.getAsNumber("SpawnZ").intValue();
		
		spawnPos = new Vector3l(spawnX, spawnY, spawnZ);
		
		xpLevel = root.getAsNumber("XpLevel").intValue();
		xpTotal = root.getAsNumber("XpTotal").intValue();
		
		// Parse inventory items (both inventory items and worn items)
		ListTag inventoryList = root.getAsListTag("Inventory");
		if (inventoryList != null)
		{
			for (NBTTag t : inventoryList)
			{
				if (t.isCompoundTag())
				{
					CompoundTag itemTag = (CompoundTag)t;
					
					short id = itemTag.getAsNumber("id").shortValue();
					short damage = itemTag.getAsNumber("Damage").shortValue();
					byte count = itemTag.getAsNumber("Count").byteValue();
					byte slot = itemTag.getAsNumber("Slot").byteValue();
					
					inventory.add( new Item(id, damage, count, slot) );
				}
			}
		}
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getSkinURL()
	{
		return skinURL;
	}
	
	public void setSkinURL(String skinURL)
	{
		this.skinURL = skinURL;
	}
	
	public String getUUID()
	{
		return UUID;
	}
	
	public Vector3d getPosition()
	{
		return new Vector3d(position);
	}
	
	public int getHealth()
	{
		return health;
	}
	
	public int getFood()
	{
		return food;
	}
	
	public int getAir()
	{
		return air;
	}
	
	public int getXpLevel()
	{
		return xpLevel;
	}
	
	public int getXpTotal()
	{
		return xpTotal;
	}
	
	public Dimension getDimension()
	{
		return dimension;
	}
	
	/** Caution - may be null if the player hasn't built a bed yet! */
	public Vector3l getSpawnPosition()
	{
		return spawnPos;
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
					System.err.println("ERROR: Unrecognized UUID");
				else if (responseCode == 429)
					System.err.println("ERROR: Too many requests. You are only allowed to contact the Mojang session server once per minute per player.  Wait for a minute and try again.");
	
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder builder = new StringBuilder();
				
				String line = null;
				while ((line = reader.readLine()) != null)
				{
					builder.append(line + "\n");
				}
				reader.close();
				
				JsonObject obj = new JsonParser().parse(builder.toString()).getAsJsonObject();
				Player.this.setName(obj.get("name").getAsString());
				JsonObject textures = obj.get("properties").getAsJsonArray().get(0).getAsJsonObject();
				byte[] decoded = DatatypeConverter.parseBase64Binary(textures.get("value").getAsString());
				obj = new JsonParser().parse(new String(decoded, "UTF-8")).getAsJsonObject();
				boolean hasSkin = obj.get("textures").getAsJsonObject().has("SKIN");
				String textureUrl = null;
				if (hasSkin == true)
					textureUrl = obj.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString();
				Player.this.setSkinURL(textureUrl);
			}
			System.out.println("Loaded " + Player.this.getName());
			return null;
		}
	}
}
