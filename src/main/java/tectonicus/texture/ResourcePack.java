/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */

package tectonicus.texture;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tectonicus.util.FileUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * One layer of a {@link ZipStack}: the Minecraft jar or a resource pack, either a zip or a folder.
 * A pack's {@code pack.mcmeta} may add overlay folders that sit on top of the pack's root and a
 * filter that hides matching files in the layers below the pack.
 */
@Slf4j
public final class ResourcePack implements AutoCloseable {
	private static final Pattern OVERLAY_DIRECTORY = Pattern.compile("[a-z0-9_.-]+");
	private static final String PACK_METADATA = "pack.mcmeta";

	@Getter
	private final File file;
	private final FileSystem fileSystem;
	/** Active overlays, highest priority first, followed by the pack root. */
	private final List<Path> roots;
	private final List<Filter> filters;

	private ResourcePack(File file, FileSystem fileSystem, List<Path> roots, List<Filter> filters) {
		this.file = file;
		this.fileSystem = fileSystem;
		this.roots = roots;
		this.filters = filters;
	}

	/** Open the Minecraft jar. Its {@code pack.mcmeta} describes the game, not a pack, so it is not interpreted. */
	static ResourcePack openJar(File jar) throws IOException {
		FileSystem fs = FileSystems.newFileSystem(jar.toPath());
		return new ResourcePack(jar, fs, List.of(fs.getPath("/")), List.of());
	}

	/**
	 * Open a resource pack zip or folder.
	 *
	 * @param gameFormat the resource pack format of the Minecraft jar, as {major, minor}, or null if unknown
	 */
	static ResourcePack openPack(File pack, int[] gameFormat) throws IOException {
		FileSystem fs = null;
		Path root;
		if (pack.isDirectory()) {
			root = pack.toPath();
		} else {
			fs = FileSystems.newFileSystem(pack.toPath());
			root = fs.getPath("/");
		}

		JsonNode metadata = null;
		Path mcmeta = root.resolve(PACK_METADATA);
		if (Files.isRegularFile(mcmeta)) {
			try (InputStream in = Files.newInputStream(mcmeta)) {
				metadata = FileUtils.getOBJECT_MAPPER().readTree(in);
			} catch (IOException | JacksonException e) {
				log.warn("Unable to read pack.mcmeta in {}", pack, e);
			}
		}

		List<Path> roots = new ArrayList<>();
		if (metadata != null) {
			roots.addAll(activeOverlays(pack, root, metadata.path("overlays").path("entries"), gameFormat));
			Collections.reverse(roots); // Later overlay entries take priority over earlier ones
		}
		roots.add(root);
		List<Filter> filters = metadata == null ? List.of() : parseFilters(pack, metadata.path("filter").path("block"));
		return new ResourcePack(pack, fs, List.copyOf(roots), filters);
	}

	private static List<Path> activeOverlays(File pack, Path root, JsonNode entries, int[] gameFormat) {
		List<Path> overlays = new ArrayList<>();
		if (!entries.isArray() || gameFormat == null)
			return overlays;
		for (JsonNode entry : entries) {
			String directory = entry.path("directory").asString("");
			if (!OVERLAY_DIRECTORY.matcher(directory).matches()) {
				log.warn("Ignoring overlay with invalid directory '{}' in {}", directory, pack);
				continue;
			}
			int[][] range = overlayRange(entry);
			if (range == null) {
				log.warn("Ignoring overlay '{}' with a missing or invalid format range in {}", directory, pack);
				continue;
			}
			if (compare(gameFormat, range[0]) >= 0 && compare(gameFormat, range[1]) <= 0) {
				Path overlay = root.resolve(directory);
				if (Files.isDirectory(overlay)) {
					log.debug("Using overlay '{}' from {}", directory, pack);
					overlays.add(overlay);
				}
			}
		}
		return overlays;
	}

