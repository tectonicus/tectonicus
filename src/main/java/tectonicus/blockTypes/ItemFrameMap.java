/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.ItemFrameEntity;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

final class ItemFrameMap {
	private ItemFrameMap() {}

	static void addQuad(Geometry geometry, BlockContext world, ItemFrameEntity frame, SubTexture fallback,
						Vector3f topLeft, Vector3f topRight, Vector3f bottomRight, Vector3f bottomLeft, Vector4f color) {
		Integer mapId = frame.getMapId();
		SubTexture image = mapId == null ? null : world.getMapTexture(mapId);
		if (image == null) {
			if (fallback != null) {
				Mesh mesh = geometry.getMesh(fallback.texture, Geometry.MeshType.AlphaTest);
				MeshUtil.addQuad(mesh, topLeft, topRight, bottomRight, bottomLeft, color, fallback);
			}
			return;
		}

		Mesh mesh = geometry.getMesh(image.texture, Geometry.MeshType.AlphaTest);
		Vector4f mapColor = frame.isGlow() ? new Vector4f(1, 1, 1, 1) : color;
		// Move the texture's top-left corner clockwise around the face.
		switch (frame.getRotation()) {
			case 1 -> MeshUtil.addQuad(mesh, topRight, bottomRight, bottomLeft, topLeft, mapColor, image);
			case 2 -> MeshUtil.addQuad(mesh, bottomRight, bottomLeft, topLeft, topRight, mapColor, image);
			case 3 -> MeshUtil.addQuad(mesh, bottomLeft, topLeft, topRight, bottomRight, mapColor, image);
			default -> MeshUtil.addQuad(mesh, topLeft, topRight, bottomRight, bottomLeft, mapColor, image);
		}
	}
}
