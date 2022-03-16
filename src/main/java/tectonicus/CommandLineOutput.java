/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CommandLineOutput implements ProgressListener {
	@Override
	public void onTaskStarted(String taskName) {
		log.info(taskName);
	}
	
	@Override
	public void onTaskUpdate(int num, int ofTotalNum) {
	//	System.out.println(""+num+" / "+ofTotalNum);
	}
}