	/** Returns {min, max} as {major, minor} pairs, or null if the range is missing or invalid. */
	static int[][] overlayRange(JsonNode entry) {
		JsonNode minFormat = entry.path("min_format");
		JsonNode maxFormat = entry.path("max_format");
		int[] min;
		int[] max;
		if (!minFormat.isMissingNode() || !maxFormat.isMissingNode()) { // 1.21.9+
			min = minFormat.isMissingNode() ? new int[] {0, 0} : formatVersion(minFormat, 0);
			max = maxFormat.isMissingNode() ? new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE} : formatVersion(maxFormat, Integer.MAX_VALUE);
		} else {
			JsonNode formats = entry.path("formats"); // 1.20.2 to 1.21.8
			minFormat = formats;
			maxFormat = formats;
			if (formats.isArray() && formats.size() == 2) {
				minFormat = formats.get(0);
				maxFormat = formats.get(1);
			} else if (formats.isObject()) {
				minFormat = formats.path("min_inclusive");
				maxFormat = formats.path("max_inclusive");
			}
			min = majorVersion(minFormat, 0);
			max = majorVersion(maxFormat, Integer.MAX_VALUE);
		}
		return min != null && max != null && compare(min, max) <= 0 ? new int[][] {min, max} : null;
	}

	private static int[] formatVersion(JsonNode node, int defaultMinor) {
		if (!node.isArray())
			return majorVersion(node, defaultMinor);
		if (node.size() == 1)
			return majorVersion(node.get(0), defaultMinor);
		if (node.size() == 2 && isFormatNumber(node.get(1)))
			return majorVersion(node.get(0), node.get(1).asInt());
		return null;
	}

	private static int[] majorVersion(JsonNode major, int minor) {
		return isFormatNumber(major) ? new int[] {major.asInt(), minor} : null;
	}

	private static boolean isFormatNumber(JsonNode node) {
		return node.isInt() && node.asInt() >= 0;
	}

	private static int compare(int[] a, int[] b) {
		int major = Integer.compare(a[0], b[0]);
		return major != 0 ? major : Integer.compare(a[1], b[1]);
	}

	private static List<Filter> parseFilters(File pack, JsonNode block) {
		if (!block.isArray())
			return List.of();
		List<Filter> filters = new ArrayList<>();
		for (JsonNode entry : block) {
			try {
				filters.add(new Filter(pattern(entry.get("namespace")), pattern(entry.get("path"))));
			} catch (PatternSyntaxException e) {
				log.warn("Ignoring invalid filter pattern in {}", pack, e);
			}
		}
		return List.copyOf(filters);
	}

	private static Predicate<String> pattern(JsonNode node) {
		// Like Minecraft, a pattern matches when it is found anywhere in the namespace or path.
		return node == null || !node.isString() ? value -> true : Pattern.compile(node.asString()).asPredicate();
	}

	/** Strip a leading slash so paths resolve against the pack root, including folder packs. */
	static String normalize(String path) {
		String result = path.replace('\\', '/');
		while (result.startsWith("/"))
			result = result.substring(1);
		return result;
	}

	private static boolean isPackRootFile(Path relative) {
		if (relative.getNameCount() != 1)
			return false;
		FileSystem fs = relative.getFileSystem();
		return relative.equals(fs.getPath(PACK_METADATA)) || relative.equals(fs.getPath("pack.png"));
	}

	private Path find(String path, Predicate<Path> matches) {
		Path packRoot = roots.getLast();
		Path relative = packRoot.getFileSystem().getPath(normalize(path));
		List<Path> searchRoots = roots.size() > 1 && isPackRootFile(relative.normalize()) ? List.of(packRoot) : roots;
		for (Path root : searchRoots) {
			Path candidate = root.resolve(relative);
			if (matches.test(candidate))
				return candidate;
		}
		return null;
	}

	public boolean has(String path) {
		return find(path, Files::isRegularFile) != null;
	}

	boolean hasDirectory(String path) {
		return find(path, Files::isDirectory) != null;
	}

	/** Returns null when this layer does not contain the file. */
	public InputStream open(String path) throws IOException {
		Path found = find(path, Files::isRegularFile);
		return found == null ? null : Files.newInputStream(found);
	}

	/** The names of the entries in a directory, across the pack root and its active overlays. */
	public Set<String> list(String directory) throws IOException {
		Set<String> names = new LinkedHashSet<>();
		String relative = normalize(directory);
		for (Path root : roots) {
			Path dir = root.resolve(relative);
			if (!Files.isDirectory(dir))
				continue;
			boolean overlayRoot = !root.equals(roots.getLast()) && dir.normalize().equals(root.normalize());
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(dir)) {
				for (Path entry : entries) {
					Path fileName = entry.getFileName();
					if (overlayRoot && isPackRootFile(fileName))
						continue;
					String name = fileName.toString();
					names.add(name.endsWith("/") ? name.substring(0, name.length() - 1) : name);
				}
			}
		}
		return names;
	}

	/** True when this pack's filter hides the file in the layers below it. */
	public boolean hides(String path) {
		if (filters.isEmpty())
			return false;
		String relative = normalize(path);
		if (!relative.startsWith("assets/"))
			return false;
		int separator = relative.indexOf('/', "assets/".length());
		if (separator < 0)
			return false;
		String namespace = relative.substring("assets/".length(), separator);
		String resource = relative.substring(separator + 1);
		return filters.stream().anyMatch(filter -> filter.namespace().test(namespace) && filter.path().test(resource));
	}

	@Override
	public void close() throws IOException {
		if (fileSystem != null)
			fileSystem.close();
	}

	@Override
	public String toString() {
		return file.getPath();
	}

	private record Filter(Predicate<String> namespace, Predicate<String> path) {}
}
