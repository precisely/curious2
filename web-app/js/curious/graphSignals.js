$(function() {
	var startDate = 1392376590540;
	var stopDate = 1402854689160;



	description1 = $('#description1').val();
	description2 = $('#description2').val();
	getUIFromDescription = function(description) {
		return $('.treeItemView').filter(function(i, elt) { return $(elt).text() == description });
	};

	// Convert a number x, 0 < x < 1, into a string percent.
	var perc = function(x) {
		return "" + 100*x + "%";
	};

	var perc2 = function(x) {
		return perc(1-x);
	};

	var shiftPlot = function(xStartMs, xStopMs) {
				plot.plotOptions['xaxis']['min'] = xStartMs;
				plot.plotOptions['xaxis']['max'] = xStopMs;
				plot.drawPlot()

				// Update UI slider postions.
				var span = (plot.maxTime - plot.minTime);
				var startPercent = perc((xStartMs - plot.minTime) / span);
				var stopPercent = perc((xStopMs - plot.minTime)/ span);
				var widthPercent = perc((xStopMs - xStartMs) / span);
				$('.ui-slider-range.ui-widget-header').css({left: startPercent, width: widthPercent})
				$('#zoomcontrol1 span').first().css({'left': startPercent})
				$('#zoomcontrol1 span').last().css({'left': startPercent})
	};

	plot = null;
	var tick = 50, drewLine = false;
	var drawPlot = function() {
		elt1 = getUIFromDescription(description1);
		if (elt1.data(DATA_KEY_FOR_ITEM_VIEW) == undefined) {
			return;
		}
		elt2 = getUIFromDescription(description2);
		var plot = Curious.getPlot();
		if (plot) {
			if (!drewLine) {
				plot.drawLine(elt1);
				plot.drawLine(elt2);
				drewLine = true;
			}
			if (plot.plotOptions) {
				shiftPlot(startDate, stopDate);
				clearInterval(plotReady);
			}
		}
	};
	var plotReady = setInterval(drawPlot, 50);
});
