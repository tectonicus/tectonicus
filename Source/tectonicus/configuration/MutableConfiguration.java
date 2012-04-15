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
import java.util.ArrayList;
import java.util.List;

import tectonicus.Log;

public class MutableConfiguration implements Configuration
{
	private Mode mode;
	
	private RasteriserType rasteriserType;
	
	private boolean extractLwjglNatives;
	
	private boolean eraseOutputDir;
	
	private File logFile;
	
	private File outputDir;
	
	private boolean useCache;
	private File cacheDir;
	
	private File minecraftJar;
	private File texturePack;
	
	private String outputHtmlName;
	
	private boolean signsInitiallyVisible;
	private boolean playersInitiallyVisible;
	private boolean portalsInitiallyVisible;
	private boolean bedsInitiallyVisible;
	private boolean viewsInitiallyVisible;
	private boolean spawnInitiallyVisible;
	
	private boolean showSpawn;
	
	private boolean isVerbose;
	
	private int tileSize;
	
	private int maxTiles;
	
	private int colourDepth;
	private int alphaBits;
	private int numSamples;
	
	private int numZoomLevels;
	
	private int numDownsampleThreads;
	
	private boolean forceLoadAwt;
	private boolean force32BitNatives;
	private boolean force64BitNatives;
	
	private String singlePlayerName;
	
	private ArrayList<MutableMap> maps;
	
	public MutableConfiguration()
	{
		mode = Mode.CommandLine;
		rasteriserType = RasteriserType.Lwjgl;
		extractLwjglNatives = true;
		showSpawn = true;
		tileSize = 512;
		maxTiles = -1;
		colourDepth = 16;
		alphaBits = 0;
		numSamples = 0;
		numZoomLevels = 8;
		signsInitiallyVisible = true;
		playersInitiallyVisible = true;
		portalsInitiallyVisible = true;
		bedsInitiallyVisible = true;
		spawnInitiallyVisible = true;
		viewsInitiallyVisible = true;
		logFile = new File("./TectonicusLog.txt");
		outputHtmlName = "map.html";
		numDownsampleThreads = 1;
		singlePlayerName = "Player";
		maps = new ArrayList<MutableMap>();
	}
	
	
	public void printActive()
	{
		System.out.println("Settings:");
		System.out.println("\tmode:"+mode());
		System.out.println("\trasteriser:"+getRasteriserType());
		System.out.println("\toutputDir:"+outputDir.getAbsolutePath());
		System.out.println("\tuseCache:"+useCache());
		System.out.println("\tcacheDir:"+cacheDir.getAbsolutePath());
		System.out.println("\tminecraftJar:"+minecraftJar.getAbsolutePath());
		System.out.println("\ttexturePack:"+(texturePack != null ? texturePack.getAbsolutePath() : "none"));
		System.out.println("\tcolourDepth:"+colourDepth());
		System.out.println("\talphaBits:"+alphaBits());
		System.out.println("\tnumSamples:"+numSamples());
		System.out.println("\ttileSize:"+tileSize());
		System.out.println("\tnumZoomLevels:"+numZoomLevels());
		System.out.println("\tportalsInitiallyVisible:"+arePortalsInitiallyVisible());
		System.out.println("\tshowSpawn:"+showSpawn());
		System.out.println("\tsignsInitiallyVisible:"+areSignsInitiallyVisible());
		System.out.println("\tplayersInitiallyVisible:"+arePlayersInitiallyVisible());
		System.out.println("\tbedsInitiallyVisible:"+areBedsInitiallyVisible());
		System.out.println("\tspawnInitiallyVisible:"+isSpawnInitiallyVisible());
		System.out.println("\tviewsInitiallyVisible:"+areViewsInitiallyVisible());
		System.out.println("\teraseOutputDir:"+eraseOutputDir());
		System.out.println("\textractLwjglNatives:"+extractLwjglNatives());
		System.out.println("\tisVerbose:"+isVerbose());
		System.out.println("\tforceLoadAwt:"+forceLoadAwt());
		System.out.println("\tforce32BitNatives:"+force32BitNatives());
		System.out.println("\tforce64BitNatives:"+force64BitNatives());
		System.out.println("\tlogFile:"+getLogFile().getAbsolutePath());
		System.out.println("\toutputHtmlName:"+getOutputHtmlName());
		System.out.println("\tnumDownsampleThreads:"+getNumDownsampleThreads());
		System.out.println("\tsinglePlayerName:"+getSinglePlayerName());
		
		System.out.println();
		
		for (Map m : getMaps())
		{
			System.out.println("'"+m.getName()+"' map");
			
			System.out.println("\tworldDir: "+m.getWorldDir().getAbsolutePath());
			System.out.println("\tdimension: "+m.getDimension());
			System.out.println("\tcameraAngle: "+m.getCameraAngleDeg());
			System.out.println("\tcameraElevation: "+m.getCameraElevationDeg());
			System.out.println("\tclosestZoomSize: "+m.getClosestZoomSize());
			System.out.println("\tworldSubset: "+m.getWorldSubsetFactory().getDescription());
			System.out.println("\tuseBiomeColours: "+m.useBiomeColours());
			
			for (Layer l : m.getLayers())
			{
				System.out.println("\t'"+l.getName()+"' layer");
				
				System.out.println("\t\trenderStyle: "+l.getRenderStyle());
				System.out.println("\t\tlightStyle: "+l.getLightStyle());
				System.out.println("\t\timageFormat: "+l.getImageFormat());
				
				if (l.getImageFormat() == ImageFormat.Jpg)
					System.out.println("\t\timageCompressionLevel: "+l.getImageCompressionLevel());
				
				if (l.getCustomBlockConfig() != null)
					System.out.println("\t\tcustomBlockConfig: "+l.getCustomBlockConfig());
			}
			if (m.numLayers() == 0)
			{
				System.out.println("\tno layers!");
			}
		}
		if (numMaps() == 0)
		{
			System.out.println("no maps!");
		}
		
		System.out.println();
	}
	
