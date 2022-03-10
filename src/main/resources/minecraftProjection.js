// members:
//	WorldVectors worldVectors

function MinecraftProjection(worldVecs) {
	this.worldVectors = worldVecs;
};

MinecraftProjection.prototype.worldToMap = function (world) {
	let point = new L.Point(this.worldVectors.origin.x, this.worldVectors.origin.y);

	point.x += this.worldVectors.xAxis.x * world.x;
	point.y += this.worldVectors.xAxis.y * world.x;

	if (this.worldVectors.yOffset > 0) {
        point.x += this.worldVectors.yAxis.x * (world.y + this.worldVectors.yOffset);
        point.y += this.worldVectors.yAxis.y * (world.y + this.worldVectors.yOffset);
    } else {
        point.x += this.worldVectors.yAxis.x * world.y;
        point.y += this.worldVectors.yAxis.y * world.y;
    }

	point.x += this.worldVectors.zAxis.x * world.z;
	point.y += this.worldVectors.zAxis.y * world.z;

	return [point.y / -2, point.x / 2];
}

//TODO: This still needs to be fixed
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

