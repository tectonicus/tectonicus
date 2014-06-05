/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import tectonicus.configuration.Configuration;
import tectonicus.util.FileUtils;

public class PlayerSkinCache
{
	private static final int INDEX_VERSION = 1;
	
	private static final long MAX_AGE_BEFORE_REFRESH = 1000 * 60 * 60  * 60; // one hour in ms
	
	private final File cacheDir;
	
	private Map<String, CacheEntry> skinCache;
	
	public PlayerSkinCache(Configuration config, MessageDigest hashAlgorithm)
	{
		cacheDir = new File(config.cacheDir(), "skinCache");
		cacheDir.mkdirs();
		
		skinCache = new HashMap<String, PlayerSkinCache.CacheEntry>();
		
		boolean indexOk = false;
		
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
						long fetchedTime = Long.parseLong( e.getAttribute("fetchedTime") );
						String filePath = e.getAttribute("skinFile");
						
						CacheEntry entry = new CacheEntry();
						entry.playerName = playerName;
						entry.fetchedTime = fetchedTime;
						entry.skinFile = filePath;
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				
				indexOk = true;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		if (indexOk)
		{
			System.out.println("Using existing player skin cache");
		}
		else
		{
			// Wipe cache dir
			System.out.println("Player skin cache is old or corrupt, cleaning...");
			
			FileUtils.deleteDirectory(cacheDir);
			cacheDir.mkdirs();
		}
	}
	
	public void destroy()
	{
		System.out.println("Writing player skin cache info ("+skinCache.size()+" skin"+ (skinCache.size()>1?"s":"") + " to write)");
		
		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(new File(cacheDir, "skins.cache"));
			
			writer.println("<skinCache version=\""+INDEX_VERSION+"\">");
			writer.println("\t<entries>");
			
			int count = 0;
			
			for (CacheEntry entry : skinCache.values())
			{
				writer.println("\t\t<entry playerName=\""+entry.playerName+"\" skinFile=\""+entry.skinFile+"\" fetchedTime=\""+entry.fetchedTime+"\" />");
				
				count++;
				if (count % 100 == 0)
				{
					final int percentage = (int)Math.floor((count / (float)skinCache.size()) * 100);
					System.out.println(percentage+"%");
				}
			}
			
			System.out.println("100%");
			
			writer.println("\t</entries>");
			writer.println("</skinCache>");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (writer != null)
				writer.close();
		}
		
		System.out.println("Player skin cache written");
	}
	
	public BufferedImage fetchSkin(String playerName)
	{
		CacheEntry existing = null;
		
		if (skinCache.containsKey(playerName))
		{
			existing = skinCache.get(playerName);
			
			// If file is null that means this player has no custom skin
			if (existing.skinFile == null)
				return null;
			
			final long age = System.currentTimeMillis() - existing.fetchedTime;
			if (age < MAX_AGE_BEFORE_REFRESH)
			{
				try
				{
					return ImageIO.read( new File(cacheDir, existing.skinFile) );
				}
				catch (Exception e)
				{
					System.err.println("Couldn't read skin cache file: "+e);
				}
			}
		}
		
		// Not in cache, or cache stale so refetch from network
		skinCache.remove(playerName);
		
		BufferedImage newSkin = fetchSkinFromNetwork(playerName);
		File skinFile = null;
		if (newSkin != null)
		{
			skinFile = new File(cacheDir, playerName+".png");
			try
			{
				ImageIO.write(newSkin, "png", skinFile);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		CacheEntry newEntry = new CacheEntry();
		newEntry.playerName = playerName;
		newEntry.skinFile = playerName + ".png";
		newEntry.fetchedTime = System.currentTimeMillis();
		
		skinCache.put(playerName, newEntry);
		
		return newSkin;
	}
	
	private BufferedImage fetchSkinFromNetwork(String playerName)
	{
		try
		{
			String url = "http://www.minecraft.net/skin/"+playerName+".png";
			
            URLConnection remote = openConnection(url);
            InputStream skinStream = remote.getInputStream();
            try
            {
                BufferedImage skin = ImageIO.read(skinStream);
                if(skin != null)
				return skin;
		}
            finally
            {
                skinStream.close();
            }
		}
		catch (Exception e) {}
		
		System.out.println("No skin for player "+playerName);
		return null;
	}
	
    private static URLConnection openConnection(String location)
        throws IOException
    {
        URLConnection connection = null;
        do
        {
            URL skinURL = new URL(location);
            connection = skinURL.openConnection();
            location = connection.getHeaderField("Location");
        }
        while(location != null);
        return connection;
    }
	
	private static class CacheEntry
	{
		public String playerName;
		public long fetchedTime;
		public String skinFile;
	}
}
