<html>
<head>
<meta name="layout" content="plot" />
<title>Curious</title>

<c:jsCSRFToken keys="getPlotDataCSRF, getSumPlotDataCSRF, showTagGroupCSRF, getPeopleDataCSRF, createTagGroupCSRF,
deleteTagGroupCSRF, getTagPropertiesCSRF, autocompleteDataCSRF, addTagToTagGroupCSRF, listTagsAndTagGroupsCSRF,
removeTagFromTagGroupCSRF, addTagGroupToTagGroupCSRF, removeTagGroupFromTagGroupCSRF, setTagPropertiesDataCSRF" />

<script type="text/javascript">
function refreshPage() {
	// TODO: used to reload taglist here, instead do incremental updates on changes
	tagList.load();
}

var plot = null;
var tagListWidget;
var tagList;
function doLogout() {
	callLogoutCallbacks();
}

$(function(){
	initTemplate();
	initTagListWidget();
	
	queueJSON("getting login info", "/home/getPeopleData?callback=?",
		getCSRFPreventionObject("getPeopleDataCSRF"),
		function(data) {
			if (!checkData(data))
				return;

			found = false;
			
			jQuery.each(data, function() {
				if (!found) {
					// set first user id as the current
					setUserId(this['id']);
					setUserName(this['username']);
					plot = new Plot(tagListWidget.list, this['id'], this['username'], "#plotArea", true, true, new PlotProperties({
						'startDate':'#startdatepicker1',
						'startDateInit':'start date and/or tag',
						'endDate':'#enddatepicker1',
						'endDateInit':'end date and/or tag',
						'cycleTag':'#cycleTag1',
						'zoomControl':'#zoomcontrol1',
						'username':'#queryUsername',
						'name':'#queryTitle',
						'rename':'#queryTitleEdit',
						'logout':'#logoutLink'
						}));

					$("#plotArea").droppable({
						drop:plot.handleDropTag.bind(plot)
					});
					
					// TODO: Refactor this to shared code

					/**$(document).mouseup(function(e) {
						if (currentDrag == null) return;
						$("#drag").html('');
						currentDrag = null;
						var found = false;
						for (var i in plot.lines) {
							var plotLineDiv = plot.lines[i].getDiv();
							if (plotLineDiv.isUnderEvent(e)) {
								if (plot.lines[i].addTag(currentDragTag)) {
									found = true;
									break;
								}
							}
						}
						if (!found) {
							if (plot.plotArea.isUnderEvent(e)) {
								plot.addLine(currentDragTag);
							} else {
								var cycleTagLine = plot.getCycleTagLine();
								if (cycleTagLine != null && cycleTagLine.getDiv().isUnderEvent(e)) {
									cycleTagLine.addTag(currentDragTag);
								} else if (plot.getCycleTagDiv().isUnderEvent(e)) {
									plot.addCycleLine(currentDragTag);
								}
							}
						}
						currentDragTag = null;
					}); **/

					if ('${plotDataId}' != '') {
						plot.loadId(${plotDataId});
					} else {
						var tagStorePoller = setInterval(function() {
							if(Object.keys(tagList.store.store).length>0) {
								clearInterval(tagStorePoller);
								plot.restore();
							}
						}, 500);
					}

					found = true;
				}
				addPerson(this['first'] + ' ' + this['last'],
					this['username'], this['id'], this['sex']);
				return true;
			});
		});
});
</script>
<r:script>
	$(document).on(beforeLinePlotEvent, function(e, tag) {
		$("div#drag-here-msg").css("visibility", "hidden"); // Keeping element space but invisible.
		$(".graphData").addClass("has-plot-data");
		$("#plotArea").removeClass("table");
	});
	$(document).on(afterLinePlotEvent, function(e, tag) {
		//adjustTrackingTagHeaderHeight();
	});
	$(document).on(afterLineRemoveEvent, function(e, plotInstance) {
		if (!plot) return;
		//adjustTrackingTagHeaderHeight();
		if (!plot.plotData || plot.plotData == null) {
			$("#zoomcontrol1").slider("destroy");
			$(".graphData").removeClass("has-plot-data");
			$("#plotArea").addClass("table").html('<div id="drag-here-msg" class="table-cell align-middle">DRAG TRACKING TAGS HERE TO GRAPH</div>');
		}
	});
	function adjustTrackingTagHeaderHeight() {
		var queryTitleHeight = $("#queryTitle").closest('.red-header').outerHeight();
		$("#toggle-tags").parent().css("height", queryTitleHeight);
	}
	// Callback handler after tag collapse animation finished.
	function afterTagCollapseToggle() {
		if (!plot) return;
		// Checking if any plot line available.
		if (plot.plotData && plot.plotData.length != 0) {
			plot.refreshPlot();
		}
	}

