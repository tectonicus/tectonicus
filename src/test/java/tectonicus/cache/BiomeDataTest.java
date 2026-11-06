package tectonicus.cache;

import biomeextractor.ByteCoord;
import biomeextractor.WorldProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.chunk.ChunkCoord;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BiomeDataTest {
	@TempDir Path directory;

	@Test
	void samplesOneRowMajorBatchAtTheCorrectWorldCoordinatesAndKeepsUnsignedColors() throws Exception {
		try (var processor = new RecordingProcessor()) {
			BiomeData data = new BiomeData(processor, new ChunkCoord(2, -3));
			assertEquals(1, processor.calls);
			assertEquals(32, processor.x);
			assertEquals(-48, processor.z);
			assertEquals(145, data.getColourCoord(15, 4).getX());
			assertEquals(204, data.getColourCoord(15, 4).getY());
			assertTrue(data.isSwamp(15, 4));
			Path file = directory.resolve("biomes.dat");
			data.write(file.toFile());
			BiomeData restored = new BiomeData(file.toFile());
			assertTrue(restored.isSwamp(15, 4));
			assertFalse(restored.isSwamp(4, 15));
			for (int x = 0; x < 16; x++)
				for (int z = 0; z < 16; z++)
					assertEquals(data.getColourCoord(x, z), restored.getColourCoord(x, z));
		}
	}

	@Test
	void rejectsTruncatedCacheFilesInsteadOfReturningPartialData() throws Exception {
		Path file = directory.resolve("bad.dat");
		Files.write(file, new byte[] {1, 2, 3});
		assertThrows(java.io.IOException.class, () -> new BiomeData(file.toFile()));
	}

	private static class RecordingProcessor extends WorldProcessor {
		int calls, x, z;
		@Override public boolean[] getSwampMaskAtBlocks(int x, int z, int width, int depth) {
			boolean[] result = new boolean[width * depth]; result[79] = true; return result;
		}
		@Override
		public ByteCoord[] getCoordsAtBlocks(ByteCoord[] dest, float[] temps, float[] moists, int x, int z, int width, int depth) {
			calls++;
			this.x = x;
			this.z = z;
			assertEquals(16, width);
			assertEquals(16, depth);
			ByteCoord[] result = new ByteCoord[256];
			for (int dz = 0; dz < 16; dz++)
				for (int dx = 0; dx < 16; dx++)
					result[dx + dz * 16] = new ByteCoord((byte)(130 + dx), (byte)(200 + dz));
			return result;
		}
	}
}
