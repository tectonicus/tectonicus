/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.Getter;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import tectonicus.blockTypes.Banner.Pattern;
import tectonicus.world.Colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BannerEntity extends BlockEntity {
	@Getter
	private final Integer baseColor;
	private final List<Pattern> patterns;
	
	public BannerEntity(int x, int y, int z, int localX, int localY, int localZ, Integer baseColor, List<Pattern> patterns) {
		super(x, y, z, localX, localY, localZ);
		this.baseColor = baseColor;
		this.patterns = patterns;
	}
	
	public List<Pattern> getPatterns() {
		return Collections.unmodifiableList(patterns);
	}

	static List<Pattern> parsePatterns(ListTag layers, boolean legacyColors) {
		if (layers == null)
			return List.of();

		List<Pattern> patterns = new ArrayList<>();
		for (var layer : layers.getValue()) {
			if (!(layer instanceof CompoundTag tag))
				continue;

			String pattern = NbtUtil.getString(tag, "Pattern", "");
			String colorName = NbtUtil.getString(tag, "color", "");
			IntTag colorTag = NbtUtil.getChild(tag, "Color", IntTag.class);
			Colors color = Colors.WHITE;
			if (!colorName.isEmpty()) {
				color = Colors.byName(colorName);
			} else if (colorTag != null) {
				int colorId = colorTag.getValue();
				color = colorId >= 0 && colorId <= 15 ? Colors.byId(legacyColors ? 15 - colorId : colorId) : null;
			}
			if (!pattern.isEmpty() && color != null)
				patterns.add(new Pattern(pattern, color));
		}
		return patterns;
	}
}
