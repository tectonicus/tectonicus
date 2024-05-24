/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache.swap;

import lombok.extern.slf4j.Slf4j;
import tectonicus.TileCoord;
import tectonicus.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class HddTileList implements Iterable<TileCoord>
{
	private final File baseDir;
	
	private int size;
	
	private int minTileX, maxTileX;
	private int minTileY, maxTileY;
	
	public HddTileList(File baseDir)
	{
		this.baseDir = baseDir;
		
		if (baseDir.exists())
			FileUtils.deleteDirectory(baseDir);
		baseDir.mkdirs();
		
		minTileX = minTileY = Integer.MAX_VALUE;
		maxTileX = maxTileY = Integer.MIN_VALUE;
	}
	
	@Override
	public java.util.Iterator<TileCoord> iterator()
	{
		return new HddTileListIterator(baseDir);
	}
	
	public void add(TileCoord coord)
	{
		File coordFile = tileCoordToFile(coord, baseDir);
		
		if (!coordFile.exists())
		{
			coordFile.getParentFile().mkdirs();

			try (FileOutputStream fOut = new FileOutputStream(coordFile)) {

				minTileX = Math.min(coord.x, minTileX);
				maxTileX = Math.max(coord.x, maxTileX);

				minTileY = Math.min(coord.y, minTileY);
				maxTileY = Math.max(coord.y, maxTileY);

				size++;
			} catch (Exception e) {
			}
		}
	}
	
	public int size()
	{
		return size;
	}
	
	/** Returns a tile coord with the smallest x coord and the smallest y coord inserted.
	 *  Note that this exact coord may not actually be present itself.
	 */
	public TileCoord getAbsoluteMinCoord()
	{
		return new TileCoord(minTileX, minTileY);
	}

	/** Returns a tile coord with the largest x coord and the largest y coord inserted.
	 *  Note that this exact coord may not actually be present itself.
	 */
	public TileCoord getAbsoluteMaxCoord()
	{
		return new TileCoord(maxTileX, maxTileY);
	}
	
	public static File tileCoordToFile(TileCoord coord, File baseDir)
	{
		final int xBucket  = coord.x / 4;
		final int yBucket = coord.y / 4;
		File first = new File(baseDir, ""+xBucket);
		File second = new File(first, ""+yBucket);
		return new File(second, "t_"+coord.x+"_"+coord.y+".tile");
	}
	
	public static TileCoord fileToTileCoord(File file)
	{
		// t_1_2.tile
		try
		{
			String working = file.getName();
			
			final int firstDelim = working.indexOf('_');
			final int secondDelim = working.indexOf('_', firstDelim+1);
			final int thirdDelim = working.indexOf('.');
			
			if (firstDelim != -1 && secondDelim != -1 && thirdDelim != -1)
			{
				String xStr = working.substring(firstDelim+1, secondDelim);
				String yStr = working.substring(secondDelim+1, thirdDelim);
				
				final int x = Integer.parseInt(xStr);
				final int y = Integer.parseInt(yStr);
				
				return new TileCoord(x, y);
			}
		}
		catch (Exception e)
		{
			log.error("Exception: ", e);
		}
		
		return null;
	}

	public Set<TileCoord> toSet() {
		Set<TileCoord> tileCoords = new HashSet<>();

		for (TileCoord c : this) {
			tileCoords.add(c);
		}

		return tileCoords;
	}
}
