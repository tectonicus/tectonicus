/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.rasteriser;

import lombok.experimental.UtilityClass;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tectonicus.BlockContext;
import tectonicus.Chunk;
import tectonicus.Util;
import tectonicus.Version;
import tectonicus.blockTypes.BlockModel;
import tectonicus.blockTypes.BlockModel.BlockElement;
import tectonicus.blockTypes.BlockModel.BlockElement.ElementFace;
import tectonicus.blockTypes.BlockStateWrapper;
import tectonicus.blockTypes.BlockUtil;
import tectonicus.configuration.LightFace;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.renderer.Geometry.MeshType;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

import java.util.List;
import java.util.Map;

@UtilityClass
public class MeshUtil
{

	public void addCube(final float x, final float y, final float z, Vector4f colour, final boolean addTop,
							   final boolean addNorth, final boolean addSouth, final boolean addEast, final boolean addWest,
							   Mesh geometry)
	{
		final float inc = 0.2f;
		Vector4f lightColour = new Vector4f(colour.x + inc, colour.y + inc, colour.z + inc, colour.w);
		Vector4f darkColour = new Vector4f(colour.x - inc, colour.y - inc, colour.z - inc, colour.w);
		
		BlockUtil.clamp(lightColour);
		BlockUtil.clamp(darkColour);
		
		// Top
		if (addTop)
		{
			geometry.addVertex(new Vector3f(x, y+1, z), colour, 0, 0);
			geometry.addVertex(new Vector3f(x+1, y+1, z), colour, 1, 0);
			geometry.addVertex(new Vector3f(x+1, y+1, z+1), colour, 1, 1);
			geometry.addVertex(new Vector3f(x, y+1, z+1), colour, 0, 1);
		}
		
		// North face
		if (addNorth)
		{
			geometry.addVertex(new Vector3f(x, y, z), darkColour, 0, 0);
			geometry.addVertex(new Vector3f(x, y+1, z), darkColour, 1, 0);
			geometry.addVertex(new Vector3f(x, y+1, z+1), darkColour, 1, 1);
			geometry.addVertex(new Vector3f(x, y, z+1), darkColour, 0, 1);	
		}
		
		// South
		if (addSouth)
		{
			geometry.addVertex(new Vector3f(x+1, y, z), darkColour, 0, 0);
			geometry.addVertex(new Vector3f(x+1, y, z+1), darkColour, 0, 1);
			geometry.addVertex(new Vector3f(x+1, y+1, z+1), darkColour, 1, 1);
			geometry.addVertex(new Vector3f(x+1, y+1, z), darkColour, 1, 0);
		}
		
		// East
		if (addEast)
		{
			geometry.addVertex(new Vector3f(x+1, y+1, z), lightColour, 1, 1);
			geometry.addVertex(new Vector3f(x, y+1, z), lightColour, 0, 1);
			geometry.addVertex(new Vector3f(x, y, z), lightColour, 0, 0);
			geometry.addVertex(new Vector3f(x+1, y, z), lightColour, 1, 0);
		}
		
		// West
		if (addWest)
		{
			geometry.addVertex(new Vector3f(x, y, z+1), lightColour, 0, 0);
			geometry.addVertex(new Vector3f(x, y+1, z+1), lightColour, 0, 1);
			geometry.addVertex(new Vector3f(x+1, y+1, z+1), lightColour, 1, 1);
			geometry.addVertex(new Vector3f(x+1, y, z+1), lightColour, 1, 0);
		}
	}

