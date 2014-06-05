/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class PropertyFile
{
	private Map<String, String> properties;
	
	public PropertyFile(String path)
	{
		properties = new HashMap<String, String>();
		
		InputStream in = getClass().getClassLoader().getResourceAsStream(path);
		if (in != null)
		{
			BufferedReader reader = new BufferedReader( new InputStreamReader(in) );
			
			try
			{	
				String str;
				while ((str = reader.readLine()) != null)
				{
					str = str.trim();
					if (!str.startsWith("#"))
					{
						String[] parts = str.split("=");
						if (parts.length == 2)
						{
							properties.put(parts[0], parts[1]);
						}
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					reader.close();
					in.close();
				}
				catch (IOException e) {}
			}
		}
	}
	
	public String get(String name)
	{
		return properties.get(name);
	}
}
