/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a player skin as Minecraft's player model seen from above and slightly to the side, with both
 * skin layers, classic or slim arms and simple face lighting. Geometry is in model units, where one unit
 * is one skin pixel; the figure stands on y = 0 and faces +z.
 */
public final class PlayerAvatarRenderer {
	/** Rotation around the vertical axis; shows the player's right side on the viewer's left. */
	private static final double YAW = Math.toRadians(30);
	/** Downward tilt of the view, so the tops of the head and shoulders are visible. */
	private static final double PITCH = Math.toRadians(20);
	/** Output pixels per model unit for the full figure. */
	static final int BODY_SCALE = 6;
	/** Each output pixel averages SUPERSAMPLE x SUPERSAMPLE samples, which smooths the outline. */
	private static final int SUPERSAMPLE = 4;
	private static final double HAT_INFLATE = 0.5;
	private static final double LAYER_INFLATE = 0.25;

	private static final double COS_YAW = Math.cos(YAW), SIN_YAW = Math.sin(YAW);
	private static final double COS_PITCH = Math.cos(PITCH), SIN_PITCH = Math.sin(PITCH);

	/** A cuboid of the model and the origin of its box-unwrapped texture in a 64x64 skin. */
	private record Box(double x0, double y0, double z0, double x1, double y1, double z1, int u, int v, int w, int h, int d) {
		static Box of(double x, double y, double z, int w, int h, int d, int u, int v, double inflate) {
			return new Box(x - inflate, y - inflate, z - inflate, x + w + inflate, y + h + inflate, z + d + inflate, u, v, w, h, d);
		}
	}

	/** One textured face: the model points under the texture's top-left, top-right and bottom-left corners. */
	private record Face(double[] origin, double[] right, double[] down, int texX, int texY, int texW, int texH, double light) {}

	private PlayerAvatarRenderer() {
	}

	/** The whole figure; every skin gives an image of the same size with the figure in the same place. */
	public static BufferedImage renderBody(BufferedImage skin, boolean slim) {
		BufferedImage texture = normalize(skin);
		if (texture == null) return null;
		return render(texture, model(slim), model(false), BODY_SCALE);
	}

	/** Only the head, fitted into a square of the given size (used for the marker toggle button). */
	public static BufferedImage renderHead(BufferedImage skin, int size) {
		BufferedImage texture = normalize(skin);
		if (texture == null) return null;
		List<Box> head = head();
		BufferedImage image = render(texture, head, head, 1);
		// Re-render at the scale that fits the requested square, then centre it.
		double scale = (double) size / Math.max(image.getWidth(), image.getHeight());
		BufferedImage scaled = render(texture, head, head, scale);
		BufferedImage square = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		square.getGraphics().drawImage(scaled, (size - scaled.getWidth()) / 2, (size - scaled.getHeight()) / 2, null);
		return square;
	}

	/**
	 * Minecraft's own guess for skins without a known model: slim arms are 3 pixels wide, which leaves the
	 * last two columns of the right arm's texture unused (transparent).
	 */
	public static boolean looksSlim(BufferedImage skin) {
		int f = skin.getWidth() / 64;
		if (f < 1 || skin.getHeight() != skin.getWidth()) return false;
		for (int y = 20 * f; y < 32 * f; y++)
			for (int x = 54 * f; x < 56 * f; x++)
				if ((skin.getRGB(x, y) >>> 24) != 0) return false;
		return true;
	}

	private static List<Box> head() {
		List<Box> boxes = new ArrayList<>();
		boxes.add(Box.of(-4, 24, -4, 8, 8, 8, 0, 0, 0));
		boxes.add(Box.of(-4, 24, -4, 8, 8, 8, 32, 0, HAT_INFLATE));
		return boxes;
	}

	private static List<Box> model(boolean slim) {
		int arm = slim ? 3 : 4;
		List<Box> boxes = head();
		// Body and jacket
		boxes.add(Box.of(-4, 12, -2, 8, 12, 4, 16, 16, 0));
		boxes.add(Box.of(-4, 12, -2, 8, 12, 4, 16, 32, LAYER_INFLATE));
		// The player's right arm is on -x, the left arm on +x
		boxes.add(Box.of(-4 - arm, 12, -2, arm, 12, 4, 40, 16, 0));
		boxes.add(Box.of(-4 - arm, 12, -2, arm, 12, 4, 40, 32, LAYER_INFLATE));
		boxes.add(Box.of(4, 12, -2, arm, 12, 4, 32, 48, 0));
		boxes.add(Box.of(4, 12, -2, arm, 12, 4, 48, 48, LAYER_INFLATE));
		// Legs and trousers
		boxes.add(Box.of(-4, 0, -2, 4, 12, 4, 0, 16, 0));
		boxes.add(Box.of(-4, 0, -2, 4, 12, 4, 0, 32, LAYER_INFLATE));
		boxes.add(Box.of(0, 0, -2, 4, 12, 4, 16, 48, 0));
		boxes.add(Box.of(0, 0, -2, 4, 12, 4, 0, 48, LAYER_INFLATE));
		return boxes;
	}

