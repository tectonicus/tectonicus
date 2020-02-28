/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache.swap;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class HddObjectListWriter<T extends Swappable>
{	
	public static final int MAGIC = 0xC0DEBABE;
	
	private FileOutputStream fileOutput;
	private DataOutputStream dataOutput;
	
	private int size;
	
	public HddObjectListWriter(File file, final boolean deleteExisting) throws Exception
	{
		if (deleteExisting && file.exists())
			file.delete();
		
		fileOutput = new FileOutputStream(file);
		dataOutput = new DataOutputStream(fileOutput);
	}
	
	public void close()
	{
		closeWriters();
	}
	
	private void closeWriters()
	{
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
	
	
	public void add(T t) throws Exception
	{
		ByteArrayOutputStream writtenBytes = new ByteArrayOutputStream();
		DataOutputStream tempOut = new DataOutputStream(writtenBytes);
		
		t.writeTo(tempOut);
		
		tempOut.flush();
		byte[] packet = writtenBytes.toByteArray();
		
		dataOutput.writeInt(MAGIC);
		dataOutput.writeInt(packet.length);
		dataOutput.write(packet);
		
		size++;
	}
	
	public int size()
	{
		return size;
	}
}
