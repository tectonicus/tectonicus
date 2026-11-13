package tectonicus.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.configuration.Configuration;
import tectonicus.texture.ZipStack;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class InventoryFontTest {
	@TempDir Path directory;

	@Test
	void exportsAccentsAndUnicodeFromFontProvidersAndKeepsOnlyRequestedGlyphs() throws Exception {
		var image = new BufferedImage(16, 8, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(4, 0, -1);
		image.setRGB(10, 1, -1);
		Path jar = directory.resolve("client.jar");
		try (var out = new ZipOutputStream(Files.newOutputStream(jar))) {
			add(out, "assets/minecraft/font/default.json", """
					{"providers":[{"type":"space","advances":{" ":4}},{"type":"reference","id":"minecraft:include/default"},
					              {"type":"reference","id":"minecraft:include/unifont"}]}
					""");
			add(out, "assets/minecraft/font/include/default.json", """
					{"providers":[{"type":"bitmap","file":"minecraft:font/accented.png","ascent":7,"chars":["ãé"]}]}
					""");
			add(out, "assets/minecraft/font/include/unifont.json", "{\"providers\":[]}");
			out.putNextEntry(new ZipEntry("assets/minecraft/textures/font/accented.png"));
			ImageIO.write(image, "png", out);
			out.closeEntry();
		}
		var hex = new ByteArrayOutputStream();
		try (var out = new ZipOutputStream(hex)) {
			add(out, "font.hex", "65E5:" + "FF00".repeat(16) + "\n");
		}
		String definition = """
				{"providers":[{"type":"unihex","hex_file":"minecraft:font/unifont_jp.zip","filter":{"jp":true},
				 "size_overrides":[{"from":"日","to":"日","left":0,"right":15}]}]}
				""";
		String definitionHash = cache(definition.getBytes(StandardCharsets.UTF_8));
		String hexHash = cache(hex.toByteArray());
		Files.writeString(directory.resolve("client.json"), "{\"assetIndex\":{\"id\":\"test\"}}");
		Path indexes = Files.createDirectories(directory.resolve("assets/indexes"));
		Files.writeString(indexes.resolve("test.json"), """
				{"objects":{"minecraft/font/include/unifont.json":{"hash":"%s"},"minecraft/font/unifont_jp.zip":{"hash":"%s"}}}
				""".formatted(definitionHash, hexHash));
		var assets = new ZipStack(jar.toFile(), null, List.of());
		var config = new Configuration();
		config.setMapLocale("ja_jp");
		try {
			var localization = MapLocalization.load(config, new MinecraftLanguageAssets(directory, uri -> fail("Unexpected download: " + uri)));
			Path output = directory.resolve(InventoryFont.IMAGE);
			var metrics = InventoryFont.write(assets, localization, Set.of(32, (int) 'ã', (int) '日'), output);
			assertEquals(Set.of(0, 32, (int) 'ã', (int) '日'), metrics.keySet());
			assertArrayEquals(new double[] {0, 0, 5, 8, 6, 0}, metrics.get((int) 'ã'));
			assertArrayEquals(new double[] {16, 0, 8, 8, 9, 0}, metrics.get((int) '日'));
			var atlas = ImageIO.read(output.toFile());
			assertEquals(-1, atlas.getRGB(8, 0));
			assertEquals(-1, atlas.getRGB(32, 0));
			assertEquals(0, atlas.getRGB(47, 0));
		} finally {
			var field = ZipStack.class.getDeclaredField("base");
			field.setAccessible(true);
			((FileSystem) field.get(assets)).close();
		}
	}

	private String cache(byte[] data) throws Exception {
		String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
		Path prefix = Files.createDirectories(directory.resolve("assets/objects/" + hash.substring(0, 2)));
		Files.write(prefix.resolve(hash), data);
		return hash;
	}

	private static void add(ZipOutputStream out, String path, String content) throws Exception {
		out.putNextEntry(new ZipEntry(path));
		out.write(content.getBytes(StandardCharsets.UTF_8));
		out.closeEntry();
	}
}
