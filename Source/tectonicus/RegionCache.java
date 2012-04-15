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
import java.util.LinkedHashMap;

public class RegionCache
{
	private SaveFormat format;
	
	private File worldDir;
	
	// TODO: Use a hash map of SoftReferences so we grow up to max memory?
	private RegionCacheMap cache;
	
	public RegionCache(File worldDir)
	{
		if (worldDir == null)
			throw new IllegalArgumentException("worldDir cannot be null");
		
		this.worldDir = worldDir;
		
		File regionDir = new File(worldDir, "region");
		
		File[] anvilFiles = regionDir.listFiles(new AnvilFileFilter());
		if (anvilFiles.length > 0)
			format = SaveFormat.Anvil;
		else
			format = SaveFormat.McRegion;
		
		System.out.println("Detected "+format+" save format");
		
		this.cache = new RegionCacheMap(8);
		this.cache.setMinSize(4);
	}
	
	public Region getRegion(RegionCoord coord)
	{
		Region region = null;
		
		region = cache.get(coord);
		if (region == null)
		{
			File regionFile = ChunkLocator.findRegionFile(worldDir, coord, format);
			if (regionFile.exists())
			{
				try
				{
					region = new Region(regionFile);
					
					cache.put(coord, region);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			cache.touch(coord);
		}
		
		return region;
	}
	
	private static class RegionCacheMap extends LinkedHashMap<RegionCoord, Region>
	{
		private static final long serialVersionUID = 1L;

		private int maxSize;
		private int minSize;
		
		public RegionCacheMap(final int maxSize)
		{
			this.maxSize = maxSize;
		}
		
		public void setMinSize(final int size)
		{
			this.minSize = size;
		}
		
		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<RegionCoord, Region> eldest)
		{
			if (size() <= minSize)
				return false;
			
			final boolean remove = size() > maxSize;
			return remove;
		}
		
		public void touch(RegionCoord coord)
		{
			Region r = remove(coord);
			if (r != null)
				put(coord, r);
		}
	}
	
	private static class AnvilFileFilter implements FilenameFilter
	{
		private boolean hasFoundAnvilFile = false;
		
		@Override
		public boolean accept(File dir, String file)
		{
			if (hasFoundAnvilFile)
				return false;
			
			if (file.endsWith(".mca"))
			{
				hasFoundAnvilFile = true;
				return true;
			}
			
			return false;
		}
	}
}
