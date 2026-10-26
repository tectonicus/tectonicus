/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.renderer;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import tectonicus.chunk.Chunk;
import tectonicus.chunk.ChunkCoord;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkRenderOriginTest {
	@Test
	void everyRenderPassAndTransparentSortingUseTheSameLocalCoordinates() throws Exception {
		List<Vector3f> offsets = new ArrayList<>();
		List<Vector3f> sortEyes = new ArrayList<>();
		Mesh mesh = (Mesh)Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class},
				(proxy, method, args) -> {
					switch (method.getName()) {
						case "draw": offsets.add(new Vector3f((float)args[0], (float)args[1], (float)args[2])); break;
						case "drawFaces": offsets.add(new Vector3f((float)args[2], (float)args[3], (float)args[4])); break;
						case "getFaceCount": return 1;
						case "getFaceDepth":
							sortEyes.add(new Vector3f((Vector3f)args[1]));
							return 0f;
					}
					return null;
				});
		Rasteriser rasteriser = (Rasteriser)Proxy.newProxyInstance(Rasteriser.class.getClassLoader(), new Class<?>[]{Rasteriser.class},
				(proxy, method, args) -> method.getName().equals("createMesh") ? mesh : null);
		Geometry geometry = new Geometry(rasteriser);
		geometry.getMesh(null, Geometry.MeshType.AlphaTest);
		Chunk chunk = new Chunk(new ChunkCoord(-1874999, 1874999), null);
		var field = Chunk.class.getDeclaredField("geometry");
		field.setAccessible(true);
		field.set(chunk, geometry);
		OrthoCamera camera = new OrthoCamera(rasteriser, 512, 512);
		camera.lookAt(-29999984.25, 128, 29999984.75, 32, (float)Math.PI / 4, (float)Math.PI / 4);
		chunk.drawSolid(camera);
		chunk.drawAlphaTestedSurfaces(camera);
		chunk.drawTransparentSurfaces(camera, false);
		chunk.drawTransparentSurfaces(camera, true);
		assertEquals(4, offsets.size());
		Vector3f expected = camera.toRenderPosition(-29999984, 0, 29999984);
		for (Vector3f offset : offsets) {
			assertEquals(expected, offset);
			assertTrue(Math.abs(offset.x) <= 16 && Math.abs(offset.z) <= 16);
		}
		assertEquals(List.of(camera.getRenderEyePosition()), sortEyes);
	}
}
