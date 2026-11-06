/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.texture;

import java.awt.image.BufferedImage;
import java.util.Random;

/** Minecraft 1.0 water simulation, frozen to a repeatable frame for static maps. */
final class LegacyWaterTexture {
	private final boolean flowing;
	private final Random random = new Random(0);
	private float[] current = new float[256];
	private float[] next = new float[256];
	private final float[] velocity = new float[256];
	private final float[] impulse = new float[256];
	private int tick;

	LegacyWaterTexture(boolean flowing) { this.flowing = flowing; }

	void step() {
		tick++;
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				float sum = 0;
				for (int offset = -1; offset <= 1; offset++) {
					int sx = flowing ? x : x + offset;
					int sy = flowing ? y + offset - 1 : y;
					sum += current[(sx & 15) + (sy & 15) * 16];
				}
				next[x + y * 16] = sum / (flowing ? 3.2f : 3.3f) + velocity[x + y * 16] * 0.8f;
			}
		}
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				int i = x + y * 16;
				velocity[i] = Math.max(0, velocity[i] + impulse[i] * 0.05f);
				impulse[i] -= flowing ? 0.3f : 0.1f;
				if (random.nextDouble() < (flowing ? 0.2 : 0.05)) impulse[i] = 0.5f;
			}
		}
		float[] previous = current;
		current = next;
		next = previous;
	}

	BufferedImage image() {
		// The flowing effect repeats its 16x16 pixels over a 32x32 atlas area.
		int size = flowing ? 32 : 16;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int i = (x & 15) + (y & 15) * 16;
				if (flowing) i = (i - tick * 16) & 255;
				image.setRGB(x, y, color(current[i]));
			}
		}
		return image;
	}

	static int color(float value) {
		float u = Math.max(0, Math.min(1, value));
		float strength = u * u;
		return (int)(146 + strength * 50) << 24 | (int)(32 + strength * 32) << 16
				| (int)(50 + strength * 64) << 8 | 255;
	}

	static BufferedImage frame(boolean flowing, int frame) {
		LegacyWaterTexture effect = new LegacyWaterTexture(flowing);
		// Warm up the initially empty simulation; loop frame selections to bound work.
		int ticks = 100 + Math.floorMod(Math.max(1, frame) - 1, 256);
		for (int i = 0; i < ticks; i++) effect.step();
		return effect.image();
	}
}
