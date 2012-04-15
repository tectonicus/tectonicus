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
package tectonicus.configuration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ArgParser
{
	private Map<String, String> argMap;
	
	public ArgParser(String[] args)
	{
		argMap = parseCommandLine(args);
	}
	
	private static Map<String, String> parseCommandLine(String[] args)
	{
		Map<String, String> result = new HashMap<String, String>();
		
		for (String str : args)
		{
			int equals = str.indexOf('=');
			if (equals != -1)
			{
				String key = str.substring(0, equals).trim();
				String value = str.substring(equals+1, str.length()).trim();
				if (key.length() > 0 && value.length() > 0)
				{
					result.put(key.toLowerCase(), value);
				}
			}
		}
		return result;
	}
	
	public boolean isEmpty()
	{
		return argMap.isEmpty();
	}
	
	public String get(String key)
	{
		return argMap.get(key.toLowerCase());
	}
	
	public boolean hasValue(String key)
	{
		return argMap.containsKey(key.toLowerCase()) && argMap.get(key.toLowerCase()) != null;
	}
	
	public String getString(String name, String defaultValue)
	{
		String value = argMap.get(name.toLowerCase());
		if (value != null)
			return value;
		return defaultValue;
	}
	
	// Accepts 'true' 'yes' or 'on' as true, anything else false
	public boolean getBoolean(String name, final boolean defaultValue)
	{
		String value = argMap.get(name.toLowerCase());
		if (value != null)
		{
			value = value.toLowerCase();
			
			return value.equals("true")
					|| value.equals("on")
					|| value.equals("yes");
		}
		return defaultValue;
	}

	public int getInteger(String name, final int defaultValue)
	{
		String value = argMap.get(name.toLowerCase());
		if (value != null)
		{
			try
			{
				return Integer.parseInt(value);
			}
			catch (Exception e) {}
		}
		return defaultValue;
	}
	
	public float getFloat(String name, final float defaultValue)
	{
		String value = argMap.get(name.toLowerCase());
		if (value != null)
		{
			try
			{
				return Float.parseFloat(value);
			}
			catch (Exception e) {}
		}
		return defaultValue;
	}
	
	public File getFile(String name, File defaultValue)
	{
		String value = argMap.get(name.toLowerCase());
		if (value != null)
		{
			return new File(value);
		}
		return defaultValue;
	}
	
}
