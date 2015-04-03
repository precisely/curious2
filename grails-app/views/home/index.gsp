<!DOCTYPE HTML>
<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />
<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>
<script src="/js/jquery/jquery.mobile.custom.min.js"></script>

<c:jsCSRFToken keys="addEntryCSRF, getPeopleDataCSRF, getListDataCSRF, autocompleteDataCSRF, listTagsAndTagGroupsCSRF,
showTagGroupCSRF, createTagGroupCSRF, deleteTagGroupDataCSRF, addTagToTagGroupDataCSRF, addTagToTagGroupCSRF, deleteGhostEntryDataCSRF, deleteEntryDataCSRF, updateEntrySDataCSRF,
removeTagFromTagGroupCSRF, removeTagFromTagGroupDataCSRF, addTagGroupToTagGroupCSRF, removeTagGroupFromTagGroupCSRF, activateGhostEntryDataCSRF, pingDataCSRF,showTagGroupDataCSRF, createTagGroupDataCSRF
excludeFromTagGroupDataCSRF, addBackToTagGroupDataCSRF, getInterestTagsDataCSRF, addInterestTagDataCSRF, deleteInterestTagDataCSRF, updateInterestTagDataCSRF" />
</head>
<body class="track-page">

	<div id="alert-message" class="hide">
		<p><p>
		<div id="alert-message-text"></div>
	</div>
	<div class="red-header date-controls clearfix">
		<g:render template="/tag/tagListWidget" model="[header: true]" />
		<h1 class="clearfix">
			<a class="back" href="#" onclick="entryListWidget.changeDate(-1);">
				<img alt="back" class="date-left-arrow" src="/images/left-arrow.png">
				<span class="hide">back</span>
			</a>
			<span class="date"><input id="datepicker" type="text" value="" /></span>
			<a class="next" href="#" onclick="entryListWidget.changeDate(1);">
				<img alt="back" class="date-right-arrow" src="/images/right-arrow.png">
				<span class="hide">next</span>
			</a>
		</h1>
		<hr>
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
						<div class="track-input-modifiers"><img alt="tag" src="/images/tag.png" class="inputTag"></div>
							<p>choose<br>details:</p>
							<a href="#" class="track-input-modifiers inputRemindPosition" onclick="entryListWidget.modifyInput('remind')">
								<img alt="remind" class="inputRemind" src="/images/input-remind.png">
							</a>
							<a href="#" class="track-input-modifiers inputRepeatPosition" onclick="entryListWidget.modifyInput('repeat')">
								<img alt="repeat" class="inputRepeat" src="/images/input-repeat.png">
							</a>
							<a href="#" class="track-input-modifiers inputPinPosition" onclick="entryListWidget.modifyInput('pinned')">
								<img alt="repeat" class="inputPin" src="/images/input-pin.png">
							</a>
							<input class="full-width" type="text" placeholder="Enter tags here: (Example: nap 2pm or sleep quality repeat)"
							name="data" id="input0" required />
						</div>
						<div style="clear: both"></div>
				</div>
				<div id="pinned-tags">
					<img alt="Pinned Tags" class="pin-header" src="/images/pinned-tags.png">
					<span id="pinned-tag-list"></span>
				</div>
				<hr>
				<span class="sort-lable">Sort By:</span>
				<a href="#" id="sort-by-description" onclick="entryListWidget.sortByDescription()" class="sort-entry">A-Z
					<img class="sort-arrow hide" src="" alt="sort"/> 
				</a>
				<a href="#" id="sort-by-time" onclick="entryListWidget.sortByTime()" class="sort-entry">TIME
					<img class="sort-arrow hide" src="" alt="sort"/> 
				</a>
				<hr>
				<div id="recordList">
					<ol id="entry0"></ol>
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
