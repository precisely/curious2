<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
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
</head>
<body>
	<!-- LEFT NAV-->
	<div class="leftNav">
		<div id="plotLeftNav">
			<div id="plotLinesplotArea"></div>
		</div>

	</div>
	<!-- /LEFT NAV-->
	<!-- MAIN -->
	<div class="main querymain">
		<div id="dialogDivplotArea" class="display:none;"></div>
		<div class="graphData">

			<h1>
				<span id="queryTitle"></span> <span id="queryTitleEdit"><img
					src="/images/edit.gif"></span>
				<div id="debug"></div>
			</h1>

			<div id="actions">
				<ul>
					<li><a href="#" onclick="plot.save()">Save</a></li>
					<li><g:link controller='home' action="load">Load</g:link></li>
					<li><a href="#" onclick="plot.saveSnapshot()">Share</a></li>
				</ul>
			</div>

			<div id="plotArea"></div>

			<div class="main querycontrols">
				<div class="calendarRange">
					<div class="zoomline">
						<div id="zoomcontrol1"></div>
					</div>
					<div class="dateline">
						<div class="startDate">
							<input id="startdatepicker1" type="text" value=""
								class="startdatepicker cycleInput" />
						</div>
						<div class="cycleTag" id="cycleTag1">drag relative tag here</div>
						<div class="endDate">
							<input id="enddatepicker1" type="text" value="" class="enddatepicker cycleInput" />
						</div>
					</div>
				</div>
			</div>

		</div>
	</div>
	<!-- /MAIN -->

	<!-- RIGHT NAV -->
	<g:render template="/tag/tagListWidget" />
	<!-- /RIGHT NAV -->
	<div style="clear: both;"></div>

	<!-- PRE-FOOTER -->
	<div id="preFooter">

		<div class="tagNav">
			view: <img src="/images/scatter.gif" alt="scatter" /> <img
				src="/images/line.gif" alt="line" /> <img src="/images/fill.gif"
				alt="fill" />
		</div>
	</div>

	<!-- /PRE-FOOTER -->
	<div style="clear: both;"></div>

</body>
</html>
