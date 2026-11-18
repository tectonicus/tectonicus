/*
 * Copyright (c) 2026 Tectonicus contributors. All rights reserved.
 * This file is subject to the license terms in the LICENSE file.
 */

package tectonicus.world;

import lombok.extern.slf4j.Slf4j;
import tectonicus.Portal;
import tectonicus.SaveFormat;
import tectonicus.configuration.Dimension;
import tectonicus.configuration.DimensionInfo;
import tectonicus.util.DirUtils;

import java.io.File;
import java.util.function.Function;

@Slf4j
public class PortalLinker {
	public record Link(Portal portal, String id, String destinationId) {}

	private final PortalScanner source;
	private final PortalScanner target;
	private final PortalSearch rules;
	private final double scale;

	public PortalLinker(PortalScanner source, PortalScanner target, PortalSearch rules, double scale) {
		this.source = source;
		this.target = target;
		this.rules = rules;
		this.scale = scale;
	}

	/**
	 * Opens saved terrain for a world's portals. Every dimension gets a source scanner, so pieces of one portal that a
	 * subset splits apart still resolve to the same identity. Exits are only searched between the Overworld and Nether.
	 */
	public static PortalLinker forWorld(World world) {
		Dimension sourceDimension = world.getDimension();
		var version = world.getWorldInfo().version();
		File sourceDirectory = DirUtils.getDimensionDir(world.getWorldDir().toPath(), world.getDimensionInfo()).toFile();
		var source = PortalScanner.open(sourceDirectory, version, world.getBiomeCache(), heightFor(world, world.getDimensionInfo().name(), sourceDimension));
		if (sourceDimension != Dimension.OVERWORLD && sourceDimension != Dimension.NETHER)
			return new PortalLinker(source, null, null, 1);
		Dimension targetDimension = sourceDimension == Dimension.OVERWORLD ? Dimension.NETHER : Dimension.OVERWORLD;
		File targetDirectory = DirUtils.getDimensionDir(world.getWorldDir().toPath(),
				new DimensionInfo(targetDimension, targetDimension.getId())).toFile();
		if (!targetDirectory.isDirectory())
			return new PortalLinker(source, null, null, 1);
		try {
			var target = PortalScanner.open(targetDirectory, version, world.getBiomeCache(), heightFor(world, targetDimension.getId(), targetDimension));
			var level = world.getLevelDat();
			PortalSearch rules = PortalSearch.forSave(level.getDataVersion(), level.getVersion(), target.getFormat(), targetDimension);
			return new PortalLinker(source, target, rules, targetDimension == Dimension.NETHER ? 1.0 / 8 : 8);
		} catch (RuntimeException e) {
			log.warn("Could not open {} terrain for portal exits. Exporting portals without jump links.", targetDimension, e);
			return new PortalLinker(source, null, null, 1);
		}
	}

	private static Function<SaveFormat, DimensionHeight> heightFor(World world, String dimensionId, Dimension dimension) {
		var level = world.getLevelDat();
		return format -> DimensionHeight.resolve(world.getWorldDir().toPath(), level.getDimensionTypes(), level.getDataPacks(),
				dimensionId, dimension, world.getWorldInfo().version(), format);
	}

	public Link resolve(Portal visible) {
		Portal complete;
		try {
			complete = source.complete(visible.columns().getFirst());
		} catch (RuntimeException e) {
			log.warn("Could not resolve portal at {}. Omitting its jump link.", visible.position(), e);
			return new Link(visible, null, null);
		}
		Portal destination = null;
		try {
			if (target != null)
				destination = target.findExit(complete.entryPos(), scale, rules);
		} catch (RuntimeException e) {
			log.warn("Could not search for an exit from portal at {}. Omitting its jump link.", visible.position(), e);
		}
		// Keep the marker on a rendered block while retaining the complete portal's identity and entry point.
		Portal marker = new Portal(visible.position(), complete.entryPos(), visible.columns());
		return new Link(marker, complete.id(), destination == null ? null : destination.id());
	}
}
