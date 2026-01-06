/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import lombok.Getter;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
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

public class ChestNew implements BlockType
{
	private final String name;
	@Getter
	private final String id;

	private final SubTexture smallTop, smallTopSide, smallTopFront, smallBottom, smallBaseSide, smallBaseFront, smallLock;
	private final SubTexture rightTop, rightTopFront, rightTopSide, rightTopBack, rightBottom, rightBaseFront, rightBaseSide, rightBaseBack, rightLock;
	private final SubTexture leftTop, leftTopFront, leftTopSide, leftTopBack, leftBottom, leftBaseFront, leftBaseSide, leftBaseBack, leftLock;

	private static final float offSet = 1.0f / 16.0f;
	private static final float height = offSet * 9.0f;

	public ChestNew(String name, String id, SubTexture single, SubTexture left, SubTexture right, SubTexture christmasSingle, SubTexture christmasLeft, SubTexture christmasRight)
	{
		this.name = name;
		this.id = id;

		final float texel = 1.0f / 64.0f;

		SubTexture singleChest = single;
		SubTexture doubleChestLeft = left;
		SubTexture doubleChestRight = right;

		Calendar calendar = Calendar.getInstance();
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		if(calendar.get(Calendar.MONTH) == Calendar.DECEMBER && (day == 24 || day == 25 || day == 26)
			&& left.texturePackVersion.getNumVersion() >= VERSION_5.getNumVersion()
                        && christmasSingle != null && christmasLeft != null && christmasRight != null)
		{
			singleChest = christmasSingle;
			doubleChestLeft = christmasLeft;
			doubleChestRight = christmasRight;
		}

		//These textures all have their UV coords reversed to rotate the texture

		// Single Chest textures
		smallTop = new SubTexture(singleChest.texture, singleChest.u0+texel*42, singleChest.v0+texel*14, singleChest.u0+texel*28, singleChest.v0);
		smallTopSide = new SubTexture(singleChest.texture, singleChest.u0+texel*14, singleChest.v0+texel*19, singleChest.u0, singleChest.v0+texel*14);
		smallTopFront = new SubTexture(singleChest.texture, singleChest.u0+texel*56, singleChest.v0+texel*19, singleChest.u0+texel*42, singleChest.v0+texel*14);
		smallBottom = new SubTexture(singleChest.texture, singleChest.u0+texel*28, singleChest.v0+texel*33, singleChest.u0+texel*14, singleChest.v0+texel*19);
		smallBaseSide = new SubTexture(singleChest.texture, singleChest.u0+texel*14, singleChest.v0+texel*42, singleChest.u0, singleChest.v0+texel*33);
		smallBaseFront = new SubTexture(singleChest.texture, singleChest.u0+texel*56, singleChest.v0+texel*42, singleChest.u0+texel*42, singleChest.v0+texel*33);
		smallLock = new SubTexture(singleChest.texture, singleChest.u0+texel*3, singleChest.v0+texel*5, singleChest.u0+texel*1, singleChest.v0+texel*1);

		float fudgeFactor = 0.05f;

		// Right side Double Chest textures
		rightTop = new SubTexture(doubleChestRight.texture, doubleChestRight.u0+texel*29, right.v0, doubleChestRight.u0+texel*(44-fudgeFactor), doubleChestRight.v0+texel*14);
		rightTopFront = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*58, doubleChestLeft.v0+texel*19, doubleChestLeft.u0+texel*(43+fudgeFactor), doubleChestLeft.v0+texel*14);
		rightTopSide = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*14, doubleChestLeft.v0+texel*19, doubleChestLeft.u0, doubleChestLeft.v0+texel*14);
		rightTopBack = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*(29-fudgeFactor), doubleChestLeft.v0+texel*19, doubleChestLeft.u0+texel*14, doubleChestLeft.v0+texel*14);
		rightBottom = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*14, doubleChestLeft.v0+texel*19, doubleChestLeft.u0+texel*29, doubleChestLeft.v0+texel*33);
		rightBaseFront = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*58, doubleChestLeft.v0+texel*42, doubleChestLeft.u0+texel*(43+fudgeFactor), doubleChestLeft.v0+texel*33);
		rightBaseSide = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*14, doubleChestLeft.v0+texel*42, doubleChestLeft.u0, doubleChestLeft.v0+texel*33);
		rightBaseBack = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*(29-fudgeFactor), doubleChestLeft.v0+texel*42, doubleChestLeft.u0+texel*14, doubleChestLeft.v0+texel*33);
		rightLock = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*4, doubleChestLeft.v0+texel*5, doubleChestLeft.u0+texel*3, doubleChestLeft.v0+texel*1);

		// Left side Double Chest textures
		leftTop = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*(29+fudgeFactor), doubleChestLeft.v0, doubleChestLeft.u0+texel*44, doubleChestLeft.v0+texel*14);
		leftTopFront = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*(58-fudgeFactor), doubleChestLeft.v0+texel*19, doubleChestLeft.u0+texel*43, doubleChestLeft.v0+texel*14);
		leftTopSide = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*43, doubleChestLeft.v0+texel*19, doubleChestLeft.u0+texel*29, doubleChestLeft.v0+texel*14);
		leftTopBack = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*29, doubleChestLeft.v0+texel*19, doubleChestLeft.u0+texel*(14+fudgeFactor), doubleChestLeft.v0+texel*14);
		leftBottom = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*14, doubleChestLeft.v0+texel*19, doubleChestLeft.u0+texel*29, doubleChestLeft.v0+texel*33);
		leftBaseFront = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*(58-fudgeFactor), doubleChestLeft.v0+texel*42, doubleChestLeft.u0+texel*43, doubleChestLeft.v0+texel*33);
		leftBaseSide = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*43, doubleChestLeft.v0+texel*42, doubleChestLeft.u0+texel*29, doubleChestLeft.v0+texel*33);
		leftBaseBack = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*29, doubleChestLeft.v0+texel*42, doubleChestLeft.u0+texel*(14+fudgeFactor), doubleChestLeft.v0+texel*33);
		leftLock = new SubTexture(doubleChestLeft.texture, doubleChestLeft.u0+texel*3, doubleChestLeft.v0+texel*5, doubleChestLeft.u0+texel*2, doubleChestLeft.v0+texel*1);
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
		final float topLight = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.Top);
		final float northSouthLight = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.NorthSouth);
		final float eastWestLight = world.getLight(chunk.getChunkCoord(), x, y, z, LightFace.EastWest);
		Vector4f topColor = new Vector4f(topLight, topLight, topLight, 1);
		Vector4f northSouthColor = new Vector4f(northSouthLight, northSouthLight, northSouthLight, 1);
		Vector4f eastWestColor = new Vector4f(eastWestLight, eastWestLight, eastWestLight, 1);

		Vector4f northSouthColorTemp = northSouthColor;
		Vector4f eastWestColorTemp = eastWestColor;


		SubMesh singleChestMesh = new SubMesh();
		SubMesh leftDoubleChestMesh = new SubMesh();
		SubMesh rightDoubleChestMesh = new SubMesh();

		Rotation horizRotation = Rotation.Clockwise;
		float horizAngle = 180;
                String type = null;
		final BlockProperties properties = chunk.getBlockState(x, y, z);
		if (properties != null) {
			switch (properties.get("facing")) {
				case "north":
					horizRotation = Rotation.AntiClockwise;
					horizAngle = 180;
					break;
				case "west":
					horizAngle = 270;
					northSouthColorTemp = eastWestColor;
					eastWestColorTemp = northSouthColor;
					break;
				case "east":
					horizAngle = 90;
					northSouthColorTemp = eastWestColor;
					eastWestColorTemp = northSouthColor;
					break;
				default:
                                        horizAngle = 0;
                                        break;
			}

			type = properties.get("type");
		}
                
                if (type != null && type.equals("left")) {
                        addLeftHalfDoubleChest(leftDoubleChestMesh, topColor, northSouthColorTemp, eastWestColorTemp);
                } else if (type != null && type.equals("right")) {
                        addRightHalfDoubleChest(rightDoubleChestMesh, topColor, northSouthColorTemp, eastWestColorTemp);
                } else {
                        addSingleChest(singleChestMesh, topColor, northSouthColorTemp, eastWestColorTemp);
                }

		singleChestMesh.pushTo(geometry.getMesh(smallTop.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, Rotation.None, 0);
		leftDoubleChestMesh.pushTo(geometry.getMesh(leftTop.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, Rotation.None, 0);
		rightDoubleChestMesh.pushTo(geometry.getMesh(rightTop.texture, Geometry.MeshType.Solid), x, y, z, horizRotation, horizAngle, Rotation.None, 0);
	}

	private void addRightHalfDoubleChest(SubMesh rightMesh, Vector4f topColor, Vector4f northSouthColor, Vector4f eastWestColor) {
		// Top
		rightMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(1, 1-offSet*2, 0+offSet),
				new Vector3f(1, 1-offSet*2, 1-offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet), topColor, rightTop);
		// West
		rightMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet),
				new Vector3f(0+offSet, height, 1-offSet),  new Vector3f(0+offSet, height, 0+offSet), eastWestColor, rightTopSide);
		// North
		rightMesh.addQuad(new Vector3f(1, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 0+offSet),
				new Vector3f(0+offSet, height, 0+offSet),  new Vector3f(1, height, 0+offSet), northSouthColor, rightTopBack);
		// South
		rightMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 1-offSet), new Vector3f(1, 1-offSet*2, 1-offSet),
				new Vector3f(1, height, 1-offSet),  new Vector3f(0+offSet, height, 1-offSet), northSouthColor, rightTopFront);


		//Chest bottom

		// Bottom
		rightMesh.addQuad(new Vector3f(0+offSet, 0, 0+offSet), new Vector3f(0+offSet, 0, 1-offSet),
				new Vector3f(1, 0, 1-offSet), new Vector3f(1, 0, 0+offSet), topColor, rightBottom);

		// West
		rightMesh.addQuad(new Vector3f(0+offSet, height, 0+offSet), new Vector3f(0+offSet, height, 1-offSet),
				new Vector3f(0+offSet, 0, 1-offSet),  new Vector3f(0+offSet, 0, 0+offSet), eastWestColor, rightBaseSide);
		// North
		rightMesh.addQuad(new Vector3f(1, height, 0+offSet), new Vector3f(0+offSet, height, 0+offSet),
				new Vector3f(0+offSet, 0, 0+offSet),  new Vector3f(1, 0, 0+offSet), northSouthColor, rightBaseBack);
		// South
		rightMesh.addQuad(new Vector3f(0+offSet, height, 1-offSet), new Vector3f(1, height, 1-offSet),
				new Vector3f(1, 0, 1-offSet),  new Vector3f(0+offSet, 0, 1-offSet), northSouthColor, rightBaseFront);

		SubMesh.addBlockSimple(rightMesh, 1-offSet, offSet*7, offSet*15, offSet, offSet*4, offSet, northSouthColor, rightLock, rightLock, rightLock);
	}

	private void addLeftHalfDoubleChest(SubMesh leftMesh, Vector4f topColor, Vector4f northSouthColor, Vector4f eastWestColor) {
		// Top
		leftMesh.addQuad(new Vector3f(0, 1-offSet*2, 0+offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
				new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(0, 1-offSet*2, 1-offSet), topColor, leftTop);
		// North
		leftMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 0+offSet), new Vector3f(0, 1-offSet*2, 0+offSet),
				new Vector3f(0, height, 0+offSet),  new Vector3f(1-offSet, height, 0+offSet), northSouthColor, leftTopBack);
		// South
		leftMesh.addQuad(new Vector3f(0, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 1-offSet),
				new Vector3f(1-offSet, height, 1-offSet),  new Vector3f(0, height, 1-offSet), northSouthColor, leftTopFront);
		// East
		leftMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
				new Vector3f(1-offSet, height, 0+offSet), new Vector3f(1-offSet, height, 1-offSet), eastWestColor, leftTopSide);


		//Chest bottom

		// Bottom
		leftMesh.addQuad(new Vector3f(0, 0, 0+offSet), new Vector3f(0, 0, 1-offSet),
				new Vector3f(1-offSet, 0, 1-offSet), new Vector3f(1-offSet, 0, 0+offSet), topColor, leftBottom);
		// North
		leftMesh.addQuad(new Vector3f(1-offSet, height, 0+offSet), new Vector3f(0, height, 0+offSet),
				new Vector3f(0, 0, 0+offSet),  new Vector3f(1-offSet, 0, 0+offSet), northSouthColor, leftBaseBack);
		// South
		leftMesh.addQuad(new Vector3f(0, height, 1-offSet), new Vector3f(1-offSet, height, 1-offSet),
				new Vector3f(1-offSet, 0, 1-offSet),  new Vector3f(0, 0, 1-offSet), northSouthColor, leftBaseFront);
		// East
		leftMesh.addQuad(new Vector3f(1-offSet, height, 1-offSet), new Vector3f(1-offSet, height, 0+offSet),
				new Vector3f(1-offSet, 0, 0+offSet), new Vector3f(1-offSet, 0, 1-offSet), eastWestColor, leftBaseSide);

		SubMesh.addBlockSimple(leftMesh, 0, offSet*7, offSet*15, offSet, offSet*4, offSet, northSouthColor, leftLock, leftLock, leftLock);
	}

	private void addSingleChest(SubMesh singleChestMesh, Vector4f topColor, Vector4f northSouthColor, Vector4f eastWestColor) {
		// Chest top

		// Top
		singleChestMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
				new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet), topColor, smallTop);
		// West
		singleChestMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 1-offSet),
				new Vector3f(0+offSet, height, 1-offSet),  new Vector3f(0+offSet, height, 0+offSet), eastWestColor, smallTopSide);
		// North
		singleChestMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 0+offSet), new Vector3f(0+offSet, 1-offSet*2, 0+offSet),
				new Vector3f(0+offSet, height, 0+offSet),  new Vector3f(1-offSet, height, 0+offSet), northSouthColor, smallTopSide);
		// South
		singleChestMesh.addQuad(new Vector3f(0+offSet, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 1-offSet),
				new Vector3f(1-offSet, height, 1-offSet),  new Vector3f(0+offSet, height, 1-offSet), northSouthColor, smallTopFront);
		// East
		singleChestMesh.addQuad(new Vector3f(1-offSet, 1-offSet*2, 1-offSet), new Vector3f(1-offSet, 1-offSet*2, 0+offSet),
				new Vector3f(1-offSet, height, 0+offSet), new Vector3f(1-offSet, height, 1-offSet), eastWestColor, smallTopSide);


		//Chest bottom

		// Bottom
		singleChestMesh.addQuad(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1),
				new Vector3f(1, 0, 1), new Vector3f(1, 0, 0), topColor, smallBottom);

		// West
		singleChestMesh.addQuad(new Vector3f(0+offSet, height, 0+offSet), new Vector3f(0+offSet, height, 1-offSet),
				new Vector3f(0+offSet, 0, 1-offSet),  new Vector3f(0+offSet, 0, 0+offSet), eastWestColor, smallBaseSide);
		// North
		singleChestMesh.addQuad(new Vector3f(1-offSet, height, 0+offSet), new Vector3f(0+offSet, height, 0+offSet),
				new Vector3f(0+offSet, 0, 0+offSet),  new Vector3f(1-offSet, 0, 0+offSet), northSouthColor, smallBaseSide);
		// South
		singleChestMesh.addQuad(new Vector3f(0+offSet, height, 1-offSet), new Vector3f(1-offSet, height, 1-offSet),
				new Vector3f(1-offSet, 0, 1-offSet),  new Vector3f(0+offSet, 0, 1-offSet), northSouthColor, smallBaseFront);
		// East
		singleChestMesh.addQuad(new Vector3f(1-offSet, height, 1-offSet), new Vector3f(1-offSet, height, 0+offSet),
				new Vector3f(1-offSet, 0, 0+offSet), new Vector3f(1-offSet, 0, 1-offSet), eastWestColor, smallBaseSide);

		SubMesh.addBlockSimple(singleChestMesh, offSet*7, offSet*7, offSet*15, offSet*2, offSet*4, offSet, northSouthColor, smallLock, smallLock, smallLock);
	}
}
