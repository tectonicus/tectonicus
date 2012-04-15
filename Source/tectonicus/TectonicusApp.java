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
package tectonicus;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import tectonicus.cache.BiomeCache;
import tectonicus.cache.CacheUtil;
import tectonicus.cache.PlayerSkinCache;
import tectonicus.configuration.ArgParser;
import tectonicus.configuration.CommandLineParser;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.Layer;
import tectonicus.configuration.MutableConfiguration;
import tectonicus.configuration.Configuration.Mode;
import tectonicus.configuration.XmlConfigurationParser;
import tectonicus.gui.Gui;
import tectonicus.raw.Player;
import tectonicus.raw.PlayerList;
import tectonicus.util.FileUtils;
import tectonicus.world.World;

// TODO:
//
//	- Heights for water blocks
//	- Heights for lava blocks

// Photo signposts
//	done!	- allow drawing with perspective camera
//			- extract signs to take photos from in preprocess
//			- write photo signs list to disk
//			- go through photo sign list and output photos to sub dir
//			- output photos.js as we do
//			- draw photo marker
//			- add photo markers to map
//			- click on photo marker to open window
//			- display photo in window
//			- check box to show/hide photo icons
//			- allow drawing into subsection of canvas
//	
//	insert custom text into html
//		- into <head>
//		- into <body>
//		- anything else?
//			- replace getTileUrl contents? (with default)
//			- replace title? (with default)
//		- all should have defaults?
//	different tree types
//	different leaf types

// Multi-threading thoughts
//
//	Several distinct stages:
//		- gather regions (TileRenderer.preprocess)
//			calculate per-chunk hash + write to disk
//			gather world stats
//		- find visible tiles
//			project every chunk into map space, store tiles to be renderer
//		- find changed tiles
//			calc tile hash from chunk hashes, compare against stored hash
//		- render base tiles
//			lots happening here
//			..
//		- downsample
//			- for each downsample level
//				- find downsample images
//				- downsample up to 4 base images into new downsample image
//		done!
//	
//	(we'll ignore small tasks like outputting the html and compass images)
//
//	Ideally we want a job-based design, with N worker threads chewing through a list of tasks
//	Mostly we have a master 'todo' list (like the list of tiles to render,
//	or a list of regions to hash) where each item in the todo list is reasonably independent.
//	
//	Simplest would be to turn every item in the todo list into a task, then wait until the
//	task queue is empty before moving on.
//	However the task queue would be *huge*
//	We need some kind of task-producer, which contantly keeps the task queue stocked with
//	jobs (say, 32)

// TODO: Tile rendering much slower on epic world, something scaling non-linearly in drawing?

// FIXME: 'Get Link' only works for 45' camera elevation?

// TODO: Several complex objects don't respect lighting
//	- doors
//	- buttons
//	- pressure plates
//	- repeaters
//	- redstone wire
//	- beds
//	- slab?
//	- double slab?
//	- cake?
//
//	May need a clever solution to deal with sub-meshes that rotate

// Improve redstone wiring connectivity to take into account repeater
//	- isRedstone() in block type?

/* Memory:
	
	16 * 16 * 128 = 32768
	530 chunks
	raw chunk: 4 * 32768 = ~131k
	geometry: worst case 32768 * 6 = 19608 faces
	each face: 4 vertices
	each vertex: position + colour + texture coord = (3 + 4 + 2) * 4 = 36 byte vertex
	one chunk geometry: ~28,235k
	
	total raw chunks: 67.8Mb
	total world geom: 1.354Gb

	Conclusions:
		geometry much heavier than raw chunks
			- need seperate caches for raw chunks and geometry
			- cache *much* more raw chunks than geometry
			- optimises vertex format (colours as packed int, texcoords as short?)
		raw chunks actually quite small (and stored on regular java heap)
			- agressively cache!
	
*/


// Next release todo:
//	- different tree types
//	- different leaves types


// Software renderer thoughts:
//
//	Needed features:
//		- draw lines and triangles
//		- depth buffer
//		- nearest texture filtering
//		- texturing
//		- ortho projection matrix
//		- look at camera matrix
//		- matrix setup like opengl
//		- regular and additive blending

// Plane todo:
//	- sign filtering:
//		- all / special / none
//		- filter file for special sybmols (begins with / ends with)

// google analytics!

// Player positions bukkit plugin:
//	- update player positions from js on timer
//	- output DefaultPlayerSkin.png

