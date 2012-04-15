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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import tectonicus.cache.PlayerSkinCache;

public class PlayerIconAssembler
{
	private BufferedImage defaultSkin;
	
	private final PlayerSkinCache skinCache;
	
	public PlayerIconAssembler(PlayerSkinCache skinCache)
	{
		this.skinCache = skinCache;
		
		try
		{
			defaultSkin = ImageIO.read( getClass().getClassLoader().getResource("Images/DefaultSkin.png") );
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void writeDefaultIcon(File file)
	{
		try
		{
			BufferedImage icon = generateIcon(defaultSkin);
			if (icon != null)
			{
				ImageIO.write(icon, "png", file);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void writeIcon(String playerName, File file)
	{
		try
		{
			BufferedImage skin = fetchSkin(playerName);
			BufferedImage icon = generateIcon(skin);
			if (icon != null)
			{
				ImageIO.write(icon, "png", file);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private BufferedImage generateIcon(BufferedImage skin)
	{
		BufferedImage icon = new BufferedImage(16, 32, BufferedImage.TYPE_4BYTE_ABGR);
		
		BufferedImage head = skin.getSubimage(8, 8, 8, 8);
		BufferedImage body = skin.getSubimage(20, 20, 8, 12);
		BufferedImage leftArm = skin.getSubimage(48, 20, 4, 12);
		BufferedImage rightArm = skin.getSubimage(40, 20, 4, 12);
		BufferedImage leg = skin.getSubimage(4, 20, 4, 12);
		
		Graphics2D g = (Graphics2D)icon.getGraphics();
		g.setColor(new Color(0, 0, 0, 0));
		g.fillRect(0, 0, icon.getWidth(), icon.getHeight());
		
		g.drawImage(head, 4, 0, null);
		g.drawImage(body, 4, 8, null);
		g.drawImage(leftArm, 0, 9, 4, 21,   4, 0, 0, 12, null); // flip the left arm
		g.drawImage(rightArm, 12, 9, null);
		g.drawImage(leg, 4, 20, null);
		g.drawImage(leg, 8, 20, null);
		
		return icon;
	}

	private BufferedImage fetchSkin(String playerName)
	{
		BufferedImage customSkin = skinCache.fetchSkin(playerName);
		if (customSkin != null)
			return customSkin;
		else
			return defaultSkin;
	}
}
