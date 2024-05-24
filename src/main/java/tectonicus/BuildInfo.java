/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.IVersionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Properties;
import java.util.TimeZone;

@Slf4j
@UtilityClass
public class BuildInfo
{
	private static final Properties info;
	private static String dateTimeFormatted;
	
	static
	{
		info = new Properties();
		dateTimeFormatted = null;
		
		final URL url = BuildInfo.class.getClassLoader().getResource("tectonicus.buildInfo");
		if (url != null)
		{
			try(final InputStream in = url.openStream())
			{
				info.load(in);
				String dateTimeProperty = info.getProperty("buildDateTime");
				if (dateTimeProperty != null) {
					DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG);
					dateTimeFormatted = formatter.format(ZonedDateTime.parse(dateTimeProperty).withZoneSameInstant(TimeZone.getDefault().toZoneId()));
				}
			} catch (IOException e) {
				log.error("Exception: ", e);
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
	
	public static void print() {
		log.info(" + + + + + + + + + + + + + + + + + + + + + + +");
		log.info("                  Tectonicus");
		log.info("   Version: " + getVersion());
		log.info("   Build: " + getBuildNumber());
		log.info("   Constructed on " + getBuildDateTimeFormatted());
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
	
	public static String getBuildDateTimeFormatted() {
		return dateTimeFormatted;
	}

	public static class PropertiesVersionProvider implements IVersionProvider {
		public String[] getVersion() {
			return new String[] {
					"Tectonicus " + BuildInfo.getVersion(),
					"Built: " + BuildInfo.getBuildDateTimeFormatted()
			};
		}
	}
}
