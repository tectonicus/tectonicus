package tectonicus.world;

import org.tectonicus.jnbt.ByteArrayTag;
import org.tectonicus.jnbt.ByteTag;
import org.tectonicus.jnbt.CompoundTag;
import org.tectonicus.jnbt.ListTag;
import org.tectonicus.jnbt.NBTOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tectonicus.Minecraft;
import tectonicus.Portal;
import tectonicus.PortalColumn;
import tectonicus.SaveFormat;
import tectonicus.Version;
import tectonicus.chunk.ChunkCoord;
import tectonicus.chunk.ChunkLocator;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static tectonicus.PortalColumn.Axis.X;
import static tectonicus.PortalColumn.Axis.UNKNOWN;
import static tectonicus.configuration.Dimension.NETHER;
import static tectonicus.world.PortalSearch.Mode.*;

class PortalScannerTest {

	@TempDir
	Path directory;

	@Test
	void resolvesTheSavedWinnerEvenWhenOnlyAFartherPortalIsExported() {
		var source = portal(column(0, 64, 0));
		var winner = column(1, 64, 0);
		var alternative = column(12, 64, 0);
		for (var rules : List.of(new PortalSearch(BLOCKS, 16, null), new PortalSearch(COLUMN_CENTERS, 128, 127))) {
			var linker = new PortalLinker(scanner(source.columns().getFirst()), scanner(winner, alternative), rules, 0.125);
			var link = linker.resolve(source);
			assertEquals(portal(winner).id(), link.destinationId());
			assertNotEquals(portal(alternative).id(), link.destinationId());
		}
	}

	@Test
	void clippedSourcesKeepTheirSavedIdentityAndEntryAcrossChunkAndHeightBoundaries() {
		var west = new PortalColumn(15, -1, 60, 66, X);
		var east = new PortalColumn(16, -1, 60, 66, X);
		var source = scanner(west, east, new PortalColumn(16, -1, 70, 73, X));
		var linker = new PortalLinker(source, null, null, 1);
		var westClip = portal(new PortalColumn(15, -1, 64, 65, X));
		var eastClip = portal(new PortalColumn(16, -1, 62, 63, X));
		var first = linker.resolve(westClip);
		var second = linker.resolve(eastClip);
		assertEquals("15,60,-1", first.id());
		assertEquals(first.id(), second.id());
		assertEquals(new Portal.EntryPosition(16, 60, -0.5), first.portal().entryPos());
		assertEquals(first.portal().entryPos(), second.portal().entryPos());
		assertSame(westClip.position(), first.portal().position());
		assertEquals(westClip.columns(), first.portal().columns());
		assertNull(first.destinationId());
		assertEquals(2, source.complete(east).columns().size());
	}

	@Test
	void completeSourceGeometryDeterminesTheDestinationRatherThanTheVisibleSlice() {
		var west = new PortalColumn(15, 0, 60, 65, X);
		var east = new PortalColumn(16, 0, 60, 65, X);
		var savedWinner = column(18, 60, 0);
		var clippedWinner = column(14, 64, 0);
		var linker = new PortalLinker(scanner(west, east), scanner(savedWinner, clippedWinner), new PortalSearch(BLOCKS, 16, null), 1);
		var link = linker.resolve(portal(new PortalColumn(15, 0, 64, 65, X)));
		assertEquals(portal(savedWinner).id(), link.destinationId());
	}

	@Test
	void legacySearchScoresBottomsAndExcludesTheNetherRoofWhileModernSearchScoresBlocks() {
		var roof = new PortalColumn(-1, 0, 129, 132, X);
		var lower = new PortalColumn(-2, 0, 115, 126, X);
		var entry = new Portal.EntryPosition(-0.25, 130, 0.5);
		var saved = scanner(roof, lower);
		assertEquals(portal(lower).id(), saved.findExit(entry, 1, new PortalSearch(COLUMN_BLOCKS, 128, 127)).id());
		assertEquals(portal(roof).id(), saved.findExit(entry, 1, new PortalSearch(BLOCKS, 16, null)).id());
	}

