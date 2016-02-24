<g:applyLayout name="menu">
<html>
<head>
<title><g:layoutTitle/></title>
<c:jsCSRFToken keys="getPlotDescDataCSRF, getSumPlotDescDataCSRF, showTagGroupCSRF, deleteTagGroupDataCSRF, showTagGroupDataCSRF, 
getTagPropertiesCSRF, autocompleteDataCSRF, addTagToTagGroupCSRF, listTagsAndTagGroupsCSRF, removeTagFromTagGroupCSRF, 
addTagGroupToTagGroupCSRF, createTagGroupDataCSRF, removeTagGroupFromTagGroupCSRF, setTagPropertiesDataCSRF,
addBackToTagGroupDataCSRF, removeTagFromTagGroupDataCSRF" />
<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>
<g:layoutHead/>
<!--script type="text/javascript" src="/js/timezone/date.js?ver=1"></script -->
<script type="text/javascript" src="/js/flot/jquery.flot.js?ver=2"></script>
<script type="text/javascript" src="/js/flot/jquery.flot.time.js?ver=2"></script>
<script type="text/javascript" src="/js/flot/jquery.flot.tooltip.js?ver=2"></script>
<!--[if IE]><script language="javascript" type="text/javascript" src="/lib/flot/excanvas.pack.js"></script><![endif]-->
<script type="text/javascript" src="/js/interpolate/smooth.js"></script>
<script type="text/javascript" src="/js/interpolate/science.js"></script>
<script type="text/javascript" src="/js/interpolate/science_loess.js"></script>
<script type="text/javascript" src="/js/curious/queryplot.js?ver=26"></script>
<script type="text/javascript" src="/js/curious/queryplot.web.js?ver=26"></script>

<script type="text/javascript">

//timezoneJS.timezone.zoneFileBasePath = '/js/timezone/zonefiles';
//timezoneJS.timezone.init();

function adjustTrackingTagHeaderHeight() {
	$('.tags-header-container').css("padding", "");		// Clearing any previous padding to calculate actual height.
	var queryTitleHeight = $('.red-header').height();
	console.log('Height is', queryTitleHeight);
	if (queryTitleHeight > (20 + 18) && $(window).width() > 480) {	// Checking if header's height is greater 20px as default plus 18px as padding.
		var padding = (queryTitleHeight - 20)/2;
		$('.tags-header-container').css('padding', padding + 'px 7px');
	}

	//Also need to adjust graph height so it does not  move
	var graphAdjustedHeight = 530 - $('#plotLeftNav').height();
	if (graphAdjustedHeight > 300) {
		$('#plotArea').css('height', graphAdjustedHeight + 'px');
		if ($(".graphData").hasClass("has-plot-data") && plot && plot.plotData && plot.plotData.length != 0) {
			plot.redrawPlot();
		}
	}

	if ($(window).width() <= 480) {
		var $queryTitle = $('#queryTitle');
		var $mobileQueryTitle = $('#mobileQueryTitle');
		var text = $queryTitle.text();
		if (text && text.length > 25) {
			text = text.substring(0, 25) + '...';
		}
		$mobileQueryTitle.html(text);
	}
}

// Callback handler after tag collapse animation finished.
function afterTagCollapseToggle() {
	if ($(window).width() <= 480) {
		adjustTrackingTagHeaderHeight();
	}
	if (!plot) return;
	// Checking if any plot line available.
	if (plot.plotData && plot.plotData.length != 0) {
		plot.refreshPlot();
	}
}

