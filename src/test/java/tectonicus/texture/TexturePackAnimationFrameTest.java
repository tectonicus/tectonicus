/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.texture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;

import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TexturePackAnimationFrameTest {
	@ParameterizedTest
	@CsvSource({"16,24576", "32,51168"})
	void uploadsOnlyOneFrameOfFaithlessSizedLavaStrips(int width, int height) {
		BufferedImage strip = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		strip.setRGB(0, width, 0xfbff6600);
		List<BufferedImage> uploads = new ArrayList<>();

		PackTexture texture = TexturePack.loadTextureFrame(rasteriser(uploads), "lava", strip, 2);

		assertEquals(1, uploads.size());
		assertEquals(width, uploads.getFirst().getWidth());
		assertEquals(width, uploads.getFirst().getHeight());
		assertEquals(0xfbff6600, texture.getImage().getRGB(0, 0));
		assertEquals(width, texture.getFullTexture().texture.getHeight());
		assertEquals(0, strip.getRGB(0, 0), "The source strip must remain unchanged");
	}

	@ParameterizedTest
	@CsvSource({"0,0", "-1,0", "1,0", "2,1", "3,2", "4,0", "8,1"})
	void selectsTheSameOneBasedWrappingFrameAsFluidRenderer(int frame, int expectedIndex) {
		BufferedImage strip = new BufferedImage(4, 12, BufferedImage.TYPE_INT_ARGB);
		for (int index = 0; index < 3; index++)
			strip.setRGB(0, index * 4, 0xff000000 | index);
		PackTexture texture = TexturePack.loadTextureFrame(rasteriser(new ArrayList<>()), "lava", strip, frame);

		assertEquals(0xff000000 | expectedIndex, texture.getImage().getRGB(0, 0));
	}

	@Test
	void blockModelLookupsAlsoAvoidUploadingOversizedAnimationStrips() {
		BufferedImage strip = new BufferedImage(16, 24576, BufferedImage.TYPE_INT_ARGB);
		strip.setRGB(0, 0, 0xffff6600);
		List<BufferedImage> uploads = new ArrayList<>();

		PackTexture texture = new PackTexture(rasteriser(uploads), "block/lava_still.png", strip);

		assertEquals(16, uploads.getFirst().getHeight());
		assertEquals(16, texture.getImage().getHeight());
		assertEquals(0xffff6600, texture.getImage().getRGB(0, 0));
	}

	@Test
	void preservesAnimationStripsThatFitTheGpu() {
		BufferedImage strip = new BufferedImage(16, 256, BufferedImage.TYPE_INT_ARGB);
		List<BufferedImage> uploads = new ArrayList<>();

		PackTexture texture = new PackTexture(rasteriser(uploads), "block/lava_still.png", strip);

		assertEquals(256, uploads.getFirst().getHeight());
		assertEquals(256, texture.getImage().getHeight());
	}

	@Test
	void leavesSquareTexturesAndTheirOpacityIntact() {
		BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 4; y++)
			for (int x = 0; x < 4; x++)
				image.setRGB(x, y, 0xffff6600);
		PackTexture texture = TexturePack.loadTextureFrame(rasteriser(new ArrayList<>()), "lava", image, 9);

		assertEquals(4, texture.getImage().getWidth());
		assertEquals(4, texture.getImage().getHeight());
		assertFalse(texture.isTransparent());
		assertFalse(texture.isTranslucent());
	}

	private Rasteriser rasteriser(List<BufferedImage> uploads) {
		return (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
				(proxy, method, args) -> {
					if (method.getName().equals("getMaxTextureSize"))
						return 16384;
					if (!method.getName().equals("createTexture"))
						throw new AssertionError("Unexpected rasteriser call: " + method.getName());
					BufferedImage image = (BufferedImage) args[0];
					uploads.add(image);
					return new Texture() {
						@Override public int getWidth() { return image.getWidth(); }
						@Override public int getHeight() { return image.getHeight(); }
					};
				});
	}
}