	@Test
	void negativeCoordinatesRespectInclusiveBlockBoundsAndHistoricalChunkWindows() {
		var entry = new Portal.EntryPosition(-0.25, 130, 0.5);
		assertNotNull(scanner(column(-17, 130, 16)).findExit(entry, 1, new PortalSearch(BLOCKS, 16, null)));
		assertNull(scanner(column(16, 130, 0)).findExit(entry, 1, new PortalSearch(BLOCKS, 16, null)));
		assertNotNull(scanner(column(128, 120, 0)).findExit(entry, 1, new PortalSearch(COLUMN_BLOCKS, 128, 127)));
		assertNull(scanner(column(32, 120, 0)).findExit(entry, 1, new PortalSearch(COLUMN_CENTERS, 32, 127)));
		assertNotNull(scanner(column(-144, 130, 0)).findExit(entry, 1, new PortalSearch(CHUNKS, 8, null)));
		assertNull(scanner(column(-145, 130, 0)).findExit(entry, 1, new PortalSearch(CHUNKS, 8, null)));
		assertNotNull(scanner(column(-145, 130, 0)).findExit(entry, 1, new PortalSearch(CHUNKS, 9, null)));
	}

	@Test
	void tiesAcrossPortalsOmitTheLinkButTiesWithinOnePortalDoNot() {
		var entry = new Portal.EntryPosition(0, 64, 0);
		assertNull(scanner(column(4, 64, 0), column(0, 64, 4)).findExit(entry, 1, new PortalSearch(BLOCKS, 16, null)));
		var low = column(0, 63, 0);
		assertEquals(portal(low).id(), scanner(low, column(0, 65, 0)).findExit(entry, 1, new PortalSearch(BLOCKS, 16, null)).id());
		var west = column(-1, 64, 0);
		var east = column(0, 64, 0);
		assertEquals(2, scanner(west, east).findExit(entry, 1, new PortalSearch(COLUMN_CENTERS, 32, 127)).columns().size());
	}

	@Test
	void returnTripsAreResolvedIndependently() {
		var original = column(8, 80, 0);
		var returning = column(80, 80, 0);
		var nether = column(10, 80, 0);
		var overworld = scanner(original, returning);
		var destination = scanner(nether);
		var outgoing = new PortalLinker(overworld, destination, new PortalSearch(BLOCKS, 16, null), 0.125).resolve(portal(original));
		var incoming = new PortalLinker(destination, overworld, new PortalSearch(BLOCKS, 128, null), 8).resolve(portal(nether));
		assertEquals(portal(nether).id(), outgoing.destinationId());
		assertEquals(portal(returning).id(), incoming.destinationId());
	}

	@Test
	void missingOrUnreadableTerrainNeverSelectsAnAlternative() {
		var source = column(0, 64, 0);
		var rules = new PortalSearch(BLOCKS, 16, null);
		assertNull(new PortalLinker(scanner(source), scanner(), rules, 1).resolve(portal(source)).destinationId());
		var unreadable = new PortalScanner(coord -> {
			if (coord.equals(new ChunkCoord(0, 0)))
				throw new IllegalStateException("Unreadable saved chunk");
			return coord.equals(new ChunkCoord(-1, 0)) ? List.of(column(-10, 64, 0)) : List.of();
		});
		var failedExitSearch = new PortalLinker(scanner(source), unreadable, rules, 1).resolve(portal(source));
		assertNull(failedExitSearch.destinationId());
		assertEquals(portal(source).id(), failedExitSearch.id()); // An unreadable exit must not break incoming jumps.
		var failedSourceRead = new PortalLinker(scanner(), scanner(source), rules, 1).resolve(portal(source));
		assertNull(failedSourceRead.destinationId());
		assertNull(failedSourceRead.id());
	}

	@Test
	void overlappingSearchesReuseScannedAndAbsentChunks() {
		var reads = new HashMap<ChunkCoord, Integer>();
		var saved = new PortalScanner(coord -> {
			reads.merge(coord, 1, Integer::sum);
			return List.of();
		});
		var rules = new PortalSearch(BLOCKS, 16, null);
		saved.findExit(new Portal.EntryPosition(0, 64, 0), 1, rules);
		saved.findExit(new Portal.EntryPosition(1, 64, 1), 1, rules);
		assertEquals(9, reads.size());
		assertTrue(reads.values().stream().allMatch(count -> count == 1));
	}

