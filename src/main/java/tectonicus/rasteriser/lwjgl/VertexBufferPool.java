/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.rasteriser.lwjgl;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Places mesh vertex data in a few large vertex buffers instead of giving each mesh its own.
 * <p>
 * Chunk geometry is built and dropped all the time, tens of thousands of meshes of a few kilobytes each.
 * With one buffer per mesh the driver packs them into its own blocks and can only return a block once all
 * of it is free, which random eviction rarely allows, so its video memory use keeps growing until it spills
 * into shared system memory. Here the large buffers are created once, up to a limit, and reused; video memory
 * for vertices never exceeds that limit. A mesh that does not fit is drawn from system memory instead.
 * <p>
 * Allocation uploads through OpenGL and must happen on the rendering thread; freeing may happen anywhere.
 */
@Slf4j
final class VertexBufferPool {
	static final long SLAB_BYTES = 32L * 1024 * 1024;
	/** Start offsets are aligned so vertex attributes stay aligned and small gaps are less likely. */
	static final int ALIGNMENT = 256;

	record Allocation(Slab slab, long offset, long bytes) {
		int buffer() {
			return slab.buffer;
		}
	}

	static final class Slab {
		private final int buffer;
		private final RangeAllocator ranges;

		private Slab(int buffer, long size) {
			this.buffer = buffer;
			this.ranges = new RangeAllocator(size);
		}
	}

	private final List<Slab> slabs = new ArrayList<>();
	private long limitBytes;
	private long allocations;
	private long failedAllocations;
	private boolean slabCreationFailed;

	VertexBufferPool(long limitBytes) {
		this.limitBytes = limitBytes;
	}

	/** Sets how much video memory the pool may reserve. Buffers already created are kept and reused. */
	synchronized void setLimitBytes(long limitBytes) {
		this.limitBytes = limitBytes;
	}

	synchronized long getLimitBytes() {
		return limitBytes;
	}

	static long align(long bytes) {
		return (bytes + ALIGNMENT - 1) / ALIGNMENT * ALIGNMENT;
	}

	/**
	 * Copies {@code vertices} (from position to limit) into a pooled buffer.
	 *
	 * @return where the data was placed, or null when there is no room and the caller should keep drawing
	 * from system memory
	 */
	synchronized Allocation allocate(ByteBuffer vertices) {
		final long bytes = align(vertices.remaining());
		if (bytes == 0 || bytes > SLAB_BYTES) {
			failedAllocations++;
			return null;
		}
		for (Slab slab : slabs) {
			long offset = slab.ranges.allocate(bytes);
			if (offset >= 0)
				return upload(slab, offset, bytes, vertices);
		}
		Slab slab = createSlab();
		if (slab == null) {
			failedAllocations++;
			return null;
		}
		return upload(slab, slab.ranges.allocate(bytes), bytes, vertices);
	}

	private Allocation upload(Slab slab, long offset, long bytes, ByteBuffer vertices) {
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, slab.buffer);
		try {
			GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, offset, vertices);
		} finally {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		}
		allocations++;
		return new Allocation(slab, offset, bytes);
	}

	private Slab createSlab() {
		if (slabCreationFailed || (slabs.size() + 1) * SLAB_BYTES > limitBytes)
			return null;
		int buffer = GL15.glGenBuffers();
		if (buffer == 0)
			return null;
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
		try {
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, SLAB_BYTES, GL15.GL_STATIC_DRAW);
			if (GL15.glGetBufferParameteri(GL15.GL_ARRAY_BUFFER, GL15.GL_BUFFER_SIZE) != SLAB_BYTES) {
				// Out of video memory: stop trying, the remaining meshes are drawn from system memory.
				GL15.glDeleteBuffers(buffer);
				slabCreationFailed = true;
				log.warn("Could not allocate a {} MB vertex buffer after {} MB; further geometry is drawn from system memory",
						SLAB_BYTES >> 20, getReservedBytes() >> 20);
				return null;
			}
		} finally {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		}
		Slab slab = new Slab(buffer, SLAB_BYTES);
		slabs.add(slab);
		return slab;
	}

	synchronized void free(Allocation allocation) {
		// After destroy() the buffer is gone and its ranges no longer matter.
		if (!slabs.contains(allocation.slab))
			return;
		allocation.slab.ranges.free(allocation.offset, allocation.bytes);
		allocations--;
	}

	/** Video memory held by the pool's buffers. */
	synchronized long getReservedBytes() {
		return slabs.size() * SLAB_BYTES;
	}

	/** Bytes of the pool's buffers given out to meshes, including alignment padding. */
	synchronized long getUsedBytes() {
		long used = 0;
		for (Slab slab : slabs)
			used += slab.ranges.usedBytes();
		return used;
	}

	synchronized long getAllocationCount() {
		return allocations;
	}

	/** How many meshes could not be placed and are drawn from system memory. */
	synchronized long getFailedAllocationCount() {
		return failedAllocations;
	}

	/** Deletes the pool's buffers. Meshes still using them must not be drawn afterwards. */
	synchronized void destroy() {
		for (Slab slab : slabs)
			GL15.glDeleteBuffers(slab.buffer);
		slabs.clear();
		allocations = 0;
	}
}
