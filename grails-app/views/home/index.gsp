<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />
<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>

<c:jsCSRFToken keys="addEntryCSRF, getPeopleDataCSRF, getListDataCSRF, autocompleteDataCSRF, listTagsAndTagGroupsCSRF,
showTagGroupCSRF, createTagGroupCSRF, deleteTagGroupCSRF, addTagToTagGroupCSRF, deleteGhostEntryDataCSRF, deleteEntryDataCSRF, updateEntrySDataCSRF,
removeTagFromTagGroupCSRF, addTagGroupToTagGroupCSRF, removeTagGroupFromTagGroupCSRF, activateGhostEntryDataCSRF, pingDataCSRF" />

</head>
<body class="track-page">

	<div id="alert-message" class="hide">
		<p><p>
		<div id="alert-message-text"></div>
	</div>

	<div class="red-header date-controls clearfix">
		<g:render template="/tag/tagListWidget" model="[header: true]" />
		<h1 class="clearfix right">
			<a class="back icon-triangle icon-triangle-left" href="#" onclick="changeDate(-1);">
				<span class="hide">back</span>
			</a>
			<span class="date"><input id="datepicker" type="text" value="" /></span>
			<a class="next  icon-triangle icon-triangle-right" href="#" onclick="changeDate(1);">
				<span class="hide">next</span>
			</a>
		</h1>
	</div>

	<!-- MAIN -->
	<div class="clearfix">
		<!-- RIGHT NAV -->
		<g:render template="/tag/tagListWidget" />
		<!-- /RIGHT NAV -->
		<div class="floating-column entry-container">
			<div class="main" id="trackmain">
				<div id="autocomplete" style="position: absolute; top: 10px; right: 10px;"></div>
				<div id="area0">
					<div id="addData" class="panel-wrapper">
						<input type="text" id="input0" name="data" style="width:calc(100% - 75px);margin-right:5px;"
							value="Enter a tag.  For example: nap at 2pm" class="textInput" />
						<a href="#" onclick="modifyInput('repeat')"><img src="/images/repeat.png" style="width:20px;height:20px;padding-top:5px;"></a>
						<a href="#" onclick="modifyInput('remind')"><img src="/images/remind.png" style="width:20px;height:20px;padding-top:5px;"></a>
						<a href="#" onclick="modifyInput('pinned')"><img src="/images/pin.png" style="width:20px;height:20px;padding-top:5px;"></a>
						<div style="clear: both"></div>
					</div>
			
					<div id="recordList">
						<ol id="entry0"></ol>
					</div>
				</div>
			</div>
		</div>
	</div>
	<!-- /MAIN -->
	<script type="text/javascript">
		var timeAfterTag = ${prefs['displayTimeAfterTag'] ? true : false};
		var currentDate = new Date();
		if (${showTime} > 0)
			currentDate = new Date(${showTime});
	</script>
	<script type="text/javascript" src="/js/curious/trackPage.js"></script>
</body>
</html>