	public static void printUsage()
	{
		System.out.println("Command line arguments:");
		System.out.println("arguments are specified as key=value in any order");
		
		// People can't be trusted with this, so make it a hidden option
		//System.out.println("'cacheDir'                - path to a directory to cache rendering info in. Optional, defaults to a subdir within the output directory");
		
		System.out.println("'alphaBits'               - number of alpha bits for output. Specify 8 or 0 for off (default 8)");
		System.out.println("'bedsInitiallyVisible'    - specifies whether bed markers are initially visible or not. Choose 'true' or 'false', default 'true'.");
		System.out.println("'cacheDir'                - specify a different directory for the cache files. Use with caution! Should be an empty directory as it may be deleted. Defaults to a subdirectory in the output dir.");
		System.out.println("'cameraAngle'             - specifies the angle to rotate the camera by, in degrees. 0 puts north at the top, 90 to put east at the top, etc. Defaults to 45, which puts NE at the top");
		System.out.println("'cameraElevation'         - specifies the angle of elevation for the camera, in degrees. 0 is looking horizontally, 90 is looking directly down. Defaults to 45. Choose 30 for a more traditional isometric view.");
		System.out.println("'closestZoomSize'         - specifies how close the camera is for the highest-detail zoom level. Higher numbers will produce maps which cannot be zoomed in as much, but will take up less disk space. Defaults to 12");
		System.out.println("'colourDepth'             - colour depth for rendering. Specify 24 or 16 (default 24)");
		System.out.println("'dimension'               - dimension to render. 'terra' for the regular world, or 'nether' for the nether dimesion. Default 'terra'");
		System.out.println("'eraseOutputDir           - specify 'true' to erase the entire output directory and start from scratch");
		System.out.println("'imageFormat'             - format to use when outputing images. Specify 'png', 'jpg' or 'gif'. Default is png");
		System.out.println("'imageCompressionLevel'   - sets the compression level for output images (jpeg only). Specify a number between 1.0 and 0.1. Default 0.95");
		System.out.println("'lighting'                - lighting style to render with. Possible values are 'day' 'night' or 'none'. Defaults to 'day'");
		System.out.println("'logFile                  - sets the file for the output log. Defaults to './TectonicusLog.txt'");
		System.out.println("'minecraftJar'            - path to your client minecraft jar, for terrain texture. If not specified, will attempt to find it in your AppData dir");
		System.out.println("'mode'                    - 'cmd' for command line, 'gui' for gui, 'players' for just player info export, 'views' to just render views");
		System.out.println("'numZoomLevels'           - how many different levels of zoom to generate");
		System.out.println("'numSamples'              - specifies the number of samples for antialiasing. Defaults to 4 (high quality), specify 0 for no antialiasing");
		System.out.println("'numDownsampleThreads     - specifies the number of threads to use while downsampling. Defaults to the number of cores your machine has");
		System.out.println("'outputDir'               - path to a directory to output the rendered map");
		System.out.println("'outputHtmlName'          - sets the name for the map html file. Defaults to 'map.html'");
		System.out.println("'players'                 - set whether to export players or not. Choose 'all', 'none', 'ops', 'whitelist' or 'blacklist'. 'ops' only exports positions for players with op privileges, whitelist only exports players in the filter file, blacklist excludes players in the filter file");
		System.out.println("'playerFilterFile'        - specify the whitelist or blacklist file for use with players=whitelist or players=blacklist. File should be one player name per line (same format as ops file)");
		System.out.println("'portals'                 - specify whether portals should be exported or not. Choose 'all' or 'none'. Default 'all'");
		System.out.println("'playersInitiallyVisible' - sets whether player markers are initially visible or hidden. Choose 'true' or 'false', default true");
		System.out.println("'portalsInitiallyVisible' - sets whether portal markers are initially visible or hidden. Choose 'true' or 'false', default true");
		System.out.println("'renderStyle'             - drawing style, 'regular' for normal, 'cave' for cave style, or 'nether' for nether. Defaults to 'regular'");
		System.out.println("'signs'                   - set whether to export signs or not. Choose 'none', 'special' or 'all'. 'Special' only exports signs which begin and end with - ! ~ or =. Default 'special'");
		System.out.println("'showSpawn'               - show an icon for the spawn position. Choose 'true' or 'false', default 'true'.");
		System.out.println("'spawnInitiallyVisible    - sets whether the spawn marker is initially visible");
		System.out.println("'signsInitiallyVisible'   - sets whether sign markers are initially visible or hidden. Choose 'true' or 'false', default true");
		System.out.println("'tileSize'                - the size of the output image tiles, in pixels. Default 512, min 64, max 1024.");
		System.out.println("'useBiomeColours'         - set to use biome colours for grass and leaves. Choose 'true' or 'false', default 'true'");
		System.out.println("'useCache'                - enable or disable the use of the cache to speed up repeated map rendering. Specify true or false, defaults to true.");
		System.out.println("'verbose'                 - set to true to print additional debug output");
		System.out.println("'worldDir'                - path to your minecraft world directory (containing level.dat), or numbers 1 to 5 for your singleplayer world");
		System.out.println("");
		System.out.println("Example:");
		System.out.println("java -jar tectonicus worldDir=1 outputDir=C:/tectonicusMap");
		
		// maxTiles is 'hidden' option
	}
	
