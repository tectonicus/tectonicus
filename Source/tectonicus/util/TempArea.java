/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import java.io.File;

public class TempArea
{
	private int counter;
	private File baseDir;
	
	public TempArea(File baseDir)
	{
		this.baseDir = baseDir;
		
		FileUtils.deleteDirectory(baseDir);
		FileUtils.ensureExists(baseDir);
	}
	
	public File generateTempFile(String prefix, String extension)
	{
		if (extension.charAt(0) != '.')
			extension = "." + extension;
		
		for (;;)
		{
			counter++;
			
			File temp = new File(baseDir, prefix+"_"+counter+extension);
			if (!temp.exists())
				return temp;
		}
	}
}
