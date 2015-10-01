/*
 * Copyright (c) 2012-2015, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import tectonicus.blockTypes.BlockModel;
import tectonicus.blockTypes.BlockModel.BlockElement;
import tectonicus.blockTypes.BlockModel.BlockElement.ElementFace;
import tectonicus.blockTypes.BlockRegistry;
import tectonicus.blockTypes.BlockVariant;
import tectonicus.blockTypes.BlockVariant.VariantModel;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.TextureFilter;
import tectonicus.rasteriser.lwjgl.LwjglTexture;
import tectonicus.rasteriser.lwjgl.LwjglTextureUtils;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;
import tectonicus.texture.ZipStack.ZipStackEntry;
import tectonicus.util.Vector3f;

public class BlockVariantTests
{
	private TexturePack texturePack;
	private Map<String, BlockModel> blockModels;
	private int modelTotal;
	private ZipStack zips;
	
	@Before
	public void setUp()
	{
		try {
			zips = new ZipStack(Minecraft.findMinecraftJar(), null, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		modelTotal = 0;
		
//		try {
//			Thread.sleep(20 * 1000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	@Test
	public void createSingleStateMap()
	{
		BlockVariant bv = new BlockVariant("normal", null);
		Map<String, String> states = bv.getStates();
		
		Map<Object, Object> testStates = new HashMap<>();
		testStates.put("normal", "");
		
		assertTrue(states.equals(testStates));
	}
	
	@Test
	public void createMultipleStatesMap()
	{
		BlockVariant bv = new BlockVariant("attached=false,facing=north,powered=false,suspended=false", null);
		Map<String, String> states = bv.getStates();
		
		Map<Object, Object> testStates = new HashMap<>();
		testStates.put("attached", "false");
		testStates.put("facing", "north");
		testStates.put("powered", "false");
		testStates.put("suspended", "false");
		
		assertTrue(states.equals(testStates));
	}
//	
//	@Test
//	public void createModelVariants() throws Exception
//	{
//		List<Map<String, BlockVariant>> blockStates = new ArrayList<>();  //List of all blockstates
//		Map<String, BlockVariant> vMap = new HashMap<>();  //Map of variants of a single blockstate
//		blockModels = new HashMap<>();
//		
//		Enumeration<? extends ZipEntry> entries = zips.getBaseEntries();
//		while(entries.hasMoreElements())
//		{
//			ZipEntry entry = entries.nextElement();
//			if(entry.getName().contains("blockstates"))
//			{
//				ZipStackEntry zse = zips.getEntry(entry.getName());
//				vMap = loadVariants(loadJSON(zse.getInputStream()));
//				blockStates.add(vMap);
//				//System.out.println(entry.getName());
//			}
//		}
//		System.out.println(modelTotal);
//		System.out.println(blockStates.size());
//		System.out.println(blockModels.size());
//		//BlockVariant bv = vMap.get("snowy=false");
//		//assertTrue(bv.getModels() != null);
//	}	
	
//	@Test
//	public void testGetModel() throws Exception
//	{
//		org.lwjgl.opengl.Display.create();
//		blockModels = new HashMap<>();
//		JSONObject model = new JSONObject( "{ \"model\": \"tripwire_hook_attached_suspended\", \"y\": 180 }");
//		VariantModel vm = getModel(model);
//		assertThat(vm.getModelPath(), equalTo("tripwire_hook_attached_suspended"));
//	}

//	@Test
//	public void testLoadModel() throws Exception
//	{
//		//texturePack = new TexturePack(null, Minecraft.findMinecraftJar(), null, null);
//		Map<String, String> textureMap = new HashMap<>();
//		//zips = new ZipStack(Minecraft.findMinecraftJar(), null, null);
//		BlockModel bm = loadModel("block/tripwire_hook", zips, textureMap);
//		assertThat(bm.getElements().size(), equalTo(7));
//		bm = loadModel("block/anvil_undamaged", zips, textureMap);
//		assertThat(bm.getElements().size(), equalTo(4));
//	}
		
	//TODO:  Move and rewrite this test once we start parsing the block variants
	@Test
	public void createBlockVariantMap()
	{
		Map<String, BlockVariant> variants = new HashMap<>();
		
		BlockVariant bv1 = new BlockVariant("attached=true,facing=south,powered=false,suspended=false", null);
		BlockVariant bv2 = new BlockVariant("attached=true,facing=south,powered=true,suspended=false", null);
		BlockVariant bv3 = new BlockVariant("attached=true,facing=west,powered=true,suspended=true", null);
		
		variants.put(bv1.getName(), bv1);
		variants.put(bv2.getName(), bv2);
		variants.put(bv3.getName(), bv3);
		
		assertTrue(variants.containsKey("attached=true,facing=west,powered=true,suspended=true"));
	}
	
	@Test
	public void testBlockModel()
	{
		BlockModel bm = new BlockModel("", false, null);
		BlockElement test = new BlockElement(null, null, null, "", 0, false, false, new HashMap<String, ElementFace>());
		test.getFrom();
	}
	
	@Test
	public void testLoadEverything()
	{
		try {
			Display.setDisplayMode(new DisplayMode(300, 300));
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
		BlockRegistry test = new BlockRegistry();
		test.deserialize();
	}
}