	public void setMode(final Mode mode)
	{
		this.mode = mode;
	}
	public Mode mode() { return mode; }
	
	public void setRasteriserType(RasteriserType type)
	{
		this.rasteriserType = type;
	}
	public RasteriserType getRasteriserType() { return rasteriserType; }
	
	public void setExtractLwjglNatives(final boolean extractLwjglNatives)
	{
		this.extractLwjglNatives = extractLwjglNatives;
	}
	public boolean extractLwjglNatives() { return extractLwjglNatives; }
	
	public void setEraseOutputDir(final boolean eraseOutputDir)
	{
		this.eraseOutputDir = eraseOutputDir;
	}
	public boolean eraseOutputDir() { return eraseOutputDir; }
	
	public void setOutputDir(File dir)
	{
		this.outputDir = dir;
	}
	public File outputDir() { return outputDir; }
	
	public void setCacheDir(File dir)
	{
		this.cacheDir = dir;
	}
	public File cacheDir() { return cacheDir; }
	
	public void setUseCache(final boolean useCache)
	{
		this.useCache = useCache;
	}
	public boolean useCache() { return useCache; }
	
	public void setMinecraftJar(File jar)
	{
		this.minecraftJar = jar;
	}
	public File minecraftJar() { return minecraftJar; }
	
	public void setTexturePack(File texturePack)
	{
		this.texturePack = texturePack;
	}
	public File texturePack() { return texturePack; }
	
	public void setShowPlayerSpawn(final boolean showSpawn)
	{
		this.showSpawn = showSpawn;
	}
	public boolean showSpawn() { return showSpawn; }
	
