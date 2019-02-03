/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import tectonicus.blockTypes.Banner.Pattern;

public class BlockEntityTests
{
	@Test
	public void testBannerEntity()
	{
		Pattern p = new Pattern("cre", 5);
		BannerEntity entity = new BannerEntity(0,0,0,0,0,0,1,Arrays.asList(p, p, p));
		assertThat(entity.getBaseColor(), is(equalTo(1)));
		assertThat(entity.getPatterns().size(), is(equalTo(3)));
	}
	
	@Test
	public void testBeaconEntity()
	{
		BeaconEntity entity = new BeaconEntity(0,0,0,0,0,0,1);
		assertThat(entity.getLevels(), is(equalTo(1)));
	}
	
	@Test
	public void testContainerEntity()
	{
		ContainerEntity entity = new ContainerEntity(0,0,0,0,0,0,"Chest","locked",true);
		assertThat(entity.getCustomName(), is(equalTo("Chest")));
		assertThat(entity.getLock(), is(equalTo("locked")));
		assertThat(entity.isUnopenedContainer(), is(equalTo(true)));
	}
	
	@Test
	public void testFlowerPotEntity()
	{
		FlowerPotEntity entity = new FlowerPotEntity(0,0,0,0,0,0,38,2);
		assertThat(entity.getItem(), is(equalTo(38)));
		assertThat(entity.getData(), is(equalTo(2)));
	}
	
	@Test
	public void testPaintingEntity()
	{
		PaintingEntity entity = new PaintingEntity(0,0,0,0,0,0,"kebab",2);
		assertThat(entity.getMotive(), is(equalTo("kebab")));
		assertThat(entity.getDirection(), is(equalTo(2)));
	}
	
	@Test
	public void testSignEntity()
	{
		SignEntity entity = new SignEntity(0,0,0,0,0,0,"","Testing","","", 2);
		assertThat(entity.getText1(), is(equalTo("")));
		assertThat(entity.getText2(), is(equalTo("Testing")));
		assertThat(entity.getText3(), is(equalTo("")));
		assertThat(entity.getText4(), is(equalTo("")));
		assertThat(entity.getBlockData(), is(equalTo(2)));
	}
	
	@Test
	public void testSkullEntity()
	{
		SkullEntity entity = new SkullEntity(0,0,0,0,0,0,2,90,"Fred","873f3a78ds8","http://skinurl.com");
		assertThat(entity.getSkullType(), is(equalTo(2)));
		assertThat(entity.getRotation(), is(equalTo(90)));
		assertThat(entity.getName(), is(equalTo("Fred")));
		assertThat(entity.getUUID(), is(equalTo("873f3a78ds8")));
		assertThat(entity.getSkinURL(), is(equalTo("http://skinurl.com")));
	}

}
