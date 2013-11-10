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
 *   * Neither the name of 'Tectonicus' nor the names of
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
package tectonicus.raw;

public class TileEntity
{
	public int blockId;
	public int blockData;
	
	public int x, y, z;
	public int localX, localY, localZ;
	
	public int data, item;
	
	public String text1, text2, text3, text4;

	public TileEntity(int blockId, int blockData,
					  int x, int y, int z,
					  int localX, int localY, int localZ,
					  int data, int item)
	{
		init(blockId, blockData, x, y, z, localX, localY, localZ, data, item, null, null, null, null);
	}
	
	public TileEntity(int blockId, int data,
					int x, int y, int z,
					int localX, int localY, int localZ,
					String text1, String text2, String text3, String text4)
	{
		init(blockId, data, x, y, z, localX, localY, localZ, 0, 0, text1, text2, text3, text4);
	}
	
	private void init(int blockId, int blockData,
					int x, int y, int z,
					int localX, int localY, int localZ,
					int data, int item,
					String text1, String text2, String text3, String text4)
	{
		this.blockId = blockId;
		this.blockData = blockData;
		
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.localX = localX;
		this.localY = localY;
		this.localZ = localZ;
		
		this.data = data;
		this.item = item;
		
		this.text1 = text1;
		this.text2 = text2;
		this.text3 = text3;
		this.text4 = text4;
	}
}
