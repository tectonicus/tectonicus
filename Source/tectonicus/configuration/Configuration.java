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
package tectonicus.configuration;

import java.io.File;
import java.util.List;

public interface Configuration
{
	public enum Mode
	{
		CommandLine,
		Gui,
		Interactive,
		ExportPlayers,
		RenderViews,
		Profile;
	}
	
	public enum RasteriserType
	{
		Lwjgl,
		Processing,
		Jpct
	};
	
	public enum RenderStyle
	{
		Regular,
		Cave,
		ExploredCaves,
		Nether
	};
	
	public enum Dimension
	{
		Terra,
		Nether,
		Ender
	}
	
	public void printActive();
	
	public Mode mode();
	
	public RasteriserType getRasteriserType();
	
	public boolean extractLwjglNatives();
	
	public boolean eraseOutputDir();
	
	public File outputDir();
	public File cacheDir();
	
	public boolean useCache();
	
	public File getLogFile();
	
	public File minecraftJar();
	
	public File texturePack();
	
	public String getOutputHtmlName();
	
	public int colourDepth();
	
	public int alphaBits();
	
	public int numSamples();
	
	public int numZoomLevels();
	
	public int maxTiles();
	
	public boolean showSpawn();
	
	public boolean areSignsInitiallyVisible();
	
	public boolean arePlayersInitiallyVisible();
	
	public boolean arePortalsInitiallyVisible();
	
	public boolean areBedsInitiallyVisible();
	
	public boolean isSpawnInitiallyVisible();
	
	public boolean areViewsInitiallyVisible();
	
	public boolean isVerbose();
	
	public boolean forceLoadAwt();
	
	public boolean force32BitNatives();
	
	public boolean force64BitNatives();
	
	public int tileSize();
	
	public int getNumDownsampleThreads();
	
	public String getSinglePlayerName();
	
	public int numMaps();
	public Map getMap(final int index);
	public List<Map> getMaps();
}
