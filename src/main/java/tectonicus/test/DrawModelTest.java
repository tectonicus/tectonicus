/*
 * Copyright (c) 2020, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.test;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import tectonicus.blockTypes.BlockModel;
import tectonicus.blockTypes.BlockModel.BlockElement;
import tectonicus.blockTypes.BlockModel.BlockElement.ElementFace;
import tectonicus.blockTypes.BlockRegistry;
import tectonicus.configuration.Configuration.RasteriserType;
import tectonicus.rasteriser.Mesh;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.rasteriser.RasteriserFactory;
import tectonicus.rasteriser.RasteriserFactory.DisplayType;
import tectonicus.rasteriser.Texture;
import tectonicus.rasteriser.lwjgl.LwjglMesh;
import tectonicus.rasteriser.lwjgl.LwjglTexture;
import tectonicus.texture.SubTexture;
import tectonicus.util.Colour4f;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_ALPHA_TEST;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CW;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glAlphaFunc;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glFrontFace;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL11.glLoadMatrixf;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex3f;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;

public class DrawModelTest 
{
	private float rot = 2.0f;
	
	private long windowId;
	
	private long prevMillis;
	
	public static void main(String[] args)
	{
		DrawModelTest test = new DrawModelTest();
		try {
			test.testDrawModel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void testDrawModel() throws Exception
	{	
		Map<Texture, Mesh> meshList = new HashMap<>();
		
		int width = 800;
		int height = 800;
		
//		try
//		{
//			Display.setDisplayMode(new DisplayMode(640, 640));
//			Display.setTitle("Test!");
//			Display.setResizable(true);
//			Display.create(new PixelFormat(8,24,0,8));
//		} catch(LWJGLException e) {
//			e.printStackTrace();
//		}

		Rasteriser rasteriser = RasteriserFactory.createRasteriser(RasteriserType.LWJGL, DisplayType.Window, width, height, 24, 8, 24, 4);
		windowId = rasteriser.getWindowId();
//		int fbo = glGenFramebuffers();
//		glBindFramebuffer(GL_FRAMEBUFFER, fbo);
//		int rbo = glGenRenderbuffers();
//		glBindRenderbuffer(GL_RENDERBUFFER, rbo);
//		glRenderbufferStorage(GL_RENDERBUFFER, GL_RGB8, 1024, 1024);

		BlockRegistry br = new BlockRegistry(rasteriser);
		BlockModel bm = br.loadModel("block/beacon", new HashMap<String, String>(), null);
		List<BlockElement> elements = bm.getElements();
		
		Vector3f rotOrigin = new Vector3f(0,0,0);
		Matrix4f rotTransform = new Matrix4f().translate(rotOrigin)
				  .rotate((float) Math.toRadians(90), 0, 1, 0) 
				  .translate(rotOrigin.negate());
		
		
		for(BlockElement element : elements)
		{
//			Vector3f rotationOrigin = element.getRotationOrigin().div(16);
			Vector3f rotationOrigin = element.getRotationOrigin();
			Vector3f rotationAxis = element.getRotationAxis();
			
			Matrix4f rotationTransform = null;
			if (element.getRotationAngle() != 0)
			{
				rotationTransform = new Matrix4f().translate(rotationOrigin)
									              .rotate((float) Math.toRadians(element.getRotationAngle()), rotationAxis.x, rotationAxis.y, rotationAxis.z)
									              .translate(rotationOrigin.negate());
			}
			
//			float x1 = element.getFrom().x()/16;
//	        float y1 = element.getFrom().y()/16;
//	        float z1 = element.getFrom().z()/16;
//	        
//	        float x2 = element.getTo().x()/16;
//	        float y2 = element.getTo().y()/16;
//	        float z2 = element.getTo().z()/16;
	        
	        float x1 = element.getFrom().x();
	        float y1 = element.getFrom().y();
	        float z1 = element.getFrom().z();
	        
	        float x2 = element.getTo().x();
	        float y2 = element.getTo().y();
	        float z2 = element.getTo().z();

			if (element.getFaces().containsKey("up"))
	        {
				ElementFace face = element.getFaces().get("up");

				//System.out.println("u0="+tex.u0+" v0="+tex.v0+" u1="+tex.u1+" v1="+tex.v1);
				
				Vector3f topLeft = new Vector3f(x1, y2, z1);
		        Vector3f topRight = new Vector3f(x2, y2, z1);
		        Vector3f bottomRight = new Vector3f(x2, y2, z2);
		        Vector3f bottomLeft = new Vector3f(x1, y2, z2);

		        addVertices(meshList, face, topLeft, topRight, bottomRight, bottomLeft, rotationTransform, rotTransform);
	        }
			
			if (element.getFaces().containsKey("down"))
	        {
				ElementFace face = element.getFaces().get("down");
				
				Vector3f topLeft = new Vector3f(x1, y1, z2);
		        Vector3f topRight = new Vector3f(x2, y1, z2);
		        Vector3f bottomRight = new Vector3f(x2, y1, z1);
		        Vector3f bottomLeft = new Vector3f(x1, y1, z1);

		        addVertices(meshList, face, topLeft, topRight, bottomRight, bottomLeft, rotationTransform, rotTransform);
	        }
			
			if (element.getFaces().containsKey("north"))
	        {
				ElementFace face = element.getFaces().get("north");
				
				Vector3f topLeft = new Vector3f(x2, y2, z1);
		        Vector3f topRight = new Vector3f(x1, y2, z1);
		        Vector3f bottomRight = new Vector3f(x1, y1, z1);
		        Vector3f bottomLeft = new Vector3f(x2, y1, z1);
		        
		        addVertices(meshList, face, topLeft, topRight, bottomRight, bottomLeft, rotationTransform, rotTransform);
	        }
			
			if (element.getFaces().containsKey("south"))
	        {
				ElementFace face = element.getFaces().get("south");
				
				Vector3f topLeft = new Vector3f(x1, y2, z2);
		        Vector3f topRight = new Vector3f(x2, y2, z2);
		        Vector3f bottomRight = new Vector3f(x2, y1, z2);
		        Vector3f bottomLeft = new Vector3f(x1, y1, z2);
		        
		        addVertices(meshList, face, topLeft, topRight, bottomRight, bottomLeft, rotationTransform, rotTransform);
	        }
			
			if (element.getFaces().containsKey("east"))
	        {
				ElementFace face = element.getFaces().get("east");
				
				Vector3f topLeft = new Vector3f(x2, y2, z2);
		        Vector3f topRight = new Vector3f(x2, y2, z1);
		        Vector3f bottomRight = new Vector3f(x2, y1, z1);
		        Vector3f bottomLeft = new Vector3f(x2, y1, z2);
		        
		        addVertices(meshList, face, topLeft, topRight, bottomRight, bottomLeft, rotationTransform, rotTransform);
	        }
			
			if (element.getFaces().containsKey("west"))
	        {
				ElementFace face = element.getFaces().get("west");
				
				Vector3f topLeft = new Vector3f(x1, y2, z1);
		        Vector3f topRight = new Vector3f(x1, y2, z2);
		        Vector3f bottomRight = new Vector3f(x1, y1, z2);
		        Vector3f bottomLeft = new Vector3f(x1, y1, z1);
		        
		        addVertices(meshList, face, topLeft, topRight, bottomRight, bottomLeft, rotationTransform, rotTransform);
	        }
		}
		
		for (Mesh m : meshList.values())
			m.finalise();
		
		resize(width, height);
		
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

		GLFW.glfwSetFramebufferSizeCallback(windowId, (window, width1, height1) -> resize(width1, height1));


		//glBindFramebuffer(GL_FRAMEBUFFER, 0);
		//glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
		int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		if (status != GL_FRAMEBUFFER_COMPLETE) {
			System.out.println("Incomplete framebuffer");
		}

		while(!GLFW.glfwWindowShouldClose(windowId))
		{
			getKeys();
			
			glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	        
			for (Mesh m : meshList.values())
			{
				m.bind();
				m.draw(0, 0, 0);
			}
			
			//oldDraw(elements);
			
			// Restore transformations
			//glPopMatrix();

			GLFW.glfwPollEvents();
			
//			long currMillis = System.currentTimeMillis();
//			long fps = 1000 / (currMillis - prevMillis);
//			prevMillis = currMillis;
//			if (fps > 60) {
//				return;
//			}
			
			GLFW.glfwSwapBuffers(windowId);
		}
//		BufferedImage image = rasteriser.takeScreenshot(0, 0, 800, 800, ImageFormat.Png);
//		Screenshot.write(new File("c:/users/Ender/Desktop/testImage.png"), image, ImageFormat.Png, 1.0f);
		GLFW.glfwDestroyWindow(windowId);
	}

	private void oldDraw(List<BlockElement> elements) {
		for(BlockElement element : elements)
		{
			float originX = element.getRotationOrigin().x();
			float originY = element.getRotationOrigin().y();
			float originZ = element.getRotationOrigin().z();
			Vector3f rotationAxis = element.getRotationAxis();
			
			glPushMatrix();
			glTranslatef(originX, originY, originZ);
			glRotatef(element.getRotationAngle(), rotationAxis.x, rotationAxis.y, rotationAxis.z);
			glTranslatef(-originX, -originY, -originZ);
			
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
				GL11.glEnable(GL_TEXTURE_2D);
				GL11.glBindTexture(GL_TEXTURE_2D, texture.getId());
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
		    
		    
		    
			if (element.getFaces().containsKey("down")) {
				//Bottom face
				tex = element.getFaces().get("down").getTexture();
				texture = (LwjglTexture) element.getFaces().get("down").getTexture().texture;
				//System.out.println(texture.getId());
				GL11.glEnable(GL_TEXTURE_2D);
				GL11.glBindTexture(GL_TEXTURE_2D, texture.getId());
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
				ElementFace northFace = element.getFaces().get("north");
				tex = northFace.getTexture();
				int rotation = northFace.getTextureRotation();
				
				tex = element.getFaces().get("north").getTexture();
				texture = (LwjglTexture) element.getFaces().get("north").getTexture().texture;
				//System.out.println(texture.getId());
				GL11.glEnable(GL_TEXTURE_2D);
				GL11.glBindTexture(GL_TEXTURE_2D, texture.getId());
				glColor3f(1.0f, 1.0f, 1.0f);
				
				if(rotation == 0)
				{
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
				else if(rotation == 90)
				{
					glBegin(GL_QUADS);
					glTexCoord2f(tex.u0, tex.v1);
					glVertex3f(x2, y2, z1);
					glTexCoord2f(tex.u0, tex.v0);
					glVertex3f(x1, y2, z1);
					glTexCoord2f(tex.u1, tex.v0);
					glVertex3f(x1, y1, z1);
					glTexCoord2f(tex.u1, tex.v1);
					glVertex3f(x2, y1, z1);
					glEnd();
				}
				else if (rotation == 180)
				{
					glBegin(GL_QUADS);
					glTexCoord2f(tex.u1, tex.v1);
					glVertex3f(x2, y2, z1);
					glTexCoord2f(tex.u0, tex.v1);
					glVertex3f(x1, y2, z1);
					glTexCoord2f(tex.u0, tex.v0);
					glVertex3f(x1, y1, z1);
					glTexCoord2f(tex.u1, tex.v0);
					glVertex3f(x2, y1, z1);
					glEnd();
				}
				else if (rotation == 270)
				{
					glBegin(GL_QUADS);
					glTexCoord2f(tex.u1, tex.v0);
					glVertex3f(x2, y2, z1);
					glTexCoord2f(tex.u1, tex.v1);
					glVertex3f(x1, y2, z1);
					glTexCoord2f(tex.u0, tex.v1);
					glVertex3f(x1, y1, z1);
					glTexCoord2f(tex.u0, tex.v0);
					glVertex3f(x2, y1, z1);
					glEnd();
				}
			}
			if (element.getFaces().containsKey("south")) {
				//South face
				ElementFace southFace = element.getFaces().get("south");
				tex = southFace.getTexture();
				int rotation = southFace.getTextureRotation();
				
				texture = (LwjglTexture) southFace.getTexture().texture;
				//System.out.println(texture.getId());
				GL11.glEnable(GL_TEXTURE_2D);
				GL11.glBindTexture(GL_TEXTURE_2D, texture.getId());
				glColor3f(1.0f, 1.0f, 1.0f);
				
				if(rotation == 0)
				{
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
				else if(rotation == 90)
				{
					glBegin(GL_QUADS);
					glTexCoord2f(tex.u0, tex.v1);
					glVertex3f(x1, y2, z2);
					glTexCoord2f(tex.u0, tex.v0);
					glVertex3f(x2, y2, z2);
					glTexCoord2f(tex.u1, tex.v0);
					glVertex3f(x2, y1, z2);
					glTexCoord2f(tex.u1, tex.v1);
					glVertex3f(x1, y1, z2);
					glEnd();
				}
				else if (rotation == 180)
				{
					glBegin(GL_QUADS);
					glTexCoord2f(tex.u1, tex.v1);
					glVertex3f(x1, y2, z2);
					glTexCoord2f(tex.u0, tex.v1);
					glVertex3f(x2, y2, z2);
					glTexCoord2f(tex.u0, tex.v0);
					glVertex3f(x2, y1, z2);
					glTexCoord2f(tex.u1, tex.v0);
					glVertex3f(x1, y1, z2);
					glEnd();
				}
				else if (rotation == 270)
				{
					glBegin(GL_QUADS);
					glTexCoord2f(tex.u1, tex.v0);
					glVertex3f(x1, y2, z2);
					glTexCoord2f(tex.u1, tex.v1);
					glVertex3f(x2, y2, z2);
					glTexCoord2f(tex.u0, tex.v1);
					glVertex3f(x2, y1, z2);
					glTexCoord2f(tex.u0, tex.v0);
					glVertex3f(x1, y1, z2);
					glEnd();
				}
			}
			if (element.getFaces().containsKey("east")) {
				//East face
				tex = element.getFaces().get("east").getTexture();
				texture = (LwjglTexture) element.getFaces().get("east").getTexture().texture;
				//System.out.println(texture.getId());
				GL11.glEnable(GL_TEXTURE_2D);
				GL11.glBindTexture(GL_TEXTURE_2D, texture.getId());
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
				GL11.glEnable(GL_TEXTURE_2D);
				GL11.glBindTexture(GL_TEXTURE_2D, texture.getId());
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
	}

	private void addVertices(Map<Texture, Mesh> meshList, ElementFace face, Vector3f topLeft, Vector3f topRight, Vector3f bottomRight, Vector3f bottomLeft, Matrix4f rotationTransform, Matrix4f rotTransform)
	{
		rotTransform.transformPosition(topLeft);
        rotTransform.transformPosition(topRight);
        rotTransform.transformPosition(bottomRight);
        rotTransform.transformPosition(bottomLeft);
		
		if(rotationTransform != null)
        {
	        rotationTransform.transformPosition(topLeft);
	        rotationTransform.transformPosition(topRight);
	        rotationTransform.transformPosition(bottomRight);
	        rotationTransform.transformPosition(bottomLeft);
        }
		
		SubTexture tex = face.getTexture();
		int texRotation = face.getTextureRotation();
		LwjglTexture texture = (LwjglTexture) tex.texture;
		
		Mesh result = null;
		result = meshList.get(texture);
		if (result == null)
		{
			result = new LwjglMesh(texture);
			meshList.put(texture, result);
		}
		
		Colour4f color = new Colour4f(1,1,1,1);

		if(texRotation == 0)
		{
			result.addVertex(topLeft, color, tex.u0, tex.v0);
			result.addVertex(topRight, color, tex.u1, tex.v0);
			result.addVertex(bottomRight, color, tex.u1, tex.v1);
			result.addVertex(bottomLeft, color, tex.u0, tex.v1);
		}
		else if (texRotation == 90)
		{
			result.addVertex(topLeft, color, tex.u0, tex.v1);
			result.addVertex(topRight, color, tex.u0, tex.v0);
			result.addVertex(bottomRight, color, tex.u1, tex.v0);
			result.addVertex(bottomLeft, color, tex.u1, tex.v1);
		}
		else if (texRotation == 180)
		{
			result.addVertex(topLeft, color, tex.u1, tex.v1);
			result.addVertex(topRight, color, tex.u0, tex.v1);
			result.addVertex(bottomRight, color, tex.u0, tex.v0);
			result.addVertex(bottomLeft, color, tex.u1, tex.v0);
		}
		else if (texRotation == 270)
		{
			result.addVertex(topLeft, color, tex.u1, tex.v0);
			result.addVertex(topRight, color, tex.u1, tex.v1);
			result.addVertex(bottomRight, color, tex.u0, tex.v1);
			result.addVertex(bottomLeft, color, tex.u0, tex.v0);
		}
	}
	
	private void getKeys() {
		if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS)
		{
			glRotatef(rot, 1.0f, 0.0f, 0.0f);
		}

		if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS)
		{
			glRotatef(-rot, 1.0f, 0.0f, 0.0f);
		}

		if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS)
		{
			glRotatef(rot, 0.0f, 1.0f, 0.0f);
		}

		if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS)
		{
			glRotatef(-rot, 0.0f, 1.0f, 0.0f);
		}
	}
	
	private void resize(int width, int height)
	{
		//final int range = 100;
		
		//glViewport(0, 0, width, height);
		
		//glMatrixMode(GL_PROJECTION);
		//glLoadIdentity();
		float aspect = width / height;
		Matrix4f m = new Matrix4f();
		m.perspective((float) Math.toRadians(45), aspect, 1f, 300.0f);
		FloatBuffer fb = BufferUtils.createFloatBuffer(16);
		glMatrixMode(GL_PROJECTION);
		glLoadMatrixf(m.get(fb));
		//glOrtho(-range, range, -range*Display.getHeight()/Display.getWidth(), range*Display.getHeight()/Display.getWidth(), -range, range);
		glMatrixMode(GL_MODELVIEW);
		glTranslatef(0,0, -50);
	}	
}
