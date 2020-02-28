/*
 * Copyright (c) 2019, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.experimental.UtilityClass;
import picocli.CommandLine.IVersionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

@UtilityClass
public class BuildInfo
{
	private static final Properties info;
	
	static
	{
		info = new Properties();
		
		final URL url = BuildInfo.class.getClassLoader().getResource("tectonicus.buildInfo");
		if (url != null)
		{
			try(final InputStream in = url.openStream())
			{
				info.load(in);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/*
	 / \ 
    /   \
   |\   /|
   | \ / |
	\ | /
	 \|/
	 */
	
	public static void print()
	{
		System.out.println(" + + + + + + + + + + + + + + + + + + + + + + +");
		System.out.println("                  Tectonicus");
		System.out.println("   Version: "+getVersion());
		System.out.println("   Build "+getBuildNumber());
		System.out.println("   Constructed on "+getBuildDate()+" at "+getBuildTime());
		System.out.println();
		System.out.println("   www.github.com/tectonicus/tectonicus  ");
		System.out.println(" + + + + + + + + + + + + + + + + + + + + + + +");
	}
	
	public static String getBuildNumber()
	{		
		return info.getProperty("buildNumber");
	}
	
	public static String getVersion()
	{		
		return info.getProperty("version");
	}
	
	public static String getBuildDate()
	{		
		return info.getProperty("buildDate");
	}
	
	public static String getBuildTime()
	{
		return info.getProperty("buildTime");
	}

	public static class PropertiesVersionProvider implements IVersionProvider {
		public String[] getVersion() {
			return new String[] {
					"Tectonicus " + BuildInfo.getVersion(),
					"Built: " + BuildInfo.getBuildDate()
			};
		}
	}
}
