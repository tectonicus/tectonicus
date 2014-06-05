/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

public class CommandLineOutput implements ProgressListener
{
	@Override
	public void onTaskStarted(String taskName)
	{
		System.out.println(""+taskName);
	}
	
	@Override
	public void onTaskUpdate(int num, int ofTotalNum)
	{
	//	System.out.println(""+num+" / "+ofTotalNum);
	}
}
