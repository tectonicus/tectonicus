/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine.IVersionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

@Log4j2
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
		log.info(" + + + + + + + + + + + + + + + + + + + + + + +");
		log.info("                  Tectonicus");
		log.info("   Version: "+getVersion());
		log.info("   Build "+getBuildNumber());
		log.info("   Constructed on "+getBuildDate()+" at "+getBuildTime());
		log.info("\n   www.github.com/tectonicus/tectonicus  ");
		log.info(" + + + + + + + + + + + + + + + + + + + + + + +");
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
