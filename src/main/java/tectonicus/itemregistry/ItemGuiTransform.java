/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */
package tectonicus.itemregistry;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import tectonicus.texture.ZipStack;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public final class ItemGuiTransform {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private ItemGuiTransform() {}

	public static Matrix4f load(ZipStack zips, String model) throws IOException {
		JsonNode gui = inheritedProperty(zips, model, "gui");
		return gui == null ? new Matrix4f().translate(-0.5f, -0.5f, -0.5f) : fromJson(gui);
	}

	public static boolean isSideLit(ZipStack zips, String model) throws IOException {
		JsonNode light = inheritedProperty(zips, model, "gui_light");
		// gui_light was introduced after 1.13; legacy 3D item models use side lighting.
		return light == null || !"front".equals(light.asString());
	}

	private static JsonNode inheritedProperty(ZipStack zips, String model, String property) throws IOException {
		Set<String> visited = new HashSet<>();
		while (model != null && visited.add(model)) {
			String[] id = model.split(":", 2);
			String path = "assets/" + (id.length == 2 ? id[0] : "minecraft")
					+ "/models/" + id[id.length - 1] + ".json";
			try (var stream = zips.getStream(path)) {
				if (stream == null)
					break;
				JsonNode json = MAPPER.readTree(stream);
				JsonNode container = property.equals("gui") ? json.get("display") : json;
				// A child's GUI entry replaces the parent's entire entry, including omitted defaults.
				if (container != null && container.has(property))
					return container.get(property);
				model = json.has("parent") ? json.get("parent").asString() : null;
			}
		}
		return null;
	}

	public static Matrix4f fromJson(JsonNode gui) {
		Vector3f rotation = vector(gui, "rotation", 0).mul((float) Math.PI / 180);
		Vector3f translation = vector(gui, "translation", 0).div(16);
		Vector3f scale = vector(gui, "scale", 1);
		translation.set(Math.clamp(translation.x, -5, 5), Math.clamp(translation.y, -5, 5), Math.clamp(translation.z, -5, 5));
		scale.set(Math.clamp(scale.x, -4, 4), Math.clamp(scale.y, -4, 4), Math.clamp(scale.z, -4, 4));
		return new Matrix4f().translation(translation)
				.rotateXYZ(rotation.x, rotation.y, rotation.z).scale(scale)
				.translate(-0.5f, -0.5f, -0.5f);
	}

	private static Vector3f vector(JsonNode gui, String key, float fallback) {
		JsonNode value = gui.get(key);
		return value == null ? new Vector3f(fallback)
				: new Vector3f(value.get(0).floatValue(), value.get(1).floatValue(), value.get(2).floatValue());
	}
}
