package tectonicus.world.subset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tectonicus.SaveFormat;
import tectonicus.chunk.ChunkCoord;
import tectonicus.world.subset.PolygonalWorldSubset.Point;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PolygonalWorldSubsetTest {

	@Test
	void includesDiagonalEdgesAndFloorsPositionsToTheSameColumnsAsTheFilter() {
		var triangle = polygon(new Point(-18, -5), new Point(-10, -5), new Point(-18, 3));
		assertTrue(triangle.containsColumn(-18, 3));
		assertTrue(triangle.containsColumn(-14, -1));
		assertFalse(triangle.containsColumn(-14, 0));
		assertFalse(triangle.containsBlock(Double.NaN, -1));
		assertFalse(triangle.containsBlock(-14, Double.POSITIVE_INFINITY));

		var filter = triangle.getBlockFilter(new ChunkCoord(-1, -1));
		assertTrue(filter.containsColumn(2, 15));
		assertEquals(filter.containsColumn(2, 15), triangle.containsBlock(-13.25, -0.25));
		assertTrue(triangle.containsBlock(-17.75, -4.25));
		assertFalse(triangle.containsBlock(-18.25, -4.25));
		assertTrue(filter.containsColumn(-2, 11)); // Anchor belongs to chunk (-2, -1).
		assertFalse(filter.containsColumn(-3, 11));
	}

	@Test
	void keepsConcaveCutoutOpenAndChoosesAViewInsideTheFootprint() {
		var vertices = new ArrayList<>(List.of(new Point(0, 0), new Point(12, 0), new Point(12, 12),
				new Point(8, 12), new Point(8, 4), new Point(4, 4), new Point(4, 12), new Point(0, 12)));
		var subset = new PolygonalWorldSubset(vertices, HeightRange.UNBOUNDED);
		vertices.clear();
		assertEquals(new BoundedWorldSubset.Bounds(0, 0, 12, 12), subset.getBounds());
		assertTrue(subset.containsColumn(2, 8));
		assertTrue(subset.containsColumn(4, 8));
		assertFalse(subset.containsColumn(6, 8));
		assertTrue(subset.containsColumn(10, 8));
		assertEquals(0, subset.getOrigin().x);
		assertEquals(0, subset.getOrigin().z);
		assertFalse(subset.needsOrigin());
	}

	@Test
	void handlesIntegerOverflowInGeometryAndBufferedBounds() {
		var triangle = polygon(new Point(Long.MIN_VALUE, Long.MIN_VALUE), new Point(Long.MAX_VALUE, Long.MIN_VALUE),
				new Point(Long.MAX_VALUE, Long.MAX_VALUE));
		assertTrue(triangle.containsColumn(0, 0));
		assertFalse(triangle.containsColumn(0, 1));
		assertTrue(triangle.containsColumn(Long.MAX_VALUE, Long.MAX_VALUE));
		assertFalse(triangle.containsBlock(0x1p63, 0));
		assertFalse(triangle.containsBlock(Math.nextDown(-0x1p63), 0));
		assertEquals(-1, triangle.getOrigin().x);
		assertTrue(triangle.contains(new ChunkCoord(Math.floorDiv(Long.MAX_VALUE, 16), Math.floorDiv(Long.MIN_VALUE, 16))));
	}

	@Test
	void visitsExistingRegionsOnBothSidesOfTheBufferedBoundary(@TempDir Path dimension) throws IOException {
		Path region = Files.createDirectory(dimension.resolve("region"));
		for (String name : List.of("r.-2.0.mca", "r.-1.0.mca", "r.0.0.mca", "r.1.0.mca", "r.0.0.mcr")) {
			Files.createFile(region.resolve(name));
		}
		var subset = polygon(new Point(-530, 64), new Point(-510, 64), new Point(-510, 72), new Point(-530, 72));
		assertEquals(Set.of("r.-2.0.mca", "r.-1.0.mca"), regionNames(subset.createRegionIterator(SaveFormat.ANVIL, dimension.toFile())));
		assertTrue(subset.contains(new ChunkCoord(-31, 4)));
		assertFalse(subset.contains(new ChunkCoord(-29, 4)));
		assertTrue(subset.containsColumn(-512, 65));

		var nearOrigin = polygon(new Point(-15, 64), new Point(-2, 64), new Point(-2, 80));
		assertEquals(Set.of("r.-1.0.mca", "r.0.0.mca"), regionNames(nearOrigin.createRegionIterator(SaveFormat.ANVIL, dimension.toFile())));
		assertEquals(Set.of("r.0.0.mcr"), regionNames(nearOrigin.createRegionIterator(SaveFormat.MC_REGION, dimension.toFile())));
		assertTrue(nearOrigin.contains(new ChunkCoord(0, 4)));
		assertFalse(nearOrigin.getBlockFilter(new ChunkCoord(0, 4)).containsColumn(0, 0));
	}

	private static Set<String> regionNames(RegionIterator iterator) {
		Set<String> names = new HashSet<>();
		while (iterator.hasNext()) {
			assertTrue(names.add(iterator.next().getName()));
		}
		assertNull(iterator.next());
		return names;
	}

	private static PolygonalWorldSubset polygon(Point... points) {
		return new PolygonalWorldSubset(List.of(points), HeightRange.UNBOUNDED);
	}
}
