/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.SubMesh;
import tectonicus.rasteriser.SubMesh.Rotation;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

import java.util.Calendar;

import static tectonicus.Version.VERSION_5;

public class Chest implements BlockType
{
	private final String name;
	
	private final SubTexture smallTop, smallTopSide, smallTopFront, smallBottom, smallBaseSide, smallBaseFront, smallLock;
	private final SubTexture largeTopLeft, largeTopRight, largeTopSide, largeTopFrontLeft, largeTopFrontRight, largeTopBackLeft, largeTopBackRight,
							 largeBottomLeft, largeBottomRight, largeBaseSide, largeBaseFrontLeft, largeBaseFrontRight, largeBaseBackLeft, largeBaseBackRight, largeLock, largeLock2;

	private static final float offSet = 1.0f / 16.0f;
	private static final float height = offSet * 10.0f;

	public Chest(String name, SubTexture small, SubTexture large, SubTexture ender,
							SubTexture trappedSmall, SubTexture trappedLarge,
							SubTexture xmasSmall, SubTexture xmasLarge)
	{
		this.name = name;
		
		final float texel = 1.0f / 64.0f;
		final float wideTexel = 1.0f / 128.0f;
		
		Calendar calendar = Calendar.getInstance();
		SubTexture smallChest;
		if(name.equals("Ender Chest"))
			smallChest = ender;
		else if(name.equals("Trapped Chest"))
			smallChest = trappedSmall;
		else
			smallChest = small;
		
		SubTexture largeChest = name.equals("Trapped Chest") ? trappedLarge : large;
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		if(calendar.get(Calendar.MONTH) == Calendar.DECEMBER && (day == 24 || day == 25 || day == 26)
				&& large.texturePackVersion.getNumVersion() >= VERSION_5.getNumVersion())
		{
			smallChest = xmasSmall;
			largeChest = xmasLarge;
		}
		
		// Small Chest Textures
		smallTop = new SubTexture(smallChest.texture, smallChest.u0+texel*14, smallChest.v0, smallChest.u0+texel*28, smallChest.v0+texel*14);
		smallTopSide = new SubTexture(smallChest.texture, smallChest.u0, smallChest.v0+texel*14, smallChest.u0+texel*14, smallChest.v0+texel*19);
		smallTopFront = new SubTexture(smallChest.texture, smallChest.u0+texel*14, smallChest.v0+texel*14, smallChest.u0+texel*28, smallChest.v0+texel*19);
		smallBottom = new SubTexture(smallChest.texture, smallChest.u0+texel*28, smallChest.v0+texel*19, smallChest.u0+texel*42, smallChest.v0+texel*33);
		smallBaseSide = new SubTexture(smallChest.texture, smallChest.u0, smallChest.v0+texel*34, smallChest.u0+texel*14, smallChest.v0+texel*43);
		smallBaseFront = new SubTexture(smallChest.texture, smallChest.u0+texel*14, smallChest.v0+texel*34, smallChest.u0+texel*28, smallChest.v0+texel*43);
		smallLock = new SubTexture(smallChest.texture, smallChest.u0+texel*3, smallChest.v0, smallChest.u0+texel*5, smallChest.v0+texel*4);
		
		//Large Chest Textures
		largeTopLeft = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*14, largeChest.v0, largeChest.u0+wideTexel*29, largeChest.v0+texel*14);
		largeTopRight = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*30, largeChest.v0, largeChest.u0+wideTexel*44, largeChest.v0+texel*14);
		largeTopSide = new SubTexture(largeChest.texture, largeChest.u0, largeChest.v0+texel*14, largeChest.u0+wideTexel*14, largeChest.v0+texel*19);
		largeTopFrontLeft = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*14, largeChest.v0+texel*14, largeChest.u0+wideTexel*29, largeChest.v0+texel*19);
		largeTopFrontRight = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*29, largeChest.v0+texel*14, largeChest.u0+wideTexel*44, largeChest.v0+texel*19);
		largeTopBackRight = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*57, largeChest.v0+texel*14, largeChest.u0+wideTexel*73, largeChest.v0+texel*19);
		largeTopBackLeft = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*73, largeChest.v0+texel*14, largeChest.u0+wideTexel*88, largeChest.v0+texel*19);
		largeBottomLeft = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*45, largeChest.v0+texel*20, largeChest.u0+wideTexel*60, largeChest.v0+texel*34);
		largeBottomRight = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*61, largeChest.v0+texel*20, largeChest.u0+wideTexel*74, largeChest.v0+texel*34);
		largeBaseSide = new SubTexture(largeChest.texture, largeChest.u0, largeChest.v0+texel*34, largeChest.u0+wideTexel*14, largeChest.v0+texel*43);
		largeBaseFrontLeft = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*14, largeChest.v0+texel*34, largeChest.u0+wideTexel*29, largeChest.v0+texel*43);
		largeBaseFrontRight = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*29, largeChest.v0+texel*34, largeChest.u0+wideTexel*44, largeChest.v0+texel*43);
		largeBaseBackRight = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*58, largeChest.v0+texel*34, largeChest.u0+wideTexel*73, largeChest.v0+texel*43);
		largeBaseBackLeft = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*73, largeChest.v0+texel*34, largeChest.u0+wideTexel*88, largeChest.v0+texel*43);
		largeLock = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*3, largeChest.v0, largeChest.u0+wideTexel*4, largeChest.v0+texel*4);
		largeLock2 = new SubTexture(largeChest.texture, largeChest.u0+wideTexel*4, largeChest.v0, largeChest.u0+wideTexel*5, largeChest.v0+texel*4);
	}

	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public boolean isSolid()
	{
		return false;
	}
	
	@Override
	public boolean isWater()
	{
		return false;
	}
	
	@Override
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}
	
	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk chunk, Geometry geometry)
	{
		int data = chunk.getBlockData(x, y, z);
		final BlockProperties properties = chunk.getBlockState(x, y, z);
		if (properties != null) {
			switch (properties.get("facing")) {
				case "north":
					data = 2;
					break;
				case "south":
					data = 3;
					break;
				case "west":
					data = 4;
					break;
				case "east":
					data = 5;
					break;
				default:
			}
		}
		
		final float lightness = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.Top);
		Vector4f colour = new Vector4f(lightness, lightness, lightness, 1);
		
		SubMesh smallMesh = new SubMesh();
		SubMesh largeMesh = new SubMesh();

		if (properties != null) {
			String type = properties.get("type");
			if (type != null && type.equals("left")) {
				addLeftHalfDoubleChest(largeMesh, colour);
			} else if (type != null && type.equals("right")) {
				addRightHalfDoubleChest(largeMesh, colour);
			} else {
				addSingleChest(smallMesh, colour);
			}
		} else {
			final int westId = world.getBlockId(chunk.getChunkCoord(), x - 1, y, z);
			final int eastId = world.getBlockId(chunk.getChunkCoord(), x + 1, y, z);
			final int northId = world.getBlockId(chunk.getChunkCoord(), x, y, z - 1);
			final int southId = world.getBlockId(chunk.getChunkCoord(), x, y, z + 1);

			final boolean chestNorth;
			final boolean chestSouth;
			final boolean chestEast;
			final boolean chestWest;
			if (name.equals("Trapped Chest")) {
				chestNorth = northId == BlockIds.TRAPPED_CHEST;
				chestSouth = southId == BlockIds.TRAPPED_CHEST;
				chestEast = eastId == BlockIds.TRAPPED_CHEST;
				chestWest = westId == BlockIds.TRAPPED_CHEST;
			} else if (name.equals("Chest")) {
				chestNorth = northId == BlockIds.CHEST;
				chestSouth = southId == BlockIds.CHEST;
				chestEast = eastId == BlockIds.CHEST;
				chestWest = westId == BlockIds.CHEST;
			} else {
				chestNorth = chestSouth = chestEast = chestWest = false;
			}

			if (chestNorth || chestSouth || chestEast || chestWest) {
				// Double chest!
				if ((data == 2 && chestWest) || (data == 3 && chestEast) || (data == 4 && chestSouth) || (data == 5 && chestNorth)) {
					addRightHalfDoubleChest(largeMesh, colour);
				} else if ((data == 2 && chestEast) || (data == 3 && chestWest) || (data == 4 && chestNorth) || (data == 5 && chestSouth)) {
					addLeftHalfDoubleChest(largeMesh, colour);
				}
			} else {
				addSingleChest(smallMesh, colour);
			}
		}

		
		Rotation horizRotation = Rotation.Clockwise;
		float horizAngle = 0;
	
		// Set angle/rotation from block data flags
		if (data == 2)
		{
			// north			
			horizRotation = Rotation.AntiClockwise;
			horizAngle = 180;
		}
		// data == 3 south
		else if (data == 4)
		{
			// west
			horizAngle = 270;
		}
		else if (data == 5)
		{
			// east
			horizAngle = 90;
		}
		
		smallMesh.pushTo(geometry.getMesh(smallTop.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, Rotation.None, 0);
		largeMesh.pushTo(geometry.getMesh(largeTopLeft.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, Rotation.None, 0);
	}

	private void addRightHalfDoubleChest(SubMesh largeMesh, Vector4f colour) {
		// Top
		largeMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(1, 1-offSet*2, 0+offSet),
				new Vector3f(1, 1-offSet*2, 1-offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet), colour, largeTopLeft);
		// West
		largeMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet),
				new Vector3f(0+offSet, height, 1-offSet),  new Vector3f(0+offSet, height, 0+offSet), colour, largeTopSide);
		// North
		largeMesh.addQuad(new Vector3f(1, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 0+offSet),
				new Vector3f(0+offSet, height, 0+offSet),  new Vector3f(1, height, 0+offSet), colour, largeTopBackLeft);
		// South
		largeMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 1-offSet), new Vector3f(1, 1-offSet*2, 1-offSet),
				new Vector3f(1, height, 1-offSet),  new Vector3f(0+offSet, height, 1-offSet), colour, largeTopFrontLeft);


		//Chest bottom

		// Bottom
		largeMesh.addQuad(new Vector3f(0+offSet, 0, 0+offSet), new Vector3f(0+offSet, 0, 1-offSet),
				new Vector3f(1, 0, 1-offSet), new Vector3f(1, 0, 0+offSet), colour, largeBottomLeft);

		// West
		largeMesh.addQuad(new Vector3f(0+offSet, height, 0+offSet), new Vector3f(0+offSet, height, 1-offSet),
				new Vector3f(0+offSet, 0, 1-offSet),  new Vector3f(0+offSet, 0, 0+offSet), colour, largeBaseSide);
		// North
		largeMesh.addQuad(new Vector3f(1, height, 0+offSet), new Vector3f(0+offSet, height, 0+offSet),
				new Vector3f(0+offSet, 0, 0+offSet),  new Vector3f(1, 0, 0+offSet), colour, largeBaseBackLeft);
		// South
		largeMesh.addQuad(new Vector3f(0+offSet, height, 1-offSet), new Vector3f(1, height, 1-offSet),
				new Vector3f(1, 0, 1-offSet),  new Vector3f(0+offSet, 0, 1-offSet), colour, largeBaseFrontLeft);

		SubMesh.addBlockSimple(largeMesh, 1-offSet, offSet*8, offSet*15, offSet, offSet*4, offSet, colour, largeLock, largeLock, largeLock);
	}

	private void addLeftHalfDoubleChest(SubMesh largeMesh, Vector4f colour) {
		// Top
		largeMesh.addQuad(new Vector3f(0, 1-offSet*2, 0+offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
				new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(0, 1-offSet*2, 1-offSet), colour, largeTopRight);
		// North
		largeMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 0+offSet), new Vector3f(0, 1-offSet*2, 0+offSet),
				new Vector3f(0, height, 0+offSet),  new Vector3f(1-offSet, height, 0+offSet), colour, largeTopBackRight);
		// South
		largeMesh.addQuad(new Vector3f(0, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 1-offSet),
				new Vector3f(1-offSet, height, 1-offSet),  new Vector3f(0, height, 1-offSet), colour, largeTopFrontRight);
		// East
		largeMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
				new Vector3f(1-offSet, height, 0+offSet), new Vector3f(1-offSet, height, 1-offSet), colour, largeTopSide);


		//Chest bottom

		// Bottom
		largeMesh.addQuad(new Vector3f(0, 0, 0+offSet), new Vector3f(0, 0, 1-offSet),
				new Vector3f(1-offSet, 0, 1-offSet), new Vector3f(1-offSet, 0, 0+offSet), colour, largeBottomRight);
		// North
		largeMesh.addQuad(new Vector3f(1-offSet, height, 0+offSet), new Vector3f(0, height, 0+offSet),
				new Vector3f(0, 0, 0+offSet),  new Vector3f(1-offSet, 0, 0+offSet), colour, largeBaseBackRight);
		// South
		largeMesh.addQuad(new Vector3f(0, height, 1-offSet), new Vector3f(1-offSet, height, 1-offSet),
				new Vector3f(1-offSet, 0, 1-offSet),  new Vector3f(0, 0, 1-offSet), colour, largeBaseFrontRight);
		// East
		largeMesh.addQuad(new Vector3f(1-offSet, height, 1-offSet), new Vector3f(1-offSet, height, 0+offSet),
				new Vector3f(1-offSet, 0, 0+offSet), new Vector3f(1-offSet, 0, 1-offSet), colour, largeBaseSide);

		SubMesh.addBlockSimple(largeMesh, 0, offSet*8, offSet*15, offSet, offSet*4, offSet, colour, largeLock2, largeLock2, largeLock2);
	}

	private void addSingleChest(SubMesh smallMesh, Vector4f colour) {
		// Chest top

		// Top
		smallMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
				new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet), colour, smallTop);
		// West
		smallMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet),
				new Vector3f(0+offSet, height, 1-offSet),  new Vector3f(0+offSet, height, 0+offSet), colour, smallTopSide);
		// North
		smallMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 0+offSet),
				new Vector3f(0+offSet, height, 0+offSet),  new Vector3f(1-offSet, height, 0+offSet), colour, smallTopSide);
		// South
		smallMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 1-offSet),
				new Vector3f(1-offSet, height, 1-offSet),  new Vector3f(0+offSet, height, 1-offSet), colour, smallTopFront);
		// East
		smallMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
				new Vector3f(1-offSet, height, 0+offSet), new Vector3f(1-offSet, height, 1-offSet), colour, smallTopSide);


		//Chest bottom

		// Bottom
		smallMesh.addQuad(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1),
				new Vector3f(1, 0, 1), new Vector3f(1, 0, 0), colour, smallBottom);

		// West
		smallMesh.addQuad(new Vector3f(0+offSet, height, 0+offSet), new Vector3f(0+offSet, height, 1-offSet),
				new Vector3f(0+offSet, 0, 1-offSet),  new Vector3f(0+offSet, 0, 0+offSet), colour, smallBaseSide);
		// North
		smallMesh.addQuad(new Vector3f(1-offSet, height, 0+offSet), new Vector3f(0+offSet, height, 0+offSet),
				new Vector3f(0+offSet, 0, 0+offSet),  new Vector3f(1-offSet, 0, 0+offSet), colour, smallBaseSide);
		// South
		smallMesh.addQuad(new Vector3f(0+offSet, height, 1-offSet), new Vector3f(1-offSet, height, 1-offSet),
				new Vector3f(1-offSet, 0, 1-offSet),  new Vector3f(0+offSet, 0, 1-offSet), colour, smallBaseFront);
		// East
		smallMesh.addQuad(new Vector3f(1-offSet, height, 1-offSet), new Vector3f(1-offSet, height, 0+offSet),
				new Vector3f(1-offSet, 0, 0+offSet), new Vector3f(1-offSet, 0, 1-offSet), colour, smallBaseSide);

		SubMesh.addBlockSimple(smallMesh, offSet*7, offSet*8, offSet*15, offSet*2, offSet*4, offSet, colour, smallLock, smallLock, smallLock);
	}
}
