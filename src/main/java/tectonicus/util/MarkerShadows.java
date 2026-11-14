/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */

package tectonicus.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import tectonicus.raw.ContainerKind;
import tectonicus.world.Colors;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@UtilityClass
class MarkerShadows {
	// Display at half resolution, with the shadow canvas starting at (-10, -12) relative to the icon.
	private static final int WIDTH = 80;
	private static final int HEIGHT = 54;
	private static final double CAST_ROTATION = Math.toRadians(10);
	private static final double CAST_X = 0.78 * Math.cos(CAST_ROTATION) - 0.65 * Math.sin(CAST_ROTATION);
	private static final double CAST_Y = -0.78 * Math.sin(CAST_ROTATION) - 0.65 * Math.cos(CAST_ROTATION);
	private static final double BLOCK_EDGE_X = 0.49;
	private static final double BLOCK_EDGE_Y = 0.21;
	// Halfway between the steeper cast and the lower-right edge.
	private static final double BLOCK_CAST_ANGLE = (Math.atan2(-CAST_Y, CAST_X)
			+ Math.atan2(BLOCK_EDGE_Y, BLOCK_EDGE_X)) / 2;
	private static final String DIRECTORY = "MarkerShadows/";
	static final String PLAYER_SHADOW = "Images/player-shadow.png";

	private static final Map<String, Style> STYLES = styles();

	private enum Shape { SILHOUETTE, BLOCK, BED }

	private record Style(Shape shape, int size, double length, double contactX) {}

	private record Projection(double x, double y, double edgeX, double edgeY, double castX, double castY) {
		double determinant() {
			return edgeX * castY - edgeY * castX;
		}
	}

	private static Map<String, Style> styles() {
		var styles = new TreeMap<String, Style>();
		styles.put("SpawnCompass.png", new Style(Shape.SILHOUETTE, 32, 15, 0.50));
		styles.put("Sign.png", new Style(Shape.SILHOUETTE, 32, 35, 0.50));
		styles.put("Picture.png", new Style(Shape.SILHOUETTE, 32, 27, 0.50));
		styles.put("Portal.png", new Style(Shape.SILHOUETTE, 32, 33, 0.82));
		styles.put("RespawnAnchor.png", new Style(Shape.BLOCK, 32, 22, 0.50));
		styles.put("beacon.png", new Style(Shape.BLOCK, 32, 25, 0.50));
		var chest = new Style(Shape.BLOCK, 32, 21, 0.50);
		styles.put("Chest.png", chest);
		styles.put("TrappedChest.png", chest);
		styles.put("EnderChest.png", chest);
		styles.put("Barrel.png", new Style(Shape.BLOCK, 32, 20, 0.50));
		var shulker = new Style(Shape.BLOCK, 32, 22, 0.50);
		styles.put("ShulkerBox.png", shulker);
		var bed = new Style(Shape.BED, 38, 0, 0);
		for (Colors color : Colors.values()) {
			String name = ContainerKind.colorName(color);
			styles.put("ShulkerBox_" + name + ".png", shulker);
			styles.put("Items/" + name + "_bed.png", bed);
		}
		styles.put("Items/silver_bed.png", bed);
		return styles;
	}

	static void write(Path images) {
		STYLES.forEach((filename, style) -> {
			Path iconFile = images.resolve(filename);
			Path shadowFile = images.resolve(DIRECTORY + filename);
			try {
				BufferedImage icon = Files.isRegularFile(iconFile) ? ImageIO.read(iconFile.toFile()) : null;
				Rectangle bounds = icon == null ? new Rectangle() : alphaBounds(icon);
				if (bounds.isEmpty()) {
					Files.deleteIfExists(shadowFile);
					return;
				}
				Files.createDirectories(shadowFile.getParent());
				ImageIO.write(project(icon, bounds, style), "png", shadowFile.toFile());
			} catch (IOException e) {
				log.warn("Unable to export marker shadow for {}", iconFile, e);
			}
		});
	}

	static Map<String, String> urls(Path export, String worldId) {
		var urls = new TreeMap<String, String>();
		String prefix = worldId + "/Images/";
		Path images = export.resolve(prefix);
		for (String filename : STYLES.keySet()) {
			if (Files.isRegularFile(images.resolve(filename)) && Files.isRegularFile(images.resolve(DIRECTORY + filename)))
				urls.put(filename, prefix + DIRECTORY + filename);
		}
		if (Files.isRegularFile(export.resolve(PLAYER_SHADOW))) urls.put("player", PLAYER_SHADOW);
		return urls;
	}

	private static Rectangle alphaBounds(BufferedImage icon) {
		int left = icon.getWidth();
		int top = icon.getHeight();
		int right = 0;
		int bottom = 0;
		for (int y = 0; y < icon.getHeight(); y++) {
			for (int x = 0; x < icon.getWidth(); x++) {
				if ((icon.getRGB(x, y) >>> 24) <= 20) continue;
				left = Math.min(left, x);
				top = Math.min(top, y);
				right = Math.max(right, x + 1);
				bottom = Math.max(bottom, y + 1);
			}
		}
		return new Rectangle(left, top, right - left, bottom - top);
	}

