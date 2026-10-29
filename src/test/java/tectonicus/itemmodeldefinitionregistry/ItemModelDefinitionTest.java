/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.itemmodeldefinitionregistry;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class ItemModelDefinitionTest {
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void preservesSpecialRendererAndBaseModel() {
		var definition = mapper.readValue("""
				{"model":{"type":"minecraft:special","base":"minecraft:item/dragon_head",
				"model":{"type":"minecraft:head","kind":"dragon"}}}
				""", ItemModelDefinition.class).getInventoryModel();
		assertEquals("minecraft:special", definition.getType());
		assertEquals("minecraft:item/dragon_head", definition.getBase());
		assertEquals("minecraft:head", definition.getInnerModel().getType());
		assertEquals("dragon", definition.getInnerModel().getKind());
	}

	@Test
	void usesDefaultChestRatherThanSeasonalCase() {
		var definition = mapper.readValue("""
				{"model":{"type":"minecraft:select","cases":[{"when":"12-25","model":{
				"type":"minecraft:special","model":{"type":"minecraft:chest","texture":"minecraft:christmas"}}}],
				"fallback":{"type":"minecraft:special","base":"minecraft:item/chest",
				"model":{"type":"minecraft:chest","texture":"minecraft:normal"}}}}
				""", ItemModelDefinition.class).getInventoryModel();
		assertEquals("minecraft:special", definition.getType());
		assertEquals("minecraft:normal", definition.getInnerModel().getTexture());
	}

	@Test
	void usesInactiveModelForConditionalItems() {
		var definition = mapper.readValue("""
				{"model":{"type":"minecraft:condition",
				"on_false":{"type":"minecraft:model","model":"minecraft:item/idle"},
				"on_true":{"type":"minecraft:model","model":"minecraft:item/active"}}}
				""", ItemModelDefinition.class).getInventoryModel();
		assertEquals("minecraft:item/idle", definition.getModelName());
	}

	@Test
	void usesGuiCaseBeforeSpecialInHandFallback() {
		var definition = mapper.readValue("""
				{"model":{"type":"minecraft:select","property":"minecraft:display_context",
				"cases":[{"when":["gui","ground"],"model":{"type":"minecraft:model","model":"minecraft:item/trident"}}],
				"fallback":{"type":"minecraft:special","model":{"type":"minecraft:trident"}}}}
				""", ItemModelDefinition.class).getInventoryModel();
		assertEquals("minecraft:item/trident", definition.getModelName());
	}

	@Test
	void usesFirstRangeEntryForStaticCompassIcon() {
		var definition = mapper.readValue("""
				{"model":{"type":"minecraft:range_dispatch","entries":[
				{"threshold":0,"model":{"type":"minecraft:model","model":"minecraft:item/compass_16"}},
				{"threshold":0.5,"model":{"type":"minecraft:model","model":"minecraft:item/compass_17"}}]}}
				""", ItemModelDefinition.class).getInventoryModel();
		assertEquals("minecraft:item/compass_16", definition.getModelName());
	}
}
