/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import tectonicus.texture.SubTexture;
import tectonicus.util.Vector3f;

public class BlockModel
{
	private final String name;
	private final boolean ambientlyOccluded;
	private final List<BlockElement> elements;
	
	public BlockModel(String name, boolean ambientlyOccluded, List<BlockElement> elements)
	{
		this.name = name;
		this.ambientlyOccluded = ambientlyOccluded;
		this.elements = elements;
	}
	
	public String getName()	{ return name; }	
	public boolean isAmbientlyOccluded() { return ambientlyOccluded; }
	public List<BlockElement> getElements()	{ return elements; }
	
	public void drawModel(int x, int y, int z, int rotX, int rotY, boolean uvlock)
	{
		
	}
	
	
	public static class BlockElement
	{
		private final Vector3f from, to;
		private final org.joml.Vector3f rotationOrigin;
		private final org.joml.Vector3f rotationAxis;
		private final float rotationAngle;
		private final boolean scaled, shaded;
		private final Map<String, ElementFace> faces;
		
		public BlockElement(Vector3f from, Vector3f to, org.joml.Vector3f rotationOrigin, org.joml.Vector3f rotationAxis, float rotationAngle, boolean scaled, boolean shaded, Map<String, ElementFace> faces)
		{
			this.from = from;
			this.to = to;
			this.rotationOrigin = rotationOrigin;
			this.rotationAxis = rotationAxis;
			this.rotationAngle = rotationAngle;
			this.scaled = scaled;
			this.shaded = shaded;
			this.faces = faces;
		}
		
		public Vector3f getFrom() {	return from; }
		public Vector3f getTo() { return to; }
		public org.joml.Vector3f getRotationOrigin() { return rotationOrigin; }
		public org.joml.Vector3f getRotationAxis() { return rotationAxis; }
		public float getRotationAngle() { return rotationAngle; }
		public boolean isScaled() { return scaled; }
		public boolean isShaded() { return shaded; }
		public Map<String, ElementFace> getFaces() { return Collections.unmodifiableMap(faces); }
		
		
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
			
			public SubTexture getTexture() { return texture; }
			public boolean isFaceCulled() {	return faceCulled; }			
			public boolean isTinted() { return tinted; }
			public int getTextureRotation() { return textureRotation; }
		}
	}
}
