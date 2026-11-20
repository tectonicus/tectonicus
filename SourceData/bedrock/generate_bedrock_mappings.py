#!/usr/bin/env python3
"""Generates the Bedrock to Java block and biome tables in src/main/resources/bedrock.

Source data comes from PrismarineJS minecraft-data (MIT licence,
https://github.com/PrismarineJS/minecraft-data). Download its npm package, unpack it, and run:

    python3 generate_bedrock_mappings.py <path to package/minecraft-data/data> <output dir>

blocks.tsv.gz holds one line per Bedrock block state: the Bedrock name and states, a tab, then the Java
name and properties, both in name[key=value,...] form with keys sorted. Tables for older Bedrock versions
fill in block names that newer versions renamed, so worlds with sub-chunks saved by older versions still
map. The newest table wins when two tables disagree.

legacy.tsv.gz maps the data values that Bedrock 1.2.13 to 1.12 saved in palettes ({name, val} instead of
{name, states}) to states: the Bedrock name, a tab, the data value, a tab, then the state in the blocks.tsv form.
The bedrock/1.16.201 blockStates.json lists each block's states in data value order, so a state's position among
its block's states is its data value.
"""
import gzip
import json
import re
import sys
from pathlib import Path

# Java blocks renamed after the Java version an older table targets.
JAVA_RENAMES = {
	'minecraft:grass': 'minecraft:short_grass',
	'minecraft:chain': 'minecraft:iron_chain',
}

# Bedrock biome names to Java biome ids. Bedrock still has the pre-1.18 variants, which Java merged.
BIOMES = {
	'ocean': 'ocean', 'plains': 'plains', 'desert': 'desert', 'extreme_hills': 'windswept_hills', 'forest': 'forest',
	'taiga': 'taiga', 'swampland': 'swamp', 'river': 'river', 'hell': 'nether_wastes', 'the_end': 'the_end',
	'legacy_frozen_ocean': 'frozen_ocean', 'frozen_river': 'frozen_river', 'ice_plains': 'snowy_plains',
	'ice_mountains': 'snowy_plains', 'mushroom_island': 'mushroom_fields', 'mushroom_island_shore': 'mushroom_fields',
	'beach': 'beach', 'desert_hills': 'desert', 'forest_hills': 'forest', 'taiga_hills': 'taiga',
	'extreme_hills_edge': 'windswept_hills', 'jungle': 'jungle', 'jungle_hills': 'jungle', 'jungle_edge': 'sparse_jungle',
	'deep_ocean': 'deep_ocean', 'stone_beach': 'stony_shore', 'cold_beach': 'snowy_beach', 'birch_forest': 'birch_forest',
	'birch_forest_hills': 'birch_forest', 'roofed_forest': 'dark_forest', 'cold_taiga': 'snowy_taiga',
	'cold_taiga_hills': 'snowy_taiga', 'mega_taiga': 'old_growth_pine_taiga', 'mega_taiga_hills': 'old_growth_pine_taiga',
	'extreme_hills_plus_trees': 'windswept_forest', 'savanna': 'savanna', 'savanna_plateau': 'savanna_plateau',
	'mesa': 'badlands', 'mesa_plateau_stone': 'wooded_badlands', 'mesa_plateau': 'badlands', 'warm_ocean': 'warm_ocean',
	'deep_warm_ocean': 'warm_ocean', 'lukewarm_ocean': 'lukewarm_ocean', 'deep_lukewarm_ocean': 'deep_lukewarm_ocean',
	'cold_ocean': 'cold_ocean', 'deep_cold_ocean': 'deep_cold_ocean', 'frozen_ocean': 'frozen_ocean',
	'deep_frozen_ocean': 'deep_frozen_ocean', 'bamboo_jungle': 'bamboo_jungle', 'bamboo_jungle_hills': 'bamboo_jungle',
	'sunflower_plains': 'sunflower_plains', 'desert_mutated': 'desert', 'extreme_hills_mutated': 'windswept_gravelly_hills',
	'flower_forest': 'flower_forest', 'taiga_mutated': 'taiga', 'swampland_mutated': 'swamp',
	'ice_plains_spikes': 'ice_spikes', 'jungle_mutated': 'jungle', 'jungle_edge_mutated': 'sparse_jungle',
	'birch_forest_mutated': 'old_growth_birch_forest', 'birch_forest_hills_mutated': 'old_growth_birch_forest',
	'roofed_forest_mutated': 'dark_forest', 'cold_taiga_mutated': 'snowy_taiga',
	'redwood_taiga_mutated': 'old_growth_spruce_taiga', 'redwood_taiga_hills_mutated': 'old_growth_spruce_taiga',
	'extreme_hills_plus_trees_mutated': 'windswept_gravelly_hills', 'savanna_mutated': 'windswept_savanna',
	'savanna_plateau_mutated': 'windswept_savanna', 'mesa_bryce': 'eroded_badlands',
	'mesa_plateau_stone_mutated': 'wooded_badlands', 'mesa_plateau_mutated': 'badlands',
	'soulsand_valley': 'soul_sand_valley', 'crimson_forest': 'crimson_forest', 'warped_forest': 'warped_forest',
	'basalt_deltas': 'basalt_deltas', 'jagged_peaks': 'jagged_peaks', 'frozen_peaks': 'frozen_peaks',
	'snowy_slopes': 'snowy_slopes', 'grove': 'grove', 'meadow': 'meadow', 'lush_caves': 'lush_caves',
	'dripstone_caves': 'dripstone_caves', 'stony_peaks': 'stony_peaks', 'deep_dark': 'deep_dark',
	'mangrove_swamp': 'mangrove_swamp', 'cherry_grove': 'cherry_grove', 'pale_garden': 'pale_garden',
}
# Numeric ids missing from the newest minecraft-data table that lists real ids (bedrock/1.20.0).
EXTRA_BIOME_IDS = {192: 'cherry_grove', 193: 'pale_garden'}

