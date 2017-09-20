/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;

import tectonicus.texture.ZipStack;
import tectonicus.util.OsDetect;

public class Minecraft
{
	static float minecraftVersion;
	static boolean useOldColorPalette = false;
	
	public static float getMinecraftVersion()
	{
		return minecraftVersion;
	}
	
	public static void setMinecraftVersion(float version)
	{
		minecraftVersion = version;
	}
	
	public static boolean useOldColorPalette()
	{
		return useOldColorPalette;
	}
	
	public static void setUseOldColorPalette(boolean b)
	{
		useOldColorPalette = b;
	}
	
	public static File findMinecraftDir()
	{
		File result;
		
		File userHome = new File(System.getProperty("user.home"));
		
		if (OsDetect.isMac())
		{
			result = new File(userHome, "Library/Application Support/minecraft");
		}
		else if (OsDetect.isWindows())
		{
			result = new File(System.getenv("APPDATA"), ".minecraft");
		}
		else
		{
			result = new File(userHome, ".minecraft");
		}
		
		return result;
	}
	
	public static File findMinecraftJar()
	{
		File versionsDir = new File(findMinecraftDir(), "versions");
		
		if(versionsDir.exists())
		{
			System.out.println("Searching for most recent Minecraft jar...");
			String[] directories = versionsDir.list(new FilenameFilter() {
			  @Override
			  public boolean accept(File dir, String name) {
			    return new File(dir, name).isDirectory();
			  }
			});
			
			String major = "0";
			String minor = "0";
			String patch = "0";
			for(String directory : directories)
			{
				String[] version = directory.split("\\.");
				try 
				{
					if(version.length == 2)
					{
						if(Integer.parseInt(major) < Integer.parseInt(version[0]))
						{
							major = version[0];
							minor = "0";
							patch = "0";
						}
						if(Integer.parseInt(minor) < Integer.parseInt(version[1]))
						{
							minor = version[1];
							patch = "0";
						}
					}
					else if (version.length == 3)
					{
						if(Integer.parseInt(major) < Integer.parseInt(version[0]))
						{
							major = version[0];
							minor = "0";
							patch = "0";
						}
						if(Integer.parseInt(minor) < Integer.parseInt(version[1]))
						{
							minor = version[1];
							patch = "0";
						}
						if(Integer.parseInt(minor) == Integer.parseInt(version[1]) && Integer.parseInt(patch) < Integer.parseInt(version[2]))
						{
							patch = version[2];
						}
					}
				}
				catch(NumberFormatException e)
				{
					System.out.println("Skipping directory with invalid version number: " + directory);
				}
			}
			
			String version;
			if(patch.equals("0"))
				version = major + "." + minor;
			else
				version = major + "." + minor + "." + patch;

			return new File(findMinecraftDir(), "versions/" + version + "/" + version + ".jar");
		}
		else	
			return new File(findMinecraftDir(), "bin/minecraft.jar");
	}
	
	public static File findWorldDir(final int index)
	{
		assert (index >= 0);
		assert (index < 5);
		
		return new File(Minecraft.findMinecraftDir(), "saves/World"+(index+1));
	}

	public static boolean isValidWorldDir(Path worldDir)
	{
		if (worldDir == null)
			return false;
		
		return Files.exists(findLevelDat(worldDir));
	}
	
	public static boolean isValidMinecraftJar(File minecraftJar) //TODO:  This is only used by the old Swing GUI, refactor to remove it
	{
		if (minecraftJar == null)
			return false;
		
		if (!minecraftJar.exists())
			return false;
		
		try
		{
			ZipStack zips = new ZipStack(minecraftJar, null, null);
			
			return zips.hasFile("terrain.png") || zips.hasFile("textures/blocks/activatorRail.png") || zips.hasFile("assets/minecraft/textures/blocks/rail_activator.png");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return false;
	}

	public static Path findLevelDat(Path worldDir)
	{
		if (worldDir == null)
			return null;
		
		return worldDir.resolve("level.dat");
	}
	
	public static File findPlayersDir(File worldDir)
	{
		if (worldDir == null)
			return null;
		
		File newDir = new File(worldDir, "playerdata");
		if (newDir.exists())
			return newDir;
		else
			return new File(worldDir, "players");
	}

	public static Path findServerPlayerFile(Path worldDir, String name)
	{	
		if (worldDir == null)
			return null;
		Path json = worldDir.getParent().resolve(name + ".json");

		Path txt = null;
		if (name.equals("whitelist"))
			txt = worldDir.getParent().resolve("white-list.txt");
		else
			txt = worldDir.getParent().resolve(name + ".txt");
		
		if (Files.exists(json) && !Files.isDirectory(json))
		{
			return json;
		}
		else if (Files.exists(txt) && !Files.isDirectory(txt))
		{		
			return txt;
		}
		else
		{
			return worldDir.getParent();
		}
	}

	/** Look for dimensionDir/region/*.mcr or dimensionDir/region/*.mca */
	public static boolean isValidDimensionDir(File dimensionDir)
	{
		File regionDir = new File(dimensionDir, "region");
		if (!regionDir.exists())
			return false;
		
		File[] mcRegionFiles = regionDir.listFiles(new McRegionFileFilter());
		
		File[] anvilRegionFiles = regionDir.listFiles(new AnvilFileFilter());
		
		return (mcRegionFiles != null && mcRegionFiles.length > 0)
				|| (anvilRegionFiles != null && anvilRegionFiles.length > 0);
	}
}
