/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.lwjgl.util.vector.Vector3f;

public class Util
{	
	public static float clamp(final float value, final float min, final float max)
	{
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
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

	public static ArrayList<Token> split(String original)
	{
		ArrayList<Token> result = new ArrayList<Token>();
		
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
				String after = original.substring(nextClose+2, original.length());
				
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
