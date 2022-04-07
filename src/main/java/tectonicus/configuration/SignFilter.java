/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tectonicus.raw.SignEntity;

@RequiredArgsConstructor
@Getter
public class SignFilter {
	private final SignFilterType type;

	public static final String HASH_VIEW = "#view";

	public boolean passesFilter(SignEntity s) {
		final String text1 = s.getText1();
		final String text2 = s.getText2();
		final String text3 = s.getText3();
		final String text4 = s.getText4();

		// Empty signs (those with no text) are used for aesthetic reasons, like building chairs
		// Always skip these
		if (text1.trim().isEmpty() && text2.trim().isEmpty() && text3.trim().isEmpty() && text4.trim().isEmpty())
			return false;

		// Always skip view signs
		if (text1.startsWith(HASH_VIEW) || text2.startsWith(HASH_VIEW) || text3.startsWith(HASH_VIEW) || text4.startsWith(HASH_VIEW)) {
			return false;
		}

		if (type == SignFilterType.NONE) {
			return false;
		} else if (type == SignFilterType.ALL || type == SignFilterType.OBEY) {
			return true;
		} else if (type == SignFilterType.SPECIAL) {
			String line = "" + text1 + text2 + text3 + text4;
			line = line.trim();
			if (line.length() > 0) {
				final char first = line.charAt(0);
				final char last = line.charAt(line.length() - 1);

				final char[] special = {'-', '=', '~', '!'};
				return containedIn(special, first) && containedIn(special, last);
			} else
				return false;
		} else {
			throw new RuntimeException("Unknown player filter:" + type);
		}
	}

	private static boolean containedIn(final char[] possible, final char actual) {
		for (char ch : possible) {
			if (ch == actual)
				return true;
		}
		return false;
	}
}
