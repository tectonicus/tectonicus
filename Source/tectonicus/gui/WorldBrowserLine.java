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
