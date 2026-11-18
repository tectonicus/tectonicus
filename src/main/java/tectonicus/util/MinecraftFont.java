/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.util;

import lombok.extern.slf4j.Slf4j;
import tectonicus.texture.ZipStack;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.zip.ZipInputStream;

/** Loads and packs Minecraft glyphs at twice their logical pixel scale. */
@Slf4j
public final class MinecraftFont {
	static final String IMAGE = "InventoryFont.png";
	private static final int CELL_SIZE = 32;
	private static final int COLUMNS = 32;

	private final ZipStack assets;
	private final Locale locale;
	private final Function<String, byte[]> launcherAsset;
	private final Set<Integer> remaining;
	private final Map<Integer, Glyph> glyphs = new TreeMap<>();
	private final Set<String> loaded = new HashSet<>();

	private MinecraftFont(ZipStack assets, Locale locale, Function<String, byte[]> launcherAsset, Set<Integer> characters) {
		this.assets = assets;
		this.locale = locale;
		this.launcherAsset = launcherAsset;
		this.remaining = new HashSet<>(characters);
	}

	static Map<Integer, double[]> write(ZipStack assets, MapLocalization localization, Set<Integer> characters, Path output) {
		MinecraftFont font = new MinecraftFont(assets, localization.locale(), key -> localization.minecraftFontAsset(assets, key), characters);
		font.loadDefinition("minecraft:default");
		Atlas atlas = pack(font.glyphs);
		if (atlas.image() == null) return Map.of();
		try {
			ImageIO.write(atlas.image(), "png", output.toFile());
			atlas.metrics().put(0, new double[] {atlas.image().getWidth() / 2.0, atlas.image().getHeight() / 2.0});
			return atlas.metrics();
		} catch (IOException e) {
			log.warn("Unable to write the inventory font to {}", output, e);
			return Map.of();
		}
	}

	/** Keeps launcher font data cached when the renderer requests more glyphs. */
	public static Function<Set<Integer>, Map<Integer, Glyph>> loader(ZipStack assets, Locale locale) {
		var launcherAssets = new MinecraftLanguageAssets();
		return characters -> {
			MinecraftFont font = new MinecraftFont(assets, locale, key -> launcherAssets.readAsset(assets, key), characters);
			font.loadGlyphs();
			return font.glyphs;
		};
	}

	private void loadGlyphs() {
		loadDefinition("minecraft:default");
		if (assets.hasFile("assets/minecraft/font/default.json")) return;
		loadLegacyBitmap();
		loadLegacyUnicode("minecraft:font/glyph_sizes.bin", "minecraft:font/unicode_page_%s.png");
	}

	private void loadDefinition(String id) {
		String path = resource(id, "font/", ".json");
		if (path == null || remaining.isEmpty() || !loaded.add(path)) return;
		// Font definitions merge: providers from higher-priority resource packs come first.
		for (var layer : assets.getLayers("assets/" + path)) {
			if (remaining.isEmpty()) return;
			try (var in = layer.open("assets/" + path)) {
				JsonNode providers = in == null ? null : FileUtils.getOBJECT_MAPPER().readTree(in).path("providers");
				if (assets.isBase(layer) && (providers == null || providers.isEmpty()) && path.equals("minecraft/font/include/unifont.json")) {
					// Modern clients ship an empty placeholder for this launcher asset.
					byte[] data = launcherAsset.apply(path);
					if (data.length > 0) providers = FileUtils.getOBJECT_MAPPER().readTree(data).path("providers");
				}
				if (providers != null) loadProviders(providers, path);
			} catch (IOException | JacksonException e) {
				log.debug("Unable to read font definition {} from {}", path, layer, e);
			}
		}
	}

