/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
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
	// Scratch space for sorting transparent faces, shared by all geometry: once the sort is turned into
	// transparentRuns nothing reads it again, so keeping it per chunk would only hold memory.
	// Drawing happens on the rendering thread only.
	private static final List<TransparentFace> transparentFaceSortBuffer = new ArrayList<>();
	private boolean hasCachedTransparentSort;
	private float cachedForwardX;
	private float cachedForwardY;
	private float cachedForwardZ;
	// Draw calls for the cached sort: one per stretch of sorted faces that share a mesh.
	private final List<TransparentRun> transparentRuns = new ArrayList<>();
	private IntBuffer transparentIndices;

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
	
	/** Lets a wrapper add vertices to another geometry's meshes. */
	protected Geometry(Geometry target)
	{
		rasteriser = target.rasteriser;
		baseMesh = target.baseMesh;
		transparentMesh = target.transparentMesh;
		meshes = target.meshes;
	}
	
	public void destroy()
	{
		transparentRuns.clear();
		transparentIndices = null;
		hasCachedTransparentSort = false;
		
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
		if (!hasCachedTransparentSort || !sameDirection(forward)) {
			Vector3f eye = camera.getRenderEyePosition();
			int faceCount = addFaces(transparentFaceSortBuffer, 0, transparentMesh, eye, forward, xOffset, yOffset, zOffset);
			Map<Texture, Mesh> transparentMeshes = meshes.get(MeshType.Transparent);
			for (Mesh m : transparentMeshes.values())
				faceCount = addFaces(transparentFaceSortBuffer, faceCount, m, eye, forward, xOffset, yOffset, zOffset);

			List<TransparentFace> faces = transparentFaceSortBuffer.subList(0, faceCount);
			faces.sort(Comparator.comparingDouble(face -> -face.depth));
			cachedForwardX = forward.x;
			cachedForwardY = forward.y;
			cachedForwardZ = forward.z;
			buildTransparentRuns(faces);
			// Drop the mesh references so the shared buffer does not keep this geometry's meshes reachable.
			for (TransparentFace face : faces)
				face.set(null, 0, 0);
			hasCachedTransparentSort = true;
		}
		// Each run draws its faces in sorted order through an index list, so faces that are not
		// next to each other in the vertex buffer no longer need a draw call each.
		Mesh boundMesh = null;
		for (TransparentRun run : transparentRuns) {
			if (run.mesh != boundMesh) {
				run.mesh.bind();
				boundMesh = run.mesh;
			}
			run.mesh.drawQuads(run.vertexIndices, xOffset, yOffset, zOffset);
		}
	}

	/** Groups consecutive sorted faces of the same mesh and writes their quad vertex indices in sorted order. */
	private void buildTransparentRuns(List<TransparentFace> faces) {
		transparentRuns.clear();
		final int indexCount = faces.size() * 4;
		if (transparentIndices == null || transparentIndices.capacity() < indexCount)
			transparentIndices = ByteBuffer.allocateDirect(indexCount * Integer.BYTES).order(ByteOrder.nativeOrder()).asIntBuffer();
		transparentIndices.clear();

		int runStart = 0;
		Mesh runMesh = null;
		for (TransparentFace face : faces) {
			if (face.mesh != runMesh) {
				if (runMesh != null)
					transparentRuns.add(new TransparentRun(runMesh, transparentIndices.slice(runStart, transparentIndices.position() - runStart)));
				runMesh = face.mesh;
				runStart = transparentIndices.position();
			}
			final int firstVertex = face.index * 4;
			transparentIndices.put(firstVertex).put(firstVertex + 1).put(firstVertex + 2).put(firstVertex + 3);
		}
		if (runMesh != null)
			transparentRuns.add(new TransparentRun(runMesh, transparentIndices.slice(runStart, transparentIndices.position() - runStart)));
	}

	private record TransparentRun(Mesh mesh, IntBuffer vertexIndices) {}

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
		
		if (transparentIndices != null)
			size += (long) transparentIndices.capacity() * Integer.BYTES;
		
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
