
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

function CreateResetViewButton(map) {
    const zoomContainer = map.zoomControl.getContainer();
    const container = L.DomUtil.create('div', 'leaflet-bar leaflet-control leaflet-control-reset', zoomContainer.parentNode);
    // Anchor a separate toolbar below zoom without moving the compass beside it.
    container.style.top = (zoomContainer.offsetTop + zoomContainer.offsetHeight) + 'px';
    const button = L.DomUtil.create('a', 'leaflet-control-reset-view', container);
    button.href = '#';
    button.title = 'Reset Map View';
    button.setAttribute('role', 'button');
    button.setAttribute('aria-label', 'Reset Map View');
    button.onclick = function (event) {
        L.DomEvent.preventDefault(event);
        const layer = activeBaseLayer;
        map.setView(layer.projection.worldToMap(layer.worldVectors.startView), 0, {animate: false});
    };
    L.DomEvent.disableClickPropagation(button);
    return button;
}

// Creates a toggle box to turn on and off a set of markers
function CreateToggleControl(text, image, markers, startEnabled, stateKey) {
	let toggleControl = L.Control.extend({
		options: {
			position: 'topright'
		},

		onAdd: function (map) {
			let container = L.DomUtil.create('input', 'leaflet-bar leaflet-control leaflet-control-custom button');
			container.type = "image";
			container.src = worldImageUrl(image);
			container.title = text;
			container.style.padding = '5px';
			container.style.width = '34px';
			container.style.height = '34px';
			container.checked = startEnabled;

			container.onclick = function () {
				this.checked = !this.checked;
                if (stateKey) {
                    setMarkerVisibility(stateKey, this.checked);
                }

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
    fragment += 'layer=' + encodeURIComponent(currentLayer.urlId);
    fragment += '&x=' + Math.round(worldCent.x);
    fragment += '&z=' + Math.round(worldCent.z);
    fragment += '&zoom=' + mymap.getZoom();
    window.history.replaceState(window.history.state, '', '#' + fragment);
}

// A world selector is created only when at least two saves are configured.
function CreateWorldControl(worlds, selectedWorldId, onChange) {
    const WorldControl = L.Control.extend({
        options: {position: 'topright'},
        onAdd: function() {
            const container = L.DomUtil.create('div', 'leaflet-bar world-control');
            const label = L.DomUtil.create('label', '', container);
            label.textContent = 'World ';
            const select = L.DomUtil.create('select', '', label);
            select.setAttribute('aria-label', 'World');
            for (const [id, layers] of worlds) {
                const option = L.DomUtil.create('option', '', select);
                option.value = id;
                option.textContent = layers[0].worldName;
            }
            select.value = selectedWorldId;
            select.onchange = () => onChange(select.value);
            this.select = select;
            L.DomEvent.disableClickPropagation(container);
            L.DomEvent.disableScrollPropagation(container);
            return container;
        },
        setWorld: function(id) {
            this.select.value = id;
        }
    });
    return new WorldControl();
}
