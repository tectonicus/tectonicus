/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import tectonicus.TectonicusApp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@Slf4j
@UtilityClass
public class FileUtils
{
	@Getter
	private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public boolean deleteDirectory(File path)
	{
		if (!path.exists())
			return true;

		File[] files = path.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				deleteDirectory(file);
			} else {
				file.delete();
			}
		}

		return path.delete();
	}
	
	public void ensureExists(File dir)
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
	public void copyFiles(File src, File dest, Set<String> excludeExtensions) throws IOException
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
		if (src.isDirectory()) {
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
			for (String s : list) {
				File dest1 = new File(dest, s);
				File src1 = new File(src, s);
				copyFiles(src1, dest1, excludeExtensions);
			}
		} else {
			String extension = getExtension(src.getName());
			if (!excludeExtensions.contains(extension))
			{
				// This was not a directory, so lets just copy the file
				try
				{
					Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
			} else {
				log.warn("Skipping {}", src.getAbsolutePath());
			}
		}
	}
	
	public void extractResource(String resource, File outputFile)
	{
		try
		{
			Files.copy(TectonicusApp.class.getClassLoader().getResourceAsStream(resource), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean isJSONValid(String json)
	{
            // Check if the string starts and ends with curly braces or square brackets
            if ((json.startsWith("{") && json.endsWith("}")) || 
                (json.startsWith("[") && json.endsWith("]"))) {
                    try {
                            OBJECT_MAPPER.readTree(json);
                            return true;
                    } catch (Exception e) {
                            return false;
                    }
            } else {
                    return false; // Not a JSON object or array
            }
	}
	
	public String getExtension(String file)
	{
		final int dotPos = file.lastIndexOf('.');
		return file.substring(dotPos+1, file.length());
	}
}