	public void setColourDepth(final int colourDepth)
	{
		this.colourDepth = colourDepth;
	}
	public int colourDepth() { return colourDepth; }
	
	public void setAlphaBits(final int alphaBits)
	{
		this.alphaBits = alphaBits;
	}
	public int alphaBits() { return alphaBits; }
	
	public void setNumSamples(final int numSamples)
	{
		this.numSamples = numSamples;
	}
	public int numSamples() { return numSamples; }
	
	public void setNumZoomLevels(final int numZoomLevels)
	{
		this.numZoomLevels = numZoomLevels;
	}
	public int numZoomLevels() { return numZoomLevels; }
	
	public void setMaxTiles(final int maxTiles)
	{
		this.maxTiles = maxTiles;
	}
	public int maxTiles() { return maxTiles; }
	
	public void setIsVerbose(final boolean isVerbose)
	{
		this.isVerbose = isVerbose;
		if (isVerbose)
			Log.setLogLevel(Log.DEBUG);
	}
	public boolean isVerbose() { return isVerbose; }
	
	public void setForceLoadAwt(final boolean forceLoadAwt)
	{
		this.forceLoadAwt = forceLoadAwt;
	}
	public boolean forceLoadAwt() { return forceLoadAwt; }
	
	public void setForce32BitNatives(final boolean force32BitNatives)
	{
		this.force32BitNatives = force32BitNatives;
	}
	public boolean force32BitNatives() { return force32BitNatives; }
	
	public void setForce64BitNatives(final boolean force64BitNatives)
	{
		this.force64BitNatives = force64BitNatives;
	}
	public boolean force64BitNatives() { return force64BitNatives; }
	
	public void setTileSize(final int tileSize)
	{
		this.tileSize = tileSize;
	}
	public int tileSize() { return tileSize; }
	
	public void setSignsInitiallyVisible(final boolean visible)
	{
		this.signsInitiallyVisible = visible;
	}
	public boolean areSignsInitiallyVisible() { return signsInitiallyVisible; }
	
	public void setPlayersInitiallyVisible(final boolean visible)
	{
		this.playersInitiallyVisible = visible;
	}
	public boolean arePlayersInitiallyVisible() { return playersInitiallyVisible; }
	
	public void setPortalsInitiallyVisible(final boolean visible)
	{
		this.portalsInitiallyVisible = visible;
	}
	public boolean arePortalsInitiallyVisible() { return portalsInitiallyVisible; }
	
	public void setBedsInitiallyVisible(final boolean visible)
	{
		this.bedsInitiallyVisible = visible;
	}
	public boolean areBedsInitiallyVisible() { return bedsInitiallyVisible; }
	
	public void setSpawnInitiallyVisible(final boolean visible)
	{
		this.spawnInitiallyVisible = visible;
	}
	public boolean isSpawnInitiallyVisible() { return spawnInitiallyVisible; }
	
	public void setViewsInitiallyVisible(final boolean visible)
	{
		this.viewsInitiallyVisible = visible;
	}
	public boolean areViewsInitiallyVisible() { return viewsInitiallyVisible; }
	
	public void setLogFile(File file)
	{
		this.logFile = file;
	}
	public File getLogFile() { return logFile; }
	
	public void setOutputHtmlName(String name)
	{
		this.outputHtmlName = name;
	}
	public String getOutputHtmlName() { return outputHtmlName; }
	
	public void setNumDownsampleThreads(final int num)
	{
		this.numDownsampleThreads = num;
	}
	public int getNumDownsampleThreads() { return numDownsampleThreads; }
	
	public void setSinglePlayerName(String name)
	{
		this.singlePlayerName = name;
	}
	public String getSinglePlayerName() { return singlePlayerName; }
	
	public int numMaps()
	{
		return maps.size();
	}
	
	public MutableMap getMap(final int index)
	{
		if (index < 0 || index >= maps.size())
			return null;
		
		return maps.get(index);
	}
	
	@Override
	public List<Map> getMaps()
	{
		return new ArrayList<Map>(maps);
	}
	
	public void addMap(MutableMap newMap)
	{
		if (maps.contains(newMap))
			throw new RuntimeException("Map already present in configuration!");
		
		maps.add(newMap);
	}
}
