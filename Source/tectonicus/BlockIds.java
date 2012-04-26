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


public class BlockIds
{
	public static final int AIR						= 0;
	public static final int STONE					= 1;
	public static final int GRASS					= 2;
	public static final int DIRT					= 3;
	public static final int COBBLESTONE				= 4;
	public static final int WOOD					= 5;
	public static final int SAPLING					= 6;
	public static final int ADAMANTIUM				= 7;
	public static final int WATER					= 8;
	public static final int STATIONARY_WATER		= 9;	
	public static final int LAVA					= 10;
	public static final int STATIONARY_LAVA			= 11;
	public static final int SAND					= 12;
	public static final int GRAVEL					= 13;
	public static final int GOLD_ORE				= 14;
	public static final int IRON_ORE				= 15;
	public static final int COAL_ORE				= 16;
	public static final int LOG						= 17;
	public static final int LEAVES					= 18;
	public static final int SPONGE					= 19;
	public static final int GLASS					= 20;
	public static final int LAPIS_LAZULI_ORE		= 21;
	public static final int LAPIS_LAZULI_BLOCK		= 22;
	public static final int DISPENSER				= 23;
	public static final int SANDSTONE				= 24;
	public static final int NOTE_BLOCK				= 25;
	public static final int BED						= 26;
	public static final int POWERED_RAIL			= 27;
	public static final int DETECTOR_RAIL			= 28;
						// 29 sticky piston
	public static final int WEB						= 30;
	public static final int TALL_GRASS				= 31; // not implemented
	public static final int DEAD_SHRUB				= 32; // not implemented
						// 33-34 no block type
	public static final int WOOL					= 35;	
						// 36 no block type
	public static final int YELLOW_FLOWER			= 37;
	public static final int RED_FLOWER				= 38;
	public static final int BROWN_MUSHROOM			= 39;
	public static final int RED_MUSHROOM			= 40;
	public static final int GOLD_BLOCK				= 41;
	public static final int IRON_BLOCK				= 42;
	public static final int DOUBLE_SLAB				= 43;
	public static final int SLAB					= 44;
	public static final int BRICK					= 45;
	public static final int TNT						= 46;
	public static final int BOOKSHELF				= 47;
	public static final int MOSSY_COBBLESTONE		= 48;
	public static final int OBSIDIAN				= 49;
	public static final int TORCH					= 50;
						//	FIRE						// NOT IMPLEMENTED
	public static final int MOB_SPAWNER				= 52;
	public static final int WOODEN_STAIRS			= 53;
	public static final int CHEST					= 54;
	public static final int REDSTONE_WIRE			= 55;
	public static final int DIAMOND_ORE				= 56;
	public static final int DIAMOND_BLOCK			= 57;
	public static final int WORKBENCH				= 58;
	public static final int WHEAT					= 59;
	public static final int SOIL					= 60;
	public static final int FURNACE					= 61;
	public static final int BURNING_FURNACE			= 62;
	public static final int SIGN_POST				= 63;
	public static final int WOODEN_DOOR				= 64;
	public static final int LADDER					= 65;
	public static final int MINECART_TRACKS			= 66;
	public static final int COBBLESTONE_STAIRS		= 67;
	public static final int WALL_SIGN				= 68;
	public static final int LEVER					= 69; // NOT IMPLEMENTED
	public static final int STONE_PRESSURE_PLATE	= 70;
	public static final int IRON_DOOR				= 71;
	public static final int WOOD_PRESSURE_PLATE		= 72;
	public static final int REDSTONE_ORE			= 73;
	public static final int GLOWING_REDSTONE_ORE	= 74;
	public static final int REDSTONE_TORCH_OFF		= 75;
	public static final int REDSTONE_TORCH_ON		= 76;
	public static final int STONE_BUTTON			= 77;
	public static final int SNOW					= 78;
	public static final int ICE						= 79;
	public static final int SNOW_BLOCK				= 80;
	public static final int CACTUS					= 81;
	public static final int CLAY					= 82;
	public static final int REEDS					= 83;
	public static final int JUKEBOX					= 84;
	public static final int FENCE					= 85;
	public static final int PUMPKIN					= 86;
	public static final int NETHERSTONE				= 87;
	public static final int SLOW_SAND				= 88;
	public static final int LIGHTSTONE				= 89;
	public static final int PORTAL					= 90;
	public static final int JACK_O_LANTERN			= 91;
	public static final int CAKE					= 92;
	public static final int REDSTONE_REPEATER_OFF	= 93;
	public static final int REDSTONE_REPEATER_ON	= 94;
	public static final int FENCE_GATE				= 107;