	private static BufferedImage project(BufferedImage icon, Rectangle bounds, Style style) {
		double scaleX = (double) style.size() / icon.getWidth();
		double scaleY = (double) style.size() / icon.getHeight();
		double left = bounds.x * scaleX;
		double width = bounds.width * scaleX;
		double height = bounds.height * scaleY;
		double bottom = (bounds.y + bounds.height) * scaleY;
		double contactX = left + width * style.contactX();
		Projection footprint = switch (style.shape()) {
			case BLOCK -> {
				// Cast from the edge joining the far corner and the right corner of the base.
				double rearDepth = width * 0.40;
				double sideDepth = width * 0.19;
				double castLength = style.length() * 0.65 * Math.hypot(CAST_X, CAST_Y);
				yield new Projection(contactX, bottom - rearDepth, width * BLOCK_EDGE_X, rearDepth - sideDepth,
						castLength * Math.cos(BLOCK_CAST_ANGLE), -castLength * Math.sin(BLOCK_CAST_ANGLE));
			}
			case BED -> {
				// The bed's ground footprint runs between the legs.
				if (icon.getWidth() == 48) {
					yield new Projection(left + width * 0.104 + 0.8, bottom - height * 0.125 + 0.5,
							width * 0.427, height * 0.125, width * 0.479, -height * 0.583);
				}
				// Legacy bed sprites sit lower and use a smaller contact shadow.
				yield new Projection(left + width * 0.19, bottom - height * 0.11,
						width * 0.52, height * 0.06, width * 0.17, -height * 0.23);
			}
			case SILHOUETTE -> null;
		};

		var shadow = new BufferedImage(WIDTH * 2, HEIGHT * 2, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < shadow.getHeight(); y++) {
			for (int x = 0; x < shadow.getWidth(); x++) {
				double iconX = (x + 0.5) / 2 - 10;
				double iconY = (y + 0.5) / 2 - 12;
				int alpha = footprint == null
						? silhouetteAlpha(icon, iconX, iconY, scaleX, scaleY, contactX, bottom, height, style.length())
						: footprintAlpha(footprint, iconX, iconY, style.shape() == Shape.BED);
				if (alpha > 0) shadow.setRGB(x, y, (alpha << 24) | 0x0a0d0d);
			}
		}
		return shadow;
	}

	private static int silhouetteAlpha(BufferedImage icon, double x, double y, double scaleX, double scaleY,
			double contactX, double bottom, double height, double length) {
		double sum = 0;
		double total = 0;
		for (int dy = -2; dy <= 2; dy++) {
			double fraction = (y + dy * 0.47 - bottom) / (CAST_Y * length);
			double progress = clamp(fraction);
			int sourceY = (int) Math.floor((bottom - fraction * height) / scaleY);
			for (int dx = -2; dx <= 2; dx++) {
				double sourceX = contactX + (x + dx * 0.47 - contactX - fraction * CAST_X * length) / 0.90;
				int column = (int) Math.floor(sourceX / scaleX);
				double weight = weight(dx, dy, 0.40 + 0.25 * progress);
				total += weight;
				if (column >= 0 && column < icon.getWidth() && sourceY >= 0 && sourceY < icon.getHeight()) {
					sum += (icon.getRGB(column, sourceY) >>> 24) * weight * (0.48 - 0.13 * progress);
				}
			}
		}
		return (int) Math.round(sum / total);
	}

	private static int footprintAlpha(Projection footprint, double x, double y, boolean bed) {
		double sum = 0;
		double total = 0;
		double determinant = footprint.determinant();
		for (int dy = -2; dy <= 2; dy++) {
			double offsetY = y + dy * 0.47 - footprint.y();
			for (int dx = -2; dx <= 2; dx++) {
				double offsetX = x + dx * 0.47 - footprint.x();
				double edge = (offsetX * footprint.castY() - offsetY * footprint.castX()) / determinant;
				double cast = (footprint.edgeX() * offsetY - footprint.edgeY() * offsetX) / determinant;
				double progress = clamp(cast);
				double weight = weight(dx, dy, bed ? 0.72 : 0.40 + 0.25 * progress);
				total += weight;
				if (edge >= 0 && edge <= 1 && cast >= 0 && cast <= 1) {
				sum += 255 * weight * (bed ? 0.38 : 0.60 - 0.13 * progress);
				}
			}
		}
		return (int) Math.round(sum / total);
	}

	private static double clamp(double value) {
		return Math.max(0, Math.min(1, value));
	}

	private static double weight(int dx, int dy, double sigma) {
		return Math.exp(-(dx * dx + dy * dy) * 0.47 * 0.47 / (2 * sigma * sigma));
	}
}
