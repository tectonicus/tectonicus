

// members:
//	int lattitudeRange
//	WorldVectors worldVectors

function MinecraftProjection(lattRange, worldVecs)
{
	this.lattitudeRange = lattRange;
	this.worldVectors = worldVecs;
};

MinecraftProjection.prototype.fromLatLngToPoint = function(latLng, opt_point)
{
	// Convert from lat-long to map coord
	
	var point = opt_point || new google.maps.Point(latLng.lat(), latLng.lng());
	
	// from lat-long to normalised (0, 1) coord
	point.x = (point.x / lattitudeRange) + 0.5;
	point.y = (point.y / 180) + 0.5;
	
	// from normalised to map coord
	point.x = point.x * this.worldVectors.mapSize.x + this.worldVectors.mapMin.x;
	point.y = point.y * this.worldVectors.mapSize.y + this.worldVectors.mapMin.y;
	
	return point;
};

// Need to map from tile coords to lat-long
// longitude is x and ranges from -180 through 0 (Greenwich) to +180
// lattitude is y and ranges from -90 (north pole) through 0 (equator) to +90 (south pole)
// ie. min lat-long is (-180, -90)
//     max lat-long is (+180, +90)

MinecraftProjection.prototype.fromPointToLatLng = function(point)
{
	// Convert from map coord to lat-long
	
	// from map coord to normalised (0, 1)
//	point.x = (point.x - mapXMin) / mapWidth;
	point.x = (point.x - this.worldVectors.mapMin.x) / this.worldVectors.mapSize.x;
//	point.y = (point.y - mapYMin) / mapHeight;
	point.y = (point.y - this.worldVectors.mapMin.y) / this.worldVectors.mapSize.y;
	
	// from normalised to lat-long
	point.x = point.x * this.lattitudeRange - (this.lattitudeRange/2);
	point.y = point.y * 180 - 90;
	
	return new google.maps.LatLng(point.x, point.y, true);
};

// Converts from minecraft world coord to map pixel coords
MinecraftProjection.prototype.worldToMap = function(world)
{
	var point = new google.maps.Point(this.worldVectors.origin.x, this.worldVectors.origin.y);
	
	point.x += this.worldVectors.xAxis.x * world.x;
	point.y += this.worldVectors.xAxis.y * world.x;
			
	point.x += this.worldVectors.yAxis.x * world.y;
	point.y += this.worldVectors.yAxis.y * world.y;
			
	point.x += this.worldVectors.zAxis.x * world.z;
	point.y += this.worldVectors.zAxis.y * world.z;
	
	return point;
}

MinecraftProjection.prototype.mapToWorld = function(point)
{
	var world = new WorldCoord(0, 0, 0);
	
	adjusted = new google.maps.Point(point.x - this.worldVectors.origin.x, point.y - this.worldVectors.origin.y);
	
	world.x += (this.worldVectors.mapXUnit.x * adjusted.x);
	world.z += (this.worldVectors.mapXUnit.y * adjusted.x);

	var xx = this.worldVectors.mapYUnit.x * adjusted.y;
	var zz = this.worldVectors.mapYUnit.y * adjusted.y;				
	world.x += xx*2; // hmmm....
	world.z += zz*2;
	
	return world;
}

