package tectonicus.world;

import org.junit.jupiter.api.Test;
import tectonicus.SaveFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static tectonicus.configuration.Dimension.NETHER;
import static tectonicus.configuration.Dimension.OVERWORLD;
import static tectonicus.world.PortalSearch.Mode.*;

class PortalSearchTest {
	@Test
	void unversionedSavesUseTheOldestRulesConsistentWithTheirStorage() {
		assertEquals(new PortalSearch(COLUMN_CENTERS, 32, 127), PortalSearch.forSave(0, "", SaveFormat.ALPHA, OVERWORLD));
		assertEquals(new PortalSearch(COLUMN_CENTERS, 32, 127), PortalSearch.forSave(0, null, null, NETHER));
		assertEquals(new PortalSearch(COLUMN_CENTERS, 128, 127), PortalSearch.forSave(0, null, SaveFormat.MC_REGION, OVERWORLD));
		assertEquals(new PortalSearch(COLUMN_CENTERS, 128, 255), PortalSearch.forSave(0, null, SaveFormat.ANVIL, OVERWORLD));
		assertEquals(new PortalSearch(COLUMN_CENTERS, 128, 127), PortalSearch.forSave(0, "unknown", SaveFormat.ANVIL, NETHER));
	}

	@Test
	void dataVersionSelectsRulesAtSnapshotBoundaries() {
		assertEquals(new PortalSearch(COLUMN_BLOCKS, 128, 127), PortalSearch.forSave(2201, "1.21", SaveFormat.ANVIL, NETHER));
		assertEquals(new PortalSearch(CHUNKS, 8, null), PortalSearch.forSave(2203, null, SaveFormat.ANVIL, NETHER));
		assertEquals(new PortalSearch(CHUNKS, 8, null), PortalSearch.forSave(2230, null, SaveFormat.ANVIL, OVERWORLD));
		assertEquals(new PortalSearch(CHUNKS, 8, null), PortalSearch.forSave(2504, null, SaveFormat.ANVIL, OVERWORLD));
		assertEquals(new PortalSearch(CHUNKS, 9, null), PortalSearch.forSave(2506, null, SaveFormat.ANVIL, OVERWORLD));
		assertEquals(new PortalSearch(CHUNKS, 9, null), PortalSearch.forSave(2569, null, SaveFormat.ANVIL, NETHER));
		assertEquals(new PortalSearch(BLOCKS, 16, null), PortalSearch.forSave(2570, null, SaveFormat.ANVIL, NETHER));
		assertEquals(new PortalSearch(BLOCKS, 128, null), PortalSearch.forSave(2570, null, SaveFormat.ANVIL, OVERWORLD));
	}

	@Test
	void versionNameFallbackDistinguishesPatchReleasesAndSnapshots() {
		assertEquals(new PortalSearch(COLUMN_BLOCKS, 128, 255), PortalSearch.forSave(0, "1.12.2", SaveFormat.ANVIL, OVERWORLD));
		assertEquals(new PortalSearch(CHUNKS, 8, null), PortalSearch.forSave(0, "19w36a", SaveFormat.ANVIL, NETHER));
		assertEquals(new PortalSearch(CHUNKS, 9, null), PortalSearch.forSave(0, "1.16.1", SaveFormat.ANVIL, NETHER));
		assertEquals(new PortalSearch(BLOCKS, 16, null), PortalSearch.forSave(0, "1.16.2 Pre-release 1", SaveFormat.ANVIL, NETHER));
		assertEquals(new PortalSearch(BLOCKS, 16, null), PortalSearch.forSave(0, "26.3", SaveFormat.ANVIL, NETHER));
	}
}
