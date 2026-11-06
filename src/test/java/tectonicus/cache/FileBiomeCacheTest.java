package tectonicus.cache;

import biomeextractor.WorldProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.jnbt.*;
import tectonicus.chunk.ChunkCoord;
import tectonicus.raw.LevelDat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FileBiomeCacheTest {
	@TempDir Path directory;

	@ParameterizedTest
	@ValueSource(strings = {"MINECRAFT_ALPHA_120_JAR", "MINECRAFT_BETA_13_JAR", "MINECRAFT_BETA_181_JAR", "MINECRAFT_1_0_JAR"})
	void matchesTheGeneratorPersistsCoordinatesAndRepairsCorruptFiles(String variable) throws Exception {
		String jar = System.getenv(variable);
		assumeTrue(jar != null && !jar.isBlank(), "Set " + variable + " for legacy Minecraft verification");
		Path world = world();
		byte[] original = Files.readAllBytes(world.resolve("level.dat"));
		long seed = new LevelDat(world.resolve("level.dat"), "").getWorldSeed();
		ChunkCoord chunk = new ChunkCoord(2, -3);
		String key;
		BiomeData first;
		try (FileBiomeCache cache = new FileBiomeCache(directory.resolve("cache").toFile(), world.toFile(), new File(jar), seed);
				WorldProcessor oracle = new WorldProcessor()) {
			key = cache.getCacheKey();
			oracle.setJarLocation(new File(jar));
			oracle.setWorldFolder(directory.resolve("cache/biomeCache/stubWorld").toFile());
			assertTrue(oracle.bindToMinecraft());
			assertTrue(oracle.loadWorld());
			first = cache.loadBiomeData(chunk);
			for (int z : new int[] {0, 4, 15}) {
				for (int x : new int[] {0, 7, 15}) {
					byte[] expected = oracle.getCoordsAtBlock(32 + x, -48 + z);
					assertEquals(expected[0] & 255, first.getColourCoord(x, z).getX());
					assertEquals(expected[1] & 255, first.getColourCoord(x, z).getY());
				}
			}
		}
		Path persisted = directory.resolve("cache/biomeCache/2/-3/biomeData_2_-3.dat");
		FileTime old = FileTime.fromMillis(1_000_000);
		Files.setLastModifiedTime(persisted, old);
		try (FileBiomeCache cache = new FileBiomeCache(directory.resolve("cache").toFile(), world.toFile(), new File(jar), seed)) {
			assertEquals(key, cache.getCacheKey());
			assertEquals(first.getColourCoord(15, 15), cache.loadBiomeData(chunk).getColourCoord(15, 15));
			assertEquals(old, Files.getLastModifiedTime(persisted), "The persisted chunk must be reused");
		}
		Files.write(persisted, new byte[] {1, 2, 3});
		try (FileBiomeCache cache = new FileBiomeCache(directory.resolve("cache").toFile(), world.toFile(), new File(jar), seed)) {
			assertEquals(first.getColourCoord(0, 0), cache.loadBiomeData(chunk).getColourCoord(0, 0));
		}
		assertArrayEquals(original, Files.readAllBytes(world.resolve("level.dat")));
		assertFalse(Files.exists(world.resolve("session.lock")), "Minecraft must only write to the stub world");
		FileBiomeCache closed = new FileBiomeCache(directory.resolve("cache").toFile(), world.toFile(), new File(jar), seed);
		closed.close();
		assertThrows(IllegalStateException.class, () -> closed.loadBiomeData(chunk));
	}

	@Test
	void changedJarContentsAndCorruptIndexInvalidateTheCache() throws Exception {
		String fixture = System.getenv("MINECRAFT_BETA_13_JAR");
		assumeTrue(fixture != null && !fixture.isBlank());
		Path jar = directory.resolve("minecraft.jar");
		Files.copy(Path.of(fixture), jar);
		Path world = world();
		long seed = new LevelDat(world.resolve("level.dat"), "").getWorldSeed();
		File base = directory.resolve("cache").toFile();
		String firstKey;
		try (FileBiomeCache cache = new FileBiomeCache(base, world.toFile(), jar.toFile(), seed)) {
			firstKey = cache.getCacheKey();
			cache.loadBiomeData(new ChunkCoord());
		}
		Path marker = directory.resolve("cache/biomeCache/stale-marker");
		Files.writeString(marker, "stale");
		try (var archive = java.nio.file.FileSystems.newFileSystem(jar)) {
			Files.writeString(archive.getPath("/biome-cache-test.txt"), "Changed contents, same signatures");
		}
		String secondKey;
		try (FileBiomeCache cache = new FileBiomeCache(base, world.toFile(), jar.toFile(), seed)) {
			secondKey = cache.getCacheKey();
			assertNotEquals(firstKey, secondKey);
			assertFalse(Files.exists(marker));
		}
		Files.writeString(marker, "stale");
		Files.write(directory.resolve("cache/biomeCache/biomes.cache"), new byte[] {(byte)0xff});
		try (FileBiomeCache cache = new FileBiomeCache(base, world.toFile(), jar.toFile(), seed)) {
			assertEquals(secondKey, cache.getCacheKey());
			assertFalse(Files.exists(marker));
		}
	}

	@Test
	void changingTheSeedInvalidatesPreviouslyGeneratedCoordinates() throws Exception {
		String jar = System.getenv("MINECRAFT_BETA_13_JAR");
		assumeTrue(jar != null && !jar.isBlank());
		Path world = world();
		long seed = new LevelDat(world.resolve("level.dat"), "").getWorldSeed();
		String key;
		try (FileBiomeCache cache = new FileBiomeCache(directory.resolve("cache").toFile(), world.toFile(), new File(jar), seed)) {
			key = cache.getCacheKey();
			cache.loadBiomeData(new ChunkCoord());
		}
		Path marker = directory.resolve("cache/biomeCache/stale-marker");
		Files.writeString(marker, "stale");
		CompoundTag root;
		try (NBTInputStream in = new NBTInputStream(Files.newInputStream(world.resolve("level.dat")))) {
			root = (CompoundTag)in.readTag();
		}
		var data = new HashMap<>(((CompoundTag)root.getValue().get("Data")).getValue());
		data.put("RandomSeed", new LongTag("RandomSeed", seed + 1));
		var tags = new HashMap<>(root.getValue());
		tags.put("Data", new CompoundTag("Data", data));
		try (NBTOutputStream out = new NBTOutputStream(Files.newOutputStream(world.resolve("level.dat")))) {
			out.writeTag(new CompoundTag(root.getName(), tags));
		}
		try (FileBiomeCache cache = new FileBiomeCache(directory.resolve("cache").toFile(), world.toFile(), new File(jar), seed + 1)) {
			assertNotEquals(key, cache.getCacheKey());
			assertFalse(Files.exists(marker));
		}
	}

	private Path world() throws Exception {
		Path world = Files.createDirectories(directory.resolve("world"));
		try (var in = getClass().getResourceAsStream("/legacybiomes/level.dat")) {
			Files.copy(in, world.resolve("level.dat"));
		}
		return world;
	}
}
