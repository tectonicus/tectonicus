/*
 * Copyright (c) 2025 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser.lwjgl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tectonicus.rasteriser.Texture;

@RequiredArgsConstructor
@Getter
public class LwjglTexture implements Texture {
	private final int width;
	private final int height;
	private final int id;
}
