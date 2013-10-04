<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />

<r:require module="selectable"/>

<c:jsCSRFToken keys="addEntryCSRF, getPeopleDataCSRF, getEntriesDataCSRF, autoCompleteDataCSRF, listTagsAndTagGroupsCSRF,
showTagGroupCSRF, createTagGroupCSRF, deleteTagGroupCSRF, addTagToTagGroupCSRF,
removeTagFromTagGroupCSRF, addTagGroupToTagGroupCSRF, removeTagGroupFromTagGroupCSRF" />

<r:script>

var defaultToNow = true;
var timeAfterTag = <g:if test="${prefs['displayTimeAfterTag']}">true</g:if><g:else>false</g:else>;

var cachedDate;
var cachedDateUTC;

function cacheDate() {
	cachedDate = $("#datepicker").datepicker('getDate');
	cachedDateUTC = cachedDate.toUTCString();
}

var currentTimeUTC;
var timeZoneOffset;

function cacheNow() {
	cacheDate();
	var now = new Date();
	currentTimeUTC = now.toUTCString();
	timeZoneOffset = now.getTimezoneOffset() * 60;
}

function changeDate(amount) {
	var currentDate = $("#datepicker").datepicker('getDate');
	$("#datepicker").datepicker('setDate', new Date(currentDate.getTime() + amount * 86400000));
	refreshPage();
}

function refreshPage() {
	cacheDate();
	
	$.getJSON("/home/getEntriesData?date="+ cachedDateUTC + "&userId=" + currentUserId + "&callback=?",
		getCSRFPreventionObject("getEntriesDataCSRF"),
		function(entries){
			if (checkData(entries))
				refreshEntries(entries);
		});
	tagList.load();
}
var tagList;
var tagListWidget;
var currentEntryId = undefined;

function clearEntries() {
	currentEntryId = undefined;
	$("#entry0").html('');
}

var currentDrag;
var currentDragTag;

$(document).mousemove(function(e) {
	if (currentDrag != null)
		currentDrag.setAbsolute({ x: e.pageX - 6, y: e.pageY - 6 });
});

$(document).mouseup(function(e) {
	if (currentDrag == null) return;
	$("#drag").html('');
	currentDrag = null;
	if ($("#area0").isUnderEvent(e)) {
		setEntryText(currentDragTag.description + ' ');
		$("#input0").focus();
		return;
	}
	currentDragTag = null;
});

var dayDuration = 86400000;

var entrySelectData;

function displayEntry(entry, isUpdating) {
	var id = entry.id,
		date = entry.date,
		datePrecisionSecs = entry.datePrecisionSecs,
		description = entry.description,
		amount = entry.amount,
		amountPrecision = entry.amountPrecision,
		units = entry.units,
		comment = entry.comment;

	var diff = date.getTime() - cachedDate.getTime();
	if (diff < 0 ||  diff >= dayDuration) {
		return; // skip items outside display
	}
	var dateStr = '';
	if (datePrecisionSecs < 43200) {
		dateStr = dateToTimeStr(date, false);
		if (timeAfterTag) {
			dateStr = ' ' + dateStr;
		} else {
			dateStr = dateStr + ' ';
		}
	}

	// store amount for post-selection highlighting
	
	var formattedAmount = formatAmount(amount, amountPrecision);
	if (formattedAmount.length > 0) {
		var selectStart = (timeAfterTag ? 0 : dateStr.length) + description.length + 1;
		var selectEnd = selectStart + formattedAmount.length - 1;
		entrySelectData[id] = [selectStart, selectEnd];
	}

	var innerHTMLContent = '<span class="content-wrapper">' + (timeAfterTag ? '' : '<span class="entryTime">' + escapehtml(dateStr) + '</span>') + '<span class="entryDescription">'
			+ escapehtml(description) + '</span>' + '<span class="entryAmount">' + escapehtml(formattedAmount) + '</span>'
			+ '<span class="entryUnits">' + escapehtml(formatUnits(units)) + '</span>' + (timeAfterTag ? '<span class="entryTime">'
			+ escapehtml(dateStr) + '</span>' : '') + (comment != '' ? ' ' + '<span class="' + (comment.startsWith('repeat') || comment.startsWith('daily') || comment.startsWith('weekly') || comment.startsWith('remind') ? 'entryRepeat' : 'entryComment') + '">' + escapehtml(comment) + '</span>' : '')
			+ '</span><a href="#" class="entryDelete" id="entrydelid' + id + '" onclick="deleteEntryId(' + id + ')"><img width="12" height="12" src="/images/x.gif"></a>';

	if(isUpdating) {
		$("#entry0 li#entryid" + id).html(innerHTMLContent);
	} else {
		$("#entry0").append('<li id="entryid' + id + '" data-entry-id="' + id + '">' + innerHTMLContent + '</li>');
	}
}

function displayEntries(entries) {
	entrySelectData = {};
	jQuery.each(entries, function() {
		displayEntry(this, false);
		return true;
	});
}

function refreshEntries(entries) {
	clearEntries();
	displayEntries(entries);
}