// Sign links
//	- some way of linking to custom user galleries
//	either:
//		custom sign text eg. 'link:gallery/castle.html'
//		custom sign_text=>url mapping file

// InfoWindow popups!
//	- show player name / icon / health and inventory
//	- show furnace contents
//	- show chest contents

// Promised features:
//	- show chunk coords in signs?
//	- streetview!
//	- figure out a way to get the fire texture (GLIntercept?)
//	- fire!
//	- define regions via signs

// TODO: Reload player positions on time interval?

// TODO: Nuke raw loaded chunks and loaded geometry when we have finished rendering base tiles

// Scan for players and add those markers to the world
//		- with inventory!

// Inventory work
//	- need to be able to render blocks out to single tile

// Software 3d renderer?

// Make worldDir work with path to dir *or* path to level.dat


// TODO: Support new biome-compatible grass


//
// Changelog
//
//	1st Dec
//		- added 'closestZoomSize' command line arg
//		- added 'texturePack' command line arg

// 4th Dec
//		- fixed not rendering top of blocks at top of world
//		- fixed auto find of minecraft jar in linux
//		- dump filename on raw chunk load error
//		- added furnace, workbench, pumpkins, jack-o-lanterns, jukebox

// 5th Dec
//		- added wooden and stone stairs
//		- added fences
//		- added crates
//		- added minecart tracks

// 11th Dec
//		- added quadtree and used to find visible chunks for output tile
//		- added caching of chunk/tile caches to skip unchanged base tiles

// 13th Dec
//		- added build info output + svn version number
//		- added system properties to log output
//		- added test forceAwt and forceXXBit options
//		- mostly unified lighting

// - alpha version here

// 15th Dec
//		- added white cloth
//		- added netherstone
//		- added slow sand
//		- added lightstone
//		- possibly fixed ice transparency rendering bug?
//		- fixed build info

// 18th Dec
//		- added Dom's optimisation for caclulating visible tiles from chunks (test map: 17s to 0.1s)
//		- master cache info held as text for manual tinkering if need be.
//		- now writes list of updated images to changed.txt in output dir
//		- added lighting options (day/night/none)

// - last update here (v1.0)

// 19th Dec
//		- added jpeg output (reduces file size of output by about a third)
//		- fix array index out of bounds when half steps at top of world

// - v1.01

// 21nd Dec
//		- fixed map.html having wrong case in file extension (.Png not .png)

// - v1.02

// 22nd Dec
//		- fixed inverted logic of CheckForValidMinecraftDir

// - v1.03

//	22nd Dec
//		- added 'writing html' with path to output

//	23rd Dec
//		- disabled chmod-ing of extracted natives to try and solve out-of-memory exception.

// - v1.04

//	28th Dec
//		- render tiles in z-order ( http://en.wikipedia.org/wiki/Z-order_(curve) ) for optimum cache efficiency

//	29th Dec
//		- clean out output dirs if cache has changed

// 30th Dec
//		- added sign icons. New command line option signs=none/special/all
//		- added player icons (with skins support!)
//		- added spawn icon

// - v1.05

//	31st Dec
//		- fixed null pointer when reading players
//		- added 'players=' option to control players export (none/ops/all)
//		- added 'showSpawn=' option
//		- fixed players without custom skin not appearing on map
//		- fixed filtering of signs with no text

// - v1.06

//	1st Jan
//		- added extra tty at init phase to help find Griffen8280's problem
//		- make signs properly clickable, and formatted to the same layout as in game

// - v1.07

//	1st Jan
//		- only try and read player info from files ending in .dat

//	2nd Jan
//		- escape " characters in signs
//		- added extra logging for mode=players

//	3rd Jan
//		-fixed signs disappearing problem
//		- disabled streetview control
//		- disabled map type control

// - v1.08

//	3rd Jan
//		- added diamond logo to map output
//		- added proper player infowindow, with health and air
//		- added signs toggle and players toggle

// - v1.09

//	4th Jan
//		- fixed 'isPng'
//		- fixed blocks at top of world being drawn too dark on top
//		- map centers on spawn position when opened
//		- added gif output (imageFormat=gif). A small map was 205Mb gif compared to 385Mb png!
//			- results nearly indistinguishable from png with default textures
//			- but possibly worse depending on texture pack
//		- fixed file handle leak in file cache
//		- fixed wrong-directioned slash in player info window

// - v1.10

//	5th Jan
//		- falls back to non-antialiased if antialiasing not supported
//		- added donations / player titles
//		- extracted sign/hearts/air/icons from texture pack