	private void loadProviders(JsonNode providers, String path) {
		for (JsonNode provider : providers) {
			if (remaining.isEmpty()) break;
			JsonNode filter = provider.path("filter");
			if (filter.path("uniform").asBoolean() || (filter.has("jp")
					&& filter.path("jp").asBoolean() != locale.getLanguage().equals("ja"))) continue;
			try {
				switch (provider.path("type").asString()) {
					case "reference" -> loadDefinition(provider.path("id").asString());
					case "space" -> loadSpaces(provider.path("advances"));
					case "bitmap" -> loadBitmap(provider);
					case "unihex" -> loadUnihex(provider);
					case "legacy_unicode" -> loadLegacyUnicode(provider.path("sizes").asString(), provider.path("template").asString());
				}
			} catch (IOException | JacksonException e) {
				log.debug("Unable to read a font provider from {}", path, e);
			}
		}
	}

	private void loadSpaces(JsonNode advances) {
		for (var entry : advances.properties()) {
			String character = entry.getKey();
			double advance = entry.getValue().asDouble();
			if (character.codePointCount(0, character.length()) == 1 && advance > 0 && remaining.remove(character.codePointAt(0)))
				glyphs.put(character.codePointAt(0), new Glyph(null, advance, 0));
		}
	}

	private void loadBitmap(JsonNode provider) throws IOException {
		String path = resource(provider.path("file").asString(), "textures/", "");
		JsonNode chars = provider.path("chars");
		if (path == null || !chars.isArray() || chars.isEmpty()) return;
		try (var stream = open(path)) {
			if (stream == null) return;
			BufferedImage sheet = ImageIO.read(stream);
			String firstRow = chars.get(0).asString();
			int columns = firstRow.codePointCount(0, firstRow.length());
			if (sheet == null || columns == 0 || sheet.getWidth() % columns != 0 || sheet.getHeight() % chars.size() != 0) return;
			int width = sheet.getWidth() / columns;
			int height = sheet.getHeight() / chars.size();
			int logicalHeight = provider.path("height").asInt(8);
			int ascent = provider.path("ascent").asInt(7);
			if (logicalHeight < 1 || logicalHeight > CELL_SIZE / 2) return;
			double scale = (double) logicalHeight / height;
			for (int row = 0; row < chars.size(); row++) {
				int[] codes = chars.get(row).asString().codePoints().toArray();
				for (int column = 0; column < Math.min(columns, codes.length); column++) {
					int code = codes[column];
					if (code == 0 || !remaining.contains(code)) continue;
					var cell = sheet.getSubimage(column * width, row * height, width, height);
					int right = rightEdge(cell);
					int pixels = (int) Math.ceil((right + 1) * scale * 2);
					if (pixels > CELL_SIZE) continue;
					if (pixels == 0 && !Character.isSpaceChar(code)) continue;
					BufferedImage image = pixels == 0 ? null : scale(cell.getSubimage(0, 0, right + 1, height), pixels, logicalHeight * 2);
					add(code, new Glyph(image, pixels == 0 ? 4 : Math.round((right + 1) * scale) + 1, 7 - ascent));
				}
			}
		}
	}

	private void loadLegacyBitmap() {
		if (remaining.isEmpty()) return;
		try (var lookup = MinecraftFont.class.getResourceAsStream("/font.txt")) {
			BufferedImage sheet = readFirst("assets/minecraft/textures/font/ascii.png", "font/default.png", "default.png");
			if (sheet == null || lookup == null || sheet.getWidth() % 16 != 0 || sheet.getHeight() % 16 != 0) return;
			int width = sheet.getWidth() / 16;
			int height = sheet.getHeight() / 16;
			float scale = 8f / height;
			try (var reader = new BufferedReader(new InputStreamReader(lookup, StandardCharsets.UTF_8))) {
				int row = 2;
				for (String line; row < 16 && (line = reader.readLine()) != null; ) {
					if (line.isEmpty() || line.startsWith("#")) continue;
					for (int column = 0; column < Math.min(line.length(), 16); column++) {
						int code = row == 6 && column == 0 ? '`' : line.charAt(column);
						if (!remaining.contains(code)) continue;
						setLegacyBitmapGlyph(code, sheet.getSubimage(column * width, row * height, width, height), scale);
					}
					row++;
				}
			}
		} catch (IOException e) {
			log.debug("Unable to read the legacy font bitmap", e);
		}
	}

