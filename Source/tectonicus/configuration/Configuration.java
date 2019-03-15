/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface Configuration
{
	@Getter
	@RequiredArgsConstructor
	enum Mode
	{
		CMD("Command Line"),
		GUI("Gui"),
		INTERACTIVE("Interactive"),
		PLAYERS("Export Players"),
		VIEWS("Render Views"),
		PROFILE("Profile");

		private final String name;
	}
	
	enum RasteriserType
	{
		LWJGL,
		PROCESSING,
		JPCT
	}
	
	enum RenderStyle
	{
		Regular,
		Cave,
		ExploredCaves,
		Nether
	}
	
	enum Dimension
	{
		Terra,
		Nether,
		Ender
	}
	
	void printActive();
	
	Mode getMode();
	
	RasteriserType getRasteriserType();
	
	boolean extractLwjglNatives();
	
	boolean eraseOutputDir();
	
	File outputDir();
	File cacheDir();
	
	boolean useCache();
	
	File getLogFile();
	
	File minecraftJar();
	
	File texturePack();
	
	String getOutputHtmlName();
	
	String getDefaultSkin();
	
	int colourDepth();
	
	int alphaBits();
	
	int numSamples();
	
	int numZoomLevels();
	
	int maxTiles();
	
	boolean showSpawn();
	
	boolean areSignsInitiallyVisible();
	
	boolean arePlayersInitiallyVisible();
	
	boolean arePortalsInitiallyVisible();
	
	boolean areBedsInitiallyVisible();
	
	boolean isSpawnInitiallyVisible();
	
	boolean areViewsInitiallyVisible();
	
	boolean isVerbose();
	
	boolean forceLoadAwt();
	
	boolean force32BitNatives();
	
	boolean force64BitNatives();
	
	int tileSize();
	
	int getNumDownsampleThreads();
	
	String getSinglePlayerName();

	Path getUpdateToLeaflet();
	
	int numMaps();
	Map getMap(final int index);
	List<Map> getMaps();
}
