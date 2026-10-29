/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import tectonicus.raw.ArmorStandEntity;

import static org.junit.jupiter.api.Assertions.*;

class ArmorStandShieldTest {
	private static final Vector3f HANDLE = new Vector3f(0.5f, 0.75f, 11 / 16f);

	@Test
	void neutralShieldMatchesMinecraftThirdPersonOffsetsAndOrientation() {
		ArmorStandEntity stand = stand(0);
		Vector3f right = ArmorStand.shieldTransform(0, 0, 0, stand, false).transformPosition(new Vector3f(HANDLE));
		Vector3f left = ArmorStand.shieldTransform(0, 0, 0, stand, true).transformPosition(new Vector3f(HANDLE));
		assertEquals(2 / 16f, right.x, 0.00001f);
		assertEquals(14 / 16f, left.x, 0.00001f);
		assertEquals(right.y, left.y, 0.00001f);
		assertEquals(16 / 16f, right.y, 0.00001f);
		Vector3f face = new Vector3f(0.5f, 0.75f, 7 / 16f);
		assertTrue(ArmorStand.shieldTransform(0, 0, 0, stand, false).transformPosition(new Vector3f(face)).x < right.x);
		assertTrue(ArmorStand.shieldTransform(0, 0, 0, stand, true).transformPosition(new Vector3f(face)).x > left.x);
		Vector3f rightTop = ArmorStand.shieldTransform(0, 0, 0, stand, false).transformPosition(new Vector3f(8 / 16f, 23 / 16f, 7 / 16f));
		Vector3f rightBottom = ArmorStand.shieldTransform(0, 0, 0, stand, false).transformPosition(new Vector3f(8 / 16f, 1 / 16f, 7 / 16f));
		assertEquals(rightTop.y, rightBottom.y, 0.00001f);
		assertEquals(22 / 16f, rightTop.z - rightBottom.z, 0.00001f, "Shield's long axis follows Minecraft's held-item rotation");
	}

	@Test
	void yawRotatesHeldShieldWithTheStand() {
		Vector3f right = ArmorStand.shieldTransform(3, 4, 5, stand(90), false).transformPosition(new Vector3f(HANDLE));
		assertEquals(3.5f, right.x, 0.00001f);
		assertEquals(4 + 16 / 16f, right.y, 0.00001f);
		assertEquals(5 + 2 / 16f, right.z, 0.00001f);
	}

	@Test
	void raisedArmMovesShieldAroundTheShoulder() {
		ArmorStandEntity stand = stand(0);
		stand.setRightArmPose(new Vector3f(-90, 0, 0));
		Vector3f hand = ArmorStand.shieldTransform(0, 0, 0, stand, false).transformPosition(new Vector3f(HANDLE));
		assertEquals(22 / 16f, hand.y, 0.00001f);
		assertEquals(0.5f + 6 / 16f, hand.z, 0.00001f);
	}

	@Test
	void posedShieldsMatchTheNativeMinecraftPipelineForBothHands() {
		for (boolean left : new boolean[]{false, true}) {
			for (Vector3f pose : new Vector3f[]{new Vector3f(), new Vector3f(-90, 0, 0), new Vector3f(-70, 25, 40)}) {
				ArmorStandEntity stand = stand(37);
				stand.setRightArmPose(pose);
				stand.setLeftArmPose(pose);
				// Independent reference in vanilla's Y-down model coordinates, before converting its world axes.
				Matrix4d vanilla = new Matrix4d().translate(3.5, 5.5, 5.5).rotateY(Math.toRadians(-37)).scale(1, -1, -1)
						.translate((left ? 5 : -5) / 16.0, 2 / 16.0, 0)
						.rotateZYX(Math.toRadians(pose.z), Math.toRadians(pose.y), Math.toRadians(pose.x))
						.rotateX(-Math.PI / 2).rotateY(Math.PI)
						.translate((left ? -1 : 1) / 16.0, 2 / 16.0, -10 / 16.0)
						.translate((left ? -10 : 10) / 16.0, 6 / 16.0, (left ? 12 : -4) / 16.0)
						.rotateY(left ? -Math.PI / 2 : Math.PI / 2).translate(-0.5, -0.5, -0.5).scale(1, -1, -1);
				for (Vector3f point : new Vector3f[]{HANDLE, new Vector3f(2 / 16f, 1 / 16f, 7 / 16f), new Vector3f(14 / 16f, 23 / 16f, 8 / 16f)}) {
					Vector3d expected = vanilla.transformPosition(new Vector3d(point.x - 0.5, 0.75 - point.y, point.z - 9 / 16.0));
					Vector3f actual = ArmorStand.shieldTransform(3, 4, 5, stand, left).transformPosition(new Vector3f(point));
					assertEquals(expected.x, actual.x, 0.00001);
					assertEquals(expected.y, actual.y, 0.00001);
					assertEquals(expected.z, actual.z, 0.00001);
				}
			}
		}
	}

	private ArmorStandEntity stand(float yaw) {
		ArmorStandEntity stand = new ArmorStandEntity(0, 0, 0, 0, 0, 0, yaw, false, false, null, null, null, null);
		stand.setRightArmPose(new Vector3f());
		stand.setLeftArmPose(new Vector3f());
		return stand;
	}
}
