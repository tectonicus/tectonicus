/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tectonicus.jnbt.ByteTag;
import org.tectonicus.jnbt.CompoundTag;
import org.tectonicus.jnbt.DoubleTag;
import org.tectonicus.jnbt.IntTag;
import org.tectonicus.jnbt.ListTag;
import org.tectonicus.jnbt.NBTInputStream;
import org.tectonicus.jnbt.ShortTag;
import org.tectonicus.jnbt.StringTag;
import org.tectonicus.jnbt.Tag;
import tectonicus.configuration.Dimension;
import tectonicus.configuration.DimensionInfo;
import tectonicus.util.FileUtils;
import tectonicus.util.Vector3d;
import tectonicus.util.Vector3l;

import java.io.InputStream;
import java.io.IOException;
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
	private String name;
	private boolean defaultPlayerName;
	@Setter
	private boolean profileResolved;
	@Setter
	private String uuid;
	@Setter
	private String skinURL;
	private Dimension dimension;
	private String dimensionId;
	private Vector3d position;
	/**
	 * Caution - may be null if the player hasn't built a bed yet!
	 */
	private Vector3l spawnPosition;
	private Dimension spawnDimension;
	private String spawnDimensionId;
	private float health; // 0-20
	private int food; // 0-20
	private int air; // 0-300
	private int xpLevel;
	private int xpTotal;
	
	private List<Item> inventory;
	private List<Item> enderChestInventory;
	
	public static final int MAX_HEALTH = 20;
	public static final int MAX_AIR = 300;
	private static final ObjectReader OBJECT_READER = FileUtils.getOBJECT_MAPPER().reader();
	
	public Player() {
		dimension = Dimension.OVERWORLD;
		dimensionId = dimension.getId();
		spawnDimension = Dimension.OVERWORLD;
		spawnDimensionId = spawnDimension.getId();
		position = new Vector3d();
		inventory = new ArrayList<>();
		enderChestInventory = new ArrayList<>();
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
			if (nbtIn.readTag() instanceof CompoundTag root) {
				parse(root, NbtUtil.getInt(root, "DataVersion", 0));
			}
		}
	}
	
	public Player(String playerName, CompoundTag tag) {
		this(playerName, tag, NbtUtil.getInt(tag, "DataVersion", LegacyItems.FLATTENING_DATA_VERSION));
	}
	
	/**
	 * @param dataVersion the data version of the save holding this player, used to tell how its items were saved
	 */
	public Player(String playerName, CompoundTag tag, int dataVersion) {
		this();
		
		// Old level.dat player tags contain no username. Keep a display label without
		// treating that label as a real account for profile and skin lookups.
		defaultPlayerName = playerName == null || playerName.isBlank();
		name = defaultPlayerName ? "Player" : playerName;
		if (!defaultPlayerName)
			uuid = playerName;
		
		parse(tag, dataVersion);
	}
	
	public Player(String name, String uuid, String skinURL) {
		this();
		this.name = name;
		this.uuid = uuid;
		this.skinURL = skinURL;
	}

	public void setName(String name) {
		this.name = name;
		defaultPlayerName = false;
	}

	public boolean isDefaultPlayerName() {
		return defaultPlayerName || name == null || name.isBlank();
	}

	public String getDisplayName() {
		return isDefaultPlayerName() ? "Player" : name;
	}
	
	private void parse(CompoundTag root, int dataVersion) {
		ShortTag healthTag = NbtUtil.getChild(root, "Health", ShortTag.class);
		if (healthTag != null) {
			health = healthTag.getValue();
		} else { // Health switched to FloatTag in MC 1.9
			health = NbtUtil.getFloat(root, "Health", 0);
		}
		air = NbtUtil.getShort(root, "Air", (short) 0);
		food = NbtUtil.getInt(root, "foodLevel", 0);
		
		//This changed to a string in 1.16
		StringTag dimensionStr = NbtUtil.getChild(root, "Dimension", StringTag.class);
		if (dimensionStr != null) {
			dimensionId = dimensionStr.getValue();
			dimension = Dimension.byId(dimensionId);
		} else {
			final int dimensionVal = NbtUtil.getInt(root, "Dimension", 0);
			if (dimensionVal == 0)
				dimension = Dimension.OVERWORLD;
			else if (dimensionVal == 1)
				dimension = Dimension.END;
			else if (dimensionVal == -1)
				dimension = Dimension.NETHER;
			dimensionId = dimension.getId();
		}
		
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
			int[] pos = respawnTag.getIntArray("pos");
			spawnPosition = new Vector3l(pos[0], pos[1], pos[2]);
			spawnDimensionId = NbtUtil.getString(respawnTag, "dimension", Dimension.OVERWORLD.getId());
			spawnDimension = Dimension.byId(spawnDimensionId);
		} else if (spawnXTag != null && spawnYTag != null && spawnZTag != null) {
			spawnPosition = new Vector3l(spawnXTag.getValue(), spawnYTag.getValue(), spawnZTag.getValue());
		}
		
		//May exist in 1.21.4 and older
		StringTag spawnDimensionTag = NbtUtil.getChild(root, "SpawnDimension", StringTag.class);
		if (spawnDimensionTag != null) {
			spawnDimensionId = spawnDimensionTag.getValue();
			spawnDimension = Dimension.byId(spawnDimensionId);
		}
		
		xpLevel = NbtUtil.getInt(root, "XpLevel", 0);
		xpTotal = NbtUtil.getInt(root, "XpTotal", 0);
		
		// Parse inventory items (both inventory items and worn items)
		boolean legacyItems = LegacyItems.isLegacy(dataVersion);
		parseInventory(root, "Inventory", inventory, legacyItems);
		parseInventory(root, "EnderItems", enderChestInventory, legacyItems);

		// Since 1.21.5 armor and offhand stacks are saved separately from Inventory.
		CompoundTag equipment = NbtUtil.getChild(root, "equipment", CompoundTag.class);
		if (equipment != null) {
			String[] slots = {"feet", "legs", "chest", "head", "offhand"};
			for (int i = 0; i < slots.length; i++) {
				CompoundTag itemTag = NbtUtil.getChild(equipment, slots[i], CompoundTag.class);
				if (itemTag != null)
					addInventoryItem(itemTag, i == 4 ? -106 : 100 + i, inventory, false);
			}
		}
	}

	private static void parseInventory(CompoundTag root, String tagName, List<Item> items, boolean legacyItems) {
		ListTag inventoryList = NbtUtil.getChild(root, tagName, ListTag.class);
		if (inventoryList == null)
			return;
		for (Tag tag : inventoryList.getValue()) {
			if (tag instanceof CompoundTag itemTag) {
				ByteTag slotTag = NbtUtil.getChild(itemTag, "Slot", ByteTag.class);
				if (slotTag != null)
					addInventoryItem(itemTag, slotTag.getValue(), items, legacyItems);
			}
		}
	}

	private static void addInventoryItem(CompoundTag itemTag, int slot, List<Item> items, boolean legacyItems) {
		Item item = ItemNbtParser.parse(itemTag, slot, legacyItems);
		items.removeIf(existing -> existing.slot == slot);
		if (item != null)
			items.add(item);
	}
	
	
	public boolean isInDimension(DimensionInfo dimension) {
		return dimensionId.equals(dimension.name());
	}

	public class RequestPlayerInfoTask implements Callable<Void> {
		private static final Duration TIMEOUT = Duration.of(15, SECONDS);

		@Override
		public Void call() {
			try (HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()) {
				loadProfile(httpClient);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (IOException | tools.jackson.core.JacksonException | IllegalArgumentException e) {
				log.warn("Unable to load Minecraft player profile for {}: {}", uuid, e.getMessage());
			}
			return null;
		}

		private void loadProfile(HttpClient httpClient) throws IOException, InterruptedException {
			boolean lookupByName = uuid.equals(name);
			if (lookupByName) {
				//Get the uuid
				JsonNode account = requestProfile(httpClient, "https://api.mojang.com/users/profiles/minecraft/" + name);
				if (account == null || !account.path("id").isString()) return;
				uuid = account.path("id").asString();
			}
			JsonNode profile = requestProfile(httpClient, "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
			if (profile == null) return;
			if (!lookupByName && profile.path("name").isString()) setName(profile.path("name").asString());
			getSkinUrl(profile);
			profileResolved = !isDefaultPlayerName();
			log.debug("Loaded {}", name);
		}
		
		private JsonNode requestProfile(HttpClient httpClient, String url) throws IOException, InterruptedException {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				log.warn("Minecraft player profile lookup for {} returned HTTP {}", uuid, response.statusCode());
				return null;
			}
			return OBJECT_READER.readTree(response.body());
		}
		
		private void getSkinUrl(JsonNode node) {
			skinURL = null;
			for (JsonNode property : node.path("properties")) {
				if (!"textures".equals(property.path("name").asString()) || !property.path("value").isString()) continue;
				byte[] decoded = Base64.getDecoder().decode(property.path("value").asString());
				var skin = OBJECT_READER.readTree(new String(decoded, StandardCharsets.UTF_8)).path("textures").path("SKIN").path("url");
				skinURL = skin.isString() ? skin.asString() : null;
				break;
			}
		}
	}
}