</r:script>
</head>
<body class="graph-page">

	<div class="row row-custom">
	    <div class="tagSearch">

		    <g:render template="/tag/tagListWidget" model="[header: true, expandByDefault: true, floatingColumn: true]" />
		    <g:render template="/tag/tagListWidget" model="[floatingColumn: true]"/>
		</div>
		<div class="col-xs-12">
			<div class="row">
				<div class="col-xs-9  graph-header-container">
					<div class="red-header">
						<h1 class="clearfix">
							<div id="actions">
								<img src="/images/menu.png">
								<ul>
									<li><a Ref="#" onclick="plot.clearGraphs()">New</a></li>
									<li><a href="#" onclick="plot.save()">Save</a></li>
									<li><g:link action="load">Load</g:link></li>
									<li><a href="#" onclick="plot.saveSnapshot()">Share (Publish to Community)  
										<img src="/images/eye.png">	
									</a></li>
								</ul>
							</div>
							<span id="queryTitle"></span>
							<span id="queryTitleEdit"><img src="/images/edit.gif"></span>
							<div id="debug"></div>
						</h1>
					</div>
					<div id="plotLeftNav">
						<div id="plotLinesplotArea"></div>
					</div>
				</div>
			</div>
			<div class="row">
				<!-- RIGHT NAV BODY -->
				<!-- /RIGHT NAV BODY -->
				<div class="col-xs-9 floating-column graph-container">
					<!-- MAIN -->
					<div class="main querymain">
						<div id="dialogDivplotArea" class="hide"></div>
						<div class="graphData">
				
							<div id="plotArea" class="table full-width">
								<div id="drag-here-msg" class="table-cell align-middle">DRAG TRACKING TAGS HERE TO GRAPH</div>
							</div>
							<div id="height-balancer"></div>
							<div class="main querycontrols">
								<div class="calendarRange">
									<div class="zoomline">
										<div id="zoomcontrol1"></div>
									</div>
									<div class="dateline row">
										<div class="col-sm-4">
											<div class="startDate">
												<input id="startdatepicker1" type="text" value="" class="startdatepicker cycleInput" />
											</div>
										</div>
										<div class="col-sm-4">
											<div class="cycleTag" id="cycleTag1">drag relative tag here</div>
										</div>
										<div class="col-sm-4">
											<div class="endDate">
												<input id="enddatepicker1" type="text" value="" class="enddatepicker cycleInput" />
											</div>
										</div>
									</div>
								</div>
							</div>
						</div>
					</div>
					<!-- /MAIN -->
					<br>
					<div class="view-types">
						view:
						<img src="/images/scatter.gif" alt="scatter" />
						<img src="/images/line.gif" alt="line" />
						<img src="/images/fill.gif" alt="fill" />
					</div>
				</div>
			</div>
		</div>
	</div>

	<!-- PRE-FOOTER -->
	<div id="preFooter"></div>
	<!-- /PRE-FOOTER -->
<r:script>
	$(window).load(function(){
		$(document).on('click','.plotGroup', function(e) {
			var $droppableElement = $(e.target).closest('.ui-droppable');
			$('.plotlineinfo', $droppableElement).toggle();
		});
		$('.red-header #actions img').click(function(e) {
			$('ul', $(e.target).parent()).toggle();
		});
		$('.red-header #actions ul').mouseleave(function(e) {
			$(e.target).closest('ul').toggle();
		});
	});

</r:script>

</body>
</html>
