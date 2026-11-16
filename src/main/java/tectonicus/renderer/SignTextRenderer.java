/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */
package tectonicus.renderer;

import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.rasteriser.Texture;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.raw.SignEntity;
import tectonicus.raw.SignTextColor;
import tectonicus.util.Colour4f;
import tectonicus.world.Colors;

import java.text.Bidi;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tectonicus.Version.VERSION_RV;

/** Places the Minecraft font on the front and back planes of a sign board. */
public final class SignTextRenderer {
	private static final float DEPTH_OFFSET = 0.001f;
	private static final Colour4f FULL_BRIGHT = new Colour4f(1, 1, 1, 1);

	private SignTextRenderer() {}

	public static void addModelText(SignEntity sign, BlockProperties properties, BlockContext world, RawChunk chunk, Geometry geometry) {
		String block = sign.getBlockName();
		boolean hanging = block.endsWith("_hanging_sign") || block.equals("minecraft:hanging_sign");
		boolean wall = block.contains("_wall_") || block.equals("minecraft:wall_sign");
		float rotation = 0;
		if (properties != null && wall && properties.containsKey("facing")) {
			rotation = switch (properties.get("facing")) {
				case "north" -> 180;
				case "west" -> 90;
				case "east" -> 270;
				default -> 0;
			};
		} else if (properties != null && properties.containsKey("rotation")) {
			rotation = Integer.parseInt(properties.get("rotation")) * 22.5f;
		}

		// Vanilla text positions in block coordinates, before rotating the board.
		float centerY = hanging ? 0.9375f - 0.3125f - 0.32f : (wall ? 0.1875f : 0.5f) + 1f / 3f;
		float centerZ = wall && !hanging ? 0.0625f : 0.5f;
		float faceOffset = hanging ? 0.073f : 0.046666667f;
		addText(sign, world, chunk, geometry, hanging, 0.5f, centerY, centerZ + faceOffset, centerZ - faceOffset, rotation);
	}

	public static void addText(SignEntity sign, BlockContext world, RawChunk chunk, Geometry geometry, boolean hanging,
			float centerX, float centerY, float frontZ, float backZ, float rotation) {
		if (sign == null) return;
		var font = world.getTexturePack().getSignFont();
		font.prepare(sign);
		var light = world.getLightSample(chunk, sign.getLocalX(), sign.getLocalY(), sign.getLocalZ(),
				LightFace.Top, world.getNightLightAdjustment()).toColour(world.getLightStyle(), world.useOldGrayscaleLighting());
		var meshes = new HashMap<Texture, SubMesh>();
		boolean aprilFools = world.getTexturePack().getVersion() == VERSION_RV;
		addFace(sign.getFront(), font, meshes, hanging, centerX, centerY, frontZ, 1, light, aprilFools);
		if (sign.getBack() != null) {
			addFace(sign.getBack(), font, meshes, hanging, centerX, centerY, backZ, -1, light, aprilFools);
		}
		meshes.forEach((texture, mesh) -> mesh.pushTo(geometry.getMesh(texture, Geometry.MeshType.AlphaTest),
				sign.getLocalX(), sign.getLocalY(), sign.getLocalZ(), Rotation.AntiClockwise, rotation));
	}

