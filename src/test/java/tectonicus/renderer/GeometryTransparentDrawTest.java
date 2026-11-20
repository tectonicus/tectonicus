/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.renderer;

import com.jogamp.opengl.GL2;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;
import tectonicus.util.Colour4f;

import java.lang.reflect.Proxy;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryTransparentDrawTest {
	private static final Rasteriser CAMERA_RASTERISER = (Rasteriser) Proxy.newProxyInstance(
			Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class}, (proxy, method, args) -> null);

	@Test
	void drawsSortedFacesInOrderWithOneDrawCallPerMeshRun() {
		List<String> log = new ArrayList<>();
		Rasteriser rasteriser = (Rasteriser) Proxy.newProxyInstance(Rasteriser.class.getClassLoader(),
				new Class<?>[]{Rasteriser.class}, (proxy, method, args) ->
						method.getName().equals("createMesh") ? new FakeMesh(log) : null);
		Geometry geometry = new Geometry(rasteriser);

		// Interleaved faces in two meshes, so sorting by depth scatters them through each vertex buffer.
		Random random = new Random(42);
		FakeMesh water = (FakeMesh) geometry.getMesh(texture(), Geometry.MeshType.Transparent);
		FakeMesh glass = (FakeMesh) geometry.getMesh(texture(), Geometry.MeshType.Transparent);
		for (int i = 0; i < 200; i++) {
			water.addFace(random.nextFloat() * 16, random.nextFloat() * 64, random.nextFloat() * 16);
			if (i % 10 == 0)
				glass.addFace(random.nextFloat() * 16, random.nextFloat() * 64, random.nextFloat() * 16);
		}

		OrthoCamera camera = new OrthoCamera(CAMERA_RASTERISER, 512, 512);
		camera.lookAtTile(3, 4, 32, radians(45), radians(45));

		List<FaceRef> expected = null;
		for (int pass = 0; pass < 2; pass++) { // the second pass uses the cached sort
			log.clear();
			geometry.drawTransparentSurfaces(camera, true, 1, 2, 3);
			if (pass == 0)
				expected = oldDrawOrder(log);
			else
				assertTrue(log.stream().noneMatch(entry -> entry.startsWith("depth ")), "sort should be cached");

			List<FaceRef> drawn = new ArrayList<>();
			int drawCalls = 0;
			for (String entry : log) {
				if (entry.startsWith("draw ")) {
					drawCalls++;
					String[] parts = entry.split(" ");
					for (int i = 2; i < parts.length; i++)
						drawn.add(new FaceRef(Integer.parseInt(parts[1]), Integer.parseInt(parts[i])));
				}
			}
			assertEquals(water.getFaceCount() + glass.getFaceCount(), expected.size());
			assertEquals(expected, drawn, "pass " + pass);
			assertEquals(meshChanges(expected), drawCalls, "pass " + pass);
			assertTrue(drawCalls < expected.size() / 4, drawCalls + " draw calls for " + expected.size() + " faces");
		}
	}

	/**
	 * The order the per-face drawing used: faces in the order Geometry collected them (the order of its
	 * depth queries), stably sorted far to near with the same comparator.
	 */
	private static List<FaceRef> oldDrawOrder(List<String> log) {
		record Depth(FaceRef face, float depth) {}
		List<Depth> faces = new ArrayList<>();
		for (String entry : log) {
			if (entry.startsWith("depth ")) {
				String[] parts = entry.split(" ");
				faces.add(new Depth(new FaceRef(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])), Float.parseFloat(parts[3])));
			}
		}
		faces.sort(Comparator.comparingDouble(face -> -face.depth));
		List<FaceRef> result = new ArrayList<>();
		for (Depth face : faces)
			result.add(face.face);
		return result;
	}

	private static int meshChanges(List<FaceRef> faces) {
		int runs = 0;
		int previous = -1;
		for (FaceRef face : faces) {
			if (face.mesh != previous)
				runs++;
			previous = face.mesh;
		}
		return runs;
	}

	private record FaceRef(int mesh, int face) {}

	/** Records depth queries and draw calls. Each face is a single point, which is enough for sorting. */
	private static final class FakeMesh implements Mesh {
		private static int nextId;
		private final int id = nextId++;
		private final List<String> log;
		private final List<float[]> centres = new ArrayList<>();

		FakeMesh(List<String> log) {
			this.log = log;
		}

		void addFace(float x, float y, float z) {
			centres.add(new float[]{x, y, z});
		}

		@Override public int getFaceCount() { return centres.size(); }

		@Override
		public float getFaceDepth(int faceIndex, Vector3f eye, Vector3f forward, float xOffset, float yOffset, float zOffset) {
			float[] c = centres.get(faceIndex);
			float depth = (c[0] + xOffset - eye.x) * forward.x + (c[1] + yOffset - eye.y) * forward.y + (c[2] + zOffset - eye.z) * forward.z;
			log.add("depth " + id + " " + faceIndex + " " + depth);
			return depth;
		}

		@Override
		public void drawQuads(IntBuffer vertexIndices, float xOffset, float yOffset, float zOffset) {
			assertEquals(List.of(1f, 2f, 3f), List.of(xOffset, yOffset, zOffset));
			assertEquals(0, vertexIndices.remaining() % 4);
			StringBuilder entry = new StringBuilder("draw " + id);
			for (int i = vertexIndices.position(); i < vertexIndices.limit(); i += 4) {
				int first = vertexIndices.get(i);
				assertEquals(0, first % 4);
				for (int corner = 1; corner < 4; corner++)
					assertEquals(first + corner, vertexIndices.get(i + corner));
				entry.append(' ').append(first / 4);
			}
			log.add(entry.toString());
		}

		@Override
		public void drawFaces(int firstFace, int faceCount, float xOffset, float yOffset, float zOffset) {
			throw new AssertionError("sorted faces should be drawn through index lists");
		}

		@Override public void destroy() {}
		@Override public void finalise() {}
		@Override public Texture getTexture() { return null; }
		@Override public void bind() {}
		@Override public void bind(GL2 gl2) {}
		@Override public void draw(float xOffset, float yOffset, float zOffset) {}
		@Override public void draw(float xOffset, float yOffset, float zOffset, GL2 gl2) {}
		@Override public int getMemorySize() { return 0; }
		@Override public int getTotalVertices() { return centres.size() * 4; }
		@Override public void addVertex(Vector3f position, Vector4f colour, float u, float v) {}
		@Override public void addVertex(Vector3f position, float u, float v) {}
		@Override public void addVertex(Vector3f position, Colour4f color, float u, float v) {}
	}

	private static Texture texture() {
		return (Texture) Proxy.newProxyInstance(Texture.class.getClassLoader(), new Class<?>[]{Texture.class}, (proxy, method, args) ->
				switch (method.getName()) {
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default -> null;
				});
	}

	private static float radians(int degrees) {
		return degrees / 360.0f * (float) Math.PI * 2.0f;
	}
}
