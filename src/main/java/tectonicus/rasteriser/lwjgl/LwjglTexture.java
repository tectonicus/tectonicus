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
import org.lwjgl.opengl.GL11;
import tectonicus.rasteriser.Texture;

import java.util.function.IntSupplier;

@Getter
public class LwjglTexture implements Texture {
	private int id;
	private final int width;
	private final int height;
	private IntSupplier upload;

	public LwjglTexture(int id, int width, int height) {
		this.id = id;
		this.width = width;
		this.height = height;
	}

	LwjglTexture(int width, int height, IntSupplier upload) {
		this(0, width, height);
		this.upload = upload;
	}

	/** Called only on the thread with the current OpenGL context. */
	public int getId() {
		if (upload != null) {
			id = upload.getAsInt();
			upload = null;
		}
		return id;
	}

	void destroy() {
		if (id != 0) {
			GL11.glDeleteTextures(id);
			id = 0;
		}
		upload = null;
	}
}