	private static void addFace(SignEntity.Face face, SignFont font, Map<Texture, SubMesh> meshes, boolean hanging,
			float centerX, float centerY, float z, int direction, Colour4f light, boolean aprilFools) {
		Colors dye = Objects.requireNonNullElse(Colors.byName(face.color()), Colors.BLACK);
		int baseColor = face.glowing() ? dye.getTextColor() : dye.getDarkTextColor();
		if (aprilFools) baseColor = 0x32b732;
		Colour4f fillLight = face.glowing() ? FULL_BRIGHT : light;
		Vector4f outline = face.glowing() ? color(dye.getGlowingTextOutline(), FULL_BRIGHT) : null;
		float scale = hanging ? 0.0140625f : 1f / 96f;
		int lineHeight = hanging ? 9 : 10;
		int maxWidth = hanging ? 60 : 90;
		for (int i = 0; i < face.lines().size(); i++) {
			var colors = i < face.textColors().size() ? face.textColors().get(i) : List.<SignTextColor>of();
			var letters = layoutLine(face.lines().get(i), colors, baseColor, font, maxWidth);
			float width = 0;
			for (Letter letter : letters) width += letter.glyph().advance();
			float x = -(int) Math.ceil(width) / 2;
			float y = centerY + (2 - i) * lineHeight * scale;
			for (Letter letter : letters) {
				var glyph = letter.glyph();
				if (glyph.sprite() != null) {
					var mesh = meshes.computeIfAbsent(glyph.sprite().texture, key -> new SubMesh());
					float top = y - glyph.top() * scale;
					if (outline != null) {
						for (int dx = -1; dx <= 1; dx++) {
							for (int dy = -1; dy <= 1; dy++) {
								if (dx != 0 || dy != 0) addGlyph(mesh, glyph, centerX, x + dx, top + dy * scale, z, scale, direction, outline);
							}
						}
					}
					addGlyph(mesh, glyph, centerX, x, top, z + direction * DEPTH_OFFSET, scale, direction, color(letter.rgb(), fillLight));
				}
				x += glyph.advance();
			}
		}
	}

	static List<Letter> layoutLine(String text, List<SignTextColor> colors, int baseColor, SignFont font, int maxWidth) {
		List<Letter> letters = new ArrayList<>();
		int start = 0;
		for (SignTextColor color : colors) {
			appendLetters(text.substring(start, color.start()), baseColor, font, letters);
			appendLetters(text.substring(color.start(), color.end()), color.rgb(), font, letters);
			start = color.end();
		}
		appendLetters(text.substring(start), baseColor, font, letters);
		float width = 0;
		int space = -1;
		int end = letters.size();
		for (int i = 0; i < letters.size(); i++) {
			Letter letter = letters.get(i);
			if (letter.code() == '\n') {
				end = i;
				break;
			}
			if (letter.code() == ' ') space = i;
			width += letter.glyph().advance();
			if (width > maxWidth) {
				end = space > 0 ? space : i;
				break;
			}
		}
		return visualOrder(letters.subList(0, end));
	}

	private static void appendLetters(String text, int rgb, SignFont font, List<Letter> letters) {
		Normalizer.normalize(text, Normalizer.Form.NFC).codePoints().forEach(code -> {
			if (code == '\n') {
				letters.add(new Letter(code, null, rgb));
			} else if (!Character.isISOControl(code) && Character.getType(code) != Character.FORMAT) {
				var glyph = font.getGlyph(code);
				if (glyph != null) letters.add(new Letter(code, glyph, rgb));
			}
		});
	}

	private static List<Letter> visualOrder(List<Letter> letters) {
		StringBuilder text = new StringBuilder();
		for (Letter letter : letters) text.appendCodePoint(letter.code());
		char[] chars = text.toString().toCharArray();
		if (!Bidi.requiresBidi(chars, 0, chars.length)) return letters;
		Bidi bidi = new Bidi(text.toString(), Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
		byte[] levels = new byte[letters.size()];
		int offset = 0;
		for (int i = 0; i < letters.size(); i++) {
			levels[i] = (byte) bidi.getLevelAt(offset);
			offset += Character.charCount(letters.get(i).code());
		}
		Letter[] reordered = letters.toArray(Letter[]::new);
		Bidi.reorderVisually(levels, 0, reordered, 0, reordered.length);
		return List.of(reordered);
	}

	private static Vector4f color(int rgb, Colour4f light) {
		return new Vector4f(((rgb >> 16) & 0xff) / 255f * light.r, ((rgb >> 8) & 0xff) / 255f * light.g,
				(rgb & 0xff) / 255f * light.b, 1);
	}

	private static void addGlyph(SubMesh mesh, SignFont.Glyph glyph, float centerX, float x, float top, float z,
			float scale, int direction, Vector4f color) {
		float left = centerX + x * scale * direction;
		float right = left + glyph.width() * scale * direction;
		float bottom = top - glyph.height() * scale;
		mesh.addQuad(new Vector3f(left, top, z), new Vector3f(right, top, z),
				new Vector3f(right, bottom, z), new Vector3f(left, bottom, z), color, glyph.sprite());
	}

	record Letter(int code, SignFont.Glyph glyph, int rgb) {}
}
