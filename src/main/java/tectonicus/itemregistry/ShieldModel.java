/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.itemregistry;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.blockTypes.Banner;
import tectonicus.blockregistry.BlockModel;
import tectonicus.blockregistry.BlockRegistry;
import tectonicus.blockregistry.ModelTexture;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Texture;
import tectonicus.raw.BannerPatternsTag;
import tectonicus.raw.BaseColorTag;
import tectonicus.raw.Item;
import tectonicus.renderer.Geometry;
import tectonicus.texture.PackTexture;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.world.Colors;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tectonicus.Version.VERSION_15;

@Slf4j
@UtilityClass
public class ShieldModel {
	public static Matrix4f handTransform(boolean leftHand) {
		float unit = 1 / 16f;
		return new Matrix4f()
				// Convert the posed arm back to Minecraft's model axes for ItemInHandLayer.
				.scale(1, -1, -1)
				.rotateX((float) -Math.PI / 2).rotateY((float) Math.PI)
				.translate((leftHand ? -1 : 1) * unit, 2 * unit, -10 * unit)
				// shield.json thirdperson transforms; ItemTransform mirrors X translation and Y rotation on the left.
				.translate((leftHand ? -10 : 10) * unit, 6 * unit, (leftHand ? 12 : -4) * unit)
				.rotateY((float) (leftHand ? -Math.PI / 2 : Math.PI / 2))
				.translate(-0.5f, -0.5f, -0.5f)
				// ShieldSpecialRenderer's default transform, followed by our shared model's native-model conversion.
				.scale(1, -1, -1)
				.translate(-8 * unit, 12 * unit, -9 * unit).scale(1, -1, 1);
	}

	public static BlockModel getModel(BlockRegistry registry, TexturePack textures) {
		return registry.getBlockModels().get("tectonicus:shield", key -> {
			String texture = textures.fileExists("assets/minecraft/textures/entity/shield/shield_base_nopattern.png")
					? "minecraft:entity/shield/shield_base_nopattern" : "minecraft:entity/shield_base_nopattern";
			try {
				return registry.loadModel("minecraft:item/shield_inventory", "", new HashMap<>(Map.of("shield", new ModelTexture(texture))), null);
			} catch (Exception e) {
				log.error("Unable to load shield model", e);
				return null;
			}
		});
	}

	/** The banner design of a shield made with a banner. */
	public record Design(Colors baseColor, List<Banner.Pattern> patterns) {
		public Design {
			patterns = List.copyOf(patterns);
		}

		/** @return the design of a shield item, or null if the shield was not made with a banner */
		public static Design of(Item item) {
			if (item == null)
				return null;
			BaseColorTag base = item.getComponent(BaseColorTag.class);
			BannerPatternsTag patterns = item.getComponent(BannerPatternsTag.class);
			boolean hasPatterns = patterns != null && !patterns.patterns().isEmpty();
			if (base == null && !hasPatterns)
				return null;
			// Like ShieldSpecialRenderer, a shield with patterns but no base color has a white field.
			return new Design(base == null ? Colors.WHITE : base.color(), hasPatterns ? patterns.patterns() : List.of());
		}
	}

	public static void addGeometry(BlockRegistry registry, TexturePack textures, Geometry geometry, Matrix4f transform, Vector4f colour) {
		addGeometry(registry, textures, geometry, transform, colour, null);
	}

