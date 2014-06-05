/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

public class MemoryMonitor
{
	private Thread memoryPollThread;
	
	private long maxMemoryBytes;
	
	public MemoryMonitor()
	{
		memoryPollThread = new Thread(new MemoryPoller());
		memoryPollThread.setDaemon(true);
		memoryPollThread.start();
	}
	
	private synchronized void updateMaxMemory(final long currentMemoryBytes)
	{
		maxMemoryBytes = Math.max(maxMemoryBytes, currentMemoryBytes);
	}
	
	public synchronized long getPeakMemory()
	{
		return maxMemoryBytes;
	}
	
	private class MemoryPoller implements Runnable
	{
		@Override
		public void run()
		{
			final long usedMemBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			updateMaxMemory(usedMemBytes);
			
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {}
		}
	}
}
