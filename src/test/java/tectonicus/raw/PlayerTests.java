/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.StringTag;
import tectonicus.configuration.Dimension;
import tectonicus.configuration.ParseUtil;

import java.nio.file.Paths;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTests
{
	@Test
	void testLoadPlayerFromPlayerFile() throws Exception
	{
		Player player = new Player(Paths.get("src/test/resources/8aeb40ad-a60c-429e-b239-25b7223c5fb7.dat"));
		assertThat(player.getUuid(), is("8aeb40ada60c429eb23925b7223c5fb7"));
		assertThat((int)player.getHealth(), is(20));
		assertThat(player.getXpTotal(), is(11));
	}

	@ParameterizedTest
	@ValueSource(strings = {"example:moon", "example:moon/caves", "another:moon"})
	void preservesCustomDimensionIdentityAndDoesNotMatchOtherDimensions(String id) {
		Player player = new Player("Explorer", new CompoundTag("", Map.of("Dimension", new StringTag("Dimension", id))));
		assertEquals(Dimension.OTHER, player.getDimension());
		assertEquals(id, player.getDimensionId());
		assertTrue(player.isInDimension(ParseUtil.parseDimension(id)));
		assertFalse(player.isInDimension(ParseUtil.parseDimension("minecraft:overworld")));
		assertFalse(player.isInDimension(ParseUtil.parseDimension("example:other")));
	}

	@ParameterizedTest
	@CsvSource({"0,minecraft:overworld", "-1,minecraft:the_nether", "1,minecraft:the_end"})
	void legacyNumericDimensionsStillMatchVanillaIdentifiers(int number, String id) {
		Player player = new Player("Explorer", new CompoundTag("", Map.of("Dimension", new IntTag("Dimension", number))));
		assertEquals(id, player.getDimensionId());
		assertTrue(player.isInDimension(ParseUtil.parseDimension(id)));
	}

	@Test
	void customRespawnDimensionIsNotAnOverworldBed() {
		Player player = new Player("Explorer", new CompoundTag("", Map.of("SpawnDimension", new StringTag("SpawnDimension", "example:moon"))));
		assertEquals(Dimension.OTHER, player.getSpawnDimension());
		assertEquals("example:moon", player.getSpawnDimensionId());
	}
}