	public static void addGeometry(BlockRegistry registry, TexturePack textures, Geometry geometry, Matrix4f transform,
			Vector4f colour, Design design) {
		BlockModel model = getModel(registry, textures);
		if (model == null)
			return;
		// The model uses the plain shield texture. A shield made with a banner has the same layout, with the
		// base color and patterns drawn on the front of the plate.
		SubTexture patterned = design == null ? null : patternedTexture(textures, design);
		// Held items share icon quads and UVs, with entity lighting and no block-face culling.
		boolean reverseWinding = transform.determinant3x3() < 0;
		for (var quad : registry.getBakedModel(model, 0, 0, 0, false).getQuads()) {
			var face = quad.getFace();
			Geometry.MeshType meshType = face.isTranslucent() ? Geometry.MeshType.Transparent
					: face.isTransparent() ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid;
			Texture texture = patterned == null ? face.getTexture().texture : patterned.texture;
			Mesh mesh = geometry.getMesh(texture, meshType);
			for (int index = 0; index < 4; index++) {
				int vertex = reverseWinding && index != 0 ? 4 - index : index;
				Vector3f position = new Vector3f(quad.getPosition(vertex, 0), quad.getPosition(vertex, 1), quad.getPosition(vertex, 2));
				transform.transformPosition(position);
				float u = quad.getUv(vertex, 0);
				float v = quad.getUv(vertex, 1);
				if (patterned != null) {
					u = patterned.u0 + u * (patterned.u1 - patterned.u0);
					v = patterned.v0 + v * (patterned.v1 - patterned.v0);
				}
				mesh.addVertex(position, colour, u, v);
			}
		}
	}

	/** @return the shield texture with the design's base color and patterns, or null if the textures are missing */
	public static SubTexture patternedTexture(TexturePack textures, Design design) {
		Map<String, BufferedImage> images = textures.getShieldPatternImages();
		BufferedImage base = images.get("shieldBase");
		if (base == null)
			return null;

		StringBuilder key = new StringBuilder("shield_base_").append(design.baseColor().getName());
		for (Banner.Pattern pattern : design.patterns())
			key.append('/').append(pattern.getPattern()).append('=').append(pattern.getColor().getName());
		PackTexture existing = textures.getPackTexture(key.toString());
		if (existing != null)
			return existing.getFullTexture();

		int width = base.getWidth();
		int height = base.getHeight();
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
		g.drawImage(base, 0, 0, null);
		// The base color is drawn like the Field pattern, under the other patterns.
		boolean legacy = textures.getVersion().getNumVersion() < VERSION_15.getNumVersion();
		addPattern(base, images.get("base"), design.baseColor(), legacy, g);
		for (Banner.Pattern pattern : design.patterns()) {
			// Field uses the base texture and was saved as "b" before 1.20.5.
			String patternId = pattern.getPattern().equals("b") ? "base" : pattern.getPattern().replace("minecraft:", "");
			BufferedImage patternImage = images.get(patternId);
			if (patternImage == null) {
				log.warn("Missing shield pattern: {}", pattern.getPattern());
				continue;
			}
			addPattern(base, patternImage, pattern.getColor(), legacy, g);
		}
		g.dispose();
		return textures.findTexture(image, key.toString());
	}

	private static void addPattern(BufferedImage base, BufferedImage pattern, Colors color, boolean legacy, Graphics2D g) {
		if (pattern == null)
			return;
		pattern = scaled(pattern, base.getWidth(), base.getHeight());
		if (!legacy) {
			Banner.addPattern(pattern, color.getColor(), g);
			return;
		}

		// Before 1.15, patterns were grayscale masks over the dyed base texture.
		int dye = color.getColor().getRGB();
		BufferedImage layer = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < base.getHeight(); y++) {
			for (int x = 0; x < base.getWidth(); x++) {
				int mask = pattern.getRGB(x, y);
				if ((mask >>> 24) == 0)
					continue;
				int pixel = base.getRGB(x, y);
				int red = ((pixel >>> 16) & 0xff) * ((dye >>> 16) & 0xff) / 255;
				int green = ((pixel >>> 8) & 0xff) * ((dye >>> 8) & 0xff) / 255;
				int blue = (pixel & 0xff) * (dye & 0xff) / 255;
				layer.setRGB(x, y, ((mask & 0xff) << 24) | (red << 16) | (green << 8) | blue);
			}
		}
		g.drawImage(layer, 0, 0, null);
	}

	/** Resource packs can have shield and pattern textures with different resolutions. */
	private static BufferedImage scaled(BufferedImage image, int width, int height) {
		if (image.getWidth() == width && image.getHeight() == height)
			return image;
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = result.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(image, 0, 0, width, height, null);
		g.dispose();
		return result;
	}
}
