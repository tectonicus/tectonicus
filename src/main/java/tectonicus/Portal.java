/*
 * Copyright (c) 2026 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 */

package tectonicus;

import tectonicus.PortalColumn.Axis;
import tectonicus.util.Vector3l;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record Portal(Vector3l position, EntryPosition entryPos, List<PortalColumn> columns) {
	public record EntryPosition(double x, int y, double z) {}
	private record ColumnPosition(long x, long z) {}
	private static final Comparator<PortalColumn> COLUMN_ORDER = Comparator.comparingLong(PortalColumn::getX)
			.thenComparingLong(PortalColumn::getZ).thenComparingInt(PortalColumn::getMinY);

	public String id() {
		PortalColumn first = columns.stream().min(COLUMN_ORDER).orElseThrow();
		return first.getX() + "," + first.getMinY() + "," + first.getZ();
	}

	public static boolean connects(PortalColumn a, PortalColumn b) {
		boolean alongX = Math.abs(a.getX() - b.getX()) == 1 && a.getZ() == b.getZ()
				&& a.getAxis() != Axis.Z && b.getAxis() != Axis.Z;
		boolean alongZ = Math.abs(a.getZ() - b.getZ()) == 1 && a.getX() == b.getX()
				&& a.getAxis() != Axis.X && b.getAxis() != Axis.X;
		return (alongX || alongZ) && a.getMinY() <= b.getMaxY() && a.getMaxY() >= b.getMinY();
	}

	public static List<Portal> groupColumns(List<PortalColumn> columns) {
		var ordered = columns.stream().sorted(COLUMN_ORDER).toList();
		Map<ColumnPosition, List<PortalColumn>> byPosition = new HashMap<>();
		for (PortalColumn column : ordered) {
			var position = new ColumnPosition(column.getX(), column.getZ());
			byPosition.computeIfAbsent(position, key -> new ArrayList<>()).add(column);
		}

		Set<PortalColumn> remaining = new HashSet<>(ordered);
		List<Portal> portals = new ArrayList<>();
		for (PortalColumn first : ordered) {
			if (!remaining.remove(first))
				continue;
			List<PortalColumn> connected = new ArrayList<>();
			connected.add(first);
			for (int i = 0; i < connected.size(); i++) {
				PortalColumn column = connected.get(i);
				long x = column.getX();
				long z = column.getZ();
				if (column.getAxis() != Axis.Z) {
					connectColumns(connected, remaining, column, byPosition.get(new ColumnPosition(x - 1, z)));
					connectColumns(connected, remaining, column, byPosition.get(new ColumnPosition(x + 1, z)));
				}
				if (column.getAxis() != Axis.X) {
					connectColumns(connected, remaining, column, byPosition.get(new ColumnPosition(x, z - 1)));
					connectColumns(connected, remaining, column, byPosition.get(new ColumnPosition(x, z + 1)));
				}
			}
			portals.add(fromColumns(connected));
		}
		return portals;
	}

	private static void connectColumns(List<PortalColumn> connected, Set<PortalColumn> remaining,
			PortalColumn column, List<PortalColumn> neighbors) {
		if (neighbors == null)
			return;
		for (PortalColumn neighbor : neighbors) {
			if (connects(column, neighbor)
					&& remaining.remove(neighbor)) {
				connected.add(neighbor);
			}
		}
	}

	private static Portal fromColumns(List<PortalColumn> columns) {
		int bottomY = columns.stream().mapToInt(PortalColumn::getMinY).min().orElseThrow();
		int topY = columns.stream().mapToInt(PortalColumn::getMaxY).max().orElseThrow();
		var bottomRow = columns.stream().filter(column -> column.getMinY() == bottomY).toList();
		long minX = bottomRow.stream().mapToLong(PortalColumn::getX).min().orElseThrow();
		long maxX = bottomRow.stream().mapToLong(PortalColumn::getX).max().orElseThrow();
		long minZ = bottomRow.stream().mapToLong(PortalColumn::getZ).min().orElseThrow();
		long maxZ = bottomRow.stream().mapToLong(PortalColumn::getZ).max().orElseThrow();
		var entry = new EntryPosition(minX + (maxX - minX + 1) / 2.0, bottomY, minZ + (maxZ - minZ + 1) / 2.0);
		// Place the marker on a visible block even when the render subset cuts through the portal.
		int centerY = bottomY + (topY - bottomY + 1) / 2;
		long centerX = (long) Math.floor(entry.x());
		long centerZ = (long) Math.floor(entry.z());
		Vector3l position = columns.stream().map(column -> new Vector3l(column.getX(),
				Math.clamp(centerY, column.getMinY(), column.getMaxY()), column.getZ()))
				.min(Comparator.comparingDouble(point -> Math.hypot(Math.hypot(point.x - centerX, point.z - centerZ),
						point.y - centerY))).orElseThrow();
		return new Portal(position, entry, List.copyOf(columns));
	}
}
