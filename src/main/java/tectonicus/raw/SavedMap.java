/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.raw;

import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class SavedMap {
	private static final int SIZE = 128;
	private static final int[] BRIGHTNESS = {180, 220, 255, 135};
	private static final int[] PALETTE = {
			0x000000, 0x7FB238, 0xF7E9A3, 0xC7C7C7, 0xFF0000, 0xA0A0FF, 0xA7A7A7, 0x007C00,
			0xFFFFFF, 0xA4A8B8, 0x976D4D, 0x707070, 0x4040FF, 0x8F7748, 0xFFFCF5, 0xD87F33,
			0xB24CD8, 0x6699D8, 0xE5E533, 0x7FCC19, 0xF27FA5, 0x4C4C4C, 0x999999, 0x4C7F99,
			0x7F3FB2, 0x334CB2, 0x664C33, 0x667F33, 0x993333, 0x191919, 0xFAEE4D, 0x5CDBD5,
			0x4A80FF, 0x00D93A, 0x815631, 0x700200, 0xD1B1A1, 0x9F5224, 0x95576C, 0x706C8A,
			0xBA8524, 0x677535, 0xA04D4E, 0x392923, 0x876B62, 0x575C5C, 0x7A4958, 0x4C3E5C,
			0x4C3223, 0x4C522A, 0x8E3C2E, 0x251610, 0xBD3031, 0x943F61, 0x5C191D, 0x167E86,
			0x3A8E8C, 0x562C3E, 0x14B485, 0x646464, 0xD8AF93, 0x7FA796
	};
	// Minecraft 1.4–1.6 used a smaller base palette.
	private static final int[] LEGACY_PALETTE = {
			0x000000, 0x7FB238, 0xF7E9A3, 0xA7A7A7, 0xFF0000, 0xA0A0FF, 0xA7A7A7,
			0x007C00, 0xFFFFFF, 0xA4A8B8, 0xB76A2F, 0x707070, 0x4040FF, 0x685332
	};

	private final byte[] colors;
	private final int[] palette;
	private final byte[] contentHash;

	private SavedMap(byte[] colors, boolean legacyPalette) {
		this.colors = colors;
		palette = legacyPalette ? LEGACY_PALETTE : PALETTE;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update((byte) (legacyPalette ? 1 : 0));
			contentHash = digest.digest(colors);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public static SavedMap load(Path path, boolean useLegacyPalette) throws IOException {
		try (InputStream in = Files.newInputStream(path);
		     NBTInputStream nbt = new NBTInputStream(in)) {
			if (nbt.readTag() instanceof CompoundTag root) {
				CompoundTag data = NbtUtil.getChild(root, "data", CompoundTag.class);
				ByteArrayTag colors = data == null ? null : NbtUtil.getChild(data, "colors", ByteArrayTag.class);
				if (colors != null && colors.getValue().length == SIZE * SIZE) {
					boolean legacyPalette = useLegacyPalette && NbtUtil.getInt(root, "DataVersion", 0) == 0;
					return new SavedMap(colors.getValue(), legacyPalette);
				}
			}
			throw new IOException("Expected a 128x128 map color array");
		}
	}

	public byte[] contentHash() {
		return contentHash.clone();
	}

	public BufferedImage toImage() {
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < pixels.length; i++) {
			int packed = Byte.toUnsignedInt(colors[i]);
			int colorId = packed >>> 2;
			if (colorId == 0 || colorId >= palette.length)
				continue;
			int rgb = palette[colorId];
			int brightness = BRIGHTNESS[packed & 3];
			int red = (rgb >>> 16) * brightness / 255;
			int green = (rgb >>> 8 & 255) * brightness / 255;
			int blue = (rgb & 255) * brightness / 255;
			pixels[i] = 0xFF000000 | red << 16 | green << 8 | blue;
		}
		return image;
	}
}
