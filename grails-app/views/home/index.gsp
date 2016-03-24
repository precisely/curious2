<!DOCTYPE HTML>
<html>
<head>
	<meta name="layout" content="menu" />
	<link type="text/css" href="/css/entry/track-page.css" rel="stylesheet">
	<script src="/js/jquery/jquery.ui.touch-punch.min.js"></script>
	<script src="/js/jquery/jquery.mobile.custom.min.js"></script>

<c:jsCSRFToken keys="addEntryCSRF, getPeopleDataCSRF, getListDataCSRF, autocompleteDataCSRF, listTagsAndTagGroupsCSRF,
		deleteTagGroupDataCSRF, addTagToTagGroupDataCSRF, addTagToTagGroupCSRF, deleteGhostEntryDataCSRF, deleteEntryDataCSRF, 
		updateEntrySDataCSRF,removeTagFromTagGroupCSRF, removeTagFromTagGroupDataCSRF, addTagGroupToTagGroupCSRF, 
		removeTagGroupFromTagGroupCSRF, pingDataCSRF,showTagGroupDataCSRF, saveDeviceEntriesStateDataCSRF,
		createTagGroupDataCSRF, excludeFromTagGroupDataCSRF, addBackToTagGroupDataCSRF, getInterestTagsDataCSRF, 
		addInterestTagDataCSRF, deleteInterestTagDataCSRF, updateInterestTagDataCSRF" />
</head>
<body class="track-page">

	<div id="alert-message" class="hide">
		<p><p>
		<div id="alert-message-text"></div>
	</div>

	<div class="red-header date-controls clearfix">
		<h1 class="clearfix">
			<a class="back" href="#" onclick="entryListWidget.changeDate(-1); return false;">
				<img alt="back" class="date-left-arrow" src="/images/left-arrow.png">
				<span class="hide">back</span>
			</a>
			<span class="date"><input id="datepicker" type="text" value="" /></span>
			<a class="next" href="#" onclick="entryListWidget.changeDate(1); return false;">
				<img alt="back" class="date-right-arrow" src="/images/right-arrow.png">
				<span class="hide">next</span>
			</a>
		</h1>
	</div>

	<!-- MAIN -->
	<!-- TODO Fix indentation after merge -->
			<div class="main entry-container clearfix">
				<div id="autocomplete" style="position: absolute; top: 10px; right: 10px;"></div>
				<div id="area0">
					<div id="addData" class="input-affordance addon">
						<div class="track-input-modifiers">
							<img alt="tag" src="/images/tag.png" class="inputTag">
						</div>
							<input class="full-width" type="text" placeholder="sleep 8 hours 8:15am (good dreams)"
									name="data" id="input0" required />
							<input type="hidden" name="repeatTypeId" id="input0RepeatTypeId" />
							<input type="hidden" name="repeatEnd" id="inpiut0RepeatEnd" />
					</div>
					<div style="clear: both"></div>
				</div>
				<div id="pinned-tags">
					<img alt="Bookmarks" class="pin-header" src="/images/bookmark-tags.png" />
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
					<ol id="entry0" class="entry-list"></ol>
				</div>
			</div>

	<!-- /MAIN -->
	<script type="text/javascript" src="/js/curious/entrylist.js?ver=27"></script>
	<script type="text/javascript" src="/js/curious/state.view.js?ver=27"></script>
	<script type="text/javascript" src="/js/curious/entry/entry.data.js?ver=27"></script>
	<script type="text/javascript" src="/js/curious/entry/entry.device.data.js?ver=27"></script>
	<script type="text/javascript" src="/js/curious/entry/entry.device.data.summary.js?ver=27"></script>
	<script type="text/javascript">
		var timeAfterTag = ${prefs['displayTimeAfterTag'] ? true : false};
		var currentDate = new Date();
		if (${showTime} > 0)
			currentDate = new Date(${showTime});

		function doLogout() {
			callLogoutCallbacks();
		}

		registerLogoutCallback(function() {
			if (typeof store == 'undefined' || typeof (store.namespace('entries.state')) == 'undefined' || typeof (store.namespace("entries.state").session) == 'undefined') {
				return;
			}
			// Clear the saved collapse/expand state of device entries on logout
			var entrySessionStorage = store.namespace("entries.state").session;
			entrySessionStorage.clear();
		});

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
			newEntryDetailsPopoverContent = _.template($('#entry-details-popover-content').clone().html())({'editType': 'new', entryId: ""});
			createPopover($('.track-input-modifiers'), newEntryDetailsPopoverContent,'#addData');
			$("#addData .choose-date-input").datepicker({changeYear: true, changeMonth: true, yearRange: "-120:+0"});

			$(document).on('change', '#addData .repeat-entry-checkbox', function() {
				$('#addData .repeat-modifiers').toggleClass('hide');
			});
			$('#addData .track-input-modifiers').click(function() {
				if ($('.ui-selected').length) {
					entryListWidget.unselectEntry($('.ui-selected'));
				}
			})
		});
	</script>
	<g:render template="/templates/track/entryDetailsPopover" />
	<g:render template="/templates/track/entryDetailsPopoverContent"/>
	<div class="entry-details-popover-temp-container test"></div>
</body>
</html>
