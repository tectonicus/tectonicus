
// The link back to the tectonicus home page
//function CreateHomeControl(map)
//{
//	return CreateImageLink(map, 'Tectonicus home page', 'http://triangularpixels.net/games/tectonicus/', 'Images/Logo.png');
//}
//
//function CreateCustomLogoControl(map)
//{
//	return CreateImageLink(map, 'Custom linkage here!', 'http://www.google.com', 'Images/CustomLogo.png');
//}
//
//function CreateImageLink(map, title, destUrl, imageUrl)
//{
//	// Create the DIV to hold the control
//	var controlDiv = document.createElement('DIV');
//	controlDiv.index = 1;
//
//	// Pad by a few pixels to offset from the edges of the map
//	controlDiv.style.padding = '5px';
//
//	// Set CSS for the control border
//	var controlUI = document.createElement('DIV');
//	controlUI.style.cursor = 'pointer';
//	controlUI.style.textAlign = 'center';
//	controlUI.title = title;
//	controlDiv.appendChild(controlUI);
//
//	var logoImage = document.createElement('IMG');
//	logoImage.src = imageUrl;
//	controlUI.appendChild(logoImage);
//
//	// Link to the forum topic on click
//	google.maps.event.addDomListener(controlUI, 'click', function()
//	{
//		window.location = destUrl;
//	});
//
//	return controlDiv;
//}

function CreateCompassControl(initialImage) {
	var compassControl = L.Control.extend({
		options: {
			position: 'topleft'
		},

		onAdd: function (map) {
			var container = L.DomUtil.create('div', 'compass leaflet-control leaflet-control-custom');
			container.style.background = "url(" + initialImage + ") center";
			container.style.backgroundSize = "128px 128px";
			container.style.width = '100px';
			container.style.height = '80px';

			return container;
		}
	});

	return new compassControl();
}

// Creates a toggle box to turn on and off a set of markers
function CreateToggleControl(text, image, markers, startEnabled) {
	let toggleControl = L.Control.extend({
		options: {
			position: 'topright'
		},

		onAdd: function (map) {
			let container = L.DomUtil.create('input', 'leaflet-bar leaflet-control leaflet-control-custom button');
			container.type = "image";
			container.src = image;
			container.title = text;
			container.style.padding = '5px';
			container.style.width = '34px';
			container.style.height = '34px';
			container.checked = startEnabled;
			SetToggleBg(container);

			container.onclick = function () {
				this.checked = !this.checked;

				SetToggleBg(this);

				for (i in markers) {
					var marker = markers[i];

					if (!this.checked)
						marker.remove();
					else
						marker.addTo(mymap);
				}

				if (this.checked) {
                    this._tippy.setContent('Hide ' + text);
                } else {
                    this._tippy.setContent('Show ' + text);
                }
			}

			L.DomEvent.disableClickPropagation(container);

			return container;
		}
	});

	return new toggleControl();
}

function SetToggleBg(control) {
	if (control.checked)
		control.style.backgroundColor = '#CCCCFF';
	else
		control.style.backgroundColor = '#FFFFFF';
}


// The little box that you click to get an url to the current view
//function CreateLinkControl(map)
//{
//	/*
//	// Create the DIV to hold the control
//	var controlDiv = document.createElement('DIV');
//	controlDiv.index = 1;
//
//	// Pad by a few pixels to offset from the edges of the map
//	controlDiv.style.padding = '5px';
//
//	// Set CSS for the control border
//	var controlUI = document.createElement('DIV');
//	controlUI.style.backgroundColor = 'white';
//	controlUI.style.borderStyle = 'solid';
//	controlUI.style.borderWidth = '2px';
//	controlUI.style.textAlign = 'center';
//	controlUI.style.padding = '2px';
//	controlDiv.appendChild(controlUI);
//
//	// The text to show the link box
//	var link = document.createElement('A');
//	link.innerHTML = 'Get Link';
//	link.style.cursor = 'pointer';
//	controlUI.appendChild(link);
//
//	var linkTextBox = document.createElement('INPUT');
//	controlUI.appendChild(linkTextBox);
//
//	// The text to hide the link box
//	var hideLink = document.createElement('A');
//	hideLink.innerHTML = 'Hide';
//	hideLink.style.cursor = 'pointer';
//	hideLink.style.display = 'none';
//	controlUI.appendChild(hideLink);
//
//	linkTextBox.style.display = 'none';
//	var textVisible = false;
//
//	google.maps.event.addDomListener(link, 'click', function()
//	{
//		toggleLink();
//	});
//	google.maps.event.addDomListener(hideLink, 'click', function()
//	{
//		toggleLink();
//	});
//
//	function toggleLink()
//	{
//		updateLinkUrl();
//
//		if (textVisible)
//		{
//			// Becoming hidden
//			link.style.display = '';
//			hideLink.style.display = 'none';
//			linkTextBox.style.display = 'none';
//		}
//		else
//		{
//			// Becoming visible
//			link.style.display = 'none';
//			hideLink.style.display = '';
//			linkTextBox.style.display = '';
//			linkTextBox.select();
//		}
//		textVisible = !textVisible;
//	}
//	*/
//
//	google.maps.event.addListener(map, 'zoom_changed',
//	function()
//	{
//		updateLinkUrl();
//	});
//
///*	google.maps.event.addListener(map, 'mousemove',
//	function(event)
//	{
//		updateVisibleCoord(event.latLng);
//	});
//*/
//	google.maps.event.addListener(map, 'dragend',
//	function(event)
//	{
//		updateLinkUrl();
//	});
//
//	google.maps.event.addListener(map, 'maptypeid_changed',
//	function(event)
//	{
//		updateLinkUrl();
//	});
//
//	function updateLinkUrl()
//	{
//		latLong = map.getCenter();
//
//		var projection = map.getProjection();
//
//		mapCent = projection.fromLatLngToPoint(latLong);
//		worldCent = projection.mapToWorld(mapCent);
//
//		/*
//		url = '';
//		url += getUrlWithoutParams();
//		url += '?layerId=' + map.getMapTypeId();
//		url += '&worldX=' + Math.round(worldCent.x);
//		url += '&worldY=' + Math.round(worldCent.y);
//		url += '&worldZ=' + Math.round(worldCent.z);
//		url += '&zoom=' + map.getZoom();
//
//		linkTextBox.value = url;
//		*/
//
//		// Update the url in the browser's address bar with the new location
//		// Have to pack the coords into the fragment as this is the only place we can put them
//		// that won't trigger a page reload
//		var fragment = '';
//		fragment += 'layerId=' + map.getMapTypeId();
//		fragment += '&worldX=' + Math.round(worldCent.x);
//		fragment += '&worldY=' + Math.round(worldCent.y);
//		fragment += '&worldZ=' + Math.round(worldCent.z);
//		fragment += '&zoom=' + map.getZoom();
//		window.location.hash = fragment;
//	}
//
//	/*
//	function updateVisibleCoord(latLong)
//	{
//		var projection = map.getProjection();
//
//		mapCent = projection.fromLatLngToPoint(latLong);
//		worldCent = projection.mapToWorld(mapCent);
//
//		x = Math.round(worldCent.x);
//		y = Math.round(worldCent.y);
//		z = Math.round(worldCent.z);
//
//		link.innerHTML = 'Get link to ('+x+', '+y+', '+z+')';
//	}
//
//	return controlDiv;
//	*/
//}
