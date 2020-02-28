/*
 * Copyright (c) 2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.texture;

import lombok.Getter;
import tectonicus.Version;
import tectonicus.rasteriser.Texture;

@Getter
public class SubTexture
{
	public final Version texturePackVersion;
	public final Texture texture;
	public final float u0;
	public final float v0;
	public final float u1;
	public final float v1;
	
	public SubTexture(Texture texture, final float u0, final float v0, final float u1, final float v1)
	{
		this(texture, u0, v0, u1, v1, null);
	}

	public SubTexture(Texture texture, final float u0, final float v0, final float u1, final float v1, Version texturePackVersion)
	{
		this.texture = texture;
		this.u0 = u0;
		this.v0 = v0;
		this.u1 = u1;
		this.v1 = v1;
		this.texturePackVersion = texturePackVersion;
	}
}
