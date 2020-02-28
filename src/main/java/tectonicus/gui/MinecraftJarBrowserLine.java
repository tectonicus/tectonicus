/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.gui;

import java.awt.Frame;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import tectonicus.Minecraft;
import tectonicus.configuration.MutableConfiguration;

public class MinecraftJarBrowserLine
{
	private FileBrowserLine line;
	
	private ControlModifiedListener modifiedListener;
	
	public MinecraftJarBrowserLine(Frame parent)
	{
		// Auto fill this with the found minecraft jar
		File minecraftJar = Minecraft.findMinecraftJar();

		// TODO: Handle minecraft jar not findable
		// ..
		
		line = new FileBrowserLine(parent, "Minecraft.jar:", "Open", minecraftJar, JFileChooser.FILES_ONLY, new JarFileFilter());
		line.setFileListener( new FileChangedHandler() );
		
		onInternalFileChanged();
	}
	
	public JPanel getPanel()
	{
		return line;
	}
	
	public void setEnabled(final boolean enabled)
	{
		this.line.setEnabled(enabled);
	}

	public void addModifiedListener(ControlModifiedListener listener)
	{
		this.modifiedListener = listener;
	}
	
	private void onInternalFileChanged()
	{
		if (isOk())
		{
			line.setInfo("Minecraft.jar ok");
		}
		else
		{
			line.setInfo("This does not look like a valid minecraft.jar!");
		}
		
		if (modifiedListener != null)
			modifiedListener.onControlModified();
	}
	
	public boolean isOk()
	{
		return Minecraft.isValidMinecraftJar(line.getFile());
	}
	
	public void apply(MutableConfiguration config)
	{
		config.setMinecraftJar( line.getFile() );
	}
	
	private static class JarFileFilter extends FileFilter
	{
		@Override
		public boolean accept(File file)
		{
			if (file.isFile())
			{
				if (file.getName().endsWith(".jar"))
					return Minecraft.isValidMinecraftJar(file);
				else
					return false;
			}
			
			return true;
		}
		
		@Override
		public String getDescription()
		{
			return "Jar file (*.jar)";
		}
	}
	
	private class FileChangedHandler implements FileLineListener
	{
		@Override
		public void onFileChanged(File newFile)
		{
			onInternalFileChanged();
		}
	}
}
