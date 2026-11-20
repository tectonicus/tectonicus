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

/**
 * Iterates a sequence of non-overlapping table files in key order (a level &gt;= 1, or a single level-0
 * file), holding at most one table open at a time.
 */
final class LevelIterator implements InternalIterator {
	private final TableCache tableCache;
	private final List<FileMetaData> files;
	private final boolean fillCache;
	private int fileIndex;
	private Table table;
	private Table.Iter tableIter;

	LevelIterator(TableCache tableCache, List<FileMetaData> files, boolean fillCache) {
		this.tableCache = tableCache;
		this.files = files;
		this.fillCache = fillCache;
		this.fileIndex = files.size();
	}

	@Override
	public boolean isValid() {
		return tableIter != null && tableIter.isValid();
	}

	@Override
	public void seekToFirst() throws IOException {
		openFile(0);
		if (tableIter != null) {
			tableIter.seekToFirst();
		}
		skipExhausted();
	}

	@Override
	public void seek(byte[] target) throws IOException {
		openFile(findFile(target));
		if (tableIter != null) {
			tableIter.seek(target);
		}
		skipExhausted();
	}

	@Override
	public void next() throws IOException {
		tableIter.next();
		skipExhausted();
	}

	@Override
	public byte[] key() {
		return tableIter.key();
	}

	@Override
	public byte[] value() {
		return tableIter.value();
	}

	@Override
	public void close() {
		closeTable();
		fileIndex = files.size();
	}

	/** Index of the first file whose largest key is {@code >= target}, or {@code files.size()}. */
	private int findFile(byte[] target) {
		int lo = 0;
		int hi = files.size();
		while (lo < hi) {
			int mid = (lo + hi) >>> 1;
			if (InternalKeys.compare(files.get(mid).largest(), target) < 0) {
				lo = mid + 1;
			} else {
				hi = mid;
			}
		}
		return lo;
	}

	private void skipExhausted() throws IOException {
		while (tableIter != null && !tableIter.isValid()) {
			openFile(fileIndex + 1);
			if (tableIter != null) {
				tableIter.seekToFirst();
			}
		}
	}

	private void openFile(int index) throws IOException {
		closeTable();
		fileIndex = index;
		if (index >= files.size()) {
			return;
		}
		table = tableCache.acquire(files.get(index).number());
		tableIter = table.iterator(fillCache);
	}

	private void closeTable() {
		if (tableIter != null) {
			tableIter.close();
			tableIter = null;
		}
		if (table != null) {
			table.release();
			table = null;
		}
	}
}