// - v1.11

//	7th Jan
//		- much better fallback testing for pbuffer creation
//		- fixed 'unknown image format' exception when exporting html resources
//		- fixed missing top face for half steps which have a full block above them
//		- made command line keys case-insensitive
//		- made command line accept 'yes' and 'on' for boolean values
//		- made html validate (does that fix any browser problems?)
//		- added player skin cache to drastically speed up players-only export

// - v1.12

//	14th Jan
//		- added sandstone
//		- added lapis lazuli block
//		- added lapis lazuli ore
//		- added note block

// - v1.14

//	19th Jan
//		- added biome colours support. Extracts biome data to a cache directory for reuse between runs

//	20th Jan
//		- added cake! 6 variants for each cake slice removed

//	21st Jan
//		- added wool colours

//	23rd Jan
//		- added dispenser
//		- added redstone torches (off and on)
//		- fixed wall torch positions

//	24th Jan
//		- added world stats (on spawn icon popup)

//	26th Jan
//		- fixed crash when unexpected .dat files encountered (eg. 'copy of chunk.0.0.dat')
//		- fixed broken 'is this you' image tag

// 29th Jan
//		- added player blacklist / whitelist output modes
//		- added 'signsInitiallyVisible' and 'playersInitiallyVisible' options

// - v1.15

//	1st Feb
//		- Added fallback to null biome colours if biome extractor can't bind to minecraft.jar
//		- Added compass

//	- v1.16

//	2nd Feb
//		- Fixed km^2 in map stats
//		- Added redstone wire

//	- v1.17

//	4th Feb
//		- Fixed crash in Chunk.collectStats if raw chunk couldn't be loaded

//	6th Feb
//		- Cave mode! Use renderStyle=cave
//		- Urls can now point to specific locations in the world
//			use http://www.example.com/map.html?worldX=0&worldY=0&worldZ=0&zoom=0
//		- Added Tectonicus stats to map stats output

//	- v1.18

//	19th Feb
//		- Removed quadtree, replaced with bounding box frustum scan (lowers memory usage at small speed hit)

//	20th Feb
//		- fixed integer wrap-around bug with world stats
//		- removed global list of chunks in world (much lower memory usage)

//	22nd Feb
//		- Implemented chunk coord iterators instead of holding global list of all chunks

//	24th Feb
//		- Implemented support for new region map format

//	- v1.19

//	25th Feb
//		- Fixed ArrayIndexOutOfBoundsException while discovering chunks
//		- Possible fix for signs and players not showing up

//	- v1.20

//	25th Feb
//		- Updated to latest version of lwjgl (2.7)

//	- v1.21

//	26th Feb
//		- Added support for new slabs and double slabs
//		- Added beds
//		- Updated redstone wire for new texture pack layout
//		- Added fading for redstone wire colours
//		- Added 'imageCompressionLevel' command line argument
//		- optimised findVisibleChunks. Should help with rendering speed (now ~2ms instead of ~100ms)
//		- added 'show link' to get link to current view position

//	27th Feb
//		- Added redstone repeater (on/off, 4 directions, 4 delay settings)
//		- Added wooden door (in 4 directions, each open or closed)
//		- Added iron door (in 4 directions, each open or closed)
//		- Added 'cameraElevation' command line option. range from 10 to 90 (degrees)
//		- Added wooden pressure plates
//		- Added stone pressure plates
//		- Added redstone buttons

//	v1.22

//	1st March
//		- Added wall signs with text and 4 orientations
//		- Added sign posts with text and 16 orientations
//		- Changed google maps to always use latest stable version (3.3)
//		- Fixed a couple of null pointer exceptions
//		- Fixed missing chunk errors! Woo!

//	v1.23

//	1st March
//		- Removed [New/Old]ChunkIterator classes. Now just iterates over all regions and all contained chunks
//			This is safer and faster, but does mean we no longer support the old map format
//		- Made TileRenderer.findVisibleTiles use region cache to improve speed
//		- Rewrote chunk hashing (calc ones at preprocess, cache to disk), means FindChangedTiles now *much* quicker
//			- test map now down to 7mins 7seconds
//		- enabled allowing software opengl by default
//		- Added version check to bail cleanly when trying to map old alpha style worlds
//		- Added rasteriser info to log

//	v1.24

//	3rd March
//		- Fixed biome extractor and updated to latest version (todo: test!)

//	v1.25

