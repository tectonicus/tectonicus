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


public class DzzdCreateMeshTest // extends Applet implements Runnable
{
	/*
	public static final long serialVersionUID = 0x00000001;
	IRender3D render;
	IScene3D scene;
	boolean run = false;

	public void start()
	{
		// Ask 3DzzD factory for a fresh Scene3D
		this.scene = DzzD.newScene3D();

		// Set the active camera in the 3d scene
		// 
		// We use a camera that is inside the 3ds file
		//
		// 3DzzD always provide a default camera that you can set using :
		// this.scene.setCurrentCamera3DById(0);
		// this.scene.setCurrentCamera3DByName("Camera01");

		// Ask 3DzzD factory for a software 3D Render
		this.render = DzzD.newRender3D("SOFT", null);

		// Add the Render3D canvas to the Applet Panel
		this.setLayout(null);
		this.add(this.render.getCanvas());

		// Set the Render3D size
		this.render.setSize(this.getSize().width, this.getSize().height, 1);

		// Set Camera Aspect ratio to 1:1
		this.scene.getCurrentCamera3D().setZoomY(
				((double) this.render.getWidth())
						/ ((double) this.render.getHeight()));

		// Tell the Render3D wich camera it should use to render
		System.out.println(this.scene.getCurrentCamera3D().getName());
		this.render.setCamera3D(this.scene.getCurrentCamera3D());

		// Create a main Thread
		Thread mainThread = new Thread(this);
		this.run = true;
		mainThread.start();
	}
	
	// Tell the main thread that it can stop running when Applet is destroyed
	public void destroy() {
		this.run = false;
	}

	private void createScene3D()
	{
		final int size = 10;
		
		IVertex3D vertices[] = DzzD.newVertex3DArray(4);
		(vertices[0] = DzzD.newVertex3D()).set(0, 0, 0); // bottom,near-left
		(vertices[1] = DzzD.newVertex3D()).set(0, 0, size); // top,far-left
		(vertices[2] = DzzD.newVertex3D()).set(size, 0, size); // top,far-right
		(vertices[3] = DzzD.newVertex3D()).set(size, 0, 0); // bottom,near-left

		IFace3D faces[] = DzzD.newFace3DArray(2);
		faces[0] = DzzD.newFace3D(vertices[0], vertices[1], vertices[2]);
		faces[1] = DzzD.newFace3D(vertices[2], vertices[3], vertices[0]);

		IMesh3D mesh = DzzD.newMesh3D(vertices, faces);
	//	mesh.getPosition().set(0, -1, 8);
	//	mesh.getRotation().set(Math.PI * 0.25, Math.PI * 0.51, 0);

		IMaterial material = DzzD.newMaterial();
		mesh.getFace3D(0).setMaterial(material);
		mesh.getFace3D(1).setMaterial(this.scene.getMaterialById(0));
	//	material.setDiffuseColor(0x550000);
		material.setDiffuseColor(0xFFFFFF);
		material.setSelfIlluminationLevel(255);
		material.setTwoSide(true);
		
		IURLTexture urlTexture = DzzD.newURLTexture();
		urlTexture.setBaseURL(this.getCodeBase().toString() + "../Data/Test/");
		urlTexture.setSourceFile("HELLO.JPG");

		material.setDiffuseTexture(urlTexture);
	//	material.setDiffuseColor(Color.red.getRGB());

		IMappingUV mappingUV = DzzD.newMappingUV();
		mappingUV.setVZoom(2);
		mappingUV.setVOffset(0.5f);
		material.setMappingUV(mappingUV);

		float mapping[] = { 0, 1, 0, 0, 1, 0, 1, 0, 1, 1, 0, 1 };
		mesh.setMappingUV(mapping);

		
		
		
		// CAMERA
		
		Vector3f eye = new Vector3f(0, 20, 20);
		Vector3f target = new Vector3f(0, 0, 0);
		scene.getCurrentCamera3D().getPosition().set(eye.x, eye.y, eye.z);
		
	
	//	scene.getCurrentCamera3D().setTarget(null);
	//	
	//	Matrix4f lookAt = MatrixUtil.createLookAt(eye, target, new Vector3f(0, 1, 0));
	//	IAxis3D cameraAxis = DzzD.newAxis3D();
	//	
	//	cameraAxis.getAX().set(lookAt.m00, lookAt.m01, lookAt.m02);
	//	cameraAxis.getAY().set(lookAt.m10, lookAt.m11, lookAt.m12);
	//	cameraAxis.getAZ().set(lookAt.m20, lookAt.m21, lookAt.m22);
	//	
	//	cameraAxis.getAX().set(lookAt.m00, lookAt.m10, lookAt.m20);
	//	cameraAxis.getAY().set(lookAt.m01, lookAt.m11, lookAt.m21);
	//	cameraAxis.getAZ().set(lookAt.m02, lookAt.m12, lookAt.m22);
	//	
	//	IPoint3D rotation = DzzD.newPoint3D();
	//	cameraAxis.getRotationXZY(rotation);
	//	
	//	scene.getCurrentCamera3D().getRotation().set(rotation.getX(), rotation.getY(), rotation.getZ());
	//	
	//	this.render.setCamera3D(this.scene.getCurrentCamera3D());
	//	scene.setCurrentCamera3DById(0);
	
		
		cameraLookAt(scene.getCurrentCamera3D(), DzzD.newPoint3D().set(target.x, target.y, target.z));
		
		
		this.scene.addMesh3D(mesh);
		this.scene.addMaterial(material);
		this.scene.addTexture(urlTexture);

		scene.setBackgroundColor(0x555555);
		
		this.render.setAntialiasLevel(0);
	}
	
	private static void cameraLookAt(ICamera3D camera, IPoint3D target)
	{
		IPoint3D localTarget = DzzD.newPoint3D();
		
		localTarget.copy(target).sub(camera.getPosition());
		
		double distance = localTarget.length();
		localTarget.mul(1.0 / distance);
		double rx = Math.asin(localTarget.getY());
		
		localTarget.copy(target).sub(camera.getPosition());
		localTarget.setY(0);
		distance = localTarget.length();
		localTarget.mul(1.0 / distance);
		double ry = -Math.asin(localTarget.getX());
		if (localTarget.getZ() < 0)
			ry = Math.PI - ry;
		
		camera.getRotation().set(rx, ry, 0.001);
	}

	// Here is the mainThread run method called by mainThread.start(); (inded
	// this method is started in a different Thread)
	public void run()
	{
		this.createScene3D();
		
		while (this.run)
		{
			// Render a single frame
			this.renderSingleFrame();
			Thread.yield();
		}
	}

	public void renderSingleFrame()
	{
		// Set the scene to world space
		this.scene.setScene3DObjectToWorld();

		// Set the scene to active camera space
		this.scene.setScene3DObjectToCamera();

		// Tell the 3D render to compute & draw the frame
		this.render.renderScene3D(this.scene);
	}
	*/
}
