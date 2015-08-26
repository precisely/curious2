$(function() {

	description1 = $('#description1').val();
	description2 = $('#description2').val();
	getUIFromDescription = function(description) {
		return $('.treeItemView').filter(function(i, elt) { return $(elt).text() == description });
	};

	var plot = null;
	var plotReady = null;
	function drawPlot() {
		console.log("plot ready?");
		elt1 = getUIFromDescription(description1);
		if (elt1.data(DATA_KEY_FOR_ITEM_VIEW) == undefined) {
			plotReady = setInterval(drawPlot, 1000);
			return;
		}
		elt2 = getUIFromDescription(description2);
		if (WeAreCurious.getPlot()) {
			plot = WeAreCurious.getPlot();
			clearInterval(plotReady);

			plot.drawLine(elt1);
			plot.drawLine(elt2);
		}
	}
	var plotReady = setInterval(drawPlot, 1000);
});