function deleteEntryId(entryId) {
	cacheNow();
	
	if (entryId == undefined) {
		alert("Please select entry you wish to delete");
	} else {
		$.getJSON("/home/deleteEntrySData?entryId=" + entryId
				+ "&currentTime=" + currentTimeUTC + "&baseDate=" + cachedDateUTC
				+ "&timeZoneOffset=" + timeZoneOffset + "&displayDate=" + cachedDateUTC
				+ "&callback=?",
				function(entries) {
					if (checkData(entries, 'success', "Error deleting entry")) {
						tagList.load();
						refreshEntries(entries[0]);
						updateAutocomplete(entries[1][0], entries[1][1], entries[1][2], entries[1][3]);
						if (entries[2] != null)
							updateAutocomplete(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
					}
				});
	}
}

function deleteCurrentEntry() {
	deleteEntryId(currentEntryId);
}

function updateEntry(entryId, text, defaultToNow) {
	cacheNow();
	
	$.getJSON("/home/updateEntrySData?entryId=" + entryId
			+ "&currentTime=" + currentTimeUTC + "&text=" + escape(text) + "&baseDate="
			+ cachedDateUTC + "&timeZoneOffset=" + timeZoneOffset + "&defaultToNow=" + (defaultToNow ? '1':'0') + "&callback=?",
	function(entries){
		if (checkData(entries, 'success', "Error updating entry")) {
			tagList.load();
			$.each(entries[0], function(index, entry) {
				/**
				 * Finding only that entry which is recently updated, and
				 * refreshing only that entry in UI.
				 */
				if(entry.id == entryId) {
					displayEntry(entry, true);
				}
			})
			updateAutocomplete(entries[1][0], entries[1][1], entries[1][2], entries[1][3]);
			if (entries[2] != null)
				updateAutocomplete(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
		}
	});
}

function addEntry(userId, text, defaultToNow) {
	cacheNow();
	
	$.getJSON("/home/addEntrySData?currentTime=" + currentTimeUTC
			+ "&userId=" + userId + "&text=" + escape(text) + "&baseDate=" + cachedDateUTC
			+ "&timeZoneOffset=" + timeZoneOffset + "&defaultToNow=" + (defaultToNow ? '1':'0')
			+ getCSRFPreventionURI("addEntryCSRF", "&") + "&callback=?",
			function(entries){
				if (checkData(entries, 'success', "Error adding entry")) {
					if (entries[1] != null) {
						alert(entries[1]);
					}
					tagList.load();
					refreshEntries(entries[0]);
					updateAutocomplete(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
				}
			});
}

function processInput(forceAdd) {
	field = $("#input0");
	field.autocomplete("close");
	var text = field.val();
	if (text == "") return; // no entry data
	field.val("");
	if ((!forceAdd) && (currentEntryId != undefined))
		updateEntry(currentEntryId, text, defaultToNow);
	else {
		addEntry(currentUserId, text, defaultToNow);
	}
	return true;
}

var entryTextSet = false;

function setEntryText(text, startSelect, endSelect) {
	var inp = $("#input0");
	inp.autocomplete("close");
	inp.val(text);
	inp.css('color','#000000');
	if (startSelect) {
		inp.selectRange(startSelect, endSelect);
	}
	inp.focus();
	entryTextSet = true;
}

// dynamic autocomplete
// var autoCache = {}, lastAutoXhr;

// static autocomplete
// changes to the autocomplete code should also get put into the index.gsp for the mobile app
var autoCache = {};
var freqTagList, dateTagList;

String.prototype.startsWith = function(str) {return (this.match("^"+str)==str)}

function doLogout() {
	callLogoutCallbacks();
}

var datepicker;
var lastEntrySelected = null;

$(function(){
	initTemplate();
	initAutocomplete();
	initTagListWidget();
	
	var currentDate;

	if (supportsLocalStorage() && localStorage['stateStored'] == "2") {
		currentDate = $.evalJSON(localStorage['currentDate']);
	} else {
		currentDate = new Date();
		localStorage['currentDate'] = $.toJSON(currentDate);
		localStorage['stateStored'] = "2";
	}
	$("#input0").val('Enter a tag.  For example: nap at 2pm');
	$("#input0").droppable({
		drop : function(event, ui) {
			var droppedItem = $(ui.draggable[0]).data(DATA_KEY_FOR_ITEM_VIEW).getData();
			setEntryText(droppedItem.description);
		}
	});

	datepicker = $("#datepicker");
	currentDate = new Date();
	if (${showTime} > 0)
		currentDate = new Date(${showTime});
	datepicker.datepicker({defaultDate: currentDate, dateFormat: 'DD MM dd, yy'});
	$("#datepicker").val($.datepicker.formatDate('DD MM dd, yy', currentDate));
	$("#ui-datepicker-div").css('display','none');
	
	datepicker.change(function () {
		refreshPage();
		localStorage['stateStored'] = "2";
		localStorage['currentDate'] = $.toJSON(datepicker.datepicker('getDate'));
	});	
	$("#input0").off("click");
	$("#input0").on("click", function(e) {
		if (!entryTextSet)
			setEntryText('');
	});
	$("#input0").keyup(function(e) {
		if (e.keyCode == 13) {
			processInput(false);
		}
	});
	$("#entry0").listable({cancel: 'a,input'});
	$("#entry0").off("listableselected");
	$("#entry0").off("listableunselecting");
	$("#entry0").on("listableunselecting", function(e, ui) {
		var $unselectee = $("#" + ui.unselecting.id);
		unselecting($unselectee);
	});
	function unselecting($unselectee, doNotUpdate) {
		if($unselectee.data('entryIsSelected') == 1) {
			$unselectee.removeClass('ui-selected');
			$unselectee.data('entryIsSelected', 2);
			$("a.entryDelete", $unselectee).hide();
			var $contentWrapper = $unselectee.find(".content-wrapper");

			if($contentWrapper.text() != $("input#tagTextInput").val() && !doNotUpdate) {
				updateEntry(currentEntryId, $("input#tagTextInput").val(), defaultToNow);
			} else {
				$contentWrapper.show();
			}

			$("input#tagTextInput").remove();
			currentEntryId = null;
		}
	}
	$("#entry0").on("listableselected", function(e, ui) {
		var $selectee = $("#" + ui.selected.id);
		selected($selectee);
	});
	function selected($selectee) {
		var state = $selectee.data('entryIsSelected');
		var $contentWrapper = $selectee.find(".content-wrapper");
		$selectee.siblings().data('entryIsSelected', 0);

		if (state == undefined || state == 0) {
			lastEntrySelected = $selectee;
			currentEntryId = $selectee.data("entry-id");
			$selectee.data('entryIsSelected', 1);
			$selectee.addClass('ui-selected');
			$("#entrydelid" + currentEntryId).css('display', 'inline');

			var entryText = $selectee.text();
			var selectRange = entrySelectData[currentEntryId];
			/*if (selectRange)
				setEntryText(entryText, selectRange[0], selectRange[1]);
			else
				setEntryText(entryText);*/
			$contentWrapper.hide();
			$selectee.append('<input type="text" id="tagTextInput" style="margin: 2px; width: 660px;"></input>');
			$("#tagTextInput").val(entryText).focus()
		} else if(state == 2) {
			$selectee.data('entryIsSelected', 0);
		}
	}
	$(document).on("keyup", "input#tagTextInput", function(e) {
		var $selectee = $(this).parent("li");
		if(e.keyCode == 13) {	// Enter pressed
			unselecting($selectee);
			selected($selectee);
		} else if(e.keyCode == 27) {	// Esc pressed
			unselecting($selectee, true);
			selected($selectee);
		}
	})
	$(document).on("blur", "input#tagTextInput", function(e) {
		var $selectee = $(this).parent("li");
		unselecting($selectee);
	})
	/**
	 * Keycode= 37:left, 38:up, 39:right, 40:down
	 */
	$("#entry0").keydown(function(e) {
		if($.inArray(e.keyCode, [38, 40]) == -1) {
			return true;
		}
		var $unselectee = $("li.ui-selected", "ol#entry0");
		if(!$unselectee) {
			return false;
		}
		var $selectee;
		if(e.keyCode == 40) { 
			$selectee = $unselectee.next();
		}
		if(e.keyCode == 38) { 
			$selectee = $unselectee.prev();
		}
		if($selectee) {
			unselecting($unselectee);
			selected($selectee);
		}
		return false;
	});
	
	/*
	$("#entry0").off("selectableselected");
	$("#entry0").on("selectableselected", function(e, ui) {
		currentEntryId = ui.selected.id.substring(7);
		var selectRange = entrySelectData[currentEntryId];
		if (selectRange)
			setEntryText(ui.selected.textContent, selectRange[0], selectRange[1]);
		else
			setEntryText(ui.selected.textContent);
	});*/
	
	/*$("input[name='tagorder']").change(function(e) {
		tagList.load(); // now doing reordering on client
	});*/

	initTemplate();

	$.getJSON("/home/getPeopleData?callback=?", getCSRFPreventionObject("getPeopleDataCSRF"),
		function(data){
			if (!checkData(data))
				return;
		
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
		
		refreshPage();
	});
});
</r:script>
</head>
<body>
<!-- MAIN -->
<div class="main" id="trackmain">

	<div id="autocomplete" style="position: absolute; top: 10px; right: 10px;"></div>
	<div id="area0">
		<div id="records">

			<h1>
				<a class="back" href="#" onclick="changeDate(-1);"><span>back</span>
				</a> <span class="date"><input id="datepicker" type="text"
					value="" />
				</span> <a class="next" href="#" onclick="changeDate(1);"><span>next</span>
				</a>
				<div style="clear: both"></div>
			</h1>

		</div>

		<div id="addData">
			<input type="text" id="input0" name="data"
				value="Enter a tag.  For example: nap at 2pm" class="textInput" />
			<div style="clear: both"></div>
		</div>

		<div id="recordList">
			<ol id="entry0">
			</ol>
		</div>
	</div>

</div>
<!-- /MAIN -->

<!-- RIGHT NAV -->
<g:render template="/tag/tagListWidget" />
<!-- /RIGHT NAV -->

<div style="clear: both;"></div>

</body>
</html>
