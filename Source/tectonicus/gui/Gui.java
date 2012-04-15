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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.MessageDigest;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import tectonicus.ProgressListener;
import tectonicus.TileRenderer;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.MutableConfiguration;
import tectonicus.configuration.MutableLayer;
import tectonicus.configuration.MutableMap;

// GUI todo list:
//
// 1. Get the basics working as soon as possible:
//		- browse to find world dir
//		- browse to specify output dir
//		- auto-detect minecraft.jar location
//		- 'start' button
//		- disable everything while rendering
//		- pop up dialog when finished

// Proper progress bar

// Progress bar needs some kind of animation to show it's still working and not frozen

// Remember last export dir

public class Gui
{
	private final MessageDigest hashAlgorithm;
	
	private JFrame frame;
	
	private JPanel topPanel;
	private JPanel infoPanel;
	private JPanel simplePanel;
	private JPanel advancedPanel;
	private JPanel togglePanel;

	// Top info panel
	private JLabel infoText;
	
	// Simple options
	private WorldBrowserLine worldBrowser;
	private OutputBrowserLine outputBrowser;
	private MinecraftJarBrowserLine minecraftBrowser;
	
	// Bottom row
	private JButton startButton;
	private JButton toggleButton;
	private boolean isSimple;
	
	private TileRendererTask activeRender;
	private Thread workerThread;
	
