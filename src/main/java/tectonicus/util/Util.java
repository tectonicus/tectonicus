/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.experimental.UtilityClass;
import org.joml.Vector3f;

@UtilityClass
public class Util
{
	public static float clamp(float val, float min, float max) {
		return Math.max(min, Math.min(max, val));
	}

	public static String toBase36(final long num)
	{
		return Long.toString(num, 36);
	}

	public static long fromBase36(String num)
	{
		return Long.parseLong(num, 36);
	}

	public static String getElapsedTime(Date start, Date end)
	{
		long duration = end.getTime() - start.getTime();

		final long days = TimeUnit.MILLISECONDS.toDays(duration);
		duration -= TimeUnit.DAYS.toMillis(days);

		final long hours = TimeUnit.MILLISECONDS.toHours(duration);
		duration -= TimeUnit.HOURS.toMillis(hours);

		final long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
		duration -= TimeUnit.MINUTES.toMillis(minutes);

		final long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);

		String result = "";
		if (days > 0)
		{
			result += days + " days ";
		}
		if (hours > 0)
		{
		    result += hours + " hours ";
		}
		if (minutes > 0)
		{
		    result += minutes + " minutes ";
		}
	    return result + seconds + " seconds";
	}

	public static List<Token> split(String original)
	{
		List<Token> result = new ArrayList<>();

		int index = 0;
		while (index < original.length())
		{
			final int nextOpen = original.indexOf("{{");
			final int nextClose = original.indexOf("}}");

			if (nextOpen != -1 && nextClose != -1
				&& nextOpen < nextClose)
			{
				// Found a {{ and }} pair in the right order
				String pre = original.substring(0, nextOpen);
				String replace = original.substring(nextOpen+2, nextClose);
				String after = original.substring(nextClose+2);

				if (pre.length() > 0)
					result.add( new Token(pre, false) );

				if (replace.length() > 0)
					result.add( new Token(replace, true) );

				original = after;
			}
			else
			{
				// Just untokenised text
				result.add( new Token(original, false));
				original = "";
			}
		}

		return result;
	}

	public static class Token
	{
		public final String value;
		public final boolean isReplaceable;

		public Token(String value, boolean isReplaceable)
		{
			this.value = value;
			this.isReplaceable = isReplaceable;
		}
	}

	public static float separation(Vector3f lhs, Vector3f rhs)
	{
		final float dx = lhs.x - rhs.x;
		final float dy = lhs.y - rhs.y;
		final float dz = lhs.z - rhs.z;
		return (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
	}
}
