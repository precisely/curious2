<html>
<head>
<meta name="layout" content="plot" />
<title>Curious</title>

<c:jsCSRFToken keys="getPlotDataCSRF, getSumPlotDataCRSF, showTagGroupCSRF, getPeopleDataCSRF, createTagGroupCSRF,
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
	
	$.getJSON("/home/getPeopleData?callback=?",
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
		$("#plotArea").removeClass("table");
	})
	$(document).on(afterLinePlotEvent, function(e, tag) {
		adjustTrackingTagHeaderHeight();
	})
	$(document).on(afterLineRemoveEvent, function(e, plotInstance) {
		adjustTrackingTagHeaderHeight();
		if ($("#plotArea").html().trim() == "") {
			//if(plotInstance.lines.length == 0)
			$("#plotArea").addClass("table").html('<div id="drag-here-msg" class="table-cell align-middle">DRAG TRACKING TAGS HERE TO GRAPH</div>');
		}
	})
	function adjustTrackingTagHeaderHeight() {
		var queryTitleHeight = $("#queryTitle").parent().height();
		$("#toggle-tags").parent().css("height", queryTitleHeight);
	}
	// Callback handler after tag collapse animation finished.
	function afterTagCollapseToggle() {
		// Checking if any plot line available.
		if ($("#plotArea").find("#drag-here-msg").length == 0) {
			plot.refreshPlot();
		}
	}
</r:script>
</head>
<body class="graph-page">

	<div class="row row-custom">
		<div class="col-xs-2">
			<!-- LEFT NAV -->
			<div class="leftNav">
				<div id="plotLeftNav">
					<div id="plotLinesplotArea"></div>
				</div>
			</div>
			<!-- /LEFT NAV -->
		</div>
		<div class="col-xs-10">
			<div class="row">
				<!-- RIGHT NAV HEADER -->
				<g:render template="/tag/tagListWidget" model="[header: true, expandByDefault: true]" />
				<!-- RIGHT NAV HEADER -->
				<div class="col-xs-9 floating-column graph-header-container">
					<div class="red-header">
						<h1 class="clearfix">
							<span id="queryTitle"></span>
							<span id="queryTitleEdit"><img src="/images/edit.gif"></span>
							<div id="debug"></div>
						</h1>
					</div>
				</div>
			</div>
			<div class="row">
				<!-- RIGHT NAV BODY -->
				<g:render template="/tag/tagListWidget" />
				<!-- /RIGHT NAV BODY -->
				<div class="col-xs-9 floating-column graph-container">
					<!-- MAIN -->
					<div class="main querymain">
						<div id="dialogDivplotArea" class="hide"></div>
						<div class="graphData">
							<div id="actions">
								<ul>
									<li><a href="#" onclick="plot.save()">Save</a></li>
									<li><g:link action="load">Load</g:link></li>
									<li><a href="#" onclick="plot.saveSnapshot()">Share</a></li>
								</ul>
							</div>
				
							<div id="plotArea" class="table full-width">
								<div id="drag-here-msg" class="table-cell align-middle">DRAG TRACKING TAGS HERE TO GRAPH</div>
							</div>
				
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

</body>
</html>