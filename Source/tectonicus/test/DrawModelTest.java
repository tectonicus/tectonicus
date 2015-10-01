/*
 * Copyright (c) 2012-2015, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.test;

import org.junit.Test;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;

import static org.lwjgl.util.glu.GLU.gluPerspective;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Vector3f;

import tectonicus.Minecraft;
import tectonicus.blockTypes.BlockModel;
import tectonicus.blockTypes.BlockModel.BlockElement;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.lwjgl.LwjglMesh;
import tectonicus.rasteriser.lwjgl.LwjglTexture;
import tectonicus.texture.SubTexture;
import tectonicus.texture.ZipStack;
import tectonicus.blockTypes.BlockRegistry;

public class DrawModelTest 
{
	private float rot = 2.0f;
	ZipStack zips; 
	
	public DrawModelTest()
	{
		try {
			zips = new ZipStack(Minecraft.findMinecraftJar(), null, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testDrawModel() throws Exception
	{	
		Map<Texture, Mesh> meshList = new HashMap<>();
		
		try
		{
			Display.setDisplayMode(new DisplayMode(640, 640));
			Display.setTitle("Test!");
			Display.setResizable(true);
			Display.create(new PixelFormat(8,24,0,8));
		} catch(LWJGLException e) {
			e.printStackTrace();
		}		
		
		BlockRegistry br = new BlockRegistry();
		Map<String, String> textureMap = new HashMap<>();
		BlockModel bm = br.loadModel("block/comparator_lit_subtract", zips, textureMap);
		List<BlockElement> elements = bm.getElements();
		
		for(BlockElement element : elements)
		{
			SubTexture tex = null;
	        LwjglTexture texture = null;
			if (element.getFaces().containsKey("up"))
	        {
	        	//int rotation = element.getFaces().get("up").getTextureRotation();
	        	
				tex = element.getFaces().get("up").getTexture();
				//System.out.println("u0="+tex.u0+" v0="+tex.v0+" u1="+tex.u1+" v1="+tex.v1);
				texture = (LwjglTexture) tex.texture;
				
				Mesh result = null;
				result = meshList.get(texture);
				if (result == null)
				{
					result = new LwjglMesh(texture);
					meshList.put(texture, result);
				}
				
		        float x1 = element.getFrom().x();
		        //float y1 = element.getFrom().y();
		        float z1 = element.getFrom().z();
		        
		        float x2 = element.getTo().x();
		        float y2 = element.getTo().y();
		        float z2 = element.getTo().z();

				result.addVertex(new Vector3f(x1, y2, z1), tex.u0, tex.v0);
				result.addVertex(new Vector3f(x2, y2, z1), tex.u1, tex.v0);
				result.addVertex(new Vector3f(x2, y2, z2), tex.u1, tex.v1);
				result.addVertex(new Vector3f(x1, y2, z2), tex.u0, tex.v1);
	        }
		}
		
		for (Mesh m : meshList.values())
			m.finalise();
		
		resize();
		
		System.out.println(glGetString(GL_VERSION));
		System.out.println(glGetString(GL_VENDOR));
		
		glColor3f(0.0f, 1.0f, 0.0f);

		//glShadeModel(GL_SMOOTH);
		glFrontFace(GL_CW);
		//glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_ALPHA_TEST);
		glAlphaFunc(GL11.GL_GREATER, 0.6f);
		//glEnable(GL_MULTISAMPLE);
		//glPolygonMode(GL_FRONT, GL_LINE);
		
		while(!Display.isCloseRequested())
		{
			if (Display.wasResized())
                resize();
			
			getKeys();
			
			glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

	        for(BlockElement element : elements)
	        {
	        	float originX = element.getRotationOrigin().x();
	        	float originY = element.getRotationOrigin().y();
	        	float originZ = element.getRotationOrigin().z();
	        	
	        	glPushMatrix();
				if(element.getRotationAxis().equals("x"))
				{
					glTranslatef(originX, originY, originZ);
					glRotatef(element.getRotationAngle(), 1.0f, 0, 0);
					glTranslatef(-originX, -originY, -originZ);
				}
				else if(element.getRotationAxis().equals("y"))
				{
					glTranslatef(originX, originY, originZ);
					glRotatef(element.getRotationAngle(), 0, 1.0f, 0);
					glTranslatef(-originX, -originY, -originZ);
				}
				else if(element.getRotationAxis().equals("z"))
				{
					glTranslatef(originX, originY, originZ);
					glRotatef(element.getRotationAngle(), 0, 0, 1.0f);
					glTranslatef(-originX, -originY, -originZ);
				}
				
		        float x1 = element.getFrom().x();
		        float y1 = element.getFrom().y();
		        float z1 = element.getFrom().z();
		        
		        float x2 = element.getTo().x();
		        float y2 = element.getTo().y();
		        float z2 = element.getTo().z();
		        
		        //Top face
		        SubTexture tex = null;
		        LwjglTexture texture = null;
		        if (element.getFaces().containsKey("up"))
		        {
		        	//int rotation = element.getFaces().get("up").getTextureRotation();
		        	
					tex = element.getFaces().get("up").getTexture();
					//System.out.println("u0="+tex.u0+" v0="+tex.v0+" u1="+tex.u1+" v1="+tex.v1);
					texture = (LwjglTexture) tex.texture;
					//System.out.println(texture.getId());
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
					glColor3f(1.0f, 1.0f, 1.0f);
					glBegin(GL_QUADS);
					glTexCoord2f(tex.u0, tex.v0);
					glVertex3f(x1, y2, z1);
					glTexCoord2f(tex.u1, tex.v0);
					glVertex3f(x2, y2, z1);
					glTexCoord2f(tex.u1, tex.v1);
					glVertex3f(x2, y2, z2);
					glTexCoord2f(tex.u0, tex.v1);
					glVertex3f(x1, y2, z2);
					glEnd();
				}
		        
//		        for (Mesh m : meshList.values())
//				{
//					m.bind();
//					m.draw(0, 0, 0);
//				}
		        
				if (element.getFaces().containsKey("down")) {
					//Bottom face
					tex = element.getFaces().get("down").getTexture();
					texture = (LwjglTexture) element.getFaces().get("down").getTexture().texture;
					//System.out.println(texture.getId());
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
					glColor3f(1.0f, 1.0f, 1.0f);
					glBegin(GL_QUADS);
					glTexCoord2f(tex.u0, tex.v0);
					glVertex3f(x2, y1, z1);
					glTexCoord2f(tex.u1, tex.v0);
					glVertex3f(x1, y1, z1);
					glTexCoord2f(tex.u1, tex.v1);
					glVertex3f(x1, y1, z2);
					glTexCoord2f(tex.u0, tex.v1);
					glVertex3f(x2, y1, z2);
					glEnd();
				}
				if (element.getFaces().containsKey("north")) {
					//North face
					tex = element.getFaces().get("north").getTexture();
					texture = (LwjglTexture) element.getFaces().get("north").getTexture().texture;
					//System.out.println(texture.getId());
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
					glColor3f(1.0f, 1.0f, 1.0f);
					glBegin(GL_QUADS);
					glTexCoord2f(tex.u0, tex.v0);
					glVertex3f(x2, y2, z1);
					glTexCoord2f(tex.u1, tex.v0);
					glVertex3f(x1, y2, z1);
					glTexCoord2f(tex.u1, tex.v1);
					glVertex3f(x1, y1, z1);
					glTexCoord2f(tex.u0, tex.v1);
					glVertex3f(x2, y1, z1);
					glEnd();
				}
				if (element.getFaces().containsKey("south")) {
					//South face
					tex = element.getFaces().get("south").getTexture();
					texture = (LwjglTexture) element.getFaces().get("south").getTexture().texture;
					//System.out.println(texture.getId());
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
					glColor3f(1.0f, 1.0f, 1.0f);
					glBegin(GL_QUADS);
					glTexCoord2f(tex.u0, tex.v0);
					glVertex3f(x1, y2, z2);
					glTexCoord2f(tex.u1, tex.v0);
					glVertex3f(x2, y2, z2);
					glTexCoord2f(tex.u1, tex.v1);
					glVertex3f(x2, y1, z2);
					glTexCoord2f(tex.u0, tex.v1);
					glVertex3f(x1, y1, z2);
					glEnd();
				}
				if (element.getFaces().containsKey("east")) {
					//East face
					tex = element.getFaces().get("east").getTexture();
					texture = (LwjglTexture) element.getFaces().get("east").getTexture().texture;
					//System.out.println(texture.getId());
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
					glColor3f(1.0f, 1.0f, 1.0f);
					glBegin(GL_QUADS);
					glTexCoord2f(tex.u0, tex.v0);
					glVertex3f(x2, y2, z2);
					glTexCoord2f(tex.u1, tex.v0);
					glVertex3f(x2, y2, z1);
					glTexCoord2f(tex.u1, tex.v1);
					glVertex3f(x2, y1, z1);
					glTexCoord2f(tex.u0, tex.v1);
					glVertex3f(x2, y1, z2);
					glEnd();
				}
				if (element.getFaces().containsKey("west")) {
					//West face
					tex = element.getFaces().get("west").getTexture();
					texture = (LwjglTexture) element.getFaces().get("west").getTexture().texture;
					//System.out.println(texture.getId());
					GL11.glEnable(GL11.GL_TEXTURE_2D);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
					glColor3f(1.0f, 1.0f, 1.0f);
					glBegin(GL_QUADS);
					glTexCoord2f(tex.u0, tex.v0);
					glVertex3f(x1, y2, z1);
					glTexCoord2f(tex.u1, tex.v0);
					glVertex3f(x1, y2, z2);
					glTexCoord2f(tex.u1, tex.v1);
					glVertex3f(x1, y1, z2);
					glTexCoord2f(tex.u0, tex.v1);
					glVertex3f(x1, y1, z1);
					glEnd();
				}
				glPopMatrix();
	        }
			// Restore transformations
			//glPopMatrix();


			Display.update();
			Display.sync(60);
		}
		Display.destroy();
	}
	
	private void getKeys() {
		if(Keyboard.isKeyDown(Keyboard.KEY_UP))
		{
			glRotatef(rot, 1.0f, 0.0f, 0.0f);
		}

		if(Keyboard.isKeyDown(Keyboard.KEY_DOWN))
		{
			glRotatef(-rot, 1.0f, 0.0f, 0.0f);
		}

		if(Keyboard.isKeyDown(Keyboard.KEY_LEFT))
		{
			glRotatef(rot, 0.0f, 1.0f, 0.0f);
		}

		if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT))
		{
			glRotatef(-rot, 0.0f, 1.0f, 0.0f);
		}
	}
	
	private void resize()
	{
		//final int range = 100;
		
		glViewport(0, 0, Display.getWidth(), Display.getHeight());
		
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		float aspect = Display.getWidth() / Display.getHeight();
	    gluPerspective(25.0f, aspect, 1.0f, 300.0f);
		//glOrtho(-range, range, -range*Display.getHeight()/Display.getWidth(), range*Display.getHeight()/Display.getWidth(), -range, range);
		glMatrixMode(GL_MODELVIEW);
		glTranslatef(0,0, -50);
	}	
}
