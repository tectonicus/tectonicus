/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
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
