package tectonicus.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;
import ch.qos.logback.classic.Level;
import tectonicus.util.DirUtils;
import tectonicus.util.OutputResourcesUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationHierarchyTest {
	@TempDir
	Path directory;

	@Test
	void defaultsNamesFromWorldMetadataDimensionAndLayerIdsWithoutInheritingNames() throws Exception {
		Path save = Files.createDirectories(directory.resolve("Save folder"));
		Files.copy(Path.of("src/test/resources/level1_12.dat"), save.resolve("level.dat"));
		Configuration config = parse("""
				<config name="Not inherited"/>
				<world worldDir="%s">
				    <dimension><layer/><layer lighting="night"/></dimension>
				    <dimension id="nether"><layer lighting="none"/></dimension>
				    <dimension id="end"><layer lighting="cave"/></dimension>
				    <dimension id="example:moon"><layer/></dimension>
				</world>
				<world name="Explicit world" worldDir="missing">
				    <dimension name="Explicit dimension"><layer/><layer name="Explicit layer"/></dimension>
				    <dimension><layer/></dimension>
				</world>
				<world worldDir="Missing save folder"><dimension><layer/></dimension></world>
				""".formatted(save.toString().replace('\\', '/')));
		assertEquals("Block Gallery", config.getWorlds().get(0).getName());
		assertEquals("Explicit world", config.getWorlds().get(1).getName());
		assertEquals("Missing save folder", config.getWorlds().get(2).getName());
		assertEquals(java.util.List.of("Overworld", "The Nether", "The End", "Other", "Explicit dimension", "Overworld", "Overworld"),
				config.getDimensions().stream().map(DimensionConfig::getName).toList());
		assertEquals("layer0", config.getDimensionConfig(0).getLayer(0).getName());
		assertEquals("layer1", config.getDimensionConfig(0).getLayer(1).getName());
		assertEquals("layer2", config.getDimensionConfig(1).getLayer(0).getName());
		assertEquals("layer3", config.getDimensionConfig(2).getLayer(0).getName());
		assertEquals("layer0", config.getDimensionConfig(4).getLayer(0).getName());
		assertEquals("Explicit layer", config.getDimensionConfig(4).getLayer(1).getName());
		assertEquals("Block Gallery", parse("<map worldDir='%s'><layer/></map>"
				.formatted(save.toString().replace('\\', '/'))).getWorlds().getFirst().getName());
		Files.writeString(save.resolve("level.dat"), "unreadable");
		assertEquals("Save folder", WorldConfig.defaultName(save.toFile()));
		try (org.jnbt.NBTOutputStream out = new org.jnbt.NBTOutputStream(Files.newOutputStream(save.resolve("level.dat")))) {
			out.writeTag(new org.jnbt.CompoundTag("", java.util.Map.of("Data", new org.jnbt.CompoundTag("Data",
					java.util.Map.of("LevelName", new org.jnbt.StringTag("LevelName", "   "))))));
		}
		assertEquals("Save folder", WorldConfig.defaultName(save.toFile()));
	}

	@Test
	void resolvesDefaultsAndOverridesWithoutLeakingIntoSiblings() throws Exception {
		Configuration config = parse("""
				<config numZoomLevels="5" imageFormat="webp" playersInitiallyVisible="false">
				    <render useSmoothLighting="false" biomeBlendRadius="3" legacyWater="true"/>
				</config>
				<world name="Survival" worldDir="save" portalsInitiallyVisible="false">
				    <render biomeBlendRadius="4"/>
				    <dimension id="minecraft:overworld" name="Surface" numZoomLevels="7" spawnInitiallyVisible="false">
				        <render useSmoothLighting="true"/>
				        <layer name="Day"/>
				        <layer name="Night" lighting="night" imageFormat="png">
				            <render biomeBlendRadius="0" useSmoothLighting="false"/>
				        </layer>
				    </dimension>
				    <dimension id="minecraft:the_nether" name="Nether"><layer name="Day"/></dimension>
				</world>
				<world worldDir="creative"><dimension><layer/></dimension></world>
				""");
		assertEquals(2, config.getWorlds().size());
		WorldConfig survival = config.getWorlds().getFirst();
		assertEquals("Survival", survival.getName());
		assertEquals(2, survival.getDimensions().size());
		DimensionConfig surface = survival.getDimensions().getFirst();
		assertSame(survival, surface.getWorld());
		assertEquals(Path.of("save"), surface.getWorldDir().toPath());
		assertEquals(7, surface.getNumZoomLevels());
		assertTrue(surface.isSmoothLit());
		assertEquals(4, surface.getLayer(0).getBiomeBlendRadius());
		assertTrue(surface.getLayer(0).isSmoothLit());
		assertEquals(ImageFormat.WEBP, surface.getLayer(0).getImageFormat());
		assertTrue(surface.getLayer(0).isLegacyWater());
		assertEquals(0, surface.getLayer(1).getBiomeBlendRadius());
		assertFalse(surface.getLayer(1).isSmoothLit());
		assertEquals(ImageFormat.PNG, surface.getLayer(1).getImageFormat());
		assertEquals(LightStyle.Night, surface.getLayer(1).getLightStyle());
		assertFalse(surface.getMarkerVisibility().isSpawn());
		assertFalse(surface.getMarkerVisibility().isPlayers());
		assertFalse(surface.getMarkerVisibility().isPortals());
		DimensionConfig nether = survival.getDimensions().get(1);
		assertEquals(Dimension.NETHER, nether.getDimension());
		assertEquals(5, nether.getNumZoomLevels());
		assertFalse(nether.isSmoothLit());
		assertTrue(nether.getMarkerVisibility().isSpawn());
		assertFalse(nether.getMarkerVisibility().isPlayers());
		DimensionConfig creative = config.getWorlds().get(1).getDimensions().getFirst();
		assertEquals(3, creative.getBiomeBlendRadius());
		assertTrue(creative.getMarkerVisibility().isPortals());
	}

	@Test
	void directRenderingAttributesOverrideInheritedRenderElements() throws Exception {
		Configuration config = parse("""
				<config legacyWater="true" useOldGrayscaleLighting="true"><render useSmoothLighting="false" biomeBlendRadius="7"/></config>
				<world worldDir="save" useSmoothLighting="true">
				    <dimension><layer biomeBlendRadius="0"/></dimension>
				</world>
				""");
		assertTrue(config.getDimensionConfig(0).getLayer(0).isSmoothLit());
		assertEquals(0, config.getDimensionConfig(0).getLayer(0).getBiomeBlendRadius());
		assertTrue(config.getDimensionConfig(0).getLayer(0).isLegacyWater());
		assertTrue(config.getDimensionConfig(0).getLayer(0).useOldGrayscaleLighting());
	}

	@Test
	void normalizesMixedLegacyAliasesAndGroupsSaveDirectories() throws Exception {
		Configuration config = parse("""
				<map name="Surface" worldDir="save"><layer name="Day"/></map>
				<dimension name="Nether" worldDir="./save" id="nether"><layer name="Night"/></dimension>
				<map name="Other world" worldDir="creative"><layer name="Day"/></map>
				""");
		assertEquals(2, config.getWorlds().size());
		assertEquals(3, config.numDimensions());
		for (int i = 0; i < 3; i++) {
			assertEquals("Map" + i, config.getDimensionConfig(i).getId());
			assertEquals("Layer" + (char) ('A' + i), config.getDimensionConfig(i).getLayer(0).getId());
		}
		assertEquals(Dimension.NETHER, config.getDimensionConfig(1).getDimension());
	}

	@Test
	void generatesUniqueIdentitiesIndependentOfDisplayNames() throws Exception {
		String body = """
				<world name="Same" worldDir="save">
				    <dimension name="Same"><layer name="Same"/><layer name="Same"/></dimension>
				    <dimension name="Same"><layer name="Same"/></dimension>
				</world>
				<world name="Same" worldDir="save"><dimension name="Same"><layer name="Same"/></dimension></world>
				""";
		Configuration config = parse(body);
		Configuration renamed = parse(body.replace("Same", "Renamed"));
		assertEquals(2, config.getWorlds().size());
		HashSet<String> dimensions = new HashSet<>();
		HashSet<String> layers = new HashSet<>();
		for (int i = 0; i < config.numDimensions(); i++) {
			DimensionConfig dimension = config.getDimensionConfig(i);
			assertTrue(dimensions.add(dimension.getId()));
			assertEquals(dimension.getId(), renamed.getDimensionConfig(i).getId());
			for (int j = 0; j < dimension.numLayers(); j++) {
				assertTrue(layers.add(dimension.getLayer(j).getId()));
				assertEquals(dimension.getLayer(j).getId(), renamed.getDimensionConfig(i).getLayer(j).getId());
			}
		}
		assertNotEquals(DirUtils.getLayerDir(directory.toFile(), config.getDimensionConfig(0).getLayer(0)),
				DirUtils.getLayerDir(directory.toFile(), config.getDimensionConfig(1).getLayer(0)));
	}
	@Test
	void rejectsWorldWithoutSaveDirectory() {
		assertThrows(IllegalArgumentException.class, () -> parse("<world><dimension/></world>"));
	}
	@Test
	void readsWorldResourcesAndPartialInheritedFilters() throws Exception {
		Configuration config = parse("""
				<config minecraftJar="global.jar" texturePack="global.zip">
				    <views width="800" height="600"/>
				    <mods><mod path="global-mod.jar"/></mods>
				</config>
				<world worldDir="a" minecraftJar="world.jar" useProgrammerArt="true">
				    <views width="1024"/>
				    <dimension><views height="768"/><layer/></dimension>
				</world>
				<world worldDir="b"><dimension><layer/></dimension></world>
				""");
		WorldConfig world = config.getWorlds().getFirst();
		assertEquals("world.jar", world.getMinecraftJar().getPath());
		assertEquals("global.zip", world.getTexturePack().getPath());
		assertTrue(world.isUsingProgrammerArt());
		assertEquals("global-mod.jar", world.getModJars().getFirst().getPath());
		assertEquals(1024, world.getDimensions().getFirst().getViewConfig().getWidth());
		assertEquals(768, world.getDimensions().getFirst().getViewConfig().getHeight());
		assertEquals("global.jar", config.getWorlds().get(1).getMinecraftJar().getPath());
		assertEquals(600, config.getDimensionConfig(1).getViewConfig().getHeight());
	}

	@Test
	void supportsNamespacedDimensionsWithoutFallingBackToTheOverworld() throws Exception {
		DimensionInfo info = ParseUtil.parseDimension("example:moon/caves");
		assertEquals(Dimension.OTHER, info.dimension());
		assertEquals("example:moon/caves", info.name());
		Path custom = Files.createDirectories(directory.resolve("dimensions/example/moon/caves"));
		assertEquals(custom, DirUtils.getDimensionDir(directory, info));
		assertThrows(IllegalStateException.class,
				() -> DirUtils.getDimensionDir(directory, ParseUtil.parseDimension("example:missing")));
		assertEquals(directory.resolve("DIM-1"), DirUtils.getDimensionDir(directory, ParseUtil.parseDimension("minecraft:the_nether")));
	}

	@ParameterizedTest
	@ValueSource(strings = {"example:../escape", "example:/absolute", "..:escape", "example:path//gap", "example:path/./dot", "example:path\\backslash"})
	void rejectsUnsafeDimensionPaths(String id) {
		assertThrows(IllegalArgumentException.class, () -> ParseUtil.parseDimension(id));
	}

	@Test
	void exportsWorldAndDimensionMetadataAndEscapesDisplayNames() throws Exception {
		Configuration config = parse("""
				<world name="A &quot;world&quot;" worldDir="save" playersInitiallyVisible="false">
				    <dimension name="Surface" numZoomLevels="3"><layer name="Day &quot;light&quot;"/></dimension>
				</world>
				""");
		config.setOutputDir(directory.toFile());
		Path contents = directory.resolve("contents.js");
		OutputResourcesUtil.outputContents(contents.toFile(), config);
		String output = Files.readString(contents);
		assertTrue(output.contains("worldId: \"World0\""));
		assertTrue(output.contains("worldName: \"A \\\"world\\\"\""));
		assertTrue(output.contains("name: \"Day \\\"light\\\"\""));
		assertTrue(output.contains("dimensionId: \"minecraft:overworld\""));
		assertTrue(output.contains("maxZoom: 3"));
		assertTrue(output.contains("playerControlChecked: false"));
	}

	@Test
	void parsesTheDocumentedMultiWorldExample() throws Exception {
		Configuration config = XmlConfigurationParser.parseConfiguration(Path.of("Docs/worldConfig.xml").toFile());
		assertEquals(2, config.getWorlds().size());
		assertEquals(4, config.numDimensions());
		assertEquals(6, config.getDimensions().stream().mapToInt(DimensionConfig::numLayers).sum());
		assertEquals(Dimension.NETHER, config.getDimensionConfig(1).getDimension());
		assertEquals(Dimension.END, config.getDimensionConfig(2).getDimension());
	}

	@Test
	void resourceIdSelectsMinecraftDimensionWhileOutputIdentityIsAutomatic() throws Exception {
		Configuration config = parse("""
				<world worldDir="save">
				    <dimension id="minecraft:the_nether"><layer/></dimension>
				    <dimension id="minecraft:the_nether"><layer/></dimension>
				    <dimension id="example:moon/caves"><layer/></dimension>
				</world>
				<map worldDir="legacy" dimension="nether"><layer/></map>
				""");
		assertEquals(Dimension.NETHER, config.getDimensionConfig(0).getDimension());
		assertEquals(Dimension.NETHER, config.getDimensionConfig(1).getDimension());
		assertNotEquals(config.getDimensionConfig(0).getId(), config.getDimensionConfig(1).getId());
		assertEquals("example:moon/caves", config.getDimensionConfig(2).getDimensionInfo().name());
		assertEquals(Dimension.NETHER, config.getDimensionConfig(3).getDimension());
	}

	@Test
	void changingGlobalResourcesUpdatesInheritedWorldsAndPreservesWorldOverrides() throws Exception {
		Configuration config = parse("""
				<config minecraftJar="global.jar" texturePack="global.zip"/>
				<world worldDir="a"><dimension/></world>
				<world worldDir="b" minecraftJar="own.jar" texturePack="own.zip"><dimension/></world>
				""");
		config.setMinecraftJar(Path.of("updated.jar").toFile());
		config.setTexturePack(Path.of("updated.zip").toFile());
		assertEquals("updated.jar", config.getWorlds().getFirst().getMinecraftJar().getPath());
		assertEquals("updated.zip", config.getWorlds().getFirst().getTexturePack().getPath());
		assertEquals("own.jar", config.getWorlds().get(1).getMinecraftJar().getPath());
		assertEquals("own.zip", config.getWorlds().get(1).getTexturePack().getPath());
	}

	@ParameterizedTest
	@ValueSource(strings = {"--minecraftJar", "-j", "minecraftJar"})
	void explicitCommandLineResourcesOverrideInheritedAndWorldValues(String jarOption) throws Exception {
		Configuration config = parse("""
				<config minecraftJar="global.jar" texturePack="global.zip"/>
				<world worldDir="a"><dimension><layer/></dimension></world>
				<world worldDir="b" minecraftJar="own.jar" texturePack="own.zip"><dimension><layer/></dimension></world>
				""");
		var options = new CommandLine(config).registerConverter(Level.class, Level::toLevel)
				.parseArgs(jarOption, "cli.jar", "-t", "cli.zip", "-a");
		config.applyCommandLineOverrides(options);
		for (WorldConfig world : config.getWorlds()) {
			assertEquals(Path.of("cli.jar").toFile(), world.getMinecraftJar());
			assertEquals(Path.of("cli.zip").toFile(), world.getTexturePack());
			assertTrue(world.isUsingProgrammerArt());
		}
	}

	@Test
	void omittedResourceFlagsPreserveWorldOverridesAndExplicitFalseDisablesProgrammerArt() throws Exception {
		Configuration config = parse("""
				<config minecraftJar="global.jar" texturePack="global.zip" useProgrammerArt="true"/>
				<world worldDir="save" minecraftJar="own.jar" texturePack="own.zip"><dimension><layer/></dimension></world>
				""");
		var command = new CommandLine(config).registerConverter(Level.class, Level::toLevel);
		config.applyCommandLineOverrides(command.parseArgs("--numZoomLevels", "3"));
		WorldConfig world = config.getWorlds().getFirst();
		assertEquals("own.jar", world.getMinecraftJar().getPath());
		assertEquals("own.zip", world.getTexturePack().getPath());
		assertTrue(world.isUsingProgrammerArt());
		assertEquals(3, config.getDimensionConfig(0).getNumZoomLevels());
		config.applyCommandLineOverrides(command.parseArgs("--useProgrammerArt=false"));
		assertFalse(world.isUsingProgrammerArt());
	}

	private Configuration parse(String body) throws Exception {
		Path file = directory.resolve("config.xml");
		Files.writeString(file, "<tectonicus version='2'>" + body + "</tectonicus>");
		return XmlConfigurationParser.parseConfiguration(file.toFile());
	}

	@Test
	void storesAllDimensionsLayersDirectlyUnderTheirWorld() throws Exception {
		Configuration config = parse("""
				<world worldDir="save">
				    <dimension><layer/><layer/></dimension>
				    <dimension id="nether"><layer/></dimension>
				</world>
				<world worldDir="other"><dimension><layer/></dimension></world>
				""");
		assertEquals(directory.resolve("World0/Layer0").toFile(),
				DirUtils.getLayerDir(directory.toFile(), config.getDimensionConfig(0).getLayer(0)));
		assertEquals(directory.resolve("World0/Layer1").toFile(),
				DirUtils.getLayerDir(directory.toFile(), config.getDimensionConfig(0).getLayer(1)));
		assertEquals(directory.resolve("World0/Layer2").toFile(),
				DirUtils.getLayerDir(directory.toFile(), config.getDimensionConfig(1).getLayer(0)));
		assertEquals(directory.resolve("World1/Layer0").toFile(),
				DirUtils.getLayerDir(directory.toFile(), config.getDimensionConfig(2).getLayer(0)));
		config.setOutputDir(directory.toFile());
		Path contents = directory.resolve("contents.js");
		OutputResourcesUtil.outputContents(contents.toFile(), config);
		String output = Files.readString(contents);
		assertTrue(output.contains("outputId: \"World0\""));
		assertTrue(output.contains("directory: \"Layer2\""));
		assertTrue(output.contains("compass: \"Scripts/World0.Dimension1/Compass.png\""));
		assertTrue(output.contains("images: \"World0/Images/\""));
		assertTrue(output.contains("images: \"World1/Images/\""));
		assertTrue(output.contains("localizations: \"Scripts/World0/localizations.json\""));
		assertTrue(output.contains("localizations: \"Scripts/World1/localizations.json\""));
	}

	@Test
	void generatesReadableIdsAndIndependentScriptNames() throws Exception {
		Configuration config = parse("""
				<world worldDir="save">
				    <dimension id="overworld"><layer/></dimension>
				    <dimension id="example:moon/caves"><layer/></dimension>
				</world>
				""");
		DimensionConfig dimension = config.getDimensionConfig(0);
		assertEquals("World0.Dimension0", dimension.getId());
		assertEquals("World0.Layer0", dimension.getLayer(0).getId());
		assertEquals("World0.Dimension1", config.getDimensionConfig(1).getId());
		assertTrue(dimension.getJavascriptId().matches("[A-Za-z_$][A-Za-z0-9_$]*"));
		assertNotEquals(new DimensionConfig("a-b").getJavascriptId(), new DimensionConfig("a_b").getJavascriptId());
		config.setOutputDir(directory.toFile());
		Path players = directory.resolve("players.js");
		OutputResourcesUtil.outputPlayers(players.toFile(), directory.toFile(), dimension, java.util.List.of(), null);
		assertTrue(Files.readString(players).contains("var " + dimension.getJavascriptId() + "_playerData"));
		Path contents = directory.resolve("contents.js");
		OutputResourcesUtil.outputContents(contents.toFile(), config);
		assertTrue(Files.readString(contents).contains("players: " + dimension.getJavascriptId() + "_playerData"));
	}

}
