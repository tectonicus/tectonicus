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
