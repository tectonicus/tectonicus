/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.ItemRenderer;
import tectonicus.ItemRenderer.ItemContext;
import tectonicus.blockregistry.BlockModel;
import tectonicus.blockregistry.BlockRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.itemmodeldefinitionregistry.ItemModelDefinition;
import tectonicus.itemregistry.ExtrudedItemModel;
import tectonicus.itemregistry.ItemGuiTransform;
import tectonicus.itemregistry.ItemLayers;
import tectonicus.itemregistry.PosedGeometry;
import tectonicus.itemregistry.ShieldModel;
import tectonicus.raw.BannerPatternsTag;
import tectonicus.raw.BedEntity;
import tectonicus.raw.BiomesOld;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.DyedColorTag;
import tectonicus.raw.Item;
import tectonicus.raw.ItemFrameEntity;
import tectonicus.raw.LegacyItems;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.Colour4f;
import tectonicus.util.FileUtils;
import tectonicus.util.LegacyItemIcons;
import tectonicus.world.Colors;
import tools.jackson.databind.JsonNode;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static tectonicus.Version.VERSION_13;

/**
 * Draws the item held by an item frame, posed like Minecraft's item frame renderer: half size, in front of the
 * frame's back, turned in eighth turns, using the item model's "fixed" display transform.
 */
@Slf4j
final class ItemFrameItems {
	private static final float UNIT = 1 / 16f;
	/** Leather armor that has not been dyed */
	private static final int DEFAULT_LEATHER_COLOR = 0xA06540;
	/** Water bottles, and potions whose colour Tectonicus does not know */
	private static final int DEFAULT_POTION_COLOR = 0x385DC6;
	private static final Set<String> LEGACY_CHESTS = Set.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:ender_chest");

	/** Draws a resolved item model in its own model space, after its display transform has been applied. */
	@FunctionalInterface
	private interface ItemDrawer {
		void draw(Geometry geometry);
	}

	/** One model of an item, drawn after its display transform */
	private record Part(Matrix4f display, ItemDrawer drawer) { }

	private record ResolvedItem(List<Part> parts) {
		static ResolvedItem of(Matrix4f display, ItemDrawer drawer) {
			return new ResolvedItem(List.of(new Part(display, drawer)));
		}
	}

	private final Map<String, Optional<ResolvedItem>> items = new HashMap<>();
	private final Set<String> failedItems = new HashSet<>();

