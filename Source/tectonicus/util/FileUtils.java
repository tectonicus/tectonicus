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
package tectonicus.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import tectonicus.TectonicusApp;

public class FileUtils
{
	public static boolean deleteDirectory(File path)
	{
		if (!path.exists())
			return true;
		
		File[] files = path.listFiles();
		for (int i=0; i<files.length; i++)
		{
			if (files[i].isDirectory())
			{
				deleteDirectory(files[i]);
			}
			else
			{
				files[i].delete();
			}
		}
		
		return path.delete();
	}
	
	public static void ensureExists(File dir)
	{
		if (!dir.exists())
			dir.mkdirs();
	}
	
	/**
	 * This function will copy files or directories from one location to
	 * another. note that the source and the destination must be mutually
	 * exclusive. This function can not be used to copy a directory to a sub
	 * directory of itself. The function will also have problems if the
	 * destination files already exist.
	 * 
	 * @param src
	 *            -- A File object that represents the source for the copy
	 * @param dest
	 *            -- A File object that represnts the destination for the copy.
	 * @throws IOException
	 *             if unable to copy.
	 */
	public static void copyFiles(File src, File dest, Set<String> excludeExtensions) throws IOException
	{
		// Check to ensure that the source is valid...
		if (!src.exists())
		{
			throw new IOException("copyFiles: Can not find source: " + src.getAbsolutePath() + ".");
		}
		else if (!src.canRead())
		{
			// check to ensure we have rights to the source...
			throw new IOException("copyFiles: No right to source: " + src.getAbsolutePath() + ".");
		}
		
		// is this a directory copy?
		if (src.isDirectory())
		{
			if (!dest.exists())
			{ // does the destination already exist?
				// if not we need to make it exist if possible (note this is
				// mkdirs not mkdir)
				if (!dest.mkdirs())
				{
					throw new IOException("copyFiles: Could not create direcotry: " + dest.getAbsolutePath() + ".");
				}
			}
			// get a listing of files...
			String list[] = src.list();
			// copy all the files in the list.
			for (int i = 0; i < list.length; i++)
			{
				File dest1 = new File(dest, list[i]);
				File src1 = new File(src, list[i]);
				copyFiles(src1, dest1, excludeExtensions);
			}
		}
		else
		{
			String extension = getExtension(src.getName());
			if (!excludeExtensions.contains(extension))
			{
				// This was not a directory, so lets just copy the file
				FileInputStream fin = null;
				FileOutputStream fout = null;
				byte[] buffer = new byte[4096]; // Buffer 4K at a time
				
				int bytesRead;
				try
				{
					// open the files for input and output
					fin = new FileInputStream(src);
					fout = new FileOutputStream(dest);
					// while bytesRead indicates a successful read, lets write...
					while ((bytesRead = fin.read(buffer)) >= 0)
					{
						fout.write(buffer, 0, bytesRead);
					}
				}
				catch (IOException e)
				{
					// Error copying file...
					IOException wrapper = new IOException("copyFiles: Unable to copy file: "
															+ src.getAbsolutePath() + "to"
															+ dest.getAbsolutePath() + ".");
					wrapper.initCause(e);
					wrapper.setStackTrace(e.getStackTrace());
					throw wrapper;
				}
				finally
				{
					// Ensure that the files are closed (if they were open).
					if (fin != null)
					{
						fin.close();
					}
					if (fout != null)
					{
						fout.close();
					}
				}
			}
			else
			{
				System.out.println("Skipping "+src.getAbsolutePath());
			}
		}
	}
	
	public static void extractResource(String resource, File outputFile)
	{
		try
		{
			if (outputFile.exists())
				outputFile.delete();
			
			InputStream mcmapIn = TectonicusApp.class.getClassLoader().getResourceAsStream(resource);
			OutputStream out = new FileOutputStream(outputFile);
			
			byte[] buffer = new byte[1024];
			while (true)
			{
				int count = mcmapIn.read(buffer);
				if (count == -1)
					break;
				out.write(buffer, 0, count);
			}
			
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/*
	public static void zipDir(File inputDir, File outputArchive)
	{
		FileOutputStream fOut = null;
		ZipOutputStream zipOut= null;
		try
		{
			fOut = new FileOutputStream(outputArchive);
			zipOut = new ZipOutputStream(fOut);
			zipDir(inputDir.getAbsolutePath(), inputDir.getAbsolutePath(), zipOut);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (zipOut != null)
					zipOut.close();
				if (fOut != null)
					fOut.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}	
	}
	
	private static void zipDir(String baseDir, String dir2zip, ZipOutputStream zos)
	{
		try
		{
			// create a new File object based on the directory we have to zip
			File zipDir = new File(dir2zip);
			
			// get a listing of the directory content
			String[] dirList = zipDir.list();
			byte[] readBuffer = new byte[2156];
			int bytesIn = 0;
			// loop through dirList, and zip the files
			for (int i = 0; i < dirList.length; i++)
			{
				File f = new File(zipDir, dirList[i]);
				if (f.isDirectory())
				{
					// if the File object is a directory, call this
					// function again to add its content recursively
					String filePath = f.getPath();
					zipDir(baseDir, filePath, zos);
					// loop again
					continue;
				}
				// if we reached here, the File object f was not a directory
				// create a FileInputStream on top of f
				FileInputStream fis = new FileInputStream(f);
				
				// create a new zip entry
				String entryPath = f.getPath();
				entryPath = entryPath.substring(baseDir.length());
				
				ZipEntry anEntry = new ZipEntry(entryPath);
				
				// place the zip entry in the ZipOutputStream object
				zos.putNextEntry(anEntry);
				// now write the content of the file to the ZipOutputStream
				while ((bytesIn = fis.read(readBuffer)) != -1)
				{
					zos.write(readBuffer, 0, bytesIn);
				}
				// close the Stream
				fis.close();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	*/
	
	public static String getExtension(String file)
	{
		final int dotPos = file.lastIndexOf('.');
		String ext = file.substring(dotPos+1, file.length());
		return ext;
	}
}