//	6th March
//		- Tile coord sets now swapped to hdd for *much* lower memory usage
//		- Fixed array out of bounds error when generating Slab geometry
//		- Fixed donation html
//		- Changed file list now written incrementally for lower memory usage
//		- Added peak memory usage stat to map stats
//		- Fixed wrong <br> tag in html
//		- Test out pbuffer on creation so we bail out early if we've got a bogus pbuffer
//		- Default 'useBiomeColours' to off since I don't think it's working correctly other than slowing things down a lot

//	v1.26

//	8th March
//		- Fixed array out of bounds crash with double slab
//		- Fixed black rectangle around compass
//		- Possibly fixed grid lines appearing in oceans

//	v1.27

//	13th March
//		- Added 'logFile' command line argument
//		- Added 'outputHtmlName' command line argument
//		- Minor speed increase in loading raw chunks

//	v1.28

// 19th March
//		- made downsampling distribute over multiple threads (use numDownsampleThreads=N to customise num cores used)
//		- Fixed misnamed log file
//		- Added debug information when no chunks found
//		- Always skip empty signs since they're used for asthetic reasons (like building chairs)
//		- fixed pre/center tag ordering problem in html
//		- added missing ';' to javascript
//		- output world/render stats to stats.js (block stats now in blockStats.js)

//	v1.29

//	22nd March
//		- fixed hdd tile list iterator counting too many tiles (fixes 107% bug)

//	v1.30

//	3rd April
//	- Added nether rendering. Use dimension=nether and renderStyle=nether
//	- Multithreaded base image writing.
//	- Optimised takeScreenshot to copy into image files a scanline at a time
//	- Optimised chunk loading and geometry creation
//	- Added extra debug info when chunks fail to load
//	- Added different half-slab material types

//	v1.31

//	- Reduced region cache size from 32 to 16 to fix OSX out of memory bug
//	- Re-exposed cacheDir with warning about usage
//	- Added toggle box for spawn point, with 'spawnInitiallyVisible' command line option
//	- Tweeked nether rendering mode to strip the roof off of nether worlds better
//	- Now compiled to java 1.5 rather than 1.6
//	- Fixed missing google maps api with no signs html
//	- Portals now have markers like players
//	- Fixed top-of-world lighting glitch
//	- Added detector rails
//	- Added powered rails (both off and on states)

//	v1.32

//	- More robust handling of region and image file opening/closing
//	- Restored tileSize param (min 64, max 1024)
//	- Added wheat blocks
//	- Added portal option. portals=All will export portal markers, or portals=None to disable them.

//	v1.33

//	- Added Web block
//	- Pointed logo link to proper website rather than forum topic
//	- Added coord of current cursor position to map overlay
//	- Added bed markers for player's spawn points
//	- Reduced region cache size to try and fix OOM errors
//	- Fixed odd lighting on stairs

//	v1.34

//	- Fixed vertical fences(!)
//	- Fixed bed markers not showing correct owner

//	v1.35

//	- Made gui mode work (ish)
//	- Fixed black grass and leaves with painterly pack
//	- Fixed bedsInitiallyVisible not working.
//	- Possible fix for image writer / "pos < flushedPos" exception

//	v1.36

//	- Added parsing of config from xml

//	v1.37

//	- Generates mipmaps of terrain atlas for better image quality
//	- Fixed bug where portal filter was not parsed correctly from xml config file
//	- Fixed bug where player filter was not parsed correctly from xml config file
//	- Fixed bug where signs filter was not parsed correctly from xml config file
//	- Fixed bug which would cause gui to crash on start
//	- Initial pass at circular regions
//	- Added 'renderStyle' option to layers node in xml config
//	- Now exports player info for single player worlds. Use 'singlePlayerName=' in layers node to set player name
//	- Added support for birch and spruce leaves
//	- Added support for birch and spruce trunks

//	v1.38

//	- Fixed memory leak in biome cache
//	- Big js / layers overhaul!

//	v2.0 beta

//	- added cameraAngle and cameraElevation to 'map' settings in xml config
//	- added useBiomeColours to map settings
//	- added birch and spruce saplings
//	- added dead shrubs
//	- added tall grass
//	- added ferns

//	v2.0 beta 2

//	- fixed biome colour sampling bug (array out of bounds error)
//	- added 'numZoomLevels' to config node
//	- fixed tall grass biome colour bug

//	v2.0 beta 3

