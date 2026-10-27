/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.texture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TexturePackSubImageTest {
	@TempDir Path directory;
	private static final String BEACON = "assets/minecraft/textures/gui/container/beacon.png";
	private static final String CHEST = "assets/minecraft/textures/gui/container/generic_54.png";

	@ParameterizedTest
	@CsvSource({"230,219", "250,256", "256,250", "512,256", "257,257"})
	void incompatibleGuiSheetsUseVanillaForEveryIconEvenWhenSomeCropsFit(int width, int height) throws Exception {
		withImages(width, height, stack -> {
			BufferedImage image = TexturePack.loadGuiImage(stack, BEACON, 256, 256);
			assertEquals(256, image.getWidth());
			assertEquals(256, image.getHeight());
			for (int[] crop : new int[][]{{17,21,21,22}, {17,46,21,22}, {17,71,21,22}, {157,21,21,22}, {232,0,18,18}}) {
				BufferedImage icon = image.getSubimage(crop[0], crop[1], crop[2], crop[3]);
				assertEquals(0xff123456, icon.getRGB(0, 0), "Every icon must come from the same vanilla sheet");
			}
		});
	}

	@ParameterizedTest
	@ValueSource(ints = {256, 512, 768})
	void compatibleGuiSheetsRetainResourcePackIconsAtTheirOriginalResolution(int resolution) throws Exception {
		withImages(resolution, resolution, stack -> {
			BufferedImage image = TexturePack.loadGuiImage(stack, BEACON, 256, 256);
			assertEquals(resolution, image.getWidth());
			assertEquals(resolution, image.getHeight());
			int scale = resolution / 256;
			assertEquals(0xffabcdef, image.getSubimage(17 * scale, 21 * scale, 21 * scale, 22 * scale).getRGB(0, 0));
			assertEquals(0xffabcdef, image.getSubimage(232 * scale, 0, 18 * scale, 18 * scale).getRGB(0, 0));
		});
	}

	@Test
	void fallsBackToVanillaWhenResourcePackRemovesTheIconArea() throws Exception {
		withImages(230, 219, stack -> {
			BufferedImage icon = TexturePack.getSubImage(stack, BEACON, 232, 0, 18, 18);
			assertEquals(18, icon.getWidth());
			assertEquals(18, icon.getHeight());
			assertEquals(0xff123456, icon.getRGB(0, 0));
		});
	}

	@Test
	void keepsResourcePackPixelsWhenCropFitsExactly() throws Exception {
		withImages(250, 18, stack -> {
			BufferedImage icon = TexturePack.getSubImage(stack, BEACON, 232, 0, 18, 18);
			assertEquals(0xffabcdef, icon.getRGB(0, 0));
		});
	}

	@Test
	void fallsBackWhenOnlyTheHeightIsTooSmall() throws Exception {
		withImages(256, 17, stack -> {
			BufferedImage icon = TexturePack.getSubImage(stack, BEACON, 232, 0, 18, 18);
			assertEquals(0xff123456, icon.getRGB(0, 0));
		});
	}

	@Test
	void fallsBackForFaithlessChestLayout() throws Exception {
		withImages(CHEST, 1056, 221, stack -> {
			BufferedImage image = TexturePack.loadChestImage(stack, "assets/minecraft/textures/");
			assertEquals(256, image.getWidth());
			assertEquals(256, image.getHeight());
			assertEquals(0xff123456, image.getRGB(0, 0));
		});
	}

	@Test
	void preservesHighResolutionChestTextures() throws Exception {
		withImages(CHEST, 512, 512, stack -> {
			BufferedImage image = TexturePack.loadChestImage(stack, "assets/minecraft/textures/");
			assertEquals(512, image.getWidth());
			assertEquals(512, image.getHeight());
			assertEquals(0xffabcdef, image.getRGB(0, 0));
		});
	}

	@Test
	void fallsBackToModernVanillaChestWhenPackOverridesOnlyLegacyPath() throws Exception {
		Path base = zip("client.jar", CHEST, 256, 256, 0xff123456);
		Path pack = zip("pack.zip", "assets/minecraft/textures/gui/container.png", 1056, 221, 0xffabcdef);
		withStack(base, pack, stack -> {
			BufferedImage image = TexturePack.loadChestImage(stack, "assets/minecraft/textures/");
			assertEquals(256, image.getWidth());
			assertEquals(0xff123456, image.getRGB(0, 0));
		});
	}

	private void withImages(int width, int height, ImageCheck check) throws Exception {
		withImages(BEACON, width, height, check);
	}

	private void withImages(String path, int width, int height, ImageCheck check) throws Exception {
		Path base = zip("client.jar", path, 256, 256, 0xff123456);
		Path pack = zip("pack.zip", path, width, height, 0xffabcdef);
		withStack(base, pack, check);
	}

	private void withStack(Path base, Path pack, ImageCheck check) throws Exception {
		ZipStack stack = new ZipStack(base.toFile(), pack.toFile(), List.of());
		try {
			check.run(stack);
		} finally {
			for (String name : List.of("base", "override")) {
				var field = ZipStack.class.getDeclaredField(name);
				field.setAccessible(true);
				((FileSystem) field.get(stack)).close();
			}
		}
	}

	private Path zip(String name, String texturePath, int width, int height, int color) throws Exception {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				image.setRGB(x, y, color);
		Path path = directory.resolve(name);
		try (ZipOutputStream stream = new ZipOutputStream(Files.newOutputStream(path))) {
			stream.putNextEntry(new ZipEntry(texturePath));
			ImageIO.write(image, "png", stream);
			stream.closeEntry();
		}
		return path;
	}

	@FunctionalInterface
	private interface ImageCheck {
		void run(ZipStack stack) throws Exception;
	}
}
