/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.texture;

import java.awt.image.BufferedImage;
import java.util.Random;

/** Historical lava simulation: warped diffusion and occasional hot impulses. */
final class LegacyLavaTexture {
	private final boolean flowing;
	private final Random random = new Random(0);
	private float[] current = new float[256];
	private float[] next = new float[256];
	private final float[] velocity = new float[256];
	private final float[] impulse = new float[256];
	private int tick;

	LegacyLavaTexture(boolean flowing) { this.flowing = flowing; }

	void step() {
		tick++;
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				int dx = (int)(Math.sin(y * (float)Math.PI * 2 / 16) * 1.2f);
				int dy = (int)(Math.sin(x * (float)Math.PI * 2 / 16) * 1.2f);
				float sum = 0;
				for (int sx = x - 1; sx <= x + 1; sx++) {
					for (int sy = y - 1; sy <= y + 1; sy++) {
						sum += current[((sx + dx) & 15) + ((sy + dy) & 15) * 16];
					}
				}
				int i = x + y * 16;
				float heat = velocity[i] + velocity[((x + 1) & 15) + y * 16]
						+ velocity[((x + 1) & 15) + ((y + 1) & 15) * 16]
						+ velocity[x + ((y + 1) & 15) * 16];
				next[i] = sum / 10 + heat / 4 * 0.8f;
				// Vanilla updates velocity in this same loop, affecting later neighbors.
				velocity[i] = Math.max(0, velocity[i] + impulse[i] * 0.01f);
				impulse[i] -= 0.06f;
				if (random.nextDouble() < 0.005) impulse[i] = 1.5f;
			}
		}
		float[] previous = current;
		current = next;
		next = previous;
	}

	BufferedImage image() {
		int size = flowing ? 32 : 16;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
			int i = (x & 15) + (y & 15) * 16;
			if (flowing) i = (i - tick / 3 * 16) & 255;
			image.setRGB(x, y, color(current[i]));
		}
		return image;
	}

	static int color(float value) {
		float u = Math.max(0, Math.min(1, value * 2));
		return 0xFF000000 | (int)(155 + u * 100) << 16 | (int)(u * u * 255) << 8
				| (int)(u * u * u * u * 128);
	}

	static BufferedImage frame(boolean flowing, int frame) {
		LegacyLavaTexture effect = new LegacyLavaTexture(flowing);
		int ticks = 100 + Math.floorMod(Math.max(1, frame) - 1, 256);
		for (int i = 0; i < ticks; i++) effect.step();
		return effect.image();
	}
}
