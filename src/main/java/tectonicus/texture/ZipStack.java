/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */

package tectonicus.texture;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tectonicus.util.FileUtils;
import tools.jackson.core.JacksonException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * The Minecraft jar with any number of resource packs stacked on top, resolved like Minecraft's resource manager:
 * the highest-priority pack that contains a file wins, and a pack's filter hides files in the layers below it.
 */
@Slf4j
public class ZipStack implements AutoCloseable
{
	@Getter
	private final List<File> modJars;
	private final String baseFileName;
	private final ResourcePack base;
	/** Resource packs, highest priority first, as in Minecraft's pack selection screen. */
	private final List<ResourcePack> packs = new ArrayList<>();
	/** Resource packs followed by the Minecraft jar. */
	private final List<ResourcePack> layers = new ArrayList<>();

	/**
	 * @param baseFile the Minecraft jar
	 * @param resourcePacks resource pack zips or folders, highest priority first; may be null
	 * @param modJars mod jars searched after the Minecraft jar; may be null
	 */
	public ZipStack(File baseFile, List<File> resourcePacks, List<File> modJars) throws IOException
	{
		baseFileName = baseFile.getPath();
		base = ResourcePack.openJar(baseFile);
		this.modJars = modJars == null ? List.of() : modJars;

		try {
			int[] gameFormat = resourcePackFormat();
			for (File pack : resourcePacks == null ? List.<File>of() : resourcePacks) {
				if (pack == null)
					continue;
				if (pack.exists())
					packs.add(ResourcePack.openPack(pack, gameFormat));
				else
					log.error("Couldn't open {}", pack.getAbsolutePath());
			}
		} catch (IOException | RuntimeException e) {
			close();
			throw e;
		}
		layers.addAll(packs);
		layers.add(base);
	}

	/** The jar's resource pack format as {major, minor}, used to choose pack overlays, or null if unknown. */
	private int[] resourcePackFormat() {
		try (InputStream in = base.open("version.json")) {
			if (in != null) {
				VersionJson.PackVersion version = FileUtils.getOBJECT_MAPPER().readerFor(VersionJson.class)
						.<VersionJson>readValue(in).getPackVersion();
				if (version != null && version.getResource() > 0)
					return new int[] {version.getResource(), version.getResourceMinor()};
			}
		} catch (IOException | JacksonException e) {
			log.debug("Unable to read version.json from {}", baseFileName, e);
		}
		try (InputStream in = base.open("pack.mcmeta")) {
			if (in != null) {
				int format = FileUtils.getOBJECT_MAPPER().readerFor(PackMcmeta.class).<PackMcmeta>readValue(in).getPack().getPackVersion();
				if (format > 0)
					return new int[] {format, 0};
			}
		} catch (IOException | JacksonException e) {
			log.debug("Unable to read pack.mcmeta from {}", baseFileName, e);
		}
		return null;
	}

	/** The resource packs in use, highest priority first. */
	public List<ResourcePack> getResourcePacks() {
		return List.copyOf(packs);
	}

	/** The resource pack files in use, highest priority first. */
	public List<File> getResourcePackFiles() {
		return packs.stream().map(ResourcePack::getFile).toList();
	}

	/**
	 * The layers that provide a file, highest priority first, ending with the Minecraft jar if it has the file.
	 * A pack whose filter matches the file hides it in every layer below that pack.
	 */
	public List<ResourcePack> getLayers(String path) {
		List<ResourcePack> result = new ArrayList<>();
		for (ResourcePack layer : layers) {
			if (layer.has(path))
				result.add(layer);
			if (layer.hides(path))
				break;
		}
		return result;
	}

	/** True for the layer holding the Minecraft jar. */
	public boolean isBase(ResourcePack layer) {
		return layer == base;
	}

	private ResourcePack top(String path) {
		return top(path, ResourcePack::has);
	}

	private ResourcePack top(String path, BiPredicate<ResourcePack, String> hasPath) {
		for (ResourcePack layer : layers) {
			if (hasPath.test(layer, path))
				return layer;
			if (layer.hides(path))
				return null;
		}
		return null;
	}

	public InputStream getStream(String path) throws IOException {
		return getStream(path, true);
	}

	public InputStream getBaseStream(String path) throws IOException {
		return base.open(path);
	}

	public boolean hasBaseFile(String path) {
		return base.has(path);
	}

	/** Open a file that belongs with another one, such as a texture's .mcmeta, from the layer that provides that one. */
	public InputStream getSiblingStream(String path, String siblingPath) throws IOException {
		ResourcePack layer = top(path);
		return layer == null ? null : layer.open(siblingPath);
	}

	@Override
	public void close() throws IOException {
		IOException failure = null;
		for (ResourcePack pack : packs) {
			try {
				pack.close();
			} catch (IOException e) {
				failure = e;
			}
		}
		base.close();
		if (failure != null)
			throw failure;
	}

	/** With minecraftJarLoaded false, resource packs are skipped and only the Minecraft jar and mods are read. */
	public InputStream getStream(String path, boolean minecraftJarLoaded) throws IOException
	{
		ResourcePack layer = minecraftJarLoaded ? top(path) : (base.has(path) ? base : null);
		if (layer != null)
			return layer.open(path);

		for (File jar : modJars)
		{
			FileSystem fs = FileSystems.newFileSystem(Paths.get(jar.getPath()));
			if (Files.exists(fs.getPath(path)))
				return Files.newInputStream(fs.getPath(path));
		}

		return null;
	}

	/** True when a resource pack, rather than the Minecraft jar, provides the file. */
	public boolean hasPackFile(String file) {
		ResourcePack layer = top(file);
		return layer != null && layer != base;
	}

	public boolean hasFile(String file)
	{
		return top(file) != null;  //TODO: Maybe need to check mod jar files too?
	}

	boolean hasDirectory(String path) {
		return top(path, ResourcePack::hasDirectory) != null;
	}

	public String getBaseFileName()
	{
		return baseFileName;
	}

	/** The paths of the entries in a directory across all layers, honoring pack filters. */
	public List<String> listFilesInDirectory(String directoryPath) throws IOException {
		String prefix = directoryPath.endsWith("/") ? directoryPath : directoryPath + "/";
		Set<String> files = new LinkedHashSet<>();
		List<ResourcePack> above = new ArrayList<>();
		for (ResourcePack layer : layers) {
			for (String name : layer.list(directoryPath)) {
				String path = prefix + name;
				if (above.stream().noneMatch(pack -> pack.hides(path)))
					files.add(path);
			}
			above.add(layer);
		}
		return new ArrayList<>(files);
	}
}
