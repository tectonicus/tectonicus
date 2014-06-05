/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.texture;

import tectonicus.rasteriser.Texture;

public class SubTexture
{
	public String texturePackVersion;
	
	public final Texture texture;
	public final float u0, v0, u1, v1;
	
	public SubTexture(Texture texture, final float u0, final float v0, final float u1, final float v1)
	{
		this.texture = texture;
		
		this.u0 = u0;
		this.v0 = v0;
		this.u1 = u1;
		this.v1 = v1;
	}
}
