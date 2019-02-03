/*
 * Copyright (c) 2012-2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import xyz.nickr.nbt.NBTCodec;
import xyz.nickr.nbt.NBTCompression;
import xyz.nickr.nbt.tags.CompoundTag;

public class PlayerTests
{
	@Test
	public void testLoadPlayerFromLevelDat() throws IOException
	{
		try(InputStream in = Files.newInputStream(Paths.get("src/test/resources/level.dat")))
		{
			NBTCodec codec = new NBTCodec(ByteOrder.BIG_ENDIAN);
			CompoundTag tag = codec.decode(in, NBTCompression.GZIP).getAsCompoundTag();
			CompoundTag data = tag.getAsCompoundTag("Data");
			CompoundTag playerTag = data.getAsCompoundTag("Player");
			
			Player player = new Player("", playerTag);
			
			assertThat(player.getHealth(), is(20));
		}
	}
	
	@Test
	public void testLoadPlayerFromPlayerFile() throws Exception
	{
		Player player = new Player(Paths.get("src/test/resources/8aeb40ad-a60c-429e-b239-25b7223c5fb7.dat"));
		assertThat(player.getUUID(), is("8aeb40ada60c429eb23925b7223c5fb7"));
		assertThat(player.getHealth(), is(20));
		assertThat(player.getXpTotal(), is(11));
	}
}
