/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.experimental.UtilityClass;
import org.tectonicus.jnbt.ByteTag;
import org.tectonicus.jnbt.CompoundTag;
import org.tectonicus.jnbt.DoubleTag;
import org.tectonicus.jnbt.FloatTag;
import org.tectonicus.jnbt.IntTag;
import org.tectonicus.jnbt.ListTag;
import org.tectonicus.jnbt.LongTag;
import org.tectonicus.jnbt.ShortTag;
import org.tectonicus.jnbt.StringTag;
import org.tectonicus.jnbt.Tag;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@UtilityClass
public class NbtUtil
{

	/** Reads an optional tag, falling back to the lowercase name if the exact name is absent. */
	public <T extends Tag> T getChild(CompoundTag parent, String name, Class<T> clazz)
	{
		Tag child = parent.getValue().get(name);
		if (child == null) { //check for lowercase version of name
			child = parent.getValue().get(name.toLowerCase());
		}
		return clazz.isInstance(child) ? clazz.cast(child) : null;
	}
	
	public <T extends Tag> T getChild(ListTag list, final int index, Class<T> clazz)
	{
		List<Tag> tags = list.getValue();
		if (index >= 0 && index < tags.size())
		{
			Tag tag = tags.get(index);
			return clazz.isInstance(tag) ? clazz.cast(tag) : null;
		}
		
		return null;
	}

	public byte getByte(CompoundTag parent, String name, final byte defaultValue)
	{
		ByteTag tag = getChild(parent, name, ByteTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}
	
	public short getShort(CompoundTag parent, String name, final short defaultValue)
	{
		ShortTag tag = getChild(parent, name, ShortTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}

	public int getInt(CompoundTag parent, String name, final int defaultValue)
	{
		IntTag tag = getChild(parent, name, IntTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}

	public long getLong(CompoundTag parent, String name, final long defaultValue)
	{
		LongTag tag = getChild(parent, name, LongTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}

	public float getFloat(CompoundTag parent, String name, final long defaultValue)
	{
		FloatTag tag = getChild(parent, name, FloatTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}
	
	public String getString(CompoundTag parent, String name, final String defaultValue)
	{
		StringTag tag = getChild(parent, name, StringTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}
	
	
	public double getDouble(CompoundTag parent, String name, final double defaultValue)
	{
		DoubleTag tag = getChild(parent, name, DoubleTag.class);
		if (tag != null)
			return tag.getValue();
		else
			return defaultValue;
	}

	public BlockProperties getProperties(CompoundTag properties) {
		if (properties != null) {
			Map<String, String> props = new TreeMap<>();
			for (Map.Entry<String, Tag> entry : properties.getValue().entrySet()) {
				props.put(entry.getKey(), entry.getValue() instanceof StringTag tag ? tag.getValue() : "");
			}
			return new BlockProperties(props);
		} else {
			return new BlockProperties(Collections.emptyMap());
		}
	}
}
