//adapted from the LoessInterpolator in org.apache.commons.math
function loess_pairs(pairs, bandwidth)
{
	if (!bandwidth) bandwidth = 0.5;
	
	var xval = pairs.map(function(pair){return pair[0]});
	var yval = pairs.map(function(pair){return pair[1]});
	var res = science.stats.loess().bandwidth(bandwidth)(xval, yval);
	return xval.map(function(x,i){return [x, res[i]]});
}
