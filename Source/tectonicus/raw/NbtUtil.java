/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package tectonicus.raw;

import java.util.List;

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
}
