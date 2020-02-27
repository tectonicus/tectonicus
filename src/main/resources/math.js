
			
function WorldCoord(xx, yy, zz)
{
	this.x = xx;
	this.y = yy;
	this.z = zz;
}
	
function dot(x0, y0, x1, y1)
{
	return x0 * x1 + y0 * y1;
}

function dot(lhs, rhs)
{
	return lhs.x * rhs.x + lhs.y * rhs.y;
}

function length(vec)
{
	return Math.sqrt(dot(vec, vec));
}	

function normalise(vec)
{
	var len = length(vec);
	var out = new google.maps.Point(vec.x/len, vec.y/len);
	return out;
}