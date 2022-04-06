/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Stack;

public class JsonWriter implements AutoCloseable
{
	private enum State
	{
		OBJECT_START,
		OBJECT_CONTINUE,
		ARRAY_START,
		ARRAY_CONTINUE
	}
	
	private final OutputStream out;
	private final PrintWriter writer;
	
	private int indent;
	
	private final Stack<State> stateStack;
	
	public JsonWriter(File file) throws IOException {
		Files.deleteIfExists(file.toPath());
		
		out = new FileOutputStream(file);
		writer = new PrintWriter(out);
		
		stateStack = new Stack<>();
		stateStack.push(State.OBJECT_START);
		
		writer.println();
	}

	@Override
	public void close() throws IOException {
		if (writer != null)
			writer.close();

		if (out != null)
			out.close();
	}
	
	public void startObject(String name)
	{
		if (stateStack.size() == 1)
		{
			// Start a new top level object
			
			writer.println();
			writer.println();
			writer.println(indent()+"var "+name+" =");
		}
		else
		{
			// Start an embedded object
			
			if (stateStack.peek() == State.OBJECT_CONTINUE)
			{
				writer.println(",");
				
				stateStack.pop();
				stateStack.push(State.OBJECT_CONTINUE);
			}
			
			writer.println(indent()+name+": ");
		}
		writer.println(indent()+"{");
		
		indent++;
		
		stateStack.push(State.OBJECT_START);
	}
	
	public void endObject()
	{
		stateStack.pop();
		indent--;
		
		if (stateStack.peek() == State.OBJECT_START)
		{
			stateStack.pop();
			stateStack.push(State.OBJECT_CONTINUE);
		}
		
		writer.println();
		writer.print(indent()+"}");
	}
	
	public void writeRawVariable(String varName, String varValue)
	{
		if (stateStack.size() > 1 && stateStack.peek() == State.OBJECT_CONTINUE)
			writer.println(",");
		else
			writer.println();
		
		if (stateStack.size() == 1)
		{
			// Start a new top level var
			writer.print(indent()+"var "+varName+" = "+varValue);
		}
		else
		{
			writer.print(indent()+varName+": "+varValue);
		}
		
		stateStack.pop();
		stateStack.push(State.OBJECT_CONTINUE);
	}
	
	public void writeVariable(String varName, String varValue)
	{
		writeRawVariable(varName, "\""+jsEscape(varValue)+"\"");
	}
	
	public void writeVariable(String varName, final int varValue)
	{
		writeRawVariable(varName, ""+varValue);
	}
	
	public void writeVariable(String varName, final long varValue)
	{
		writeRawVariable(varName, ""+varValue);
	}
	
	public void writeWorldCoord(String varName, Vector3l varValue)
	{
		String val = "new WorldCoord("+varValue.x+", "+varValue.y+", "+varValue.z+")";
		writeRawVariable(varName, val);
	}
	
	public void writeMapsPoint(String varName, Vector2f varValue)
	{
		String val = "new L.Point("+varValue.x+", "+varValue.y+")";
		writeRawVariable(varName, val);
	}
	
	public void writeMapsPoint(String varName, final long x, final long y)
	{
		String val = "new L.Point("+x+", "+y+")";
		writeRawVariable(varName, val);
	}
	
	public void startArray(String arrayName)
	{
		if (stateStack.size() == 1)
		{
			// Start a new top level array
			
			writer.println();
			writer.println();
			writer.println(indent()+"var "+arrayName+" =");
		}
		else
		{
			// Start an embedded array
			
			if (stateStack.peek() == State.OBJECT_CONTINUE)
			{
				writer.println(indent()+",");
				
				stateStack.pop();
				stateStack.push(State.OBJECT_CONTINUE);
			}
			
			writer.println(indent()+arrayName+": ");
		}
		writer.println(indent()+"[");
		
		indent++;
		stateStack.push(State.ARRAY_START);
	}
	
	public void startArrayObject()
	{
		if (stateStack.peek() == State.ARRAY_CONTINUE)
			writer.println(",");
	//	else
	//		writer.println();
		
		writer.println(indent()+"{");
		
		stateStack.pop();
		stateStack.push(State.ARRAY_CONTINUE);
		stateStack.push(State.OBJECT_START);
		
		indent++;
	}
	
	public void endArrayObject()
	{
		stateStack.pop();
		indent--;
		
		writer.println();
		writer.print(indent()+"}");
	}
	
	public void endArray()
	{
		stateStack.pop();
		indent--;
		
		writer.println();
		writer.print(indent()+"]");		
	}
	
	public String indent()
	{
		String res = "";
		for (int i=0; i<indent; i++)
			res += "\t";
		return res;
	}
	
	private static String jsEscape(String text)
	{
		text = text.replace("\\", "\\\\");	// Replace \ with \\
		text = text.replace(" ", "&nbsp;");	// Replace spaces with &nbsp;
		text = text.replace("\"", "\\\"");	// Replace " with \"
		
		return text;
	}
}
