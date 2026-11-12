package tectonicus.world;

import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.NBTOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.raw.SavedMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SavedMapsTest {
	@TempDir Path world;

	@Test
	void decodesPaletteBrightnessAndUnexploredPixels() throws IOException {
		byte[] colors = new byte[128 * 128];
		colors[0] = 16; // Red, low brightness.
		colors[1] = 18; // Red, high brightness.
		colors[128] = (byte) 246; // Glow lichen, high brightness, on the second row.
		colors[129] = (byte) 255; // Unused base color.
		Path path = writeMap("data/minecraft/maps/0.dat", colors);
		var image = SavedMap.load(path, false).toImage();
		assertEquals(0xFFB40000, image.getRGB(0, 0));
		assertEquals(0xFFFF0000, image.getRGB(1, 0));
		assertEquals(0xFF7FA796, image.getRGB(0, 1));
		assertEquals(0, image.getRGB(1, 1));
		assertEquals(0, image.getRGB(2, 0));
	}

	@Test
	void usesLegacyColorsForEarlySaves() throws IOException {
		byte[] colors = new byte[128 * 128];
		colors[0] = 14; // Base color 3, high brightness.
		Path path = writeMap("data/map_0.dat", colors);
		assertEquals(0xFFA7A7A7, SavedMap.load(path, true).toImage().getRGB(0, 0));
		assertEquals(0xFFC7C7C7, SavedMap.load(path, false).toImage().getRGB(0, 0));
	}

	@Test
	void snapshotsReferencedMapsAndPrefersTheNamespacedFile() throws IOException {
		byte[] colors = new byte[128 * 128];
		Path legacy = writeMap("data/map_0.dat", colors);
		try (var firstRender = new SavedMaps(world, null, false)) {
			byte[] firstHash = firstRender.getContentHash(0);
			assertArrayEquals(SavedMap.load(legacy, false).contentHash(), firstHash);
			assertNull(firstRender.getContentHash(1));

			colors[0] = 6;
			Path modern = writeMap("data/minecraft/maps/0.dat", colors);
			writeMap("data/minecraft/maps/1.dat", new byte[1]);
			assertArrayEquals(firstHash, firstRender.getContentHash(0));
			try (var secondRender = new SavedMaps(world, null, false)) {
				byte[] secondHash = secondRender.getContentHash(0);
				assertFalse(MessageDigest.isEqual(firstHash, secondHash));
				assertArrayEquals(SavedMap.load(modern, false).contentHash(), secondHash);
				assertNull(secondRender.getContentHash(1));
				assertNull(secondRender.getTexture(1));
			}
		}
	}

	private Path writeMap(String relativePath, byte[] colors) throws IOException {
		Path path = world.resolve(relativePath);
		Files.createDirectories(path.getParent());
		try (var nbt = new NBTOutputStream(Files.newOutputStream(path))) {
			nbt.writeTag(new CompoundTag("", Map.of("data", new CompoundTag("data", Map.of("colors", new ByteArrayTag("colors", colors))))));
		}
		return path;
	}
}
