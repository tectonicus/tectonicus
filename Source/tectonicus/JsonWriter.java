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
package tectonicus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Stack;

import tectonicus.util.Vector2f;
import tectonicus.util.Vector3l;

public class JsonWriter
{
	private enum State
	{
		ObjectStart,
		ObjectContinue,
		ArrayStart,
		ArrayContinue
	}
	
	private OutputStream out;
	private PrintWriter writer;
	
	private int indent;
	
	private Stack<State> stateStack;
	
	public JsonWriter(File file) throws FileNotFoundException, IOException
	{
		if (file.exists())
			file.delete();
		
		out = new FileOutputStream(file);
		writer = new PrintWriter(out);
		
		stateStack = new Stack<State>();
		stateStack.push(State.ObjectStart);
		
		writer.println();
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
			
			if (stateStack.peek() == State.ObjectContinue)
			{
				writer.println(",");
				
				stateStack.pop();
				stateStack.push(State.ObjectContinue);
			}
			
			writer.println(indent()+name+": ");
		}
		writer.println(indent()+"{");
		
		indent++;
		
		stateStack.push(State.ObjectStart);
	}
	
	public void endObject()
	{
		stateStack.pop();
		indent--;
		
		if (stateStack.peek() == State.ObjectStart)
		{
			stateStack.pop();
			stateStack.push(State.ObjectContinue);
		}
		
		writer.println();
		writer.print(indent()+"}");
	}
	
	public void writeRawVariable(String varName, String varValue)
	{
		if (stateStack.size() > 1 && stateStack.peek() == State.ObjectContinue)
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
		stateStack.push(State.ObjectContinue);
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
		String val = "new google.maps.Point("+varValue.x+", "+varValue.y+")";
		writeRawVariable(varName, val);
	}
	
	public void writeMapsPoint(String varName, final long x, final long y)
	{
		String val = "new google.maps.Point("+x+", "+y+")";
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
			
			if (stateStack.peek() == State.ObjectContinue)
			{
				writer.println(indent()+",");
				
				stateStack.pop();
				stateStack.push(State.ObjectContinue);
			}
			
			writer.println(indent()+arrayName+": ");
		}
		writer.println(indent()+"[");
		
		indent++;
		stateStack.push(State.ArrayStart);
	}
	
	public void startArrayObject()
	{
		if (stateStack.peek() == State.ArrayContinue)
			writer.println(",");
	//	else
	//		writer.println();
		
		writer.println(indent()+"{");
		
		stateStack.pop();
		stateStack.push(State.ArrayContinue);
		stateStack.push(State.ObjectStart);
		
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
