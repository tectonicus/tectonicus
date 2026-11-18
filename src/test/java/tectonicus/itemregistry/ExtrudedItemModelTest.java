/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */
package tectonicus.itemregistry;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExtrudedItemModelTest {
	@Test
	void addsFrontBackAndTheOutlineOfOpaquePixels() {
		// Two opaque pixels side by side in a 4x4 sprite.
		BufferedImage sprite = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
		sprite.setRGB(1, 2, 0xFFFF0000);
		sprite.setRGB(2, 2, 0xFF00FF00);
		List<ExtrudedItemModel.Quad> quads = new ExtrudedItemModel(sprite).getQuads();
		// Front and back, plus top and bottom for each pixel and the two outer ends; none between them.
		assertEquals(2 + 4 + 2, quads.size());
		assertTrue(new Vector3f(0, 0, 1).equals(normal(quads.get(0)), 0.00001f));
		assertTrue(new Vector3f(0, 0, -1).equals(normal(quads.get(1)), 0.00001f));
		long sides = quads.stream().skip(2).filter(q -> q.corners()[0].x == 0.5f && q.corners()[2].x == 0.5f).count();
		assertEquals(0, sides, "no face between two opaque pixels");
	}

	@Test
	void sideFacesPointOutOfTheirPixelAndSampleItsCentre() {
		BufferedImage sprite = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
		sprite.setRGB(0, 0, 0xFFFFFFFF);
		List<ExtrudedItemModel.Quad> quads = new ExtrudedItemModel(sprite).getQuads();
		assertEquals(6, quads.size());
		Vector3f centre = new Vector3f(0.25f, 0.75f, 0.5f);
		for (var quad : quads.subList(2, quads.size())) {
			Vector3f outward = new Vector3f(quad.corners()[0]).add(quad.corners()[2]).mul(0.5f).sub(centre);
			assertTrue(normal(quad).dot(outward) > 0);
			for (int i = 0; i < 4; i++) {
				assertEquals(0.25f, quad.us()[i]);
				assertEquals(0.25f, quad.vs()[i]);
			}
		}
	}

	/** The front facing normal of a clockwise quad, as the meshes and posed geometry use it */
	private static Vector3f normal(ExtrudedItemModel.Quad quad) {
		Vector3f[] c = quad.corners();
		return new Vector3f(c[2]).sub(c[0]).cross(new Vector3f(c[1]).sub(c[0])).normalize();
	}
}
