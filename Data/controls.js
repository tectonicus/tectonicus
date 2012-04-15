
// The link back to the tectonicus home page
function CreateHomeControl(map)
{
	return CreateImageLink(map, 'Tectonicus home page', 'http://triangularpixels.net/games/tectonicus/', 'Images/Logo.png');
}

function CreateCustomLogoControl(map)
{
	return CreateImageLink(map, 'Custom linkage here!', 'http://www.google.com', 'Images/CustomLogo.png');
}

function CreateImageLink(map, title, destUrl, imageUrl)
{
	// Create the DIV to hold the control
	var controlDiv = document.createElement('DIV');
	controlDiv.index = 1;
	
	// Pad by a few pixels to offset from the edges of the map
	controlDiv.style.padding = '5px';
	
	// Set CSS for the control border
	var controlUI = document.createElement('DIV');
	controlUI.style.cursor = 'pointer';
	controlUI.style.textAlign = 'center';
	controlUI.title = title;
	controlDiv.appendChild(controlUI);

	var logoImage = document.createElement('IMG');
	logoImage.src = imageUrl;
	controlUI.appendChild(logoImage);
	
	// Link to the forum topic on click
	google.maps.event.addDomListener(controlUI, 'click', function()
	{
		window.location = destUrl;
	});
	
	return controlDiv;
}


function CompassControl(controlDiv, imageElement)
{
	this.controlDiv = controlDiv;
	this.imageElement = imageElement;
}
CompassControl.prototype.setCompassImage = function(imageUrl)
{
	this.imageElement.src = imageUrl
}
CompassControl.prototype.getDiv = function()
{
	return this.controlDiv;
}

function CreateCompassControl(map, initialImage)
{
	// Create the DIV to hold the control
	var controlDiv = document.createElement('DIV');
	controlDiv.index = 1;
	
	// Pad by a few pixels to offset from the edges of the map
	controlDiv.style.padding = '0px';
	
	// Set CSS for the control border
	var controlUI = document.createElement('DIV');
	controlDiv.appendChild(controlUI);

	var logoImage = document.createElement('IMG');
	logoImage.src = initialImage;
	controlUI.appendChild(logoImage);
					
	return new CompassControl(controlDiv, logoImage);
}


// Helper function to create the actual toggle box
function CreateToggleControl(text, image, startChecked)
{
	var controlDiv = document.createElement('DIV');
	controlDiv.index = 1;
	
	// Pad by a few pixels to offset from the edges of the map
//	controlDiv.style.paddingBottom = '5px';
	controlDiv.style.padding = '2px';
	controlDiv.style.height = '55px';
	controlDiv.style.width = '55px';
	
	// Set CSS for the control border
	var controlUI = document.createElement('DIV');
/*	
	controlUI.style.borderStyle = 'solid';
	controlUI.style.borderWidth = '2px';
	controlUI.style.borderColor = '#666699';
	controlUI.style.cursor = 'pointer';
	controlUI.style.textAlign = 'center';
	controlUI.style.padding = '2px';
*/
	controlUI.className += " button";
	controlUI.className += " shadow";
	
	controlDiv.appendChild(controlUI);
	controlDiv.controlUI = controlUI;
	
	var imageElement = document.createElement('IMG');
	imageElement.src = image;
	controlUI.appendChild(imageElement);
	
	var textDiv = document.createElement('DIV');
	textDiv.innerHTML = text;
	
	controlDiv.checked = startChecked;
	
	SetToggleBg(controlDiv);
	
	return controlDiv;
}

function SetToggleBg(control)
{
	if (control.checked)
		control.controlUI.style.backgroundColor = '#CCCCFF';
	else
		control.controlUI.style.backgroundColor = '#EEEEEE';
}

// Creates a toggle box to turn on and off a set of markers
function CreateMarkerToggle(map, text, image, markers, startEnabled)
{
	var control = CreateToggleControl(text, image, startEnabled);
	
	google.maps.event.addDomListener(control, 'click', function()
	{
		this.checked = !this.checked;
		
		SetToggleBg(this);
		
		for (i in markers)
		{
			var marker = markers[i];
			
			if (!this.checked)
				marker.setMap(null);
			else
				marker.setMap(map);
		}
	});
	
	return control;
}


