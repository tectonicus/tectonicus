/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import lombok.Getter;
import lombok.Setter;
import tectonicus.Minecraft;
import tectonicus.raw.LevelDat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A Minecraft save and the dimensions rendered from it. */
@Getter
@Setter
public class WorldConfig extends RenderOptions {
	private final String id;
	private String name;
	private File worldDir;
	private File minecraftJar;
	private File texturePack;
	private boolean usingProgrammerArt;
	private List<File> modJars = new ArrayList<>();
	private MarkerVisibility markerVisibility = new MarkerVisibility();
	@Setter(lombok.AccessLevel.NONE)
	@Getter(lombok.AccessLevel.NONE)
	private final List<DimensionConfig> dimensions = new ArrayList<>();

	public WorldConfig(String id, File worldDir) {
		this.id = id;
		this.name = id;
		this.worldDir = worldDir;
	}

	public static String defaultName(File worldDir) {
		Path directory = worldDir.toPath().toAbsolutePath().normalize();
		Path levelDat = Minecraft.findLevelDat(directory);
		if (levelDat != null && Files.isRegularFile(levelDat)) {
			try {
				String name = new LevelDat(levelDat, "").getWorldName();
				if (name != null && !name.isBlank())
					return name;
			} catch (RuntimeException ignored) {
				// Unreadable world metadata should not prevent using the folder name.
			}
		}
		return directory.getFileName() != null ? directory.getFileName().toString() : directory.toString();
	}

	public List<DimensionConfig> getDimensions() {
		return List.copyOf(dimensions);
	}

	public void addDimension(DimensionConfig dimension) {
		if (dimensions.stream().anyMatch(existing -> existing.getId().equalsIgnoreCase(dimension.getId())))
			throw new IllegalArgumentException("Duplicate dimension ID: " + dimension.getId());
		if (dimension.getWorld() != null && dimension.getWorld() != this)
			throw new IllegalArgumentException("Dimension already belongs to a world");
		dimension.setWorld(this);
		for (LayerConfig layer : dimension.getLayers())
			layer.setWorld(this);
		dimensions.add(dimension);
	}
}
