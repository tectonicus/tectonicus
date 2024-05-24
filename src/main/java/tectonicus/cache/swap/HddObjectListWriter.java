/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache.swap;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class HddObjectListWriter<T extends Swappable>
{	
	public static final int MAGIC = 0xC0DEBABE;
	
	private FileOutputStream fileOutput;
	private DataOutputStream dataOutput;
	
	private int size;
        
        private ConcurrentLinkedQueue<T> queue;
	
	public HddObjectListWriter(File file, final boolean deleteExisting) throws Exception
	{
		if (deleteExisting && file.exists())
			file.delete();
		
		fileOutput = new FileOutputStream(file);
		dataOutput = new DataOutputStream(fileOutput);
                
                queue = new ConcurrentLinkedQueue<T>();
	}
	
	public synchronized void close()
	{
                try
                {
                        flush();
                }
                catch (Exception e)
                {
                        log.error("Exception: ", e);
                }
                
                try
		{
			if (dataOutput != null)
			{
				dataOutput.close();
				dataOutput = null;
			}
		}
		catch (Exception e) {}
		
		try
		{
			if (fileOutput != null)
			{
				fileOutput.close();
				fileOutput = null;
			}
		}
		catch (Exception e) {}
	}
	
	public void add(T t)
	{
                queue.add(t);
		size++;
	}
        
        public synchronized void flush() throws Exception {
                T t;
                while ((t = queue.poll()) != null) {
                        ByteArrayOutputStream writtenBytes = new ByteArrayOutputStream();
                        DataOutputStream tempOut = new DataOutputStream(writtenBytes);

                        t.writeTo(tempOut);

                        tempOut.flush();
                        byte[] packet = writtenBytes.toByteArray();

                        dataOutput.writeInt(MAGIC);
                        dataOutput.writeInt(packet.length);
                        dataOutput.write(packet);
                }
        }
	
	public int size()
	{
		return size;
	}
}
