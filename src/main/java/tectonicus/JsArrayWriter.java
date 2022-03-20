/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Map;

public class JsArrayWriter implements AutoCloseable {
	private final OutputStream out;
	private final PrintWriter writer;

	private boolean hasWrittenEntry;

	public JsArrayWriter(File file, String arrayName) throws IOException {
		Files.deleteIfExists(file.toPath());

		out = new FileOutputStream(file);
		writer = new PrintWriter(out);

		writer.println("var " + arrayName + "=[");
	}

	public void write(Map<String, String> vars) {
		if (hasWrittenEntry) {
			writer.println(",");
		}
		writer.println("\t{");

		boolean hasWrittenLine = false;

		for (Map.Entry<String, String> entry : vars.entrySet()) {
			if (hasWrittenLine) {
				writer.println(",");
			}

			writer.print("\t\t");
			writer.print(entry.getKey());
			writer.print(": ");
			writer.print(entry.getValue());

			hasWrittenLine = true;
		}

		writer.println();

		writer.print("\t}");
		hasWrittenEntry = true;
	}

	@Override
	public void close() throws IOException {
		writer.println();
		writer.println("];");

		writer.close();
		out.close();
	}
}
