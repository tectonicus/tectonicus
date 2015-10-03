/*
 * Copyright (c) 2012-2015, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.Is.*;

import tectonicus.blockTypes.BlockModel;
import tectonicus.blockTypes.BlockModel.BlockElement;
import tectonicus.blockTypes.BlockModel.BlockElement.ElementFace;
import tectonicus.blockTypes.BlockRegistry;
import tectonicus.blockTypes.BlockVariant;
import tectonicus.blockTypes.BlockVariant.VariantModel;
import tectonicus.texture.ZipStack;

public class BlockVariantTests
{	
	@Before
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
	
	@Test
	public void testDeserializeVariantSingleModel() throws JSONException
	{
		JSONObject variant = new JSONObject("{ \"model\": \"acacia_fence_n\", \"y\": 90, \"uvlock\": true }");
		BlockVariant bv = BlockVariant.deserializeVariant("east=true,north=false,south=false,west=false", variant);
		
		assertThat(bv.getModels().size(), is(equalTo(1)));
		assertThat(bv.getModels().get(0).getModelPath(), is(equalTo("acacia_fence_n")));
	}
	
	@Test
	public void testDeserializeVariantMultipleModels() throws JSONException
	{
		JSONArray variant = new JSONArray("[{ \"model\": \"grass_normal\" }, { \"model\": \"grass_normal\", \"y\": 90 },{ \"model\": \"grass_normal\", \"y\": 180 },{ \"model\": \"grass_normal\", \"y\": 270 }]");
		BlockVariant bv = BlockVariant.deserializeVariant("snowy=false", variant);
		
		assertThat(bv.getModels().size(), is(equalTo(4)));
		assertThat(bv.getModels().get(2).getModelPath(), is(equalTo("grass_normal")));
	}
	
	@Test
	public void testDeserializeVariantModel() throws Exception
	{
		JSONObject model = new JSONObject( "{ \"model\": \"tripwire_hook_attached_suspended\", \"y\": 180 }");
		VariantModel vm = VariantModel.deserializeVariantModel(model);
		assertThat(vm.getModelPath(), equalTo("tripwire_hook_attached_suspended"));
	}

	@Test
	public void testLoadModel() throws Exception
	{
		Display.setDisplayMode(new DisplayMode(300, 300));
		Display.create();
		
		ZipStack zips = new ZipStack(Minecraft.findMinecraftJar(), null, null);

		BlockRegistry br = new BlockRegistry();
		Map<String, String> textureMap = new HashMap<>();
		BlockModel bm = br.loadModel("block/tripwire_hook", zips, textureMap);
		assertThat(bm.getElements().size(), equalTo(7));
		bm = br.loadModel("block/anvil_undamaged", zips, textureMap);
		Display.destroy();
		assertThat(bm.getElements().size(), equalTo(4));
	}
	
	@Test
	public void testBlockModel()
	{
		BlockModel bm = new BlockModel("", false, null);
		BlockElement test = new BlockElement(null, null, null, "", 0, false, false, new HashMap<String, ElementFace>());
		test.getFrom();
	}
	
	@Test
	public void testDeserializeBlockStates()
	{
		BlockRegistry test = new BlockRegistry();
		test.deserializeBlockstates();
		
		Map<String, List<BlockVariant>> blockStates = test.getBlockStates();
		
		assertFalse(blockStates.isEmpty());
		assertThat(blockStates.size(), is(equalTo(340)));
		assertThat(blockStates.containsKey("minecraft:acacia_door"), is(equalTo(true)));
	}
	
	@Test
	public void testLoadModels() throws Exception
	{
		Display.setDisplayMode(new DisplayMode(300, 300));
		Display.create();
		
		BlockRegistry test = new BlockRegistry();
		test.deserializeBlockstates();
		
		try {
			test.loadModels();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Display.destroy();
		Map<String, BlockModel> models = test.getBlockModels();
		
		assertThat(models.size(), is(equalTo(937)));
	}
}
