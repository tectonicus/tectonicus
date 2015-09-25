/*
 * Copyright (c) 2012-2015, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.util.List;
import java.util.Map;

import tectonicus.texture.SubTexture;
import tectonicus.util.Vector3f;

public class BlockModel
{
	final private String name;
	final private boolean ambientlyOccluded;
	final private List<BlockElement> elements;
	
	public BlockModel(String name, boolean ambientlyOccluded, List<BlockElement> elements)
	{
		this.name = name;
		this.ambientlyOccluded = ambientlyOccluded;
		this.elements = elements;
	}
	
	public String getName()	{ return name; }	
	public boolean isAmbientlyOccluded() { return ambientlyOccluded; }
	public List<BlockElement> getElements()	{ return elements; }
	
	
	public static class BlockElement
	{
		final private Vector3f from, to, rotationOrigin;
		final private String rotationAxis;
		final private float rotationAngle;
		final private boolean scaled, shaded;
		final private Map<String, ElementFace> faces;
		
		public BlockElement(Vector3f from, Vector3f to, Vector3f rotationOrigin, String rotationAxis, float rotationAngle, boolean scaled, boolean shaded, Map<String, ElementFace> faces)
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
		public Vector3f getRotationOrigin() { return rotationOrigin; }
		public String getRotationAxis() { return rotationAxis; }
		public float getRotationAngle() { return rotationAngle; }
		public boolean isScaled() { return scaled; }
		public boolean isShaded() { return shaded; }
		public Map<String, ElementFace> getFaces() { return faces; }
		
		
		public static class ElementFace
		{
			final private SubTexture texture;
			final private boolean faceCulled, tinted;  // May need to change the type of these variables in the future, for now they work fine as booleans
			final private int textureRotation;
			
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
