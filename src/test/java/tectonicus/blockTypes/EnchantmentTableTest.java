package tectonicus.blockTypes;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import tectonicus.rasteriser.Mesh;
import tectonicus.texture.SubTexture;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnchantmentTableTest {
	@Test
	void bookFloatsAboveTableAndUsesOnlyItsTextureRegion() {
		List<Vector3f> vertices = new ArrayList<>();
		SubTexture texture = new SubTexture(null, 0.25f, 0.125f, 0.75f, 0.625f);
		Mesh mesh = (Mesh)Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class},
				(proxy, method, args) -> {
					if (method.getName().equals("addVertex")) {
						vertices.add(new Vector3f((Vector3f)args[0]));
						assertTrue((float)args[2] >= texture.u0 && (float)args[2] <= texture.u1);
						assertTrue((float)args[3] >= texture.v0 && (float)args[3] <= texture.v1);
						assertEquals(new Vector4f(0.7f, 0.7f, 0.7f, 1), args[1]);
					}
					return null;
				});
		EnchantmentTable.addBookGeometry(mesh, texture, 3, 20, 7, new Vector4f(0.7f, 0.7f, 0.7f, 1));
		assertFalse(vertices.isEmpty());
		for (Vector3f vertex : vertices) {
			assertTrue(vertex.y > 20.75f, "Book must clear the table's top");
			assertTrue(vertex.y < 21.5f);
			assertTrue(vertex.x > 3 && vertex.x < 4);
			assertTrue(vertex.z > 7 && vertex.z < 8);
		}
	}

	@Test
	void renderingIsDeterministicAndUsesChunkLocalOffsets() {
		List<Vector3f> vertices = new ArrayList<>();
		Mesh mesh = (Mesh)Proxy.newProxyInstance(Mesh.class.getClassLoader(), new Class<?>[]{Mesh.class},
				(proxy, method, args) -> {
					if (method.getName().equals("addVertex"))
						vertices.add(new Vector3f((Vector3f)args[0]));
					return null;
				});
		SubTexture texture = new SubTexture(null, 0, 0, 1, 1);
		EnchantmentTable.addBookGeometry(mesh, texture, 0, 0, 0, new Vector4f(1));
		List<Vector3f> first = List.copyOf(vertices);
		// Closed page blocks must face outwards with the renderer's clockwise winding.
		for (int start : new int[]{72, 96}) {
			Vector3f centre = new Vector3f();
			for (int i = start; i < start + 24; i++)
				centre.add(first.get(i));
			centre.div(24);
			for (int i = start; i < start + 24; i += 4) {
				Vector3f a = first.get(i);
				Vector3f normal = new Vector3f(first.get(i + 1)).sub(a)
						.cross(new Vector3f(first.get(i + 2)).sub(a));
				assertTrue(normal.dot(new Vector3f(a).sub(centre)) < 0);
			}
		}
		vertices.clear();
		EnchantmentTable.addBookGeometry(mesh, texture, 15, 255, 15, new Vector4f(1));
		assertEquals(first.size(), vertices.size());
		for (int i = 0; i < first.size(); i++) {
			Vector3f expected = new Vector3f(first.get(i)).add(15, 255, 15);
			assertTrue(expected.equals(vertices.get(i), 0.0001f));
		}
	}
}
