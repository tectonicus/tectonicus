/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.itemregistry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemModel {
	private String parent;
	private Map<String, ArrayList<Float>> transform;
        private Map<String, String> textures;
                
	@JsonProperty("display")
	private void unpackNested(Map<String, Map<String, ArrayList<Float>>> brand) {
		this.transform = brand.get("gui");
	}
}
