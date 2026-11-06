package tectonicus.world;

import biomeextractor.WorldProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.jnbt.*;
import tectonicus.Minecraft;
import tectonicus.SaveFormat;
import tectonicus.cache.CacheUtil;
import tectonicus.cache.NullBiomeCache;
import tectonicus.chunk.ChunkCoord;
import tectonicus.configuration.*;
import tectonicus.configuration.filter.ViewFilter;
import tectonicus.configuration.filter.ViewFilterType;
import tectonicus.raw.BiomeIds;
import tectonicus.rasteriser.*;
import tectonicus.util.Colour4f;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LegacyBiomeColorTest {
	@TempDir Path directory;
	private final int previousHeight = Minecraft.getChunkHeight();
	@AfterEach void restoreHeight() { Minecraft.setChunkHeight(previousHeight); }

	@ParameterizedTest
	@ValueSource(strings = {"MINECRAFT_ALPHA_120_JAR", "MINECRAFT_BETA_13_JAR", "MINECRAFT_BETA_181_JAR", "MINECRAFT_1_0_JAR"})
	void worldUsesExtractedGrassAndFoliageIncludingBlendedChunkEdges(String variable) throws Exception {
		File jar = fixture(variable);
		Path source = createWorld(false);
		DimensionConfig map = dimension(source, jar);
		map.setBiomeBlendRadius(1);
		Configuration config = new Configuration();
		config.setCacheDir(directory.resolve("cache").toFile());
		try (World world = new World(rasteriser(), map, null, config); WorldProcessor oracle = new WorldProcessor()) {
			assertNotNull(world.getTexturePack().getFont(), "Historical font must be available for map export");
			Path oracleWorld = Files.createDirectory(directory.resolve("oracle"));
			Files.copy(source.resolve("level.dat"), oracleWorld.resolve("level.dat"));
			oracle.setJarLocation(jar);
			oracle.setWorldFolder(oracleWorld.toFile());
			assertTrue(oracle.bindToMinecraft());
			assertTrue(oracle.loadWorld());
			assertFalse(world.getBiomeCache() instanceof NullBiomeCache);
			for (int x : new int[] {0, 1, 15, 16}) {
				for (boolean foliage : new boolean[] {false, true}) {
					Colour4f expected = BiomeColorBlender.blend(x, 5, 1, (sx, sz) -> {
						try {
							return new Colour4f(oracle.getColorAtBlock(sx, sz, foliage
									? WorldProcessor.ColourType.FoliageColour : WorldProcessor.ColourType.GrassColour));
						} catch (Exception e) { throw new AssertionError(e); }
					});
					Colour4f actual = foliage ? world.getFoliageColor(new ChunkCoord(), x, 65, 5)
							: world.getGrassColor(new ChunkCoord(), x, 65, 5);
					assertEquals(expected.toRgb(), actual.toRgb(), "world x=" + x + ", foliage=" + foliage);
				}
			}
			assertTrue(Files.exists(directory.resolve("cache/legacy/biomeCache/1/0/biomeData_1_0.dat")));
		}
	}

	@Test
	void minecraft10OptionUsesGeneratedSwampsForPlantsAndBlueWater() throws Exception {
		File jar = fixture("MINECRAFT_1_0_JAR");
		Path source = createWorld(false);
		int worldX = 0, worldZ = 0;
		boolean found = false;
		int grass, foliage;
		try (WorldProcessor oracle = new WorldProcessor()) {
			oracle.setJarLocation(jar);
			oracle.setWorldFolder(source.toFile());
			assertTrue(oracle.bindToMinecraft());
			assertTrue(oracle.loadWorld());
			for (int cx = -100; cx <= 100 && !found; cx++) {
				boolean[] mask = oracle.getSwampMaskAtBlocks(cx * 16, 0, 16, 16);
				for (int i = 0; i < mask.length; i++) if (mask[i]) {
					worldX = cx * 16 + i % 16; worldZ = i / 16; found = true; break;
				}
			}
			assertTrue(found, "Fixture seed must include a swamp along the sampled strip");
			grass = oracle.getRGBAtBlock(worldX, worldZ, WorldProcessor.ColourType.GrassColour);
			foliage = oracle.getRGBAtBlock(worldX, worldZ, WorldProcessor.ColourType.FoliageColour);
		}
		// Populate the discovered column using the same seed and generated biome mask.
		Path original = source;
		Path moved = directory.resolve("original");
		Files.move(original, moved);
		source = createWorld(false, worldX >> 4);
		DimensionConfig map = dimension(source, jar);
		map.setMinecraft10SwampColors(true);
		map.setSwampWaterTint(SwampWaterTint.MINECRAFT10);
		map.setBiomeBlendRadius(0);
		Configuration config = new Configuration();
		config.setCacheDir(directory.resolve("cache").toFile());
		try (World world = new World(rasteriser(), map, null, config)) {
			ChunkCoord coord = new ChunkCoord(worldX >> 4, 0);
			int x = worldX & 15;
			assertEquals(((grass & 0xFEFEFE) + 0x4E0E4E) / 2, world.getGrassColor(coord, x, 65, worldZ).toRgb());
			assertEquals(((foliage & 0xFEFEFE) + 0x4E0E4E) / 2, world.getFoliageColor(coord, x, 65, worldZ).toRgb());
			Path chunk = source.resolve(Integer.toString((int)coord.x & 63, 36) + "/0/c." + Integer.toString((int)coord.x, 36) + ".0.dat");
			assertEquals(0xE0FF70, world.getWaterColor(new tectonicus.raw.RawChunk(chunk.toFile()), x, 65, worldZ).toRgb());
			map.setMinecraft10SwampColors(false);
			map.setSwampWaterTint(SwampWaterTint.AUTO);
			world.applyRenderOptions(map);
			assertEquals(0xE0FFAE, world.getWaterColor(new tectonicus.raw.RawChunk(chunk.toFile()), x, 65, worldZ).toRgb());
			for (SwampWaterTint mode : SwampWaterTint.values()) {
				map.setSwampWaterTint(mode);
				world.applyRenderOptions(map);
				assertEquals(mode.getRgb(), world.getWaterColor(new tectonicus.raw.RawChunk(chunk.toFile()), x, 65, worldZ).toRgb());
			}
			map.setSwampWaterTint(SwampWaterTint.AUTO);
			map.setMinecraft10SwampColors(true);
			world.applyRenderOptions(map);
			assertEquals(0xE0FFAE, world.getWaterColor(new tectonicus.raw.RawChunk(chunk.toFile()), x, 65, worldZ).toRgb(), "Plant option must not override water");
			map.setMinecraft10SwampColors(false);
			world.applyRenderOptions(map);
			assertEquals(grass & 0xFFFFFF, world.getGrassColor(coord, x, 65, worldZ).toRgb());
		}
	}

	@Test
	void generatedWaterKeepsResourcePackAtlasesAndExplicitTextures() throws Exception {
		File jar = fixture("MINECRAFT_1_0_JAR");
		var textures = new tectonicus.texture.TexturePack(rasteriser(), jar, null, false, List.of(), List.of());
		assertTrue(textures.canGenerateProceduralFluid("terrain[15, 12]", false));
		assertFalse(textures.canGenerateProceduralFluid("custom/water.png", false));
		assertTrue(textures.canGenerateProceduralFluid("terrain[15, 15]", true));
		assertFalse(textures.canGenerateProceduralFluid("custom/lava.png", true));
		assertEquals(16, textures.getProceduralFluid(true, false, 1).texture.getWidth());
		assertEquals(32, textures.getProceduralFluid(true, true, 1).texture.getWidth());
		assertFalse(textures.canGenerateProceduralFluid("terrain[1, 2]", false));
		var still = textures.getProceduralFluid(false, false, 1);
		var flow = textures.getProceduralFluid(false, true, 1);
		assertEquals(16, still.texture.getWidth());
		assertEquals(32, flow.texture.getWidth());
		assertSame(still, textures.getProceduralFluid(false, false, 1));
		Path pack = directory.resolve("custom.zip");
		try (var out = new java.util.zip.ZipOutputStream(Files.newOutputStream(pack))) {
			out.putNextEntry(new java.util.zip.ZipEntry("terrain.png"));
			javax.imageio.ImageIO.write(new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB), "png", out);
			out.closeEntry();
		}
		var overridden = new tectonicus.texture.TexturePack(rasteriser(), jar, pack.toFile(), false, List.of(), List.of());
		assertFalse(overridden.canGenerateProceduralFluid("terrain[15, 12]", false));
		assertFalse(overridden.canGenerateProceduralFluid("terrain[15, 15]", true));
	}

	@Test
	void savedBiomesTakePrecedenceEvenWhenExtractionIsAvailable() throws Exception {
		File jar = fixture("MINECRAFT_BETA_13_JAR");
		Path source = createWorld(true);
		DimensionConfig map = dimension(source, jar);
		map.setBiomeBlendRadius(0);
		Configuration config = new Configuration();
		config.setCacheDir(directory.resolve("cache").toFile());
		try (World world = new World(rasteriser(), map, null, config)) {
			var coordinates = BiomeIds.getColourCoord(1);
			int expected = world.getTexturePack().getGrassColour(coordinates.x, coordinates.y).getRGB() & 0xffffff;
			assertEquals(expected, world.getGrassColor(new ChunkCoord(1, 0), 3, 65, 5).toRgb());
			Path savedChunk = source.resolve("1/0/c.1.0.dat");
			assertEquals(0xFFFFFF, world.getWaterColor(new tectonicus.raw.RawChunk(savedChunk.toFile()), 3, 65, 5).toRgb());
			assertFalse(Files.exists(directory.resolve("cache/legacy/biomeCache/1/0/biomeData_1_0.dat")));
		}
	}

	@Test
	void layerBiomeColorOverridesControlRenderingAndCacheAvailability() throws Exception {
		File jar = fixture("MINECRAFT_1_0_JAR");
		Path source = createWorld(false);
		DimensionConfig map = dimension(source, jar);
		map.setUseBiomeColours(false);
		map.setBiomeBlendRadius(0);
		LayerConfig colored = new LayerConfig("colored", map.getId());
		colored.setBiomeBlendRadius(0);
		map.addLayer(colored);
		Configuration config = new Configuration();
		config.setCacheDir(directory.resolve("cache").toFile());
		int extracted;
		try (World world = new World(rasteriser(), map, null, config)) {
			assertFalse(world.getBiomeCache() instanceof NullBiomeCache, "An enabled layer needs extraction even when the map default is off");
			var coordinates = BiomeIds.getColourCoord(1);
			int plain = world.getTexturePack().getGrassColour(coordinates.x, coordinates.y).getRGB() & 0xFFFFFF;
			assertEquals(plain, world.getGrassColor(new ChunkCoord(), 3, 65, 5).toRgb());
			assertFalse(Files.exists(directory.resolve("cache/legacy/biomeCache/0/0/biomeData_0_0.dat")));
			world.applyRenderOptions(colored);
			extracted = world.getGrassColor(new ChunkCoord(), 3, 65, 5).toRgb();
			assertTrue(Files.exists(directory.resolve("cache/legacy/biomeCache/0/0/biomeData_0_0.dat")));
			world.applyRenderOptions(map);
			assertEquals(plain, world.getGrassColor(new ChunkCoord(), 3, 65, 5).toRgb());
			Path chunk = source.resolve("0/0/c.0.0.dat");
			assertEquals(0xFFFFFF, world.getWaterColor(new tectonicus.raw.RawChunk(chunk.toFile()), 3, 65, 5).toRgb());
			world.applyRenderOptions(colored);
			assertEquals(extracted, world.getGrassColor(new ChunkCoord(), 3, 65, 5).toRgb());
		}
		colored.setUseBiomeColours(false);
		map.setUseBiomeColours(true);
		try (World world = new World(rasteriser(), map, null, config)) {
			assertFalse(world.getBiomeCache() instanceof NullBiomeCache, "Views still use the map's biome colors");
			assertEquals(extracted, world.getGrassColor(new ChunkCoord(), 3, 65, 5).toRgb());
			world.applyRenderOptions(colored);
			var coordinates = BiomeIds.getColourCoord(1);
			int plain = world.getTexturePack().getGrassColour(coordinates.x, coordinates.y).getRGB() & 0xFFFFFF;
			assertEquals(plain, world.getGrassColor(new ChunkCoord(), 3, 65, 5).toRgb());
		}
		map.setViewFilter(new ViewFilter(ViewFilterType.None));
		try (World world = new World(rasteriser(), map, null, config)) {
			assertInstanceOf(NullBiomeCache.class, world.getBiomeCache(), "Neither views nor tile layers need extraction");
		}
	}

	@Test
	void skipsExtractionForModernSavesOtherDimensionsAndExplicitlyDisabledColors() {
		Configuration config = new Configuration();
		DimensionConfig map = new DimensionConfig("test");
		File absent = directory.resolve("absent.jar").toFile();
		assertInstanceOf(NullBiomeCache.class, CacheUtil.createBiomeCache(config, map, absent, SaveFormat.ANVIL, 1L));
		map.setDimension(Dimension.NETHER);
		assertInstanceOf(NullBiomeCache.class, CacheUtil.createBiomeCache(config, map, absent, SaveFormat.ALPHA, 1L));
		map.setDimension(Dimension.OVERWORLD);
		map.setUseBiomeColours(false);
		assertInstanceOf(NullBiomeCache.class, CacheUtil.createBiomeCache(config, map, absent, SaveFormat.ALPHA, 1L));
	}

	private File fixture(String variable) {
		String path = System.getenv(variable);
		assumeTrue(path != null && !path.isBlank(), "Set " + variable + " for legacy rendering verification");
		return new File(path);
	}

	private DimensionConfig dimension(Path source, File jar) {
		WorldConfig definition = new WorldConfig("test", source.toFile());
		definition.setMinecraftJar(jar);
		DimensionConfig map = new DimensionConfig("legacy");
		map.setWorldDir(source.toFile());
		definition.addDimension(map);
		return map;
	}

	private Path createWorld(boolean storedBiomes, int... chunkXs) throws Exception {
		Path world = Files.createDirectory(directory.resolve("world"));
		Files.createDirectory(world.resolve("players"));
		try (var input = getClass().getResourceAsStream("/legacybiomes/level.dat")) {
			Files.copy(input, world.resolve("level.dat"));
		}
		for (int x : chunkXs.length == 0 ? new int[] {-1, 0, 1} : chunkXs) {
			Path chunk = world.resolve(Integer.toString(x & 63, 36) + "/0/c." + Integer.toString(x, 36) + ".0.dat");
			Files.createDirectories(chunk.getParent());
			Map<String, Tag> level = new HashMap<>();
			level.put("xPos", new IntTag("xPos", x));
			level.put("zPos", new IntTag("zPos", 0));
			if (storedBiomes && x == 1) {
				level.put("Sections", new ListTag("Sections", CompoundTag.class, List.of()));
				byte[] biomes = new byte[256];
				java.util.Arrays.fill(biomes, (byte)1);
				level.put("Biomes", new ByteArrayTag("Biomes", biomes));
			} else {
				level.put("Blocks", new ByteArrayTag("Blocks", new byte[32768]));
			}
			try (NBTOutputStream out = new NBTOutputStream(Files.newOutputStream(chunk))) {
				out.writeTag(new CompoundTag("", Map.of("Level", new CompoundTag("Level", level))));
			}
		}
		return world;
	}

	private Rasteriser rasteriser() {
		return (Rasteriser)Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[] {Rasteriser.class},
				(proxy, method, args) -> {
					if (method.getName().equals("getMaxTextureSize")) return 16384;
					if (method.getName().equals("createTexture")) {
						BufferedImage image = args[0] instanceof BufferedImage bitmap ? bitmap : ((BufferedImage[])args[0])[0];
						return new Texture() {
							public int getWidth() { return image.getWidth(); }
							public int getHeight() { return image.getHeight(); }
						};
					}
					if (method.getName().equals("createMesh")) {
						return Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[] {Mesh.class},
								(mesh, call, arguments) -> call.getReturnType() == int.class ? 0 : null);
					}
					if (method.getReturnType() == int.class) return 0;
					if (method.getReturnType() == boolean.class) return false;
					return null;
				});
	}
}
