/*
 * Copyright (c) 2024 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus.world;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

@Getter
@RequiredArgsConstructor
public enum Effect {
	NONE(0),
	SPEED(1),
	SLOWNESS(2),
	HASTE(3),
	MINING_FATIGUE(4),
	STRENGTH(5),
	INSTANT_HEALTH(6),
	INSTANT_DAMAGE(7),
	JUMP_BOOST(8),
	NAUSEA(9),
	REGENERATION(10),
	RESISTANCE(11),
	FIRE_RESISTANCE(12),
	WATER_BREATHING(13),
	INVISIBILITY(14),
	BLINDNESS(15),
	NIGHT_VISION(16),
	HUNGER(17),
	WEAKNESS(18),
	POISON(19),
	WITHER(20),
	HEALTH_BOOST(21),
	ABSORPTION(22),
	SATURATION(23),
	GLOWING(24),
	LEVITATION(25),
	LUCK(26),
	UNLUCK(27),
	SLOW_FALLING(28),
	CONDUIT_POWER(29),
	DOLPHINS_GRACE(30),
	BAD_OMEN(31),
	HERO_OF_THE_VILLAGE(32),
	DARKNESS(33),
	TRIAL_OMEN(34),
	RAID_OMEN(35),
	WIND_CHARGED(36),
	WEAVING(37),
	OOZING(38),
	INFESTED(39);
	
	private static final Effect[] ID_LOOKUP = Stream.of(Effect.values()).toArray(Effect[]::new);
	private static final Map<String, Effect> NAME_LOOKUP = Stream.of(Effect.values()).collect(toMap(effect -> effect.name().toLowerCase(), identity()));
	private final int id;
	
	public static Effect byId(int id) {
		return ID_LOOKUP[Math.max(id, 0)];
	}
	
	public static Effect byName(String name) {
		return NAME_LOOKUP.get(name.toLowerCase());
	}
}