	public Gui(MessageDigest hashAlgorithm)
	{
		/*
		try
		{
		//	UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			UIManager.setLookAndFeel("com.seaglasslookandfeel.SeaGlassLookAndFeel");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		*/
		
		this.hashAlgorithm = hashAlgorithm;
		
		isSimple = true;
		
		frame = new JFrame("Tectonicus");
		
		topPanel = new JPanel();
		topPanel.setLayout( new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		
		infoPanel = createInfoPanel();
		
		simplePanel = createSimplePanel();
		
		advancedPanel = createAdvancedPanel();
		advancedPanel.setVisible(false);
		
		togglePanel = createTogglePanel();
		
		topPanel.add(infoPanel);
		topPanel.add(simplePanel);
		topPanel.add(advancedPanel);
		topPanel.add(togglePanel);
		
		topPanel.setBorder( new EmptyBorder(8, 8, 8, 8) );
		frame.getContentPane().add(topPanel);
		frame.setMinimumSize( new Dimension(400, 10) );
		frame.pack();
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		internalOnControlModified();
	}
	
	private JPanel createInfoPanel()
	{
		JPanel info = new JPanel();
	
		infoText = new JLabel("Choose map options");
		info.add(infoText);
		
		return info;
	}
	
	private JPanel createSimplePanel()
	{
		JPanel simple = new JPanel();
		simple.setLayout( new BoxLayout(simple, BoxLayout.Y_AXIS));
		
		worldBrowser = new WorldBrowserLine(frame);
		worldBrowser.addModifiedListener( new EnableStartHandler() );
		simple.add( worldBrowser.getPanel() );
		
		outputBrowser = new OutputBrowserLine(frame);
		outputBrowser.addModifiedListener( new EnableStartHandler() );
		simple.add( outputBrowser.getPanel() );
		
		minecraftBrowser = new MinecraftJarBrowserLine(frame);
		minecraftBrowser.addModifiedListener( new EnableStartHandler() );
		simple.add( minecraftBrowser.getPanel() );
		
		return simple;
	}
	
	private static JPanel createAdvancedPanel()
	{
		JPanel advanced = new JPanel();
		
		advanced.add( new JLabel("advanced") );
		
		return advanced;
	}
	
	private JPanel createTogglePanel()
	{
		JPanel toggle = new JPanel();
		toggle.setLayout( new BorderLayout() );
		
		startButton = new JButton("Start!");
		toggle.add(startButton, BorderLayout.EAST);
		startButton.addActionListener( new StartHandler() );
		
		toggleButton = new JButton("Show advanced options");
		toggleButton.addActionListener(new ToggleHandler());
		toggle.add(toggleButton, BorderLayout.WEST);
		
		return toggle;
	}
	
	public void setControlsEnabled(final boolean enabled)
	{
		worldBrowser.setEnabled(enabled);
		outputBrowser.setEnabled(enabled);
		minecraftBrowser.setEnabled(enabled);
		
		startButton.setEnabled(enabled);
		toggleButton.setEnabled(enabled);
	}
	
	public void display()
	{
		frame.setVisible(true);
	}
	
	private class ToggleHandler implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (isSimple)
			{
				toggleButton.setText("Hide advanced options");
				advancedPanel.setVisible(true);
			}
			else
			{
				toggleButton.setText("Show advanced options");
				advancedPanel.setVisible(false);
			}
			frame.pack();
			isSimple = !isSimple;
		}
	}
	
	private class StartHandler implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (activeRender == null)
			{
				// Disable all controls
				setControlsEnabled(false);
				
				MutableConfiguration config = new MutableConfiguration();
				MutableMap map = new MutableMap("Map0");
				MutableLayer layer = new MutableLayer("LayerA", map.getId());
				map.addLayer(layer);
				config.addMap(map);
				
				worldBrowser.apply(config);
				outputBrowser.apply(config);
				minecraftBrowser.apply(config);
				
				// Enable caching
				config.setUseCache(true);
				config.setCacheDir( new File(config.outputDir(), "config") );
				
				// For testing
			//	config.setMaxTiles(10);
				
				// Perform the rendering in a background thread
				activeRender = new TileRendererTask(config);
				workerThread = new Thread(activeRender, "Tile renderer thread");
				workerThread.start();
				
				startButton.setEnabled(true);
				startButton.setText("Stop");
			}
			else
			{
				// Abort the renderer
				activeRender.abort();
			}
		}
	}
	
	private class TileRendererTask implements Runnable
	{
		private final Configuration config;
		private TileRenderer renderer;
		
		public TileRendererTask(Configuration config)
		{
			this.config = config;
		}
		
		@Override
		public void run()
		{
			try
			{
				ProgressHandler progressHandler = new ProgressHandler();
				
				renderer = new TileRenderer(config, progressHandler, hashAlgorithm);
				
				progressHandler.onTaskStarted(TileRenderer.Task.LoadingWorld.toString());
				
				TileRenderer.Result res = renderer.output();
				
				if (!res.aborted)
				{
					String message = "Rendering complete!\n" +
									 "Map exported to "+res.htmlFile.getAbsolutePath();
					JOptionPane.showMessageDialog(frame, message, "Finished!", JOptionPane.INFORMATION_MESSAGE);
				}
				else
				{
					// Aborted, do nothing?
				}
				
				activeRender = null;
				workerThread = null;
				
				// Reenable all the buttons
				startButton.setText("Start");
				setControlsEnabled(true);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		
		public void abort()
		{
			renderer.abort();
		}
	}
	
	private void internalOnControlModified()
	{
		if (worldBrowser.isOk()
				&& outputBrowser.isOk()
				&& minecraftBrowser.isOk())
		{
			startButton.setEnabled(true);
		}
		else
		{
			startButton.setEnabled(false);
		}		
	}
	
	private class EnableStartHandler implements ControlModifiedListener
	{
		@Override
		public void onControlModified()
		{
			internalOnControlModified();
		}
	}
	
	private class ProgressHandler implements ProgressListener
	{
		private String taskName;
		
		@Override
		public void onTaskStarted(String taskName)
		{
			this.taskName = taskName;
			
			infoText.setText(taskName);
		}
		
		@Override
		public void onTaskUpdate(final int num, final int ofTotalNum)
		{
			infoText.setText(taskName + " ("+num+" of "+ofTotalNum+")");
		}
	}
}
