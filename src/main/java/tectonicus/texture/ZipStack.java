/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.texture;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class ZipStack
{
	private final List<File> modJars;
	private final String baseFileName;
	private final FileSystem base;
	@Getter
	private String overrideFileName;
	private FileSystem override;
	
	public ZipStack(File baseFile, File overrideFile, List<File> modJars) throws IOException
	{
		baseFileName = baseFile.getPath();
		base = FileSystems.newFileSystem(Paths.get(baseFileName), null);
		
		if (overrideFile != null)
		{
			overrideFileName = overrideFile.getPath();
			if (overrideFile.exists())
			{
				override = FileSystems.newFileSystem(Paths.get(overrideFile.getPath()), null);
			}
			else
				log.error("Couldn't open {}", overrideFile.getAbsolutePath());
		}
		
		this.modJars = modJars;
	}

	public InputStream getStream(String path) throws IOException {
		return getStream(path, true);
	}

	public InputStream getStream(String path, boolean minecraftJarLoaded) throws IOException
	{
		if (hasFile(path, override) && minecraftJarLoaded)
		{
			return Files.newInputStream(override.getPath(path));
		}
		else if (hasFile(path, base))
		{
			return Files.newInputStream(base.getPath(path));
		}
		else
		{
			for (File jar : modJars)
			{
				FileSystem fs = FileSystems.newFileSystem(Paths.get(jar.getPath()), null);
				if (hasFile(path, fs))
					return Files.newInputStream(fs.getPath(path));
			}
			
			return null;
		}
	}
	
	public boolean hasFile(String file)
	{
		return hasFile(file, override) || hasFile(file, base);  //TODO: Maybe need to check mod jar files too?
	}
	
	private boolean hasFile(String file, FileSystem fs)
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
        
        public List<String> listFilesInDirectory(String directoryPath) throws IOException {
                List<String> fileList = new ArrayList<>();
               
                if (hasFile(directoryPath, override))
		{
                        Path directory = override.getPath(directoryPath);
                        if (Files.isDirectory(directory)) {
                                try (var directoryStream = Files.newDirectoryStream(directory)) {
                                        for (Path path : directoryStream) {
                                                fileList.add(path.toString());
                                        }
                                }
                        }
		}
		
                if (hasFile(directoryPath, base))
		{
                        Path directory = base.getPath(directoryPath);
                        if (Files.isDirectory(directory)) {
                                try (var directoryStream = Files.newDirectoryStream(directory)) {
                                        for (Path path : directoryStream) {
                                                if (!fileList.contains(path.toString())) {
                                                        fileList.add(path.toString());
                                                }
                                        }
                                }
                        }
		}

                return fileList;
        }
}