//assumes propertyClosure has the following methods:
//get/set: name, startDate. endDate, centered
function PlotProperties(divIdArray) {
	// assumes divArray has the following properties:
	// startDate, endDate

	this.startDatePicker = $(divIdArray['startDate']);
	this.endDatePicker = $(divIdArray['endDate']);
	if (divIdArray['username'] != null)
		this.usernameField = $(divIdArray['username']);
	else
		this.usernameField = null;
	this.nameField = $(divIdArray['name']);
	this.renameField = $(divIdArray['rename']);
	if (divIdArray['zoomControl'])
		this.zoomControl = $(divIdArray['zoomControl']);
	this.cycleTagDiv = $(divIdArray['cycleTag']);

	this.startDateInit = divIdArray['startDateInit'];
	this.initStartDate = function() {
		this.startDatePicker.datepicker("setDate", null);
		initTextField(this.startDatePicker, this.startDateInit);
	}
	this.endDateInit = divIdArray['endDateInit'];
	this.initEndDate = function() {
		this.endDatePicker.datepicker("setDate", null);
		initTextField(this.endDatePicker, this.endDateInit);
	}
	this.initStartDate();
	this.initEndDate();

	this.getName = function() {
		if (this.nameField) {
			return this.nameField.data('completeName');
		}
		return '';
	}
	this.setName = function(name) {
		if (this.nameField) {
			this.nameField.data('completeName', name);
			// Using max character limit 70 to render properly on both desktop and iPads
			this.nameField.text(shorten(name, 70, false));
		}
	}
	this.setUsername = function(name) {
		if (this.usernameField)
			this.usernameField.text(name);
	}
	this.getStartDate = function() {
		if (this.startDatePicker.data('textFieldAlreadyReset')) {
			return this.startDatePicker.datepicker('getDate');
		}
		return null;
	}
	this.getStartTime = function() {
		var startDate = this.getStartDate();
		if (!startDate) return 0;

		return startDate.getTime();
	}
	this.setStartDate = function(date) {
		setDateField(this.startDatePicker, date, this.startDateInit);
	}
	this.getEndDate = function() {
		if (this.endDatePicker.data('textFieldAlreadyReset')) {
			return this.endDatePicker.datepicker('getDate');
		}
		return null;
	}
	this.getEndTime = function() {
		var endDate = this.getEndDate();
		if (!endDate) return 0;

		return endDate.getTime();
	}
	this.setEndDate = function(date) {
		setDateField(this.endDatePicker, date, this.endDateInit);
	}
	this.getZoomControl = function() {
		return this.zoomControl;
	}
	this.getStartDatePicker = function() {
		return this.startDatePicker;
	}
	this.getEndDatePicker = function() {
		return this.endDatePicker;
	}
	this.getUsernameField = function() {
		return this.usernameField;
	}
	this.getNameField = function() {
		return this.nameField;
	}
	this.getRenameField = function() {
		return this.renameField;
	}
	this.getCycleTagDiv = function() {
		return this.cycleTagDiv;
	}
	// show data for a given userId at a given timestamp
	this.showDataUrl = function(userId, userName, timestamp) {
		if (currentUserId != userId) return;
		if (currentUserName != userName) return;
		if (currentUserId < 0) return; // disallow showData for anonymous users
		return "/home/index?showTime=" + timestamp;
	}
	// show data for a given userId at a given timestamp
	this.showData = function(userId, userName, timestamp) {
		window.location = this.showDataUrl(userId, userName, timestamp);
	}
}

$(document).on(beforeLinePlotEvent, function(e, tag) {
	$("div#drag-here-msg").css("visibility", "hidden"); // Keeping element space but invisible.
	$(".graphData").addClass("has-plot-data");
	$("#plotArea").removeClass("table");
});
$(document).on(afterLinePlotEvent, function(e, tag) {
	adjustTrackingTagHeaderHeight();
});
$(document).on(afterQueryTitleChangeEvent, function(e, tag) {
	adjustTrackingTagHeaderHeight();
});
$(document).on(afterLineRemoveEvent, function(e, plotInstance) {
	if (!plot) return;
	adjustTrackingTagHeaderHeight();
	if (!plot.plotData || plot.plotData == null) {
		$("#zoomcontrol1").slider("destroy");
		$(".graphData").removeClass("has-plot-data");
		$("#plotArea").addClass("table").html('<div id="drag-here-msg" class="table-cell align-middle">DRAG TRACKING TAGS HERE TO GRAPH</div>');
	}
});

$(window).resize(function() {
	if (plot && plot.plotData && plot.plotData.length != 0 && !plot.plotArea.is(":hidden")) {
		console.log('Refreshing graph on window resize');
		plot.refreshAll();
		adjustTrackingTagHeaderHeight();
	}
});

var plot;

var WeAreCurious = {};

WeAreCurious.getPlot = function() {
	return plot;
}

</script>
</head>
<body class="${pageProperty(name: 'body.class') ?: '' }">
	<content tag="processUserData"><g:pageProperty name="page.processUserData"/></content>
	<g:layoutBody/>
</body>
</html>
</g:applyLayout>
