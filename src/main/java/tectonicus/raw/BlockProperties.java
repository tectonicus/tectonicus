/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.raw;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@Data
@NoArgsConstructor
public class BlockProperties {
    private Map<String, String> properties = new TreeMap<>();  // we use a TreeMap to get automatic sorting
    private String propertiesString;

    public BlockProperties(Map<String, String> properties) {
        this.properties.putAll(properties);
        this.propertiesString = this.toString();
    }

	/** Copies a sorted property state that can be shared by multiple blocks. */
	public static BlockProperties immutableCopyOf(Map<String, String> properties) {
		return new ImmutableBlockProperties(properties);
	}

	private static final class ImmutableBlockProperties extends BlockProperties {
		private ImmutableBlockProperties(Map<String, String> properties) {
			getProperties().putAll(properties);
			super.setProperties(Collections.unmodifiableMap(getProperties()));
			super.setPropertiesString(super.toString());
		}

		@Override
		public void setProperties(Map<String, String> properties) {
			throw new UnsupportedOperationException("Immutable block properties");
		}

		@Override
		public void setPropertiesString(String propertiesString) {
			throw new UnsupportedOperationException("Immutable block properties");
		}

		@Override
		public String toString() {
			return getPropertiesString();
		}
	}

    /**
     * The properties as {@code key=value} pairs separated by commas, in the map's order and without any whitespace, for
     * example {@code facing=north,half=top}. Every palette entry of every chunk builds this, so it avoids a regex.
     */
    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!text.isEmpty())
                text.append(',');
            appendWithoutWhitespace(text, String.valueOf(entry.getKey()));
            text.append('=');
            appendWithoutWhitespace(text, String.valueOf(entry.getValue()));
        }
        return text.toString();
    }

    /** Appends {@code value} without the characters a regex {@code \s} matches. */
    private static void appendWithoutWhitespace(StringBuilder text, String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\u000B' && c != '\f' && c != '\r')
                text.append(c);
        }
    }

    public String get(String key) {
        return properties.get(key);
    }

    public boolean containsKey(String prop) {
        return properties.containsKey(prop);
    }

    public boolean contains(String props) {
        return propertiesString.contains(props);
    }
    public boolean containsAll(Map<String, String> props) {
        return properties.entrySet().containsAll(props.entrySet());
    }
}
