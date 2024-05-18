/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import ch.qos.logback.classic.Level;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;

public interface Configuration  //TODO: is this interface needed?
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
		REGULAR,
		CAVE,
		EXPLORED_CAVES,
		NETHER
	}
	
	void printActive();
	
	Mode getMode();
	
	RasteriserType getRasteriserType();
	boolean isUseEGL();
	
	boolean eraseOutputDir();
	
	File getOutputDir();
	File getCacheDir();
	File getWorldDir();
	
	boolean useCache();
	
	Level getLoggingLevel();
	void setLoggingLevel(Level loggingLevel);
	
	File getMinecraftJar();
	void setMinecraftJar(File minecraftJar);
	
	File getTexturePack();
	
	String getOutputHtmlName();
	String getHtmlTitle();
	
	String getDefaultSkin();
	
	int getColourDepth();
	
	int getAlphaBits();
	
	int getNumSamples();
	
	int getNumZoomLevels();
	
	int getMaxTiles();
	
	boolean showSpawn();
	
	boolean areSignsInitiallyVisible();
	
	boolean arePlayersInitiallyVisible();
	
	boolean arePortalsInitiallyVisible();
	
	boolean areBedsInitiallyVisible();

	boolean areRespawnAnchorsInitiallyVisible();
	
	boolean isSpawnInitiallyVisible();
	
	boolean areViewsInitiallyVisible();
	boolean isChestsInitiallyVisible();
	boolean isBeaconsInitiallyVisible();
	
	boolean isVerbose();
	
	boolean forceLoadAwt();

	boolean isUsingProgrammerArt();
	
	int getTileSize();
	
	int getNumDownsampleThreads();
	
	String getSinglePlayerName();
	
	String getCustomStyle();
	
	String getCustomScript();
	
	String getUseCdn();
	
	int numMaps();
	Map getMap(final int index);
	List<Map> getMaps();
}
