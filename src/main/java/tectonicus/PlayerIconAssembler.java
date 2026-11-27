/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import lombok.extern.slf4j.Slf4j;
import tectonicus.cache.PlayerSkinCache;
import tectonicus.raw.Player;
import tectonicus.util.OutputResourcesUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class PlayerIconAssembler implements AutoCloseable
{
	/** Size of the square head icon used on the player marker toggle button. */
	public static final int HEAD_ICON_SIZE = 32;

	private final PlayerSkinCache skinCache;
	private final ExecutorService iconWriters = Executors.newFixedThreadPool(4, Thread.ofPlatform().name("player-icon-", 1).factory());

	public PlayerIconAssembler(PlayerSkinCache skinCache)
	{
		this.skinCache = skinCache;
	}

	/** Writes the figure shown for players without a skin of their own. */
	public void writeDefaultIcon(BufferedImage skin, boolean slim, File file)
	{
		write(PlayerAvatarRenderer.renderBody(skin, slim), file);
	}

	/** Writes the head of the default skin for the player marker toggle button. */
	public void writeDefaultHeadIcon(BufferedImage skin, File file)
	{
		write(PlayerAvatarRenderer.renderHead(skin, HEAD_ICON_SIZE), file);
	}

	public void writeIcon(Player player, File file)
	{
		try
		{
			BufferedImage skin = skinCache.fetchSkin(player);
			if (skin != null)
			{
				// The account profile names the arm model; without it, guess from the skin like Minecraft does.
				boolean slim = player.getSlimSkin() != null ? player.getSlimSkin() : PlayerAvatarRenderer.looksSlim(skin);
				write(PlayerAvatarRenderer.renderBody(skin, slim), file);
			}
		}
		catch (Exception e)
		{
			log.error("Exception: ", e);
		}
	}

	private static void write(BufferedImage icon, File file)
	{
		if (icon == null)
		{
			log.warn("Unable to render player icon {}: the skin doesn't have skin proportions", file.getName());
			return;
		}
		OutputResourcesUtil.writeImage(icon, icon.getWidth(), icon.getHeight(), file);
	}

	public void writeIconAsync(Player player, File iconFile) {
		iconWriters.execute(() -> writeIcon(player, iconFile));
	}

	@Override
	public void close() {
		iconWriters.close();
	}
}
