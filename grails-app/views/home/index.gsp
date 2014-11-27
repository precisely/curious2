<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />
<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>
<script src="/js/jquery/jquery.mobile.custom.min.js"></script>

<c:jsCSRFToken keys="addEntryCSRF, getPeopleDataCSRF, getListDataCSRF, autocompleteDataCSRF, listTagsAndTagGroupsCSRF,
showTagGroupCSRF, createTagGroupCSRF, deleteTagGroupCSRF, addTagToTagGroupCSRF, deleteGhostEntryDataCSRF, deleteEntryDataCSRF, updateEntrySDataCSRF,
removeTagFromTagGroupCSRF, addTagGroupToTagGroupCSRF, removeTagGroupFromTagGroupCSRF, activateGhostEntryDataCSRF, pingDataCSRF,
excludeFromTagGroupDataCSRF, addBackToTagGroupDataCSRF" />

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
			<div class="main container-fluid" id="trackmain">
				<div id="autocomplete" style="position: absolute; top: 10px; right: 10px;"></div>
				<div id="area0">
					<div id="addData" class="input-affordance addon">
						<div class="icon icon-tag"></div>
						<p>choose<br>details:</p>
						<a href="#" class="icon icon-xl icon-bell" onclick="entryListWidget.modifyInput('remind')">
							<span class="text icon-bell-text">Reminder</span></a>
						<a href="#" class="icon icon-xl icon-repeat" onclick="entryListWidget.modifyInput('repeat')">
							<span class="text icon-repeat-text">Repeat</span></a>
						<a href="#" class="icon icon-xl icon-pin" onclick="entryListWidget.modifyInput('pinned')">
							<span class="text icon-pin-text">Pin it</span></a>
							 <input class="full-width" type="text" placeholder="Enter tags here: (Example: nap 2pm or sleep quality repeat)"
							name="data" id="input0" required />
					</div>
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
	<div id="remove-exclusion-dialog" class="hide" title="Add back to the Group"></div>
	<script type="text/javascript" src="/js/curious/entrylist.js"></script>
	<script type="text/javascript">
		var timeAfterTag = ${prefs['displayTimeAfterTag'] ? true : false};
		var currentDate = new Date();
		if (${showTime} > 0)
			currentDate = new Date(${showTime});

		function doLogout() {
			callLogoutCallbacks();
		}

		var entryListWidget;
		
		$(function() {
			queueJSON("getting login info", "/home/getPeopleData?callback=?", getCSRFPreventionObject("getPeopleDataCSRF"),
					function(data){
				if (!checkData(data)) {
					return;
				}
	
				var found = false;
	
				jQuery.each(data, function() {
					if (!found) {
						// set first user id as the current
						setUserId(this['id']);
						found = true;
					}
					addPerson(this['first'] + ' ' + this['last'],
							this['username'], this['id'], this['sex']);
					return true;
				});
				
				entryListWidget = new EntryListWidget(initTagListWidget());				
			});
		});
	</script>
</body>
</html>
