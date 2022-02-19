/*
 * Copyright (c) 2022 Tectonicus contributors.  All rights reserved.
 *
 * This file is part of Tectonicus. It is subject to the license terms in the LICENSE file found in
 * the top-level directory of this distribution.  The full list of project contributors is contained
 * in the AUTHORS file found in the same location.
 *
 */

package tectonicus;

import org.joml.Vector3f;
import tectonicus.blockTypes.BlockRegistry;
import tectonicus.blockTypes.BlockStateModel;
import tectonicus.blockTypes.BlockStateWrapper;
import tectonicus.cache.BiomeCache;
import tectonicus.cache.BiomeData;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.raw.Biome;
import tectonicus.raw.BlockProperties;
import tectonicus.raw.RawChunk;
import tectonicus.raw.SignEntity;
import tectonicus.renderer.Camera;
import tectonicus.renderer.Geometry;
import tectonicus.renderer.OrthoCamera;
import tectonicus.texture.TexturePack;
import tectonicus.util.BoundingBox;
import tectonicus.world.World;
import tectonicus.world.filter.BlockFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chunk
{	
	private final ChunkCoord coord;
	
	private final BiomeCache biomeCache;
	
	private final BoundingBox bounds;
	
	private RawChunk rawChunk;
	private BiomeData biomeData;
	
	private Geometry geometry;
	
	private byte[] hash;
	
	public Chunk(ChunkCoord coord, BiomeCache biomeCache)
	{
		this.coord = new ChunkCoord(coord);
		this.biomeCache = biomeCache;
		
		Vector3f origin = new Vector3f(coord.x * RawChunk.WIDTH, 0, coord.z * RawChunk.DEPTH);
		this.bounds = new BoundingBox(origin, RawChunk.WIDTH, RawChunk.HEIGHT, RawChunk.DEPTH);
	}
	
	public ChunkCoord getCoord() { return coord; }
	
	public void calculateHash(MessageDigest hashAlgorithm)
	{
		if (rawChunk != null && hash == null)
		{
			hash = rawChunk.calculateHash(hashAlgorithm);
		}
	}
	
	public byte[] getHash() { return hash; }
	
	public RawChunk getRawChunk()
	{
		return rawChunk;
	}
	
	public void loadRaw(ChunkData chunkData, BlockFilter filter, WorldStats worldStats, Version version) throws IOException {
		if (rawChunk == null)
		{
			rawChunk = new RawChunk(chunkData, worldStats, version);
			filter.filter(rawChunk);
		}
		
		if (biomeData == null)
		{
			biomeData = biomeCache.loadBiomeData(coord);
		}
	}

	public void loadRaw(ChunkData chunkData, ChunkData entityChunkData, BlockFilter filter, WorldStats worldStats, Version version) throws IOException {
		if (rawChunk == null)
		{
			rawChunk = new RawChunk(chunkData, entityChunkData, worldStats, version);
			filter.filter(rawChunk);
		}

		if (biomeData == null)
		{
			biomeData = biomeCache.loadBiomeData(coord);
		}
	}
	
	public boolean createGeometry(Rasteriser rasteriser, World world, BlockTypeRegistry registry, BlockRegistry modelRegistry, BlockMaskFactory maskFactory, TexturePack texturePack)
	{
		if (rawChunk == null)
			return false;
		if (geometry != null)
			return false;
		
		BlockMask mask = maskFactory.createMask(coord, rawChunk);
		
		geometry = new Geometry(rasteriser);
		
		for (int y=0; y<RawChunk.HEIGHT; y++)
		{
			for (int x=0; x<RawChunk.WIDTH; x++)
			{
				for (int z=0; z<RawChunk.DEPTH; z++)
				{
					if (mask.isVisible(x, y, z))
					{
						BlockType type = null;
						final String blockName = rawChunk.getBlockName(x, y, z);
						if (blockName != null && !blockName.contains("air"))
						{
							final BlockProperties properties = rawChunk.getBlockState(x, y, z);

							// These blocks don't have models or they require special handling so they are created by the old system
							//TODO: we should be able to detect some of these programatically and not hard code all of these blocks
							if (blockName.equals("minecraft:water") || blockName.equals("minecraft:lava") //|| blockName.equals("minecraft:air") || blockName.equals("minecraft:cave_air")
									|| blockName.contains("shulker_box") || (blockName.contains("head") && !blockName.equals("minecraft:piston_head"))
									|| blockName.contains("skull") || blockName.contains("banner")
									|| blockName.endsWith("bed") || blockName.contains("sign") || blockName.contains("chest")
									|| blockName.contains("beacon") || blockName.equals("minecraft:end_portal") || blockName.equals("minecraft:end_gateway")
									|| blockName.equals("minecraft:conduit") || blockName.equals("minecraft:bell")) {
								type = registry.find(blockName);
							} else {
								if (modelRegistry.containsSingleVariantBlock(blockName)) {
									modelRegistry.getSingleVariantModel(blockName).createGeometry(x, y, z, world, rawChunk, geometry);
								} else {
									//TODO: This is quite slow. Need to profile and figure out if it can be sped up
									List<BlockStateModel> models;
									BlockStateWrapper stateWrapper = modelRegistry.getBlock(blockName);
									models = stateWrapper.getModels(properties);

									for (BlockStateModel model : models) {
										model.createGeometry(x, y, z, world, rawChunk, geometry);
									}
								}
							}

							//Render a water block at this same location if waterlogged
							if (properties != null && properties.containsKey("waterlogged") && properties.get("waterlogged").equals("true")
									|| blockName.equals("minecraft:kelp") || blockName.equals("minecraft:kelp_plant") || blockName.contains("seagrass")) {  //TODO: is there some way to avoid hard-coding these blocks?
								registry.find("minecraft:water").addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
							}
						}
						else
						{
							final int blockId = rawChunk.getBlockId(x, y, z);
							if (blockId == 0)
								continue;
							final int data = rawChunk.getBlockData(x, y, z);

							type = registry.find(blockId, data);
						}

						if (type != null)
						{
							if (x == 0 || y == 0 || z == 0 || x == RawChunk.WIDTH-1 || y == RawChunk.HEIGHT-1 || z == RawChunk.DEPTH-1)
							{
								type.addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
							}
							else
							{
								type.addInteriorGeometry(x, y, z, world, registry, rawChunk, geometry);
							}
						}
					}
				}
			}
		}

		// Create painting geometry
		BlockType type = registry.find(-1, 0);
		if (type != null)
			type.addEdgeGeometry(0, 0, 0, world, registry, rawChunk, geometry);
		
		// Create itemframe geometry
		type = registry.find(-2, 0);
		if (type != null)
			type.addEdgeGeometry(0, 0, 0, world, registry, rawChunk, geometry);
		
		/*
		for (int y=0; y<RawChunk.HEIGHT; y++)
		{
			for (int x=0; x<RawChunk.WIDTH; x++)
			{
				for (int z=0; z<RawChunk.DEPTH; z++)
				{
					if (x == 0 || y == 0 || z == 0 || x == RawChunk.WIDTH-1 || y == RawChunk.HEIGHT-1 || z == RawChunk.DEPTH-1)
					{
						if (mask.isVisible(x, y, z))
						{
							final int blockId = rawChunk.getBlockId(x, y, z);
							
							BlockType type = registry.find(blockId);
							if (type != null)
							{
								type.addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
							}
						}
					}
				}
			}
		}
		*/
		/*
		// North and south edges
		for (int y=0; y<RawChunk.HEIGHT; y++)
		{
			for (int x=0; x<RawChunk.WIDTH; x++)
			{
				addEdgeGeometry(x, y, 0, mask, world, registry);
				addEdgeGeometry(x, y, RawChunk.DEPTH-1, mask, world, registry);
			}
		}
		
		// East and west edges
		for (int y=0; y<RawChunk.HEIGHT; y++)
		{
			for (int z=1; z<RawChunk.DEPTH-1; z++)
			{
				addEdgeGeometry(0, y, z, mask, world, registry);
				addEdgeGeometry(RawChunk.WIDTH-1, y, z, mask, world, registry);
			}
		}
		
		// Top and bottom
		for (int x=1; x<RawChunk.WIDTH-1; x++)
		{
			for (int z=1; z<RawChunk.DEPTH-1; z++)
			{
				addEdgeGeometry(x, 0, z, mask, world, registry);
				addEdgeGeometry(x, RawChunk.HEIGHT-1, z, mask, world, registry);
			}
		}
		*/
		
		geometry.finalise();
		
		return true;
	}
	
	public void unloadRaw()
	{
		rawChunk = null;
	}
	
	public void unloadGeometry()
	{
		if (geometry != null)
			geometry.destroy();
		
		geometry = null;
	}
	
	public boolean isVisible(OrthoCamera camera)
	{
		return bounds.isVisible(camera);
	}
	
	public float getDistance(OrthoCamera camera)
	{
		final float centerDist = getDistance(camera, bounds.getCenterX(), bounds.getCenterY(), bounds.getCenterZ());
		return centerDist;
	}
	
	public static float getDistance(Camera camera, final float pointX, final float pointY, final float pointZ)
	{
		Vector3f eye = camera.getEyePosition();
		
		final float deltaX = eye.x - pointX;
		final float deltaY = eye.y - pointY;
		final float deltaZ = eye.z - pointZ;
		
		return tectonicus.util.Vector3f.length(deltaX, deltaY, deltaZ);
	}
	
	public void drawSolid(Camera camera)
	{
		if (geometry == null)
			return;

		geometry.drawSolidSurfaces(	coord.x * RawChunk.WIDTH,
									0,
									coord.z * RawChunk.DEPTH);
	}
	
	public void drawAlphaTestedSurfaces(Camera camera)
	{
		if (geometry == null)
			return;

		geometry.drawAlphaTestedSurfaces(	coord.x * RawChunk.WIDTH,
											0,
											coord.z * RawChunk.DEPTH);
	}
	
	public void drawTransparentSurfaces(Camera camera)
	{
		if (geometry == null)
			return;

		geometry.drawTransparentSurfaces(	coord.x * RawChunk.WIDTH,
											0,
											coord.z * RawChunk.DEPTH);
	}
	
	public ArrayList<Vector3f> getCornerPoints()
	{
		return bounds.getCornerPoints();
	}
	
	public int getBlockId(final int x, final int y, final int z, final int defaultId)
	{
		if (rawChunk == null)
			return defaultId;
		else
			return rawChunk.getBlockId(x, y, z);
	}
	
	public Biome getBiome(final int x, final int y, final int z)
	{
		return rawChunk.getBiome(x, y, z);
	}

	public int getSkyLight(final int x, final int y, final int z)
	{
		return getSkyLight(rawChunk, x, y, z);
	}
	
	public static int getSkyLight(RawChunk rawChunk, final int x, final int y, final int z)
	{
		//TODO: do we need this for backwards compatibility?
//		int actualY = y;
//		if (rawChunk.getBlockIdClamped(x, y, z, BlockIds.AIR) == BlockIds.SLAB)
//		{
//			if (y == RawChunk.HEIGHT-1)
//				return RawChunk.MAX_LIGHT;
//			else
//				actualY++;
//		}
			
		return rawChunk.getSkyLight(x, y, z);
	}
	
	public int getBlockLight(final int x, final int y, final int z)
	{
		return getBlockLight(rawChunk, x, y, z);
	}
	public static int getBlockLight(RawChunk rawChunk, final int x, final int y, final int z)
	{
//		if (rawChunk == null)
//			return RawChunk.MAX_LIGHT;
//		else
//		{
			//int actualY = y;
//			if (rawChunk.getBlockIdClamped(x, y, z, BlockIds.AIR) == BlockIds.SLAB)
//			{
//				if (y == RawChunk.HEIGHT-1)
//					return RawChunk.MAX_LIGHT;
//				else
//					actualY++;
//			}
			
		return rawChunk.getBlockLight(x, y, z);
//		}
	}
	
	public long getRawMemorySize()
	{
		return rawChunk.getMemorySize();
	}

	public long getGeometryMemorySize()
	{
		return geometry.getMemorySize();
	}

	public void printGeometryStats()
	{
		geometry.printGeometryStats();
	}

	public Map<String, SignEntity> getSigns()
	{
		if (rawChunk == null)
		{
			return new HashMap<>();
		}
		else
		{
			return rawChunk.getSigns();
		}
	}

	public Vector3f calcWorldPos(final float xOffset, final float yOffset, final float zOffset)
	{
		final float worldX = coord.x * RawChunk.WIDTH + xOffset;
		final float worldY = yOffset;
		final float worldZ = coord.z * RawChunk.DEPTH + zOffset;
		
		return new Vector3f(worldX, worldY, worldZ);
	}

	public BoundingBox getBounds()
	{
		return bounds;
	}
	
	public void collectStats(WorldStats worldStats)
	{
		assert (rawChunk != null);
		
		if (rawChunk != null)
		{
			for (int y=0; y<RawChunk.HEIGHT; y++)
			{
				for (int x=0; x<RawChunk.WIDTH; x++)
				{
					for (int z=0; z<RawChunk.DEPTH; z++)
					{
						final int blockId = rawChunk.getBlockId(x, y, z);
						final int blockData = rawChunk.getBlockData(x, y, z);
						
						worldStats.incBlockId(blockId, blockData);
					}		
				}
			}
		}
	}

	//TODO: is there any way to make this method faster?
	public static float getLight(LightStyle lightStyle, LightFace face, RawChunk c, final int x, final int y, final int z)
	{
		float result = 0;

		switch (lightStyle)
		{
			case Day:
			{
				if (c == null || y >= RawChunk.HEIGHT || y < 0)
				{
					result = 1.0f;
				}
				else
				{
					final int skyLight = c.getSkyLight(x, y, z);
					final int blockLight = c.getBlockLight(x, y, z);

					result = Util.clamp( (skyLight * 0.7f + blockLight * 0.3f) / RawChunk.MAX_LIGHT + 0.3f, 0, 1 );
				}

				if (face == LightFace.NorthSouth)
				{
					result -= 0.15f;
				}
				else if (face == LightFace.EastWest)
				{
					result -= 0.30f;
				}
				
				break;
			}
			case Night:
			{
				if (c == null || y >= RawChunk.HEIGHT || y < 0)
				{
					result = 0.1f;
				}
				else
				{
					final float skyLight = getSkyLight(c, x, y, z) / (float)RawChunk.MAX_LIGHT;
					final float blockLight = getBlockLight(c, x, y, z) / (float)RawChunk.MAX_LIGHT;
					
					result = Util.clamp( skyLight * 0.1f + blockLight * 0.7f + 0.1f, 0, 1 );
				}

				if (face == LightFace.NorthSouth)
				{
					result -= 0.05f;
				}
				else if (face == LightFace.EastWest)
				{
					result -= 0.1f;
				}
				
				break;
			}
			case Cave:
			{
				if (c == null || y >= RawChunk.HEIGHT || y < 0)
				{
					result = 0.1f;
				}
				else
				{
					final float blockLight = getBlockLight(c, x, y, z) / (float)RawChunk.MAX_LIGHT;
					final float heightScale = (y / (float)RawChunk.HEIGHT) * 0.6f + 0.1f;
					
					result = Util.clamp(heightScale + blockLight * 0.5f, 0f, 1f);
				}

				if (face == LightFace.NorthSouth)
				{
					result -= 0.05f;
				}
				else if (face == LightFace.EastWest)
				{
					result -= 0.1f;
				}
				
				break;
			}
			case None:
			{
				if (face == LightFace.Top)
				{
					result = 1.0f;
				}
				else if (face == LightFace.NorthSouth)
				{
					result = 0.85f;
				}
				else if (face == LightFace.EastWest)
				{
					result = 0.7f;
				}
				break;
			}
			default:
				assert false;
		}

		if(result < 0)
			result = 0;
		
		return result;
	}
}
