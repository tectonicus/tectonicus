/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.itemregistry;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.renderer.Geometry;
import tectonicus.util.Colour4f;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuiLightingTest {
	@Test void standardCubeUsesMinecraftGuiLightsInsteadOfWorldFaceFactors() {
		GuiLighting light = new GuiLighting(true, new Matrix4f());
		Matrix4f pose = new Matrix4f().rotateXYZ((float) Math.toRadians(30), (float) Math.toRadians(225), 0);
		// Reference values from Minecraft's ITEMS_3D rig and item shader for block/block's GUI rotation.
		assertEquals(1, light.brightness(pose.transformDirection(new Vector3f(0, 1, 0))), 0.00001f);
		assertEquals(0.4f, light.brightness(pose.transformDirection(new Vector3f(0, 0, -1))), 0.00001f);
		assertEquals(0.65053725f, light.brightness(pose.transformDirection(new Vector3f(1, 0, 0))), 0.00001f);
	}

	@Test void frontLightingFullyLightsAFlatGuiSprite() {
		assertEquals(1, new GuiLighting(false, new Matrix4f()).brightness(new Vector3f(0, 0, 1)), 0.00001f);
	}

	@Test void changingTheRenderingCameraDoesNotChangeTheGuiLighting() {
		Matrix4f basis = new Matrix4f().rotateXYZ(0.3f, -1.2f, 0.7f);
		Vector3f normal = new Vector3f(0.2f, 0.7f, 0.4f).normalize();
		for (boolean side : new boolean[]{false, true})
			assertEquals(new GuiLighting(side, new Matrix4f()).brightness(normal),
					new GuiLighting(side, basis).brightness(basis.transformDirection(normal, new Vector3f())), 0.00001f);
	}

	@Test void meshLightingFollowsThePosedNormalAndPreservesTintAlphaAndUv() {
		List<Vector4f> colours = new ArrayList<>();
		List<Float> us = new ArrayList<>();
		Mesh output = (Mesh) Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class}, (proxy, method, args) -> {
			if (method.getName().equals("addVertex")) {
				colours.add(new Vector4f((Vector4f) args[1]));
				us.add((float) args[2]);
			}
			return null;
		});
		Rasteriser rasteriser = (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
				(proxy, method, args) -> method.getName().equals("createMesh") ? output : null);
		// A north-facing quad turns to face the viewer; front lighting should then be full bright.
		GuiGeometry geometry = new GuiGeometry(rasteriser, new Matrix4f().rotateY((float) Math.PI).scale(2, 3, 0.5f),
				new GuiLighting(false, new Matrix4f()));
		Mesh mesh = geometry.getMesh(null, Geometry.MeshType.Transparent);
		Vector3f position = new Vector3f(1, 1, 0);
		Vector4f tint = new Vector4f(0.3f, 0.6f, 0.9f, 0.5f);
		mesh.addVertex(position, tint, 0, 0);
		mesh.addVertex(new Vector3f(0, 1, 0), new Colour4f(tint.x, tint.y, tint.z, tint.w), 1, 0);
		mesh.addVertex(new Vector3f(0, 0, 0), tint, 1, 1);
		mesh.addVertex(new Vector3f(1, 0, 0), tint, 0, 1);
		assertEquals(List.of(0f, 1f, 1f, 0f), us);
		assertEquals(4, colours.size());
		for (Vector4f colour : colours)
			assertTrue(tint.equals(colour, 0.00001f));
		assertEquals(new Vector3f(1, 1, 0), position, "Emission must not mutate the source geometry");
	}
}
