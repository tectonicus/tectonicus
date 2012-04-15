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
