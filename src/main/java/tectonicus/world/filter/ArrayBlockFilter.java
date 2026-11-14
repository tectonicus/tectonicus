/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.filter;

import tectonicus.BlockIds;
import tectonicus.Minecraft;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.RawChunk;

import java.util.function.Function;

public class ArrayBlockFilter implements BlockFilter
{

	private final boolean[][] columns;
	private final ChunkCoord coord;
	private final Function<ChunkCoord, ArrayBlockFilter> filterForChunk;
	
	public ArrayBlockFilter(ChunkCoord coord, Function<ChunkCoord, ArrayBlockFilter> filterForChunk) {
		this.coord = coord;
		this.filterForChunk = filterForChunk;
		columns = new boolean[RawChunk.WIDTH][RawChunk.DEPTH];
	}
	
	public void set(final int x, final int z, final boolean allow)
	{
		columns[x][z] = allow;
	}

	public void include(ArrayBlockFilter other) {
		for (int x = 0; x < columns.length; x++) {
			for (int z = 0; z < columns[x].length; z++) {
				columns[x][z] |= other.columns[x][z];
			}
		}
	}
	
	@Override
	public void filter(RawChunk rawChunk)
	{
		for (int x = 0; x< columns.length; x++)
		{
			for (int z = 0; z< columns[0].length; z++)
			{
				if (!columns[x][z])
				{
					for (int y = 0; y< Minecraft.getChunkHeight(); y++)
					{
						rawChunk.setBlockId(x, y, z, (byte)BlockIds.AIR);
						rawChunk.setSkyLight(x, y, z, (byte)16);
					}
				}
			}
		}

		rawChunk.filterEntities(this::containsColumn);
	}

	private boolean containsColumn(int x, int z) {
		if (x >= 0 && x < RawChunk.WIDTH && z >= 0 && z < RawChunk.DEPTH)
			return columns[x][z];

		// Hanging entities can be stored in a different chunk from their anchor block.
		ChunkCoord anchorChunk = new ChunkCoord(coord.x + Math.floorDiv(x, RawChunk.WIDTH),
				coord.z + Math.floorDiv(z, RawChunk.DEPTH));
		return filterForChunk.apply(anchorChunk).columns[Math.floorMod(x, RawChunk.WIDTH)][Math.floorMod(z, RawChunk.DEPTH)];
	}
	
	@Override
	public void preGeometryFilter(RawChunk center, RawChunk north, RawChunk south, RawChunk east, RawChunk west)
	{
		
	}
	
}
