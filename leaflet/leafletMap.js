function main() {
	L.TileLayer.Tectonicus = L.TileLayer.extend({
		getTileUrl: function(coords) {			
			var xBin = coords.x % 16;
			var yBin = coords.y % 16;
			
			return this.mapId + "/" + this.layerId + "/Zoom"+coords.z
				+"/"+xBin+"/"+yBin+"/tile_"+coords.x+"_"+coords.y+"."+this.imageFormat;
		},
		getAttribution: function() {
			return '<a href="https://github.com/tectonicus/tectonicus">Tectonicus</a> - ' + this.mapName;
		},
		initialize: function(mapId, layerId, imageFormat, mapName) {
			this.mapId = mapId;
			this.layerId = layerId;
			this.imageFormat = imageFormat;
			this.mapName = mapName;
		}
	});
	
	var baseMaps = {}
	var layers = [];
	for (var i = 0; i < contents.length; i++) {
		var tecMap = contents[i];
		for (var j = 0; j < tecMap.layers.length; j++) {
			var layer = tecMap.layers[j];
			var tileLayer = new L.TileLayer.Tectonicus(tecMap.id, layer.id, layer.imageFormat, tecMap.name);
			if (baseMaps.hasOwnProperty(tecMap.name + " - " + layer.name)) {
				baseMaps[tecMap.name + " - " + layer.name + j] = tileLayer;  //A hack to handle duplicate layer names in the layer control
			} else {
				baseMaps[tecMap.name + " - " + layer.name] = tileLayer;
			}
			layers.push(tileLayer);
		}
	}

	var map = L.map('map_canvas', {crs: L.CRS.Simple, minZoom: 0, maxZoom: maxZoom}).setView([100, -100], 0);
	layers[0].addTo(map);
	
	if (layers.length > 1) {
		L.control.layers(baseMaps).addTo(map);
	}
}

window.onload = main;