	/** Adds the frame's item to the chunk geometry. Frames holding maps are drawn by the frame itself. */
	void add(ItemFrameEntity frame, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry) {
		Item item = frame.getItem();
		if (item == null)
			return;
		ResolvedItem resolved;
		try {
			resolved = resolve(item, world, registry).orElse(null);
		} catch (Exception e) {
			if (failedItems.add(item.id))
				log.warn("Unable to draw {} in an item frame", item.id, e);
			return;
		}
		if (resolved == null)
			return;

		int x = frame.getLocalX(), y = frame.getLocalY(), z = frame.getLocalZ();
		PosedGeometry.Shading shading = frame.isGlow() ? normal -> 1
				: shading(world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top),
						world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.NorthSouth),
						world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.EastWest));
		Matrix4f transform = frameTransform(x, y, z, frame.getDirection(), frame.getItemRotation(), frame.isInvisible());
		for (Part part : resolved.parts())
			part.drawer().draw(new PosedGeometry(geometry, new Matrix4f(transform).mul(part.display()), shading));
	}

	/**
	 * Places the item like Minecraft's ItemFrameRenderer: the frame faces along local -Z, the item sits 7/16 behind
	 * the block's centre (8/16 in an invisible frame, with no back to lie on), turned around the frame's normal and
	 * drawn at half size.
	 */
	static Matrix4f frameTransform(float x, float y, float z, int direction, int itemRotation, boolean invisible) {
		Matrix4f transform = new Matrix4f().translation(x + 0.5f, y + 0.5f, z + 0.5f);
		switch (direction) {
			case 0 -> transform.rotateX((float) Math.toRadians(90));  // Facing down
			case 1 -> transform.rotateX((float) Math.toRadians(-90)); // Facing up
			default -> { }
		}
		// Minecraft turns frames by 180 degrees less their yaw: south 0, west 90, north 180, east 270.
		float yaw = switch (direction) {
			case 2 -> 180;
			case 4 -> 90;
			case 5 -> 270;
			default -> 0;
		};
		return transform.rotateY((float) Math.toRadians(180 - yaw))
				.translate(0, 0, invisible ? 0.5f : 7 * UNIT)
				.rotateZ((float) Math.toRadians(itemRotation * 45))
				.scale(0.5f);
	}

	/** Shades item faces with the light of the frame's block, weighted by how much each face points along each axis. */
	static PosedGeometry.Shading shading(float topLight, float northSouthLight, float eastWestLight) {
		return normal -> normal.x() * normal.x() * eastWestLight + normal.y() * normal.y() * topLight
				+ normal.z() * normal.z() * northSouthLight;
	}

	private Optional<ResolvedItem> resolve(Item item, BlockContext world, BlockTypeRegistry registry) throws Exception {
		String key = cacheKey(item);
		Optional<ResolvedItem> resolved = items.get(key);
		if (resolved == null) {
			TexturePack texturePack = world.getTexturePack();
			resolved = Optional.ofNullable(texturePack.getVersion().getNumVersion() >= VERSION_13.getNumVersion()
					? resolveModern(item, key, texturePack, world.getModelRegistry(), registry)
					: resolveLegacy(item, key, texturePack, world.getModelRegistry(), registry));
			if (resolved.isEmpty())
				log.debug("No model to draw {} in an item frame", item.id);
			items.put(key, resolved);
		}
		return resolved;
	}

	private static String cacheKey(Item item) {
		DyedColorTag dye = item.getComponent(DyedColorTag.class);
		BannerPatternsTag patterns = item.getComponent(BannerPatternsTag.class);
		return item.id + (dye == null ? "" : "#" + Integer.toHexString(dye.color))
				+ (patterns == null ? "" : patterns.toString());
	}

	// Jars from 1.13 on: item model definitions (1.21.4+) or item models

	private ResolvedItem resolveModern(Item item, String key, TexturePack texturePack, BlockRegistry modelRegistry,
			BlockTypeRegistry registry) throws Exception {
		if (!item.id.startsWith("minecraft:"))
			return null;
		String name = item.id.substring("minecraft:".length());
		ItemModelDefinition definition = loadDefinition(texturePack, name);
		if (definition != null)
			return resolveDefinition(item, name, key, definition.getModelForContext("fixed"), texturePack, modelRegistry, registry);
		String modelName = "minecraft:item/" + name;
		if (!texturePack.fileExists("assets/minecraft/models/item/" + name + ".json"))
			return null;
		if (isBuiltinEntity(texturePack, modelName))
			return resolveSpecial(item, name, modelName, null, texturePack, modelRegistry, registry);
		Part part = resolveModel(item, name, key, modelName, null, texturePack, modelRegistry, registry);
		return part == null ? null : new ResolvedItem(List.of(part));
	}

	/** Resolves a 1.21.4+ item model definition, as chosen for the "fixed" display context. */
	private ResolvedItem resolveDefinition(Item item, String name, String key, ItemModelDefinition model, TexturePack texturePack,
			BlockRegistry modelRegistry, BlockTypeRegistry registry) throws Exception {
		String type = model.getType() == null ? "" : Strings.CI.removeStart(model.getType(), "minecraft:");
		if (type.equals("special"))
			return resolveSpecial(item, name, model.getBase(), model.getInnerModel() == null ? null : model.getInnerModel().getType(),
					texturePack, modelRegistry, registry);
		if (type.equals("composite") && model.getModels() != null) {
			// For example beds since 26.2: the head and foot models, the foot moved one block south.
			List<Part> parts = new ArrayList<>();
			for (int i = 0; i < model.getModels().size(); i++) {
				ResolvedItem resolved = resolveDefinition(item, name, key + "/" + i,
						model.getModels().get(i).getModelForContext("fixed"), texturePack, modelRegistry, registry);
				if (resolved != null)
					parts.addAll(resolved.parts());
			}
			return parts.isEmpty() ? null : new ResolvedItem(List.copyOf(parts));
		}
		if (model.getModelName() == null)
			return null;
		Part part = resolveModel(item, name, key, model.getModelName(), model.getTints(), texturePack, modelRegistry, registry);
		if (part == null)
			return null;
		Matrix4f transformation = transformation(model.getTransformation());
		return new ResolvedItem(List.of(transformation == null ? part
				: new Part(new Matrix4f(part.display()).mul(transformation), part.drawer())));
	}

	/** Resolves an item model or block model: a flat model with layer textures, or one with elements. */
	private static Part resolveModel(Item item, String name, String key, String modelName, List<Map<String, Object>> tints,
			TexturePack texturePack, BlockRegistry modelRegistry, BlockTypeRegistry registry) throws Exception {
		Map<String, String> layers = generatedLayers(texturePack, modelName);
		if (layers != null) {
			if (layers.isEmpty())
				return null;
			List<BufferedImage> images = new ArrayList<>();
			for (var layer : layers.entrySet())
				images.add(ItemLayers.load(texturePack, modelRegistry, layer.getKey(), layer.getValue()));
			List<Integer> layerTints = new ArrayList<>();
			for (int i = 0; i < images.size(); i++)
				layerTints.add(tints != null ? definitionTint(tints, i, item, texturePack) : namedTint(name, i, item));
			return flatItem(key, images, layerTints, display(texturePack, modelName, generatedDisplay()), texturePack).parts().getFirst();
		}

		BlockModel model = modelRegistry.loadModel(modelName, "", new HashMap<>(), null);
		if (model == null || model.getElements() == null || model.getElements().isEmpty())
			return null;
		var baked = modelRegistry.getBakedModel(model, 0, 0, 0, false);
		ItemContext context = new ItemContext(texturePack, registry, modelRegistry);
		return new Part(display(texturePack, modelName, blockDisplay()),
				geometry -> baked.createGeometry(0, 0, 0, context, fullyLitChunk(), geometry));
	}

	/**
	 * @param transformation a model transformation: translation, left_rotation, scale and right_rotation (rotations as
	 *                       quaternions or axis and angle), or 16 matrix values in row-major order
	 * @return the transformation, or null for none
	 */
	static Matrix4f transformation(Object transformation) {
		if (transformation instanceof List<?> values && values.size() == 16) {
			float[] m = new float[16];
			for (int i = 0; i < 16; i++)
				m[i] = values.get(i) instanceof Number n ? n.floatValue() : 0;
			return new Matrix4f().setTransposed(m);
		}
		if (!(transformation instanceof Map<?, ?> map))
			return null;
		float[] translation = floats(map.get("translation"), 0, 0, 0);
		float[] scale = floats(map.get("scale"), 1, 1, 1);
		return new Matrix4f().translation(translation[0], translation[1], translation[2])
				.rotate(rotation(map.get("left_rotation")))
				.scale(scale[0], scale[1], scale[2])
				.rotate(rotation(map.get("right_rotation")));
	}

	private static Quaternionf rotation(Object value) {
		if (value instanceof Map<?, ?> axisAngle) {
			float[] axis = floats(axisAngle.get("axis"), 0, 1, 0);
			float angle = axisAngle.get("angle") instanceof Number n ? n.floatValue() : 0;
			return new Quaternionf().setAngleAxis(angle, axis[0], axis[1], axis[2]);
		}
		float[] q = floats(value, 0, 0, 0, 1);
		return new Quaternionf(q[0], q[1], q[2], q[3]);
	}

	private static float[] floats(Object value, float... defaults) {
		float[] result = defaults.clone();
		if (value instanceof List<?> list && list.size() == defaults.length) {
			for (int i = 0; i < result.length; i++)
				if (list.get(i) instanceof Number n)
					result[i] = n.floatValue();
		}
		return result;
	}

	private ResolvedItem resolveSpecial(Item item, String name, String baseModel, String specialType, TexturePack texturePack,
			BlockRegistry modelRegistry, BlockTypeRegistry registry) throws Exception {
		String blockName = "minecraft:" + name;
		if (baseModel == null)
			baseModel = "minecraft:item/" + name;
		Matrix4f display = display(texturePack, baseModel, blockDisplay());

		if (name.endsWith("_bed"))
			return bed(name, display, texturePack, modelRegistry, registry);

		if (registry != null && !(registry.find(blockName) instanceof Air)) {
			BannerPatternsTag patterns = item.getComponent(BannerPatternsTag.class);
			List<Banner.Pattern> bannerPatterns = patterns == null ? List.of() : patterns.patterns();
			return ResolvedItem.of(display, geometry -> ItemRenderer.addSpecialItem(geometry, registry, modelRegistry,
					texturePack, blockName, bannerPatterns));
		}
		if (modelRegistry.getBlock(blockName) != null) {
			Matrix4f blockTransform = ItemRenderer.blockStateItemTransform(blockName);
			if (blockTransform != null)
				display.mul(blockTransform);
			return ResolvedItem.of(display, geometry -> ItemRenderer.addBlockStateItem(geometry, modelRegistry, texturePack, blockName));
		}
		if ("minecraft:shield".equals(specialType) || "shield".equals(specialType) || name.equals("shield")) {
			// The same placement as the inventory icon: ShieldSpecialRenderer's model in item space.
			Matrix4f shield = new Matrix4f().scale(1, -1, -1).translate(-0.5f, 0.75f, -9 / 16f).scale(1, -1, 1);
			return ResolvedItem.of(display, geometry -> ShieldModel.addGeometry(modelRegistry, texturePack, geometry, shield, new Vector4f(1)));
		}
		return null;
	}

	/** Beds are two blocks long: draw the head and foot, centred on the item's origin. */
	private static ResolvedItem bed(String name, Matrix4f display, TexturePack texturePack, BlockRegistry modelRegistry,
			BlockTypeRegistry registry) throws Exception {
		display.translate(0, 0, -0.5f);
		String modelPrefix = "minecraft:block/" + Strings.CI.removeEnd(name, "_bed") + "_bed_";
		BlockModel head = modelRegistry.loadModel(modelPrefix + "head", "", new HashMap<>(), null);
		BlockModel foot = modelRegistry.loadModel(modelPrefix + "foot", "", new HashMap<>(), null);
		ItemContext context = new ItemContext(texturePack, registry, modelRegistry);
		if (head != null && foot != null && head.getElements() != null && !head.getElements().isEmpty()) {
			// Bed block models (26.2+)
			var bakedHead = modelRegistry.getBakedModel(head, 0, 0, 0, false);
			var bakedFoot = modelRegistry.getBakedModel(foot, 0, 0, 0, false);
			return ResolvedItem.of(display, geometry -> {
				RawChunk chunk = fullyLitChunk();
				bakedHead.createGeometry(0, 0, 0, context, chunk, geometry);
				bakedFoot.createGeometry(0, 0, 1, context, chunk, geometry);
			});
		}

		BlockType type = registry == null ? null : registry.find("minecraft:" + name);
		if (type == null || type instanceof Air)
			return null;
		Colors color = Colors.byName(Strings.CI.removeEnd(name, "_bed"));
		int colorId = color == null ? Colors.RED.getId() : color.getId();
		return ResolvedItem.of(display, geometry -> {
			RawChunk chunk = fullyLitChunk();
			Map<String, BedEntity> beds = new HashMap<>();
			for (int z = 0; z < 2; z++) {
				chunk.setBlockName(0, 0, z, "minecraft:" + name);
				chunk.setBlockId(0, 0, z, BlockIds.BED);
				chunk.setBlockData(0, 0, z, (byte) (z == 0 ? 10 : 6));
				chunk.setBlockState(0, 0, z, new BlockProperties(Map.of("part", z == 0 ? "head" : "foot", "facing", "north")));
				chunk.setBlockLight(0, 0, z, (byte) 16);
				chunk.setSkyLight(0, 0, z, (byte) 16);
				beds.put("x0y0z" + z, new BedEntity(0, 0, z, 0, 0, z, colorId));
			}
			chunk.setBeds(beds);
			type.addInteriorGeometry(0, 0, 0, context, registry, chunk, geometry);
			type.addInteriorGeometry(0, 0, 1, context, registry, chunk, geometry);
		});
	}

	private static ItemModelDefinition loadDefinition(TexturePack texturePack, String name) throws IOException {
		String path = "assets/minecraft/items/" + name + ".json";
		if (!texturePack.fileExists(path))
			return null;
		try (InputStream in = texturePack.getZipStack().getStream(path)) {
			return in == null ? null : FileUtils.getOBJECT_MAPPER().readValue(in, ItemModelDefinition.class);
		}
	}

	private static boolean isBuiltinEntity(TexturePack texturePack, String modelName) throws IOException {
		for (JsonNode model : modelChain(texturePack, modelName)) {
			if (parent(model).endsWith("builtin/entity"))
				return true;
		}
		return false;
	}

	/**
	 * @return the layer textures of a flat item model by texture variable ("layer0" first), an empty map for a flat
	 * model without textures, or null if the model is not flat
	 */
	static Map<String, String> generatedLayers(TexturePack texturePack, String modelName) throws IOException {
		Map<String, String> layers = null;
		for (JsonNode model : modelChain(texturePack, modelName)) {
			JsonNode textures = model.path("textures");
			if (layers == null && textures.has("layer0")) {
				layers = new LinkedHashMap<>();
				for (int i = 0; textures.has("layer" + i); i++)
					layers.put("layer" + i, textures.path("layer" + i).asString());
			}
			if (model.has("elements"))
				return null;
			if (parent(model).endsWith("builtin/generated"))
				return layers == null ? Map.of() : layers;
		}
		return null;
	}

	/** @return the model and its parents, nearest first */
	private static List<JsonNode> modelChain(TexturePack texturePack, String modelName) throws IOException {
		List<JsonNode> chain = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		String model = modelName;
		while (model != null && visited.add(model) && chain.size() < 32) {
			String[] id = model.split(":", 2);
			String path = "assets/" + (id.length == 2 ? id[0] : "minecraft") + "/models/" + id[id.length - 1] + ".json";
			JsonNode json;
			try (InputStream in = texturePack.getZipStack().getStream(path)) {
				if (in == null)
					break;
				json = FileUtils.getOBJECT_MAPPER().readTree(in);
			}
			chain.add(json);
			model = parent(json).isEmpty() ? null : parent(json);
		}
		return chain;
	}

	private static String parent(JsonNode model) {
		JsonNode parent = model.path("parent");
		return parent.isString() ? parent.asString() : "";
	}

	/** @return the colour multiplied into a flat item layer from a 1.21.4+ tint source, or -1 for none */
	private static int definitionTint(List<Map<String, Object>> tints, int layer, Item item, TexturePack texturePack) {
		if (layer >= tints.size() || tints.get(layer) == null)
			return -1;
		Map<String, Object> tint = tints.get(layer);
		String type = Strings.CI.removeStart(String.valueOf(tint.get("type")), "minecraft:");
		switch (type) {
			case "dye" -> {
				DyedColorTag dye = item.getComponent(DyedColorTag.class);
				if (dye != null)
					return dye.color & 0xFFFFFF;
			}
			case "constant" -> {
				return color(tint.get("value"));
			}
			case "grass" -> {
				return texturePack.getGrassColor(BiomesOld.FOREST).toArgb() & 0xFFFFFF;
			}
			default -> { }
		}
		return color(tint.get("default"));
	}

	private static int color(Object value) {
		if (value instanceof Number number)
			return number.intValue() & 0xFFFFFF;
		if (value instanceof List<?> rgb && rgb.size() >= 3 && rgb.get(0) instanceof Number r
				&& rgb.get(1) instanceof Number g && rgb.get(2) instanceof Number b)
			return Math.round(r.floatValue() * 255) << 16 | Math.round(g.floatValue() * 255) << 8 | Math.round(b.floatValue() * 255);
		return -1;
	}

	/** @return the colour Minecraft multiplies into a flat item layer before 1.21.4 for the few items Tectonicus can colour */
	private static int namedTint(String name, int layer, Item item) {
		if (layer != 0)
			return -1;
		if (name.startsWith("leather_")) {
			DyedColorTag dye = item.getComponent(DyedColorTag.class);
			return dye == null ? DEFAULT_LEATHER_COLOR : dye.color & 0xFFFFFF;
		}
		if (name.endsWith("potion") || name.equals("tipped_arrow"))
			return DEFAULT_POTION_COLOR;
		return -1;
	}

	private static ResolvedItem flatItem(String key, List<BufferedImage> layers, List<Integer> tints, Matrix4f display,
			TexturePack texturePack) {
		BufferedImage sprite = composite(layers, tints);
		SubTexture texture = texturePack.findTexture(sprite, "tectonicus:item_frame_item/" + key);
		ExtrudedItemModel model = new ExtrudedItemModel(sprite);
		return ResolvedItem.of(display, geometry -> model.addGeometry(geometry, texture, new Vector4f(1)));
	}

	static BufferedImage composite(List<BufferedImage> layers, List<Integer> tints) {
		int width = layers.getFirst().getWidth();
		int height = layers.getFirst().getHeight();
		for (BufferedImage layer : layers) {
			width = Math.max(width, layer.getWidth());
			height = Math.max(height, layer.getHeight());
		}
		BufferedImage sprite = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		var graphics = sprite.createGraphics();
		try {
			for (int i = 0; i < layers.size(); i++) {
				BufferedImage layer = layers.get(i);
				int tint = i < tints.size() ? tints.get(i) : -1;
				if (tint != -1) {
					BufferedImage tinted = new BufferedImage(layer.getWidth(), layer.getHeight(), BufferedImage.TYPE_INT_ARGB);
					tinted.getGraphics().drawImage(layer, 0, 0, null);
					ItemLayers.tint(tinted, new Colour4f(0xFF000000 | tint));
					layer = tinted;
				}
				graphics.drawImage(layer, 0, 0, width, height, null);
			}
		} finally {
			graphics.dispose();
		}
		return sprite;
	}

	// Jars before 1.13: numeric block types and 1.8 to 1.12 item models, found through the 1.13 item ID

	private ResolvedItem resolveLegacy(Item item, String key, TexturePack texturePack, BlockRegistry modelRegistry,
			BlockTypeRegistry registry) throws Exception {
		LegacyItems.Entry entry = LegacyItems.get().item(item.id);
		if (entry == null)
			return null;
		String name = item.id.substring(item.id.indexOf(':') + 1);
		String modelName = entry.model() == null ? null : "minecraft:item/" + entry.model();

		List<BufferedImage> layers = LegacyItemIcons.flatItemLayers(texturePack, entry, name);
		if (!layers.isEmpty()) {
			List<Integer> tints = new ArrayList<>();
			for (int i = 0; i < layers.size(); i++)
				tints.add(namedTint(name, i, item));
			Matrix4f display = modelName == null ? generatedDisplay() : display(texturePack, modelName, generatedDisplay());
			return flatItem(key, layers, tints, display, texturePack);
		}

		if (!entry.isBlock() || registry == null)
			return null;
		// Chest blocks before 1.13 store their facing as data. Face north, like the 1.13+ item models.
		int data = LEGACY_CHESTS.contains(entry.legacyId()) ? 2 : entry.damage();
		BlockType type = registry.find(entry.numericId(), data);
		if (type == null || type instanceof Air)
			return null;
		Matrix4f display = modelName == null ? blockDisplay() : display(texturePack, modelName, blockDisplay());
		ItemContext context = new ItemContext(texturePack, registry, modelRegistry);
		return ResolvedItem.of(display, geometry -> {
			RawChunk chunk = fullyLitChunk();
			chunk.setBlockId(0, 0, 0, entry.numericId());
			chunk.setBlockData(0, 0, 0, (byte) data);
			type.addInteriorGeometry(0, 0, 0, context, registry, chunk, geometry);
		});
	}

	// Display transforms

	private static Matrix4f display(TexturePack texturePack, String modelName, Matrix4f fallback) throws IOException {
		Matrix4f fixed = ItemGuiTransform.loadDisplay(texturePack.getZipStack(), modelName, "fixed");
		return fixed == null ? fallback : fixed;
	}

	/** {@code item/generated}'s fixed transform, for models without one: turned to face out of the frame. */
	static Matrix4f generatedDisplay() {
		return new Matrix4f().rotateY((float) Math.PI).translate(-0.5f, -0.5f, -0.5f);
	}

	/** {@code block/block}'s fixed transform, for models without one: half size. */
	static Matrix4f blockDisplay() {
		return new Matrix4f().scale(0.5f).translate(-0.5f, -0.5f, -0.5f);
	}

	private static RawChunk fullyLitChunk() {
		RawChunk chunk = new RawChunk();
		chunk.setBlockLight(0, 0, 0, (byte) 16);
		chunk.setSkyLight(0, 0, 0, (byte) 16);
		return chunk;
	}
}
