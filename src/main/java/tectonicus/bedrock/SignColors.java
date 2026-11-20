/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

/** Converts the ARGB text colour Bedrock stores on signs into the dye name Java stores. */
final class SignColors {
	private static final String[] NAMES = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
			"light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
	// Bedrock's dye text colours, in the order above
	private static final int[] COLORS = {0xF0F0F0, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
			0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21};

	private SignColors() {}

	static String dyeName(int argb) {
		int rgb = argb & 0xFFFFFF;
		if (rgb == 0)
			return "black";
		String best = "black";
		long bestDistance = Long.MAX_VALUE;
		for (int i = 0; i < COLORS.length; i++) {
			int dr = ((rgb >> 16) & 0xFF) - ((COLORS[i] >> 16) & 0xFF);
			int dg = ((rgb >> 8) & 0xFF) - ((COLORS[i] >> 8) & 0xFF);
			int db = (rgb & 0xFF) - (COLORS[i] & 0xFF);
			long distance = (long) dr * dr + (long) dg * dg + (long) db * db;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = NAMES[i];
			}
		}
		return best;
	}
}
