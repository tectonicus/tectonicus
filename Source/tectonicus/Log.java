/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

public class Log
{
	public static final int ERROR = 0;
	public static final int MESSAGE = 1;
	public static final int DEBUG = 2;
	
	private static int level = MESSAGE;
	
	public static void setLogLevel(final int newLevel)
	{
		level = newLevel;
	}
	
	public static void logError(String str)
	{
		if (level >= ERROR)
			System.err.println(str);
	}
	
	public static void logMessage(String str)
	{
		if (level >= MESSAGE)
			System.out.println(str);
	}
	
	public static void logDebug(String str)
	{
		if (level >= DEBUG)
			System.out.println(str);
	}
}
