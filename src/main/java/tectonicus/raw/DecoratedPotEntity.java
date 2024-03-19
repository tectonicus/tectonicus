/*
 * Copyright (c) 2024, Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

public class DecoratedPotEntity extends BlockEntity
{
	private final String sherd1;
	private final String sherd2;
	private final String sherd3;
	private final String sherd4;
	
	public DecoratedPotEntity(int x, int y, int z, int localX, int localY, int localZ, String sherd1, String sherd2, String sherd3, String sherd4)
	{
		super(x, y, z, localX, localY, localZ);
                this.sherd1 = sherd1;
                this.sherd2 = sherd2;
                this.sherd3 = sherd3;
                this.sherd4 = sherd4;
	}

	public String getSherd1() { return sherd1; }
	public String getSherd2() { return sherd2; }
	public String getSherd3() { return sherd3; }
	public String getSherd4() { return sherd4; }
}