//	- added 'singlePlayerName' to config node
//	- added initially visible toggles to config node (todo: add to example config)
//	- added sign/player/portal filtering to each layer (todo: test this)
//	- fixed player skin cache getting wiped all the time
//	- fixed image compression level setting
//	- added version checked to config file
//	- removed old command line arg parsing

//	v2.0 beta 4

//	- fixed force load 32/64 natives flags
//	- fixed initially visible flags
//	- compass image now changes when map angle changes

//	v2.0 beta 5

//	- command line args work now
//	- hide map switch control if only one map type
//	- removed 'get link' box, now just changes the url in the browser's address bar
//	- redone icon toggle buttons to be smaller

//	v2.0 beta 6

//	- fixed problem with map selector not appearing on output with one map but multiple layers
//	- merged some js files to reduce server requests
//	- added players-only mode back in. Either use mode="players" in xml or mode=players on command line
//	- made gui work again

//	v2.0 beta 7

//	- added first person views!
//	- added better error messages when terrain.png not found (detects use of server jar or launcher jar)
//	- reverted google maps api to 3.3 to fixed unclickable markers bug

//	v2.0

//	- views now default to an angle of 90 degrees (horizontal)
//	- removed #view signs from being output in signs=all mode
//	- added view files to changed file list
//	- added bottom to wool
//	- added bottom to log
//	- added bottom to cactus
//	- added bottom to double slab
//	- added bottom to furnace
//	- added bottom to leaves
//	- added bottom to workbench
//	- added bottom to jack-o-lantern
//	- added bottom to dispenser
//	- added bottom to grass
//	- can now set view distance in config file
//	- can now set image format for views in config file
//	- can now set image compression level for views in config file
//	- layers now default to 'day' light style, no longer a required attribute

//	v2.01

//	- fixed bottom of grass blocks
//	- fixed too dark biome grass
//	- added biome-coloured grass to side of grass blocks
//	- fixed night lighting on cake
//	- fixed night lighting on pressure plates
//	- fixed night lighting on doors
//	- fixed night lighting on redstone wire
//	- fixed night lighting on redstone repeater
//	- added bottom to stairs
//	- improved glass rendering (hide internal edges)
//	- added bottom to water
//	- changed skybox to sky tube for better quality
//	- added night option to views (with separate skybox)

//	v2.02

//	18th September
//
// - refactored block registry code
// - fixed cake (1.8 moved it's position in terrain.png)
// - fixed beds (1.8 moved it's position in terrain.png)
// - fixed burning furnace
// - added new brick half slab
// - added new stone brick half slab
// - added new brick double slab
// - added new stone brick double slab
// - added brick steps
// - added stone brick steps
// - added cracked stone brick
// - added mossy stone brick
// - added locked chest
// - added melon
// - added glass pane
// - added iron fence
// - added vines
// - added melon stems
// - added pumpkin stems
// - added huge red mushroom
// - added huge brown mushroom
// - added regular piston (+extended piston arm)
// - added sticky piston (+extended piston arm)
// - fixed single quote characters in view signs breaking javascript syntax
// - disabled google maps marker 'optimisation' that prevents markers being clicked

// v2.03

// 10th November

// - updated to latest stable version of google maps
// - added mode="views" option. Just renders any changed or new views.
// - added cameraAngle= to command line options
// - added cameraEvevation= to command line options
// - fixed map being cut off past a certain distance above the origin
// - refactored texture loading

// 14th November

// - added fire block
// - added portal block
// - refactored all block types to support custom textures
// - supports custom block types for each layer
// - supports default + custom block types for each layer

// v2.04

// - made custom block config optional
// - added Mycelium
// - added Nether Brick
// - added Nether Brick Stairs
// - added Nether Brick Fence
// - added End Stone
// - added Lilly Pad
// - added Dragon Egg
// - added Nether Wart
// - added End Portal Frame (with and without eye)
// - added Cauldron (with different water levels)
// - added Enchantment Table
// - added Brewing Stand (with and without bottles in each corner)
// - fixed lighting on fire block
// - added support for ender dimension (dimension="ender" instead of dimension="nether")

// v2.05

// 25th November

// - fixed bug in DataSolid where wrong texture may be used for interior blocks

// 12th January

// - Made compass default to new north direction
// - Can set north direction for compass for each map (eg. north="+x")
// - Can use a custom image for the compass rose (compassRose="/path/to/rose.png")
// - fixed bug where minecart tracks didn't show correct name in block stats
// - fixed bug where glass wouldn't show up in block stats
// - added comma separation for 1,000s in block stats
// - added some world stats

// 15th January

