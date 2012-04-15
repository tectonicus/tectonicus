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
