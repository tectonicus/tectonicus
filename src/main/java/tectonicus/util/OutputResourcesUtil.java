/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import tectonicus.Block;
import tectonicus.BlockIds;
import tectonicus.BlockTypeRegistry;
import tectonicus.BuildInfo;
import tectonicus.ItemRenderer;
import tectonicus.MemoryMonitor;
import tectonicus.PlayerIconAssembler;
import tectonicus.Portal;
import tectonicus.TileRenderer;
import tectonicus.Version;
import tectonicus.blockregistry.BlockRegistry;
import tectonicus.cache.swap.HddObjectListReader;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.Dimension;
import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.Layer;
import tectonicus.configuration.PlayerFilter;
import tectonicus.configuration.SignFilterType;
import tectonicus.itemregistry.ItemModel;
import tectonicus.itemregistry.ItemRegistry;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.raw.ArmorTrimTag;
import tectonicus.raw.BiomesOld;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.ContainerEntity;
import tectonicus.raw.DisplayTag;
import tectonicus.raw.EnchantmentTag;
import tectonicus.raw.EnchantmentsTag;
import tectonicus.raw.Player;
import tectonicus.raw.Item;
import tectonicus.raw.StoredEnchantmentsTag;
import tectonicus.texture.TexturePack;
import tectonicus.world.Sign;
import tectonicus.world.World;
import tectonicus.world.subset.WorldSubset;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static tectonicus.Version.VERSION_13;
import static tectonicus.Version.VERSION_16;

@Log4j2
@UtilityClass
public class OutputResourcesUtil {
	public static void outputSigns(File outputFile, File signListFile, tectonicus.configuration.Map map) {
		HddObjectListReader<Sign> signsIn = null;
		try {
			signsIn = new HddObjectListReader<>(signListFile);
			outputSigns(outputFile, signsIn, map);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (signsIn != null)
				signsIn.close();
		}
	}

