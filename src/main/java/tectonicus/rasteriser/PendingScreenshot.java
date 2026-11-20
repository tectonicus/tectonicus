/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser;

import java.awt.image.BufferedImage;

/**
 * A screenshot whose pixels may still be on their way from the GPU.
 * Call {@link #get()} on the rendering thread; it waits for the pixels only if they have not arrived yet.
 */
@FunctionalInterface
public interface PendingScreenshot {
	/** The captured image, or null if it could not be read. Repeated calls return the same image. */
	BufferedImage get();
}
