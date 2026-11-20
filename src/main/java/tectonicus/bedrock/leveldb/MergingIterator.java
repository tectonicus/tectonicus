/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock.leveldb;

import java.io.IOException;
import java.util.List;
import java.util.PriorityQueue;

/** K-way merge of sorted child iterators into a single internal-key-ordered stream. */
final class MergingIterator implements InternalIterator {
	private final List<InternalIterator> children;
	private final PriorityQueue<InternalIterator> heap;
	private InternalIterator current;

	MergingIterator(List<InternalIterator> children) {
		this.children = children;
		this.heap = new PriorityQueue<>(Math.max(1, children.size()), (a, b) -> InternalKeys.compare(a.key(), b.key()));
	}

	@Override
	public boolean isValid() {
		return current != null;
	}

	@Override
	public void seekToFirst() throws IOException {
		heap.clear();
		for (InternalIterator child : children) {
			child.seekToFirst();
			if (child.isValid()) {
				heap.add(child);
			}
		}
		current = heap.poll();
	}

	@Override
	public void seek(byte[] target) throws IOException {
		heap.clear();
		for (InternalIterator child : children) {
			child.seek(target);
			if (child.isValid()) {
				heap.add(child);
			}
		}
		current = heap.poll();
	}

	@Override
	public void next() throws IOException {
		current.next();
		if (current.isValid()) {
			heap.add(current);
		}
		current = heap.poll();
	}

	@Override
	public byte[] key() {
		return current.key();
	}

	@Override
	public byte[] value() throws IOException {
		return current.value();
	}

	@Override
	public void close() {
		heap.clear();
		current = null;
		for (InternalIterator child : children) {
			child.close();
		}
	}
}
