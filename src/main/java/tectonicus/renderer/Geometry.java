/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.renderer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.log4j.Log4j2;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.Texture;

@Log4j2
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
	
	public void drawTransparentSurfaces(final float xOffset, final float yOffset, final float zOffset)
	{
		transparentMesh.bind();
		transparentMesh.draw(xOffset, yOffset, zOffset);

		Map<Texture, Mesh> transparentMeshes = meshes.get(MeshType.Transparent);
		for (Mesh m : transparentMeshes.values())
		{
			m.bind();
			m.draw(xOffset, yOffset, zOffset);
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
