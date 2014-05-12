<html>
<head>
<meta name="layout" content="plot" />
<title>Curious</title>

<c:jsCSRFToken keys="getPlotDataCSRF, getSumPlotDataCSRF, showTagGroupCSRF, getPeopleDataCSRF, createTagGroupCSRF,
deleteTagGroupCSRF, getTagPropertiesCSRF, autocompleteDataCSRF, addTagToTagGroupCSRF, listTagsAndTagGroupsCSRF,
removeTagFromTagGroupCSRF, addTagGroupToTagGroupCSRF, removeTagGroupFromTagGroupCSRF, setTagPropertiesDataCSRF" />
<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>

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
<script>
	$(document).on(beforeLinePlotEvent, function(e, tag) {
		$("div#drag-here-msg").css("visibility", "hidden"); // Keeping element space but invisible.
		$(".graphData").addClass("has-plot-data");
		$("#plotArea").removeClass("table");
	});
	$(document).on(afterLinePlotEvent, function(e, tag) {
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
	function adjustTrackingTagHeaderHeight() {
		$('.tags-header-container').css("padding", "");		// Clearing any previous padding to calculate actual height.
		var queryTitleHeight = $('.red-header').height();
		if (queryTitleHeight > (20 + 18)) {		// Checking if header's height is greater 20px as default plus 18px as padding.
			var padding = (queryTitleHeight - 20)/2;
			console.log('Adjusting height to: ' + queryTitleHeight);
			$('.tags-header-container').css('padding', padding + 'px 7px');
		}

		//Also need to adjust graph height so it does not  move
		var graphAdjustedHeight = 530 - $('#plotLeftNav').height();
		if (graphAdjustedHeight > 300) {
			$('#plotArea').css('height', graphAdjustedHeight + 'px');
			if ($(".graphData").hasClass("has-plot-data")) {
				plot.redrawPlot();
			}
		}
	}
	// Callback handler after tag collapse animation finished.
	function afterTagCollapseToggle() {
		if (!plot) return;
		// Checking if any plot line available.
		if (plot.plotData && plot.plotData.length != 0) {
			plot.refreshPlot();
		}
	}
</script>
</head>
<body class="graph-page">
	<div class="red-header graph-header-container clearfix">
		<g:render template="/tag/tagListWidget" model="[header: true, expandByDefault: true]" />
		<h1 class="clearfix right">
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
			<span id="queryTitle">PLOT</span>
			<span id="queryTitleEdit"><img src="/images/edit.gif"></span>
			<div id="debug"></div>
		</h1>
	</div>

	<div class="clearfix">
		<!-- RIGHT NAV BODY -->
		<g:render template="/tag/tagListWidget" />
		<!-- /RIGHT NAV BODY -->
		<div class="floating-column graph-container">
			<div id="plotLeftNav">
				<div id="plotLinesplotArea" class="plotlines clearfix"></div>
			</div>
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
								<div class="col-xs-4">
									<div class="startDate">
										<input id="startdatepicker1" type="text" value="" class="startdatepicker cycleInput" />
									</div>
								</div>
								<div class="col-xs-4">
									<div class="cycleTag" id="cycleTag1">drag relative tag here</div>
								</div>
								<div class="col-xs-4">
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
		</div>
	</div>

	<!-- PRE-FOOTER -->
	<div id="preFooter"></div>
	<!-- /PRE-FOOTER -->
	<script>
		$(window).load(function(){
			$('.red-header #actions img').click(function(e) {
				$('ul', $(e.target).parent()).toggle();
			});
			$('.red-header #actions ul').mouseleave(function(e) {
				$(e.target).closest('ul').toggle();
			});
		});
		var resizeTimeout;
		$(window).resize(function() {
			clearTimeout(resizeTimeout);
			// Adding 1 second buffer for refreshing graph on fast window resize;
			setTimeout(function() {
				afterTagCollapseToggle();
				if ($(window).width() >= 768) {
					// Only adjust height when columns are horizontally stacked.
					adjustTrackingTagHeaderHeight();
				}
			}, 1000);
		});
	</script>
</body>
</html>