// - added food display to player info
// - added XP level to player info
// - reworked raw chunk and geometry caching

// v2.06

// 22nd January

// - expanded mod support to allow different block types for different data values

// v2.07

// - added Anvil map format support

// v2.08

// TODO: Remove Wool and any other block types made redundant by new id:data syntax

// TODO: Write up biome data in chunk proposal

// TODO: Snow and Soil classes check that above block is solid, but are not full height
// Looks like a bug. Check up!

// Idea: Lots of wasted time looking chunk data, then (id+data)->BlockType
//		Instead resolve BlockTypes into one big chunk array and use that when generating geometry
//	Also merge with padding around the side so we can pretend a chunk is part of a huge
//  3d array, so we don't have to do slow world.resolveCoord(chunkCoord, x, y, z) etc.
//	Also calculate empty/solid into a similar array

// TODO: Fix/go over batched rendering and memory usage
// - record max/average chunks per render
// - trim chunk caches after each addition?
// - swap chunk cache collection types?
// - maybe extract templated cache class?

// TODO: Fix donation html

// Todo: expand stats
//	- world stats (num chunks, area, file size, num players)
//  - total paintings
//  - items count
//  - mobs count (per mob type?)

// http://tectonicus.betaeasy.com/thread/10422

// todo: put jar verification code (from TexturePack c'tor, sniffing for class files) back somewhere
// todo: check version of block config file
// todo: support custom blocks in views

// todo: enchantment table should have book

public class TectonicusApp
{
	private Configuration args;
	private CompositePrintStream newOut, newErr;
	
	private TectonicusApp(Configuration args)
	{	
		this.args = args;
		
		openLog( args.getLogFile() );
		
		BuildInfo.print();
		
		System.out.println("Started on "+new Date());
		
		System.out.println("System:");
		System.out.println("\tOS Name: "+getProperty("os.name"));
		System.out.println("\tOS Architecture: "+getProperty("os.arch"));
		System.out.println("\tOS Version: "+getProperty("os.version"));
		
		System.out.println("\tJava vendor: "+getProperty("java.vendor"));
		System.out.println("\tJava version: "+getProperty("java.version"));
		
		System.out.println("\tAwt toolkit: "+getProperty("awt.toolkit"));
		System.out.println("\tHeadless?: "+getProperty("java.awt.headless"));
	}
	
	private static String getProperty(String key)
	{
		String result = "";
		try
		{
			result = System.getProperty(key);
		}
		catch (Exception e) {}
		return result;
	}
	
