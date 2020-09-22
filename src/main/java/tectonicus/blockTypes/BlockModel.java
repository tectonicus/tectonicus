/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import tectonicus.BlockContext;
import tectonicus.rasteriser.MeshUtil;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.PackTexture;
import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.ImageUtils;
import tectonicus.util.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Log4j2
@Getter
public class BlockModel
{
	private final String name;
	private final boolean ambientlyOccluded;
	private final List<BlockElement> elements;
	@Setter
	private boolean isSolid = true;
	@Setter
	private boolean isTransparent = false;
	@Setter
	private boolean isFullBlock = true;

	private static final String ELEMENTS_FIELD = "elements";
	private static final String TEXTURES_FIELD = "textures";
	private static final String ROTATION_FIELD = "rotation";
	
	public BlockModel(String name, boolean ambientlyOccluded, Map<String, String> combineMap, JsonNode elementsNode, TexturePack texturePack)
	{
		this.name = name;
		this.ambientlyOccluded = ambientlyOccluded;
		if (elementsNode != null) {
			this.elements = deserializeBlockElements(combineMap, elementsNode, texturePack, this);
		} else {
			this.isFullBlock = false;  //This assumes that any block with no elements is not a full block, this isn't correct (shulker box) but works fine
			this.elements = Collections.emptyList();
		}
	}
	
	public void createGeometry(int x, int y, int z, BlockContext world, RawChunk rawChunk, Geometry geometry, int xRotation, int yRotation)
	{
		MeshUtil.addBlock(world, rawChunk, x, y, z, this, geometry, xRotation, yRotation);
	}

	private List<BlockElement> deserializeBlockElements(Map<String, String> combineMap, JsonNode elements, TexturePack texturePack, BlockModel blockModel)
	{
		List<BlockElement> elementsList = new ArrayList<>();

		for(JsonNode element : elements)
		{
			JsonNode from = element.get("from");
			Vector3f fromVector = new Vector3f(from.get(0).floatValue(), from.get(1).floatValue(), from.get(2).floatValue());
			JsonNode to = element.get("to");
			Vector3f toVector = new Vector3f(to.get(0).floatValue(), to.get(1).floatValue(), to.get(2).floatValue());

			//Test if not full-block (grass path and farmland are treated as full blocks)
			if (fromVector.x() > 0 && fromVector.x() < 16 || fromVector.y() > 0 && fromVector.y() < 16 || fromVector.z() > 0 && fromVector.z() < 16
					|| toVector.x() > 0 && toVector.x() < 16 || toVector.y() > 0 && toVector.y() < 15 || toVector.z() > 0 && toVector.z() < 16) {
				blockModel.setFullBlock(false);
			}

			org.joml.Vector3f rotationOrigin = new org.joml.Vector3f(8.0f, 8.0f, 8.0f);
			org.joml.Vector3f rotAxis = new org.joml.Vector3f(0.0f, 1.0f, 0.0f);
			float rotationAngle = 0;
			boolean rotationScale = false;

			if(element.has(ROTATION_FIELD))
			{
				JsonNode rot = element.get(ROTATION_FIELD);
				JsonNode rotOrigin = rot.get("origin");
				rotationOrigin = new org.joml.Vector3f(rotOrigin.get(0).floatValue(), rotOrigin.get(1).floatValue(), rotOrigin.get(2).floatValue());

				String rotationAxis = rot.get("axis").asText();
				if (rotationAxis.equals("x")) {
					rotAxis = new org.joml.Vector3f(1.0f, 0.0f, 0.0f);
				}
				else if (rotationAxis.equals("y")) {
					rotAxis = new org.joml.Vector3f(0.0f, 1.0f, 0.0f);
				}
				else {
					rotAxis = new org.joml.Vector3f(0.0f, 0.0f, 1.0f);
				}


				rotationAngle = rot.get("angle").floatValue();

				if(rot.has("rescale")) {
					rotationScale = rot.get("rescale").asBoolean();
				}
			}

			boolean shaded = true;
			if(element.has("shade")) {
				shaded = element.get("shade").asBoolean();
			}

			JsonNode facesNode = element.get("faces");
			SubTexture subTexture = new SubTexture(null, fromVector.x(), 16-toVector.y(), toVector.x(), 16-fromVector.y());
			BlockElement be = new BlockElement(fromVector, toVector, rotationOrigin, rotAxis, rotationAngle,
					rotationScale, shaded, combineMap, subTexture, facesNode, texturePack, blockModel);
			elementsList.add(be);
		}
		return elementsList;
	}
	
	@Getter
	public static class BlockElement
	{
		private final Vector3f from, to;
		private final org.joml.Vector3f rotationOrigin;
		private final org.joml.Vector3f rotationAxis;
		private final float rotationAngle;
		private final boolean scaled, shaded;
		private final Map<String, ElementFace> faces;
		
		public BlockElement(Vector3f from, Vector3f to, org.joml.Vector3f rotationOrigin, org.joml.Vector3f rotationAxis,
							float rotationAngle, boolean scaled, boolean shaded, Map<String, String> combineMap,
							SubTexture subTexture, JsonNode facesNode, TexturePack texturePack, BlockModel blockModel)
		{
			this.from = from;
			this.to = to;
			this.rotationOrigin = rotationOrigin;
			this.rotationAxis = rotationAxis;
			this.rotationAngle = rotationAngle;
			this.scaled = scaled;
			this.shaded = shaded;
			this.faces = deserializeElementFaces(combineMap, subTexture, facesNode, from, to, texturePack, blockModel);
		}