	private static List<Face> faces(Box b) {
		int u = b.u(), v = b.v(), w = b.w(), h = b.h(), d = b.d();
		double x0 = b.x0(), y0 = b.y0(), z0 = b.z0(), x1 = b.x1(), y1 = b.y1(), z1 = b.z1();
		// The texture net is [right side][front][left side][back] below [top][bottom]; neighbouring
		// faces share edges, which fixes the direction each face's texture runs in.
		return List.of(
				new Face(p(x0, y1, z1), p(x1, y1, z1), p(x0, y0, z1), u + d, v + d, w, h, 0.85), // front
				new Face(p(x1, y1, z0), p(x0, y1, z0), p(x1, y0, z0), u + 2 * d + w, v + d, w, h, 0.55), // back
				new Face(p(x0, y1, z0), p(x0, y1, z1), p(x0, y0, z0), u, v + d, d, h, 0.65), // player's right side
				new Face(p(x1, y1, z1), p(x1, y1, z0), p(x1, y0, z1), u + d + w, v + d, d, h, 0.55), // player's left side
				new Face(p(x0, y1, z0), p(x1, y1, z0), p(x0, y1, z1), u + d, v, w, d, 1.0), // top
				new Face(p(x0, y0, z1), p(x1, y0, z1), p(x0, y0, z0), u + d + w, v, w, d, 0.5)); // bottom
	}

	private static double[] p(double x, double y, double z) {
		return new double[] {x, y, z};
	}

	/** Orthographic view: screen x to the right, screen y down, depth growing towards the viewer. */
	private static double[] project(double[] point) {
		double x = point[0] * COS_YAW + point[2] * SIN_YAW;
		double z = -point[0] * SIN_YAW + point[2] * COS_YAW;
		double y = point[1] * COS_PITCH - z * SIN_PITCH;
		double depth = point[1] * SIN_PITCH + z * COS_PITCH;
		return new double[] {x, -y, depth};
	}

	/**
	 * Rasterises the faces of {@code boxes} with a depth buffer. The canvas is sized from {@code frame}, so
	 * models that differ only in detail (slim arms) land on identical canvases.
	 */
	private static BufferedImage render(BufferedImage texture, List<Box> boxes, List<Box> frame, double scale) {
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		for (Box b : frame) {
			for (double x : new double[] {b.x0(), b.x1()})
				for (double y : new double[] {b.y0(), b.y1()})
					for (double z : new double[] {b.z0(), b.z1()}) {
						double[] s = project(p(x, y, z));
						minX = Math.min(minX, s[0]);
						maxX = Math.max(maxX, s[0]);
						minY = Math.min(minY, s[1]);
						maxY = Math.max(maxY, s[1]);
					}
		}
		int width = (int) Math.ceil((maxX - minX) * scale);
		int height = (int) Math.ceil((maxY - minY) * scale);
		double sampleScale = scale * SUPERSAMPLE;
		int sw = width * SUPERSAMPLE, sh = height * SUPERSAMPLE;
		int[] colour = new int[sw * sh];
		double[] depth = new double[sw * sh];
		java.util.Arrays.fill(depth, Double.NEGATIVE_INFINITY);
		int f = texture.getWidth() / 64;

		for (Box box : boxes) {
			for (Face face : faces(box)) {
				double[] a = project(face.origin());
				double[] r = project(face.right());
				double[] dn = project(face.down());
				double ax = (a[0] - minX) * sampleScale, ay = (a[1] - minY) * sampleScale;
				double ux = (r[0] - a[0]) * sampleScale, uy = (r[1] - a[1]) * sampleScale;
				double vx = (dn[0] - a[0]) * sampleScale, vy = (dn[1] - a[1]) * sampleScale;
				double det = ux * vy - uy * vx;
				if (Math.abs(det) < 1e-9) continue; // seen edge-on
				int left = (int) Math.max(0, Math.floor(Math.min(Math.min(ax, ax + ux), Math.min(ax + vx, ax + ux + vx))));
				int right = (int) Math.min(sw - 1, Math.ceil(Math.max(Math.max(ax, ax + ux), Math.max(ax + vx, ax + ux + vx))));
				int top = (int) Math.max(0, Math.floor(Math.min(Math.min(ay, ay + uy), Math.min(ay + vy, ay + uy + vy))));
				int bottom = (int) Math.min(sh - 1, Math.ceil(Math.max(Math.max(ay, ay + uy), Math.max(ay + vy, ay + uy + vy))));
				int texW = face.texW() * f, texH = face.texH() * f;
				for (int py = top; py <= bottom; py++) {
					for (int px = left; px <= right; px++) {
						double dx = px + 0.5 - ax, dy = py + 0.5 - ay;
						double s = (dx * vy - dy * vx) / det;
						double t = (ux * dy - uy * dx) / det;
						if (s < 0 || s >= 1 || t < 0 || t >= 1) continue;
						double z = a[2] + s * (r[2] - a[2]) + t * (dn[2] - a[2]);
						int i = py * sw + px;
						if (z <= depth[i]) continue;
						int argb = texture.getRGB(face.texX() * f + (int) (s * texW), face.texY() * f + (int) (t * texH));
						// Minecraft draws skins with an alpha cutoff: pixels are either shown or not.
						if ((argb >>> 24) < 128) continue;
						depth[i] = z;
						colour[i] = shade(argb, face.light());
					}
				}
			}
		}
		return downsample(colour, sw, width, height);
	}

