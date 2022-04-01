/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import java.io.File;
import java.util.List;

import tectonicus.util.OsDetect;

public class ProcessUtil
{
	public static void addFilePath(List<String> args, File file)
	{
		String filePath = "";
		if (OsDetect.isMac())
		{
			filePath = file.getAbsolutePath().replace(" ", "\\ ");
		}
		else
		{
			filePath = "\"" + file.getAbsolutePath() + "\"";
		}
		args.add(filePath);
	}
	
	public static boolean isRunning(Process process)
	{
		if (process == null)
			return false;
		
		try
		{
			process.exitValue();
			return false;
		}
		catch (IllegalThreadStateException e)
		{
			return true;
		}
	}
}
