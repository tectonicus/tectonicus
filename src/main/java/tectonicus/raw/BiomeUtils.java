/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.experimental.UtilityClass;

import java.awt.Point;

@UtilityClass
public class BiomeUtils {
	public Point getColorCoords(float temperature, float rainfall) {
		float adjTemp = clamp(temperature);
		float adjRainfall = clamp(rainfall) * adjTemp;

		return new Point(normalize(adjTemp), normalize(adjRainfall));
	}

	public float clamp(float val) {
		return Math.max(0.0f, Math.min(1.0f, val));
	}

	public int normalize(float val) {
		return 255 - Math.round(val * 255);
	}
}
