/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

public class BuildInfo
{

	private static PropertyFile info;
	
	/*
	 / \ 
    /   \
   |\   /|
   | \ / |
	\ | /
	 \|/
	 */
	{
		info = new PropertyFile("tectonicus.buildInfo");
		
		System.out.println(" + + + + + + + + + + + + + + + + + + + + + + +");
		System.out.println("                  Tectonicus");
		System.out.println("   Version: "+getVersion());
		System.out.println("   Build "+getBuildNumber());
		System.out.println("   Constructed on "+getBuildDate()+" at "+getBuildTime());
	//	System.out.println();
	//	System.out.println("   www.triangularpixels.com/Tectonicus  ");
		System.out.println(" + + + + + + + + + + + + + + + + + + + + + + +");
	}
	
	public static BuildInfo print()
	{
		return new BuildInfo();
	}
	public static String getBuildNumber()
	{
		if (info == null)
			new BuildInfo();
		
		return info.get("buildNumber");
	}
	
	public static String getVersion()
	{
		if (info == null)
			new BuildInfo();
		
		return info.get("version");
	}
	
	public static String getBuildDate()
	{
		if (info == null)
			new BuildInfo();
		
		return info.get("buildDate");
	}
	
	public static String getBuildTime()
	{
		if (info == null)
			new BuildInfo();
		
		return info.get("buildTime");
	}
}
