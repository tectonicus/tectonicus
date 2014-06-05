/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
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
