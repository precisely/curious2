<g:applyLayout name="plot">
<html>
<head>
<title><g:layoutTitle/></title>
<g:layoutHead/>
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

<content tag="processUserData">
	processUserData = function(data) {
		if (!checkData(data))
			return;
	
		found = false;
		
		tagListWidget = initTagListWidget();
		
		jQuery.each(data, function() {
			if (!found) {
				// set first user id as the current
				setUserId(this['id']);
				setUserName(this['username']);
				setNotificationBadge(this['notificationCount']);
				plot = new PlotWeb(tagListWidget.list, this['id'], this['username'], "#plotArea", true, true, new PlotProperties({
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
			addPerson(this['name'],
				this['username'], this['id'], this['sex']);
			return true;
		});
	};
</content>

</script>
</head>

<body class="${pageProperty(name: 'body.class') ?: '' }">
	<div class="red-header graph-header-container clearfix">
		<g:render template="/tag/tagListWidget" model="[header: true, expandByDefault: true]" />
		<h1 class="clearfix">
			<span id="queryTitle">PLOT</span>
			<span id="mobileQueryTitle" class="hid">PLOT</span>
			<span id="queryTitleEdit"><img src="/images/edit.gif"></span>
			<div id="debug"></div>
		</h1>

		<hr>
	</div>

	<div class="clearfix">
		<!-- RIGHT NAV BODY -->
		<g:render template="/tag/tagListWidget" />
		<!-- /RIGHT NAV BODY -->
		<div class="floating-column graph-container">
			<nav class="row disable-select" id="plot-menu" >
				<ul class="disable-select">
						<ul>
							<li class="filter"><a href="#" onclick="plot.clearGraphs()">New</a></li>
							<li class="filter"><a href="#" onclick="plot.save()">Save</a></li>
							<li class="filter"><g:link action="load">Load</g:link></li>
							<li class="filter"><a href="#" id="publish-chart">Publish</a></li>
						</ul>
				</ul>
			</nav>
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
	<div id="remove-exclusion-dialog" class="hide" title="Add back to the TagGroup"></div>
	<script>
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
	
<g:layoutBody/>
<g:render template="/templates/chart/publishGroupModal"/>
</body>
</html>
</g:applyLayout>
