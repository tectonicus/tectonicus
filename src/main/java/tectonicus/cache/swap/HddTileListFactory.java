/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache.swap;

import java.io.File;

import com.google.code.minecraftbiomeextractor.FileUtils;

public class HddTileListFactory
{
	private int counter;
	
	private final File baseDir;
	
	public HddTileListFactory(File cacheDir)
	{
		baseDir = cacheDir;
		if (baseDir.exists())
		{
			FileUtils.deleteDirectory(baseDir);
		}
		baseDir.mkdirs();
	}
	
	public HddTileList createList()
	{
		File listDir = null;
		do
		{
			String dirName = "autoList"+counter;
			counter++;
			listDir = new File(baseDir, dirName);
		}
		while (listDir.exists());
			
		return new HddTileList(listDir);
	}
	
	public HddTileList createList(String name)
	{
		File listDir = new File(baseDir, name);
		if (listDir.exists())
			throw new RuntimeException("Tile list already exists at "+listDir.getAbsolutePath());
		
		return new HddTileList(listDir);
	}
}
