/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BlockOcclusion {
	OPEN(false, 1.0f),
	TRANSLUCENT(false, 0.2f),
	OPAQUE(true, 0.2f);

	private final boolean opaque;
	private final float shadeBrightness;
}
