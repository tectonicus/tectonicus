/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import tectonicus.cache.swap.Swappable;
import tectonicus.raw.SignEntity;
import tectonicus.raw.SignTextColor;
import tectonicus.util.Vector3l;

public class Sign implements Swappable
{
	private int blockData;
	
	private Vector3l position;
	
	private String[] text;
	@Getter
	private Colors dye = Colors.BLACK;
	@Getter
	private boolean glowing;
	@Getter
	private List<List<SignTextColor>> textColors = List.of();
	@Getter
	private String blockName = "";
	
	public Sign()
	{
		position = new Vector3l();
		text = new String[4];
	}
	
	public Sign(SignEntity rawSign)
	{
		blockData = rawSign.getBlockData();

		position = new Vector3l(rawSign.getX(), rawSign.getY(), rawSign.getZ());
		
		text = new String[4];
		text[0] = rawSign.getText1();
		text[1] = rawSign.getText2();
		text[2] = rawSign.getText3();
		text[3] = rawSign.getText4();
		dye = Objects.requireNonNullElse(Colors.byName(rawSign.getColor()), Colors.BLACK);
		glowing = rawSign.isGlowing();
		textColors = rawSign.getTextColors();
		blockName = rawSign.getBlockName();
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
		dest.writeInt(blockData);
		
		dest.writeLong(position.x);
		dest.writeLong(position.y);
		dest.writeLong(position.z);
		
		for (int i=0; i<text.length; i++)
			dest.writeUTF(text[i]);

		dest.writeByte(dye.getId());
		dest.writeBoolean(glowing);
		dest.writeByte(textColors.size());
		for (List<SignTextColor> line : textColors) {
			dest.writeInt(line.size());
			for (SignTextColor color : line) {
				dest.writeInt(color.start());
				dest.writeInt(color.end());
				dest.writeInt(color.rgb());
			}
		}
		dest.writeUTF(blockName);
	}
	
	@Override
	public void readFrom(DataInputStream source) throws Exception
	{
		blockData = source.readInt();
		
		position.x = source.readLong();
		position.y = source.readLong();
		position.z = source.readLong();
		
		for (int i=0; i<text.length; i++)
			text[i] = source.readUTF();

		// Swap records are read from a stream bounded to one sign.
		dye = Colors.BLACK;
		glowing = false;
		textColors = List.of();
		blockName = "";
		if (source.available() == 0) return;

		dye = Colors.byId(source.readUnsignedByte());
		glowing = source.readBoolean();
		int lineCount = source.readUnsignedByte();
		var lines = new ArrayList<List<SignTextColor>>(lineCount);
		for (int i = 0; i < lineCount; i++) {
			int colorCount = source.readInt();
			var colors = new ArrayList<SignTextColor>(colorCount);
			for (int j = 0; j < colorCount; j++) {
				colors.add(new SignTextColor(source.readInt(), source.readInt(), source.readInt()));
			}
			lines.add(List.copyOf(colors));
		}
		textColors = List.copyOf(lines);
		if (source.available() > 0) blockName = source.readUTF();
	}
}
