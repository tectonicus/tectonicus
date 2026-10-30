/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.PlayerIconAssembler;
import tectonicus.configuration.DimensionConfig;
import tectonicus.raw.CustomNameTag;
import tectonicus.raw.ContainerEntity;
import tectonicus.raw.EnchantmentTag;
import tectonicus.raw.EnchantmentsTag;
import tectonicus.raw.Item;
import tectonicus.raw.Player;

import javax.imageio.ImageIO;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

class OutputResourcesUtilTest {
	@TempDir Path directory;

	@Test
	void exportsEnderChestMarkerTypeAndPositionAlongsideOrdinaryChests() throws Exception {
		var chests = new ConcurrentLinkedQueue<ContainerEntity>();
		chests.add(new ContainerEntity(-17, -36, -1, 15, 28, 15, "Ender Chest", "", false,
				"south", "single", List.of(), true));
		chests.add(new ContainerEntity(8, 64, 8, 8, 64, 8, "Chest", "", false,
				"south", "single", List.of(new Item("minecraft:stone", 0, 32, 0, List.of()))));
		Path output = directory.resolve("chests.js");
		OutputResourcesUtil.outputChests(output.toFile(), new DimensionConfig("Overworld"), chests);
		String javascript = Files.readString(output);
		assertTrue(javascript.contains("enderChest: true"));
		assertEquals(1, javascript.split("enderChest: true", -1).length - 1);
		assertTrue(javascript.contains("worldPos: new WorldCoord(-16.5, -36.0, -0.5)"));
		assertTrue(javascript.contains("name: \"Ender Chest\""));
		assertTrue(javascript.contains("name: \"Chest\""));
		assertTrue(javascript.contains("id: \"minecraft:stone\""));
	}

	@Test
	void exportsPlayerInventoryWithItemDetailsAndEscapedNames() throws Exception {
		Player player = new Player();
		player.setName("Alex");
		player.getInventory().add(new Item("minecraft:diamond_sword", 0, 1, -106, List.of(
				new CustomNameTag("Alex's \\\"Sword\\\"\nSecond line"),
				new EnchantmentsTag(List.of(new EnchantmentTag("minecraft:sharpness", 3))))));
		player.getEnderChestInventory().add(new Item("minecraft:ender_pearl", 0, 16, 26, List.of(
				new CustomNameTag("Stashed pearls"))));
		Player empty = new Player();
		empty.setName("Steve");
		PlayerIconAssembler icons = new PlayerIconAssembler(null) {
			@Override
			public void writeIcon(Player ignored, java.io.File file) {}
		};
		Path output = directory.resolve("players.js");
		DimensionConfig visible = new DimensionConfig("Overworld");
		visible.setShowPlayerInventories(true);
		OutputResourcesUtil.outputPlayers(output.toFile(), directory.resolve("icons").toFile(),
				visible, List.of(player, empty), icons);

		String javascript = Files.readString(output);
		assertTrue(javascript.contains("inventory: ["));
		assertTrue(javascript.contains("id: \"minecraft:diamond_sword\""));
		assertTrue(javascript.contains("count: 1, slot: -106"));
		assertTrue(javascript.contains("customName: " + FileUtils.getOBJECT_MAPPER().writeValueAsString("Alex's \\\"Sword\\\"\nSecond line")));
		assertTrue(javascript.contains("enchantments: [{ id: \"minecraft:sharpness\", level: 3 }"));
		assertTrue(javascript.contains("inventory: []"));
		assertTrue(javascript.contains("enderChestInventory: [\t\t\t{ id: \"minecraft:ender_pearl\""));
		assertTrue(javascript.contains("customName: \"Stashed pearls\", count: 16, slot: 26"));
		assertTrue(javascript.contains("enderChestInventory: []"));

		DimensionConfig hidden = new DimensionConfig("Hidden");
		OutputResourcesUtil.outputPlayers(output.toFile(), directory.resolve("icons").toFile(),
				hidden, List.of(player), icons);
		String hiddenJavascript = Files.readString(output);
		assertTrue(hiddenJavascript.contains("Alex"));
		assertTrue(hiddenJavascript.contains("health:"));
		assertFalse(hiddenJavascript.contains("inventory:"));
		assertFalse(hiddenJavascript.contains("enderChestInventory:"));
		assertFalse(hiddenJavascript.contains("minecraft:diamond_sword"));
		assertFalse(hiddenJavascript.contains("Stashed pearls"));
	}

	@ParameterizedTest
	@ValueSource(ints = {2, 4, 8})
	void exportsCustomGrayscaleAlphaImagesAtRequestedSize(int size) throws Exception {
		ComponentColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
				new int[] {8, 8}, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
		BufferedImage source = new BufferedImage(colorModel,
				Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 4, 4, 2, null), false, null);
		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				source.getRaster().setSample(x, y, 0, y < 2 ? 255 : 0);
				source.getRaster().setSample(x, y, 1, x < 2 ? 255 : y < 2 ? 128 : 0);
			}
		}
		Path input = directory.resolve("font.png");
		ImageIO.write(source, "png", input.toFile());
		BufferedImage font = ImageIO.read(input.toFile());
		assertEquals(BufferedImage.TYPE_CUSTOM, font.getType());
		Path output = directory.resolve("exported.png");

		OutputResourcesUtil.writeImage(font, size, size, output.toFile());

		assertTrue(Files.isRegularFile(output), "The font must be exported even when its color model is custom");
		BufferedImage exported = ImageIO.read(output.toFile());
		assertNotNull(exported);
		assertEquals(size, exported.getWidth());
		assertEquals(size, exported.getHeight());
		assertEquals(0xffffffff, exported.getRGB(0, 0));
		assertEquals(0x80ffffff, exported.getRGB(size - 1, 0));
		assertEquals(0xff000000, exported.getRGB(0, size - 1));
		assertEquals(0, exported.getRGB(size - 1, size - 1) >>> 24);
	}
}
