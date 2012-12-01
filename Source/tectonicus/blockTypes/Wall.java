package tectonicus.blockTypes;

import org.lwjgl.util.vector.Vector4f;

import tectonicus.BlockContext;
import tectonicus.BlockIds;
import tectonicus.BlockType;
import tectonicus.BlockTypeRegistry;
import tectonicus.configuration.LightFace;
import tectonicus.rasteriser.Mesh;
import tectonicus.raw.RawChunk;
import tectonicus.renderer.Geometry;
import tectonicus.texture.SubTexture;

public class Wall implements BlockType
{
	private final String name;
	private final int blockId;
	private final SubTexture texture0;
	private final SubTexture texture1;

	public Wall(String name, final int blockId, SubTexture texture0, SubTexture texture1)
	{
		this.name = name;
		this.blockId = blockId;
		this.texture0 = texture0;
		this.texture1 = texture1;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public boolean isSolid()
	{
		return false;
	}

	@Override
	public boolean isWater()
	{
		return false;
	}

	@Override
	public void addInteriorGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		addEdgeGeometry(x, y, z, world, registry, rawChunk, geometry);
	}

	@Override
	public void addEdgeGeometry(int x, int y, int z, BlockContext world, BlockTypeRegistry registry, RawChunk rawChunk, Geometry geometry)
	{
		SubTexture texture;
		final int data = rawChunk.getBlockData(x, y, z);
		if (data == 0)
		{
			texture = texture0;
		}
		else
		{
			texture = texture1;
		}
		
		Mesh mesh = geometry.getMesh(texture.texture, Geometry.MeshType.Solid);
		
		Vector4f colour = new Vector4f(1, 1, 1, 1);
		
		final float topLight = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.Top);
		final float northSouthLight = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.NorthSouth);
		final float eastWestLight = world.getLight(rawChunk.getChunkCoord(), x, y, z, LightFace.EastWest);
		
		final int aboveId = world.getBlockId(rawChunk.getChunkCoord(), x, y+1, z);
		final int northId = world.getBlockId(rawChunk.getChunkCoord(), x, y, z+1);
		final int southId = world.getBlockId(rawChunk.getChunkCoord(), x, y, z-1);
		final int eastId = world.getBlockId(rawChunk.getChunkCoord(), x+1, y, z);
		final int westId = world.getBlockId(rawChunk.getChunkCoord(), x-1, y, z);
		
		if ((northId == blockId && southId == blockId) && (eastId != blockId && westId != blockId && aboveId != blockId))
		{
			BlockUtil.addBlock(mesh, x, y, z, 	5, 0, 0, 
												6, 13, 16, colour, texture, topLight, northSouthLight, eastWestLight);
		}
		else if ((eastId == blockId && westId == blockId) && (northId != blockId && southId != blockId && aboveId != blockId))
		{
			BlockUtil.addBlock(mesh, x, y, z, 	0, 0, 5,
												16, 13, 6, colour, texture, topLight, northSouthLight, eastWestLight);
		}
		else
		{
			// Center column
			BlockUtil.addBlock(mesh, x, y, z, 4, 0, 4, 8, 16, 8, colour, texture, topLight, northSouthLight, eastWestLight);
			
			// North		
			if (northId == blockId || northId == BlockIds.FENCE_GATE)
			{
				BlockUtil.addBlock(mesh, x, y, z,	5, 0, 12,
													6, 13, 4, colour, texture, topLight, northSouthLight, eastWestLight);
			}
			
			// South		
			if (southId == blockId || southId == BlockIds.FENCE_GATE)
			{
				BlockUtil.addBlock(mesh, x, y, z,	5, 0, 0,
													6, 13, 4, colour, texture, topLight, northSouthLight, eastWestLight);
			}
			
			// East		
			if (eastId == blockId || eastId == BlockIds.FENCE_GATE)
			{
				BlockUtil.addBlock(mesh, x, y, z,	12, 0, 5,
													4, 13, 6, colour, texture, topLight, northSouthLight, eastWestLight);
			}
			
			// West
			if (westId == blockId || westId == BlockIds.FENCE_GATE)
			{
				BlockUtil.addBlock(mesh, x, y, z,	0, 0, 5,
													4, 13, 6, colour, texture, topLight, northSouthLight, eastWestLight);
			}
		}
	}

}
