var vm = new Vue({
	el: "#map_canvas",
	data: {
		map: null,
		tileLayers: [],
		bgcolor: null
	},
	mounted: function() {
		this.initMap();
	},
	methods: {
		initMap: function() {
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
					this.tileLayers.push(tileLayer);
				}
			}

			this.map = L.map('map_canvas', {crs: L.CRS.Simple, minZoom: 0, maxZoom: maxZoom}).setView([100, -100], 0);
			this.bgcolor = this.tileLayers[0].backgroundColor;
			this.tileLayers[0].addTo(this.map);

			if (this.tileLayers.length > 1) {
				L.control.layers(baseMaps).addTo(this.map);
			}

			this.map.on('baselayerchange', this.onBaseLayerChange);
		},
		onBaseLayerChange: function(e) {
			this.bgcolor = e.layer.backgroundColor;
		}
	}
});
