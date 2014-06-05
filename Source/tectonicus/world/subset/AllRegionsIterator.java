/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.subset;

import java.io.File;
import java.io.FileFilter;

import tectonicus.SaveFormat;


public class AllRegionsIterator implements RegionIterator
{
	private final File worldDir;
	
	private File[] regionFiles;
	
	private int position;
	
	public AllRegionsIterator(File worldDir, SaveFormat saveFormat)
	{
		this.worldDir = worldDir;
		
		File regionDir = new File(worldDir, "region");
		regionFiles = regionDir.listFiles(saveFormat == SaveFormat.McRegion ? new RegionFileFilter() : new AnvilFileFilter());
		if (regionFiles == null)
			regionFiles = new File[0];
		
		position = 0;
	}
	
	@Override
	public File getBaseDir()
	{
		return worldDir;
	}
	
	@Override
	public boolean hasNext()
	{
		if (regionFiles == null)
			return false;
		
		if (regionFiles.length == 0)
			return false;
		
		return position < regionFiles.length;
	}
	
	@Override
	public File next()
	{
		if (!hasNext())
			return null;
		
		File file = regionFiles[position];
		
		position++;
		
		return file;
	}
	
	private static class RegionFileFilter implements FileFilter
	{
		@Override
		public boolean accept(File pathname)
		{
			return pathname != null && pathname.isFile() && pathname.getName().startsWith("r.") && pathname.getName().endsWith(".mcr");
		}
	}

	private static class AnvilFileFilter implements FileFilter
	{
		@Override
		public boolean accept(File pathname)
		{
			return pathname != null && pathname.isFile() && pathname.getName().startsWith("r.") && pathname.getName().endsWith(".mca");
		}
	}
}
