/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.io.OutputStream;
import java.io.PrintStream;

/** Allows you to take two print streams and treat them as one - everything sent
 *  to this print stream will be output to both of the child streams.
 *  Useful for echoing debugging messages (eg. sending the same messages to both the
 *  console and a log file).
 *  
 * @author John Campbell
 *
 */
public class CompositePrintStream extends PrintStream
{
	private PrintStream otherStream;
	
	public CompositePrintStream(OutputStream basisStream, PrintStream otherStream)
	{
		super(basisStream);
		this.otherStream = otherStream;
	}

	public void close()
	{
		super.close();
		otherStream.close();
	}

	public void flush()
	{
		super.flush();
		otherStream.flush();
	}

	public void println(Object x)
	{
		super.println(x);
		otherStream.println(x);
	}

	public void println(String x)
	{
		super.println(x);
		otherStream.println(x);
	}
	
}
