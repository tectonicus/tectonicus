package tectonicus.world;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import tectonicus.cache.swap.Swappable;
import tectonicus.raw.TileEntity;
import tectonicus.util.Vector3l;

public class Chest implements Swappable
{
	private Vector3l position;
	
	public Chest()
	{
		position = new Vector3l();
	}
	
	public Chest(TileEntity entity)
	{
		position = new Vector3l(entity.x, entity.y, entity.z);
	}

	public long getX()
	{
		return position.x;
	}
	public long getY()
	{
		return position.y;
	}
	public long getZ()
	{
		return position.z;
	}
	
	@Override
	public void readFrom(DataInputStream source) throws Exception
	{
		position.x = source.readLong();
		position.y = source.readLong();
		position.z = source.readLong();
	}

	@Override
	public void writeTo(DataOutputStream dest) throws Exception
	{
		dest.writeLong(position.x);
		dest.writeLong(position.y);
		dest.writeLong(position.z);
	}
}
