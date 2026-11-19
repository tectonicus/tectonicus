/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser.lwjgl;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import tectonicus.configuration.Configuration.RasteriserType;
import tectonicus.configuration.ImageFormat;
import tectonicus.rasteriser.BlendFunc;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LwjglMeshTest {
	private static final int SIZE = 64;

	@Test
	void indexedQuadsMatchFaceByFaceDrawing() {
		Rasteriser rasteriser;
		try {
			rasteriser = RasteriserFactory.createRasteriser(RasteriserType.LWJGL, DisplayType.OFFSCREEN, SIZE, SIZE, 24, 8, 24, 0);
		} catch (Throwable e) {
			rasteriser = null;
		}
		assumeTrue(rasteriser != null, "No OpenGL context available");
		try {
			// Overlapping translucent quads, so the blended result depends on the draw order.
			Random random = new Random(7);
			LwjglMesh mesh = new LwjglMesh(null);
			int faces = 60;
			for (int face = 0; face < faces; face++) {
				float x = random.nextFloat() * 1.4f - 1, y = random.nextFloat() * 1.4f - 1, size = 0.3f + random.nextFloat() * 0.3f;
				Vector4f colour = new Vector4f(random.nextFloat(), random.nextFloat(), random.nextFloat(), 0.5f);
				mesh.addVertex(new Vector3f(x, y, 0), colour, 0, 0);
				mesh.addVertex(new Vector3f(x + size, y, 0), colour, 0, 0);
				mesh.addVertex(new Vector3f(x + size, y + size, 0), colour, 0, 0);
				mesh.addVertex(new Vector3f(x, y + size, 0), colour, 0, 0);
			}
			mesh.finalise();

			List<Integer> order = new ArrayList<>();
			for (int face = 0; face < faces; face++)
				order.add(face);
			Collections.shuffle(order, random);

			BufferedImage perFace = render(rasteriser, mesh, () -> {
				for (int face : order)
					mesh.drawFaces(face, 1, 0.05f, -0.05f, 0);
			});
			IntBuffer indices = BufferUtils.createIntBuffer(faces * 4);
			for (int face : order)
				indices.put(face * 4).put(face * 4 + 1).put(face * 4 + 2).put(face * 4 + 3);
			indices.flip();
			BufferedImage indexed = render(rasteriser, mesh, () -> mesh.drawQuads(indices, 0.05f, -0.05f, 0));
			assertEquals(0, indices.position(), "draw must not consume the index buffer");

			Set<Integer> colours = new HashSet<>();
			for (int y = 0; y < SIZE; y++) {
				for (int x = 0; x < SIZE; x++) {
					assertEquals(perFace.getRGB(x, y), indexed.getRGB(x, y), "pixel " + x + "," + y);
					colours.add(perFace.getRGB(x, y));
				}
			}
			assertTrue(colours.size() > 20, "test scene should contain many blended colours");
		} finally {
			rasteriser.destroy();
		}
	}

	private static BufferedImage render(Rasteriser rasteriser, LwjglMesh mesh, Runnable draw) {
		rasteriser.setViewport(0, 0, SIZE, SIZE);
		rasteriser.resetState();
		rasteriser.clear(Color.BLACK);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		rasteriser.enableDepthTest(false);
		GL11.glDisable(GL11.GL_CULL_FACE); // the test quads are wound counter-clockwise
		rasteriser.enableAlphaTest(false);
		rasteriser.enableBlending(true);
		rasteriser.setBlendFunc(BlendFunc.REGULAR);
		mesh.bind();
		draw.run();
		return rasteriser.takeScreenshot(0, 0, SIZE, SIZE, ImageFormat.JPG);
	}
}
