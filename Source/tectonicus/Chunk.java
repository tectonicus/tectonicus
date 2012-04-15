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

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;

import org.jnbt.NBTInputStream.Compression;
import org.lwjgl.util.vector.Vector3f;

import tectonicus.cache.BiomeCache;
import tectonicus.cache.BiomeData;
import tectonicus.configuration.LightFace;
import tectonicus.configuration.LightStyle;
import tectonicus.rasteriser.Rasteriser;
import tectonicus.raw.RawChunk;
import tectonicus.raw.RawSign;
import tectonicus.renderer.Camera;
import tectonicus.renderer.Geometry;
import tectonicus.renderer.OrthoCamera;
import tectonicus.texture.TexturePack;
import tectonicus.util.BoundingBox;
import tectonicus.util.Vector3l;
import tectonicus.world.World;
import tectonicus.world.filter.BlockFilter;

public class Chunk
{	
	private final ChunkCoord coord;
	
	private final BiomeCache biomeCache;
	
	private BoundingBox bounds;
	
	private RawChunk rawChunk;
	private BiomeData biomeData;
	
	private Geometry geometry;
	
	private byte[] hash;
	
	public Chunk(ChunkCoord coord, BiomeCache biomeCache) throws Exception
	{
		this.coord = new ChunkCoord(coord);
		this.biomeCache = biomeCache;
		
		Vector3l origin = new Vector3l(coord.x * RawChunk.WIDTH, 0, coord.z * RawChunk.DEPTH);
		this.bounds = new BoundingBox(origin, RawChunk.WIDTH, RawChunk.HEIGHT, RawChunk.DEPTH);
	}
	
	public ChunkCoord getCoord() { return coord; }
	
	public void calculateHash(MessageDigest hashAlgorithm)
	{
		assert (rawChunk != null);
		
		if (rawChunk != null)
		{
			if (hash == null)
				hash = rawChunk.calculateHash(hashAlgorithm);
		}
	}
	
	public byte[] getHash() { return hash; }
	
	public RawChunk getRawChunk()
	{
		return rawChunk;
	}
	
	public void loadRaw(InputStream in, Compression compression, BlockFilter filter) throws Exception
	{
		if (rawChunk == null)
		{
			rawChunk = new RawChunk(in, compression);
			filter.filter(rawChunk);
		}
		
		if (biomeData == null)
		{
			biomeData = biomeCache.loadBiomeData(coord);
		}
	}
	
	public boolean createGeometry(Rasteriser rasteriser, World world, BlockTypeRegistry registry, BlockMaskFactory maskFactory, TexturePack texturePack)
	{
		if (rawChunk == null)
			return false;
		if (geometry != null)
			return false;
		
		BlockMask mask = maskFactory.createMask(coord, rawChunk); 
		
		geometry = new Geometry(rasteriser, texturePack.getTexture());
		
		for (int y=0; y<RawChunk.HEIGHT; y++)
		{
			for (int x=0; x<RawChunk.WIDTH; x++)
			{
				for (int z=0; z<RawChunk.DEPTH; z++)
				{
					if (mask.isVisible(x, y, z))
					{
						final int blockId = rawChunk.getBlockId(x, y, z);
						final int data = rawChunk.getBlockData(x, y, z);
						
						BlockType type = registry.find(blockId, data);
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
	
	public int getBiomeId(final int x, final int y, final int z)
	{
		return rawChunk.getBiomeId(x, y, z);
	}

	public int getSkyLight(final int x, final int y, final int z)
	{
		return getSkyLight(rawChunk, x, y, z);
	}
	
	public static int getSkyLight(RawChunk rawChunk, final int x, final int y, final int z)
	{
		if (rawChunk == null)
			return RawChunk.MAX_LIGHT;
		else
		{
			int actualY = y;
			if (rawChunk.getBlockIdClamped(x, y, z, BlockIds.AIR) == BlockIds.SLAB)
			{
				if (y == RawChunk.HEIGHT-1)
					return RawChunk.MAX_LIGHT;
				else
					actualY++;
			}
			
			return rawChunk.getSkyLight(x, actualY, z);
		}
	}
	
	public int getBlockLight(final int x, final int y, final int z)
	{
		return getBlockLight(rawChunk, x, y, z);
	}
	public static int getBlockLight(RawChunk rawChunk, final int x, final int y, final int z)
	{
		if (rawChunk == null)
			return RawChunk.MAX_LIGHT;
		else
		{
			int actualY = y;
			if (rawChunk.getBlockIdClamped(x, y, z, BlockIds.AIR) == BlockIds.SLAB)
			{
				if (y == RawChunk.HEIGHT-1)
					return RawChunk.MAX_LIGHT;
				else
					actualY++;
			}
			
			return rawChunk.getBlockLight(x, actualY, z);
		}
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

	public ArrayList<RawSign> getSigns()
	{
		if (rawChunk == null)
		{
			return new ArrayList<RawSign>();
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
					final float skyLight = getSkyLight(c, x, y, z) / (float)RawChunk.MAX_LIGHT;
					final float blockLight = getBlockLight(c, x, y, z) / (float)RawChunk.MAX_LIGHT;
					
					result = Util.clamp( skyLight * 0.7f + blockLight * 0.3f + 0.3f, 0, 1 );
				}
				
				if (face == LightFace.Top)
				{
					
				}
				else if (face == LightFace.NorthSouth)
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
				
				if (face == LightFace.Top)
				{
					
				}
				else if (face == LightFace.NorthSouth)
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
				
				if (face == LightFace.Top)
				{
					
				}
				else if (face == LightFace.NorthSouth)
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
		
		result = Util.clamp(result, 0, 1);
		
		return result;
	}
}
