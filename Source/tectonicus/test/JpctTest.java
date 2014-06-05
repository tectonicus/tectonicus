/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.test;

import java.awt.Color;
import java.awt.Image;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import tectonicus.util.MatrixUtil;

import com.threed.jpct.FrameBuffer;
import com.threed.jpct.IRenderer;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

public class JpctTest
{
	private com.threed.jpct.World world;
	private FrameBuffer frameBuffer;
	private Object3D box;
	private JFrame jframe;

	public static void main(String[] args) throws Exception
	{
		new JpctTest().loop();
	}

	public JpctTest() throws Exception
	{
		jframe = new JFrame("Hello world");
		jframe.setSize(800, 600);
		jframe.setVisible(true);
		jframe.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		
		world = new com.threed.jpct.World();
		world.setAmbientLight(0, 255, 0);

		
		Image img = ImageIO.read( getClass().getClassLoader().getResourceAsStream("Images/FullHeart.png") );
		assert (img != null);
		TextureManager.getInstance().addTexture("box", new Texture(img) );
		int id = TextureManager.getInstance().getTextureID("box");
		
		// Make a quad from (0,0) to (100, 100)
		Object3D obj = new Object3D(128);
		obj.addTriangle(new SimpleVector(0, 0, 0), 0, 0,
						new SimpleVector(100, 0, 0), 1, 0,
						new SimpleVector(0, 0, 100), 0, 1,
						id);
		obj.addTriangle(new SimpleVector(0, 0, 100), 0, 1,
						new SimpleVector(100, 0, 0), 1, 0,
						new SimpleVector(100, 0, 100), 1, 1,
						id);
		obj.setBaseTexture("box");
		obj.setCulling(false);
		obj.build();
		world.addObject(obj);
		
		world.setAmbientLight(255, 255, 255);
	
		box = Primitives.getBox(2f, 2f);
		box.setAdditionalColor(Color.red);
		box.setLighting(Object3D.LIGHTING_NO_LIGHTS);
		box.build();
		world.addObject(box);

		
		Vector3f eye = new Vector3f(50, -120, 100);
		Vector3f target = new Vector3f(50, 0, 50);
		
		// TODO: Make this work somehow
		Matrix4f camMatrix = MatrixUtil.createLookAt(eye, target, new Vector3f(0, 1, 0));
		Matrix ownLookAt = toJptcMatrix(camMatrix);
		world.getCamera().setBack(ownLookAt);
		
		world.getCamera().setPosition(eye.x, eye.y, eye.z);
		world.getCamera().lookAt(new SimpleVector(target.x, target.y, target.z));
		Matrix jptcLookAt = world.getCamera().getBack();
		System.out.println(jptcLookAt);
	}

	private static Matrix toJptcMatrix(Matrix4f src)
	{
		Matrix dest = new Matrix();
		
		/*
		dest.getXAxis().x = src.m00;
		dest.getXAxis().y = src.m01;
		dest.getXAxis().z = src.m02;
		
		dest.getYAxis().x = src.m10;
		dest.getYAxis().y = src.m11;
		dest.getYAxis().z = src.m12;
		
		dest.getZAxis().x = src.m20;
		dest.getZAxis().y = src.m21;
		dest.getZAxis().z = src.m22;
		
		dest.getTranslation().x = src.m03;
		dest.getTranslation().y = src.m13;
		dest.getTranslation().z = src.m23;
		*/
		
		dest.setRow(0, src.m00, src.m01, src.m02, src.m03);
		dest.setRow(1, src.m10, src.m11, src.m12, src.m13);
		dest.setRow(2, src.m20, src.m21, src.m22, src.m23);
		dest.setRow(3, src.m30, src.m31, src.m32, src.m33);
	/*	
		dest.getXAxis().x = src.m00;
		dest.getXAxis().y = src.m10;
		dest.getXAxis().z = src.m20;
		
		dest.getYAxis().x = src.m01;
		dest.getYAxis().y = src.m11;
		dest.getYAxis().z = src.m21;
		
		dest.getZAxis().x = src.m02;
		dest.getZAxis().y = src.m12;
		dest.getZAxis().z = src.m22;
		
		dest.getTranslation().x = src.m30;
		dest.getTranslation().y = src.m31;
		dest.getTranslation().z = src.m32;
	*/
		return dest;
	}
	
	private void loop() throws Exception
	{
		frameBuffer = new FrameBuffer(800, 600, FrameBuffer.SAMPLINGMODE_NORMAL);
		frameBuffer.enableRenderer(IRenderer.RENDERER_SOFTWARE);
		
		while (jframe.isShowing())
		{
			if (box != null)
				box.rotateY(0.01f);
			
			frameBuffer.clear(java.awt.Color.green);
			
			
			world.renderScene(frameBuffer);
			world.draw(frameBuffer);
			
			frameBuffer.update();
			frameBuffer.display(jframe.getGraphics());
			Thread.sleep(10);
		}
		
		frameBuffer.disableRenderer(IRenderer.RENDERER_OPENGL);
		frameBuffer.dispose();
		jframe.dispose();
		System.exit(0);
	}
	
}
