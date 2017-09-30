/*
 * Copyright (c) 2012-2017, John Campbell and other contributors.  All rights reserved.
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
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

import tectonicus.cache.PlayerSkinCache;
import tectonicus.raw.Player;

public class PlayerIconAssembler
{	
	private final PlayerSkinCache skinCache;
	
	public PlayerIconAssembler(PlayerSkinCache skinCache)
	{
		this.skinCache = skinCache;
	}

	public void writeDefaultIcon(BufferedImage skin, File file)
	{
		try
		{
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
	
	public void writeIcon(Player player, File file)
	{
		try
		{
			BufferedImage skin = skinCache.fetchSkin(player);
			if (skin != null)
			{
				BufferedImage icon = generateIcon(skin);
				if (icon != null)
				{
					ImageIO.write(icon, "png", file);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private BufferedImage generateIcon(BufferedImage skin)
	{
		int imgSize = skin.getWidth();
		int factor = (int) (imgSize / 64.0f);
		
		BufferedImage icon = new BufferedImage(factor*16, factor*32, BufferedImage.TYPE_4BYTE_ABGR);
		
		BufferedImage head = skin.getSubimage(factor*8, factor*8, factor*8, factor*8);
		BufferedImage body = skin.getSubimage(factor*20, factor*20, factor*8, factor*12);
		BufferedImage leftArm = skin.getSubimage(factor*48, factor*20, factor*4, factor*12);
		BufferedImage rightArm = skin.getSubimage(factor*40, factor*20, factor*4, factor*12);
		BufferedImage leg = skin.getSubimage(factor*4, factor*20, factor*4, factor*12);
		
		Graphics2D g = (Graphics2D)icon.getGraphics();
		g.setColor(new Color(0, 0, 0, 0));
		g.fillRect(0, 0, icon.getWidth(), icon.getHeight());
		
		g.drawImage(head, factor*4, 0, null);
		g.drawImage(body, factor*4, factor*8, null);
		g.drawImage(leftArm, 0, factor*9, factor*4, factor*21,   factor*4, 0, 0, factor*12, null); // flip the left arm
		g.drawImage(rightArm, factor*12, factor*9, null);
		g.drawImage(leg, factor*4, factor*20, null);
		g.drawImage(leg, factor*8, factor*20, null);
		
		return icon;
	}
	
	public class WriteIconTask implements Callable<Void>
	{
		Player player;
		File iconFile;
		
		public WriteIconTask(Player player, File iconFile)
		{
			this.player = player;
			this.iconFile = iconFile;
		}

		@Override
		public Void call() throws Exception
		{
			writeIcon(player, iconFile);
			return null;
		}
	}
}