	private void setLegacyBitmapGlyph(int code, BufferedImage cell, float scale) {
		if (code == ' ') {
			add(code, new Glyph(null, 4, 0));
			return;
		}
		int width = rightEdge(cell) + 1;
		if (width > 0 && width * scale <= CELL_SIZE / 2f) {
			BufferedImage image = scale(cell.getSubimage(0, 0, width, cell.getHeight()), (int) Math.ceil(width * scale * 2), 16);
			add(code, new Glyph(image, Math.round(width * scale) + 1, 0));
		}
	}

	private void loadLegacyUnicode(String sizesId, String template) {
		String sizes = resource(sizesId, "", "");
		if (sizes == null || remaining.isEmpty() || !template.contains("%s")) return;
		try (var stream = assets.getStream(assets.hasFile("assets/" + sizes) ? "assets/" + sizes
				: sizes.substring(sizes.indexOf('/') + 1))) {
			if (stream == null) return;
			byte[] widths = stream.readNBytes(65536);
			Map<Integer, BufferedImage> pages = new HashMap<>();
			for (int code : Set.copyOf(remaining)) {
				if (code >= widths.length || widths[code] == 0) continue;
				int page = code >> 8;
				if (!pages.containsKey(page)) {
					String path = resource(template.replace("%s", String.format(Locale.ROOT, "%02x", page)), "textures/", "");
					if (path == null) continue;
					pages.put(page, readFirst("assets/" + path, path.substring(path.indexOf("textures/") + "textures/".length())));
				}
				BufferedImage image = pages.get(page);
				if (image == null || image.getWidth() != 256 || image.getHeight() != 256) continue;
				int left = (widths[code] & 0xff) >> 4;
				int right = widths[code] & 0xf;
				if (right < left) continue;
				BufferedImage glyph = image.getSubimage((code & 0xf) * 16 + left, ((code >> 4) & 0xf) * 16, right - left + 1, 16);
				add(code, new Glyph(glyph, glyph.getWidth() / 2.0 + 1, 0));
			}
		} catch (IOException e) {
			log.debug("Unable to read legacy Unicode font {}", sizes, e);
		}
	}

	private BufferedImage readFirst(String... paths) throws IOException {
		for (String path : paths) {
			try (var stream = assets.getStream(path)) {
				if (stream != null) return ImageIO.read(stream);
			}
		}
		return null;
	}

	private void loadUnihex(JsonNode provider) throws IOException {
		String path = resource(provider.path("hex_file").asString(), "", "");
		if (path == null || remaining.isEmpty()) return;
		try (var stream = open(path)) {
			byte[] asset = stream == null ? launcherAsset.apply(path) : null;
			if (asset != null && asset.length == 0) return;
			try (var zip = new ZipInputStream(stream == null ? new ByteArrayInputStream(asset) : stream)) {
				for (var entry = zip.getNextEntry(); entry != null && !remaining.isEmpty(); entry = zip.getNextEntry()) {
					if (!entry.getName().endsWith(".hex")) continue;
					var reader = new BufferedReader(new InputStreamReader(zip, StandardCharsets.US_ASCII));
					for (String line; !remaining.isEmpty() && (line = reader.readLine()) != null; ) {
						int colon = line.indexOf(':');
						int length = line.length() - colon - 1;
						if (colon < 4 || colon > 6 || length < 32 || length > 128 || length % 32 != 0) continue;
						try {
							int code = Integer.parseInt(line, 0, colon, 16);
							if (remaining.contains(code)) addHexGlyph(code, line, colon + 1, length / 16, provider.path("size_overrides"));
						} catch (NumberFormatException ignored) {
							// Skip malformed rows in optional font assets.
						}
					}
				}
			}
		}
	}

