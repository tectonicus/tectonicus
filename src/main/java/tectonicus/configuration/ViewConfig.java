/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ViewConfig {
	public static final int DEFAULT_WIDTH = 1920;
	public static final int DEFAULT_HEIGHT = 1080;
	private ImageFormat imageFormat = ImageFormat.PNG;
	private float imageCompressionLevel = 0.75f;
	private int viewDistance = 100;
	private int fov = 70;
	private int width = DEFAULT_WIDTH;
	private int height = DEFAULT_HEIGHT;

	public int getFOV() {
		return fov;
	}

	public void setFOV(int fov) {
		this.fov = fov;
	}
}
