package tectonicus;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.Test;

import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;


public class HddTileListTests
{
	@Test
	public void createList()
	{
		File dataDir = new File("UnitTestData");
		File workingDir = new File(dataDir, "HddTileList");
		
		HddTileListFactory factory = new HddTileListFactory(workingDir);
		HddTileList list = factory.createList("basic");
		
		list.add(new TileCoord(  0,   0));
		list.add(new TileCoord(  1,   1));
		list.add(new TileCoord( 20,   0));
		list.add(new TileCoord(  0,  20));
		list.add(new TileCoord( 40,  40));
		list.add(new TileCoord( 41,  40));
		
		list.add(new TileCoord( -5,   0));
		list.add(new TileCoord(-10, -10));
		
		// Add this one twice, should be a no-op the second time
		list.add(new TileCoord(40, 40));
	}
	
	@Test
	public void emptyIterate()
	{
		File dataDir = new File("UnitTestData");
		File workingDir = new File(dataDir, "HddTileList");
		
		HddTileListFactory factory = new HddTileListFactory(workingDir);
		HddTileList list = factory.createList("basic");
		
		int count = 0;
		for (TileCoord coord : list)
		{
			System.out.println("Found: "+coord);
			count++;
		}
		
		TestCase.assertEquals(0, count);
	}
	
	@Test
	public void singleIterate()
	{
		File dataDir = new File("UnitTestData");
		File workingDir = new File(dataDir, "HddTileList");
		
		HddTileListFactory factory = new HddTileListFactory(workingDir);
		HddTileList list = factory.createList("basic");
		
		Set<TileCoord> coords = new HashSet<TileCoord>();
		coords.add( new TileCoord( 0,  0) );
		
		for (TileCoord coord : coords)
		{
			list.add(coord);
		}
		
		Set<TileCoord> foundCoords = new HashSet<TileCoord>();
		for (TileCoord coord : list)
		{
			foundCoords.add(coord);
		}
		
		// Check we got everything
		assertEquals(coords, foundCoords);
	}
	
	
	@Test
	public void iterate()
	{
		File dataDir = new File("UnitTestData");
		File workingDir = new File(dataDir, "HddTileList");
		
		HddTileListFactory factory = new HddTileListFactory(workingDir);
		HddTileList list = factory.createList("basic");
		
		Set<TileCoord> coords = new HashSet<TileCoord>();
		coords.add( new TileCoord( 0,  0) );
		coords.add( new TileCoord( 1,  1) );
		coords.add( new TileCoord(20,  0) );
		coords.add( new TileCoord( 0, 20) );
		coords.add( new TileCoord(20, 20) );
		
		for (TileCoord coord : coords)
		{
			list.add(coord);
		}
		
		Set<TileCoord> foundCoords = new HashSet<TileCoord>();
		for (TileCoord coord : list)
		{
			System.out.println("Iterator returned "+coord);
			foundCoords.add(coord);
		}
		
		// Check we got everything
		assertEquals(coords, foundCoords);
	}
	
	
	private void assertEquals(Set<TileCoord> lhs, Set<TileCoord> rhs)
	{
		TestCase.assertEquals(lhs.size(), rhs.size());
		for (TileCoord lhsCoord : lhs)
		{
			TestCase.assertTrue(rhs.contains(lhsCoord));
		}
	}
	
	
}
