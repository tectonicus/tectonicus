
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

function CreateToggleMenuControl() {
	const ToggleMenu = L.Control.extend({
		options: {position: 'topright'},

		onAdd: function(map) {
			const container = L.DomUtil.create('div', 'leaflet-control-layers toggle-menu');
			const button = L.DomUtil.create('button', 'leaflet-control-layers-toggle toggle-menu-button', container);
			button.type = 'button';
			button.title = 'Marker toggles';
			button.setAttribute('aria-label', 'Marker toggles');
			button.setAttribute('aria-expanded', 'false');
			const icon = L.DomUtil.create('span', 'toggle-menu-icon', button);
			icon.setAttribute('aria-hidden', 'true');
			const panel = L.DomUtil.create('div', 'toggle-menu-panel', container);
			panel.hidden = true;
			this._items = L.DomUtil.create('div', 'toggle-menu-items', panel);
			this._toggles = [];
			this._pinned = false;
			this._setExpanded = expanded => {
				panel.hidden = !expanded;
				button.setAttribute('aria-expanded', String(expanded));
			};
			this._close = () => {
				this._pinned = false;
				this._setExpanded(false);
			};
			container.onpointerenter = event => {
				if (event.pointerType === 'mouse') this._setExpanded(true);
			};
			container.onpointerleave = () => {
				if (!this._pinned) this._setExpanded(false);
			};
			button.onclick = () => {
				this._pinned = !this._pinned;
				this._setExpanded(this._pinned);
			};
			container.onkeydown = event => {
				if (event.key === 'Escape') {
					this._close();
					button.focus();
				}
			};
			this._onMapClick = event => {
				if (!container.contains(event.target)) this._close();
			};
			// Capture clicks on markers too, even when their handlers stop propagation.
			map.getContainer().addEventListener('click', this._onMapClick, true);
			L.DomEvent.disableClickPropagation(container);
			L.DomEvent.disableScrollPropagation(container);
			return container;
		},

		setToggles: function(toggles) {
			for (const toggle of this._toggles) toggle.remove();
			this._toggles = toggles.map(([toggle, checked]) => {
				toggle.addTo(this._map);
				toggle.setChecked(checked);
				this._items.appendChild(toggle.getContainer());
				return toggle;
			});
			this._items.style.setProperty('--toggle-columns', Math.max(1, Math.min(3, toggles.length)));
			this._container.style.display = toggles.length ? '' : 'none';
			if (!toggles.length) this._close();
		},

		onRemove: function(map) {
			map.getContainer().removeEventListener('click', this._onMapClick, true);
			for (const toggle of this._toggles) toggle.remove();
		}
	});
	return new ToggleMenu();
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
			container.alt = text;
			container.setAttribute('aria-label', 'Toggle ' + text);
			container.checked = startEnabled;

			container.onclick = function () {
				this.checked = !this.checked;
                if (stateKey) {
                    setMarkerVisibility(stateKey, this.checked);
                }

				SetToggleBg(this);

				for (const marker of markers) {
					if (!this.checked)
						marker.remove();
					else
						marker.addTo(map);
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
		onRemove: function() {
			if (this._container._tippy) this._container._tippy.destroy();
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
	control.setAttribute('aria-pressed', String(control.checked));
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
