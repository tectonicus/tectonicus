
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

			container.onclick = function () { //Reload the map with default starting view
                window.location = window.location.href.split('?')[0].split('#')[0];
            }

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
		},
		setChecked: function(checked) {
			let container = this._container;
			container.checked = checked;
			SetToggleBg(container);
		}
	});

	return new toggleControl();
}

function SetToggleBg(control) {
	if (control.checked)
		control.style.backgroundColor = '#FFFFFF';
	else
		control.style.backgroundColor = '#CCCCFF';
}

function updateLinkUrl(currentLayer) {
    let worldCent = currentLayer.projection.mapToWorld(mymap.getCenter());

    // Update the current URL without reloading or adding browser history entries.
    let fragment = '';
    fragment += 'layer=' + currentLayer.layerId;
    fragment += '&x=' + Math.round(worldCent.x);
    fragment += '&z=' + Math.round(worldCent.z);
    fragment += '&zoom=' + mymap.getZoom();
    window.history.replaceState(window.history.state, '', '#' + fragment);
}
