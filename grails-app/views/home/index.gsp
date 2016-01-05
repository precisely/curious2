<!DOCTYPE HTML>
<html>
<head>
<meta name="layout" content="menu" />
<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>
<script src="/js/jquery/jquery.mobile.custom.min.js"></script>

<c:jsCSRFToken keys="addEntryCSRF, getPeopleDataCSRF, getListDataCSRF, autocompleteDataCSRF, listTagsAndTagGroupsCSRF,
		deleteTagGroupDataCSRF, addTagToTagGroupDataCSRF, addTagToTagGroupCSRF, deleteGhostEntryDataCSRF, deleteEntryDataCSRF, 
		updateEntrySDataCSRF,removeTagFromTagGroupCSRF, removeTagFromTagGroupDataCSRF, addTagGroupToTagGroupCSRF, 
		removeTagGroupFromTagGroupCSRF, pingDataCSRF,showTagGroupDataCSRF, 
		createTagGroupDataCSRF, excludeFromTagGroupDataCSRF, addBackToTagGroupDataCSRF, getInterestTagsDataCSRF, 
		addInterestTagDataCSRF, deleteInterestTagDataCSRF, updateInterestTagDataCSRF" />
</head>
<body class="track-page">

	<div id="alert-message" class="hide">
		<p><p>
		<div id="alert-message-text"></div>
	</div>
	<div class="red-header date-controls clearfix">
		<!-- g:render template="/tag/tagListWidget" model="[header: true]" / -->
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
	</div>

	<!-- MAIN -->
	<div class="clearfix">
		<!-- RIGHT NAV -->
		<!-- g:render template="/tag/tagListWidget" / -->
		<!-- /RIGHT NAV -->
		<div class="floating-column entry-container">
			<div class="main container-fluid" id="trackmain">
				<div id="autocomplete" style="position: absolute; top: 10px; right: 10px;"></div>
				<div id="area0">
					<div id="addData" class="input-affordance addon">
						<div class="track-input-modifiers">
							<img alt="tag" src="/images/tag.png" class="inputTag">
						</div>
							<!-- <p>choose<br>details:</p>
							<a class="track-input-modifiers inputRemindPosition" onclick="entryListWidget.modifyInput('remind')">
								<img alt="remind" class="inputRemind" src="/images/input-remind.png">
							</a>
							<a class="track-input-modifiers inputRepeatPosition" onclick="entryListWidget.modifyInput('repeat')">
								<img alt="repeat" class="inputRepeat" src="/images/input-repeat.png">
							</a>ook
							<a class="track-input-modifiers inputPinPosition" onclick="entryListWidget.modifyInput('bookmark')">
								<img alt="repeat" class="inputPin" src="/images/input-pin.png">
							</a>
							--!>
							<input class="full-width" type="text" placeholder="Enter tags here: (Example: nap 2pm or sleep quality repeat)"
									name="data" id="input0" required />
							<input type="hidden" name="repeatTypeId" id="input0RepeatTypeId" />
							<input type="hidden" name="repeatEnd" id="inpiut0RepeatEnd" />
					</div>
					<div style="clear: both"></div>
				</div>
				<div id="pinned-tags">
					<!-- img alt="Bookmarks" class="pin-header" src="/images/bookmark-tags.png" -->
					<span id="pinned-tag-list"></span>
				</div>
				<hr>
				<span class="sort-lable">Sort By:</span>
				<a href="#" id="sort-by-description" onclick="entryListWidget.sortByDescription(); return false;" class="sort-entry">A-Z
					<img class="sort-arrow hide" src="" alt="sort"/> 
				</a>
				<a href="#" id="sort-by-time" onclick="entryListWidget.sortByTime(); return false;" class="sort-entry">TIME
					<img class="sort-arrow hide" src="" alt="sort"/> 
				</a>
				<div id="recordList">
					<ol id="entry0"></ol>
				</div>
			</div>
		</div>
	</div>
	<!-- /MAIN -->
	<div id="remove-exclusion-dialog" class="hide" title="Add back to the Group"></div>
	<script type="text/javascript" src="/js/curious/entrylist.js?ver=27"></script>
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
					addPerson(this['name'],
							this['username'], this['id'], this['sex']);
					return true;
				});
				
				entryListWidget = new EntryListWidget();
			});
		});

		$(document).ready(function() {
			var newEntryDetailsPopover = _.template($('#entry-details-popover').clone().html())({'editType': 'new'});
			
			$('#addData').prepend(newEntryDetailsPopover);
			$('#addData .track-input-dropdown').show();
			$("#addData .choose-date-input").datepicker();

			$('#addData .repeat-entry-checkbox').change(function() {
				$('#addData .repeat-modifiers').toggleClass('hide');
			});
		});
	</script>
	<g:render template="/templates/track/entryDetailsPopover" />
	</body>
</html>
