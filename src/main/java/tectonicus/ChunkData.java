/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jnbt.NBTInputStream.Compression;

@Getter
@RequiredArgsConstructor
public class ChunkData {
	private final byte[] bytes;
	private final Compression compressionType;
}
