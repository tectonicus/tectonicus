/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.util;

import lombok.extern.slf4j.Slf4j;
import tectonicus.texture.ZipStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/** The front board shared by signs of one wood and model type. */
@Slf4j
record SignBackground(String namespace, String wood, boolean hanging) {
	private static final Pattern BLOCK = Pattern.compile(
			"(?:(?<namespace>[a-z0-9_][a-z0-9_.-]*):)?(?:(?<wood>[a-z0-9_]+?)_)??(?:wall_)?(?<hanging>hanging_)?sign");

	static SignBackground forBlock(String blockName) {
		var match = BLOCK.matcher(blockName);
		if (!match.matches()) return new SignBackground("minecraft", "oak", false);
		return new SignBackground(Objects.requireNonNullElse(match.group("namespace"), "minecraft"),
				Objects.requireNonNullElse(match.group("wood"), "oak"), match.group("hanging") != null);
	}

	String filename() {
		return "Signs/" + namespace + "/" + (hanging ? "hanging/" : "") + wood + ".png";
	}

	boolean write(ZipStack assets, Path imagesDir, boolean usesBlockTextures) {
		try {
			BufferedImage board = readBoard(assets, usesBlockTextures);
			if (board == null) return false;
			Path output = imagesDir.resolve(filename());
			Files.createDirectories(output.getParent());
			return ImageIO.write(board, "png", output.toFile());
		} catch (IOException e) {
			log.warn("Unable to export sign texture {}", filename(), e);
			return false;
		}
	}

	private BufferedImage readBoard(ZipStack assets, boolean usesBlockTextures) throws IOException {
		String textures = "assets/" + namespace + "/textures/";
		if (usesBlockTextures) {
			// Front UVs from the regular and hanging sign block models, in 16-unit UV space.
			BufferedImage image = read(assets, textures + "block/" + wood + (hanging ? "_hanging_sign.png" : "_sign.png"));
			return hanging ? crop(image, 16, 16, 1, 8, 7, 5) : crop(image, 16, 16, 0, 1, 12, 6);
		}

		BufferedImage image = read(assets, textures + "entity/signs/" + (hanging ? "hanging/" : "") + wood + ".png");
		if (image == null && !hanging && namespace.equals("minecraft") && wood.equals("oak")) {
			image = read(assets, textures + "entity/sign.png");
			if (image == null) image = read(assets, "item/sign.png");
		}
		// The entity sheets use a 64×32 layout, including the post or chains.
		return hanging ? crop(image, 64, 32, 2, 14, 14, 10) : crop(image, 64, 32, 2, 2, 24, 12);
	}

	private static BufferedImage read(ZipStack assets, String path) throws IOException {
		try (var input = assets.getStream(path)) {
			return input == null ? null : ImageIO.read(input);
		}
	}

	private static BufferedImage crop(BufferedImage image, int sheetWidth, int sheetHeight,
			int x, int y, int width, int height) {
		if (image == null || image.getWidth() % sheetWidth != 0 || image.getHeight() % sheetHeight != 0) return null;
		int scaleX = image.getWidth() / sheetWidth;
		int scaleY = image.getHeight() / sheetHeight;
		return image.getSubimage(x * scaleX, y * scaleY, width * scaleX, height * scaleY);
	}
}
