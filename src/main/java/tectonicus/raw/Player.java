/*
 * Copyright (c) 2025 Tectonicus contributors.  All rights reserved.
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
import lombok.extern.slf4j.Slf4j;
import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.DoubleTag;
import org.jnbt.IntArrayTag;
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

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;

import static java.time.temporal.ChronoUnit.SECONDS;

@Slf4j
@Getter
public class Player {
	@Setter
	private String name;
	@Setter
	private String uuid;
	@Setter
	private String skinURL;
	private Dimension dimension;
	private Vector3d position;
	/**
	 * Caution - may be null if the player hasn't built a bed yet!
	 */
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
	
	public Player() {
		dimension = Dimension.OVERWORLD;
		spawnDimension = Dimension.OVERWORLD;
		position = new Vector3d();
		inventory = new ArrayList<>();
	}
	
	public Player(Path playerFile) throws Exception {
		this();
		log.debug("Loading raw player from {}", playerFile);
		
		uuid = playerFile.getFileName().toString();
		
		final int dotPos = uuid.lastIndexOf('.');
		if (uuid.contains("-")) {
			uuid = uuid.substring(0, dotPos).replace("-", "");
		} else { // It's a username not actually a uuid
			name = uuid = uuid.substring(0, dotPos);
		}
		
		skinURL = null;
		
		try (InputStream in = Files.newInputStream(playerFile); NBTInputStream nbtIn = new NBTInputStream(in)) {
			Tag tag = nbtIn.readTag();
			if (tag instanceof CompoundTag) {
				CompoundTag root = (CompoundTag) tag;
				parse(root);
			}
		}
	}
	
	public Player(String playerName, CompoundTag tag) {
		this();
		
		if (!playerName.isEmpty()) {
			name = uuid = playerName;
		}
		
		parse(tag);
	}
	
	public Player(String name, String uuid, String skinURL) {
		this.name = name;
		this.uuid = uuid;
		this.skinURL = skinURL;
	}
	
	private void parse(CompoundTag root) {
		ShortTag healthTag = NbtUtil.getChild(root, "Health", ShortTag.class);
		if (healthTag != null) {
			health = healthTag.getValue();
		} else { // Health switched to FloatTag in MC 1.9
			health = NbtUtil.getFloat(root, "Health", 0);
		}
		air = NbtUtil.getShort(root, "Air", (short) 0);
		food = NbtUtil.getInt(root, "foodLevel", 0);
		
		final int dimensionVal = NbtUtil.getInt(root, "Dimension", 0);
		if (dimensionVal == 0)
			dimension = Dimension.OVERWORLD;
		else if (dimensionVal == 1)
			dimension = Dimension.END;
		else if (dimensionVal == -1)
			dimension = Dimension.NETHER;
		
		ListTag posList = NbtUtil.getChild(root, "Pos", ListTag.class);
		if (posList != null) {
			DoubleTag xTag = NbtUtil.getChild(posList, 0, DoubleTag.class);
			DoubleTag yTag = NbtUtil.getChild(posList, 1, DoubleTag.class);
			DoubleTag zTag = NbtUtil.getChild(posList, 2, DoubleTag.class);
			
			if (xTag != null && yTag != null && zTag != null) {
				position.set(xTag.getValue(), yTag.getValue(), zTag.getValue());
			}
		}
		
		//May exist in 1.21.4 and older
		IntTag spawnXTag = NbtUtil.getChild(root, "SpawnX", IntTag.class);
		IntTag spawnYTag = NbtUtil.getChild(root, "SpawnY", IntTag.class);
		IntTag spawnZTag = NbtUtil.getChild(root, "SpawnZ", IntTag.class);
		
		//May exist in 1.21.5 and newer
		CompoundTag respawnTag = NbtUtil.getChild(root, "respawn", CompoundTag.class);
		
		if (respawnTag != null) {
			int[] pos = NbtUtil.getChild(respawnTag, "pos", IntArrayTag.class).getValue();
			spawnPosition = new Vector3l(pos[0], pos[1], pos[2]);
			spawnDimension = Dimension.byId(NbtUtil.getString(respawnTag, "dimension", "minecraft:overworld"));
		} else if (spawnXTag != null && spawnYTag != null && spawnZTag != null) {
			spawnPosition = new Vector3l(spawnXTag.getValue(), spawnYTag.getValue(), spawnZTag.getValue());
		}
		
		//May exist in 1.21.4 and older
		StringTag spawnDimensionTag = NbtUtil.getChild(root, "SpawnDimension", StringTag.class);
		if (spawnDimensionTag != null) {
			spawnDimension = Dimension.byId(spawnDimensionTag.getValue());
		}
		
		xpLevel = NbtUtil.getInt(root, "XpLevel", 0);
		xpTotal = NbtUtil.getInt(root, "XpTotal", 0);
		
		// Parse inventory items (both inventory items and worn items)
		ListTag inventoryList = NbtUtil.getChild(root, "Inventory", ListTag.class);
		if (inventoryList != null) {
			for (Tag t : inventoryList.getValue()) {
				if (t instanceof CompoundTag) {
					CompoundTag itemTag = (CompoundTag) t;
					
					StringTag idTag = NbtUtil.getChild(itemTag, "id", StringTag.class);
					ShortTag damageTag = NbtUtil.getChild(itemTag, "Damage", ShortTag.class);
					ByteTag countTag = NbtUtil.getChild(itemTag, "Count", ByteTag.class);
					ByteTag slotTag = NbtUtil.getChild(itemTag, "Slot", ByteTag.class);
					
					if (idTag != null && damageTag != null && countTag != null && slotTag != null) {
						inventory.add(new Item(idTag.getValue(), damageTag.getValue(), countTag.getValue(), slotTag.getValue(), null));
					}
				}
			}
		}
	}
	
	
	public class RequestPlayerInfoTask implements Callable<Void> {
		@Override
		public Void call() throws Exception {
			HttpClient httpClient = HttpClient.newHttpClient();
			if (Player.this.getUuid().equals(Player.this.getName())) { //no uuid just a username
				//Get the uuid
				HttpRequest request = HttpRequest.newBuilder(new URI("https://api.mojang.com/users/profiles/minecraft/" + Player.this.getName())).GET().build();
				HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				log.trace(response.body());
				JsonNode usernameNode = OBJECT_READER.readTree(response.body());
				Player.this.setUuid(usernameNode.get("id").asText());
				
				JsonNode profileNode = getProfile(httpClient);
				getSkinUrl(profileNode);
			} else {
				JsonNode profileNode = getProfile(httpClient);
				Player.this.setName(profileNode.get("name").asText());
				getSkinUrl(profileNode);
			}
			log.debug("Loaded {}", Player.this.getName());
			return null;
		}
		
		private JsonNode getProfile(HttpClient httpClient) throws Exception {
			HttpRequest request = HttpRequest.newBuilder(new URI("https://sessionserver.mojang.com/session/minecraft/profile/" + Player.this.getUuid()))
					.timeout(Duration.of(15, SECONDS)).GET().build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			log.trace(response.body());
			int responseCode = response.statusCode();
			if (responseCode == 204)
				log.error("ERROR: Unrecognized UUID");
			else if (responseCode == 429) //Is this error still necessary?  It doesn't seem to occur anymore
				log.error("ERROR: Too many requests. You are only allowed to contact the Mojang session server once per minute per player.  Wait for a minute and try again.");
			
			return OBJECT_READER.readTree(response.body());
		}
		
		private void getSkinUrl(JsonNode node) throws Exception {
			JsonNode properties = node.get("properties").get(0);
			byte[] decoded = Base64.getDecoder().decode(properties.get("value").asText());
			String textureValue = new String(decoded, StandardCharsets.UTF_8);
			log.trace(textureValue);
			node = OBJECT_READER.readTree(textureValue);
			boolean hasSkin = node.get("textures").has("SKIN");
			String textureUrl = null;
			if (hasSkin)
				textureUrl = node.get("textures").get("SKIN").get("url").asText();
			Player.this.setSkinURL(textureUrl);
		}
	}
}
