// members:
//	WorldVectors worldVectors

// World vectors are in rendered tile pixels; Leaflet draws every tile 256 units wide.
// contents.js sets tileSize, which older outputs may lack (they always used 512).
function tilePixelScale() {
	const size = typeof tileSize === 'number' && tileSize > 0 ? tileSize : 512;
	return size / 256;
}

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

	const scale = tilePixelScale();
	return [point.y / -scale, point.x / scale];
}

// Leaflet coordinates for a world position. The projection is affine, so straight
// world segments stay straight and can be drawn by projecting only their ends.
MinecraftProjection.prototype.worldToLatLng = function (world) {
	return L.latLng(this.worldToMap(world));
}

// A map position has no height. Pass the block height to solve for; the default
// is the start view height, which share links also use.
MinecraftProjection.prototype.mapToWorld = function(latLng, y) {
    const scale = tilePixelScale();
    let point = new L.Point(latLng.lng * scale, latLng.lat * -scale);

    point.x -= this.worldVectors.origin.x;
    point.y -= this.worldVectors.origin.y;

    // It is not possible to determine altitude only from longitude and latitude,
    // so let's get approximate coordinates using startView y value
    let worldY = Number.isFinite(y) ? y : this.worldVectors.startView.y;
    let offsetWorldY = worldY; //We use the offset y value for calculating the approximate x and z values

    if (this.worldVectors.yOffset > 0) {
            offsetWorldY += this.worldVectors.yOffset;
    }

    point.x -= this.worldVectors.yAxis.x * offsetWorldY;
    point.y -= this.worldVectors.yAxis.y * offsetWorldY;

    let worldX =
            (point.x * this.worldVectors.zAxis.y - point.y * this.worldVectors.zAxis.x) /
            (this.worldVectors.xAxis.x * this.worldVectors.zAxis.y - this.worldVectors.zAxis.x * this.worldVectors.xAxis.y);
    let worldZ =
            (point.x * this.worldVectors.xAxis.y - point.y * this.worldVectors.xAxis.x) /
            (this.worldVectors.zAxis.x * this.worldVectors.xAxis.y - this.worldVectors.xAxis.x * this.worldVectors.zAxis.y);

    return new WorldCoord(worldX, worldY, worldZ);
}

