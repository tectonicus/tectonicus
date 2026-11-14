/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.cache;

import lombok.extern.slf4j.Slf4j;
import tectonicus.texture.ZipStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Properties;
import java.util.Set;

/** Reuses exported icons, including their optional color overlays, across renders. */
@Slf4j
public class ItemIconCache {
	// Increment when icon rendering changes require cached icons to be regenerated.
	private static final int RENDERER_VERSION = 2;
	private final Path directory;
	private final Properties previous = new Properties();
	private String inputs;

	/** A null options string disables reuse, for renders with useCache=false. */
	public ItemIconCache(Path directory, ZipStack resources, String options) {
		this.directory = directory;
		if (options == null)
			return;
		try {
			MessageDigest digest = newDigest();
			update(digest, RENDERER_VERSION + "\n" + options);
			update(digest, hashFile(Path.of(resources.getBaseFileName())));
			update(digest, resources.getOverrideFileName() == null ? "none" : hashFile(Path.of(resources.getOverrideFileName())));
			for (var mod : resources.getModJars())
				update(digest, hashFile(mod.toPath()));
			inputs = HexFormat.of().formatHex(digest.digest());
			if (Files.isRegularFile(manifest())) {
				try (InputStream in = Files.newInputStream(manifest())) {
					previous.load(in);
				}
			}
		} catch (IOException | IllegalArgumentException e) {
			previous.clear();
			log.warn("Unable to read item icon cache; icons will be regenerated", e);
		}
	}

	/** Remove outdated outputs before rendering so failures cannot cache stale images. */
	public Set<String> prepare(Set<String> items) throws IOException {
		Set<String> reused = new HashSet<>();
		for (String item : items) {
			String saved = previous.getProperty("icon." + item);
			if (inputs != null && inputs.equals(previous.getProperty("inputs"))
					&& saved != null && saved.equals(outputHash(item))) {
				reused.add(item);
			} else {
				Files.deleteIfExists(directory.resolve(item + ".png"));
				Files.deleteIfExists(directory.resolve(item + "_overlay.png"));
			}
		}
		// An interrupted render must not leave a manifest describing its previous outputs.
		Files.deleteIfExists(manifest());
		return reused;
	}

	public void save(Set<String> items, Set<String> failed) throws IOException {
		if (inputs == null)
			return;
		Properties current = new Properties();
		current.setProperty("inputs", inputs);
		for (String item : items) {
			if (failed.contains(item))
				continue;
			String hash = outputHash(item);
			if (hash != null)
				current.setProperty("icon." + item, hash);
		}
		Path temporary = Files.createTempFile(directory, "item-icons-", ".tmp");
		try {
			try (var out = Files.newOutputStream(temporary)) {
				current.store(out, "Tectonicus inventory item icons");
			}
			Files.move(temporary, manifest(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private Path manifest() {
		return directory.resolve("item-icons.cache");
	}

	private String outputHash(String item) throws IOException {
		Path base = directory.resolve(item + ".png");
		if (!Files.isRegularFile(base) || Files.size(base) == 0)
			return null;
		Path overlay = directory.resolve(item + "_overlay.png");
		return hashFile(base) + ":" + (Files.isRegularFile(overlay) ? hashFile(overlay) : "none");
	}

	private static String hashFile(Path file) throws IOException {
		MessageDigest digest = newDigest();
		try (var in = new DigestInputStream(Files.newInputStream(file), digest)) {
			in.transferTo(java.io.OutputStream.nullOutputStream());
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	private static void update(MessageDigest digest, String value) {
		digest.update(value.getBytes(StandardCharsets.UTF_8));
		digest.update((byte) 0);
	}

	private static MessageDigest newDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
