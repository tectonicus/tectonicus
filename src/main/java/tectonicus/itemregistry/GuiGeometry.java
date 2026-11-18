/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */
package tectonicus.itemregistry;

import org.joml.Matrix4f;
import tectonicus.rasteriser.Rasteriser;

/** Applies the GUI pose as vertices are emitted, so transparent sorting uses the posed geometry too. */
public class GuiGeometry extends PosedGeometry {
	public GuiGeometry(Rasteriser rasteriser, Matrix4f transform, GuiLighting lighting) {
		super(rasteriser, transform, lighting::brightness);
	}
}
