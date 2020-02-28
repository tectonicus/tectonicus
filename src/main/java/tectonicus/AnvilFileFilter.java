/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.io.File;
import java.io.FilenameFilter;

public class AnvilFileFilter implements FilenameFilter
{
	@Override
	public boolean accept(File dir, String file)
	{
		return file.endsWith(".mca");
	}
}