	private void openLog(File logFile)
	{
		try
		{
			logFile.getAbsoluteFile().getParentFile().mkdirs();
			
			if (logFile.exists())
				logFile.delete();
			
			PrintStream fileOut = new PrintStream( new FileOutputStream(logFile) );
			
			newOut = new CompositePrintStream(System.out, fileOut);
			newErr = new CompositePrintStream(System.err, fileOut);
			
			System.setOut(newOut);
			System.setErr(newErr);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void closeLog()
	{
		newOut.close();
		newErr.close();
	}
		
	private void run()
	{
		InteractiveRenderer interactiveRenderer = null;
		TileRenderer tileRenderer = null;
		try
		{
			MessageDigest hashAlgorithm = MessageDigest.getInstance("sha1");
			
			if (args.mode() == Mode.Interactive)
			{
				interactiveRenderer = new InteractiveRenderer(args, 512, 512);
				
				tectonicus.configuration.Map map = args.getMap(0);
				Layer layer = map.getLayer(0);
				
				BiomeCache biomeCache = CacheUtil.createBiomeCache(args.minecraftJar(), args.cacheDir(), map, hashAlgorithm);
				
				World world = new World(interactiveRenderer.getRasteriser(), map.getWorldDir(), map.getDimension(), args.minecraftJar(), args.texturePack(), biomeCache, hashAlgorithm, args.getSinglePlayerName(), map.getWorldSubsetFactory());	
				TileRenderer.setupWorldForLayer(layer, world);
				
				interactiveRenderer.display(world);
				
				interactiveRenderer.destroy();
			}
			else if (args.mode() == Mode.CommandLine)
			{
				// Do this first before we attempt to load any caches
				if (args.eraseOutputDir())
				{
					System.out.println("Deleting output dir: "+args.outputDir().getAbsolutePath());
					
					FileUtils.deleteDirectory(args.outputDir());
				}
				
				tileRenderer = new TileRenderer(args, new CommandLineOutput(), hashAlgorithm);
				
				tileRenderer.output();
			}
			else if (args.mode() == Mode.RenderViews)
			{
				tileRenderer = new TileRenderer(args, new CommandLineOutput(), hashAlgorithm);
				
				tileRenderer.outputViews();				
			}
			else if (args.mode() == Mode.ExportPlayers)
			{
				final Date startTime = new Date();
				
				PlayerSkinCache skinCache = new PlayerSkinCache(args, hashAlgorithm);
				PlayerIconAssembler iconAssembler = new PlayerIconAssembler(skinCache);
				
				for (tectonicus.configuration.Map map : args.getMaps())
				{
					ArrayList<Player> players = World.loadPlayers(map.getWorldDir());
				
					PlayerList ops = PlayerList.loadOps(map.getWorldDir());
									
					File mapDir = new File(args.outputDir(), map.getId());
					File playerDir = new File(mapDir, "players.js");
					
					File imagesDir = new File(args.outputDir(), "Images");
					
					TileRenderer.outputPlayers(playerDir, imagesDir, map, map.getPlayerFilter(), players, ops, iconAssembler);
				}
				
				skinCache.destroy();
				
				final Date endTime = new Date();
				String time = Util.getElapsedTime(startTime, endTime);
				System.out.println("Player export took "+time);
			}
			else if (args.mode() == Mode.Gui)
			{
				Gui gui = new Gui(hashAlgorithm);
				gui.display();
			}
			else if (args.mode() == Mode.Profile)
			{
				/*
				BiomeCache biomeCache = new NullBiomeCache();
				BlockFilter blockFilter = new NullBlockFilter();
				
				RegionIterator it = new AllRegionsIterator(args.worldDir());
				File regionFile = it.next();
				Region region = new Region(regionFile);
				ChunkCoord chunkCoord = region.getContainedChunks()[0];
				
				Chunk c = region.loadChunk(chunkCoord, biomeCache, blockFilter);
				
				// createGeometry profiling
				Rasteriser rasteriser = RasteriserFactory.createRasteriser(RasteriserType.Lwjgl, DisplayType.Offscreen, 512, 312, 24, 8, 16, 4);
				NullBlockMaskFactory mask = new NullBlockMaskFactory();
				TexturePack texturePack = new TexturePack(rasteriser, args.minecraftJar(), args.texturePack());
				World world = new World(rasteriser, args.worldDir(), args.getDimensionDir(), args.minecraftJar(), args.texturePack(), biomeCache, hashAlgorithm, "Player", new FullWorldSubsetFactory());
				
				// Warm it up
				for (int i=0; i<100000; i++)
				{
					c.createGeometry(rasteriser, world, world.getBlockTypeRegistry(), mask, texturePack);
				}
				
				final long start = System.currentTimeMillis();
				
				for (long i=0; i<10000000000l; i++)
				{
					c.createGeometry(rasteriser, world, world.getBlockTypeRegistry(), mask, texturePack);
				}
				
				final long end = System.currentTimeMillis();
				
				final long time = end - start;
				
				System.out.println("Total time: "+time+"ms");
				
				// Original:			5120ms
				// Two-loop version:	5117ms
				*/
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		catch (OutOfMemoryError e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (tileRenderer != null)
				tileRenderer.destroy();
			if (interactiveRenderer != null)
				interactiveRenderer.destroy();
		}
		
		System.out.println("Finished");
	}
	
	public static void unpackLwjgl(final boolean force32BitNatives, final boolean force64BitNatives)
	{
		String[] files =
		{
			"jinput-dx8_64.dll",
			"jinput-dx8.dll",
			"jinput-raw_64.dll",
			"jinput-raw.dll",
			"libjinput-linux.so",
			"libjinput-linux64.so",
			"libjinput-osx.jnilib",
			"liblwjgl.jnilib",
			"liblwjgl.so",
			"liblwjgl64.so",
			"libopenal.so",
			"libopenal64.so",
			"lwjgl.dll",
			"lwjgl64.dll",
			"openal.dylib",
			"OpenAL32.dll",
			"OpenAL64.dll"
		};
		
		Map<String, String> force64BitMapping = new HashMap<String, String>();
		force64BitMapping.put("jinput-dx8_64.dll", "jinput-dx8.dll");
		force64BitMapping.put("jinput-raw_64.dll", "jinput-raw.dll");
		force64BitMapping.put("libjinput-linux64.so", "libjinput-linux.so");
		force64BitMapping.put("liblwjgl64.so", "liblwjgl.so");
		force64BitMapping.put("lwjgl64.dll", "lwjgl.dll");
		force64BitMapping.put("OpenAL64.dll", "OpenAL32.dll");
		
		Map<String, String> force32BitMapping = new HashMap<String, String>();
		force32BitMapping.put("jinput-dx8.dll", "jinput-dx8_64.dll");
		force32BitMapping.put("jinput-raw.dll", "jinput-raw_64.dll");
		force32BitMapping.put("libjinput-linux.so", "libjinput-linux64.so");
		force32BitMapping.put("liblwjgl.so", "liblwjgl64.so");
		force32BitMapping.put("lwjgl.dll", "lwjgl64.dll");
		force32BitMapping.put("OpenAL32.dll", "OpenAL64.dll");
		
		File cacheDir = new File(System.getProperty("user.home"), ".tectonicus/native");
		FileUtils.deleteDirectory(cacheDir);
		cacheDir.mkdirs();
		
		for (String f : files)
		{
			File outFile = new File(cacheDir, f);
			FileUtils.extractResource("Native/"+f, outFile);
			 
			/*
			Disabled this because it was causing out-of-memory (due to the large fork) in some cases
			and I'm not sure if it's actually required
			if (!OsDetect.isWindows())
			{
				// Make the new file executable by calling chmod
				try
				{
					ArrayList<String> args = new ArrayList<String>();
					args.add("chmod");
					args.add("+x");
					ProcessUtil.addFilePath(args, outFile);
					
					ProcessBuilder builder = new ProcessBuilder(args);
					builder.directory(outFile.getParentFile());
					
					Process process = builder.start();
					
					process.waitFor();
					process.destroy();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			*/
		}
		
		Map<String, String> renameMap = null;
		if (force32BitNatives)
		{
			System.out.println("Forcing 32-bit native libraries");
			renameMap = force32BitMapping;
		}
		else if (force64BitNatives)
		{
			System.out.println("Forcing 64-bit native libraries");
			renameMap = force64BitMapping;
		}
		if (renameMap != null)
		{
			for (String src : renameMap.keySet())
			{
				File srcFile = new File(cacheDir, src);
				File destFile = new File(cacheDir, renameMap.get(src));
				
				destFile.delete();
				try
				{
					FileUtils.copyFiles(srcFile, destFile, new HashSet<String>());
				}
				catch (Exception e)
				{
					throw new RuntimeException("Couldn't copy "+src, e);
				}
			}
		}
		
		String nativePath = cacheDir.getAbsolutePath();
		System.setProperty("org.lwjgl.librarypath", nativePath);
		System.setProperty("net.java.games.input.librarypath", nativePath);
		System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true");
	}
	
	public static void main(String[] argArray) throws Exception
	{
	//	jsonWriteTest(new File("/Users/John/TectonicusTests/LayerTest/test.js"));
		
		ArgParser parser = new ArgParser(argArray);
		String configFile = parser.getString("config", null);
		
		MutableConfiguration args = null;
		if (configFile != null)
		{
			// Load config from xml
			args = XmlConfigurationParser.parseConfiguration(new File(configFile));
			
			final String modeStr = parser.getString("mode", "");
			if (modeStr.equalsIgnoreCase("players"))
				args.setMode(Mode.ExportPlayers);
			else if (modeStr.equalsIgnoreCase("views"))
				args.setMode(Mode.RenderViews);
			else if (modeStr.equalsIgnoreCase("interactive"))
				args.setMode(Mode.Interactive);
		}
		else
		{
			// Parse config from command line
			args = CommandLineParser.parseCommandLine(argArray);
		}
		
		if (args != null)
		{
			TectonicusApp app = new TectonicusApp(args);
			
			args.printActive();
			
			// Workaround for sun bug 6539705 ( http://bugs.sun.com/view_bug.do?bug_id=6539705 )
			// Trigger the load of the awt libraries before we load lwjgl
			Toolkit.getDefaultToolkit();
			
			if (args.forceLoadAwt())
			{
				System.loadLibrary("awt");
			}
			
			if (args.extractLwjglNatives())
				unpackLwjgl(args.force32BitNatives(), args.force64BitNatives());
			
			app.run();
			
			app.closeLog();
		}
		else
		{
			BuildInfo.print();
			MutableConfiguration.printUsage();
		}
		
	}
	
	// 5m 42s vs 5m 47s
	// New takeScreenshot: 5m 12s
	
	// old 35m 25s
}
