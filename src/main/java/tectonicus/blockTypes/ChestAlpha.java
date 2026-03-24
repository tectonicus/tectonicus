/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import lombok.RequiredArgsConstructor;
import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.chunk.ChunkCoord;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

@RequiredArgsConstructor
public class ChestAlpha implements BlockType {
	private final String name;
	
	private final SubTexture topTexture;
	private final SubTexture sideTexture;
	private final SubTexture frontTexture;
	
	private final SubTexture doubleSide0, doubleSide1;
	private final SubTexture doubleFront0, doubleFront1;
	
	private final Colour4f colour = new Colour4f(1, 1, 1, 1);
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public boolean isSolid() {
		return true;
	}
	
	@Override
	public boolean isWater() {
		return false;
	}
	
	@Override
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry) {
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry) {
		Mesh mesh = geometry.getMesh(topTexture.texture, Geometry.MeshType.Solid);
		Mesh sideMesh = geometry.getMesh(sideTexture.texture, Geometry.MeshType.Solid);
		Mesh frontMesh = geometry.getMesh(frontTexture.texture, Geometry.MeshType.Solid);
		Mesh doubleSide0Mesh = geometry.getMesh(doubleSide0.texture, Geometry.MeshType.Solid);
		Mesh doubleSide1Mesh = geometry.getMesh(doubleSide1.texture, Geometry.MeshType.Solid);
		Mesh doubleFront0Mesh = geometry.getMesh(doubleFront0.texture, Geometry.MeshType.Solid);
		Mesh doubleFront1Mesh = geometry.getMesh(doubleFront1.texture, Geometry.MeshType.Solid);
		
		final int northId = world.getBlockId(chunk.getChunkCoord(), x - 1, y, z);
		final int southId = world.getBlockId(chunk.getChunkCoord(), x + 1, y, z);
		final int eastId = world.getBlockId(chunk.getChunkCoord(), x, y, z - 1);
		final int westId = world.getBlockId(chunk.getChunkCoord(), x, y, z + 1);
		
		BlockType northType = world.getBlockType(chunk.getChunkCoord(), x - 1, y, z);
		BlockType southType = world.getBlockType(chunk.getChunkCoord(), x + 1, y, z);
		BlockType eastType = world.getBlockType(chunk.getChunkCoord(), x, y, z - 1);
		BlockType westType = world.getBlockType(chunk.getChunkCoord(), x, y, z + 1);
		
		final boolean chestNorth = northId == BlockIds.CHEST;
		final boolean chestSouth = southId == BlockIds.CHEST;
		final boolean chestEast = eastId == BlockIds.CHEST;
		final boolean chestWest = westId == BlockIds.CHEST;
		
		// Default everything to the side textures
		SubTexture northTex = sideTexture;
		SubTexture southTex = sideTexture;
		SubTexture eastTex = sideTexture;
		SubTexture westTex = sideTexture;
		Mesh northMesh = sideMesh;
		Mesh southMesh = sideMesh;
		Mesh eastMesh = sideMesh;
		Mesh westMesh = sideMesh;
		
		if (chestNorth || chestSouth || chestEast || chestWest) {
			// Double chest!
			// We either can run north-south or east-west
			
			if (chestNorth) {
				// North-south, this south
				// face east if any blocks west, otherwise face west
				if (isSolid(chunk.getChunkCoord(), x, y, z + 1, world, registry)
					|| isSolid(chunk.getChunkCoord(), x - 1, y, z + 1, world, registry)) {
					// face east
					eastTex = doubleFront0;
					eastMesh = doubleFront0Mesh;
					westTex = doubleSide1;
					westMesh = doubleSide1Mesh;
				} else {
					// face west
					westTex = doubleFront1;
					westMesh = doubleFront1Mesh;
					eastTex = doubleSide0;
					eastMesh = doubleSide0Mesh;
				}
			} else if (chestSouth) {
				// North-south, this north
				// face east if any blocks west, otherwise face west
				if (isSolid(chunk.getChunkCoord(), x, y, z + 1, world, registry)
					|| isSolid(chunk.getChunkCoord(), x - 1, y, z + 1, world, registry)) {
					// face east
					eastTex = doubleFront1;
					eastMesh = doubleFront1Mesh;
					westTex = doubleSide0;
					westMesh = doubleSide0Mesh;
				} else {
					// face west
					westTex = doubleFront0;
					westMesh = doubleFront0Mesh;
					eastTex = doubleSide1;
					eastMesh = doubleSide1Mesh;
				}
			} else if (chestEast) {
				// East-west, this west
				// face north if any blocks south, otherwise face south
				if (isSolid(chunk.getChunkCoord(), x + 1, y, z, world, registry)
					|| isSolid(chunk.getChunkCoord(), x + 1, y, z - 1, world, registry)) {
					// face north
					northTex = doubleFront1;
					northMesh = doubleFront1Mesh;
					southTex = doubleSide0;
					southMesh = doubleSide0Mesh;
				} else {
					// face south
					southTex = doubleFront0;
					southMesh = doubleFront0Mesh;
					northTex = doubleSide1;
					northMesh = doubleSide1Mesh;
				}
			} else {
				// East-west, this east
				// face north if any blocks south, otherwise face south
				if (isSolid(chunk.getChunkCoord(), x + 1, y, z, world, registry)
					|| isSolid(chunk.getChunkCoord(), x + 1, y, z + 1, world, registry)) {
					// face north
					northTex = doubleFront0;
					northMesh = doubleFront0Mesh;
					southTex = doubleSide1;
					southMesh = doubleSide1Mesh;
				} else {
					// face south
					southTex = doubleFront1;
					southMesh = doubleFront1Mesh;
					northTex = doubleSide0;
					northMesh = doubleSide0Mesh;
				}
			}
		} else {
			// Single chest
			// Direction changes based on surrounding blocks
			int numSolid = 0;
			if (northType.isSolid())
				numSolid++;
			if (southType.isSolid())
				numSolid++;
			if (eastType.isSolid())
				numSolid++;
			if (westType.isSolid())
				numSolid++;
			
			if (numSolid == 4) {
				// Don't really care which is the front face, all hidden
			} else if (numSolid == 3) {
				// Faces the direction which isn't covered
				
				if (!northType.isSolid()) {
					northTex = frontTexture;
					northMesh = frontMesh;
				} else if (!southType.isSolid()) {
					southTex = frontTexture;
					southMesh = frontMesh;
				} else if (!eastType.isSolid()) {
					eastTex = frontTexture;
					eastMesh = frontMesh;
				} else if (!westType.isSolid()) {
					westTex = frontTexture;
					westMesh = frontMesh;
				}
			} else if (numSolid == 2) {
				// Have to hard code all possibilities since MC logic seems a bit weird
				
				if (northType.isSolid() && southType.isSolid()) {
					westTex = frontTexture;
					westMesh = frontMesh;
				} else if (eastType.isSolid() && westType.isSolid()) {
					// Front doesn't show, oddly!
					// Probably a MC bug, expect this to get fixed sometime
				} else if ((northType.isSolid() && eastType.isSolid()) || (westType.isSolid() && northType.isSolid())) {
					southTex = frontTexture;
					southMesh = frontMesh;
				} else if ((eastType.isSolid() && southType.isSolid()) || (southType.isSolid() && westType.isSolid())) {
					northTex = frontTexture;
					northMesh = frontMesh;
				}
			} else if (numSolid == 1) {
				// Faces away from a single solid block
				
				if (northType.isSolid()) {
					southTex = frontTexture;
					southMesh = frontMesh;
				} else if (southType.isSolid()) {
					northTex = frontTexture;
					northMesh = frontMesh;
				} else if (eastType.isSolid()) {
					westTex = frontTexture;
					westMesh = frontMesh;
				} else if (westType.isSolid()) {
					eastTex = frontTexture;
					eastMesh = frontMesh;
				}
			} else {
				// Default to facing west
				westTex = frontTexture;
				westMesh = frontMesh;
			}
		}
		
		// Top is always the same
		BlockUtil.addTop(world, chunk, mesh, x, y, z, colour, topTexture, registry);
		
		BlockUtil.addNorth(world, chunk, northMesh, x, y, z, colour, northTex, registry);
		BlockUtil.addSouth(world, chunk, southMesh, x, y, z, colour, southTex, registry);
		BlockUtil.addEast(world, chunk, eastMesh, x, y, z, colour, eastTex, registry);
		BlockUtil.addWest(world, chunk, westMesh, x, y, z, colour, westTex, registry);
	}
	
	private static boolean isSolid(ChunkCoord coord, int x, int y, int z, BlockContext world, BlockTypeRegistry registry) {
		BlockType type = world.getBlockType(coord, x, y, z);
		return type.isSolid();
	}
}