	// Enum thoughts:
	//	using a proper enum instead of constants would be nice for typesafety
	//	and it would avoid autoboxing to/from Integer when used in a Map
	//	But all the data files use raw ints, so we'd have to constantly convert from int->enum
	//	this could add lots of extra overhead in tight loops, especially since we already have
	//	a block id -> block type mapping
	
	/*
	public enum BlockId
	{
		Air(0, "Air"),
		Stone(1, "Stone"),
		Grass(2, "Grass"),
		Dirt(3, "Dirt"),
		Cobblestone(4, "Cobblestone"),
		Wood(5, "Wood"),
		Sapling(6, "Sapling"),
		Adamantium(7, "Adamantium"),
		Water(8, "Water"),
		StationaryWater(9, "Water"),
		Lava(10, "Lava"),
		StationaryLava(11, "Lava"),
		Sand(12, "Sand"),
		Gravel(13, "Gravel"),
		GoldOre(14, "Gold Ore"),
		IronOre(15, "Iron Ore"),
		CoalOre(16, "Coal Ore"),
		Log(17, "Log"),
		Leaves(18, "Leaves"),
		Sponge(19, "Sponge"),
		Glass(20, "Glass"),
		
		Dispenser(23, "Dispenser"),
		
		Wool(35, "Wool"),
		
		YellowFlower(37, "Yellow Flower"),
		RedFlower(38, "Red Flower"),
		BrownMushroom(39, "Brown Mushroom"),
		RedMushroom(40, "Red Mushroom"),
		GoldBlock(41, "Gold Block"),
		IronBlock(42, "Iron Block"),
		DoubleStep(43, "Double Step"),
		Step(44, "Step"),
		Brick(45, "Brick"),
		Tnt(46, "Tnt"),
		Bookshelf(47, "Bookshelf"),
		MossyCobblestone(48, "Mossy Cobblestone"),
		Obsidian(49, "Obsidian"),
		Torch(50, "Torch"),
		
		MobSpawner(52, "Mob Spawner"),
		WoodenStairs(53, "Wooden Stairs"),
		Chest(54, "Chest"),
		
		DiamondOre(56, "Diamond Ore"),
		DiamondBlock(57, "Diamond Block"),
		Workbench(58, "Workbench"),
		
		Soil(60, "Soil"),
		Furnace(61, "Furnace"),
		BurningFurnace(62, "Burning Furnace"),
		
		Ladder(65, "Ladder"),
		MinecartTracks(66, "Minecart Tracks"),
		CobblestoneStairs(67, "Cobblestone Stairs"),
		
		RedstoneOre(73, "Redstone Ore"),
		GlowingRedstoneOre(74, "Glowing Restone Ore"),
		RedstoneTorchOff(75, "Redstone Torch Off"),
		RedstoneTorchOn(76, "Redstone Torch On"),
		
		Snow(78, "Snow"),
		Ice(79, "Ice"),
		SnowBlock(80, "Snow Block"),
		Cactus(81, "Cactus"),
		Clay(82, "Clay"),
		Reeds(83, "Sugar Canes"),
		Jukebox(84, "Jukebox"),
		Fence(85, "Fence"),
		Pumpkin(86, "Pumpkin"),
		Netherstone(87, "Netherstone"),
		SlowSand(88, "Slow Sand"),
		Lightstone(89, "Lightstone"),
		Portal(90, "Portal"),
		JackOLantern(91, "Jack-o-lantern"),
		Cake(92, "Cake!");
		
		public final int id;
		public final String name;
		
		private BlockId(final int id, final String name)
		{
			this.id = id;
			this.name = name;
		}
	}
	*/
}
