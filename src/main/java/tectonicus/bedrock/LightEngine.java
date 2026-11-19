/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import tectonicus.bedrock.BedrockBlockMapper.JavaState;
import tectonicus.raw.RawChunk;

/**
 * Computes sky and block light for a chunk, which Bedrock does not save, the way Java does: sky light falls straight
 * down at full strength until something dims it, then both kinds spread to neighbouring blocks, losing at least one
 * level per block. Light reaches up to 15 blocks, so the chunks around it are included. A missing neighbour is open
 * air, so terrain at the edge of the explored world is lit from the side.
 */
final class LightEngine {
	private static final int MARGIN = 16;
	private static final int SIZE = 16 + 2 * MARGIN; // 48 blocks across

	private final int height;
	private final byte[] opacity;
	private final byte[] sky;
	private final byte[] block;
	private int[] queue = new int[4096];
	private int head;
	private int tail;

	private LightEngine(int height) {
		this.height = height;
		int cells = SIZE * SIZE * height;
		opacity = new byte[cells];
		sky = new byte[cells];
		block = new byte[cells];
	}

	/**
	 * Lights a chunk.
	 *
	 * @param hasSky false for the Nether and the End, which have no sky light
	 */
	static void light(RawChunk chunk, Neighbourhood blocks, boolean hasSky) {
		int top = 0;
		for (int dz = -1; dz <= 1; dz++)
			for (int dx = -1; dx <= 1; dx++) {
				Blocks neighbour = blocks.chunk(dx, dz);
				if (neighbour != null)
					top = Math.max(top, neighbour.top());
			}
		int chunkHeight = blocks.centre().sections().length * 16;
		// Everything above the highest block is in full sky light; only light below it, plus one block of headroom.
		int height = Math.min(chunkHeight, top + 1);
		if (height <= 0)
			return;
		LightEngine engine = new LightEngine(height);
		engine.load(blocks);
		if (hasSky)
			engine.sky();
		engine.blockLight();
		engine.store(chunk, hasSky, chunkHeight);
	}

	private int index(int x, int y, int z) {
		return (y * SIZE + z) * SIZE + x;
	}

	private void load(Neighbourhood blocks) {
		for (int dz = -1; dz <= 1; dz++)
			for (int dx = -1; dx <= 1; dx++) {
				Blocks chunk = blocks.chunk(dx, dz);
				if (chunk == null)
					continue;
				int baseX = (dx + 1) * 16;
				int baseZ = (dz + 1) * 16;
				for (int section = 0; section < chunk.sections().length && section * 16 < height; section++) {
					JavaState[] states = chunk.sections()[section];
					if (states == null)
						continue;
					for (int x = 0; x < 16; x++)
						for (int z = 0; z < 16; z++)
							for (int y = 0; y < 16 && section * 16 + y < height; y++) {
								JavaState state = states[RawChunk.sectionIndex(x, y, z)];
								int i = index(baseX + x, section * 16 + y, baseZ + z);
								opacity[i] = (byte) state.opacity();
								block[i] = (byte) state.emission();
							}
				}
			}
	}

	private void sky() {
		// Straight down from the sky: full light until a block dims it
		for (int x = 0; x < SIZE; x++)
			for (int z = 0; z < SIZE; z++) {
				int light = 15;
				for (int y = height - 1; y >= 0 && light > 0; y--) {
					int i = index(x, y, z);
					int o = opacity[i];
					if (o > 0)
						light = Math.max(0, light - o);
					sky[i] = (byte) light;
				}
			}
		// Then sideways and down into overhangs and caves, from blocks next to darker ones
		head = tail = 0;
		for (int y = 0; y < height; y++)
			for (int z = 0; z < SIZE; z++)
				for (int x = 0; x < SIZE; x++) {
					int i = index(x, y, z);
					int light = sky[i];
					if (light > 1 && hasDarkerNeighbour(sky, x, y, z, light))
						push(i);
				}
		spread(sky);
	}

	private boolean hasDarkerNeighbour(byte[] light, int x, int y, int z, int level) {
		return x > 0 && light[index(x - 1, y, z)] < level - 1 || x < SIZE - 1 && light[index(x + 1, y, z)] < level - 1
				|| z > 0 && light[index(x, y, z - 1)] < level - 1 || z < SIZE - 1 && light[index(x, y, z + 1)] < level - 1
				|| y > 0 && light[index(x, y - 1, z)] < level - 1;
	}

	private void blockLight() {
		head = tail = 0;
		for (int i = 0; i < block.length; i++)
			if (block[i] > 1)
				push(i);
		spread(block);
	}

	private void spread(byte[] light) {
		int layer = SIZE * SIZE;
		while (head != tail) {
			int i = queue[head];
			head = (head + 1) % queue.length;
			int level = light[i];
			int x = i % SIZE;
			int z = (i / SIZE) % SIZE;
			int y = i / layer;
			if (x > 0) offer(light, i - 1, level);
			if (x < SIZE - 1) offer(light, i + 1, level);
			if (z > 0) offer(light, i - SIZE, level);
			if (z < SIZE - 1) offer(light, i + SIZE, level);
			if (y > 0) offer(light, i - layer, level);
			if (y < height - 1) offer(light, i + layer, level);
		}
	}

	private void offer(byte[] light, int i, int level) {
		int o = opacity[i];
		if (o >= 15)
			return;
		int next = level - Math.max(1, o);
		if (next > light[i]) {
			light[i] = (byte) next;
			if (next > 1)
				push(i);
		}
	}

	private void push(int i) {
		queue[tail] = i;
		tail = (tail + 1) % queue.length;
		if (tail == head) {
			// Grow the ring buffer, unrolling it so the queue starts at 0
			int[] bigger = new int[queue.length * 2];
			int first = queue.length - head;
			System.arraycopy(queue, head, bigger, 0, first);
			System.arraycopy(queue, 0, bigger, first, head);
			head = 0;
			tail = queue.length;
			queue = bigger;
		}
	}

	private void store(RawChunk chunk, boolean hasSky, int chunkHeight) {
		int sections = Math.min(chunkHeight, chunk.getSectionCount() * 16) / 16;
		for (int section = 0; section < sections; section++) {
			// A section that is not stored reads as full sky light and no block light, so only add one when that is wrong
			if (!chunk.hasSection(section) && !needsSection(section, hasSky))
				continue;
			for (int x = 0; x < 16; x++)
				for (int z = 0; z < 16; z++)
					for (int dy = 0; dy < 16; dy++) {
						int y = section * 16 + dy;
						chunk.setSkyLight(x, y, z, (byte) skyAt(x, y, z, hasSky));
						chunk.setBlockLight(x, y, z, (byte) blockAt(x, y, z));
					}
		}
	}

	private boolean needsSection(int section, boolean hasSky) {
		for (int x = 0; x < 16; x++)
			for (int z = 0; z < 16; z++)
				for (int dy = 0; dy < 16; dy++) {
					int y = section * 16 + dy;
					if (blockAt(x, y, z) != 0 || hasSky && skyAt(x, y, z, true) != 15)
						return true;
				}
		return false;
	}

	private int skyAt(int x, int y, int z, boolean hasSky) {
		if (!hasSky)
			return 0;
		return y < height ? sky[index(MARGIN + x, y, MARGIN + z)] : 15;
	}

	private int blockAt(int x, int y, int z) {
		return y < height ? block[index(MARGIN + x, y, MARGIN + z)] : 0;
	}
}
