package tectonicus.cache;

import org.junit.jupiter.api.Test;
import tectonicus.util.Vector3l;
import tectonicus.world.subset.HeightRange;
import tectonicus.world.subset.PolygonalWorldSubset;
import tectonicus.world.subset.PolygonalWorldSubset.Point;
import tectonicus.world.subset.RectangularWorldSubset;
import tectonicus.world.subset.UnionWorldSubset;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileViewCacheTest {
	@Test
	void accountsForTheOutlineEvenWhenThePolygonBoundsStayTheSame() {
		Point a = new Point(0, 0);
		Point b = new Point(12, 0);
		Point c = new Point(12, 12);
		Point d = new Point(4, 5);
		Point e = new Point(0, 12);
		var outline = List.of(a, b, c, d, e);
		var polygon = new PolygonalWorldSubset(outline, HeightRange.UNBOUNDED);
		var rotated = new PolygonalWorldSubset(List.of(c, d, e, a, b), HeightRange.UNBOUNDED);
		var reversedAndClosed = new PolygonalWorldSubset(List.of(d, c, b, a, e, d), HeightRange.UNBOUNDED);
		var reshaped = new PolygonalWorldSubset(List.of(a, b, c, new Point(4, 6), e), HeightRange.UNBOUNDED);
		var sliced = new PolygonalWorldSubset(outline, new HeightRange(45, null));

		String key = FileViewCache.subsetCacheKey(polygon);
		assertNotNull(key);
		assertEquals(key, FileViewCache.subsetCacheKey(rotated));
		assertEquals(key, FileViewCache.subsetCacheKey(reversedAndClosed));
		assertEquals(polygon.toString(), reversedAndClosed.toString());
		assertEquals(polygon.getBounds(), reshaped.getBounds());
		assertNotEquals(polygon.toString(), reshaped.toString());
		assertNotEquals(key, FileViewCache.subsetCacheKey(reshaped));
		assertNotEquals(polygon.toString(), sliced.toString());
		assertEquals(key, FileViewCache.subsetCacheKey(sliced));

		var parentHeight = new HeightRange(null, 80);
		var single = new UnionWorldSubset(List.of(new UnionWorldSubset.Member("A", polygon)), List.of(), parentHeight);
		var changed = new UnionWorldSubset(List.of(new UnionWorldSubset.Member("A", reshaped)), List.of(), parentHeight);
		assertNotNull(FileViewCache.subsetCacheKey(single));
		assertNotEquals(FileViewCache.subsetCacheKey(single), FileViewCache.subsetCacheKey(changed));

		var rectangle = new RectangularWorldSubset(new Vector3l(8, 64, 8), 16, 16);
		var includes = List.of(new UnionWorldSubset.Member("Base", rectangle));
		assertNull(FileViewCache.subsetCacheKey(new UnionWorldSubset(includes, List.of(), parentHeight)));
		assertNotEquals(FileViewCache.subsetCacheKey(new UnionWorldSubset(includes, List.of(polygon))),
				FileViewCache.subsetCacheKey(new UnionWorldSubset(includes, List.of(reshaped))));
	}
}
