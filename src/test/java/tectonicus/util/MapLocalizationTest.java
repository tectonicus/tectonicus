package tectonicus.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import picocli.CommandLine;
import tectonicus.configuration.Configuration;
import tectonicus.configuration.XmlConfigurationParser;
import tectonicus.texture.ZipStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class MapLocalizationTest {
	@TempDir Path directory;

	@Test
	void readsLanguageCatalogFromLauncherOrClientJar() throws Exception {
		byte[] catalog = """
				{"language":{"pt_br":{"name":"Português","region":"Brasil"},
				             "ja_jp":{"name":"日本語","region":"日本"},
				             "en_us":{"name":"English","region":"US"}}}
				""".getBytes(StandardCharsets.UTF_8);
		String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(catalog));
		Path client = zip("client.jar", "en_us.json", "{}");
		Files.writeString(directory.resolve("client.json"), "{\"assetIndex\":{\"id\":\"34\"}}");
		Path indexes = Files.createDirectories(directory.resolve("assets/indexes"));
		Files.writeString(indexes.resolve("34.json"), "{\"objects\":{\"pack.mcmeta\":{\"hash\":\"" + hash + "\"}}}");
		Path objects = Files.createDirectories(directory.resolve("assets/objects/" + hash.substring(0, 2)));
		Files.write(objects.resolve(hash), catalog);

		Path olderClient = directory.resolve("older.jar");
		try (var out = new ZipOutputStream(Files.newOutputStream(olderClient))) {
			out.putNextEntry(new ZipEntry("pack.mcmeta"));
			out.write(catalog);
		}
		Configuration config = new Configuration();
		new CommandLine(config).parseArgs("--listLanguages");
		assertTrue(config.isListLanguages());
		var localization = MapLocalization.load(config, new MinecraftLanguageAssets(directory, uri -> fail("Unexpected download " + uri)));
		for (Path jar : List.of(client, olderClient)) {
			try (var assets = new ZipStack(jar.toFile(), null, List.of())) {
				var names = localization.languageNames(assets);
				assertEquals(List.of("en_us", "ja_jp", "pt_br"), new ArrayList<>(names.keySet()));
				assertEquals("Português (Brasil)", names.get("pt_br"));
				assertEquals("日本語 (日本)", names.get("ja_jp"));
			}
		}
	}

	@Test
	void combinesSourcesPerKeyAndExportsOnlyUsedKeys() throws Exception {
		Path jar = zip("client.jar", "en_us.json", """
				{"item.minecraft.apple":"Apple","container.inventory":"Inventory","container.enderchest":"Ender Chest",
				 "unused":"Not exported"}
				""");
		Path pack = zip("pack.zip", "ja_jp.json", """
				{"item.minecraft.apple":"リンゴ","container.inventory":"持ち物","container.enderchest":27}
				""");
		Path file = Files.writeString(directory.resolve("locale.json"), """
				{"item.minecraft.apple":"青リンゴ","tectonicus.title":"地図","unused":"Not exported"}
				""");
		Configuration config = new Configuration();
		config.setMapLocale("ja_jp");
		config.setMapLocaleFile(file.toFile());
		byte[] language = "{\"item.minecraft.apple\":\"リンゴ\"}".getBytes(StandardCharsets.UTF_8);
		String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(language));
		String index = "{\"objects\":{\"minecraft/lang/ja_jp.json\":{\"hash\":\"" + hash + "\"}}}";
		String indexUrl = "https://piston-meta.mojang.com/v1/packages/test/34.json";
		Files.writeString(directory.resolve("client.json"), """
				{"assetIndex":{"id":"34","url":"%s"}}
				""".formatted(indexUrl));
		Path objects = Files.createDirectories(directory.resolve("assets/objects/" + hash.substring(0, 2)));
		Path indexFile = Files.createDirectories(directory.resolve("assets/indexes")).resolve("34.json");
		Files.writeString(indexFile, index);
		Path cachedLanguage = Files.write(objects.resolve(hash), language);
		ZipStack zip = new ZipStack(jar.toFile(), pack.toFile(), List.of());
		try {
			var assets = new WorldViewerAssets();
			assets.addItem("minecraft:apple", null, false, null, null, null);
			var cached = new MinecraftLanguageAssets(directory, uri -> fail("Unexpected network request: " + uri));
			assets.write(zip, pack.toFile(), MapLocalization.load(config, cached), "World0", directory, directory, false);
			String script = Files.readString(directory.resolve("localizations.js"));
			assertTrue(script.contains("リンゴ"));
			assertFalse(script.contains("青リンゴ"));
			assertTrue(script.contains("持ち物"));
			assertTrue(script.contains("Ender Chest"));
			assertTrue(script.contains("地図"));
			assertFalse(script.contains("unused"));
			assertEquals("Layers", MapLocalization.english().get("tectonicus.controls.layers"));

			Files.writeString(cachedLanguage, "corrupt cache entry");
			Files.delete(indexFile);
			var requests = new ArrayList<String>();
			var remote = new MinecraftLanguageAssets(directory, uri -> {
				requests.add(uri.toString());
				return uri.toString().equals(indexUrl) ? index.getBytes(StandardCharsets.UTF_8) : language;
			});
			var localization = MapLocalization.load(config, remote);
			assets.write(zip, pack.toFile(), localization, "World0", directory, directory, false);
			assertTrue(Files.readString(directory.resolve("localizations.js")).contains("リンゴ"));
			assets.write(zip, pack.toFile(), localization, "World1", directory, directory, false);
			assertEquals(List.of(indexUrl, "https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash), requests);

			var offline = new MinecraftLanguageAssets(directory, uri -> { throw new IOException("Offline"); });
			assets.write(zip, pack.toFile(), MapLocalization.load(config, offline), "World0", directory, directory, false);
			assertTrue(Files.readString(directory.resolve("localizations.js")).contains("青リンゴ"));

			Files.delete(directory.resolve("client.json"));
			String versionUrl = "https://piston-meta.mojang.com/v1/packages/test/client.json";
			var noMetadata = new MinecraftLanguageAssets(directory, uri -> switch (uri.getPath()) {
				case "/mc/game/version_manifest_v2.json" ->
						("{\"versions\":[{\"id\":\"client\",\"url\":\"" + versionUrl + "\"}]}").getBytes(StandardCharsets.UTF_8);
				case "/v1/packages/test/client.json" ->
						("{\"assetIndex\":{\"id\":\"34\",\"url\":\"" + indexUrl + "\"}}").getBytes(StandardCharsets.UTF_8);
				case "/v1/packages/test/34.json" -> index.getBytes(StandardCharsets.UTF_8);
				default -> {
					assertEquals("resources.download.minecraft.net", uri.getHost());
					yield language;
				}
			});
			assertEquals("リンゴ", MapLocalization.load(config, noMetadata).minecraftTranslations(zip).get("item.minecraft.apple"));
		} finally {
			for (String name : List.of("base", "override")) {
				var field = ZipStack.class.getDeclaredField(name);
				field.setAccessible(true);
				((FileSystem) field.get(zip)).close();
			}
		}
	}

	@ParameterizedTest
	@CsvSource({"SAH_SAH,sah_sah", "cbk-zam,cbk_zam", "enws,enws"})
	void usesMinecraftLocaleCodeForAssetLookup(String configuredLocale, String assetCode) throws Exception {
		Path jar = zip("client.jar", "en_us.json", "{}");
		writeLauncherLanguage("minecraft/lang/" + assetCode + ".json", """
				{"container.chest":"Localized chest"}
				""");
		Configuration config = new Configuration();
		config.setMapLocale(configuredLocale);
		try (var assets = new ZipStack(jar.toFile(), null, List.of())) {
			var localization = MapLocalization.load(config, new MinecraftLanguageAssets(directory, uri -> fail("Unexpected download " + uri)));
			assertEquals("Localized chest", localization.minecraftTranslations(assets).get("container.chest"));
		}
	}

	@Test
	void exportsLegacyLangAssetsFromLauncherClientAndResourcePack() throws Exception {
		Path jar = zip("client.jar", "en_us.lang", "container.enderchest=Ender Chest\n");
		Path pack = zip("pack.zip", "fr_fr.lang", "container.chestDouble=Grand coffre\n");
		writeLauncherLanguage("minecraft/lang/fr_fr.lang", """
				# comment=ignored
				invalid line
				container.chest=Coffre spécial=1
				""");
		Configuration config = new Configuration();
		config.setMapLocale("fr_fr");
		try (var assets = new ZipStack(jar.toFile(), pack.toFile(), List.of())) {
			var localization = MapLocalization.load(config, new MinecraftLanguageAssets(directory, uri -> fail("Unexpected download " + uri)));
			assertEquals(Map.of("container.chest", "Coffre spécial=1"), localization.minecraftTranslations(assets));
			new WorldViewerAssets().write(assets, pack.toFile(), localization, "World0", directory, directory, false);
			String script = Files.readString(directory.resolve("localizations.js"));
			assertTrue(script.contains("Coffre spécial=1"));
			assertTrue(script.contains("Grand coffre"));
			assertTrue(script.contains("Ender Chest"));
		}
	}

	@Test
	void parsesXmlAndCliAndWritesPageLanguageAndCustomTitle() throws Exception {
		Path file = Files.writeString(directory.resolve("settings.xml"), """
				<tectonicus version="2">
				    <config mapLocale="fr_fr" mapLocaleFile="fr.json" htmlTitle="A &amp; B"/>
				</tectonicus>
				""");
		Configuration config = XmlConfigurationParser.parseConfiguration(file.toFile());
		assertEquals(directory.resolve("fr.json"), config.getMapLocaleFile().toPath());
		assertEquals("fr-FR", MapLocalization.parseLocale(config.getMapLocale()).toLanguageTag());
		new CommandLine(config).parseArgs("--mapLocale", "ja-JP", "--mapLocaleFile", "ja.json");
		assertEquals(Path.of("ja.json"), config.getMapLocaleFile().toPath());
		String html = Files.readString(OutputResourcesUtil.outputHtml(directory.toFile(), config).toPath());
		assertTrue(html.contains("<html lang=\"ja-JP\">"));
		assertTrue(html.contains("<title>A &amp; B</title>"));
	}

	@Test
	void rejectsInvalidExplicitTranslationFiles() throws IOException {
		Configuration config = new Configuration();
		assertEquals("en-US", MapLocalization.load(config).locale().toLanguageTag());
		Path file = directory.resolve("invalid.json");
		config.setMapLocaleFile(file.toFile());
		assertThrows(IOException.class, () -> MapLocalization.load(config));
		for (String contents : List.of("", "{", "[]", "{\"key\":3}", "{} {}")) {
			Files.writeString(file, contents);
			assertThrows(IOException.class, () -> MapLocalization.load(config));
		}
		config.setMapLocale("../en_us");
		assertThrows(IllegalArgumentException.class, () -> MapLocalization.load(config));
	}

	private void writeLauncherLanguage(String key, String translations) throws Exception {
		byte[] data = translations.getBytes(StandardCharsets.UTF_8);
		String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
		Files.writeString(directory.resolve("client.json"), "{\"assetIndex\":{\"id\":\"test\"}}");
		Path indexes = Files.createDirectories(directory.resolve("assets/indexes"));
		Files.writeString(indexes.resolve("test.json"), """
				{"objects":{"%s":{"hash":"%s"}}}
				""".formatted(key, hash));
		Path objects = Files.createDirectories(directory.resolve("assets/objects/" + hash.substring(0, 2)));
		Files.write(objects.resolve(hash), data);
	}

	private Path zip(String filename, String languageFile, String translations) throws IOException {
		Path file = directory.resolve(filename);
		try (var out = new ZipOutputStream(Files.newOutputStream(file))) {
			out.putNextEntry(new ZipEntry("assets/minecraft/lang/" + languageFile));
			out.write(translations.getBytes(StandardCharsets.UTF_8));
		}
		return file;
	}
}
