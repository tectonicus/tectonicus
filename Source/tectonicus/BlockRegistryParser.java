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

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import tectonicus.blockTypes.Air;
import tectonicus.blockTypes.Bed;
import tectonicus.blockTypes.BrewingStand;
import tectonicus.blockTypes.Button;
import tectonicus.blockTypes.Cactus;
import tectonicus.blockTypes.Cake;
import tectonicus.blockTypes.Cauldron;
import tectonicus.blockTypes.Chest;
import tectonicus.blockTypes.DataSolid;
import tectonicus.blockTypes.Dispenser;
import tectonicus.blockTypes.Door;
import tectonicus.blockTypes.DoubleSlab;
import tectonicus.blockTypes.DragonEgg;
import tectonicus.blockTypes.EnchantmentTable;
import tectonicus.blockTypes.EnderPortal;
import tectonicus.blockTypes.EnderPortalFrame;
import tectonicus.blockTypes.Fence;
import tectonicus.blockTypes.FenceGate;
import tectonicus.blockTypes.Fire;
import tectonicus.blockTypes.FruitStem;
import tectonicus.blockTypes.Furnace;
import tectonicus.blockTypes.Glass;
import tectonicus.blockTypes.GlassPane;
import tectonicus.blockTypes.Grass;
import tectonicus.blockTypes.HugeMushroom;
import tectonicus.blockTypes.Ice;
import tectonicus.blockTypes.JackOLantern;
import tectonicus.blockTypes.Ladder;
import tectonicus.blockTypes.Leaves;
import tectonicus.blockTypes.Lilly;
import tectonicus.blockTypes.Log;
import tectonicus.blockTypes.MinecartTracks;
import tectonicus.blockTypes.NetherWart;
import tectonicus.blockTypes.PistonBase;
import tectonicus.blockTypes.PistonExtension;
import tectonicus.blockTypes.Plant;
import tectonicus.blockTypes.Portal;
import tectonicus.blockTypes.PressurePlate;
import tectonicus.blockTypes.RedstoneRepeater;
import tectonicus.blockTypes.RedstoneWire;
import tectonicus.blockTypes.Sapling;
import tectonicus.blockTypes.Sign;
import tectonicus.blockTypes.Slab;
import tectonicus.blockTypes.Snow;
import tectonicus.blockTypes.Soil;
import tectonicus.blockTypes.SolidBlockType;
import tectonicus.blockTypes.Stairs;
import tectonicus.blockTypes.TallGrass;
import tectonicus.blockTypes.Torch;
import tectonicus.blockTypes.Trapdoor;
import tectonicus.blockTypes.Vines;
import tectonicus.blockTypes.Water;
import tectonicus.blockTypes.Wheat;
import tectonicus.blockTypes.Wool;
import tectonicus.blockTypes.Workbench;
import tectonicus.cache.BiomeCache;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;

public class BlockRegistryParser
{
	private final TexturePack texturePack;
	private final BiomeCache biomeCache;
	
	public BlockRegistryParser(TexturePack texturePack, BiomeCache biomeCache)
	{
		this.texturePack = texturePack;
		this.biomeCache = biomeCache;
	}
	
	public void parse(final String resName, BlockTypeRegistry registry)
	{	
		if (resName == null || resName.trim().length() == 0)
			return;
		
		Element root = loadXml(resName, "blockConfig");
		
		if (root == null)
			throw new RuntimeException("Couldn't load block config from: '"+resName+"'");
		
		// TODO: Check version here
		// ..
		
		NodeList children = root.getChildNodes();
		for (int i=0; i<children.getLength(); i++)
		{
			Node n = children.item(i);
			if (n.getNodeType() == Node.ELEMENT_NODE)
			{
				Element element = (Element)n;
				
				try
				{
					parse(element, registry);
				}
				catch (Exception e)
				{
					System.err.println("Error while parsing "+n);
					e.printStackTrace();
				}
			}
		}
	}
	
	public static InputStream openStream(String name) throws Exception
	{
		if (name == null)
			return null;
		
		InputStream in = null;
		
		// Try classpath
		in = BlockRegistryParser.class.getClassLoader().getResourceAsStream(name);
		
		if (in == null)
		{
			in = new FileInputStream(new File(name) );
		}
		
		return in;
	}
	
