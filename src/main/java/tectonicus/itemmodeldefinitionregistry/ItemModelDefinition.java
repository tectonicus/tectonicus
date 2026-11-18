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
import java.util.Map;

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
	/** Tint sources of a {@code minecraft:model}, by tint index: each has a type and a default or constant value. */
	private List<Map<String, Object>> tints;
	/** The models drawn together by a {@code minecraft:composite} */
	private List<ItemModelDefinition> models;
	/** A transformation of a {@code minecraft:model}, as a map or as 16 matrix values */
	private Object transformation;

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
		return getModelForContext("gui");
	}

	/**
	 * @param displayContext where the item is shown, such as "gui" or "fixed" (item frames)
	 * @return the model used there, following the default branch of other conditions
	 */
	public ItemModelDefinition getModelForContext(String displayContext) {
		if ("minecraft:special".equals(type) || "special".equals(type))
			return this;
		if (innerModel != null)
			return innerModel.getModelForContext(displayContext);
		if (("minecraft:display_context".equals(property) || "display_context".equals(property)) && cases != null) {
			for (ConditionalModel entry : cases)
				if (displayContext.equals(entry.when) || entry.when instanceof List<?> values && values.contains(displayContext))
					return entry.model.getModelForContext(displayContext);
		}
		if (onFalse != null)
			return onFalse.getModelForContext(displayContext);
		if (fallback != null)
			return fallback.getModelForContext(displayContext);
		if (entries != null && !entries.isEmpty())
			return entries.getFirst().model.getModelForContext(displayContext);
		if (cases != null && !cases.isEmpty())
			return cases.getFirst().model.getModelForContext(displayContext);
		return this;
	}

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class ConditionalModel {
		private Object when;
		private ItemModelDefinition model;
	}
}