	private void addHexGlyph(int code, String line, int offset, int digits, JsonNode overrides) {
		int width = digits * 4;
		int[] rows = new int[16];
		int mask = 0;
		for (int i = 0; i < rows.length; i++) {
			rows[i] = Integer.parseUnsignedInt(line, offset + i * digits, offset + (i + 1) * digits, 16);
			mask |= rows[i];
		}
		if (mask == 0 && !Character.isSpaceChar(code)) return;
		int left = mask == 0 ? 0 : Integer.numberOfLeadingZeros(mask) - (32 - width);
		int right = mask == 0 ? width - 1 : width - 1 - Integer.numberOfTrailingZeros(mask);
		for (JsonNode override : overrides) {
			String from = override.path("from").asString();
			String to = override.path("to").asString();
			if (!from.isEmpty() && !to.isEmpty() && code >= from.codePointAt(0) && code <= to.codePointAt(0)) {
				left = override.path("left").asInt(left);
				right = override.path("right").asInt(right);
				break;
			}
		}
		if (left < 0 || right >= width || right < left) return;
		var image = new BufferedImage(right - left + 1, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < rows.length; y++) {
			for (int x = left; x <= right; x++) {
				if ((rows[y] & (1 << (width - 1 - x))) != 0) image.setRGB(x - left, y, -1);
			}
		}
		add(code, new Glyph(image, image.getWidth() / 2.0 + 1, 0));
	}

	private void add(int code, Glyph glyph) {
		remaining.remove(code);
		glyphs.put(code, glyph);
	}

	public static Atlas pack(Map<Integer, Glyph> glyphs) {
		long images = glyphs.values().stream().filter(glyph -> glyph.image() != null).count();
		if (images == 0) {
			Map<Integer, double[]> spaces = new TreeMap<>();
			glyphs.forEach((code, glyph) -> spaces.put(code, new double[] {0, 0, 0, 0, glyph.advance(), 0}));
			return new Atlas(null, spaces);
		}
		int width = (int) Math.min(images, COLUMNS) * CELL_SIZE;
		int height = (int) ((images + COLUMNS - 1) / COLUMNS) * CELL_SIZE;
		BufferedImage atlas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Map<Integer, double[]> metrics = new TreeMap<>();
		var graphics = atlas.createGraphics();
		try {
			int index = 0;
			for (var entry : glyphs.entrySet()) {
				Glyph glyph = entry.getValue();
				BufferedImage image = glyph.image();
				if (image == null) {
					metrics.put(entry.getKey(), new double[] {0, 0, 0, 0, glyph.advance(), 0});
					continue;
				}
				int x = index % COLUMNS * CELL_SIZE;
				int y = index / COLUMNS * CELL_SIZE;
				index++;
				graphics.drawImage(image, x, y, null);
				// Coordinates, size, advance and baseline offset in CSS pixels.
				metrics.put(entry.getKey(), new double[] {x / 2.0, y / 2.0, image.getWidth() / 2.0, image.getHeight() / 2.0, glyph.advance(), glyph.top()});
			}
		} finally {
			graphics.dispose();
		}
		return new Atlas(atlas, metrics);
	}

	private static BufferedImage scale(BufferedImage image, int width, int height) {
		var result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		var graphics = result.createGraphics();
		try {
			graphics.drawImage(image, 0, 0, width, height, null);
		} finally {
			graphics.dispose();
		}
		return result;
	}

	private static int rightEdge(BufferedImage image) {
		for (int x = image.getWidth() - 1; x >= 0; x--) {
			for (int y = 0; y < image.getHeight(); y++) if ((image.getRGB(x, y) >>> 24) != 0) return x;
		}
		return -1;
	}

	private InputStream open(String path) throws IOException {
		String resource = "assets/" + path;
		var layers = assets.getLayers(resource);
		return layers.isEmpty() ? null : layers.getFirst().open(resource);
	}

	private static String resource(String id, String prefix, String suffix) {
		String[] parts = id.split(":", -1);
		String namespace = parts.length == 1 ? "minecraft" : parts[0];
		String name = parts[parts.length - 1];
		if (parts.length > 2 || !namespace.matches("[a-z0-9_.-]+") || !name.matches("[a-z0-9_./-]+")
				|| namespace.equals("..") || name.contains("..") || name.startsWith("/")) return null;
		return namespace + "/" + prefix + name + suffix;
	}

	public record Atlas(BufferedImage image, Map<Integer, double[]> metrics) {}
	public record Glyph(BufferedImage image, double advance, int top) {}
}