	private void parse(Element element, BlockTypeRegistry registry)
	{
		BlockType blockType = null;
		
		// Every type has an id
		String idStr = element.getAttribute("id");
		IdDataPair id = parseIdDataPair(idStr);
		
		// Every type has a name
		String name = element.getAttribute("name");
		
		String nodeName = element.getTagName().toLowerCase();
		
		if (nodeName.equals("solid"))
		{
			SubTexture tex = parseTexture(element, "texture", null);
			SubTexture side = parseTexture(element, "side", tex);
			SubTexture top = parseTexture(element, "top", tex);
			
			String alphaTestStr = element.getAttribute("alphaTest");
			final boolean alphaTest = (alphaTestStr != null && alphaTestStr.equalsIgnoreCase("true"));
			
			blockType = new SolidBlockType(name, side, top, alphaTest);
		}
		else if (nodeName.equals("datasolid"))
		{
			ArrayList<SubTexture> sides = new ArrayList<SubTexture>();
			ArrayList<SubTexture> tops = new ArrayList<SubTexture>();
			
			int sideIndex = 0;
			while (true)
			{
				String attribName = "side"+sideIndex;
				sideIndex++;
				
				SubTexture side = parseTexture(element, attribName, null);
				if (side != null)
				{
					sides.add(side);
				}
				else
					break;
			}
			
			int topIndex = 0;
			while (true)
			{
				String attribName = "top"+topIndex;
				topIndex++;
				
				SubTexture top = parseTexture(element, attribName, null);
				if (top != null)
				{
					tops.add(top);
				}
				else
					break;
			}
			
			String alphaTestStr = element.getAttribute("alphaTest");
			final boolean alphaTest = (alphaTestStr != null && alphaTestStr.equalsIgnoreCase("true"));
			
			blockType = new DataSolid(name, sides.toArray(new SubTexture[0]), tops.toArray(new SubTexture[0]), alphaTest);
		}
		else if (nodeName.equals("air"))
		{
			blockType = new Air(name);
		}
		else if (nodeName.equals("water"))
		{
			SubTexture tex = parseTexture(element, "texture", null);
			
			blockType = new Water(name, tex);
		}
		else if (nodeName.equals("grass"))
		{
			SubTexture side = parseTexture(element, "dirtSide", null);
			SubTexture grassSide = parseTexture(element, "grassSide", null);
			SubTexture snowSide = parseTexture(element, "snowSide", null);
			SubTexture top = parseTexture(element, "top", null);
			SubTexture bottom = parseTexture(element, "bottom", null);

			String betterGrassMode = element.getAttribute("betterGrass");
			Grass.BetterGrassMode betterGrass =
					betterGrassMode == null ? Grass.BetterGrassMode.None
					: betterGrassMode.equalsIgnoreCase("fast") ? Grass.BetterGrassMode.Fast
					: betterGrassMode.equalsIgnoreCase("fancy") ? Grass.BetterGrassMode.Fancy
					: Grass.BetterGrassMode.None;
			
			blockType = new Grass(name, betterGrass, side, grassSide, snowSide, top, bottom, biomeCache, texturePack);
		}
		else if (nodeName.equals("sapling"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Sapling(name, texture);
			registry.register(id.id, id.data | 0x8, blockType);
		}
		else if (nodeName.equals("log"))
		{
			SubTexture side = parseTexture(element, "side", null);
			SubTexture top = parseTexture(element, "top", null);
			
			blockType = new Log(name, side, top);
		}
		else if (nodeName.equals("leaves"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			Color color = parseColor(element, "color", null);

			blockType = new Leaves(name, texture, color, biomeCache, texturePack);
			registry.register(id.id, id.data | 0x4, blockType);
			registry.register(id.id, id.data | 0x8, blockType);
		}
		else if (nodeName.equals("glass"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Glass(name, texture);
		}
		else if (nodeName.equals("bed"))
		{
			SubTexture headTop = parseTexture(element, "headTop", null);
			SubTexture footTop = parseTexture(element, "footTop", null);
			
			SubTexture headSide = parseTexture(element, "headSide", null);
			SubTexture footSide = parseTexture(element, "footSide", null);
			
			SubTexture headEdge = parseTexture(element, "headEdge", null);
			SubTexture footEdge = parseTexture(element, "footEdge", null);
			
			blockType = new Bed(headTop, footTop, headSide, footSide, headEdge, footEdge);
		}
		else if (nodeName.equals("dispenser"))
		{
			SubTexture top = parseTexture(element, "top", null);
			SubTexture side = parseTexture(element, "side", null);
			SubTexture front = parseTexture(element, "front", null);
			
			blockType = new Dispenser(top, side, front);
		}
		else if (nodeName.equals("minecarttracks"))
		{
			SubTexture straight = parseTexture(element, "straight", null);
			SubTexture corner = parseTexture(element, "corner", null);
			SubTexture powered= parseTexture(element, "powered", null);
			
			String isStraightStr = element.getAttribute("isStraightOnly");
			final boolean isStraightOnly = (isStraightStr != null && isStraightStr.equalsIgnoreCase("true"));
			
			blockType = new MinecartTracks(name, straight, corner, powered, isStraightOnly);
		}
		else if (nodeName.equals("plant"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Plant(name, texture);
		}
		else if (nodeName.equals("tallgrass"))
		{
			SubTexture dead = parseTexture(element, "dead", null);
			SubTexture tall = parseTexture(element, "tall", null);
			SubTexture fern = parseTexture(element, "fern", null);
			
			blockType = new TallGrass(name, dead, tall, fern, biomeCache, texturePack);
		}
		else if (nodeName.equals("wool"))
		{
			blockType = new Wool(name, texturePack);
		}
		else if (nodeName.equals("slab"))
		{
			SubTexture tex = parseTexture(element, "texture", null);
			SubTexture side = parseTexture(element, "side", tex);
			SubTexture top = parseTexture(element, "top", tex);
			
			blockType = new Slab(name, side, top);
			
			// upsidedown half-slab has bit 0x8
			registry.register(id.id, id.data | 0x8, blockType);
		}
		else if (nodeName.equals("doubleslab"))
		{
			System.out.println("Warning: DoubleSlab block type is obsolete. It will be removed in a future version. Use Solid instead.");
			
			SubTexture stoneSide = parseTexture(element, "stoneSide", null);
			SubTexture stoneTop = parseTexture(element, "stoneTop", null);
			
			SubTexture sandSide = parseTexture(element, "sandSide", null);
			SubTexture sandTop = parseTexture(element, "sandTop", null);
			
			SubTexture woodenSide = parseTexture(element, "woodenSide", null);
			SubTexture woodenTop = parseTexture(element, "woodenTop", null);
			
			SubTexture cobblestoneSide = parseTexture(element, "cobblestoneSide", null);
			SubTexture cobblestoneTop = parseTexture(element, "cobblestoneTop", null);
			
			SubTexture brickSide = parseTexture(element, "brickSide", null);
			SubTexture brickTop = parseTexture(element, "brickTop", null);
			
			SubTexture stoneBrickSide = parseTexture(element, "stoneBrickSide", null);
			SubTexture stoneBrickTop = parseTexture(element, "stoneBrickTop", null);
			
			blockType = new DoubleSlab(name, stoneSide, stoneTop, sandSide, sandTop, woodenSide, woodenTop, cobblestoneSide, cobblestoneTop, brickSide, brickTop, stoneBrickSide, stoneBrickTop);
		}
		else if (nodeName.equals("torch"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Torch(name, texture);
		}
		else if (nodeName.equals("stairs"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Stairs(name, texture);
		}
		else if (nodeName.equals("chest"))
		{
			SubTexture top = parseTexture(element, "top", null);
			SubTexture side = parseTexture(element, "side", null);
			SubTexture front = parseTexture(element, "front", null);
			SubTexture doubleSideLeft = parseTexture(element, "doubleSideLeft", null);
			SubTexture doubleSideRight = parseTexture(element, "doubleSideRight", null);
			SubTexture doubleFrontLeft = parseTexture(element, "doubleFrontLeft", null);
			SubTexture doubleFrontRight = parseTexture(element, "doubleFrontRight", null);
			
			blockType = new Chest(name, top, side, front,
									doubleSideLeft, doubleSideRight,
									doubleFrontLeft, doubleFrontRight );
		}
		else if (nodeName.equals("redstonewire"))
		{
			SubTexture offJunction = parseTexture(element, "offJunction", null);
			SubTexture onJunction = parseTexture(element, "onJunction", null);
			SubTexture offLine = parseTexture(element, "offLine", null);
			SubTexture onLine = parseTexture(element, "onLine", null);
			
			blockType = new RedstoneWire(offJunction, onJunction, offLine, onLine);
		}
		else if (nodeName.equals("workbench"))
		{
			SubTexture top = parseTexture(element, "top", null);
			SubTexture side1 = parseTexture(element, "side1", null);
			SubTexture side2 = parseTexture(element, "side2", null);
			
			blockType = new Workbench(name, top, side1, side2);
		}
		else if (nodeName.equals("wheat"))
		{
			SubTexture tex0 = parseTexture(element, "tex0", null);
			SubTexture tex1 = parseTexture(element, "tex1", null);
			SubTexture tex2 = parseTexture(element, "tex2", null);
			SubTexture tex3 = parseTexture(element, "tex3", null);
			SubTexture tex4 = parseTexture(element, "tex4", null);
			SubTexture tex5 = parseTexture(element, "tex5", null);
			SubTexture tex6 = parseTexture(element, "tex6", null);
			SubTexture tex7 = parseTexture(element, "tex7", null);
			
			blockType = new Wheat(name, tex0, tex1, tex2, tex3, tex4, tex5, tex6, tex7);
		}
		else if (nodeName.equals("soil"))
		{
			SubTexture top = parseTexture(element, "top", null);
			SubTexture side = parseTexture(element, "side", null);
			
			blockType = new Soil(name, top, side);
		}
		else if (nodeName.equals("furnace"))
		{
			SubTexture top = parseTexture(element, "top", null);
			SubTexture side = parseTexture(element, "side", null);
			SubTexture front = parseTexture(element, "front", null);
			
			blockType = new Furnace(name, top, side, front);
		}
		else if (nodeName.equals("sign"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			String hasPostStr = element.getAttribute("hasPost");
			final boolean hasPost = (hasPostStr != null && hasPostStr.equalsIgnoreCase("true"));
			
			blockType = new Sign(name, texture, hasPost);
		}
		else if (nodeName.equals("door"))
		{
			SubTexture top = parseTexture(element, "top", null);
			SubTexture bottom = parseTexture(element, "bottom", null);
			
			blockType = new Door(name, top, bottom);
		}
		else if (nodeName.equals("ladder"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Ladder(name, texture);
			
		}
		else if (nodeName.equals("pressureplate"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new PressurePlate(name, texture);
		}
		else if (nodeName.equals("button"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Button(name, texture);
		}
		else if (nodeName.equals("snow"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Snow(name, texture);
		}
		else if (nodeName.equals("ice"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Ice(name, texture);
		}
		else if (nodeName.equals("cactus"))
		{
			SubTexture side = parseTexture(element, "side", null);
			SubTexture top = parseTexture(element, "top", null);
			
			blockType = new Cactus(name, side, top);
		}
		else if (nodeName.equals("fence"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Fence(name, id.id, texture);
		}
		else if (nodeName.equals("fencegate"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new FenceGate(name, texture);
		}
		else if (nodeName.equals("pumpkin"))
		{
			SubTexture top = parseTexture(element, "top", null);
			SubTexture side = parseTexture(element, "side", null);
			SubTexture front = parseTexture(element, "front", null);
			
			blockType = new JackOLantern(name, top, side, front);
		}
		else if (nodeName.equals("cake"))
		{
			SubTexture top = parseTexture(element, "top", null);
			SubTexture side = parseTexture(element, "side", null);
			SubTexture interior = parseTexture(element, "interior", null);
			
			blockType = new Cake(name, top, side, interior);
		}
		else if (nodeName.equals("redstonerepeater"))
		{
			SubTexture base = parseTexture(element, "base", null);
			SubTexture side = parseTexture(element, "side", null);
			SubTexture torch = parseTexture(element, "torch", null);
			
			blockType = new RedstoneRepeater(name, base, side, torch);
		}
		else if (nodeName.equals("glasspane"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new GlassPane(name, id.id, texture);
		}
		else if (nodeName.equals("pistonbase"))
		{
			SubTexture side = parseTexture(element, "side", null);
			SubTexture top = parseTexture(element, "top", null);
			SubTexture bottom = parseTexture(element, "bottom", null);
			SubTexture pistonFace = parseTexture(element, "pistonFace", null);
			
			blockType = new PistonBase(name, side, top, bottom, pistonFace);
		}
		else if (nodeName.equals("pistonextension"))
		{
			SubTexture normalFace = parseTexture(element, "normalFace", null);
			SubTexture stickyFace = parseTexture(element, "stickyFace", null);
			SubTexture edge = parseTexture(element, "edge", null);
			
			blockType = new PistonExtension(name, edge, normalFace, stickyFace);
		}
		else if (nodeName.equals("vines"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Vines(name, texture, biomeCache, texturePack);
		}
		else if (nodeName.equals("fruitstem"))
		{
			SubTexture growingStem = parseTexture(element, "growingStem", null);
			SubTexture bentStem = parseTexture(element, "bentStem", null);
			
			String fruitIdStr = element.getAttribute("fruitId");
			final int fruitId = Integer.parseInt(fruitIdStr);
			
			blockType = new FruitStem(name, fruitId, growingStem, bentStem, biomeCache, texturePack);
		}
		else if (nodeName.equals("hugemushroom"))
		{
			SubTexture cap = parseTexture(element, "cap", null);
			SubTexture pores = parseTexture(element, "pores", null);
			SubTexture stem = parseTexture(element, "stem", null);
			
			blockType = new HugeMushroom(name, cap, pores, stem);
		}
		else if (nodeName.equals("fire"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Fire(name, texture);
		}
		else if (nodeName.equals("portal"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Portal(name, texture);
		}
		else if (nodeName.equals("lilly"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Lilly(name, texture, biomeCache, texturePack);
		}
		else if (nodeName.equals("dragonegg"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new DragonEgg(name, texture);
		}
		else if (nodeName.equals("netherwart"))
		{
			SubTexture tex0 = parseTexture(element, "texture0", null);
			SubTexture tex1 = parseTexture(element, "texture1", null);
			SubTexture tex2 = parseTexture(element, "texture2", null);
			
			blockType = new NetherWart(name, tex0, tex1, tex2);
		}
		else if (nodeName.equals("enderportalframe"))
		{
			SubTexture top = parseTexture(element, "top", null);
			SubTexture side = parseTexture(element, "side", null);
			SubTexture bottom = parseTexture(element, "bottom", null);
			SubTexture eye = parseTexture(element, "eye", null);
			
			blockType = new EnderPortalFrame(name, top, side, bottom, eye);
		}
		else if (nodeName.equals("enderportal"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new EnderPortal(name, texture);
		}
		else if (nodeName.equals("cauldron"))
		{
			SubTexture top = parseTexture(element, "top", null);
			SubTexture side = parseTexture(element, "side", null);
			SubTexture bottom = parseTexture(element, "bottom", null);
			SubTexture water = parseTexture(element, "water", null);
			
			blockType = new Cauldron(name, top, side, bottom, water);
		}
		else if (nodeName.equals("enchantmenttable"))
		{
			SubTexture top = parseTexture(element, "top", null);
			SubTexture side = parseTexture(element, "side", null);
			SubTexture bottom = parseTexture(element, "bottom", null);
			
			blockType = new EnchantmentTable(name, top, side, bottom);
		}
		else if (nodeName.equals("brewingstand"))
		{
			SubTexture base = parseTexture(element, "base", null);
			SubTexture stand = parseTexture(element, "stand", null);
	
			blockType = new BrewingStand(name, base, stand);
		}
		else if (nodeName.equals("trapdoor"))
		{
			SubTexture texture = parseTexture(element, "texture", null);
			
			blockType = new Trapdoor(name, texture);
		}
		else
		{
			System.err.println("Unrecognised block type: "+nodeName);
		}
		
		if (blockType != null)
		{	
			if (id.data == -1)
				registry.register(id.id, blockType);
			else
				registry.register(id.id, id.data, blockType);
		}
	}
	
	private SubTexture parseTexture(Element element, String attribName, SubTexture defaultTex)
	{
		if (!element.hasAttribute(attribName))
			return defaultTex;
		
		String texName = element.getAttribute(attribName);
		SubTexture result = texturePack.findTexture(texName);
		
		if (result == null)
			return defaultTex;
		
		return result;
	}
	
	private Color parseColor(Element element, String attribName, Color defaultColor)
	{
		if (!element.hasAttribute(attribName))
			return defaultColor;
		
		String colorName = element.getAttribute(attribName);
		Color result = parseHtmlColor(colorName);
		
		if (result == null)
			return defaultColor;
		
		return result;
	}
	
	private Color parseHtmlColor(String colorStr)
	{
		Color result = null;
		if (colorStr != null && colorStr != "" && colorStr.charAt(0) == '#')
		{
			try {
				result = new Color(Integer.parseInt(colorStr.substring(1,7), 16));
			} catch (Exception e) {}
		}
		return result;
	}
	
	private static Element loadXml(String resource, String rootName)
	{
		try
		{
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			
			InputStream in = openStream(resource);
			
			Document doc = docBuilder.parse(in);
			NodeList nodeList = doc.getElementsByTagName(rootName);
			Element root = (Element)nodeList.item(0);
			
			in.close();
			
			return root;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	private static IdDataPair parseIdDataPair(String str)
	{
		final int colonPos = str.indexOf(':');
		if (colonPos == -1)
		{
			final int id = Integer.parseInt(str);
			return new IdDataPair(id, -1);
		}
		else
		{
			String idStr = str.substring(0, colonPos);
			String dataStr = str.substring(colonPos+1);
			
			final int id = Integer.parseInt(idStr);
			final int data = Integer.parseInt(dataStr);
			
			return new IdDataPair(id, data);
		}
	}
	
}