	private static int shade(int argb, double light) {
		int r = (int) Math.round(((argb >> 16) & 0xff) * light);
		int g = (int) Math.round(((argb >> 8) & 0xff) * light);
		int b = (int) Math.round((argb & 0xff) * light);
		return 0xff000000 | r << 16 | g << 8 | b;
	}

	private static BufferedImage downsample(int[] samples, int sampleWidth, int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int count = SUPERSAMPLE * SUPERSAMPLE;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int a = 0, r = 0, g = 0, b = 0;
				for (int sy = 0; sy < SUPERSAMPLE; sy++) {
					for (int sx = 0; sx < SUPERSAMPLE; sx++) {
						int c = samples[(y * SUPERSAMPLE + sy) * sampleWidth + x * SUPERSAMPLE + sx];
						if (c == 0) continue;
						a++;
						r += (c >> 16) & 0xff;
						g += (c >> 8) & 0xff;
						b += c & 0xff;
					}
				}
				if (a == 0) continue;
				image.setRGB(x, y, (a * 255 / count) << 24 | (r / a) << 16 | (g / a) << 8 | (b / a));
			}
		}
		return image;
	}

	/**
	 * Converts a skin to the 64x64 layout the way Minecraft does: old 64x32 skins get mirrored copies of the
	 * right arm and leg as their left limbs, the base layer is made opaque, and an old skin's hat layer is
	 * hidden when it has no transparent pixel at all.
	 * @return null when the image doesn't have skin proportions
	 */
	static BufferedImage normalize(BufferedImage skin) {
		if (skin == null) return null;
		int f = skin.getWidth() / 64;
		boolean legacy = skin.getHeight() * 2 == skin.getWidth();
		if (f < 1 || skin.getWidth() % 64 != 0 || (!legacy && skin.getHeight() != skin.getWidth())) return null;

		BufferedImage image = new BufferedImage(64 * f, 64 * f, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < skin.getHeight(); y++)
			for (int x = 0; x < skin.getWidth(); x++)
				image.setRGB(x, y, skin.getRGB(x, y));

		if (legacy) {
			int[][] copies = { // x, y, dx, dy, width, height in 64x32 units
					{4, 16, 16, 32, 4, 4}, {8, 16, 16, 32, 4, 4}, {0, 20, 24, 32, 4, 12}, {4, 20, 16, 32, 4, 12},
					{8, 20, 8, 32, 4, 12}, {12, 20, 16, 32, 4, 12}, {44, 16, -8, 32, 4, 4}, {48, 16, -8, 32, 4, 4},
					{40, 20, 0, 32, 4, 12}, {44, 20, -8, 32, 4, 12}, {48, 20, -16, 32, 4, 12}, {52, 20, -8, 32, 4, 12}};
			for (int[] c : copies) copyMirrored(image, c[0] * f, c[1] * f, c[2] * f, c[3] * f, c[4] * f, c[5] * f);
		}
		setNoAlpha(image, 0, 0, 32 * f, 16 * f);
		if (legacy) hideOpaqueHat(image, 32 * f, 0, 64 * f, 16 * f);
		setNoAlpha(image, 0, 16 * f, 64 * f, 32 * f);
		setNoAlpha(image, 16 * f, 48 * f, 48 * f, 64 * f);
		return image;
	}

	private static void copyMirrored(BufferedImage image, int x, int y, int dx, int dy, int width, int height) {
		for (int row = 0; row < height; row++)
			for (int col = 0; col < width; col++)
				image.setRGB(x + dx + width - 1 - col, y + dy + row, image.getRGB(x + col, y + row));
	}

	private static void setNoAlpha(BufferedImage image, int x0, int y0, int x1, int y1) {
		for (int y = y0; y < y1; y++)
			for (int x = x0; x < x1; x++)
				image.setRGB(x, y, image.getRGB(x, y) | 0xff000000);
	}

	private static void hideOpaqueHat(BufferedImage image, int x0, int y0, int x1, int y1) {
		for (int y = y0; y < y1; y++)
			for (int x = x0; x < x1; x++)
				if ((image.getRGB(x, y) >>> 24) < 128) return;
		for (int y = y0; y < y1; y++)
			for (int x = x0; x < x1; x++)
				image.setRGB(x, y, image.getRGB(x, y) & 0x00ffffff);
	}
}
