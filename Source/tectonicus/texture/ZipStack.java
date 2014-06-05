/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipStack
{
	private ZipFile base;
	private ZipFile override;
	
	public ZipStack(File baseFile, File overrideFile) throws IOException
	{
		base = new ZipFile(baseFile);
		
		if (overrideFile != null)
		{
			if (overrideFile.exists())
				override = new ZipFile(overrideFile);
			else
				System.out.println("Couldn't open \""+overrideFile.getAbsolutePath()+"\"");
		}
	}
	
	public ZipStackEntry getEntry(String path)
	{
		ZipStackEntry first = getEntry(override, path);
		if (first != null)
			return first;
		
		return getEntry(base, path);
	}
	
	public ZipStackEntry getEntry(ZipFile zip, String path)
	{
		if (zip == null)
			return null;
		
		// Strip off any leading slashes
		if (path.charAt(0) == '\\' || path.charAt(0) == '/')
			path = path.substring(1);
		
		ZipEntry entry;
		
		entry = zip.getEntry(path);
		if (entry != null)
			return new ZipStackEntry(zip, entry);
		
		entry = zip.getEntry("/"+path);
		if (entry != null)
			return new ZipStackEntry(zip, entry);
		
		entry = zip.getEntry("\\"+path);
		if (entry != null)
			return new ZipStackEntry(zip, entry);
		
		return null;
	}
	
	public static class ZipStackEntry
	{
		public final ZipFile file;
		public final ZipEntry entry;
		
		public ZipStackEntry(ZipFile f, ZipEntry e)
		{
			if (f == null)
				throw new RuntimeException("file cannot be null");
			if (e == null)
				throw new RuntimeException("entry cannot be null");
			
			this.file = f;
			this.entry = e;
		}
		
		public InputStream getInputStream() throws IOException
		{
			return file.getInputStream(entry);
		}
	}
}