	public void addQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, SubTexture texture)
	{
		mesh.addVertex(p0, colour, texture.u0, texture.v0);
		mesh.addVertex(p1, colour, texture.u1, texture.v0);
		mesh.addVertex(p2, colour, texture.u1, texture.v1);
		mesh.addVertex(p3, colour, texture.u0, texture.v1);
	}
	
	public void addDoubleSidedQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, SubTexture texture)
	{
		// Clockwise
		mesh.addVertex(p0, colour, texture.u0, texture.v0);
		mesh.addVertex(p1, colour, texture.u1, texture.v0);
		mesh.addVertex(p2, colour, texture.u1, texture.v1);
		mesh.addVertex(p3, colour, texture.u0, texture.v1);
		
		// Anticlockwise
		mesh.addVertex(p0, colour, texture.u0, texture.v0);
		mesh.addVertex(p3, colour, texture.u0, texture.v1);
		mesh.addVertex(p2, colour, texture.u1, texture.v1);
		mesh.addVertex(p1, colour, texture.u1, texture.v0);
	}
	
	public void addDoubleSidedQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, Vector2f uv0, Vector2f uv1, Vector2f uv2, Vector2f uv3)
	{
		// Clockwise
		mesh.addVertex(p0, colour, uv0.x, uv0.y);
		mesh.addVertex(p1, colour, uv1.x, uv1.y);
		mesh.addVertex(p2, colour, uv2.x, uv2.y);
		mesh.addVertex(p3, colour, uv3.x, uv3.y);
		
		// Anticlockwise
		mesh.addVertex(p0, colour, uv0.x, uv0.y);
		mesh.addVertex(p3, colour, uv3.x, uv3.y);
		mesh.addVertex(p2, colour, uv2.x, uv2.y);
		mesh.addVertex(p1, colour, uv1.x, uv1.y);
	}

	public void addQuad(Mesh mesh, Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector4f colour, Vector2f uv0, Vector2f uv1, Vector2f uv2, Vector2f uv3)
	{
		mesh.addVertex(p0, colour, uv0.x, uv0.y);
		mesh.addVertex(p1, colour, uv1.x, uv1.y);
		mesh.addVertex(p2, colour, uv2.x, uv2.y);
		mesh.addVertex(p3, colour, uv3.x, uv3.y);
	}
	
	public void addBlock(BlockContext world, RawChunk rawChunk, int x, int y, int z, BlockModel model, Geometry geometry, int xRotation, int yRotation)
	{
		List<BlockElement> elements = model.getElements();

		boolean selfFull = true;
		boolean above;
		boolean below;
		boolean north;
		boolean south;
		boolean east;
		boolean west;

		float topLight;
		float bottomLight;
		float northLight;
		float southLight;
		float eastLight;
		float westLight;

		Version textureVersion = world.getTexturePack().getVersion();
		if (textureVersion.getNumVersion() <= Version.VERSION_12.getNumVersion()) {
			above = world.getBlockType(rawChunk.getChunkCoord(), x, y + 1, z).isSolid();
			below = world.getBlockType(rawChunk.getChunkCoord(), x, y - 1, z).isSolid();
			north = world.getBlockType(rawChunk.getChunkCoord(), x, y, z - 1).isSolid();
			south = world.getBlockType(rawChunk.getChunkCoord(), x, y, z + 1).isSolid();
			east = world.getBlockType(rawChunk.getChunkCoord(), x + 1, y, z).isSolid();
			west = world.getBlockType(rawChunk.getChunkCoord(), x - 1, y, z).isSolid();

			topLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
			bottomLight = world.getLight(rawChunk.getChunkCoord(), x, y - 1, z, LightFace.Top);
			northLight = world.getLight(rawChunk.getChunkCoord(), x, y, z - 1, LightFace.NorthSouth);
			southLight = world.getLight(rawChunk.getChunkCoord(), x, y, z + 1, LightFace.NorthSouth);
			eastLight = world.getLight(rawChunk.getChunkCoord(), x + 1, y, z, LightFace.EastWest);
			westLight = world.getLight(rawChunk.getChunkCoord(), x - 1, y, z, LightFace.EastWest);
		} else {
			BlockStateWrapper selfBlock = world.getBlock(rawChunk, x, y, z);
			BlockStateWrapper aboveBlock = world.getBlock(rawChunk.getChunkCoord(), x, y+1, z);
			BlockStateWrapper belowBlock = world.getBlock(rawChunk.getChunkCoord(), x, y-1, z);
			BlockStateWrapper northBlock = world.getBlock(rawChunk.getChunkCoord(), x, y, z-1);
			BlockStateWrapper southBlock = world.getBlock(rawChunk.getChunkCoord(), x, y, z+1);
			BlockStateWrapper eastBlock = world.getBlock(rawChunk.getChunkCoord(), x+1, y, z);
			BlockStateWrapper westBlock = world.getBlock(rawChunk.getChunkCoord(), x-1, y, z);

			selfFull = selfBlock.isFullBlock();
			if (selfBlock.isTransparent()) {
				above = aboveBlock.isFullBlock();
				below = belowBlock.isFullBlock();
				north = northBlock.isFullBlock();
				south = southBlock.isFullBlock();
				east = eastBlock.isFullBlock();
				west = westBlock.isFullBlock();
			} else {
				above = aboveBlock.isFullBlock() && !aboveBlock.isTransparent();
				below = belowBlock.isFullBlock() && !belowBlock.isTransparent();
				north = northBlock.isFullBlock() && !northBlock.isTransparent();
				south = southBlock.isFullBlock() && !southBlock.isTransparent();
				east = eastBlock.isFullBlock() && !eastBlock.isTransparent();
				west = westBlock.isFullBlock() && !westBlock.isTransparent();
			}

			//If the block is covered by solid blocks then skip it
			if (above && north && south && east && west)
				return;

			//Handle some special cases e.g. double slabs, 8 layer snow
			if (!selfFull && model.isFullBlock()) {
				selfFull = true;
			}

			topLight = world.getLight(rawChunk.getChunkCoord(), x, y+1, z, LightFace.Top);
			bottomLight = world.getLight(rawChunk.getChunkCoord(), x, y - 1, z, LightFace.Top);
			northLight = world.getLight(rawChunk.getChunkCoord(), x, y, z - 1, LightFace.NorthSouth);
			southLight = world.getLight(rawChunk.getChunkCoord(), x, y, z + 1, LightFace.NorthSouth);
			eastLight = world.getLight(rawChunk.getChunkCoord(), x + 1, y, z, LightFace.EastWest);
			westLight = world.getLight(rawChunk.getChunkCoord(), x - 1, y, z, LightFace.EastWest);

			String blockName = selfBlock.getBlockName();
			if(!selfFull) {
				//Since stairs and slabs don't contain any lighting value we have to approximate it, it's not perfect but good enough for now
				if(blockName.contains("stair") || blockName.contains("slab")) {
					boolean aboveStairOrSlab = aboveBlock.getBlockName().contains("stair") || aboveBlock.getBlockName().contains("slab");
					boolean belowStairOrSlab = belowBlock.getBlockName().contains("stair") || belowBlock.getBlockName().contains("slab");
					boolean northStairOrSlab = northBlock.getBlockName().contains("stair") || northBlock.getBlockName().contains("slab");
					boolean southStairOrSlab = southBlock.getBlockName().contains("stair") || southBlock.getBlockName().contains("slab");
					boolean eastStairOrSlab = eastBlock.getBlockName().contains("stair") || eastBlock.getBlockName().contains("slab");
					boolean westStairOrSlab = westBlock.getBlockName().contains("stair") || westBlock.getBlockName().contains("slab");

					float lightTotal = 0;
					int count = 0;
					if (!above && !aboveStairOrSlab) {
						lightTotal += topLight;
						count++;
					}
					if (!below && !belowStairOrSlab) {
						lightTotal += bottomLight;
						count++;
					}
					if (!north && !northStairOrSlab) {
						lightTotal += northLight;
						count++;
					}
					if (!south && !southStairOrSlab) {
						lightTotal += southLight;
						count++;
					}
					if (!east && !eastStairOrSlab) {
						lightTotal += eastLight;
						count++;
					}
					if (!west && !westStairOrSlab) {
						lightTotal += westLight;
						count++;
					}

					float lightAvg = 0;
					if (count > 0) {
						lightAvg = Util.clamp(lightTotal / count, 0, 1);
					}

					topLight = bottomLight = northLight = southLight = eastLight = westLight = lightAvg;
				} else {
					float selfTopLight = Chunk.getLight(world.getLightStyle(), LightFace.Top, rawChunk, x, y, z);
					float selfNorthSouthLight = Chunk.getLight(world.getLightStyle(), LightFace.NorthSouth, rawChunk, x, y, z);
					float selfEastWestLight = Chunk.getLight(world.getLightStyle(), LightFace.EastWest, rawChunk, x, y, z);
					topLight = bottomLight = selfTopLight;
					northLight = southLight = selfNorthSouthLight;
					eastLight = westLight = selfEastWestLight;
				}
			}
		}


		boolean upFaceCovered = above;
		boolean downFaceCovered = below;
		boolean northFaceCovered = north;
		boolean southFaceCovered = south;
		boolean eastFaceCovered = east;
		boolean westFaceCovered = west;

		float topLightTemp = topLight;
		float bottomLightTemp = bottomLight;
		float northLightTemp = northLight;
		float southLightTemp = southLight;
		float eastLightTemp = eastLight;
		float westLightTemp = westLight;

		Matrix4f blockRotation = null;

		if (xRotation != 0 || yRotation != 0) {
			Vector3f rotOrigin = new Vector3f(x + 0.5f, y + 0.5f, z + 0.5f);
			blockRotation = new Matrix4f().translate(rotOrigin)
					.rotate(-(float) Math.toRadians(yRotation), 0, 1, 0)
					.rotate(-(float) Math.toRadians(xRotation), 1, 0, 0)
					.translate(rotOrigin.negate());

			if (Math.abs(xRotation) == 90 && yRotation == 0) {
				upFaceCovered = north;
				downFaceCovered = south;
				northFaceCovered = below;
				southFaceCovered = above;

				if (selfFull) {
					topLightTemp = northLight;
					bottomLightTemp = southLight;
					northLightTemp = bottomLight;
					southLightTemp = topLight;
				}
			} else if (xRotation == 180 && yRotation == 0) {
				upFaceCovered = below;
				downFaceCovered = above;
				northFaceCovered = south;
				southFaceCovered = north;

				if (selfFull) {
					topLightTemp = bottomLight;
					bottomLightTemp = topLight;
					northLightTemp = southLight;
					southLightTemp = northLight;
				}
			} else if (Math.abs(xRotation) == 270 && yRotation == 0) {
				upFaceCovered = south;
				downFaceCovered = north;
				northFaceCovered = above;
				southFaceCovered = below;

				if (selfFull) {
					topLightTemp = southLight;
					bottomLightTemp = northLight;
					northLightTemp = topLight;
					southLightTemp = bottomLight;
				}
			} else if (yRotation == 90 && xRotation == 0 || yRotation == -270 && xRotation == 0) {
				northFaceCovered = east;
				southFaceCovered = west;
				eastFaceCovered = south;
				westFaceCovered = north;

				if (selfFull) {
					northLightTemp = eastLight;
					southLightTemp = westLight;
					eastLightTemp = southLight;
					westLightTemp = northLight;
				}
			} else if (yRotation == 90 && xRotation == 90) {
				upFaceCovered = east;
				downFaceCovered = west;
				northFaceCovered = below;
				southFaceCovered = above;
				eastFaceCovered = south;
				westFaceCovered = north;
				if (selfFull) {
					topLightTemp = eastLight;
					bottomLightTemp = westLight;
					northLightTemp = bottomLight;
					southLightTemp = topLight;
					eastLightTemp = southLight;
					westLightTemp = northLight;
				}
			} else if (yRotation == 90 && xRotation == 180) {
				upFaceCovered = below;
				downFaceCovered = above;
				northFaceCovered = west;
				southFaceCovered = east;
				eastFaceCovered = south;
				westFaceCovered = north;

				if (selfFull) {
					topLightTemp = bottomLight;
					bottomLightTemp = topLight;
					northLightTemp = westLight;
					southLightTemp = eastLight;
					eastLightTemp = southLight;
					westLightTemp = northLight;
				}
			} else if (yRotation == 90 && xRotation == 270) {
				upFaceCovered = west;
				downFaceCovered = east;
				northFaceCovered = above;
				southFaceCovered = below;
				eastFaceCovered = south;
				westFaceCovered = north;

				if (selfFull) {
					topLightTemp = westLight;
					bottomLightTemp = eastLight;
					northLightTemp = topLight;
					southLightTemp = bottomLight;
					eastLightTemp = southLight;
					westLightTemp = northLight;
				}
			} else if (Math.abs(yRotation) == 180 && xRotation == 0) {
				northFaceCovered = south;
				southFaceCovered = north;
				eastFaceCovered = west;
				westFaceCovered = east;

				if (selfFull) {
					northLightTemp = southLight;
					southLightTemp = northLight;
					eastLightTemp = westLight;
					westLightTemp = eastLight;
				}
			} else if (Math.abs(yRotation) == 180 && xRotation == 90) {
				upFaceCovered = south;
				downFaceCovered = north;
				northFaceCovered = below;
				southFaceCovered = above;
				eastFaceCovered = west;
				westFaceCovered = east;

				if (selfFull) {
					topLightTemp = southLight;
					bottomLightTemp = northLight;
					northLightTemp = bottomLight;
					southLightTemp = topLight;
					eastLightTemp = westLight;
					westLightTemp = eastLight;
				}
			} else if (Math.abs(yRotation) == 180 && xRotation == 180) {
				upFaceCovered = below;
				downFaceCovered = above;
				eastFaceCovered = west;
				westFaceCovered = east;

				if (selfFull) {
					topLightTemp = bottomLight;
					bottomLightTemp = topLight;
					eastLightTemp = westLight;
					westLightTemp = eastLight;
				}
			} else if (Math.abs(yRotation) == 180 && xRotation == 270) {
				upFaceCovered = north;
				downFaceCovered = south;
				northFaceCovered = above;
				southFaceCovered = below;
				eastFaceCovered = west;
				westFaceCovered = east;

				if (selfFull) {
					topLightTemp = northLight;
					bottomLightTemp = southLight;
					northLightTemp = topLight;
					southLightTemp = bottomLight;
					eastLightTemp = westLight;
					westLightTemp = eastLight;
				}
			} else if (yRotation == 270 && xRotation == 0 || yRotation == -90 && xRotation == 0) {
				northFaceCovered = west;
				southFaceCovered = east;
				eastFaceCovered = north;
				westFaceCovered = south;

				if (selfFull) {
					northLightTemp = westLight;
					southLightTemp = eastLight;
					eastLightTemp = northLight;
					westLightTemp = southLight;
				}
			} else if (yRotation == 270 && xRotation == 90) {
				upFaceCovered = west;
				downFaceCovered = east;
				northFaceCovered = below;
				southFaceCovered = above;
				eastFaceCovered = north;
				westFaceCovered = south;

				if (selfFull) {
					topLightTemp = westLight;
					bottomLightTemp = eastLight;
					northLightTemp = bottomLight;
					southLightTemp = topLight;
					eastLightTemp = northLight;
					westLightTemp = southLight;
				}
			} else if (yRotation == 270 && xRotation == 180) {
				upFaceCovered = below;
				downFaceCovered = above;
				northFaceCovered = east;
				southFaceCovered = west;
				eastFaceCovered = north;
				westFaceCovered = south;

				if (selfFull) {
					topLightTemp = bottomLight;
					bottomLightTemp = topLight;
					northLightTemp = eastLight;
					southLightTemp = westLight;
					eastLightTemp = northLight;
					westLightTemp = southLight;
				}
			} else if (yRotation == 270 && xRotation == 270) {
				upFaceCovered = east;
				downFaceCovered = west;
				northFaceCovered = above;
				southFaceCovered = below;
				eastFaceCovered = north;
				westFaceCovered = south;

				if (selfFull) {
					topLightTemp = eastLight;
					bottomLightTemp = westLight;
					northLightTemp = topLight;
					southLightTemp = bottomLight;
					eastLightTemp = northLight;
					westLightTemp = southLight;
				}
			}
		}

		for(BlockElement element : elements)
		{
			Matrix4f elementRotation = null;
			if (element.getRotationAngle() != 0) {
				float xrot = x + element.getRotationOrigin().x()/16;
				float yrot = y + element.getRotationOrigin().y()/16;
				float zrot = z + element.getRotationOrigin().z()/16;
				Vector3f rotationOrigin = new Vector3f(xrot, yrot, zrot);
				Vector3f rotationAxis = element.getRotationAxis();

				if (element.isScaled()) {
					elementRotation = new Matrix4f().translate(rotationOrigin)
							.rotate((float) Math.toRadians(element.getRotationAngle()), rotationAxis.x, rotationAxis.y, rotationAxis.z)
							.scale(1, 1, 1.4f)  //TODO: this needs work
							.translate(rotationOrigin.negate());
				} else {
					elementRotation = new Matrix4f().translate(rotationOrigin)
							.rotate((float) Math.toRadians(element.getRotationAngle()), rotationAxis.x, rotationAxis.y, rotationAxis.z)
							.translate(rotationOrigin.negate());
				}
			}
			
			float x1 = x + element.getFrom().x()/16;
			float y1 = y + element.getFrom().y()/16;
			float z1 = z + element.getFrom().z()/16;
	        
			float x2 = x + element.getTo().x()/16;
			float y2 = y + element.getTo().y()/16;
			float z2 = z + element.getTo().z()/16;
	
	        Vector3f topLeft, topRight, bottomRight, bottomLeft;
			Map<String, ElementFace> faces = element.getFaces();
			ElementFace upFace = faces.get("up");
			ElementFace downFace = faces.get("down");
			ElementFace northFace = faces.get("north");
			ElementFace southFace = faces.get("south");
			ElementFace eastFace = faces.get("east");
			ElementFace westFace = faces.get("west");

			boolean isGrassOverlay = false;
			float fudgeFactor = 0;

			//Set the tint color if any
			Colour4f tintColor = new Colour4f();
			if ((upFace != null && upFace.isTinted()) || (downFace != null && downFace.isTinted())
					|| (northFace != null && northFace.isTinted()) || (southFace != null && southFace.isTinted())
					|| (eastFace != null && eastFace.isTinted()) || (westFace != null && westFace.isTinted())) {

				String modelName = model.getName();

				tintColor = getTintColor(modelName, rawChunk, world, x, y, z, element);

				//Grass block side overlay hack
				if (modelName.contains("grass_block") && element.getFaces().size() == 4) {
					isGrassOverlay = true;
					fudgeFactor = 0.0001f;
				}
			}

			if (upFace != null && !(upFaceCovered && upFace.isFaceCulled()))
	        {
				Colour4f color = new Colour4f(topLightTemp, topLightTemp, topLightTemp, 1);
	        	if (upFace.isTinted()) {
	        		color.multiply(tintColor);
				}
				
				topLeft = new Vector3f(x1, y2, z1);
		        topRight = new Vector3f(x2, y2, z1);
		        bottomRight = new Vector3f(x2, y2, z2);
		        bottomLeft = new Vector3f(x1, y2, z2);

		        addVertices(geometry, color, upFace, topLeft, topRight, bottomRight, bottomLeft, elementRotation, blockRotation, model, false);
	        }
			
			if (downFace != null && !(downFaceCovered && downFace.isFaceCulled()))
	        {
				Colour4f color = new Colour4f(bottomLightTemp, bottomLightTemp, bottomLightTemp, 1);
				if (downFace.isTinted()) {
					color.multiply(tintColor);
				}
				
				topLeft = new Vector3f(x1, y1, z2);
		        topRight = new Vector3f(x2, y1, z2);
		        bottomRight = new Vector3f(x2, y1, z1);
		        bottomLeft = new Vector3f(x1, y1, z1);

		        addVertices(geometry, color, downFace, topLeft, topRight, bottomRight, bottomLeft, elementRotation, blockRotation, model, false);
	        }

			if (northFace != null && !(northFaceCovered && northFace.isFaceCulled()))
	        {
				Colour4f color = new Colour4f(northLightTemp, northLightTemp, northLightTemp, 1);
				if (northFace.isTinted()) {
					color.multiply(tintColor);
				}

				topLeft = new Vector3f(x2, y2, z1 - fudgeFactor);
		        topRight = new Vector3f(x1, y2, z1 - fudgeFactor);
		        bottomRight = new Vector3f(x1, y1, z1 - fudgeFactor);
		        bottomLeft = new Vector3f(x2, y1, z1 - fudgeFactor);

		        addVertices(geometry, color, northFace, topLeft, topRight, bottomRight, bottomLeft, elementRotation, blockRotation, model, isGrassOverlay);
	        }
			
			if (southFace != null && !(southFaceCovered && southFace.isFaceCulled()))
	        {
				Colour4f color = new Colour4f(southLightTemp, southLightTemp, southLightTemp, 1);
				if (southFace.isTinted()) {
					color.multiply(tintColor);
				}

				topLeft = new Vector3f(x1, y2, z2 + fudgeFactor);
		        topRight = new Vector3f(x2, y2, z2 + fudgeFactor);
		        bottomRight = new Vector3f(x2, y1, z2 + fudgeFactor);
		        bottomLeft = new Vector3f(x1, y1, z2 + fudgeFactor);

		        addVertices(geometry, color, southFace, topLeft, topRight, bottomRight, bottomLeft, elementRotation, blockRotation, model, isGrassOverlay);
	        }

			if (eastFace != null && !(eastFaceCovered && eastFace.isFaceCulled()))
	        {
				Colour4f color = new Colour4f(eastLightTemp, eastLightTemp, eastLightTemp, 1);
				if (eastFace.isTinted()) {
					color.multiply(tintColor);
				}

				topLeft = new Vector3f(x2 + fudgeFactor, y2, z2);
		        topRight = new Vector3f(x2 + fudgeFactor, y2, z1);
		        bottomRight = new Vector3f(x2 + fudgeFactor, y1, z1);
		        bottomLeft = new Vector3f(x2 + fudgeFactor, y1, z2);

		        addVertices(geometry, color, eastFace, topLeft, topRight, bottomRight, bottomLeft, elementRotation, blockRotation, model, isGrassOverlay);
	        }

			if (westFace != null && !(westFaceCovered && westFace.isFaceCulled()))
	        {
				Colour4f color = new Colour4f(westLightTemp, westLightTemp, westLightTemp, 1);
				if (westFace.isTinted()) {
					color.multiply(tintColor);
				}

				topLeft = new Vector3f(x1 - fudgeFactor, y2, z1);
		        topRight = new Vector3f(x1 - fudgeFactor, y2, z2);
		        bottomRight = new Vector3f(x1 - fudgeFactor, y1, z2);
		        bottomLeft = new Vector3f(x1 - fudgeFactor, y1, z1);

		        addVertices(geometry, color, westFace, topLeft, topRight, bottomRight, bottomLeft, elementRotation, blockRotation, model, isGrassOverlay);
	        }
		}
	}

	private Colour4f getTintColor(String modelName, RawChunk rawChunk, BlockContext world, int x, int y, int z, BlockElement element) {
		Colour4f tintColor;

		if (modelName.contains("cauldron")) {
			tintColor = world.getWaterColor(rawChunk, x, y, z);
		} else if (modelName.contains("redstone")) {
			final float power = (Integer.parseInt(rawChunk.getBlockState(x, y, z).get("power")) / 16.0f);
			tintColor = new Colour4f(Util.clamp(power + 0.25f, 0, 1), 0.2f * power, 0.2f * power, 1);
		} else if (modelName.contains("stem") && !modelName.contains("mushroom")) {
			BlockProperties stemProperties = rawChunk.getBlockState(x, y, z);
			int age;
			if (stemProperties.containsKey("age")) {
				age = Integer.parseInt(rawChunk.getBlockState(x, y, z).get("age"));
			} else {
				age = 7;
			}
			tintColor = new Colour4f(age*32/255f, (255-age*8)/255f, age*4/255f);
		} else if (modelName.contains("lily_pad")) {
			tintColor = new Colour4f(32/255f, 128/255f, 48/255f);
		} else if (modelName.contains("spruce_leaves")) {
			tintColor = new Colour4f(97/255f, 153/255f, 97/255f);
		} else if (modelName.contains("birch_leaves")) {
			tintColor = new Colour4f(128/255f, 167/255f, 85/255f);
		} else if (modelName.contains("oak_leaves") || modelName.contains("jungle_leaves")
				|| modelName.contains("acacia_leaves") || modelName.contains("vines")) {
			tintColor = world.getFoliageColor(rawChunk.getChunkCoord(), x, y, z);
		} else {
			tintColor = world.getGrassColor(rawChunk.getChunkCoord(), x, y, z);
		}

		//Stonecutter has tintindex but does not need tint

		return tintColor;
	}
	
	private void addVertices(Geometry geometry, Colour4f color, ElementFace face, Vector3f topLeft, Vector3f topRight,
							 Vector3f bottomRight, Vector3f bottomLeft, Matrix4f elementRotation, Matrix4f blockRotation, BlockModel model, boolean isGrassOverlay)
	{
		if(elementRotation != null)
        {
	        elementRotation.transformPosition(topLeft);
	        elementRotation.transformPosition(topRight);
	        elementRotation.transformPosition(bottomRight);
	        elementRotation.transformPosition(bottomLeft);
        }

		if (blockRotation != null) {
			blockRotation.transformPosition(topLeft);
			blockRotation.transformPosition(topRight);
			blockRotation.transformPosition(bottomRight);
			blockRotation.transformPosition(bottomLeft);
		}

		SubTexture tex = face.getTexture();
		Mesh mesh;
		if (model.isSolid() || model.getName().contains("grass_block") && !isGrassOverlay) {
			mesh = geometry.getMesh(tex.texture, MeshType.Solid);
		} else if (model.isTranslucent()) {
			mesh = geometry.getMesh(tex.texture, MeshType.Transparent);
		} else {
			mesh = geometry.getMesh(tex.texture, MeshType.AlphaTest);
		}


		//TODO: if the block has ambient occlusion we should figure out smooth lighting
		
		int texRotation = face.getTextureRotation();
		if(texRotation == 0)
		{
			mesh.addVertex(topLeft, color, tex.u0, tex.v0);
			mesh.addVertex(topRight, color, tex.u1, tex.v0);
			mesh.addVertex(bottomRight, color, tex.u1, tex.v1);
			mesh.addVertex(bottomLeft, color, tex.u0, tex.v1);
		}
		else if (texRotation == 90)
		{
			mesh.addVertex(topLeft, color, tex.u0, tex.v1);
			mesh.addVertex(topRight, color, tex.u0, tex.v0);
			mesh.addVertex(bottomRight, color, tex.u1, tex.v0);
			mesh.addVertex(bottomLeft, color, tex.u1, tex.v1);
		}
		else if (texRotation == 180)
		{
			mesh.addVertex(topLeft, color, tex.u1, tex.v1);
			mesh.addVertex(topRight, color, tex.u0, tex.v1);
			mesh.addVertex(bottomRight, color, tex.u0, tex.v0);
			mesh.addVertex(bottomLeft, color, tex.u1, tex.v0);
		}
		else if (texRotation == 270)
		{
			mesh.addVertex(topLeft, color, tex.u1, tex.v0);
			mesh.addVertex(topRight, color, tex.u1, tex.v1);
			mesh.addVertex(bottomRight, color, tex.u0, tex.v1);
			mesh.addVertex(bottomLeft, color, tex.u0, tex.v0);
		}
	}
}
