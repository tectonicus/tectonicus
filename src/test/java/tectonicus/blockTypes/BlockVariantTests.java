/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.lwjgl.glfw.GLFW;
import tectonicus.configuration.Configuration;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;


class BlockVariantTests
{
	ObjectMapper mapper = new ObjectMapper();

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
	void createSingleStateMap()
	{
		BlockVariant bv = new BlockVariant("normal", null);
		Map<String, String> states = bv.getStates();
		
		Map<Object, Object> testStates = new HashMap<>();
		testStates.put("normal", "");
		
		assertThat(states, is(equalTo(testStates)));
	}
	
	@Test
	void createMultipleStatesMap()
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

	@EnabledIfSystemProperty(named = "os.name", matches = "Windows 10")
	@Test
	void testDeserializeVariantSingleModel() throws JsonProcessingException {
		System.out.println(System.getProperty("os.name") + " " + System.getProperty("os.version"));
		JsonNode variant = mapper.readTree("{ \"model\": \"acacia_fence_n\", \"y\": 90, \"uvlock\": true }");
		BlockRegistry registry = new BlockRegistry();
		List<BlockStateModel> models = registry.deserializeBlockStateModels(variant);

		assertThat(models.size(), is(equalTo(1)));
		assertThat(models.get(0).getModel(), is("acacia_fence_n"));
	}

	@EnabledIfSystemProperty(named = "os.name", matches = "Windows 10")
	@Test
	void testDeserializeVariantMultipleModels() throws JsonProcessingException {
		JsonNode variant = mapper.readTree("[{ \"model\": \"grass_normal\" }, { \"model\": \"grass_normal\", \"y\": 90 },{ \"model\": \"grass_normal\", \"y\": 180 },{ \"model\": \"grass_normal\", \"y\": 270 }]");
		BlockRegistry registry = new BlockRegistry();
		List<BlockStateModel> models = registry.deserializeBlockStateModels(variant);
		
		assertThat(models.size(), is(equalTo(4)));
		assertThat(models.get(2).getModel(), is("grass_normal"));
	}

	//TODO: This test is very outdated
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
//	void testDeserializeBlockStates()
//	{
//		BlockRegistry test = new BlockRegistry();
//		test.deserializeBlockstates();
//		Map<String, BlockStateWrapper> blockStates = test.getBlockStates();
//
//		assertFalse(blockStates.isEmpty());
//		assertThat(blockStates.size(), is(equalTo(764))); //MC 1.16.2
//		assertThat(blockStates.containsKey("minecraft:acacia_door"), is(equalTo(true)));
//	}

	//TODO: This is more of an integration test as it initializes a rasterizer window. Should it stay here?
//	@Test
//	void testLoadModels() throws Exception
//	{
//		int width = 800;
//		int height = 800;
//
//		Rasteriser rasteriser = RasteriserFactory.createRasteriser(Configuration.RasteriserType.LWJGL, RasteriserFactory.DisplayType.Window, width, height, 24, 8, 24, 4);
//		long windowId = rasteriser.getWindowId();
//
//		BlockRegistry br = new BlockRegistry(rasteriser);
//		br.deserializeBlockstates();
//
//		try {
//			br.loadModels();
//			GLFW.glfwDestroyWindow(windowId);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		Map<String, BlockModel> models = br.getBlockModels();
//
////		assertThat(models.size(), is(equalTo(937))); // 1.8
//		assertThat(models.size(), is(equalTo(1252))); // 1.16
//	}
}
