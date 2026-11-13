/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.PlayerIconAssembler;
import tectonicus.blockregistry.BlockRegistry;
import tectonicus.itemregistry.ItemRegistry;
import tectonicus.itemmodeldefinitionregistry.ItemModelDefinitionRegistry;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.WorldConfig;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.raw.ArmorTrimTag;
import tectonicus.raw.ContainerEntity;
import tectonicus.raw.CustomNameTag;
import tectonicus.raw.DyedColorTag;
import tectonicus.raw.EnchantmentTag;
import tectonicus.raw.EnchantmentsTag;
import tectonicus.raw.Item;
import tectonicus.raw.PotionContentsTag;
import tectonicus.configuration.DimensionConfig;
import tectonicus.texture.TexturePack;
import tectonicus.texture.ZipStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Base64;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class WorldResourcesTest {
	@TempDir
	Path directory;

	@Test
	void differentWorldResourcesDoNotOverwriteImagesOrTranslations() throws Exception {
		Configuration config = new Configuration();
		for (int i = 0; i < 2; i++) {
			String id = "World" + i;
			int color = i == 0 ? 0xff123456 : 0xffabcdef;
			Path jar = createJar(id, color);
			TexturePack textures = new TexturePack(rasteriser(), jar.toFile(), null, false, List.of(), List.of());
			try {
				WorldConfig world = new WorldConfig(id, directory.toFile());
				WorldViewerAssets viewerAssets = new WorldViewerAssets();
				Item helmet = new Item("minecraft:leather_helmet", 0, 1, 0, List.of(new DyedColorTag(5),
						new CustomNameTag("{translate:custom.helmet,color:gold}"),
						new EnchantmentsTag(List.of(new EnchantmentTag("minecraft:protection", 1)))));
				Item potion = new Item("minecraft:potion", 0, 1, 1, List.of(new PotionContentsTag("minecraft:long_swiftness")));
				Item armor = new Item("minecraft:leather_chestplate", 0, 1, 2, List.of(new ArmorTrimTag("minecraft:gold", "minecraft:sentry")));
				exportInventory(world, "Overworld", viewerAssets, helmet, potion);
				exportInventory(world, "Nether", viewerAssets, helmet, armor);
				viewerAssets.addBeaconEffect("speed");
				Files.createDirectories(directory.resolve(id + "/Images"));
				OutputResourcesUtil.outputInventoryItemIcons(directory.resolve(id + "/Images").toFile(), rasteriser(), textures,
						null, new BlockRegistry(textures), new ItemRegistry(textures), new ItemModelDefinitionRegistry(textures));
				OutputResourcesUtil.outputWorldResources(textures, new PlayerIconAssembler(null), config,
						directory.toFile(), world, viewerAssets, MapLocalization.load(config));
			} finally {
				var field = ZipStack.class.getDeclaredField("base");
				field.setAccessible(true);
				((FileSystem) field.get(textures.getZipStack())).close();
			}
		}
		for (int i = 0; i < 2; i++) {
			String id = "World" + i;
			int color = i == 0 ? 0xff123456 : 0xffabcdef;
			for (String name : List.of("Sign.png", "FullHeart.png", "Font.png", "EnchantedGlint.png",
					"PlayerInventory.png", "PlayerInventoryWithEnderChest.png",
					"PlayerIcons/Tectonicus_Default_Player_Icon.png", "effects/speed.png", "Items/leather_helmet.png", "Items/leather_helmet_overlay.png")) {
				BufferedImage image = ImageIO.read(directory.resolve(id + "/Images/" + name).toFile());
				assertEquals(color, image.getRGB(16 < image.getWidth() ? 16 : 0, 0), name);
			}
			var assets = readScriptPayload(id, "localizations.js", "registerWorldLocalizations");
			assertTrue(assets.get("hasFont").asBoolean());
			var translations = assets.get("translations");
			assertEquals(id + " \"Helmet\"", translations.get("custom.helmet").asString());
			assertEquals("Inventory", translations.get("container.inventory").asString());
			assertEquals("Leather Helmet", translations.get("item.minecraft.leather_helmet").asString());
			assertEquals("Swiftness", translations.get("item.minecraft.potion.effect.swiftness").asString());
			assertEquals("Protection", translations.get("enchantment.minecraft.protection").asString());
			assertEquals("Sentry", translations.get("trim_pattern.minecraft.sentry").asString());
			assertEquals("Gold", translations.get("trim_material.minecraft.gold").asString());
			assertEquals("Speed", translations.get("effect.minecraft.speed").asString());
			assertEquals(8, translations.size());
			var masks = readScriptPayload(id, "masks.js", "registerWorldMasks");
			assertEquals(3, masks.size());
			for (String filename : List.of("Font.png", "Items/leather_helmet.png", "Items/leather_helmet_overlay.png")) {
				String expected = Base64.getEncoder().encodeToString(Files.readAllBytes(directory.resolve(id + "/Images/" + filename)));
				assertEquals("data:image/png;base64," + expected, masks.get(filename).asString());
			}
			assertFalse(Files.exists(directory.resolve("Scripts/" + id + "/localizations.json")));
		}
		assertFalse(Files.exists(directory.resolve("Images/FullHeart.png")));
		assertFalse(Files.exists(directory.resolve("Scripts/localizations.json")));
	}

	private void exportInventory(WorldConfig world, String dimension, WorldViewerAssets assets, Item... items) {
		DimensionConfig map = new DimensionConfig(dimension);
		world.addDimension(map);
		var containers = new ConcurrentLinkedQueue<ContainerEntity>();
		containers.add(new ContainerEntity(0, 64, 0, 0, 64, 0, "Chest", "", false, null, "single", List.of(items), false));
		OutputResourcesUtil.outputContainers(directory.resolve(world.getId() + dimension + ".js").toFile(), map, containers, assets);
	}

	private tools.jackson.databind.JsonNode readScriptPayload(String worldId, String filename, String registration) throws Exception {
		String script = Files.readString(directory.resolve("Scripts/" + worldId + "/" + filename));
		String prefix = registration + "(\"" + worldId + "\",JSON.parse(";
		assertTrue(script.startsWith(prefix));
		assertTrue(script.endsWith("));\n"));
		var reader = FileUtils.getOBJECT_MAPPER().reader();
		return reader.readTree(reader.readTree(script.substring(prefix.length(), script.length() - 4)).asString());
	}

	private Rasteriser rasteriser() {
		return (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
				(proxy, method, args) -> {
					if (method.getName().equals("getMaxTextureSize")) return 16384;
					if (!method.getName().equals("createTexture")) throw new AssertionError(method.getName());
					BufferedImage image = (BufferedImage) args[0];
					return new Texture() {
						@Override public int getWidth() { return image.getWidth(); }
						@Override public int getHeight() { return image.getHeight(); }
					};
				});
	}

	private Path createJar(String name, int color) throws Exception {
		Path jar = directory.resolve(name + ".jar");
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
			out.putNextEntry(new ZipEntry("version.json"));
			out.write("{\"name\":\"1.21.11\",\"pack_version\":{\"resource\":75}}".getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("assets/minecraft/lang/en_us.json"));
			out.write(FileUtils.getOBJECT_MAPPER().writeValueAsBytes(java.util.Map.of(
					"custom.helmet", name + " \"Helmet\"", "container.inventory", "Inventory",
					"item.minecraft.leather_helmet", "Leather Helmet", "item.minecraft.potion.effect.swiftness", "Swiftness",
					"enchantment.minecraft.protection", "Protection", "effect.minecraft.speed", "Speed",
					"trim_pattern.minecraft.sentry", "Sentry", "trim_material.minecraft.gold", "Gold",
					"item.minecraft.potion.desc", 5, "unused.translation", "unused")));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("assets/minecraft/models/item/leather_helmet.json"));
			out.write("{\"parent\":\"builtin/generated\",\"textures\":{\"layer0\":\"item/leather_helmet\",\"layer1\":\"item/leather_helmet_overlay\"}}".getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
			for (String imagePath : List.of("gui/sprites/hud/heart/container", "gui/sprites/hud/heart/half", "gui/sprites/hud/heart/full",
					"gui/sprites/hud/food_empty", "gui/sprites/hud/food_half", "gui/sprites/hud/food_full",
					"gui/sprites/hud/air_bursting", "gui/sprites/hud/air", "gui/container/generic_54", "gui/container/beacon",
					"item/oak_sign", "item/painting", "item/iron_ingot", "item/gold_ingot", "item/diamond", "item/compass_16",
					"entity/player/wide/steve", "misc/enchanted_glint_item", "mob_effect/speed", "font/ascii", "colormap/grass", "colormap/foliage",
					"item/leather_helmet", "item/leather_helmet_overlay")) {
				int size = imagePath.contains("container/") || imagePath.startsWith("colormap/") ? 256
						: imagePath.startsWith("font/") ? 128 : imagePath.startsWith("entity/") ? 64 : 16;
				BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
				for (int y = 0; y < size; y++)
					for (int x = 0; x < size; x++) image.setRGB(x, y, color);
				out.putNextEntry(new ZipEntry("assets/minecraft/textures/" + imagePath + ".png"));
				ImageIO.write(image, "png", out);
				out.closeEntry();
			}
		}
		return jar;
	}
}