// The little box that you click to get an url to the current view
function CreateLinkControl(map)
{
	/*
	// Create the DIV to hold the control
	var controlDiv = document.createElement('DIV');
	controlDiv.index = 1;
	
	// Pad by a few pixels to offset from the edges of the map
	controlDiv.style.padding = '5px';
	
	// Set CSS for the control border
	var controlUI = document.createElement('DIV');
	controlUI.style.backgroundColor = 'white';
	controlUI.style.borderStyle = 'solid';
	controlUI.style.borderWidth = '2px';
	controlUI.style.textAlign = 'center';
	controlUI.style.padding = '2px';
	controlDiv.appendChild(controlUI);

	// The text to show the link box
	var link = document.createElement('A');
	link.innerHTML = 'Get Link';
	link.style.cursor = 'pointer';
	controlUI.appendChild(link);
	
	var linkTextBox = document.createElement('INPUT');
	controlUI.appendChild(linkTextBox);

	// The text to hide the link box
	var hideLink = document.createElement('A');
	hideLink.innerHTML = 'Hide';
	hideLink.style.cursor = 'pointer';
	hideLink.style.display = 'none';
	controlUI.appendChild(hideLink);
	
	linkTextBox.style.display = 'none';
	var textVisible = false;
	
	google.maps.event.addDomListener(link, 'click', function()
	{
		toggleLink();
	});
	google.maps.event.addDomListener(hideLink, 'click', function()
	{
		toggleLink();
	});
	
	function toggleLink()
	{
		updateLinkUrl();
		
		if (textVisible)
		{
			// Becoming hidden
			link.style.display = '';
			hideLink.style.display = 'none';
			linkTextBox.style.display = 'none';
		}
		else
		{
			// Becoming visible
			link.style.display = 'none';
			hideLink.style.display = '';
			linkTextBox.style.display = '';
			linkTextBox.select();
		}													
		textVisible = !textVisible;
	}
	*/
	
	google.maps.event.addListener(map, 'zoom_changed',
	function()
	{
		updateLinkUrl();
	});

/*	google.maps.event.addListener(map, 'mousemove',
	function(event)
	{
		updateVisibleCoord(event.latLng);
	});
*/	
	google.maps.event.addListener(map, 'dragend',
	function(event)
	{
		updateLinkUrl();
	});
	
	google.maps.event.addListener(map, 'maptypeid_changed',
	function(event)
	{
		updateLinkUrl();
	});
	
	function updateLinkUrl()
	{
		latLong = map.getCenter();
		
		var projection = map.getProjection();
		
		mapCent = projection.fromLatLngToPoint(latLong);
		worldCent = projection.mapToWorld(mapCent);
		
		/*
		url = '';
		url += getUrlWithoutParams();
		url += '?layerId=' + map.getMapTypeId();
		url += '&worldX=' + Math.round(worldCent.x);
		url += '&worldY=' + Math.round(worldCent.y);
		url += '&worldZ=' + Math.round(worldCent.z);
		url += '&zoom=' + map.getZoom();
		
		linkTextBox.value = url;
		*/
		
		// Update the url in the browser's address bar with the new location
		// Have to pack the coords into the fragment as this is the only place we can put them
		// that won't trigger a page reload
		var fragment = '';
		fragment += 'layerId=' + map.getMapTypeId();
		fragment += '&worldX=' + Math.round(worldCent.x);
		fragment += '&worldY=' + Math.round(worldCent.y);
		fragment += '&worldZ=' + Math.round(worldCent.z);
		fragment += '&zoom=' + map.getZoom();
		window.location.hash = fragment;
	}
	
	/*
	function updateVisibleCoord(latLong)
	{
		var projection = map.getProjection();
		
		mapCent = projection.fromLatLngToPoint(latLong);
		worldCent = projection.mapToWorld(mapCent);
		
		x = Math.round(worldCent.x);
		y = Math.round(worldCent.y);
		z = Math.round(worldCent.z);
		
		link.innerHTML = 'Get link to ('+x+', '+y+', '+z+')';				
	}
	
	return controlDiv;
	*/
}