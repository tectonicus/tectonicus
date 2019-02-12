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
		initialize: function(mapId, layerId, imageFormat, mapName, backgroundColor) {
			this.mapId = mapId;
			this.layerId = layerId;
			this.imageFormat = imageFormat;
			this.mapName = mapName;
			this.backgroundColor = backgroundColor;
		}
	});
	
	var baseMaps = {}
	var layers = [];
	for (var i = 0; i < contents.length; i++) {
		var tecMap = contents[i];
		for (var j = 0; j < tecMap.layers.length; j++) {
			var layer = tecMap.layers[j];
			var tileLayer = new L.TileLayer.Tectonicus(tecMap.id, layer.id, layer.imageFormat, tecMap.name, layer.backgroundColor);
			if (baseMaps.hasOwnProperty(tecMap.name + " - " + layer.name)) {
				baseMaps[tecMap.name + " - " + layer.name + j] = tileLayer;  //A hack to handle duplicate layer names in the layer control
			} else {
				baseMaps[tecMap.name + " - " + layer.name] = tileLayer;
			}
			layers.push(tileLayer);
		}
	}

	var map = L.map('map_canvas', {crs: L.CRS.Simple, minZoom: 0, maxZoom: maxZoom}).setView([100, -100], 0);
	$("#map_canvas").css({"background-color" : layers[0].backgroundColor});
	layers[0].addTo(map);
	
	if (layers.length > 1) {
		L.control.layers(baseMaps).addTo(map);
	}
	
	var onBaseLayerChange = function(e) {
		$("#map_canvas").css({"background-color" : e.layer.backgroundColor});
	}
	map.on('baselayerchange', onBaseLayerChange);
}

window.onload = main;
