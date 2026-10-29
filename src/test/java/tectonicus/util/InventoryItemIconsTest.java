/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.util;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.BlockTypeRegistry;
import tectonicus.BlockContext;
import tectonicus.Minecraft;
import tectonicus.blockTypes.ChestNew;
import tectonicus.blockTypes.Banner;
import tectonicus.blockTypes.ArmorStand;
import tectonicus.blockTypes.Conduit;
import tectonicus.blockregistry.BlockRegistry;
import tectonicus.configuration.Configuration;
import tectonicus.itemmodeldefinitionregistry.ItemModelDefinitionRegistry;
import tectonicus.itemregistry.ItemRegistry;
import tectonicus.itemregistry.ShieldModel;
import tectonicus.raw.ArmorStandEntity;
import tectonicus.raw.Item;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
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
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class InventoryItemIconsTest {
	@TempDir Path directory;
	private final List<Vector3f> emittedPositions = new ArrayList<>();
	private final List<Vector4f> emittedColours = new ArrayList<>();
	private static final List<String> DEFINITION_ONLY_ITEMS = List.of("chest", "trapped_chest", "waxed_copper_chest",
			"red_shulker_box", "dragon_head", "player_head", "piglin_head", "red_banner", "decorated_pot");

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void exportsEntityGeometryWithLegacyAndParentlessItemModels(boolean modern) throws Exception {
		Path jar = directory.resolve("client.jar");
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
			json(out, "version.json", "{\"name\":\"1.21.11\",\"pack_version\":{\"resource\":75}}");
			json(out, "assets/minecraft/models/item/conduit.json", modern
					? "{\"textures\":{\"particle\":\"block/conduit\"}}" : "{\"parent\":\"builtin/entity\"}");
			json(out, "assets/minecraft/models/item/ender_chest.json", modern
					? "{\"parent\":\"minecraft:item/template_chest\"}" : "{\"parent\":\"builtin/entity\"}");
			json(out, "assets/minecraft/models/item/template_chest.json", "{}");
			if (modern) {
				json(out, "assets/minecraft/models/item/canonical.json", """
						{"parent":"builtin/generated","textures":{"layer0":"minecraft:entity/conduit/base"}}
						""");
				json(out, "assets/minecraft/items/alias.json", "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"minecraft:item/canonical\"}}");
				json(out, "assets/minecraft/items/shield.json", """
						{"model":{"type":"minecraft:condition","on_false":{"type":"minecraft:special",
						"base":"minecraft:item/shield","model":{"type":"minecraft:shield"}}}}
						""");
				for (String item : DEFINITION_ONLY_ITEMS) {
					String special = "{\"type\":\"minecraft:special\",\"base\":\"minecraft:item/" + item
							+ "\",\"model\":{\"type\":\"minecraft:chest\"}}";
					if (item.equals("chest") || item.equals("trapped_chest"))
						special = "{\"type\":\"minecraft:select\",\"fallback\":" + special + "}";
					json(out, "assets/minecraft/items/" + item + ".json", "{\"model\":" + special + "}");
				}
				json(out, "assets/minecraft/items/conduit.json", """
						{"model":{"type":"minecraft:special","base":"minecraft:item/conduit",
						"model":{"type":"minecraft:conduit"}}}
						""");
				json(out, "assets/minecraft/items/ender_chest.json", """
						{"model":{"type":"minecraft:special","base":"minecraft:item/ender_chest",
						"model":{"type":"minecraft:chest","texture":"minecraft:ender"}}}
						""");
			}
			for (String path : List.of("gui/sprites/hud/heart/container.png", "gui/sprites/hud/heart/half.png",
					"gui/sprites/hud/heart/full.png", "gui/sprites/hud/food_empty.png", "gui/sprites/hud/food_half.png",
					"gui/sprites/hud/food_full.png", "gui/sprites/hud/air_bursting.png", "gui/sprites/hud/air.png",
					"gui/container/generic_54.png", "entity/chest/ender.png", "entity/conduit/base.png",
					modern ? "entity/shield/shield_base_nopattern.png" : "entity/shield_base_nopattern.png",
					modern ? "entity/armorstand/armorstand.png" : "entity/armorstand/wood.png", "entity/banner_base.png")) {
				out.putNextEntry(new ZipEntry("assets/minecraft/textures/" + path));
				ImageIO.write(new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB), "png", out);
				out.closeEntry();
			}
		}

		AtomicInteger drawnVertices = new AtomicInteger();
		AtomicInteger screenshots = new AtomicInteger();
		Rasteriser rasteriser = (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "getMaxTextureSize" -> 16384;
					case "createTexture" -> {
						BufferedImage image = (BufferedImage) args[0];
						yield new Texture() {
							@Override public int getWidth() { return image.getWidth(); }
							@Override public int getHeight() { return image.getHeight(); }
						};
					}
					case "createMesh" -> mesh(drawnVertices);
					case "takeScreenshot" -> {
						assertTrue(drawnVertices.getAndSet(0) >= 24, "Icon must draw entity geometry");
						screenshots.incrementAndGet();
						yield new BufferedImage((int) args[2], (int) args[3], BufferedImage.TYPE_INT_ARGB);
					}
					default -> null;
				});
		Configuration config = new Configuration();
		config.setMinecraftJar(jar.toFile());
		TexturePack textures = new TexturePack(rasteriser, config, List.of(), List.of());
		int previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(16);
		try {
			BlockTypeRegistry blocks = new BlockTypeRegistry();
			// Reuse simple entity geometry to isolate definition routing from individual block implementations.
			for (String item : DEFINITION_ONLY_ITEMS)
				blocks.register("minecraft:" + item, new Conduit(item, "minecraft:" + item,
						textures.findTexture("entity/conduit/base.png")));
			textures.getBannerPatternImages().put("bannerBase", new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB));
			textures.getBannerPatternImages().put("base", new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB));
			blocks.register("minecraft:red_banner", new Banner("minecraft:red_banner", "Red Banner", textures, true));
			blocks.register("minecraft:conduit", new Conduit("Conduit", "minecraft:conduit",
					textures.findTexture("entity/conduit/base.png")));
			var enderTexture = textures.findTexture("entity/chest/ender.png");
			blocks.register("minecraft:ender_chest", new ChestNew("Ender Chest", "minecraft:ender_chest",
					enderTexture, enderTexture, enderTexture, null, null, null));
			BlockRegistry models = new BlockRegistry(textures);
			var air = new tectonicus.blockregistry.BlockStateWrapper("minecraft:air");
			air.setFullOpaqueBlock(false);
			models.getBlockStates().put("minecraft:air", air);
			OutputResourcesUtil.outputInventoryItemIcons(directory.toFile(), rasteriser, textures, blocks,
					models, new ItemRegistry(textures), new ItemModelDefinitionRegistry(textures));

			assertEquals(2 + (modern ? DEFINITION_ONLY_ITEMS.size() + 1 : 0), screenshots.get());
			if (modern) {
				assertNotNull(textures.getPackTexture("banner_base_red"), "Entityless banners must retain their item color");
				assertArrayEquals(Files.readAllBytes(directory.resolve("Items/canonical.png")), Files.readAllBytes(directory.resolve("Items/alias.png")));
				assertTrue(Files.exists(directory.resolve("Items/shield.png")));
			}
			if (modern)
				for (String item : DEFINITION_ONLY_ITEMS)
					assertTrue(Files.exists(directory.resolve("Items/" + item + ".png")), item);
			for (String item : List.of("conduit", "ender_chest")) {
				BufferedImage icon = ImageIO.read(directory.resolve("Items/" + item + ".png").toFile());
				assertEquals(48, icon.getWidth());
				assertEquals(48, icon.getHeight());
			}

			RawChunk chunk = new RawChunk();
			ArmorStandEntity stand = new ArmorStandEntity(0, 0, 0, 0, 0, 0, 0, true, true, null, null, null, null);
			stand.setMainHandItem(new Item("minecraft:shield", List.of()));
			stand.setOffHandItem(new Item("minecraft:stick", List.of()));
			chunk.getArmorStands().add(stand);
			BlockContext world = (BlockContext) Proxy.newProxyInstance(BlockContext.class.getClassLoader(), new Class<?>[]{BlockContext.class},
					(proxy, method, args) -> switch (method.getName()) {
						case "getLight" -> 0.6f;
						case "getModelRegistry" -> models;
						default -> throw new AssertionError(method.getName());
					});
			ArmorStand armorStand = new ArmorStand("Armor Stand", textures);
			var shieldModel = ShieldModel.getModel(models, textures);
			for (boolean leftHanded : new boolean[]{false, true}) {
				stand.setLeftHanded(leftHanded);
				emittedPositions.clear();
				emittedColours.clear();
				armorStand.addEdgeGeometry(0, 0, 0, world, blocks, chunk, new Geometry(rasteriser));
				assertEquals(48, emittedPositions.size(), "A shield in either hand must draw both model cuboids");
				double centerX = emittedPositions.stream().mapToDouble(position -> position.x).average().orElseThrow();
				assertTrue(leftHanded ? centerX > 0.5 : centerX < 0.5);
				int frontVertex = 0;
				for (var quad : models.getBakedModel(shieldModel, 0, 0, 0, false).getQuads()) {
					if (quad.getSourceFace().equals("north"))
						break;
					frontVertex += 4;
				}
				Vector3f front = emittedPositions.get(frontVertex);
				Vector3f normal = new Vector3f(emittedPositions.get(frontVertex + 1)).sub(front)
						.cross(new Vector3f(emittedPositions.get(frontVertex + 2)).sub(front)).negate(); // Tectonicus uses clockwise fronts.
				assertTrue(leftHanded ? normal.x > 0 : normal.x < 0, "Coordinate conversion must preserve outward face winding");
				assertTrue(emittedColours.stream().allMatch(colour -> colour.equals(new Vector4f(0.6f, 0.6f, 0.6f, 1))));
			}
			assertSame(shieldModel, ShieldModel.getModel(models, textures), "Icons and stands must share the cached model");

			chunk.getArmorStands().clear();
			ArmorStandEntity visibleStand = new ArmorStandEntity(0, 0, 0, 0, 0, 0, 0, false, true, null, null, null, null);
			visibleStand.setRightArmPose(new Vector3f());
			visibleStand.setLeftArmPose(new Vector3f());
			chunk.getArmorStands().add(visibleStand);
			emittedPositions.clear();
			armorStand.addEdgeGeometry(0, 0, 0, world, blocks, chunk, new Geometry(rasteriser));
			List<Vector3f> body = List.copyOf(emittedPositions);
			visibleStand.setShowArms(true);
			emittedPositions.clear();
			armorStand.addEdgeGeometry(0, 0, 0, world, blocks, chunk, new Geometry(rasteriser));
			assertEquals(body.size() + 48, emittedPositions.size(), "ShowArms must add two complete cuboids");
			assertEquals(body, emittedPositions.subList(0, body.size()));
			List<Vector3f> rightArm = emittedPositions.subList(body.size(), body.size() + 24);
			List<Vector3f> leftArm = emittedPositions.subList(body.size() + 24, body.size() + 48);
			assertEquals(1 / 16.0, rightArm.stream().mapToDouble(position -> position.x).min().orElseThrow(), 0.00001);
			assertEquals(3 / 16.0, rightArm.stream().mapToDouble(position -> position.x).max().orElseThrow(), 0.00001);
			assertEquals(13 / 16.0, leftArm.stream().mapToDouble(position -> position.x).min().orElseThrow(), 0.00001);
			assertEquals(15 / 16.0, leftArm.stream().mapToDouble(position -> position.x).max().orElseThrow(), 0.00001);
			assertEquals(12 / 16.0, rightArm.stream().mapToDouble(position -> position.y).min().orElseThrow(), 0.00001);
			assertEquals(24 / 16.0, rightArm.stream().mapToDouble(position -> position.y).max().orElseThrow(), 0.00001);

			List<Vector3f> unposed = List.copyOf(rightArm);
			visibleStand.setRightArmPose(new Vector3f(-90, 20, 30));
			emittedPositions.clear();
			armorStand.addEdgeGeometry(0, 0, 0, world, blocks, chunk, new Geometry(rasteriser));
			assertNotEquals(unposed, emittedPositions.subList(body.size(), body.size() + 24));
			ArmorStandEntity invisibleStand = new ArmorStandEntity(0, 0, 0, 0, 0, 0, 0, true, true, null, null, null, null);
			invisibleStand.setShowArms(true);
			chunk.getArmorStands().clear();
			chunk.getArmorStands().add(invisibleStand);
			emittedPositions.clear();
			armorStand.addEdgeGeometry(0, 0, 0, world, blocks, chunk, new Geometry(rasteriser));
			assertTrue(emittedPositions.isEmpty(), "Invisible stands must hide their wooden arms");
		} finally {
			Minecraft.setChunkHeight(previousHeight);
			var base = ZipStack.class.getDeclaredField("base");
			base.setAccessible(true);
			((FileSystem) base.get(textures.getZipStack())).close();
		}
	}

	private Mesh mesh(AtomicInteger drawnVertices) {
		AtomicInteger vertices = new AtomicInteger();
		return (Mesh) Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class},
				(proxy, method, args) -> {
					switch (method.getName()) {
						case "addVertex" -> {
							vertices.incrementAndGet();
							emittedPositions.add(new Vector3f((Vector3f) args[0]));
							if (args[1] instanceof Vector4f colour)
								emittedColours.add(new Vector4f(colour));
						}
						case "draw" -> drawnVertices.addAndGet(vertices.get());
						case "getFaceCount", "getMemorySize", "getTotalVertices" -> { return 0; }
					}
					return null;
				});
	}

	private void json(ZipOutputStream out, String path, String json) throws Exception {
		out.putNextEntry(new ZipEntry(path));
		out.write(json.getBytes(StandardCharsets.UTF_8));
		out.closeEntry();
	}
}
