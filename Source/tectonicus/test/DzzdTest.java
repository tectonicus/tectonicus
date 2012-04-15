/*
 * Source code from Tectonicus, http://code.google.com/p/tectonicus/
 *
 * Tectonicus is released under the BSD license (below).
 *
 *
 * Original code John Campbell / "Orangy Tang" / www.triangularpixels.com
 *
 * Copyright (c) 2012, John Campbell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   * Neither the name of 'Tecctonicus' nor the names of
 *     its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
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
