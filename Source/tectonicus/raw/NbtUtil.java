/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import java.util.*;

import org.jnbt.ByteTag;
import org.jnbt.CompoundTag;
import org.jnbt.DoubleTag;
import org.jnbt.IntTag;
import org.jnbt.ListTag;
import org.jnbt.LongTag;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;

public class NbtUtil
{

	@SuppressWarnings("unchecked")
	public static <T extends Tag> T getChild(CompoundTag parent, String name, Class<T> clazz)
	{
		Tag child = parent.getValue().get(name);
		if (clazz.isInstance(child))
		{
			return (T)child;
		}
		else
		{
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Tag> T getChild(ListTag list, final int index, Class<T> clazz)
	{
		List<Tag> tags = list.getValue();
		if (index >= 0 && index < tags.size())
		{
			Tag t = tags.get(index);
			if (clazz.isInstance(t))
			{
				return (T)t;
			}
		}
		
		return null;
	}

	public static byte getByte(CompoundTag parent, String name, final byte defaultValue)
	{
		ByteTag tag = getChild(parent, name, ByteTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}
	
	public static short getShort(CompoundTag parent, String name, final short defaultValue)
	{
		ShortTag tag = getChild(parent, name, ShortTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}

	public static int getInt(CompoundTag parent, String name, final int defaultValue)
	{
		IntTag tag = getChild(parent, name, IntTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}

	public static long getLong(CompoundTag parent, String name, final long defaultValue)
	{
		LongTag tag = getChild(parent, name, LongTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}
	
	public static String getString(CompoundTag parent, String name, final String defaultValue)
	{
		StringTag tag = getChild(parent, name, StringTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}
	
	
	public static double getDouble(CompoundTag parent, String name, final double defaultValue)
	{
		DoubleTag tag = getChild(parent, name, DoubleTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}

	public static BlockProperties getProperties(CompoundTag properties) {
		if (properties != null) {
			Map<String, String> props = new HashMap<>();
			for (Map.Entry<String, Tag> entry : properties.getValue().entrySet()) {
				String key = entry.getKey();
				props.put(key, getString(properties, key, ""));
			}
			return new BlockProperties(props);
		} else {
			return new BlockProperties(Collections.emptyMap());
		}
	}
}
