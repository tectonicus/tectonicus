/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.itemmodeldefinitionregistry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemModelDefinition {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private String type;
	private String base;
	private String model;
	private ItemModelDefinition innerModel;
	private String texture;
	private String kind;
	private ItemModelDefinition fallback;
	@JsonProperty("on_false")
	private ItemModelDefinition onFalse;
	private String property;
	private List<ConditionalModel> cases;
	private List<ConditionalModel> entries;

	@JsonProperty("model")
	private void unpackModel(Object model) {
		if (model instanceof String) {
			this.model = (String) model;
		} else if (model instanceof java.util.Map) {
			innerModel = OBJECT_MAPPER.convertValue(model, ItemModelDefinition.class);
		}
	}

	public String getModelName() {
		if (model != null)
			return model;
		if (innerModel != null)
			return innerModel.getModelName();
		return null;
	}

	public ItemModelDefinition getInventoryModel() {
		if ("minecraft:special".equals(type) || "special".equals(type))
			return this;
		if (innerModel != null)
			return innerModel.getInventoryModel();
		if (("minecraft:display_context".equals(property) || "display_context".equals(property)) && cases != null) {
			for (ConditionalModel entry : cases)
				if ("gui".equals(entry.when) || entry.when instanceof List<?> values && values.contains("gui"))
					return entry.model.getInventoryModel();
		}
		if (onFalse != null)
			return onFalse.getInventoryModel();
		if (fallback != null)
			return fallback.getInventoryModel();
		if (entries != null && !entries.isEmpty())
			return entries.getFirst().model.getInventoryModel();
		if (cases != null && !cases.isEmpty())
			return cases.getFirst().model.getInventoryModel();
		return this;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ConditionalModel {
		private Object when;
		private ItemModelDefinition model;
	}
}
