/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.bedrock;

import org.tectonicus.jnbt.CompoundTag;
import org.tectonicus.jnbt.IntTag;
import org.tectonicus.jnbt.ListTag;
import org.tectonicus.jnbt.LongTag;
import org.tectonicus.jnbt.StringTag;
import org.tectonicus.jnbt.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The parts of a Bedrock Edition {@code level.dat} that Tectonicus uses. The file is an 8-byte header (storage
 * version and NBT length, little-endian) followed by uncompressed little-endian NBT.
 *
 * @param lastOpenedWithVersion the game version that last saved the world, such as [1, 21, 120, 4, 0]
 */
public record BedrockLevel(String name, int spawnX, int spawnY, int spawnZ, Long seed, int[] lastOpenedWithVersion) {
	/** Bedrock's spawn Y when the game should find the surface itself. */
	public static final int SPAWN_Y_UNSET = 32767;

	/** Whether a {@code level.dat} has Bedrock's header rather than Java Edition's gzip stream. */
	public static boolean isBedrockLevelDat(Path file) throws IOException {
		long size = Files.size(file);
		if (size < 9)
			return false;
		byte[] header = new byte[8];
		try (InputStream in = Files.newInputStream(file)) {
			if (in.readNBytes(header, 0, 8) != 8)
				return false;
		}
		if ((header[0] & 0xFF) == 0x1F && (header[1] & 0xFF) == 0x8B)
			return false; // gzip, so Java Edition
		int length = ByteBuffer.wrap(header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
		return length == size - 8;
	}

	public static BedrockLevel read(Path file) throws IOException {
		byte[] bytes = Files.readAllBytes(file);
		CompoundTag root = BedrockNbt.readCompound(java.util.Arrays.copyOfRange(bytes, 8, bytes.length));
		int[] version = new int[0];
		if (root.getValue().get("lastOpenedWithVersion") instanceof ListTag list)
			version = list.getValue().stream().filter(IntTag.class::isInstance).mapToInt(t -> ((IntTag) t).getValue()).toArray();
		int spawnY = intValue(root, "SpawnY", 64);
		return new BedrockLevel(root.getValue().get("LevelName") instanceof StringTag s ? s.getValue() : "",
				intValue(root, "SpawnX", 0), spawnY == SPAWN_Y_UNSET ? 64 : spawnY, intValue(root, "SpawnZ", 0),
				root.getValue().get("RandomSeed") instanceof LongTag l ? l.getValue() : null, version);
	}

	private static int intValue(CompoundTag tag, String name, int fallback) {
		Tag child = tag.getValue().get(name);
		return child instanceof IntTag i ? i.getValue() : fallback;
	}

	/** The version that last saved the world, such as 1.21.120, or "unknown". */
	public String versionName() {
		if (lastOpenedWithVersion.length < 3)
			return "unknown";
		return List.of(lastOpenedWithVersion[0], lastOpenedWithVersion[1], lastOpenedWithVersion[2]).stream()
				.map(String::valueOf).collect(Collectors.joining("."));
	}

	/**
	 * The Java Edition version with roughly the same blocks, used to choose version-dependent behaviour. Bedrock and
	 * Java ship the same game drops, but Bedrock numbers its releases differently. Worlds older than 1.21 use 1.21:
	 * the block mapping always gives modern, flattened Java names, which older Java versions' assets cannot draw.
	 */
	public String javaVersion() {
		if (lastOpenedWithVersion.length < 3)
			return "1.21";
		int major = lastOpenedWithVersion[0];
		int minor = lastOpenedWithVersion[1];
		int patch = lastOpenedWithVersion[2];
		if (major == 1 && minor <= 21)
			return minor == 21 && patch >= 110 ? "1.21.10" : "1.21";
		// Bedrock 1.26.x and later use the year-based Java numbering.
		int drop = major == 1 ? patch : minor;
		if (drop < 20)
			return "26.1";
		if (drop < 40)
			return "26.2";
		if (drop < 60)
			return "26.3";
		return "26.4";
	}

	/** Whether the world has the 1.18 height range: Y -64 to 319 in the Overworld. */
	public boolean hasCavesAndCliffsHeight() {
		if (lastOpenedWithVersion.length < 2)
			return true;
		return lastOpenedWithVersion[0] > 1 || lastOpenedWithVersion[1] >= 18;
	}
}
