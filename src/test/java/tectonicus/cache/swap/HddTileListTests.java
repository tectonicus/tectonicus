/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache.swap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tectonicus.TileCoord;
import tectonicus.cache.swap.HddTileList;
import tectonicus.cache.swap.HddTileListFactory;


public class HddTileListTests
{
	HddTileList list;
	
	@BeforeEach
	public void setUp()
	{	
		File dataDir = new File("build/tmp");
		File workingDir = new File(dataDir, "HddTileList");
		
		HddTileListFactory factory = new HddTileListFactory(workingDir);
		list = factory.createList("basic");
	}
	
	@Test
	public void createList()
	{		
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
		int count = 0;
		for (TileCoord coord : list)
		{
			System.out.println("Found: "+coord);
			count++;
		}
		
		assertThat(count, is(0));
	}
	
	@Test
	public void singleIterate()
	{
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
		assertIsEqual(coords, foundCoords);
	}
	
	
	@Test
	public void iterate()
	{
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
		assertIsEqual(coords, foundCoords);
	}
	
	
	private void assertIsEqual(Set<TileCoord> lhs, Set<TileCoord> rhs)
	{
		assertThat(lhs.size(), is(equalTo(rhs.size())));
		for (TileCoord lhsCoord : lhs)
		{
			assertThat(rhs, hasItem(lhsCoord));
		}
	}
	
}
