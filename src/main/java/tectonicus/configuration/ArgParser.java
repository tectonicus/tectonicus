/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
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
