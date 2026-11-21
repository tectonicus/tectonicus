/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tectonicus.configuration.Configuration;
import tectonicus.raw.Player;
import tectonicus.util.ImageUtils;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class PlayerSkinCache
{
	private static final int INDEX_VERSION = 1;
	
	public static final long MAX_AGE_BEFORE_REFRESH = 1000L * 60 * 60 * 24; // 24 hours
	
	private final File cacheDir;
	
	private final Map<String, CacheEntry> skinCache;
	private final Cache<String, Optional<BufferedImage>> skinsByUuid = Caffeine.newBuilder().maximumSize(256)
			.expireAfterWrite(Duration.ofMillis(MAX_AGE_BEFORE_REFRESH)).build();
	private final ExecutorService headSkinDownloads = Executors.newFixedThreadPool(4, Thread.ofPlatform().name("head-skin-", 1).factory());
	private final AsyncCache<String, Optional<BufferedImage>> skinsByUrl = Caffeine.newBuilder().maximumSize(256)
			.expireAfterWrite(Duration.ofMillis(MAX_AGE_BEFORE_REFRESH)).buildAsync();
	private final Map<String, PlayerProfile> refreshedProfiles = new ConcurrentHashMap<>();
	
	public PlayerSkinCache(Configuration config, MessageDigest hashAlgorithm)
	{
		cacheDir = new File(config.getCacheDir(), "skinCache");
		cacheDir.mkdirs();
		
		// Player loading and skull rendering both read and update the cache from worker threads.
		skinCache = new ConcurrentHashMap<>();
		
		// Try to open the skin cache file
		File indexFile = new File(cacheDir, "skins.cache");
		if (indexFile.exists())
		{
			try
			{
				DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				
				Document doc = docBuilder.parse(indexFile);
				NodeList nodeList = doc.getElementsByTagName("skinCache");
				Element root = (Element)nodeList.item(0);
				Element entriesNode = (Element)root.getElementsByTagName("entries").item(0);
				
				NodeList entriesList = entriesNode.getElementsByTagName("*");
				for (int i=0; i<entriesList.getLength(); i++)
				{
					try
					{
						Element e = (Element)entriesList.item(i);
						
						String playerName = e.getAttribute("playerName");
						String playerUUID = e.getAttribute("playerUUID");
						long fetchedTime = Long.parseLong( e.getAttribute("fetchedTime") );
						String skinURL = e.getAttribute("skinURL");
						String filePath = e.getAttribute("skinFile");
						
						CacheEntry entry = new CacheEntry();
						entry.playerName = playerName;
						entry.playerUUID = playerUUID;
						entry.fetchedTime = fetchedTime;
						entry.skinURL = skinURL;
						entry.skinFile = filePath;
						
						skinCache.put(playerUUID, entry);
					}
					catch (Exception e)
					{
						log.error("Exception: ", e);
					}
				}
				
				log.info("Using existing player skin cache");
			}
			catch (Exception e)
			{
				log.warn("Could not read player skin cache index {}", indexFile.getAbsolutePath(), e);
			}
		}
	}
	
	public void destroy()
	{
		headSkinDownloads.close();
		log.info("Writing player skin cache info ({} skin{} to write)", skinCache.size(), skinCache.size() != 1 ? "s" : "");
		
		File indexFile = new File(cacheDir, "skins.cache");
		try
		{
			Path temporary = Files.createTempFile(cacheDir.toPath(), "skins-", ".tmp");
			try {
				writeIndex(temporary.toFile());
				Files.move(temporary, indexFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} finally {
				Files.deleteIfExists(temporary);
			}
			log.info("Player skin cache written");
		}
		catch (Exception e)
		{
			log.error("Couldn't write player skin cache index {}", indexFile.getAbsolutePath(), e);
		}
	}

	private void writeIndex(File file) throws Exception
	{
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("skinCache");
		root.setAttribute("version", Integer.toString(INDEX_VERSION));
		doc.appendChild(root);
		Element entries = doc.createElement("entries");
		root.appendChild(entries);

		// The DOM serializer escapes quotes, ampersands and angle brackets in the attribute values.
		for (CacheEntry entry : skinCache.values())
		{
			Element e = doc.createElement("entry");
			e.setAttribute("playerName", String.valueOf(entry.playerName));
			e.setAttribute("playerUUID", String.valueOf(entry.playerUUID));
			e.setAttribute("skinURL", String.valueOf(entry.skinURL));
			e.setAttribute("skinFile", String.valueOf(entry.skinFile));
			e.setAttribute("fetchedTime", Long.toString(entry.fetchedTime));
			entries.appendChild(e);
		}

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		try (OutputStream out = Files.newOutputStream(file.toPath()))
		{
			transformer.transform(new DOMSource(doc), new StreamResult(out));
		}
	}
	
	public CacheEntry getCacheEntry(String playerUUID)
	{
		CacheEntry entry = skinCache.get(playerUUID);
		return entry == null || entry.playerName == null || entry.playerName.isBlank() || entry.playerName.equals("null")
				? null : entry;
	}

	public void refreshProfile(Player player) {
		// A render loads the same player save in each dimension, including after a failed lookup.
		PlayerProfile profile = refreshedProfiles.computeIfAbsent(player.getUuid(), uuid -> {
			player.new RequestPlayerInfoTask().call();
			return new PlayerProfile(player.getUuid(), player.getName(), player.getSkinURL(), player.isProfileResolved());
		});
		player.setUuid(profile.uuid());
		player.setName(profile.name());
		player.setSkinURL(profile.skinUrl());
		player.setProfileResolved(profile.resolved());
	}
	
	public BufferedImage fetchSkin(Player player)
	{
		String skinUrl = player.getSkinURL();
		if (player.getUuid() == null || player.getUuid().isBlank() || player.isDefaultPlayerName()) {
			// Player heads can carry a texture without a complete account profile.
			return fetchHeadSkin(skinUrl);
		}
		if (!player.isProfileResolved() && !hasSkinUrl(skinUrl)) return null;

		return ImageUtils.copy(skinsByUuid.get(player.getUuid(), ignored -> Optional.ofNullable(loadPlayerSkin(player))).orElse(null));
	}

	private BufferedImage loadPlayerSkin(Player player) {
		CacheEntry existing = getCacheEntry(player.getUuid());
		
		if (existing != null)
		{
			/* TODO:  Player icons should be stored in the cache too.  We don't need to regenerate the icon every run if the skin refresh
			 * time hasn't expired yet */
			final long age = System.currentTimeMillis() - existing.fetchedTime;
			if (age < MAX_AGE_BEFORE_REFRESH)
			{
				if (existing.skinFile.equals("Tectonicus_Default_Player_Skin.png"))
					return null;
				try
				{
					// Read the file the entry recorded, since the player may have been renamed since it was written.
					BufferedImage cached = ImageIO.read(new File(cacheDir, existing.skinFile));
					if (cached != null)
						return cached;
				}
				catch (Exception e)
				{
					log.warn("Couldn't read skin cache file {}", existing.skinFile, e);
				}
			}
		}
		
		// Not in cache, or cache stale so refetch from network
		skinCache.remove(player.getUuid());
		
		CacheEntry newEntry = new CacheEntry();
		BufferedImage newSkin = fetchSkinFromNetwork(player.getSkinURL());
		if (newSkin != null)
		{
			// Name the file by UUID: it survives a name change and can't contain path characters.
			String fileName = skinFileName(player.getUuid());
			try
			{
				writeSkinImage(newSkin, new File(cacheDir, fileName));
			}
			catch (Exception e)
			{
				log.error("Couldn't write skin for player {}", player.getName(), e);
				// Don't record a file that isn't there; the next render fetches it again.
				return newSkin;
			}
			newEntry.skinFile = fileName;
		}
		else
		{
			newEntry.skinFile = "Tectonicus_Default_Player_Skin.png";
			log.warn("No custom skin found for player {}", player.getName());
		}
		
		newEntry.playerName = player.getName();
		newEntry.playerUUID = player.getUuid();
		newEntry.skinURL = player.getSkinURL();
		newEntry.fetchedTime = System.currentTimeMillis();
		
		skinCache.put(player.getUuid(), newEntry);
		
		return newSkin;
	}
	
	static String skinFileName(String uuid)
	{
		return uuid.replaceAll("[^0-9A-Za-z-]", "_") + ".png";
	}

	/** Heads specify a particular texture, which can differ from the player's current skin. */
	public BufferedImage fetchHeadSkin(String skinUrl) {
		return hasSkinUrl(skinUrl) ? ImageUtils.copy(loadHeadSkinAsync(skinUrl).join().orElse(null)) : null;
	}

	public void prefetchHeadSkin(String skinUrl) {
		if (hasSkinUrl(skinUrl))
			loadHeadSkinAsync(skinUrl);
	}

	private CompletableFuture<Optional<BufferedImage>> loadHeadSkinAsync(String skinUrl) {
		return skinsByUrl.get(skinUrl, (url, executor) -> CompletableFuture.supplyAsync(() -> loadHeadSkin(url), headSkinDownloads));
	}

	private static boolean hasSkinUrl(String url) {
		return url != null && !url.isBlank() && !url.equals("null");
	}

	private Optional<BufferedImage> loadHeadSkin(String skinUrl) {
		File file = new File(cacheDir, headSkinFileName(skinUrl));
		if (file.isFile() && System.currentTimeMillis() - file.lastModified() < MAX_AGE_BEFORE_REFRESH) {
			try {
				BufferedImage skin = ImageIO.read(file);
				if (skin != null)
					return Optional.of(skin);
			} catch (IOException e) {
				log.warn("Could not read cached head skin {}", file, e);
			}
		}
		BufferedImage skin = fetchSkinFromNetwork(skinUrl);
		if (skin != null) {
			try {
				writeSkinImage(skin, file);
			} catch (IOException e) {
				log.warn("Could not cache head skin {}", file, e);
			}
		}
		return Optional.ofNullable(skin);
	}

	private void writeSkinImage(BufferedImage skin, File file) throws IOException {
		Path temporary = Files.createTempFile(cacheDir.toPath(), "skin-", ".tmp");
		try {
			if (!ImageIO.write(skin, "png", temporary.toFile()))
				throw new IOException("No PNG writer available");
			Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private static String headSkinFileName(String url) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
			return "head-" + HexFormat.of().formatHex(digest) + ".png";
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private BufferedImage fetchSkinFromNetwork(String skinURL)
	{
		if (!hasSkinUrl(skinURL))
			return null;
		try
		{
			URLConnection remote = openConnection(skinURL);
			try (InputStream skinStream = remote.getInputStream()) {
				return ImageIO.read(skinStream);
			}
		}
		catch (Exception e) {}
		
		return null;
	}
	
	private static URLConnection openConnection(String location) throws IOException {
		URI url = URI.create(location);
		for (int redirects = 0; redirects <= 5; redirects++) {
			URLConnection connection = url.toURL().openConnection();
			connection.setConnectTimeout(5_000);
			connection.setReadTimeout(5_000);
			if (connection instanceof HttpURLConnection http)
				http.setInstanceFollowRedirects(false);
			String redirect = connection.getHeaderField("Location");
			if (redirect == null)
				return connection;
			if (connection instanceof HttpURLConnection http)
				http.disconnect();
			url = url.resolve(redirect);
		}
		throw new IOException("Too many redirects fetching player skin");
	}
	
	public static class CacheEntry
	{
		public String playerName;
		public String playerUUID;
		public long fetchedTime;
		public String skinURL;
		public String skinFile;
	}

	private record PlayerProfile(String uuid, String name, String skinUrl, boolean resolved) {}
}
