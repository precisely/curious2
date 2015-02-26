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
		var startMs = parseInt(xStartMs);
		var stopMs = parseInt(xStopMs);
		plot.plotOptions['xaxis']['min'] = startMs;
		plot.plotOptions['xaxis']['max'] = stopMs;
		plot.drawPlot()

		// Update UI slider postions.
		var span = (plot.maxTime - plot.minTime);
		var startPercent = perc((startMs - plot.minTime) / span);
		var stopPercent = perc((stopMs - plot.minTime)/ span);
		var widthPercent = perc((stopMs - startMs) / span);
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
				appendOption({
					id: -1,
					startMs: plot.plotOptions['xaxis']['min'],
					stopMs:  plot.plotOptions['xaxis']['max']});
				getIntervals();
				clearInterval(plotReady);
			}
		}
	};

	var intervalOptionTemplate = $('#interval-option-template').html();
	Mustache.parse(intervalOptionTemplate);

	var formatDate = function(d) {
		return '' + (d.getMonth() + 1) + '/' + d.getDate() + '/' + d.getFullYear();
	};

	var appendOption = function(d) {
		var id = d.id;
		var stopMs = d.stopMs;
		var startMs = d.startMs;
		var startDate = new Date(startMs);
		var stopDate = new Date(stopMs);
		var newOption = Mustache.render(intervalOptionTemplate,
			{ id: id,
				start: formatDate(startDate),
				stop: formatDate(stopDate),
				startMs: startMs,
				stopMs: stopMs });
		$('#interval-select').append(newOption);
	};

	var appendOptions = function(data) {
		for (var i=0; i < data.length; i++) {
			appendOption(data[i]);
		}
	};

	var processCorrelationIntervalResults = function(data) {
		if (data.length < 2) {
			$('#interval-nav-content').hide();
		} else {
			$('#interval-nav-content').show();
			$('#interval-selec:').show();
			appendOptions(data);
		}
	};

	var getIntervals = function() {
		var url = '/correlationInterval/index/' + $('#correlationId').val();
		queuePostJSON('search', url, {}, processCorrelationIntervalResults);
	};

	$('#interval-select').on('change', function(e) {
		var startMs = $(this).find('option:selected').attr('startMs');
		var stopMs = $(this).find('option:selected').attr('stopMs');
		shiftPlot(startMs, stopMs);
	});

	var plotReady = setInterval(drawPlot, 50);
});
