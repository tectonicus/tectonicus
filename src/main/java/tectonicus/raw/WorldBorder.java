/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.extern.slf4j.Slf4j;
import org.jnbt.CompoundTag;
import org.jnbt.NBTInputStream;
import org.jnbt.Tag;
import tectonicus.configuration.Dimension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** The square world border of one dimension, in block coordinates. */
@Slf4j
public record WorldBorder(double centerX, double centerZ, double size) {
	static final double DEFAULT_SIZE = 59_999_968;
	private static final Path SAVED_DATA = Path.of("data", "minecraft", "world_border.dat");

	/**
	 * Newer versions save the border as saved data, older ones in level.dat. A border shared by the save
	 * applies to the Nether at an eighth of its position and size, like Minecraft's own coordinate scale.
	 */
	public static Optional<WorldBorder> load(Path worldDir, Path dimensionDir, Dimension dimension) {
		boolean ownBorder = !dimensionDir.normalize().equals(worldDir.normalize());
		Optional<WorldBorder> border = ownBorder ? readSavedData(dimensionDir.resolve(SAVED_DATA)) : Optional.empty();
		if (border.isPresent())
			return border;
		border = readSavedData(worldDir.resolve(SAVED_DATA)).or(() -> readLevelDat(worldDir.resolve("level.dat")));
		return dimension == Dimension.NETHER ? border.map(b -> b.scaled(1 / 8.0)) : border;
	}

	WorldBorder scaled(double scale) {
		return new WorldBorder(centerX * scale, centerZ * scale, size * scale);
	}

	static Optional<WorldBorder> readSavedData(Path file) {
		return readRoot(file).map(root -> NbtUtil.getChild(root, "data", CompoundTag.class))
				.filter(data -> data.getValue().containsKey("size"))
				.map(data -> new WorldBorder(NbtUtil.getDouble(data, "center_x", 0), NbtUtil.getDouble(data, "center_z", 0),
						NbtUtil.getDouble(data, "size", DEFAULT_SIZE)));
	}

	static Optional<WorldBorder> readLevelDat(Path file) {
		// Worlds from before 1.8 have no border fields and no border.
		return readRoot(file).map(root -> NbtUtil.getChild(root, "Data", CompoundTag.class))
				.filter(data -> data.getValue().containsKey("BorderSize"))
				.map(data -> new WorldBorder(NbtUtil.getDouble(data, "BorderCenterX", 0), NbtUtil.getDouble(data, "BorderCenterZ", 0),
						NbtUtil.getDouble(data, "BorderSize", DEFAULT_SIZE)));
	}

	private static Optional<CompoundTag> readRoot(Path file) {
		if (!Files.isRegularFile(file))
			return Optional.empty();
		try (InputStream stream = Files.newInputStream(file); NBTInputStream in = new NBTInputStream(stream)) {
			Tag tag = in.readTag();
			return tag instanceof CompoundTag compound ? Optional.of(compound) : Optional.empty();
		} catch (IOException e) {
			log.warn("Unable to read the world border from {}", file, e);
			return Optional.empty();
		}
	}
}
