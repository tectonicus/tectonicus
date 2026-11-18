package tectonicus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.cache.swap.HddObjectListWriter;
import tectonicus.configuration.DimensionConfig;
import tectonicus.configuration.filter.PortalFilter;
import tectonicus.configuration.filter.PortalFilterType;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.world.subset.FullWorldSubset;
import tectonicus.world.PortalLinker;
import tectonicus.world.PortalScanner;
import tectonicus.world.subset.HeightRange;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static tectonicus.PortalColumn.Axis.*;
import static tectonicus.util.FindEntityUtil.findPortals;
import static tectonicus.util.FindEntityUtil.findPortalColumns;
import static tectonicus.util.OutputResourcesUtil.outputPortals;

class PortalTest {
	@TempDir
	Path directory;

	@Test
	void groupsPortalsAcrossChunkBoundariesAndKeepsStackedAndPerpendicularPortalsSeparate() {
		var west = new PortalColumn(-1, 2, 6, 8, X);
		var east = new PortalColumn(0, 2, 6, 8, X);
		var above = new PortalColumn(0, 2, 11, 13, X);
		var perpendicular = new PortalColumn(0, 3, 6, 8, Z);
		List<Portal> portals = Portal.groupColumns(List.of(above, perpendicular, east, west));
		assertEquals(3, portals.size());
		assertEquals(List.of(west, east), portals.getFirst().columns());
		assertEquals(new Portal.EntryPosition(0, 6, 2.5), portals.getFirst().entryPos());
		assertEquals(0, portals.getFirst().position().x);
		assertEquals(7, portals.getFirst().position().y);
	}

	@Test
	void subsetSplitPortalsExportOneMarkerEvenWithoutAnExitDimension() throws Exception {
		// A 4-wide portal whose middle columns are hidden by a subset, as in the End or a world without a Nether.
		var saved = new PortalColumn[] {new PortalColumn(14, 0, 64, 66, X), new PortalColumn(15, 0, 64, 66, X),
				new PortalColumn(16, 0, 64, 66, X), new PortalColumn(17, 0, 64, 66, X)};
		var swap = directory.resolve("split.list").toFile();
		var writer = new HddObjectListWriter<PortalColumn>(swap, true);
		try {
			writer.add(saved[0]);
			writer.add(saved[3]);
		} finally {
			writer.close();
		}
		var exported = directory.resolve("split.js").toFile();
		var map = new DimensionConfig("Map0");
		assertEquals(2, outputPortals(exported, swap, map).size());
		var source = new PortalScanner(coord -> Arrays.stream(saved).filter(column ->
				Math.floorDiv(column.getX(), 16) == coord.x && Math.floorDiv(column.getZ(), 16) == coord.z).toList());
		List<Portal> portals = outputPortals(exported, swap, map, () -> new PortalLinker(source, null, null, 1));
		assertEquals(1, portals.size());
		assertEquals(new Portal.EntryPosition(16, 64, 0.5), portals.getFirst().entryPos());
	}

	@Test
	void scansNumericAndNamedBlocksAndExportsOnlyTheVisibleRuns() throws Exception {
		int previousHeight = Minecraft.getChunkHeight();
		Minecraft.setChunkHeight(16);
		try {
			var chunk = new RawChunk();
			for (int y = 0; y <= 3; y++) {
				chunk.setBlockId(0, y, 0, BlockIds.PORTAL);
				chunk.setBlockId(1, y, 0, BlockIds.PORTAL);
			}
			chunk.setBlockName(10, 15, 10, Block.NETHER_PORTAL.getName());
			chunk.setBlockState(10, 15, 10, new BlockProperties(Map.of("axis", "z")));
			var complete = findPortalColumns(chunk);
			assertEquals(3, complete.size());
			assertEquals(0, complete.getFirst().getMinY());
			assertEquals(3, complete.getFirst().getMaxY());
			assertEquals(Z, complete.getLast().getAxis());
			var swap = directory.resolve("portals.list").toFile();
			var exported = directory.resolve("portals.js");
			var map = new DimensionConfig("Map0");
			var subset = new FullWorldSubset(new HeightRange(2, null));
			var writer = new HddObjectListWriter<PortalColumn>(swap, true);
			try {
				findPortals(chunk, writer, new PortalFilter(PortalFilterType.All), subset);
			} finally {
				writer.close();
			}
			List<Portal> portals = outputPortals(exported.toFile(), swap, map);
			assertEquals(2, portals.size());
			assertEquals(2, portals.getFirst().columns().size());
			assertEquals(2, portals.getFirst().columns().getFirst().getMinY());
			assertEquals(3, portals.getFirst().columns().getFirst().getMaxY());
			assertEquals(Z, portals.getLast().columns().getFirst().getAxis());
			assertEquals(15, portals.getLast().entryPos().y());
			String script = Files.readString(exported);
			assertTrue(script.contains("worldPos: new WorldCoord(1, 3, 0)"));
			assertTrue(script.contains("entryPos: {"));
			assertTrue(script.contains("columns: [{"));

			var disabled = new HddObjectListWriter<PortalColumn>(swap, true);
			try {
				findPortals(chunk, disabled, new PortalFilter(PortalFilterType.None), subset);
			} finally {
				disabled.close();
			}
			assertTrue(outputPortals(exported.toFile(), swap, map).isEmpty());
		} finally {
			Minecraft.setChunkHeight(previousHeight);
		}
	}
}
