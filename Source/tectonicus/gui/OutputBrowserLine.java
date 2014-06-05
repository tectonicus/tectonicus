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

import tectonicus.configuration.MutableConfiguration;

public class OutputBrowserLine
{
	private FileBrowserLine line;
	
	private ControlModifiedListener modifiedListener;
	
	public OutputBrowserLine(Frame parent)
	{
		// TODO: Remember last output directory?
		// ..
		
		line = new FileBrowserLine(parent, "Output folder:", "Open", new File(""), JFileChooser.DIRECTORIES_ONLY, null);
		line.setFileListener( new FileChangedHandler() );
		
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
	
	public boolean isOk()
	{
		return line.getFile() != null;
	}
	
	public void apply(MutableConfiguration config)
	{
		config.setOutputDir( line.getFile() );
	}
	
	private class FileChangedHandler implements FileLineListener
	{
		@Override
		public void onFileChanged(File newFile)
		{
			if (modifiedListener != null)
				modifiedListener.onControlModified();
		}
	}
	
}
