/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
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