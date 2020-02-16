/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class BlockVariantTests
{	
	@BeforeEach
	public void setUp()
	{	
//		try {
//			Thread.sleep(20 * 1000);
//		} catch (InterruptedException e) {
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
		
		assertThat(states, is(equalTo(testStates)));
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
		
		assertThat(states, is(equalTo(testStates)));
	}
	
	@Test
	public void testDeserializeVariantSingleModel() throws JsonSyntaxException
	{
		JsonElement variant = JsonParser.parseString("{ \"model\": \"acacia_fence_n\", \"y\": 90, \"uvlock\": true }");
		BlockVariant bv = BlockVariant.deserializeVariant("east=true,north=false,south=false,west=false", variant);
		
		assertThat(bv.getModels().size(), is(equalTo(1)));
		assertThat(bv.getModels().get(0).getModel(), is("acacia_fence_n"));
	}
	
	@Test
	public void testDeserializeVariantMultipleModels() throws JsonSyntaxException
	{
		JsonElement variant = JsonParser.parseString("[{ \"model\": \"grass_normal\" }, { \"model\": \"grass_normal\", \"y\": 90 },{ \"model\": \"grass_normal\", \"y\": 180 },{ \"model\": \"grass_normal\", \"y\": 270 }]");
		BlockVariant bv = BlockVariant.deserializeVariant("snowy=false", variant);
		
		assertThat(bv.getModels().size(), is(equalTo(4)));
		assertThat(bv.getModels().get(2).getModel(), is("grass_normal"));
	}

	//TODO: Fix this test once we've switched to using Gson
//	@Test
//	public void testLoadModel() throws Exception
//	{
//		ZipStack zips = new ZipStack(Minecraft.findMinecraftJar(), null, null);
//		Rasteriser rasteriser = RasteriserFactory.createRasteriser(RasteriserType.Lwjgl, DisplayType.Window, 300, 300, 24, 8, 24, 4);
//		BlockRegistry br = new BlockRegistry(rasteriser);
//		Map<String, String> textureMap = new HashMap<>();
//		BlockModel bm = br.loadModel("block/tripwire_hook", zips, textureMap);
//		assertThat(bm.getElements().size(), equalTo(7));
//		bm = br.loadModel("block/anvil_undamaged", zips, textureMap);
//		rasteriser.destroy();
//		assertThat(bm.getElements().size(), equalTo(4));
//	}
	
//	@Test
//	public void testBlockModel()
//	{
//		BlockModel bm = new BlockModel("", false, null);
//		BlockElement test = new BlockElement(null, null, null, "", 0, false, false, new HashMap<String, ElementFace>());
//		test.getFrom();
//	}
	
	//TODO: This test assumes minecraft.jar is located on the system.  Need to add a resource pack to the unit test data instead.
//	@Test
//	public void testDeserializeBlockStates()
//	{
//		BlockRegistry test = new BlockRegistry();
//		test.deserializeBlockstates();
//		Map<String, List<BlockVariant>> blockStates = test.getBlockStates();
//
//		assertFalse(blockStates.isEmpty());
//		assertThat(blockStates.size(), is(equalTo(355)));
//		assertThat(blockStates.containsKey("minecraft:acacia_door"), is(equalTo(true)));
//	}
	
	//TODO: This test is broken because we can't handle the new 1.9 "multipart" format.  Fix this once we've switched to Gson
//	@Test
//	public void testLoadModels() throws Exception
//	{
//		Rasteriser rasteriser = RasteriserFactory.createRasteriser(RasteriserType.Lwjgl, DisplayType.Window, 300, 300, 24, 8, 24, 4);
//	
//		BlockRegistry test = new BlockRegistry(rasteriser);
//		test.deserializeBlockstates();
//		
//		try {
//			test.loadModels();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		rasteriser.destroy();
//		Map<String, BlockModel> models = test.getBlockModels();
//		
//		assertThat(models.size(), is(equalTo(937)));
//	}
}
