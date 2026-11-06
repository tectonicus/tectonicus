package tectonicus.world;

import tectonicus.raw.Biome;
import tectonicus.raw.BiomesOld;
import tectonicus.util.Colour4f;

/** The Minecraft 1.0 swamp palette adjustment, applied before spatial blending. */
final class Minecraft10SwampColors {
	static boolean applies(Biome biome) {
		return biome == BiomesOld.SWAMP || biome == BiomesOld.SWAMP_HILLS;
	}

	static Colour4f plant(Colour4f color) {
		int rgb = ((color.toRgb() & 0xFEFEFE) + 0x4E0E4E) / 2;
		return new Colour4f((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255);
	}
}
