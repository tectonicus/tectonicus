/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemFrameItemsTest {
	private static final float EPSILON = 0.0001f;

	@Test
	void flatItemsLieOnTheFrameAndFaceOutOfTheWall() {
		// Facing: 0 down, 1 up, 2 north, 3 south, 4 west, 5 east
		Vector3f[] outward = {new Vector3f(0, -1, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, -1),
				new Vector3f(0, 0, 1), new Vector3f(-1, 0, 0), new Vector3f(1, 0, 0)};
		for (int direction = 0; direction < 6; direction++) {
			Matrix4f transform = ItemFrameItems.frameTransform(0, 0, 0, direction, 0, false).mul(ItemFrameItems.generatedDisplay());
			// The middle of the sprite sits 1/16 in front of the wall behind the frame.
			Vector3f centre = transform.transformPosition(new Vector3f(0.5f, 0.5f, 0.5f));
			Vector3f wall = new Vector3f(0.5f).sub(new Vector3f(outward[direction]).mul(0.5f));
			assertTrue(new Vector3f(wall).add(new Vector3f(outward[direction]).mul(1 / 16f)).equals(centre, EPSILON),
					"direction " + direction + ": " + centre);
			// The sprite's front (+Z in item space) faces out of the wall.
			Vector3f front = transform.transformDirection(new Vector3f(0, 0, 1)).normalize();
			assertTrue(outward[direction].equals(front, EPSILON), "direction " + direction + ": " + front);
			// Half size
			assertEquals(0.5f, transform.transformDirection(new Vector3f(1, 0, 0)).length(), EPSILON);
		}
	}

	@Test
	void uprightItemsOnWallsKeepTheirTopUpAndAreNotMirrored() {
		for (int direction = 2; direction < 6; direction++) {
			Matrix4f transform = ItemFrameItems.frameTransform(0, 0, 0, direction, 0, false).mul(ItemFrameItems.generatedDisplay());
			assertTrue(new Vector3f(0, 0.5f, 0).equals(transform.transformDirection(new Vector3f(0, 1, 0)), EPSILON));
			assertTrue(transform.determinant3x3() > 0);
		}
	}

	@Test
	void eachRotationStepTurnsTheItemAnEighthClockwiseAsSeenFromTheFront() {
		// A south facing frame is seen from the south: up is +Y and right is +X.
		Matrix4f turned = ItemFrameItems.frameTransform(0, 0, 0, 3, 2, false).mul(ItemFrameItems.generatedDisplay());
		assertTrue(new Vector3f(0.5f, 0, 0).equals(turned.transformDirection(new Vector3f(0, 1, 0)), EPSILON));
	}

	@Test
	void invisibleFramesMoveTheItemBackAgainstTheWall() {
		Matrix4f transform = ItemFrameItems.frameTransform(0, 0, 0, 3, 0, true).mul(ItemFrameItems.generatedDisplay());
		assertEquals(0, transform.transformPosition(new Vector3f(0.5f)).z, EPSILON);
	}

	@Test
	void shadesFacesWithTheLightOfTheirAxis() {
		var shading = ItemFrameItems.shading(1, 0.8f, 0.6f);
		assertEquals(1, shading.brightness(new Vector3f(0, -1, 0)), EPSILON);
		assertEquals(0.8f, shading.brightness(new Vector3f(0, 0, 1)), EPSILON);
		assertEquals(0.6f, shading.brightness(new Vector3f(1, 0, 0)), EPSILON);
		assertEquals(0.7f, shading.brightness(new Vector3f(1, 0, 1).normalize()), EPSILON);
	}

	@Test
	void readsModelTransformations() {
		Matrix4f moved = ItemFrameItems.transformation(Map.of("translation", List.of(0, 0, 1),
				"left_rotation", List.of(0, 0, 0, 1), "scale", List.of(2, 2, 2), "right_rotation", List.of(0, 0, 0, 1)));
		assertTrue(new Vector3f(2, 0, 1).equals(moved.transformPosition(new Vector3f(1, 0, 0)), EPSILON));
		Matrix4f turned = ItemFrameItems.transformation(Map.of("left_rotation", Map.of("axis", List.of(0, 1, 0), "angle", Math.PI / 2)));
		assertTrue(new Vector3f(0, 0, -1).equals(turned.transformPosition(new Vector3f(1, 0, 0)), EPSILON));
		Matrix4f matrix = ItemFrameItems.transformation(List.of(1, 0, 0, 3, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1));
		assertTrue(new Vector3f(3, 0, 0).equals(matrix.transformPosition(new Vector3f()), EPSILON));
		assertNull(ItemFrameItems.transformation(null));
	}

	@Test
	void tintsOnlyTheLayersThatHaveATint() {
		BufferedImage base = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		base.setRGB(0, 0, 0xFFFFFFFF);
		BufferedImage overlay = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
		overlay.setRGB(1, 1, 0xFF808080);
		BufferedImage sprite = ItemFrameItems.composite(List.of(base, overlay), List.of(0x3366FF, -1));
		assertEquals(2, sprite.getWidth());
		assertEquals(0xFF3366FF, sprite.getRGB(0, 0));
		assertEquals(0xFF808080, sprite.getRGB(1, 1));
	}
}
