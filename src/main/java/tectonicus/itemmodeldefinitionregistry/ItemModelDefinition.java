/*
 * Copyright (c) 2025 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.itemmodeldefinitionregistry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemModelDefinition {
	private String type;
        private String base;
	private String model;
        private ItemModelDefinition innerModel; 
        private String texture;
        private String kind;
        
        @JsonProperty("model")
	private void unpackModel(Object model) {
		if (model instanceof String) {
			this.model = (String) model;
		} else if (model instanceof java.util.Map) {
			@SuppressWarnings("unchecked")
			java.util.Map<String, Object> modelMap = (java.util.Map<String, Object>) model;

			innerModel = new ItemModelDefinition();
			innerModel.setType((String) modelMap.get("type"));
			innerModel.setTexture((String) modelMap.get("texture"));
                        
                        model = modelMap.get("model");
			if (model != null) {
				innerModel.unpackModel(model);
			}
		}
	}
        
        public String getModelName() {
                if (model != null) {
                        return model;
                }
                if (innerModel != null) {
                        return innerModel.getModelName();
                }
                return null;
        }
}
