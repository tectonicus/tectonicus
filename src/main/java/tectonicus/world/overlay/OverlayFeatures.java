/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world.overlay;

import lombok.experimental.UtilityClass;
import tectonicus.util.FileUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reads lines, polygons and point markers in Minecraft block coordinates. Points are [x, y, z], [x, z]
 * or {"x":..., "y":..., "z":...}; a point without y is drawn at the overlay height.
 * Only known fields are copied so the viewer never receives arbitrary data.
 */
@UtilityClass
public class OverlayFeatures {
	private static final Pattern COLOR = Pattern.compile("#(?:[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})|[a-zA-Z]{3,20}");

	public ArrayNode read(Path file) throws IOException {
		try (InputStream in = Files.newInputStream(file)) {
			return parse(FileUtils.getOBJECT_MAPPER().readTree(in));
		}
	}

	public ArrayNode parse(JsonNode root) {
		JsonNode items = root != null && root.isObject() ? root.get("features") : root;
		if (items == null || !items.isArray())
			throw new IllegalArgumentException("expected an array of features or an object with a \"features\" array");
		ArrayNode result = FileUtils.getOBJECT_MAPPER().createArrayNode();
		for (int i = 0; i < items.size(); i++) {
			try {
				result.add(parseFeature(items.get(i)));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("feature " + (i + 1) + ": " + e.getMessage());
			}
		}
		return result;
	}

	private ObjectNode parseFeature(JsonNode feature) {
		if (feature == null || !feature.isObject())
			throw new IllegalArgumentException("expected an object");
		String type = text(feature, "type");
		type = type == null ? "" : type.toLowerCase(Locale.ROOT);
		ObjectNode result = FileUtils.getOBJECT_MAPPER().createObjectNode();
		switch (type) {
			case "line", "polyline" -> {
				result.put("type", "line");
				result.set("points", points(feature.get("points"), 2));
			}
			case "polygon" -> {
				result.put("type", "polygon");
				result.set("points", points(feature.get("points"), 3));
			}
			case "marker", "point" -> {
				result.put("type", "marker");
				JsonNode point = feature.has("point") ? feature.get("point") : feature.get("position");
				result.set("point", point(point));
			}
			default -> throw new IllegalArgumentException("type must be line, polygon or marker");
		}
		copyText(feature, result, "name");
		copyText(feature, result, "description");
		copyColor(feature, result, "color");
		copyColor(feature, result, "fillColor");
		copyNumber(feature, result, "width", 0, 50);
		copyNumber(feature, result, "opacity", 0, 1);
		copyNumber(feature, result, "fillOpacity", 0, 1);
		return result;
	}

	private ArrayNode points(JsonNode points, int minimum) {
		if (points == null || !points.isArray() || points.size() < minimum)
			throw new IllegalArgumentException("needs at least " + minimum + " points");
		ArrayNode result = FileUtils.getOBJECT_MAPPER().createArrayNode();
		for (JsonNode point : points)
			result.add(point(point));
		return result;
	}

	/** Normalizes a point to [x, y, z] or [x, null, z]. */
	private ArrayNode point(JsonNode point) {
		ArrayNode result = FileUtils.getOBJECT_MAPPER().createArrayNode();
		if (point != null && point.isArray() && (point.size() == 2 || point.size() == 3)) {
			result.add(number(point.get(0)));
			if (point.size() == 3) result.add(number(point.get(1))); else result.addNull();
			result.add(number(point.get(point.size() - 1)));
		} else if (point != null && point.isObject() && point.has("x") && point.has("z")) {
			result.add(number(point.get("x")));
			if (point.has("y")) result.add(number(point.get("y"))); else result.addNull();
			result.add(number(point.get("z")));
		} else {
			throw new IllegalArgumentException("a point must be [x, y, z], [x, z] or {\"x\", \"y\", \"z\"}");
		}
		return result;
	}

	private double number(JsonNode node) {
		if (node == null || !node.isNumber() || !Double.isFinite(node.asDouble()))
			throw new IllegalArgumentException("coordinates must be numbers");
		return node.asDouble();
	}

	private String text(JsonNode feature, String field) {
		JsonNode node = feature.get(field);
		return node != null && node.isString() ? node.asString() : null;
	}

	private void copyText(JsonNode feature, ObjectNode result, String field) {
		String value = text(feature, field);
		if (value != null && !value.isBlank())
			result.put(field, value);
	}

	private void copyColor(JsonNode feature, ObjectNode result, String field) {
		String value = text(feature, field);
		if (value == null)
			return;
		if (!COLOR.matcher(value).matches())
			throw new IllegalArgumentException(field + " must be a #hex color or a color name");
		result.put(field, value);
	}

	private void copyNumber(JsonNode feature, ObjectNode result, String field, double min, double max) {
		JsonNode node = feature.get(field);
		if (node == null)
			return;
		if (!node.isNumber() || node.asDouble() < min || node.asDouble() > max)
			throw new IllegalArgumentException(field + " must be a number from " + min + " to " + max);
		result.put(field, node.asDouble());
	}
}
