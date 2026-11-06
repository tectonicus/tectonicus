/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache;

import biomeextractor.WorldProcessor;
import lombok.extern.slf4j.Slf4j;
import tectonicus.Minecraft;
import tectonicus.chunk.ChunkCoord;
import tectonicus.util.FileUtils;
import tectonicus.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class FileBiomeCache implements BiomeCache {
	private static final int VERSION = 3;
	private final Path cacheDir;
	private final WorldProcessor processor;
	private final String cacheKey;
	private boolean closed;
	private final Map<ChunkCoord, BiomeData> memory = new LinkedHashMap<>(128, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<ChunkCoord, BiomeData> eldest) { return size() > 128; }
	};

	public FileBiomeCache(File baseCacheDir, File worldDir, File minecraftJar, long seed) throws IOException {
		cacheDir = baseCacheDir.toPath().resolve("biomeCache");
		processor = new WorldProcessor();
		boolean ready = false;
		try {
			processor.setJarLocation(minecraftJar);
			if (!processor.bindToMinecraft())
				throw new IOException("No biome extractor profile matches " + minecraftJar);
			Path implementation;
			try {
				implementation = Path.of(WorldProcessor.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			} catch (java.net.URISyntaxException e) {
				throw new IOException("Could not locate biome extractor implementation", e);
			}
			String identity = "version=" + VERSION + "\nseed=" + seed + "\nprofile=" + processor.getBindingProfileName()
					+ "\nminecraft=" + hash(minecraftJar.toPath()) + "\nextractor=" + hash(implementation);
			cacheKey = "extracted-biomes:" + HexFormat.of().formatHex(digest().digest(identity.getBytes(StandardCharsets.UTF_8)));
			Path index = cacheDir.resolve("biomes.cache");
			boolean valid = false;
			try {
				valid = Files.isRegularFile(index) && Files.readString(index).equals(cacheKey);
			} catch (IOException e) {
				log.debug("Regenerating unreadable biome cache index {}", index, e);
			}
			if (!valid) {
				log.info("Creating biome color cache for {}", processor.getBindingProfileName());
				FileUtils.deleteDirectory(cacheDir.toFile());
			}
			Path stub = cacheDir.resolve("stubWorld");
			FileUtils.deleteDirectory(stub.toFile());
			Files.createDirectories(stub);
			Files.copy(Minecraft.findLevelDat(worldDir.toPath()), stub.resolve("level.dat"), StandardCopyOption.REPLACE_EXISTING);
			if (!processor.setWorldFolder(stub.toFile()) || !processor.loadWorld())
				throw new IOException("Could not load world into biome extractor");
			Files.writeString(index, cacheKey);
			log.info("Using {} biome extraction at {}", processor.getBindingProfileName(), cacheDir);
			ready = true;
		} finally {
			if (!ready)
				processor.close();
		}
	}

	@Override
	public String getCacheKey() { return cacheKey; }

	@Override
	public synchronized BiomeData loadBiomeData(ChunkCoord chunk) {
		if (closed)
			throw new IllegalStateException("Biome cache is closed");
		BiomeData data = memory.get(chunk);
		if (data != null)
			return data;
		Path file = cacheDir.resolve(Util.toBase36(chunk.x)).resolve(Util.toBase36(chunk.z))
				.resolve("biomeData_" + Util.toBase36(chunk.x) + "_" + Util.toBase36(chunk.z) + ".dat");
		if (Files.isRegularFile(file)) {
			try {
				data = new BiomeData(file.toFile());
			} catch (IOException | RuntimeException e) {
				log.debug("Regenerating unreadable biome cache {}", file, e);
			}
		}
		if (data == null) {
			data = new BiomeData(processor, chunk);
			Path temporary = null;
			try {
				Files.createDirectories(file.getParent());
				temporary = Files.createTempFile(file.getParent(), "biomes-", ".tmp");
				data.write(temporary.toFile());
				Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				log.warn("Could not persist biome cache {}", file, e);
			} finally {
				if (temporary != null) {
					try { Files.deleteIfExists(temporary); } catch (IOException e) { log.debug("Could not remove {}", temporary, e); }
				}
			}
		}
		memory.put(new ChunkCoord(chunk), data);
		return data;
	}

	@Override
	public synchronized void close() {
		closed = true;
		memory.clear();
		try { processor.close(); } catch (IOException e) { log.warn("Could not close biome extractor", e); }
	}

	private static MessageDigest digest() {
		try { return MessageDigest.getInstance("SHA-256"); }
		catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
	}

	private static String hash(Path path) throws IOException {
		MessageDigest hash = digest();
		try (InputStream input = Files.newInputStream(path)) {
			byte[] bytes = new byte[8192];
			int read;
			while ((read = input.read(bytes)) != -1)
				hash.update(bytes, 0, read);
		}
		return HexFormat.of().formatHex(hash.digest());
	}
}
