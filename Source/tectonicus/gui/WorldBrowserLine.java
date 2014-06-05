/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
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

import tectonicus.Minecraft;
import tectonicus.configuration.MutableConfiguration;

public class WorldBrowserLine
{
	private FileBrowserLine line;
	
	private ControlModifiedListener modifiedListener;
	
	public WorldBrowserLine(Frame parent)
	{
		// Try and find a single player world to default to
		File worldDir = null;
		for (int i=0; i<5; i++)
		{
			File dir = Minecraft.findWorldDir(i);
			if (Minecraft.isValidWorldDir(dir))
			{
				worldDir = dir;
				break;
			}
		}
		
		line = new FileBrowserLine(parent, "World: ", "Open", worldDir, JFileChooser.DIRECTORIES_ONLY, null);		
		line.setFileListener( new FileChangedHandler() );

		onInternalFileChanged();
	}
	
	public void addModifiedListener(ControlModifiedListener listener)
	{
		this.modifiedListener = listener;
	}
	
	public JPanel getPanel()
	{
		return line;
	}
	
	public void setEnabled(final boolean enabled)
	{
		this.line.setEnabled(enabled);
	}
	
	public boolean isOk()
	{
		File worldDir = line.getFile();
		return Minecraft.isValidWorldDir(worldDir);
	}
	
	public void apply(MutableConfiguration config)
	{
		File worldDir = line.getFile();
		config.getMap(0).setWorldDir(worldDir);
	}
	
	private void onInternalFileChanged()
	{
		if (line.getFile() == null)
		{
			line.setInfo("Choose a world to map");
		}
		else if (isOk())
		{
			line.setInfo("World folder ok");
		}
		else
		{
			line.setInfo("Not a minecraft world folder!");
		}
		
		if (modifiedListener != null)
			modifiedListener.onControlModified();
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
