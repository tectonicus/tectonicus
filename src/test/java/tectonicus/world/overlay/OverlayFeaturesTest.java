/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.world.overlay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tectonicus.util.FileUtils;

import static org.junit.jupiter.api.Assertions.*;

class OverlayFeaturesTest {
	@Test
	void normalizesPointsAndKeepsOnlyKnownFields() {
		var features = parse("""
				{"features": [
				  {"type": "line", "name": "Road", "color": "#c33", "width": 4, "script": "alert(1)",
				   "points": [[0, 64, 0], [10, 20], {"x": 5, "y": 70, "z": -5}, {"x": 1, "z": 2}]},
				  {"type": "Polygon", "fillColor": "gold", "fillOpacity": 0.3, "points": [[0, 0], [16, 0], [16, 16]]},
				  {"type": "marker", "name": "Shop", "description": "Line 1\\nLine 2", "point": [3, 65, 4]}
				]}""");
		assertEquals("""
				[{"type":"line","points":[[0.0,64.0,0.0],[10.0,null,20.0],[5.0,70.0,-5.0],[1.0,null,2.0]],"name":"Road","color":"#c33","width":4.0},\
				{"type":"polygon","points":[[0.0,null,0.0],[16.0,null,0.0],[16.0,null,16.0]],"fillColor":"gold","fillOpacity":0.3},\
				{"type":"marker","point":[3.0,65.0,4.0],"name":"Shop","description":"Line 1\\nLine 2"}]""",
				FileUtils.getOBJECT_MAPPER().writeValueAsString(features));
	}

	@Test
	void acceptsATopLevelArray() {
		assertEquals(1, parse("[{\"type\": \"point\", \"position\": [1, 2]}]").size());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{}",
			"[{\"type\": \"circle\", \"point\": [0, 0]}]",
			"[{\"type\": \"line\", \"points\": [[0, 0]]}]",
			"[{\"type\": \"polygon\", \"points\": [[0, 0], [1, 1]]}]",
			"[{\"type\": \"line\", \"points\": [[0, 0], [\"a\", 1]]}]",
			"[{\"type\": \"marker\", \"point\": [0, 0, 0, 0]}]",
			"[{\"type\": \"marker\", \"point\": [0, 0], \"color\": \"red;background:url(x)\"}]",
			"[{\"type\": \"line\", \"points\": [[0, 0], [1, 1]], \"opacity\": 2}]"
	})
	void rejectsInvalidFeatures(String json) {
		assertThrows(IllegalArgumentException.class, () -> parse(json));
	}

	private static tools.jackson.databind.node.ArrayNode parse(String json) {
		return OverlayFeatures.parse(FileUtils.getOBJECT_MAPPER().readTree(json));
	}
}
