/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.util;

import lombok.extern.slf4j.Slf4j;
import tectonicus.Minecraft;
import tectonicus.texture.ZipStack;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/** Resolves the language asset for the exact Minecraft client used to render a world. */
@Slf4j
final class MinecraftLanguageAssets {
	private static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
	private static final String OBJECT_SERVER = "https://resources.download.minecraft.net/";
	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	private final Path launcherDir;
	private final Download download;
	private final Map<Language, Map<String, String>> languages = new HashMap<>();
	private final Map<Language, byte[]> extraAssets = new HashMap<>();
	private final Map<Path, JsonNode> indexes = new HashMap<>();
	private JsonNode manifest;

	MinecraftLanguageAssets() {
		this(Minecraft.findMinecraftDir().toPath(), new Download() {
			private HttpClient client;

			@Override
			public byte[] get(URI uri) throws IOException, InterruptedException {
				if (client == null) client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
				var response = client.send(HttpRequest.newBuilder(uri).timeout(TIMEOUT).build(), HttpResponse.BodyHandlers.ofByteArray());
				if (response.statusCode() != 200) throw new IOException("HTTP " + response.statusCode() + " for " + uri);
				return response.body();
			}
		});
	}

	MinecraftLanguageAssets(Path launcherDir, Download download) {
		this.launcherDir = launcherDir;
		this.download = download;
	}

	Map<String, String> load(ZipStack assets, String localeCode) {
		// The client already ships en_us. Default exports need no launcher lookup or download.
		if (localeCode.equals("en_us")) return Map.of();
		Path jar = Path.of(assets.getBaseFileName()).toAbsolutePath().normalize();
		return languages.computeIfAbsent(new Language(jar, localeCode), language -> {
			try {
				for (String path : MapLocalization.languagePaths(localeCode)) {
					String key = path.substring("assets/".length());
					byte[] data = resolve(assets, new Language(jar, key));
					if (data.length != 0) return MapLocalization.readMinecraftTable(new ByteArrayInputStream(data), path);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.debug("Interrupted while loading Minecraft language {}", localeCode);
			} catch (IOException | JacksonException e) {
				log.warn("Unable to load Minecraft language {} for {}: {}", localeCode, jar.getFileName(), e.getMessage());
			}
			return Map.of();
		});
	}

	byte[] readAsset(ZipStack assets, String key) {
		Path jar = Path.of(assets.getBaseFileName()).toAbsolutePath().normalize();
		return extraAssets.computeIfAbsent(new Language(jar, key), asset -> {
			try {
				return resolve(assets, asset);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (IOException | JacksonException e) {
				log.debug("Unable to load Minecraft asset {} for {}", key, jar.getFileName(), e);
			}
			return new byte[0];
		});
	}

	private byte[] resolve(ZipStack assets, Language asset) throws IOException, InterruptedException {
		JsonNode index = indexes.get(asset.jar());
		if (index == null) {
			index = resolveIndex(assets, asset.jar());
			indexes.put(asset.jar(), index);
		}
		String hash = index.path("objects").path(asset.key()).path("hash").asString();
		if (!hash.matches("[a-f0-9]{40}")) return new byte[0];
		String object = hash.substring(0, 2) + "/" + hash;
		Path local = launcherDir.resolve("assets/objects").resolve(object);
		if (Files.isRegularFile(local)) {
			try {
				byte[] bytes = Files.readAllBytes(local);
				verifyHash(bytes, hash);
				return bytes;
			} catch (IOException e) {
				log.debug("Unable to read cached Minecraft asset {}", local, e);
			}
		}
		byte[] bytes = download.get(URI.create(OBJECT_SERVER + object));
		verifyHash(bytes, hash);
		return bytes;
	}

	private JsonNode resolveIndex(ZipStack assets, Path jar) throws IOException, InterruptedException {
		String jarName = jar.getFileName().toString();
		String jarId = jarName.replaceFirst("(?i)\\.jar$", "");
		JsonNode metadata = readLocalJson(jar.resolveSibling(jarId + ".json"));
		String version = jarId;
		if (metadata.path("assetIndex").isMissingNode()) {
			try (var stream = assets.getBaseStream("version.json")) {
				if (stream != null) version = FileUtils.getOBJECT_MAPPER().readTree(stream).path("id").asString(jarId);
			}
			if (version.matches("[A-Za-z0-9_.-]+") && !version.equals("..") && !version.equals(".")) {
				Path installedMetadata = launcherDir.resolve("versions").resolve(version).resolve(version + ".json");
				if (!installedMetadata.equals(jar.resolveSibling(jarId + ".json")))
					metadata = readLocalJson(installedMetadata);
			}
		}
		JsonNode indexInfo = metadata.path("assetIndex");
		if (indexInfo.isMissingNode()) {
			if (manifest == null) manifest = readRemoteJson(VERSION_MANIFEST, "");
			for (JsonNode entry : manifest.path("versions")) {
				if (version.equals(entry.path("id").asString())) {
					indexInfo = readRemoteJson(entry.path("url").asString(), entry.path("sha1").asString()).path("assetIndex");
					break;
				}
			}
		}
		String indexId = indexInfo.path("id").asString();
		if (!indexId.matches("[A-Za-z0-9_.-]+")) return FileUtils.getOBJECT_MAPPER().createObjectNode();
		String indexHash = indexInfo.path("sha1").asString();
		JsonNode index = readLocalJson(launcherDir.resolve("assets/indexes").resolve(indexId + ".json"), indexHash);
		if (!index.path("objects").isObject()) {
			index = readRemoteJson(indexInfo.path("url").asString(), indexHash);
		}
		return index;
	}

	private JsonNode readLocalJson(Path path) {
		return readLocalJson(path, "");
	}

	private JsonNode readLocalJson(Path path, String hash) {
		if (Files.isRegularFile(path)) {
			try {
				byte[] bytes = Files.readAllBytes(path);
				if (!hash.isEmpty()) verifyHash(bytes, hash);
				return FileUtils.getOBJECT_MAPPER().readTree(bytes);
			} catch (IOException | JacksonException e) {
				log.debug("Unable to read launcher metadata {}", path, e);
			}
		}
		return FileUtils.getOBJECT_MAPPER().createObjectNode();
	}

	private JsonNode readRemoteJson(String url, String hash) throws IOException, InterruptedException {
		URI uri;
		try {
			uri = URI.create(url);
		} catch (IllegalArgumentException e) {
			throw new IOException("Invalid Minecraft metadata URL", e);
		}
		if (!"https".equals(uri.getScheme()) ||
				!("piston-meta.mojang.com".equals(uri.getHost()) || "launchermeta.mojang.com".equals(uri.getHost())))
			throw new IOException("Expected an official Mojang metadata URL");
		byte[] bytes = download.get(uri);
		if (!hash.isEmpty()) verifyHash(bytes, hash);
		return FileUtils.getOBJECT_MAPPER().readTree(bytes);
	}

	private static void verifyHash(byte[] bytes, String expected) throws IOException {
		try {
			String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
			if (!actual.equals(expected)) throw new IOException("Minecraft asset checksum mismatch");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private record Language(Path jar, String key) {}

	@FunctionalInterface
	interface Download {
		byte[] get(URI uri) throws IOException, InterruptedException;
	}
}
