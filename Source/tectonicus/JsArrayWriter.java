/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

public class JsArrayWriter
{
	private OutputStream out;
	private PrintWriter writer;
	
	private boolean hasWritenEntry;
	
	public JsArrayWriter(File file, String arrayName) throws FileNotFoundException, IOException
	{
		if (file.exists())
			file.delete();
		
		out = new FileOutputStream(file);
		writer = new PrintWriter(out);
		
		writer.println("var "+arrayName+"=[");
	}
	
	public void write(Map<String, String> vars)
	{
		if (hasWritenEntry)
			writer.println(",");
		writer.println("\t{");
		
		boolean hasWrittenLine = false;
		
		for (String name : vars.keySet())
		{
			String value = vars.get(name);
			
			if (hasWrittenLine)
				writer.println(",");
			
			writer.print("\t\t");
			writer.print(name);
			writer.print(": ");
			writer.print(value);
			
			
			// ..
			
			hasWrittenLine = true;
		}
		
		writer.println();
		
		writer.print("\t}");
		hasWritenEntry = true;
	}
	
	public void close()
	{
		writer.println();
		writer.println("];");
		
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