# The newest minecraft-data table whose block states are listed in data value order.
LEGACY_VERSION = '1.16.201'

STATE = re.compile(r'^([^\[]+)\[(.*)\]$')


def version_key(name):
	return tuple(int(p) for p in name.split('.'))


def canonical(state):
	match = STATE.match(state)
	if not match:
		return state
	props = sorted(p for p in match.group(2).split(',') if p)
	return f'{match.group(1)}[{",".join(props)}]'


def main(data_dir, out_dir):
	bedrock = Path(data_dir) / 'bedrock'
	versions = sorted((p.name for p in bedrock.iterdir() if (p / 'blocksB2J.json').exists()), key=version_key, reverse=True)
	blocks = {}
	for version in versions:
		table = json.loads((bedrock / version / 'blocksB2J.json').read_text())
		for b, j in table.items():
			key = canonical(b)
			if key in blocks:
				continue
			java = canonical(j)
			match = STATE.match(java)
			name = match.group(1) if match else java
			if name in JAVA_RENAMES:
				java = JAVA_RENAMES[name] + java[len(name):]
			blocks[key] = java
	out = Path(out_dir)
	out.mkdir(parents=True, exist_ok=True)
	with gzip.open(out / 'blocks.tsv.gz', 'wt', encoding='utf-8', compresslevel=9) as f:
		f.write(f'# Bedrock to Java block states from minecraft-data {", ".join(versions)} (MIT licence)\n')
		for key in sorted(blocks):
			f.write(f'{key}\t{blocks[key]}\n')

	legacy = {}
	for state in json.loads((bedrock / LEGACY_VERSION / 'blockStates.json').read_text()):
		name = 'minecraft:' + state['name']
		values = legacy.setdefault(name, [])
		if len(values) < 16:
			props = ','.join(f'{k}={v["value"]}' for k, v in sorted(state['states'].items()))
			values.append(f'{name}[{props}]')
	with gzip.open(out / 'legacy.tsv.gz', 'wt', encoding='utf-8', compresslevel=9) as f:
		f.write(f'# Bedrock data values to block states from minecraft-data {LEGACY_VERSION} (MIT licence)\n')
		for name in sorted(legacy):
			for value, key in enumerate(legacy[name]):
				f.write(f'{name}\t{value}\t{key}\n')

	ids = {b['id']: b['name'] for b in json.loads((bedrock / '1.20.0' / 'biomes.json').read_text())}
	ids.update(EXTRA_BIOME_IDS)
	with open(out / 'biomes.tsv', 'w', encoding='utf-8', newline='\n') as f:
		f.write('# Bedrock biome id, Bedrock name, Java biome id\n')
		for biome_id in sorted(ids):
			name = ids[biome_id]
			f.write(f'{biome_id}\t{name}\tminecraft:{BIOMES[name]}\n')
	print(f'{len(blocks)} block states from {len(versions)} tables, {len(ids)} biomes')


if __name__ == '__main__':
	main(sys.argv[1], sys.argv[2])
