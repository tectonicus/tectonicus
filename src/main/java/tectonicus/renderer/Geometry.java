/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.renderer;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3f;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class Geometry
{
	public enum MeshType
	{
		Solid,
		AlphaTest,
		Transparent
	}
	
	private final Rasteriser rasteriser;
	
	private final Mesh baseMesh;
	private final Mesh transparentMesh;
	
	private final Map<MeshType, Map<Texture, Mesh>> meshes;
	private final List<TransparentFace> transparentFaceSortBuffer = new ArrayList<>();
	private boolean hasCachedTransparentSort;
	private float cachedForwardX;
	private float cachedForwardY;
	private float cachedForwardZ;
	private int cachedTransparentFaceCount;

	public Geometry(Rasteriser rasteriser)
	{
		this.rasteriser = rasteriser;
		
		// Notes:
		//	base vertices generally around 20k-30k
		//	alpha generally around 2-3k, often lower, occasionally up to 5k
		//	transparent often low, but goes to around 10k when ocean visible
		
		// At the moment, with all three at 50k max, 100 loaded geometry chunks comes to 514Mb
		
		baseMesh = rasteriser.createMesh(null);
		transparentMesh = rasteriser.createMesh(null);
		
		meshes = new EnumMap<>(MeshType.class);
		
		meshes.put(MeshType.Solid, new HashMap<>());
		meshes.put(MeshType.AlphaTest, new HashMap<>());
		meshes.put(MeshType.Transparent, new HashMap<>());
	}
	
	public void destroy()
	{
		baseMesh.destroy();
		transparentMesh.destroy();
		
		for (Map<Texture, Mesh> meshMap : meshes.values())
			for (Mesh m : meshMap.values())
				m.destroy();
	}
	
	// TODO: Refactor to remove these
	public Mesh getBaseMesh() { return baseMesh; }
	
	public Mesh getMesh(Texture texture, MeshType type)
	{
		Map<Texture, Mesh> meshList = meshes.get(type);

		Mesh mesh = meshList.get(texture);
		if (mesh == null) {
			Mesh newMesh = rasteriser.createMesh(texture);
			meshList.put(texture, newMesh);
			return newMesh;
		} else {
			return mesh;
		}
	}
	
	public void finalise()
	{
		baseMesh.finalise();
		transparentMesh.finalise();
		
		for (Map<Texture, Mesh> meshMap : meshes.values())
			for (Mesh m : meshMap.values())
				m.finalise();
	}
	
	public void drawSolidSurfaces(final float xOffset, final float yOffset, final float zOffset)
	{
		baseMesh.bind();
		baseMesh.draw(xOffset, yOffset, zOffset);
		
		Map<Texture, Mesh> solidMeshes = meshes.get(MeshType.Solid);
		for (Mesh m : solidMeshes.values())
		{
			m.bind();
			m.draw(xOffset, yOffset, zOffset);
		}
	}
	
	public void drawAlphaTestedSurfaces(final float xOffset, final float yOffset, final float zOffset)
	{
		Map<Texture, Mesh> alphaTestMeshes = meshes.get(MeshType.AlphaTest);
		for (Mesh m : alphaTestMeshes.values())
		{
			m.bind();
			m.draw(xOffset, yOffset, zOffset);
		}
	}
	
	public void drawTransparentSurfaces(Camera camera, final float xOffset, final float yOffset, final float zOffset)
	{
		drawTransparentSurfaces(camera, true, xOffset, yOffset, zOffset);
	}

	public void drawTransparentSurfaces(Camera camera, boolean sortFaces, final float xOffset, final float yOffset, final float zOffset)
	{
		if (!sortFaces) {
			transparentMesh.bind();
			transparentMesh.draw(xOffset, yOffset, zOffset);

			Map<Texture, Mesh> transparentMeshes = meshes.get(MeshType.Transparent);
			for (Mesh mesh : transparentMeshes.values()) {
				mesh.bind();
				mesh.draw(xOffset, yOffset, zOffset);
			}
			return;
		}

		Vector3f forward = camera.getForward();
		int faceCount;
		if (hasCachedTransparentSort && sameDirection(forward)) {
			faceCount = cachedTransparentFaceCount;
		} else {
			Vector3f eye = camera.getEyePosition();
			faceCount = 0;
			faceCount = addFaces(transparentFaceSortBuffer, faceCount, transparentMesh, eye, forward, xOffset, yOffset, zOffset);
			Map<Texture, Mesh> transparentMeshes = meshes.get(MeshType.Transparent);
			for (Mesh m : transparentMeshes.values())
				faceCount = addFaces(transparentFaceSortBuffer, faceCount, m, eye, forward, xOffset, yOffset, zOffset);

			List<TransparentFace> faces = transparentFaceSortBuffer.subList(0, faceCount);
			faces.sort(Comparator.comparingDouble(face -> -face.depth));
			cachedForwardX = forward.x;
			cachedForwardY = forward.y;
			cachedForwardZ = forward.z;
			cachedTransparentFaceCount = faceCount;
			hasCachedTransparentSort = true;
		}
		List<TransparentFace> faces = transparentFaceSortBuffer.subList(0, faceCount);
		int firstFace = 0;
		Mesh boundMesh = null;
		while (firstFace < faceCount) {
			TransparentFace first = faces.get(firstFace);
			int runLength = 1;
			while (firstFace + runLength < faceCount) {
				TransparentFace next = faces.get(firstFace + runLength);
				TransparentFace previous = faces.get(firstFace + runLength - 1);
				if (next.mesh != first.mesh || next.index != previous.index + 1)
					break;
				runLength++;
			}
			if (first.mesh != boundMesh) {
				first.mesh.bind();
				boundMesh = first.mesh;
			}
			first.mesh.drawFaces(first.index, runLength, xOffset, yOffset, zOffset);
			firstFace += runLength;
		}
	}

	private boolean sameDirection(Vector3f forward) {
		// Orthographic map tiles translate the camera while keeping its direction fixed.
		// Translation adds the same depth offset to every face, so the sorted order is unchanged.
		final float tolerance = 0.000001f;
		return Math.abs(forward.x - cachedForwardX) < tolerance
				&& Math.abs(forward.y - cachedForwardY) < tolerance
				&& Math.abs(forward.z - cachedForwardZ) < tolerance;
	}

	private static int addFaces(List<TransparentFace> faces, int startIndex, Mesh mesh, Vector3f eye, Vector3f forward, float xOffset, float yOffset, float zOffset) {
		int endIndex = startIndex + mesh.getFaceCount();
		while (faces.size() < endIndex)
			faces.add(new TransparentFace());
		for (int faceIndex = 0; faceIndex < mesh.getFaceCount(); faceIndex++)
			faces.get(startIndex + faceIndex).set(mesh, faceIndex, mesh.getFaceDepth(faceIndex, eye, forward, xOffset, yOffset, zOffset));
		return endIndex;
	}

	private static class TransparentFace {
		private Mesh mesh;
		private int index;
		private float depth;

		private void set(Mesh mesh, int index, float depth) {
			this.mesh = mesh;
			this.index = index;
			this.depth = depth;
		}
	}
	
	public long getMemorySize()
	{
		final int baseSize = baseMesh.getMemorySize() + transparentMesh.getMemorySize();
		
		long size = 0;
		
		for (Map<Texture, Mesh> meshMap : meshes.values())
			for (Mesh m : meshMap.values())
				size += m.getMemorySize();
		
		return baseSize + size;
	}

	public void printGeometryStats()
	{
		final int baseVerts = countVertices(MeshType.Solid);
		final int alphaTestVerts = countVertices(MeshType.AlphaTest);
		final int transparentVerts = countVertices(MeshType.Transparent);
		
		log.info("Geometry:");
		log.info("\tbase vertices: {}", baseMesh.getTotalVertices() + baseVerts);
		log.info("\talpha vertices: {}", alphaTestVerts);
		log.info("\ttransparent vertices: {}", transparentMesh.getTotalVertices() + transparentVerts);
	}
	
	private int countVertices(MeshType type)
	{
		int vertCount = 0;
		
		Map<Texture, Mesh> subMeshes = meshes.get(type);
		for (Mesh m : subMeshes.values())
		{
			vertCount += m.getTotalVertices();
		}
		
		return vertCount;
	}
}
