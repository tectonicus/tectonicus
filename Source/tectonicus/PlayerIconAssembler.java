/*
 * Copyright (c) 2012-2014, John Campbell and other contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
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
