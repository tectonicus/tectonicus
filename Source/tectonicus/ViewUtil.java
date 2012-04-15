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
package tectonicus;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.lwjgl.util.vector.Vector3f;

import tectonicus.configuration.ImageFormat;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.renderer.PerspectiveCamera;
import tectonicus.world.Sign;

public class ViewUtil
{
	public static final int viewWidth = 2048;
	public static final int viewHeight = 1152;

	public static class Viewpoint
	{
		public Vector3f eye = new Vector3f();
		public Vector3f lookAt = new Vector3f();
		public Vector3f up = new Vector3f();
		
		public float fov = 90.0f;
		
		public Viewpoint() {}
		
		public Viewpoint(Vector3f eye, Vector3f lookAt, Vector3f up)
		{
			this.eye.set(eye);
			this.lookAt.set(lookAt);
			this.up.set(up);
		}
	}
	
	private static Set<String> extractSettings(Sign sign)
	{
		String toParse = sign.getText(0);
		
		if (sign.getText(1).startsWith("#"))
			toParse += " " + sign.getText(1);
		
		if (sign.getText(2).startsWith("#"))
			toParse += " " + sign.getText(2);
		
		Set<String> settings = new HashSet<String>();
		
		StringTokenizer tokeniser = new StringTokenizer(toParse);
		while (tokeniser.hasMoreTokens())
		{
			String token = tokeniser.nextToken();
			if (token != null)
			{
				token = token.trim().toLowerCase();
				settings.add(token);
			}
		}
		
		return settings;
	}
	
	private static int parseHeight(Sign sign)
	{
		int height = 0;
		
		Set<String> settings = extractSettings(sign);
		for (String s : settings)
		{
			if (s.startsWith("h"))
			{
				String sub = s.substring(1).trim();
				try
				{
					height = Integer.parseInt(sub);
					break;
				}
				catch (Exception e)
				{}
			}
		}
		
		return height;
	}
	
	private static int parseElevation(Sign sign)
	{
		int angle = 90;
		
		Set<String> settings = extractSettings(sign);
		for (String s : settings)
		{
			if (s.startsWith("a"))
			{
				String sub = s.substring(1).trim();
				try
				{
					angle = Integer.parseInt(sub);
					break;
				}
				catch (Exception e)
				{}
			}
		}
		
		if (angle < 0)
			angle = 0;
		if (angle > 180)
			angle = 180;
		
		return angle;
	}
	
	public static Viewpoint findView(Sign sign)
	{
		final int heightOffset = parseHeight(sign);
		int elevation = parseElevation(sign);
		
		final float angleDeg = 90 / 4.0f * sign.getData() - 90;
		final float angleRad = angleDeg / 360f * 2.0f * (float)Math.PI;
		
		Vector3f eye = new Vector3f(sign.getX() + 0.5f, sign.getY() + 0.5f + heightOffset, sign.getZ() + 0.5f);
		
		Vector3f up, forward, lookAt;
		
		if (elevation == 0)
		{
			// Looking straight up
			up = new Vector3f((float)Math.cos(angleRad), 0, (float)Math.sin(angleRad));
			forward = new Vector3f(0, 1, 0);
		}
		else if (elevation == 180)
		{
			// Looking straight down
			up = new Vector3f((float)Math.cos(angleRad), 0, (float)Math.sin(angleRad));
			forward = new Vector3f(0, -1, 0);
		}
		else
		{
			// Use elevation angle
			
			final int adjustedElevation = elevation - 90; // convert into 0 straight ahead, -90 as up, +90 as down
			final float elevationRads = ((float)adjustedElevation / 360.0f) * 2.0f * (float)Math.PI;
			
			final float dy = -(float)Math.tan(elevationRads);
			
			up = new Vector3f(0, 1, 0);
			forward = new Vector3f((float)Math.cos(angleRad), dy, (float)Math.sin(angleRad));
		}
		
		lookAt = new Vector3f(eye.x + forward.x, eye.y + forward.y, eye.z + forward.z);
		
		// Make orthogonal
		Vector3f right = new Vector3f();
		Vector3f.cross(forward, up, right);
		Vector3f.cross(right, forward, up);
		
		return new Viewpoint(eye, lookAt, up);
	}
	
	
	public static PerspectiveCamera createCamera(Rasteriser rasteriser, Viewpoint view, final int drawDistance)
	{
		PerspectiveCamera perspectiveCamera = new PerspectiveCamera(rasteriser, viewWidth, viewHeight);
		perspectiveCamera.lookAt(view.eye, view.lookAt, view.up, view.fov, (float)viewWidth/(float)viewHeight, 0.1f, drawDistance);
		
		return perspectiveCamera;
	}
	
	public static File createViewFile(File viewsDir, Sign sign, ImageFormat imageFormat)
	{
		File viewFile = new File(viewsDir, "View_"+sign.getX()+"_"+sign.getY()+"_"+sign.getZ()+"."+imageFormat.getExtension());
		return viewFile;
	}

	public static LightStyle parseLightStyle(Sign sign)
	{
		LightStyle style = LightStyle.Day;
		
		Set<String> settings = extractSettings(sign);
		
		if (settings.contains("day"))
			style = LightStyle.Day;
		else if (settings.contains("night"))
			style = LightStyle.Night;
		
		return style;
	}
}
