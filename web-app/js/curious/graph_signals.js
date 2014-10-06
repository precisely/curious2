$(function() {

	description1 = $('#description1').val();
	description2 = $('#description2').val();
	getUIFromDescription = function(description) {
		return $('.treeItemView').filter(function(i, elt) { return $(elt).text() == description });
	};

	var plot = null;
	var plotReady = setInterval(function() {
		console.log("plot ready?");
		elt1 = getUIFromDescription(description1);
		elt2 = getUIFromDescription(description2);
		if (Curious.getPlot()) {
			plot = Curious.getPlot();
			clearInterval(plotReady);

			plot.drawLine(elt1);
			plot.drawLine(elt2);
		}
	}, 1000);

});