	private static void outputSigns(File signFile, HddObjectListReader<Sign> signs, tectonicus.configuration.Map map) throws IOException {
		log.info("Exporting signs to {}", signFile.getAbsolutePath());

		Files.deleteIfExists(signFile.toPath());

		try (JsArrayWriter jsWriter = new JsArrayWriter(signFile, map.getId() + "_signData")) {

			WorldSubset worldSubset = map.getWorldSubset();
			Sign sign = new Sign();
			while (signs.hasNext()) {
				signs.read(sign);
				String message = "\"" + sign.getText(0) + "/n" + sign.getText(1) + "/n" + sign.getText(2) + "/n" + sign.getText(3) + "\"";
				if (map.getSignFilter().getType() == SignFilterType.OBEY)
					message = "\"/nOBEY/n/n\"";

				Map<String, String> signArgs = new HashMap<>();

				final float worldX = sign.getX() + 0.5f;
				final float worldY = sign.getY();
				final float worldZ = sign.getZ() + 0.5f;

				String posStr = "new WorldCoord(" + worldX + ", " + worldY + ", " + worldZ + ")";
				signArgs.put("worldPos", posStr);
				signArgs.put("message", message);
				if (map.getSignFilter().getType() == SignFilterType.OBEY) {
					signArgs.put("text1", "\"\"");
					signArgs.put("text2", "\"OBEY\"");
					signArgs.put("text3", "\"\"");
					signArgs.put("text4", "\"\"");
				} else {
					signArgs.put("text1", "\"" + sign.getText(0) + "\"");
					signArgs.put("text2", "\"" + sign.getText(1) + "\"");
					signArgs.put("text3", "\"" + sign.getText(2) + "\"");
					signArgs.put("text4", "\"" + sign.getText(3) + "\"");
				}

				if (worldSubset.containsBlock(sign.getX(), sign.getZ())) {
					jsWriter.write(signArgs);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void outputPlayers(File playersFile, File imagesDir, tectonicus.configuration.Map map, List<Player> players, PlayerIconAssembler playerIconAssembler) {
		try {
			Files.deleteIfExists(playersFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		FileUtils.ensureExists(imagesDir);

		log.info("Exporting players to {}", playersFile.getAbsolutePath());

		int numOutput = 0;
		ExecutorService executor = Executors.newCachedThreadPool();
		try (JsArrayWriter jsWriter = new JsArrayWriter(playersFile, map.getId() + "_playerData")) {

			PlayerFilter playerFilter = map.getPlayerFilter();
			WorldSubset worldSubset = map.getWorldSubset();
			for (Player player : players) {
				if (playerFilter.passesFilter(player)) {
					Vector3d position = player.getPosition();
					if (worldSubset.containsBlock(position.x, position.z)) {
						log.debug("\texporting {}", player.getName());

						Map<String, String> args = new HashMap<>();

						Vector3d pos = player.getPosition();
						args.put("name", "\"" + player.getName() + "\"");

						String posStr = "new WorldCoord(" + pos.x + ", " + pos.y + ", " + pos.z + ")";
						args.put("worldPos", posStr);

						args.put("health", "" + player.getHealth());
						args.put("food", "" + player.getFood());
						args.put("air", "" + player.getAir());

						args.put("xpLevel", "" + player.getXpLevel());
						args.put("xpTotal", "" + player.getXpTotal());

						jsWriter.write(args);

						File iconFile = new File(imagesDir, player.getName() + ".png");
						PlayerIconAssembler.WriteIconTask task = playerIconAssembler.new WriteIconTask(player, iconFile);
						executor.submit(task);

						numOutput++;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			executor.shutdown();
		}
		log.debug("Exported {} players", numOutput);
	}

	public static void outputBeds(File exportDir, tectonicus.configuration.Map map, List<Player> players) {
		File bedsFile = new File(exportDir, "beds.js");
		try {
			Files.deleteIfExists(bedsFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Exporting beds to {}", bedsFile.getAbsolutePath());

		int numOutput = 0;

		try (JsArrayWriter jsWriter = new JsArrayWriter(bedsFile, map.getId() + "_bedData")) {

			if (map.getDimension() == Dimension.OVERWORLD) // Beds only exist in the overworld dimension
			{
				WorldSubset worldSubset = map.getWorldSubset();
				PlayerFilter filter = map.getPlayerFilter();
				for (Player player : players) {
					if (filter.isShowBeds() && filter.passesFilter(player) && player.getSpawnDimension() == Dimension.OVERWORLD && player.getSpawnPosition() != null) {
						Map<String, String> bedArgs = new HashMap<>();

						Vector3l spawn = player.getSpawnPosition();

						if (worldSubset.containsBlock(spawn.x, spawn.z)) {
							log.debug("\texporting {}'s bed", player.getName());

							bedArgs.put("playerName", "\"" + player.getName() + "\"");

							String posStr = "new WorldCoord(" + spawn.x + ", " + spawn.y + ", " + spawn.z + ")";
							bedArgs.put("worldPos", posStr);


							jsWriter.write(bedArgs);
							numOutput++;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.debug("Exported {} beds", numOutput);
	}

	public static void outputRespawnAnchors(File exportDir, tectonicus.configuration.Map map, List<Player> players) {
		File anchorsFile = new File(exportDir, "respawnAnchors.js");
		try {
			Files.deleteIfExists(anchorsFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Exporting respawn anchors to {}", anchorsFile.getAbsolutePath());

		int numOutput = 0;

		try (JsArrayWriter jsWriter = new JsArrayWriter(anchorsFile, map.getId() + "_respawnAnchorData")) {

			if (map.getDimension() == Dimension.NETHER) // Respawn anchors only work in the nether dimension
			{
				WorldSubset worldSubset = map.getWorldSubset();
				PlayerFilter filter = map.getPlayerFilter();
				for (Player player : players) {
					if (filter.isShowRespawnAnchors() && filter.passesFilter(player) && player.getSpawnDimension() == Dimension.NETHER && player.getSpawnPosition() != null) {
						Map<String, String> anchorArgs = new HashMap<>();

						Vector3l spawn = player.getSpawnPosition();

						if (worldSubset.containsBlock(spawn.x, spawn.z)) {
							log.debug("\texporting {}'s respawn anchor", player.getName());

							anchorArgs.put("playerName", "\"" + player.getName() + "\"");

							String posStr = "new WorldCoord(" + spawn.x + ", " + spawn.y + ", " + spawn.z + ")";
							anchorArgs.put("worldPos", posStr);


							jsWriter.write(anchorArgs);
							numOutput++;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.debug("Exported {} respawn anchors", numOutput);
	}

	public static List<Portal> outputPortals(File outFile, File portalListFile, tectonicus.configuration.Map map) {
		List<Portal> portals = new ArrayList<>();

		try {
			HddObjectListReader<Portal> portalsIn = new HddObjectListReader<>(portalListFile);
			portals = outputPortals(outFile, portalsIn, map);
			portalsIn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return portals;
	}

	private static List<Portal> outputPortals(File portalFile, HddObjectListReader<Portal> portalPositions, tectonicus.configuration.Map map) throws IOException {
		log.info("Exporting portals...");

		Files.deleteIfExists(portalFile.toPath());

		List<Portal> portals = new ArrayList<>();
		try (JsArrayWriter jsWriter = new JsArrayWriter(portalFile, map.getId() + "_portalData")) {
			if (portalPositions.hasNext()) {
				long prevX;
				long prevY;
				long prevZ;
				long firstX;
				long firstZ;

				Portal portal = new Portal();
				portalPositions.read(portal);
				firstX = portal.getX();
				firstZ = portal.getZ();
				prevX = portal.getX();
				prevY = portal.getY();
				prevZ = portal.getZ();

				while (portalPositions.hasNext()) {
					portalPositions.read(portal);

					//Find the horizontal center portal block location
					if ((portal.getX() == prevX && portal.getZ() == prevZ + 1) || (portal.getX() == prevX + 1 && portal.getZ() == prevZ)) {
						prevX = portal.getX();
						prevY = portal.getY();
						prevZ = portal.getZ();
					} else {
						portals.add(new Portal(prevX + (firstX - prevX) / 2, prevY, prevZ + (firstZ - prevZ) / 2));
						prevX = portal.getX();
						prevY = portal.getY();
						prevZ = portal.getZ();
						firstX = portal.getX();
						firstZ = portal.getZ();
					}
				}
				portals.add(new Portal(portal.getX() + ((firstX - prevX) / 2), portal.getY(), portal.getZ() + (firstZ - prevZ) / 2));

				WorldSubset worldSubset = map.getWorldSubset();
				for (Portal p : portals) {
					final float worldX = p.getX();
					final float worldY = p.getY();
					final float worldZ = p.getZ();

					Map<String, String> portalArgs = new HashMap<>();
					String posStr = "new WorldCoord(" + worldX + ", " + worldY + ", " + worldZ + ")";
					portalArgs.put("worldPos", posStr);

					if (worldSubset.containsBlock(p.getX(), p.getZ())) {
						jsWriter.write(portalArgs);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.debug("Exported {} portals", portals.size());
		return portals;
	}

	public static void outputViews(File outputFile, File viewsListFile, tectonicus.configuration.Map map) {
		HddObjectListReader<Sign> viewsIn = null;
		try {
			viewsIn = new HddObjectListReader<>(viewsListFile);
			outputViews(outputFile, viewsIn, map);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (viewsIn != null)
				viewsIn.close();
		}
	}

	private static void outputViews(File viewsFile, HddObjectListReader<Sign> views, tectonicus.configuration.Map map) throws IOException {
		log.info("Exporting views...");

		Files.deleteIfExists(viewsFile.toPath());

		try (JsArrayWriter jsWriter = new JsArrayWriter(viewsFile, map.getId() + "_viewData")) {
			Sign sign = new Sign();
			while (views.hasNext()) {
				views.read(sign);

				Map<String, String> viewArgs = new HashMap<>();

				final float worldX = sign.getX() + 0.5f;
				final float worldY = sign.getY();
				final float worldZ = sign.getZ() + 0.5f;

				String posStr = "new WorldCoord(" + worldX + ", " + worldY + ", " + worldZ + ")";
				viewArgs.put("worldPos", posStr);

				StringBuilder text = new StringBuilder();
				for (int i = 0; i < 4; i++) {
					if (!sign.getText(i).startsWith("#")) {
						text.append(sign.getText(i)).append(" ");
					}
				}

				viewArgs.put("text", "\"" + text.toString().trim() + "\"");

				ImageFormat imageFormat = map.getViewConfig().getImageFormat();
				String filename = map.getId() + "/Views/View_" + sign.getX() + "_" + sign.getY() + "_" + sign.getZ() + "." + imageFormat.getExtension();
				viewArgs.put("imageFile", "\"" + filename + "\"");

				if (map.getWorldSubset().containsBlock(sign.getX(), sign.getZ())) {
					jsWriter.write(viewArgs);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void outputChests(File chestFile, tectonicus.configuration.Map map, ConcurrentLinkedQueue<ContainerEntity> chestList) {
		log.info("Exporting chests to {}", chestFile.getAbsolutePath());

		try {
			Files.deleteIfExists(chestFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (JsArrayWriter jsWriter = new JsArrayWriter(chestFile, map.getId() + "_chestData")) {
			WorldSubset worldSubset = map.getWorldSubset();
			for (ContainerEntity entity : chestList) {
                                if ("left".equals(entity.getType())) {
                                        // Skip left part of large chests (we will merge them with right part and display them as one sigle large chest)
                                        continue;
                                }
                            
				float worldX = entity.getX() + 0.5f;
				float worldY = entity.getY();
				float worldZ = entity.getZ() + 0.5f;
				Map<String, String> chestArgs = new HashMap<>();

                                String items = "[\r\n";
                                for (var item : entity.getItems()) {
                                        items += outputItem(item, false);
                                }
                                if ("right".equals(entity.getType())) {
                                        // Find right part and add its items
                                        int leftX = entity.getX();
                                        int leftY = entity.getY();
                                        int leftZ = entity.getZ();

                                        switch (entity.getFacing()) {
                                                case "east":
                                                        leftZ -= 1;
                                                        break;
                                                case "north":
                                                        leftX -= 1;
                                                        break;
                                                case "south":
                                                        leftX += 1;
                                                        break;
                                                case "west":
                                                        leftZ += 1;
                                                        break;
                                        }
                                        
                                        for (ContainerEntity left : chestList) {
                                                if (left.getX()!=leftX || left.getY()!=leftY || left.getZ()!=leftZ || !"left".equals(left.getType())) {
                                                     continue;
                                                }
                                                for (var item : left.getItems()) {
                                                    items += outputItem(item, true);
                                                }
                                                break;
                                        }
                                }
                                items += "\t\t]";
                                
				chestArgs.put("worldPos", "new WorldCoord(" + worldX + ", " + worldY + ", " + worldZ + ")");
                                chestArgs.put("name", "\"" + entity.getCustomName() + "\"");
                                chestArgs.put("items", items);
                                if ("right".equals(entity.getType())) {
                                        chestArgs.put("large", "true");
                                }

				if (worldSubset.containsBlock(entity.getX(), entity.getZ())) {
					jsWriter.write(chestArgs);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
        
        private static String outputItem(Item item, Boolean isLeft) {
                String result = "\t\t\t{ id: \"" + item.id + "\", ";

                DisplayTag displayTag = item.getTag(DisplayTag.class);
                if (displayTag != null) {
                        if (displayTag.name != null) {
                                result += "customName: \"" + displayTag.name + "\", ";
                        }
                        if (displayTag.color != null) {
                                result += "color: " + displayTag.color + ", ";
                        }
                }

                int slot = item.slot;
                slot += isLeft ? 3 * 9 : 0;
                result += "count: " + item.count + ", slot: " + slot + ", ";
                
                ArmorTrimTag trimTag = item.getTag(ArmorTrimTag.class);
                if (trimTag != null) {
                        result += "trim: { pattern: \"" + trimTag.pattern + "\", material: \"" + trimTag.material + "\" }, ";
                }
                
                List<EnchantmentTag> enchantments = null;
                
                EnchantmentsTag enchantmentsTag = item.getTag(EnchantmentsTag.class);
                if (enchantmentsTag != null) {
                        enchantments = enchantmentsTag.enchantments;
                }
                StoredEnchantmentsTag storedEnchantmentsTag = item.getTag(StoredEnchantmentsTag.class);
                if (storedEnchantmentsTag != null) {
                        enchantments = storedEnchantmentsTag.enchantments;
                }
                
                if (enchantments != null) {
                        result += "enchantments: [";
                        for (var enchantment : enchantments) {
                                result += "{ id: \"" + enchantment.id + "\", level: " + enchantment.level.toString() + " }, ";
                        }
                        result += "], ";
                }

                result += "},\r\n";
                
                return result;
        }

	public static void outputInventoryItemIcons(Configuration args, Rasteriser rasteriser, TexturePack texturePack, BlockTypeRegistry blockTypeRegistry, BlockRegistry blockRegistry, ItemRegistry itemRegistry) {
                System.out.println("Rendering icons for inventory items");
                log.trace("Rendering icons for inventory items");
                
		try {
			ItemRenderer itemRenderer = new ItemRenderer(rasteriser);
			for (Map.Entry<String, ItemModel> entry : itemRegistry.getModels().entrySet()) {
                                final String entryKey = entry.getKey();
                                final ItemModel itemModel = entry.getValue();
                                final ItemModel ultimatePredecessorModel = itemRegistry.findUltimatePredecessor(itemModel);
                                final File outFile = new File(args.getOutputDir(), "Images/Items/" + entryKey + ".png");

                                System.out.print("\tRendering icon for: " + entryKey + "                    \r"); //prints a carriage return after line
                                log.trace("\tRendering icon for: " + entryKey);

                                String modelName = ultimatePredecessorModel.getParent();
                                if (modelName == null) {
                                        // Do not crash for blocks without parent (Air)
                                        continue;
                                }
                                
                                // Some items need special handling. Namely beds and builtin entities
                                if (modelName.endsWith("builtin/entity")){
                                        modelName = "minecraft:" + entryKey;
                                        
                                        if (entryKey.endsWith("_bed")) {
                                                itemRenderer.renderBed(outFile, blockTypeRegistry, texturePack, modelName);
                                                continue;
                                        }
                                        
                                        List<Map<String, ArrayList<Float>>> transforms = itemRegistry.getTransformsList(itemModel);                                        
                                        itemRenderer.renderItem(outFile, blockTypeRegistry, texturePack, modelName, transforms);
                                        continue;
                                }
                                
                                // Items that are just 2d textures
                                if (modelName.endsWith("builtin/generated")) {
                                        final Map<String, String> textures = itemModel.getTextures();
                                        if (textures != null) {
                                                BufferedImage composited = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                                                for (var layer : textures.entrySet()) {
                                                        if (!layer.getKey().startsWith("layer")) {
                                                                // Ignore particles
                                                                continue;
                                                        }
                                                        final String[] layerTexture = layer.getValue().split(":");
                                                        final String namespace = layerTexture.length == 1 ? "minecraft" : layerTexture[0];
                                                        final String textureId = layerTexture.length == 1 ? layerTexture[0] : layerTexture[1];
                                                        
                                                        BufferedImage texture;
                                                        
                                                        if (!layer.getKey().equals("layer0") && textureId.contains("trim")) {
                                                                String[] trimParts = textureId.split("_trim_");
                                                                
                                                                String trim = "assets/" + namespace + "/textures/" + trimParts[0] + "_trim.png";
                                                                String palette = "assets/" + namespace + "/textures/trims/color_palettes/" + trimParts[1] + ".png";
                                                                String keyPalette = "assets/" + namespace + "/textures/trims/color_palettes/trim_palette.png";
                                                                
                                                                texture = texturePack.loadPalettedTexture(trim, palette, keyPalette);
                                                        } else {
                                                                texture = texturePack.loadTexture("assets/" + namespace + "/textures/" + textureId + ".png");
                                                        }
                                                        
                                                        var block = blockRegistry.getBlockModels().getIfPresent(layer.getValue());
                                                        if (block != null) {
                                                                if (block.getElements().get(0).getFaces().values().iterator().next().isTinted()) {
                                                                        Colour4f tintColor = texturePack.getFoliageColor(BiomesOld.FOREST);
                                                                        for (int y=0; y<texture.getHeight(); y++) {
                                                                                for (int x=0; x<texture.getWidth(); x++) {
                                                                                        Colour4f pixel = new Colour4f(texture.getRGB(x, y));
                                                                                        pixel.multiply(tintColor);
                                                                                        texture.setRGB(x, y, pixel.toArgb());
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                                                                                
                                                        composited.getGraphics().drawImage(texture, 0, 0, null);
                                                }
                                                writeImage(composited, 16, 16, outFile);
                                                continue;
                                        }
                                }
                                
                                // Inventory block models are not loaded in the registry because they do not have a block state. Let's load them manually
                                if (modelName.contains("_inventory")) {
                                        var model = blockRegistry.loadModel(modelName, "", new HashMap<>(), null);
                                        itemRenderer.renderInventoryBlockModel(outFile, blockRegistry, texturePack, model);
                                        continue;
                                }
                                
                                // Rest of the items
                                itemRenderer.renderInventoryBlockModel(outFile, blockRegistry, texturePack, modelName);
			}
                        System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void outputIcons(File exportDir, Configuration args, tectonicus.configuration.Map map, World world, Rasteriser rasteriser)
	{
		BlockTypeRegistry registryOld = world.getBlockTypeRegistry();
		BlockRegistry registry = world.getModelRegistry();
		TexturePack texturePack = world.getTexturePack();
		Version version = world.getWorldInfo().getVersion();

		try {
			ItemRenderer itemRenderer = new ItemRenderer(rasteriser);
			if (texturePack.getVersion().getNumVersion() < VERSION_13.getNumVersion()) {
				itemRenderer.renderBlockOld(new File(exportDir, "Images/Chest.png"), registryOld, texturePack, BlockIds.CHEST, 5);
			} else {
				Map<String, String> properties = new HashMap<>();
				properties.put("facing", "south");
				itemRenderer.renderBlock(new File(exportDir, "Images/Chest.png"), registryOld, texturePack, Block.CHEST, new BlockProperties(properties));
			}
                        itemRenderer.renderBed(new File(exportDir, "Images/Bed.png"), registryOld, texturePack);
			itemRenderer.renderCompass(map, new File(exportDir, map.getId()+"/Compass.png"));
			itemRenderer.renderPortal(new File(args.getOutputDir(), "Images/Portal.png"), registryOld, texturePack);
			if (version.getNumVersion() >= VERSION_16.getNumVersion()) {
				itemRenderer.renderBlockModel(new File(args.getOutputDir(), "Images/RespawnAnchor.png"), registry, texturePack, Block.RESPAWN_ANCHOR, "_4");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void outputHtmlResources(TexturePack texturePack, PlayerIconAssembler playerIconAssembler, Configuration config, File exportDir, int numZoomLevels, int tileWidth, int tileHeight) {
		log.info("Writing javascript and image resources...");
		String defaultSkin = config.getDefaultSkin();

		File imagesDir = new File(exportDir, "Images");
                File itemsDir = new File(imagesDir, "Items");
		itemsDir.mkdirs();

		FileUtils.extractResource("Images/Spawn.png", new File(imagesDir, "Spawn.png"));
		FileUtils.extractResource("Images/Logo.png", new File(imagesDir, "Logo.png"));

		FileUtils.extractResource("Images/Spacer.png", new File(imagesDir, "Spacer.png"));

		String defaultSkinPath = defaultSkin;
		Version texturePackVersion = texturePack.getVersion();
		switch (texturePackVersion) {
			case VERSION_4:
				writeImage(texturePack.getItem(10, 2), 32, 32, new File(imagesDir, "Sign.png"));
				writeImage(texturePack.getItem(10, 1), 32, 32, new File(imagesDir, "Picture.png"));
				writeImage(texturePack.getItem(7, 1), 32, 32, new File(imagesDir, "IronIcon.png"));
				writeImage(texturePack.getItem(7, 2), 32, 32, new File(imagesDir, "GoldIcon.png"));
				writeImage(texturePack.getItem(7, 3), 32, 32, new File(imagesDir, "DiamondIcon.png"));
				writeImage(texturePack.getItem(13, 2), 32, 32, new File(imagesDir, "Bed.png"));
				if (defaultSkin.equals("steve"))
					defaultSkinPath = "mob/char.png";
				break;

			case VERSION_5:
				writeImage(texturePack.getItem("textures/items/sign.png"), 32, 32, new File(imagesDir, "Sign.png"));
				writeImage(texturePack.getItem("textures/items/painting.png"), 32, 32, new File(imagesDir, "Picture.png"));
				writeImage(texturePack.getItem("textures/items/ingotIron.png"), 32, 32, new File(imagesDir, "IronIcon.png"));
				writeImage(texturePack.getItem("textures/items/ingotGold.png"), 32, 32, new File(imagesDir, "GoldIcon.png"));
				writeImage(texturePack.getItem("textures/items/diamond.png"), 32, 32, new File(imagesDir, "DiamondIcon.png"));
				writeImage(texturePack.getItem("textures/items/bed.png"), 32, 32, new File(imagesDir, "Bed.png"));
				if (defaultSkin.equals("steve"))
					defaultSkinPath = "mob/char.png";
				break;

			default: //assume version is 1.6 or higher
				if (texturePack.fileExists("assets/minecraft/textures/items/bed.png")) { //Use the old bed image for 1.6 - 1.11 if found
					writeImage(texturePack.getItem("assets/minecraft/textures/items/bed.png"), 32, 32, new File(imagesDir, "Bed.png"));
				}

				String path = "assets/minecraft/textures/items/"; //path for 1.6 - 1.12
				if (texturePackVersion.getNumVersion() >= VERSION_13.getNumVersion()) {
					path = "assets/minecraft/textures/item/"; //path for 1.13+
				}

				if (texturePack.fileExists(path + "oak_sign.png")) { //1.14 and higher use the new sign image
					writeImage(texturePack.getItem(path + "oak_sign.png"), 32, 32, new File(imagesDir, "Sign.png"));
				} else {
					writeImage(texturePack.getItem(path + "sign.png"), 32, 32, new File(imagesDir, "Sign.png"));
				}
				writeImage(texturePack.getItem(path + "painting.png"), 32, 32, new File(imagesDir, "Picture.png"));
				writeImage(texturePack.getItem(path + "iron_ingot.png"), 32, 32, new File(imagesDir, "IronIcon.png"));
				writeImage(texturePack.getItem(path + "gold_ingot.png"), 32, 32, new File(imagesDir, "GoldIcon.png"));
				writeImage(texturePack.getItem(path + "diamond.png"), 32, 32, new File(imagesDir, "DiamondIcon.png"));

				if (defaultSkin.equals("steve") || defaultSkin.equals("alex") || defaultSkin.equals("ari") || defaultSkin.equals("efe") || defaultSkin.equals("kai") || defaultSkin.equals("makena")
						|| defaultSkin.equals("noor") || defaultSkin.equals("sunny") || defaultSkin.equals("zuri")) {

					defaultSkinPath = "assets/minecraft/textures/entity/player/wide/" + defaultSkin + ".png";
					if (!texturePack.fileExists(defaultSkinPath)) {
						defaultSkinPath = "assets/minecraft/textures/entity/steve.png";
						//Check for Alex skin which was added in 1.8
						if (defaultSkin.equals("alex") && texturePack.fileExists("assets/minecraft/textures/entity/alex.png")) {
							defaultSkinPath = "assets/minecraft/textures/entity/alex.png";
						}
					}
				}
		}

                writeImage(texturePack.getEmptyHeartImage(), 18, 18, new File(imagesDir, "EmptyHeart.png"));
                writeImage(texturePack.getHalfHeartImage(), 18, 18, new File(imagesDir, "HalfHeart.png"));
                writeImage(texturePack.getFullHeartImage(), 18, 18, new File(imagesDir, "FullHeart.png"));

                writeImage(texturePack.getEmptyFoodImage(), 18, 18, new File(imagesDir, "EmptyFood.png"));
                writeImage(texturePack.getHalfFoodImage(), 18, 18, new File(imagesDir, "HalfFood.png"));
                writeImage(texturePack.getFullFoodImage(), 18, 18, new File(imagesDir, "FullFood.png"));

		writeImage(texturePack.getEmptyAirImage(), 18, 18, new File(imagesDir, "EmptyAir.png"));
		writeImage(texturePack.getFullAirImage(), 18, 18, new File(imagesDir, "FullAir.png"));

		writeImage(texturePack.getChestImage(), 176, 78, new File(imagesDir, "SmallChest.png"));
		writeImage(texturePack.getLargeChestImage(), 176, 132, new File(imagesDir, "LargeChest.png"));
                
                // Write font texture
                writeImage(texturePack.getFont().getFontSheet(), 128, 128, new File(imagesDir, "Font.png"));

		// Write default player icon
		BufferedImage defaultSkinIcon = texturePack.getItem(defaultSkinPath);
		if (defaultSkinIcon == null) {
			log.warn("Unable to find default skin!");
		} else {
			playerIconAssembler.writeDefaultIcon(defaultSkinIcon, new File(imagesDir, "PlayerIcons/Tectonicus_Default_Player_Icon.png"));
		}
                
                // Extract enchanted glint texture
                extractFile(texturePack, "assets/minecraft/textures/misc/enchanted_glint_item.png", new File(imagesDir, "EnchantedGlint.png"), true);

		// Extract Leaflet resources
                File scriptsDir = new File(exportDir, "Scripts");
		extractMapResources(scriptsDir);
                
                // Extract localized texts
                extractFile(texturePack, "assets/minecraft/lang/en_us.json", new File(scriptsDir, "localizations.json"), false);

		List<String> scriptResources = new ArrayList<>();
		scriptResources.add("marker.js");
		scriptResources.add("controls.js");
		scriptResources.add("minecraftProjection.js");
		scriptResources.add("containers.js");
		scriptResources.add("main.js");
		outputMergedJs(new File(exportDir, "Scripts/tectonicus.js"), scriptResources, numZoomLevels, config, tileWidth, tileHeight);
	}

	public static void writeImage(BufferedImage img, final int width, final int height, File file) {
		try {
			BufferedImage toWrite;
			if (img.getWidth() != width || img.getHeight() != height) {
				toWrite = new BufferedImage(width, height, img.getType());
				toWrite.getGraphics().drawImage(img, 0, 0, width, height, null);
			} else {
				toWrite = img;
			}
			ImageIO.write(toWrite, "png", file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

        private void extractFile(TexturePack texturePack, String filepath, File outputFile, boolean minecraftJarLoaded) {
                if (texturePack.fileExists(filepath)) {
                        try {
                                try (var stream = texturePack.getZipStack().getStream(filepath, minecraftJarLoaded)) {
                                        Files.copy(stream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                }
                        } catch (IOException e) {
                                e.printStackTrace();
                        }
                }
        }

	private static void extractMapResources(File scriptsDir) {
		scriptsDir.mkdirs();
		File scriptImagesDir = new File(scriptsDir, "images");
		scriptImagesDir.mkdirs();

		FileUtils.extractResource("math.js", new File(scriptsDir, "math.js"));
		FileUtils.extractResource("leaflet.js", new File(scriptsDir, "leaflet.js"));
		FileUtils.extractResource("leaflet.css", new File(scriptsDir, "leaflet.css"));
		FileUtils.extractResource("tectonicusStyles.css", new File(scriptsDir, "tectonicusStyles.css"));
		FileUtils.extractResource("Images/layers.png", new File(scriptImagesDir, "layers.png"));
		FileUtils.extractResource("Images/layers-2x.png", new File(scriptImagesDir, "layers-2x.png"));
		FileUtils.extractResource("Images/marker-icon.png", new File(scriptImagesDir, "marker-icon.png"));
		FileUtils.extractResource("Images/marker-icon-2x.png", new File(scriptImagesDir, "marker-icon-2x.png"));
		FileUtils.extractResource("Images/marker-shadow.png", new File(scriptImagesDir, "marker-shadow.png"));
		FileUtils.extractResource("popper.min.js", new File(scriptsDir, "popper.min.js"));
		FileUtils.extractResource("tippy-bundle.umd.min.js", new File(scriptsDir, "tippy-bundle.umd.min.js"));
		FileUtils.extractResource("tippy-light-theme.css", new File(scriptsDir, "tippy-light-theme.css"));
	}
        
	private void outputMergedJs(File outFile, List<String> inputResources, int numZoomLevels, Configuration config, int tileWidth, int tileHeight)
	{
		InputStream in = null;
		final int scale = (int)Math.pow(2, numZoomLevels);
		try (PrintWriter writer = new PrintWriter(new FileOutputStream(outFile)))
		{
			for (String res : inputResources)
			{
				in = TileRenderer.class.getClassLoader().getResourceAsStream(res);

				assert in != null;
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));

				String line;
				while ((line = reader.readLine()) != null)
				{
					StringBuilder outLine = new StringBuilder();

					List<Util.Token> tokens = Util.split(line);

					while (!tokens.isEmpty())
					{
						Util.Token first = tokens.remove(0);
						if (first.isReplaceable)
						{
							if (first.value.equals("tileWidth"))
							{
								outLine.append(tileWidth);
							}
							else if (first.value.equals("tileHeight"))
							{
								outLine.append(tileHeight);
							}
							else if (first.value.equals("maxZoom"))
							{
								outLine.append(numZoomLevels);
							}
							else if (first.value.equals("mapCoordScaleFactor"))
							{
								outLine.append(scale);
								outLine.append(".0"); // Append .0 so that it's treated as float in the javascript
							}
							else if (first.value.equals("showSpawn"))
							{
								outLine.append(config.showSpawn());
							}
							else if (first.value.equals("signsInitiallyVisible"))
							{
								outLine.append(config.areSignsInitiallyVisible());
							}
							else if (first.value.equals("playersInitiallyVisible"))
							{
								outLine.append(config.arePlayersInitiallyVisible());
							}
							else if (first.value.equals("portalsInitiallyVisible"))
							{
								outLine.append(config.arePortalsInitiallyVisible());
							}
							else if (first.value.equals("bedsInitiallyVisible"))
							{
								outLine.append(config.areBedsInitiallyVisible());
							}
							else if (first.value.equals("respawnAnchorsInitiallyVisible"))
							{
								outLine.append(config.areRespawnAnchorsInitiallyVisible());
							}
							else if (first.value.equals("spawnInitiallyVisible"))
							{
								outLine.append(config.isSpawnInitiallyVisible());
							}
							else if (first.value.equals("viewsInitiallyVisible"))
							{
								outLine.append(config.areViewsInitiallyVisible());
							}
						}
						else
						{
							outLine.append(first.value);
						}
					}
					writer.write(outLine.append("\n").toString());
				}

				writer.flush();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (in != null) {
					in.close();
				}
			}
			catch (Exception e) {}
		}
	}

	public static void outputContents(File outputFile, Configuration config)
	{
		try {
			Files.deleteIfExists(outputFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Writing master contents to {}", outputFile.getAbsolutePath());

		try (PrintWriter writer = new PrintWriter(outputFile))
		{
			writer.println("tileSize = "+config.getTileSize()+";");
			writer.println("maxZoom = "+config.getNumZoomLevels()+";");
			writer.println();

			writer.println("var contents = ");
			writer.println("[");

			List<tectonicus.configuration.Map> maps = config.getMaps();
			for (int i=0; i<maps.size(); i++)
			{
				tectonicus.configuration.Map m = maps.get(i);

				writer.println("\t{");

				writer.println("\t\tid: \""+m.getId()+"\",");
				writer.println("\t\tname: \""+m.getName()+"\",");
				writer.println("\t\tplayers: "+m.getId()+"_playerData,");
				writer.println("\t\tbeds: "+m.getId()+"_bedData,");
				writer.println("\t\trespawnAnchors: "+m.getId()+"_respawnAnchorData,");
				writer.println("\t\tsigns: "+m.getId()+"_signData,");
				writer.println("\t\tportals: "+m.getId()+"_portalData,");
				writer.println("\t\tviews: "+m.getId()+"_viewData,");
				writer.println("\t\tchests: "+m.getId()+"_chestData,");
				writer.println("\t\tblockStats: "+m.getId()+"_blockStats,");
				writer.println("\t\tworldStats: "+m.getId()+"_worldStats,");
				writer.println("\t\tworldVectors: "+m.getId()+"_worldVectors,");

				writer.println("\t\tlayers:");
				writer.println("\t\t[");
				for (int j=0; j<m.numLayers(); j++)
				{
					Layer l = m.getLayer(j);

					writer.println("\t\t\t{");

					writer.println("\t\t\t\tid: \""+l.getId()+"\",");
					writer.println("\t\t\t\tname: \""+l.getName()+"\",");
					writer.println("\t\t\t\tdimension: \"" + m.getDimension() + "\",");
					writer.println("\t\t\t\tbackgroundColor: \""+l.getBackgroundColor()+"\",");
					writer.println("\t\t\t\timageFormat: \""+l.getImageFormat().getExtension()+"\",");
					writer.println("\t\t\t\tisPng: \""+l.getImageFormat().isPng()+"\"");

					if (j < m.numLayers()-1)
						writer.println("\t\t\t},");
					else
						writer.println("\t\t\t}");
				}
				writer.println("\t\t]");

				if (i < maps.size()-1)
					writer.println("\t},");
				else
					writer.println("\t}");
			}

			writer.println("]");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static File outputHtml(File exportDir, Configuration config) throws IOException {
		File outputHtmlFile = new File(exportDir, config.getOutputHtmlName());
		log.info("Writing html to {}", outputHtmlFile.getAbsolutePath());

		URL url = OutputResourcesUtil.class.getClassLoader().getResource("mapWithSigns.html");
		if (url == null)
			throw new IOException("resource not found");
		try (Scanner scanner = new Scanner(url.openStream());
			 PrintWriter writer = new PrintWriter(new FileOutputStream(outputHtmlFile)))
		{
			while (scanner.hasNext())
			{
				String line = scanner.nextLine();
				StringBuilder outLine = new StringBuilder();

				List<Util.Token> tokens = Util.split(line);

				while (!tokens.isEmpty())
				{
					Util.Token first = tokens.remove(0);
					if (first.isReplaceable)
					{
						if (first.value.equals("title")) {
							outLine.append(config.getHtmlTitle());
                                                } else if (first.value.equals("customStyleIncludes")) {
                                                        if (config.getCustomStyle() != null)
                                                        {
                                                                outLine.append("<link rel=\"stylesheet\" href=\"Scripts/");
                                                                outLine.append(config.getCustomStyle());
                                                                outLine.append("\" />");
                                                        }
                                                } else if (first.value.equals("customScriptIncludes")) {
                                                        if (config.getCustomScript() != null) {
                                                                outLine.append("<script src=\"Scripts/");
								outLine.append(config.getCustomScript());
								outLine.append("\"></script>");
                                                        }
                                                } else if (first.value.equals("scriptIncludes")) {
							String templateStart = "		<script src=\"";
							String templateEnd = "\"></script>\n";
                                                        
							for (tectonicus.configuration.Map map : config.getMaps())
							{
								outLine.append(templateStart);
								outLine.append(map.getId()).append("/players.js");
								outLine.append(templateEnd);

								outLine.append(templateStart);
								outLine.append(map.getId()).append("/beds.js");
								outLine.append(templateEnd);

								outLine.append(templateStart);
								outLine.append(map.getId()).append("/respawnAnchors.js");
								outLine.append(templateEnd);

								outLine.append(templateStart);
								outLine.append(map.getId()).append("/portals.js");
								outLine.append(templateEnd);

								outLine.append(templateStart);
								outLine.append(map.getId()).append("/signs.js");
								outLine.append(templateEnd);

								outLine.append(templateStart);
								outLine.append(map.getId()).append("/views.js");
								outLine.append(templateEnd);

								outLine.append(templateStart);
								outLine.append(map.getId()).append("/chests.js");
								outLine.append(templateEnd);

								outLine.append(templateStart);
								outLine.append(map.getId()).append("/worldVectors.js");
								outLine.append(templateEnd);

								outLine.append(templateStart);
								outLine.append(map.getId()).append("/blockStats.js");
								outLine.append(templateEnd);

								outLine.append(templateStart);
								outLine.append(map.getId()).append("/worldStats.js");
								outLine.append(templateEnd);

								// Any per layer includes?
							}
						}
					} else {
						outLine.append(first.value);
					}
				}

				writer.write(outLine.append("\n").toString());
			}

			writer.flush();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return outputHtmlFile;
	}

	public static void outputRenderStats(File exportDir, MemoryMonitor memoryMonitor, final String timeTaken)
	{
		File statsFile = new File(new File(exportDir, "Scripts"), "stats.js");
		try {
			Files.deleteIfExists(statsFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Exporting stats to {}", statsFile.getAbsolutePath());

		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy");
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm z");
		final String renderedDateStr = dateFormat.format( new Date() );
		final String renderedTimeStr = timeFormat.format( new Date() );

		try (JsObjectWriter jsWriter = new JsObjectWriter(statsFile)) {

			Map<String, Object> stats = new HashMap<>();

			stats.put("tectonicusVersion", BuildInfo.getVersion());

			stats.put("renderTime", timeTaken);
			stats.put("renderedOnDate", renderedDateStr);
			stats.put("renderedOnTime", renderedTimeStr);
			stats.put("peakMemoryBytes", memoryMonitor.getPeakMemory());

			jsWriter.write("stats", stats);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
