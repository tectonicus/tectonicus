/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */
package tectonicus.itemregistry;

import lombok.experimental.UtilityClass;
import tectonicus.blockregistry.BlockRegistry;
import tectonicus.raw.BiomesOld;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;

import java.awt.image.BufferedImage;
import java.io.IOException;

/** Loads the layer textures of flat (generated) item models. */
@UtilityClass
public class ItemLayers {
	/**
	 * @param layerKey the texture variable of the layer, such as "layer0"
	 * @param layerValue the texture of the layer, such as "minecraft:item/diamond_sword"
	 * @return the first animation frame of the layer, with armor trim palettes and block tints applied
	 */
	public static BufferedImage load(TexturePack texturePack, BlockRegistry blockRegistry, String layerKey, String layerValue) throws IOException {
		final String[] layerTexture = layerValue.split(":");
		final String namespace = layerTexture.length == 1 ? "minecraft" : layerTexture[0];
		final String textureId = layerTexture.length == 1 ? layerTexture[0] : layerTexture[1];

		BufferedImage texture;
		String texturePath;

		if (!layerKey.equals("layer0") && textureId.contains("trim")) {
			String[] trimParts = textureId.split("_trim_");

			texturePath = "assets/" + namespace + "/textures/" + trimParts[0] + "_trim.png";
			String palette;
			String keyPalette;
			String newKeyPalette = "assets/" + namespace + "/textures/palettes/trim_base.png";
			if (texturePack.getZipStack().hasFile(newKeyPalette)) { //26.3+
				palette = "assets/" + namespace + "/textures/palettes/trim/" + trimParts[1] + ".png";
				keyPalette = newKeyPalette;
			} else {
				palette = "assets/" + namespace + "/textures/trims/color_palettes/" + trimParts[1] + ".png";
				keyPalette = "assets/" + namespace + "/textures/trims/color_palettes/trim_palette.png";
			}

			texture = texturePack.loadPalettedTexture(texturePath, palette, keyPalette);
		} else {
			texturePath = "assets/" + namespace + "/textures/" + textureId + ".png";
			texture = texturePack.loadTexture(texturePath);
		}
		texture = texturePack.getTextureFirstFrame(texturePath, texture);

		var block = blockRegistry == null ? null : blockRegistry.getBlockModels().getIfPresent(layerValue);
		if (block != null && block.getElements().get(0).getFaces().values().iterator().next().isTinted())
			tint(texture, texturePack.getFoliageColor(BiomesOld.FOREST));
		return texture;
	}

	/** Multiplies every pixel of the image by a colour. */
	public static void tint(BufferedImage image, Colour4f tintColor) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				Colour4f pixel = new Colour4f(image.getRGB(x, y));
				pixel.multiply(tintColor);
				image.setRGB(x, y, pixel.toArgb());
			}
		}
	}
}
