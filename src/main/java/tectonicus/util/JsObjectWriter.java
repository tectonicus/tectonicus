/*
 * Copyright (c) 2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

public class JsObjectWriter
{
	private OutputStream out;
	private PrintWriter writer;
	
	public JsObjectWriter(File file) throws FileNotFoundException, IOException
	{
		if (file.exists())
			file.delete();
		
		out = new FileOutputStream(file);
		writer = new PrintWriter(out);
	}
	
	public void write(String objName, Map<String, Object> vars)
	{
		writer.println("var "+objName+" =");
		writer.println("{");
		
		boolean hasWrittenLine = false;
		
		for (String name : vars.keySet())
		{
			Object value = vars.get(name);
			
			if (hasWrittenLine)
				writer.println(",");
			
			writer.print("\t\t");
			writer.print(name);
			writer.print(": ");
			
			if (value instanceof Integer)
			{
				Integer intValue = (Integer)value;
				writer.print(intValue.intValue());
			}
			else if (value instanceof Long)
			{
				Long longValue = (Long)value;
				writer.print(longValue.longValue());
			}
			else if (value instanceof Float)
			{
				Float floatValue = (Float)value;
				writer.print(floatValue.floatValue());
			}
			else if (value instanceof Double)
			{
				Double doubleValue = (Double)value;
				writer.print(doubleValue.doubleValue());
			}
			else if (value instanceof Boolean)
			{
				Boolean boolValue = (Boolean)value;
				writer.print(boolValue.booleanValue() ? "true" : "false");
			}
			else
			{
				writer.print("\"");
				writer.print(value);
				writer.print("\"");
			}
			
			hasWrittenLine = true;
		}
		
		writer.println();
		
		writer.println("};");
	}
	
	public void close()
	{	
		try
		{
			if (writer != null)
				writer.close();
		
			if (out != null)
				out.close();
		}
		catch (IOException e) {}
	}
}
