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
package tectonicus.world;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import tectonicus.cache.swap.Swappable;
import tectonicus.raw.RawSign;
import tectonicus.util.Vector3l;

public class Sign implements Swappable
{
	private int blockId;
	private int blockData;
	
	private Vector3l position;
	
	private String[] text;
	
	public Sign()
	{
		position = new Vector3l();
		text = new String[4];
	}
	
	public Sign(RawSign rawSign)
	{
		blockId = rawSign.blockId;
		blockData = rawSign.data;
		
		position = new Vector3l(rawSign.x, rawSign.y, rawSign.z);
		
		text = new String[4];
		text[0] = rawSign.text1;
		text[1] = rawSign.text2;
		text[2] = rawSign.text3;
		text[3] = rawSign.text4;
	}
	
	public int getData()
	{
		return blockData;
	}

	public long getX()
	{
		return position.x;
	}
	public long getY()
	{
		return position.y;
	}
	public long getZ()
	{
		return position.z;
	}
	
	public String getText(final int index)
	{
		if (index < 0 || index >= text.length)
			return "";
		
		return text[index];
	}
	
	@Override
	public void writeTo(DataOutputStream dest) throws Exception
	{
		dest.writeInt(blockId);
		dest.writeInt(blockData);
		
		dest.writeLong(position.x);
		dest.writeLong(position.y);
		dest.writeLong(position.z);
		
		for (int i=0; i<text.length; i++)
			dest.writeUTF(text[i]);
	}
	
	@Override
	public void readFrom(DataInputStream source) throws Exception
	{
		blockId = source.readInt();
		blockData = source.readInt();
		
		position.x = source.readLong();
		position.y = source.readLong();
		position.z = source.readLong();
		
		for (int i=0; i<text.length; i++)
			text[i] = source.readUTF();
	}
}
