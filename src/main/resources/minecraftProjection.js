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

MinecraftProjection.prototype.mapToWorld = function(map) {
        let point = new L.Point(map.y * 2, map.x * -2);

        point.x -= this.worldVectors.origin.x;
        point.y -= this.worldVectors.origin.y;

        // It is not possible to determine altitude only from longitude and latitude,
        // so let's get approximate coordinates and assume sea level
        let worldY = 62;

        if (this.worldVectors.yOffset > 0) {
                worldY += this.worldVectors.yOffset;
        }

        point.x -= this.worldVectors.yAxis.x * worldY;
        point.y -= this.worldVectors.yAxis.y * worldY;

        let worldX =
                (point.x * this.worldVectors.zAxis.y - point.y * this.worldVectors.zAxis.x) /
                (this.worldVectors.xAxis.x * this.worldVectors.zAxis.y - this.worldVectors.zAxis.x * this.worldVectors.xAxis.y);
        let worldZ =
                (point.x * this.worldVectors.xAxis.y - point.y * this.worldVectors.xAxis.x) /
                (this.worldVectors.zAxis.x * this.worldVectors.xAxis.y - this.worldVectors.xAxis.x * this.worldVectors.zAxis.y);

        return new WorldCoord(worldX, worldY, worldZ);
}

