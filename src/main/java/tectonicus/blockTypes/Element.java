/*
 * Copyright (c) 2020 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.blockTypes;

import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import tectonicus.texture.SubTexture;
import tectonicus.texture.TexturePack;
import tectonicus.util.Vector3f;

public class Element
{
	private float[] from, to;
	private Rotation rotation;
	private boolean shade = true;
	private Faces faces;
	private transient Map<String, String> combineMap;
	
	public Vector3f getFrom() { return Vector3f.fromArray(from); }
	public Vector3f getTo() { return Vector3f.fromArray(to); }
	public Rotation getRotation() { return rotation; }
	public boolean isShaded() { return shade; }
	public Faces getFaces() { return faces; }
	//public void setCombineMap {  }
	
	public static class Rotation
	{
		private float[] origin;
		private String axis;
		private float angle;
		private boolean rescale;
		
		public Vector3f getOrigin() { return Vector3f.fromArray(origin); }
		public String getAxis() { return axis; }
		public float getAngle() { return angle; }
		public boolean isRescaled() { return rescale; }
	}
	
	public static class Faces
	{
		private Face up, down, north, south, east, west;
		
		public Face getUp() { return up; };
		public Face getDown() { return down; }
		public Face getNorth() { return north; }
		public Face getSouth() { return south; }
		public Face getEast() { return east; }
		public Face getWest() { return west; }
		
		public static class Face
		{
			private float[] uv;
			private String texture, cullface;
			private int rotation, tintindex;
			
			public float[] getUV() { return uv; }
			public String getTexture() { return texture.substring(1); }
			public String isFaceCulled() {	return cullface; }
			public int isTinted() { return tintindex; }
			public int getRotation() { return rotation; }
			
//			public SubTexture getSubTexture(TexturePack texturePack, SubTexture texCoords)
//			{
//				SubTexture te = texturePack.findTexture(StringUtils.removeStart(combineMap.get(texture), "blocks/")+ ".png");
//		    	
//		    	final float texHeight = te.texture.getHeight();
//				final float texWidth = te.texture.getWidth();
//		    	final int numTiles = te.texture.getHeight()/te.texture.getWidth();
//		    	
//		    	float u0 = texCoords.u0 / texWidth;
//				float v0 = texCoords.v0 / texWidth;
//				float u1 = texCoords.u1 / texWidth;
//				float v1 = texCoords.v1 / texWidth;
//		    	
//		    	if(uv != null)
//				{
//		    		//System.out.println("Before: u0="+u0+" v0="+v0+" u1="+u1+" v1="+v1);
//					//JsonArray uv = face.getAsJsonArray("uv");
//					u0 = uv[0]/16.0f;
//					v0 = (uv[1]/16.0f) / numTiles;
//					u1 = uv[2]/16.0f;
//					v1 = (uv[3]/16.0f) / numTiles;
//				}
//		    	
//		    	System.out.println(texWidth + " x " + texHeight);
//		    	int frame = 1;
//		    	if(numTiles > 1)
//				{
//					Random rand = new Random();
//					frame = rand.nextInt(numTiles)+1;
//				}
//
//		    	return new SubTexture(te.texture, u0, v0+(float)(frame-1)*(texWidth/texHeight), u1, v1+(float)(frame-1)*(texWidth/texHeight));
//				
//				//return null;
//			}
		}
	}
}
