/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
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
	
	/** A fresh area in a subfolder, for files that belong to one map. */
	public TempArea subArea(String name)
	{
		return new TempArea(new File(baseDir, name.replaceAll("[^0-9A-Za-z._-]", "_")));
	}
	
	/** Removes the area's folder and every file in it. */
	public void delete()
	{
		FileUtils.deleteDirectory(baseDir);
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
