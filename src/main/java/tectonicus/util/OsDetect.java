/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OsDetect {
	public static final String OS_NAME = "os.name";

	public static boolean isWindows() {
		String os = System.getProperty(OS_NAME).toLowerCase();
		return (os.contains("win"));
	}

	public static boolean isMac() {
		String os = System.getProperty(OS_NAME).toLowerCase();
		return (os.contains("mac"));
	}

	public static boolean isUnix() {
		String os = System.getProperty(OS_NAME).toLowerCase();
		return (os.contains("nix") || os.contains("nux"));
	}
}
