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
import java.util.Objects;
import java.util.TreeMap;

@Data
@NoArgsConstructor
public class BlockProperties {
	public static final int LOWER_SLAB_HALF = 1;
	public static final int UPPER_SLAB_HALF = 2;
	public static final int BOTH_SLAB_HALVES = LOWER_SLAB_HALF | UPPER_SLAB_HALF;
	// North, east, south, west. Quadrant bits are indexed by x + 2*z within a layer.
	private static final int[] STAIR_HALF_MASKS = {0b0011, 0b1010, 0b1100, 0b0101};

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
		private final int slabCoverage;
		private final int stairVoxels;

		private ImmutableBlockProperties(Map<String, String> properties) {
			getProperties().putAll(properties);
			super.setProperties(Collections.unmodifiableMap(getProperties()));
			super.setPropertiesString(super.toString());
			slabCoverage = super.getSlabCoverage();
			stairVoxels = super.getStairVoxels();
		}

		@Override
		public int getSlabCoverage() {
			return slabCoverage;
		}

		@Override
		public int getStairVoxels() {
			return stairVoxels;
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

	/** Encodes the occupied vertical halves declared by a slab state. */
	public int getSlabCoverage() {
		return switch (get("type")) {
			case "bottom" -> LOWER_SLAB_HALF;
			case "top" -> UPPER_SLAB_HALF;
			case "double" -> BOTH_SLAB_HALVES;
			case null, default -> 0;
		};
	}

	/** Encodes stair occupancy in a 2x2x2 grid indexed by x + 2*z + 4*y. */
	public int getStairVoxels() {
		String shape = get("shape");
		if (shape == null)
			return 0;
		int facing = switch (get("facing")) {
			case "north" -> 0;
			case "east" -> 1;
			case "south" -> 2;
			case "west" -> 3;
			case null, default -> -1;
		};
		if (facing < 0)
			return 0;
		int front = STAIR_HALF_MASKS[facing];
		int step = switch (shape) {
			case "straight" -> front;
			case "inner_left" -> front | STAIR_HALF_MASKS[(facing + 3) & 3];
			case "inner_right" -> front | STAIR_HALF_MASKS[(facing + 1) & 3];
			case "outer_left" -> front & STAIR_HALF_MASKS[(facing + 3) & 3];
			case "outer_right" -> front & STAIR_HALF_MASKS[(facing + 1) & 3];
			default -> 0;
		};
		if (step == 0)
			return 0;
		return switch (get("half")) {
			case "bottom" -> (step << 4) | 0x0f;
			case "top" -> 0xf0 | step;
			case null, default -> 0;
		};
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
        // A plain loop: the entry-set view and its iterators showed up in allocation profiles.
        for (Map.Entry<String, String> entry : props.entrySet()) {
            String value = properties.get(entry.getKey());
            if (!Objects.equals(value, entry.getValue()) || value == null && !properties.containsKey(entry.getKey()))
                return false;
        }
        return true;
    }
}