		private Map<String, ElementFace> deserializeElementFaces(Map<String, String> combineMap, SubTexture texCoords,
																 JsonNode faces, Vector3f fromVector, Vector3f toVector, TexturePack texturePack, BlockModel blockModel)
		{
			Map<String, ElementFace> elementFaces = new HashMap<>();

			Iterator<Map.Entry<String, JsonNode>> iter = faces.fields();
			while (iter.hasNext()) {
				Map.Entry<String, JsonNode> entry = iter.next();

				String key = entry.getKey();
				JsonNode face = entry.getValue();

				float u0 = texCoords.u0;
				float v0 = texCoords.v0;
				float u1 = texCoords.u1;
				float v1 = texCoords.v1;

				if (key.equals("up") || key.equals("down"))
				{
					v0 = fromVector.z();
					v1 = toVector.z();
				}
				else if (key.equals("north"))
				{
					u0 = 16 - texCoords.u1;
					u1 = 16 - texCoords.u0;
				}
				else if (key.equals("east"))
				{
					u0 = 16 - toVector.z();
					u1 = 16 - fromVector.z();
				}
				else if (key.equals("west"))
				{
					u0 = fromVector.z();
					u1 = toVector.z();
				}


				int rotation = 0;
				if(face.has(ROTATION_FIELD))
					rotation = face.get(ROTATION_FIELD).asInt();

				//System.out.println("u0="+u0+" v0="+v0+" u1="+u1+" v1="+v1);
				// TODO: Need to test more texture packs
				final float texel = 1.0f/16.0f;
				SubTexture subTexture = new SubTexture(null, u0*texel, v0*texel, u1*texel, v1*texel);

				StringBuilder tex = new StringBuilder(face.get("texture").asText());
				if(tex.charAt(0) == '#')
				{
					String texture = tex.deleteCharAt(0).toString();

					//TODO: Can we use texture request object to do this instead of duplicating code?
					String texturePath = combineMap.get(texture);
					if (texturePath.contains("minecraft:block/")) { // 1.16+
						texturePath = texturePath.replace("minecraft:block/", "") + ".png";
					} else if (texturePath.contains("block/")) { // 1.13 - 1.15
						texturePath = StringUtils.removeStart(combineMap.get(texture), "block/") + ".png";
					} else { // 1.8 - 1.12
						texturePath = StringUtils.removeStart(combineMap.get(texture), "blocks/") + ".png";
					}

					SubTexture te = texturePack.findTexture(texturePath);
					PackTexture pt = texturePack.getTexture(texturePack.getTexturePathPrefix("") + texturePath);

					//These are hard-coded in as I'm not sure how to programmatically tell the difference between these and other transparent blocks
					if (blockModel.getName().contains("stained_glass") || blockModel.getName().contains("_ice") || blockModel.getName().contains("nether_portal")) {
						blockModel.setSolid(false);
						blockModel.setTransparent(true);
					} else if (blockModel.isSolid() && ImageUtils.containsTransparency(pt.getImage())) {
						blockModel.setSolid(false);
						log.trace(key + ": " + texturePath + " contains transparency");
					}

					final float texHeight = te.texture.getHeight();
					final float texWidth = te.texture.getWidth();
					final int numTiles = te.texture.getHeight()/te.texture.getWidth();

					u0 /= texWidth;
					v0 = (v0 / texWidth) / numTiles;
					u1 /= texWidth;
					v1 = (v1 / texWidth) / numTiles;

					if(face.has("uv"))
					{
						//System.out.println("Before: u0="+u0+" v0="+v0+" u1="+u1+" v1="+v1);
						JsonNode uv = face.get("uv");
						u0 = uv.get(0).floatValue()/16.0f;
						v0 = (uv.get(1).floatValue()/16.0f) / numTiles;
						u1 = uv.get(2).floatValue()/16.0f;
						v1 = (uv.get(3).floatValue()/16.0f) / numTiles;
					}

					//System.out.println(texWidth + " x " + texHeight);
					int frame = 1;
					if(numTiles > 1)
					{
						Random rand = new Random();
						frame = rand.nextInt(numTiles)+1;
					}

					subTexture = new SubTexture(te.texture, u0, v0+(float)(frame-1)*(texWidth/texHeight), u1, v1+(float)(frame-1)*(texWidth/texHeight));
					//subTexture = new SubTexture(test, u0, v0, u1, v1);
					//System.out.println("u0="+subTexture.u0+" v0="+subTexture.v0+" u1="+subTexture.u1+" v1="+subTexture.v1);
				}

				boolean cullFace = false;
				if(face.has("cullface"))
					cullFace = true;

				boolean tintIndex = false;
				if(face.has("tintindex"))
					tintIndex = true;

				ElementFace ef = new ElementFace(subTexture, cullFace, rotation, tintIndex);
				elementFaces.put(key, ef);
			}

			return elementFaces;
		}

		@Getter
		public static class ElementFace
		{
			private final SubTexture texture;
			private final boolean faceCulled, tinted;  // May need to change the type of these variables in the future, for now they work fine as booleans
			private final int textureRotation;
			
			public ElementFace(SubTexture texture, boolean faceCulled, int textureRotation, boolean tinted)
			{
				this.texture = texture;
				this.faceCulled = faceCulled;
				this.textureRotation = textureRotation;
				this.tinted = tinted;
			}
		}
	}
}
