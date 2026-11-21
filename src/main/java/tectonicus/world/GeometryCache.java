/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;

/**
 * Keeps built chunk geometry limited by the vertex memory it uses rather than by chunk count.
 * <p>
 * The rasteriser places vertices in a fixed amount of video memory (see {@link #chooseVertexMemoryBytes}).
 * The cache budget is a little below that, so evicting chunks frees room before new geometry runs out of it;
 * geometry that still does not fit is drawn from system memory, which is slower but never overfills the GPU.
 */
@Slf4j
class GeometryCache extends ChunkCache
{
	/** System property that sets the vertex memory in megabytes, overriding the one derived from video memory. */
	static final String BUDGET_PROPERTY = "tectonicus.geometryCacheMb";

	static final long MB = 1024L * 1024L;
	static final long DEFAULT_VERTEX_MEMORY_BYTES = 512 * MB;
	static final long MIN_VERTEX_MEMORY_BYTES = 128 * MB;
	static final long MAX_VERTEX_MEMORY_BYTES = 8192 * MB;
	/** Most of the video memory to use for vertices, so textures and render targets keep room. */
	static final double VIDEO_MEMORY_SHARE = 0.6;
	/** Video memory to leave free for the driver, the desktop and other programs. */
	static final double VIDEO_MEMORY_RESERVE_SHARE = 0.15;
	static final long MIN_VIDEO_MEMORY_RESERVE_BYTES = 192 * MB;
	/** Share of the vertex memory the cache may fill; the rest absorbs alignment and fragmentation. */
	static final double CACHE_SHARE_OF_VERTEX_MEMORY = 0.9;

	/** Allowance per mesh for alignment of its place in the shared vertex buffers. */
	static final long MESH_OVERHEAD_BYTES = 256;
	/** Lower bound on a chunk's weight, which also bounds how many nearly empty chunks the cache holds. */
	static final int MIN_WEIGHT_KB = 16;

	private final long budgetBytes;

	public GeometryCache(long budgetBytes, Function<ChunkCoord, Chunk> getChunk)
	{
		super(budgetBytes / 1024, (coord, chunk) -> weightKb(chunk.getGeometryGpuMemoryEstimate(MESH_OVERHEAD_BYTES)), getChunk);
		this.budgetBytes = budgetBytes;
	}

	public long getBudgetBytes()
	{
		return budgetBytes;
	}

	/** Estimated GPU memory of the cached geometry, as weighed when each chunk was added. */
	public long getEstimatedGpuBytes()
	{
		return weightedSize().orElse(0) * 1024;
	}

	static int weightKb(long bytes)
	{
		long kb = (bytes + 1023) / 1024;
		return (int) Math.min(Integer.MAX_VALUE, Math.max(MIN_WEIGHT_KB, kb));
	}

	/** The cache budget for a given amount of vertex memory. */
	static long cacheBudgetBytes(long vertexMemoryBytes)
	{
		return (long) (vertexMemoryBytes * CACHE_SHARE_OF_VERTEX_MEMORY);
	}

	/**
	 * Chooses how much video memory to reserve for vertices: the {@link #BUDGET_PROPERTY} override when set,
	 * otherwise a share of the video memory that also leaves a reserve free, or a fixed default when the
	 * driver reports nothing.
	 *
	 * @param totalBytes dedicated video memory, or zero or less when unknown
	 * @param availableBytes video memory free now plus any already reserved for vertices, or less than zero when unknown
	 * @param overrideMb the override in megabytes, or null
	 */
	static long chooseVertexMemoryBytes(long totalBytes, long availableBytes, String overrideMb)
	{
		if (overrideMb != null && !overrideMb.isBlank()) {
			try {
				long mb = Long.parseLong(overrideMb.trim());
				if (mb > 0)
					return mb * MB;
			} catch (NumberFormatException e) {
				// Reported below.
			}
			log.warn("Ignoring {}={}: expected a positive number of megabytes", BUDGET_PROPERTY, overrideMb);
		}
		if (totalBytes <= 0)
			return DEFAULT_VERTEX_MEMORY_BYTES;
		long bytes = (long) (totalBytes * VIDEO_MEMORY_SHARE);
		if (availableBytes >= 0) {
			long reserve = Math.max(MIN_VIDEO_MEMORY_RESERVE_BYTES, (long) (totalBytes * VIDEO_MEMORY_RESERVE_SHARE));
			bytes = Math.min(bytes, availableBytes - reserve);
		}
		return Math.max(MIN_VERTEX_MEMORY_BYTES, Math.min(MAX_VERTEX_MEMORY_BYTES, bytes));
	}

	public long getGeometryMemorySize()
	{
		long total = 0;
		for (Chunk c : chunks.asMap().values())
		{
			total += c.getGeometryMemorySize();
		}
		return total;
	}
        
        @Override
        protected void unloadInvalidatedChunk(Chunk chunk)
        {
                chunk.unloadGeometry();
        }	
}
