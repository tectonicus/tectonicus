/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.texture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ZipStack
{
	private List<File> modJars;
	private String baseFileName;
	private FileSystem base, override;
	
	public ZipStack(File baseFile, File overrideFile, List<File> modJars) throws IOException
	{
		baseFileName = baseFile.getPath();
		base = FileSystems.newFileSystem(Paths.get(baseFileName), null);
		
		if (overrideFile != null)
		{
			if (overrideFile.exists())
			{
				override = FileSystems.newFileSystem(Paths.get(overrideFile.getPath()), null);
			}
			else
				System.out.println("Couldn't open \""+overrideFile.getAbsolutePath()+"\"");
		}
		
		this.modJars = modJars;
	}
	
	public InputStream getStream(String path) throws IOException
	{
		if (override != null && hasFileFS(path, override))
		{
			return Files.newInputStream(override.getPath(path));
		}
		else if (hasFileFS(path, base))
		{
			return Files.newInputStream(base.getPath(path));
		}
		else
		{
			for (File jar : modJars)
			{
				FileSystem fs = FileSystems.newFileSystem(Paths.get(jar.getPath()), null);
				if (hasFileFS(path, fs))
					return Files.newInputStream(fs.getPath(path));
			}
			
			return null;
		}
	}
	
	public boolean hasFile(String file)
	{
		return hasFileFS(file, override) || hasFileFS(file, base);  //TODO: Maybe need to check mod jar files too?
	}
	
	public boolean hasFileFS(String file, FileSystem fs)
	{
		if (fs == null)
			return false;
		else
			return Files.exists(fs.getPath(file));
	}
	
	public String getBaseFileName()
	{
		return baseFileName;
	}
}
