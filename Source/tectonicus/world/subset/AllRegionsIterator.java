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
