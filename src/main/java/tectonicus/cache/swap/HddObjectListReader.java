/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.cache.swap;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

public class HddObjectListReader<T extends Swappable>
{
	private final long fileSize;
	
	private long position;
	
	private FileInputStream fileInput;
	private DataInputStream dataInput;
	
	public HddObjectListReader(File file) throws Exception
	{
		fileInput = new FileInputStream(file);
		dataInput = new DataInputStream(fileInput);
		
		this.fileSize = file.length();
	}
	
	public void close()
	{
		try
		{
			if (dataInput != null)
				dataInput.close();
		}
		catch (Exception e) {}
		
		try
		{
			if (fileInput != null)
				fileInput.close();
		}
		catch (Exception e) {}
	}
	
	public boolean hasNext()
	{
		return position < fileSize;
	}
	
	public void read(T t) throws Exception
	{
		final int magic = dataInput.readInt();
		position += 4;
		
		if (magic != HddObjectListWriter.MAGIC)
		{
			throw new Exception("Oh noes!");
		}
		
		final int length = dataInput.readInt();
		position += 4;
		
		byte[] data = new byte[length];
		dataInput.read(data);
		position += length;
		
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		DataInputStream memoryIn = new DataInputStream(in);
		
		t.readFrom(memoryIn);
	}
}
