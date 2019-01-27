function main() {
	var mymap = L.map('map_canvas', {crs: L.CRS.Simple, minZoom: 0, maxZoom: 5}).setView([100, -100], 0);
	
	L.TileLayer.Tectonicus = L.TileLayer.extend({
		getTileUrl: function(coords) {			
			var xBin = coords.x % 16;
			var yBin = coords.y % 16;
			
			var url = "Map0/LayerA/Zoom"+coords.z+"/"+xBin+"/"+yBin+"/tile_"+coords.x+"_"+coords.y+"."+"png";
			return url;
		},
		getAttribution: function() {
			return 'Tectonicus';
		}
	});
	
	L.tileLayer.tectonicus = function() {
		return new L.TileLayer.Tectonicus();
	}
	
	L.tileLayer.tectonicus().addTo(mymap);
}

window.onload = main;