	@ParameterizedTest
	@EnumSource(value = SaveFormat.class, mode = EnumSource.Mode.EXCLUDE, names = "BEDROCK") // Bedrock has no region files; see BedrockWorldTest
	void anEmptySavedChunkOrRegionIsAnErrorInsteadOfMissingTerrain(SaveFormat format) throws Exception {
		Path file = format == SaveFormat.ALPHA
				? ChunkLocator.findChunkFile(directory.toFile(), new ChunkCoord(0, 0)).toPath()
				: directory.resolve("region/r.0.0." + format.extension);
		Files.createDirectories(file.getParent());
		Files.createFile(file);
		var saved = PortalScanner.open(directory.toFile(), Version.VERSION_UNKNOWN, ignored -> null, NETHER);
		int previousHeight = Minecraft.getChunkHeight();
		assertThrows(IllegalStateException.class, () -> saved.columns(new ChunkCoord(0, 0)));
		assertEquals(previousHeight, Minecraft.getChunkHeight());
	}

	@ParameterizedTest
	@EnumSource(value = SaveFormat.class, mode = EnumSource.Mode.EXCLUDE, names = "BEDROCK") // Bedrock has no region files; see BedrockWorldTest
	void readsNumericPortalBlocksFromUnfilteredSavedChunks(SaveFormat format) throws Exception {
		var coord = new ChunkCoord(0, 0);
		var bytes = new ByteArrayOutputStream();
		CompoundTag level;
		if (format == SaveFormat.ANVIL) {
			byte[] blocks = new byte[4096];
			blocks[0] = blocks[256] = 90;
			var section = new CompoundTag("", Map.of("Y", new ByteTag("Y", (byte) 4),
					"Blocks", new ByteArrayTag("Blocks", blocks), "Data", new ByteArrayTag("Data", new byte[2048])));
			level = new CompoundTag("Level", Map.of("Sections", new ListTag("Sections", CompoundTag.class, List.of(section))));
		} else {
			byte[] blocks = new byte[32768];
			blocks[64] = blocks[65] = 90;
			level = new CompoundTag("Level", Map.of("Blocks", new ByteArrayTag("Blocks", blocks)));
		}
		try (var out = new NBTOutputStream(bytes)) {
			out.writeTag(new CompoundTag("", Map.of("Level", level)));
		}
		if (format == SaveFormat.ALPHA) {
			var file = ChunkLocator.findChunkFile(directory.toFile(), coord).toPath();
			Files.createDirectories(file.getParent());
			Files.write(file, bytes.toByteArray());
		} else {
			Files.createDirectory(directory.resolve("region"));
			var region = ByteBuffer.allocate(12288);
			region.putInt(0, 0x201); // Chunk 0,0 in sector 2, occupying one sector.
			region.position(8192);
			region.putInt(bytes.size() + 1).put((byte) 1).put(bytes.toByteArray()); // Gzip chunk payload.
			Files.write(directory.resolve("region/r.0.0." + (format == SaveFormat.ANVIL ? "mca" : "mcr")), region.array());
		}
		int previousHeight = Minecraft.getChunkHeight();
		var saved = PortalScanner.open(directory.toFile(), Version.VERSION_UNKNOWN, ignored -> null, NETHER);
		assertEquals(format, saved.getFormat());
		assertEquals(List.of(new PortalColumn(0, 0, 64, 65, UNKNOWN)), saved.columns(coord));
		assertTrue(saved.columns(new ChunkCoord(1, 0)).isEmpty());
		assertEquals(previousHeight, Minecraft.getChunkHeight());
	}

	private static PortalColumn column(long x, int y, long z) {
		return new PortalColumn(x, z, y, y, X);
	}

	private static Portal portal(PortalColumn... columns) {
		return Portal.groupColumns(List.of(columns)).getFirst();
	}

	private static PortalScanner scanner(PortalColumn... columns) {
		return new PortalScanner(coord -> Arrays.stream(columns).filter(column ->
				Math.floorDiv(column.getX(), 16) == coord.x && Math.floorDiv(column.getZ(), 16) == coord.z).toList());
	}
}
