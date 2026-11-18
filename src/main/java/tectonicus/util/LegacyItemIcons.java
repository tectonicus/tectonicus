/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import lombok.extern.slf4j.Slf4j;
import tectonicus.BlockTypeRegistry;
import tectonicus.ItemRenderer;
import tectonicus.Version;
import tectonicus.blockTypes.Air;
import tectonicus.raw.BiomesOld;
import tectonicus.raw.LegacyItems;
import tectonicus.raw.SpawnEggColors;
import tectonicus.texture.TexturePack;
import tools.jackson.databind.JsonNode;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Renders inventory icons with a pre-1.13 Minecraft jar. Items are exported with their 1.13 IDs, so each icon is
 * written under that name.
 * <ul>
 *     <li>1.8 to 1.12 jars: flat items use the layers of their item model; other items are rendered as blocks.</li>
 *     <li>1.6 and 1.7 jars: flat items use the textures named by the 1.12 item model.</li>
 *     <li>1.5 jars: flat items use textures named after their translation key, such as {@code swordDiamond.png}.</li>
 *     <li>1.4 and older jars: flat items come from the {@code gui/items.png} sheet.</li>
 * </ul>
 * Items without a flat icon are rendered with the legacy block renderer from their numeric block ID and damage value.
 */
@Slf4j
public final class LegacyItemIcons {
	private static final Set<String> CHESTS = Set.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:ender_chest",
			"minecraft:furnace", "minecraft:dispenser", "minecraft:dropper");
	private static final Set<String> TINTED_TEXTURES = Set.of("tallgrass", "fern", "vine", "waterlily",
			"double_plant_grass_bottom", "double_plant_fern_bottom", "leaves_oak", "leaves_spruce", "leaves_birch",
			"leaves_jungle", "leaves_acacia", "leaves_big_oak");
	private static final int ITEM_SHEET_POTION_CONTENTS = 140;
	private static final int ITEM_SHEET_LEATHER_OVERLAY_OFFSET = 144;

	private final TexturePack texturePack;
	private final BlockTypeRegistry blockTypeRegistry;
	private final ItemRenderer itemRenderer;
	private final File itemIconDir;
	private final Version version;

	LegacyItemIcons(TexturePack texturePack, BlockTypeRegistry blockTypeRegistry, ItemRenderer itemRenderer, File itemIconDir) {
		this.texturePack = texturePack;
		this.blockTypeRegistry = blockTypeRegistry;
		this.itemRenderer = itemRenderer;
		this.itemIconDir = itemIconDir;
		this.version = texturePack.getVersion();
	}

	void render(Collection<String> itemIds) {
		LegacyItems legacyItems = LegacyItems.get();
		int rendered = 0;
		for (String id : itemIds) {
			LegacyItems.Entry entry = legacyItems.item(id);
			if (entry == null)
				continue;
			String name = id.substring(id.indexOf(':') + 1);
			try {
				List<BufferedImage> layers = SpawnEggColors.tint(id, flatLayers(entry, name));
				if (!layers.isEmpty()) {
					writeFlatIcon(name, layers);
					rendered++;
				} else if (renderBlock(entry, name)) {
					rendered++;
				} else {
					log.debug("No legacy icon for {}", id);
				}
			} catch (Exception e) {
				log.warn("Unable to render legacy item icon for {}", id, e);
			}
		}
		log.info("Rendered {} inventory item icons with the {} jar", rendered, version);
	}

	/**
	 * @param name the 1.13 item ID without its namespace
	 * @return the layers of a flat item in a pre-1.13 jar, bottom first, or an empty list for a block or a 3D item
	 */
	public static List<BufferedImage> flatItemLayers(TexturePack texturePack, LegacyItems.Entry entry, String name) throws IOException {
		return new LegacyItemIcons(texturePack, null, null, null).flatLayers(entry, name);
	}

	private List<BufferedImage> flatLayers(LegacyItems.Entry entry, String name) throws IOException {
		if (version.getNumVersion() <= Version.VERSION_4.getNumVersion())
			return itemSheetLayers(entry, name);
		if (version == Version.VERSION_5)
			return version5Layers(entry, name);

		List<String> textures = entry.textures();
		if (entry.model() != null) {
			String modelPath = "assets/minecraft/models/item/" + entry.model() + ".json";
			if (texturePack.fileExists(modelPath))
				textures = modelLayers(modelPath);
		}
		List<BufferedImage> layers = new ArrayList<>();
		for (String texture : textures) {
			BufferedImage image = loadTexture(texturePath(texture));
			if (image == null)
				return List.of();
			layers.add(tint(texture, image));
		}
		return layers;
	}

	/** @return the layer textures of a flat (generated) item model, or an empty list for a 3D item */
	private List<String> modelLayers(String modelPath) throws IOException {
		List<String> layers = null;
		String path = modelPath;
		for (int depth = 0; path != null && depth < 16; depth++) {
			JsonNode model;
			try (InputStream in = texturePack.getZipStack().getStream(path)) {
				if (in == null)
					return List.of();
				model = FileUtils.getOBJECT_MAPPER().readTree(in);
			}
			JsonNode textures = model.path("textures");
			if (layers == null && textures.has("layer0")) {
				layers = new ArrayList<>();
				for (int i = 0; textures.has("layer" + i); i++)
					layers.add(textures.path("layer" + i).asString());
			}
			String parent = model.path("parent").isString() ? model.path("parent").asString() : "";
			parent = parent.startsWith("minecraft:") ? parent.substring("minecraft:".length()) : parent;
			if (parent.equals("builtin/generated") || parent.equals("item/generated") || parent.equals("item/handheld"))
				return layers == null ? List.of() : layers;
			if (!parent.startsWith("item/"))
				return List.of();
			path = "assets/minecraft/models/" + parent + ".json";
		}
		return List.of();
	}

	private static String texturePath(String texture) {
		String path = texture.startsWith("minecraft:") ? texture.substring("minecraft:".length()) : texture;
		return "assets/minecraft/textures/" + path + ".png";
	}

	/** MC 1.5 named item textures after the item's translation key, for example textures/items/swordDiamond.png. */
	private List<BufferedImage> version5Layers(LegacyItems.Entry entry, String name) {
		if (entry.isBlock() || entry.langKey() == null || !entry.langKey().startsWith("item."))
			return List.of();
		String key = entry.langKey().substring("item.".length(), entry.langKey().length() - ".name".length()).replace('.', '_');
		List<String> names = new ArrayList<>();
		if (name.contains("potion")) {
			names.add("potion_contents");
			names.add(name.startsWith("splash") ? "potion_splash" : "potion");
		} else {
			names.add(key);
			if (name.startsWith("leather_"))
				names.add(key + "_overlay");
		}
		List<BufferedImage> layers = new ArrayList<>();
		for (String texture : names) {
			BufferedImage image = loadTexture("textures/items/" + texture + ".png");
			if (image == null)
				return layers.isEmpty() ? List.of() : layers;
			layers.add(image);
		}
		return layers;
	}

	/** MC 1.4 and older keep item icons in one 16x16 grid. */
	private List<BufferedImage> itemSheetLayers(LegacyItems.Entry entry, String name) {
		int index = entry.itemSheetIndex();
		if (index < 0)
			return List.of();
		if (name.contains("potion"))
			return List.of(itemSheetIcon(ITEM_SHEET_POTION_CONTENTS), itemSheetIcon(index));
		if (name.startsWith("leather_"))
			return List.of(itemSheetIcon(index), itemSheetIcon(index + ITEM_SHEET_LEATHER_OVERLAY_OFFSET));
		return List.of(itemSheetIcon(index));
	}

	private BufferedImage itemSheetIcon(int index) {
		return texturePack.getItem(index % 16, index / 16);
	}

	private BufferedImage loadTexture(String path) {
		if (!texturePack.fileExists(path))
			return null;
		try {
			return texturePack.getTextureFirstFrame(path, texturePack.loadTexture(path));
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	private BufferedImage tint(String texture, BufferedImage image) {
		String textureName = texture.substring(texture.lastIndexOf('/') + 1);
		if (!TINTED_TEXTURES.contains(textureName))
			return image;
		Colour4f tintColor = texturePack.getFoliageColor(BiomesOld.FOREST);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				Colour4f pixel = new Colour4f(image.getRGB(x, y));
				pixel.multiply(tintColor);
				image.setRGB(x, y, pixel.toArgb());
			}
		}
		return image;
	}

	/**
	 * Writes a flat icon. Leather armor, potions and tipped arrows keep their first layer separate from the others,
	 * so the viewer can color it, matching the icons rendered for 1.13 and newer.
	 */
	private void writeFlatIcon(String name, List<BufferedImage> layers) {
		boolean splitOverlay = layers.size() > 1 && (name.startsWith("leather_") || name.contains("potion") || name.equals("tipped_arrow"));
		BufferedImage first = layers.getFirst();
		int width = first.getWidth();
		int height = first.getHeight();
		if (splitOverlay) {
			OutputResourcesUtil.writeImage(first, 16, 16, new File(itemIconDir, name + ".png"));
			OutputResourcesUtil.writeImage(composite(layers.subList(1, layers.size()), width, height), 16, 16,
					new File(itemIconDir, name + "_overlay.png"));
		} else {
			OutputResourcesUtil.writeImage(composite(layers, width, height), 16, 16, new File(itemIconDir, name + ".png"));
		}
	}

	private static BufferedImage composite(List<BufferedImage> layers, int width, int height) {
		BufferedImage composited = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		var graphics = composited.createGraphics();
		try {
			for (BufferedImage layer : layers)
				graphics.drawImage(layer, 0, 0, width, height, null);
		} finally {
			graphics.dispose();
		}
		return composited;
	}

	private boolean renderBlock(LegacyItems.Entry entry, String name) throws Exception {
		if (!entry.isBlock() || blockTypeRegistry == null)
			return false;
		// Face containers toward the camera, like the container marker icons.
		int data = CHESTS.contains(entry.legacyId()) ? 5 : entry.damage();
		if (blockTypeRegistry.find(entry.numericId(), data) instanceof Air)
			return false;
		itemRenderer.renderBlockOld(new File(itemIconDir, name + ".png"), blockTypeRegistry, texturePack, entry.numericId(), data);
		return true;
	}
}
