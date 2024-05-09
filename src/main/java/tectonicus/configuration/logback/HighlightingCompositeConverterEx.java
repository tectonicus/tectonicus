/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.configuration.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

//This code is taken from here: https://github.com/shuwada/logback-custom-color
public class HighlightingCompositeConverterEx extends ForegroundCompositeConverterBase<ILoggingEvent> {
	@Override
	protected String getForegroundColorCode(ILoggingEvent event) {
		Level level = event.getLevel();
		switch (level.toInt()) {
			case Level.ERROR_INT:
				return ANSIConstants.BOLD + ANSIConstants.RED_FG; // same as default color scheme
			case Level.WARN_INT:
				return ANSIConstants.RED_FG;// same as default color scheme
			default:
				return ANSIConstants.DEFAULT_FG;
		}
	}
}
