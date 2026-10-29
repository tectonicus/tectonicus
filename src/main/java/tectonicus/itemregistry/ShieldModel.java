/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution. The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.itemregistry;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.blockregistry.BlockModel;
import tectonicus.blockregistry.BlockRegistry;
import tectonicus.rasteriser.Mesh;
import tectonicus.renderer.Geometry;
import tectonicus.texture.TexturePack;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@UtilityClass
public class ShieldModel {
	public static Matrix4f handTransform(boolean leftHand) {
		float unit = 1 / 16f;
		return new Matrix4f()
				// Convert the posed arm back to Minecraft's model axes for ItemInHandLayer.
				.scale(1, -1, -1)
				.rotateX((float) -Math.PI / 2).rotateY((float) Math.PI)
				.translate((leftHand ? -1 : 1) * unit, 2 * unit, -10 * unit)
				// shield.json thirdperson transforms; ItemTransform mirrors X translation and Y rotation on the left.
				.translate((leftHand ? -10 : 10) * unit, 6 * unit, (leftHand ? 12 : -4) * unit)
				.rotateY((float) (leftHand ? -Math.PI / 2 : Math.PI / 2))
				.translate(-0.5f, -0.5f, -0.5f)
				// ShieldSpecialRenderer's default transform, followed by our shared model's native-model conversion.
				.scale(1, -1, -1)
				.translate(-8 * unit, 12 * unit, -9 * unit).scale(1, -1, 1);
	}

	public static BlockModel getModel(BlockRegistry registry, TexturePack textures) {
		return registry.getBlockModels().get("tectonicus:shield", key -> {
			String texture = textures.fileExists("assets/minecraft/textures/entity/shield/shield_base_nopattern.png")
					? "minecraft:entity/shield/shield_base_nopattern" : "minecraft:entity/shield_base_nopattern";
			try {
				return registry.loadModel("minecraft:item/shield_inventory", "", new HashMap<>(Map.of("shield", texture)), null);
			} catch (Exception e) {
				log.error("Unable to load shield model", e);
				return null;
			}
		});
	}

	public static void addGeometry(BlockRegistry registry, TexturePack textures, Geometry geometry, Matrix4f transform, Vector4f colour) {
		BlockModel model = getModel(registry, textures);
		if (model == null)
			return;
		// Held items share icon quads and UVs, with entity lighting and no block-face culling.
		boolean reverseWinding = transform.determinant3x3() < 0;
		for (var quad : registry.getBakedModel(model, 0, 0, 0, false).getQuads()) {
			var face = quad.getFace();
			Geometry.MeshType meshType = face.isTranslucent() ? Geometry.MeshType.Transparent
					: face.isTransparent() ? Geometry.MeshType.AlphaTest : Geometry.MeshType.Solid;
			Mesh mesh = geometry.getMesh(face.getTexture().texture, meshType);
			for (int index = 0; index < 4; index++) {
				int vertex = reverseWinding && index != 0 ? 4 - index : index;
				Vector3f position = new Vector3f(quad.getPosition(vertex, 0), quad.getPosition(vertex, 1), quad.getPosition(vertex, 2));
				transform.transformPosition(position);
				mesh.addVertex(position, colour, quad.getUv(vertex, 0), quad.getUv(vertex, 1));
			}
		}
	}
}
