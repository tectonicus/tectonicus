/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tectonicus.cache.swap.Swappable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PortalColumn implements Swappable {
	public enum Axis { X, Z, UNKNOWN }

	private long x;
	private long z;
	private int minY;
	private int maxY;
	@JsonIgnore
	private Axis axis;

	@Override
	public void readFrom(DataInputStream source) throws IOException {
		x = source.readLong();
		z = source.readLong();
		minY = source.readInt();
		maxY = source.readInt();
		axis = Axis.values()[source.readUnsignedByte()];
	}

	@Override
	public void writeTo(DataOutputStream dest) throws IOException {
		dest.writeLong(x);
		dest.writeLong(z);
		dest.writeInt(minY);
		dest.writeInt(maxY);
		dest.writeByte(axis.ordinal());
	}
}
