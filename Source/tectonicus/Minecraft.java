/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package tectonicus;

import java.io.File;
import java.io.FilenameFilter;

import tectonicus.texture.ZipStack;
import tectonicus.texture.ZipStack.ZipStackEntry;
import tectonicus.util.OsDetect;

public class Minecraft
{

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
						if(Integer.parseInt(patch) < Integer.parseInt(version[2]))
							patch = version[2];
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

	public static boolean isValidWorldDir(File worldDir)
	{
		if (worldDir == null)
			return false;
		
		File levelDat =  findLevelDat(worldDir);
		return levelDat.exists();
	}
	
	public static boolean isValidMinecraftJar(File minecraftJar)
	{
		if (minecraftJar == null)
			return false;
		
		if (!minecraftJar.exists())
			return false;
		
		try
		{
			ZipStack zips = new ZipStack(minecraftJar, null);
			
			//ZipStackEntry terrainEntry = zips.getEntry("terrain.png");
			
			return (zips.getEntry("terrain.png") != null || zips.getEntry("textures/blocks/activatorRail.png") != null || zips.getEntry("assets/minecraft/textures/blocks/rail_activator.png") != null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static String getTexturePackVersion(File minecraftJar)
	{
		ZipStack zipStack;
		
		try
		{
			zipStack = new ZipStack(minecraftJar, null);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Couldn't open jar files for texture reading", e);
		}
		
		ZipStackEntry terrainEntry = zipStack.getEntry("terrain.png");
		if (terrainEntry != null)
			return "1.4";
		else
			return "1.5";
	}

	public static File findLevelDat(File worldDir)
	{
		if (worldDir == null)
			return null;
		
		return new File(worldDir, "level.dat");
	}
	
	public static File findPlayersDir(File worldDir)
	{
		if (worldDir == null)
			return null;
		
		return new File(worldDir, "players");
	}

	public static File findOpsFile(File worldDir)
	{
		if (worldDir == null)
			return null;
		
		return new File(worldDir.getParentFile(), "ops.txt");
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
