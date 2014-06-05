/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.test;


public class DzzdTest // extends JApplet
{
	/*
	IRender3D render3d;
	IScene3D scene3d;
	
	Canvas canvas;

	public void start()
	{
		// Ask 3DzzD factory for a fresh Scene3D
		this.scene3d = DzzD.newScene3D();	
		
		//Create a Scene3D loader and link it to a 3DS file
		IScene3DLoader loader = DzzD.newScene3DLoader();
		loader.loadScene3D("file:D:/CodeRepository/Tectonicus/Data/Test/","CUBE.3DS");
		
		// Add the loader to the scene
		this.scene3d.setScene3DLoader(loader);
		
		//Wait until all object & texture are loaded
		while(this.scene3d.getNbMonitoredSceneObject()!=0)
		{
		 this.scene3d.updateMonitoredSceneObjects();	
		 DzzD.sleep(10);
		}
		
		this.scene3d.setCurrentCamera3DByName("Camera01");
		
		
		//Ask 3DzzD factory for a software 3D Render
		this.render3d = DzzD.newRender3D("SOFT", null);
	
		setSize(new Dimension(200, 400));
		
		//Add the Render3D canvas to the Applet Panel
		this.setLayout(new BorderLayout());
	//	getContentPane().add(render3d.getCanvas(), BorderLayout.NORTH);
		
		canvas = new Canvas();
		canvas.setMinimumSize( new Dimension(200, 200));
		canvas.setPreferredSize( new Dimension(200, 200));
		canvas.setMaximumSize( new Dimension(200, 200));
		getContentPane().add(canvas, BorderLayout.SOUTH);
		
		//Set the Render3D size and enable maximum antialias
		this.render3d.setSize(200, 200 ,7);
		
		//Set Camera Aspect ratio to 1:1
		this.scene3d.getCurrentCamera3D().setZoomY(((double)this.render3d.getWidth())/((double)this.render3d.getHeight()));	
		
		//Tell the Render3D wich camera it should use to render
		this.render3d.setCamera3D(this.scene3d.getCurrentCamera3D());
		
		
		
		//Render the frame
		this.renderSingleFrame();	

	}

	
	private void drawOnCanvas()
	{
		Graphics g = canvas.getGraphics();
		g.setColor(Color.BLUE);
		g.fillRect(0, 0, 200, 200);
	//	render3d.getCanvas().paint( g );
	}
	
	public void renderSingleFrame()
	{
		//Set the scene to world space
		this.scene3d.setScene3DObjectToWorld();
		
		//Set the scene to active camera space
		this.scene3d.setScene3DObjectToCamera();
		
		//Tell the 3D render to compute & draw the frame
		this.render3d.renderScene3D(this.scene3d);
		
		drawOnCanvas();
		
		try
		{
			BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_4BYTE_ABGR);
			render3d.getCanvas().paint( img.getGraphics() );
			ImageIO.write(img, "png", new File("C:/3dzzd.png"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	*/
}
