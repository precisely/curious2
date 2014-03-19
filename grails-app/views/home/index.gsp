<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />

<r:require module="selectable"/>

<c:jsCSRFToken keys="addEntryCSRF, getPeopleDataCSRF, getListDataCSRF, autocompleteDataCSRF, listTagsAndTagGroupsCSRF,
showTagGroupCSRF, createTagGroupCSRF, deleteTagGroupCSRF, addTagToTagGroupCSRF, deleteGhostEntryDataCSRF, deleteEntryDataCSRF, updateEntrySDataCSRF,
removeTagFromTagGroupCSRF, addTagGroupToTagGroupCSRF, removeTagGroupFromTagGroupCSRF, activateGhostEntryCSRF, pingDataCSRF" />

<r:script>

$.datepicker._gotoToday = function(id) {
    var target = $(id);
    var inst = this._getInst(target[0]);
    if (this._get(inst, 'gotoCurrent') && inst.currentDay) {
            inst.selectedDay = inst.currentDay;
            inst.drawMonth = inst.selectedMonth = inst.currentMonth;
            inst.drawYear = inst.selectedYear = inst.currentYear;
    }
    else {
            var date = new Date();
            inst.selectedDay = date.getDate();
            inst.drawMonth = inst.selectedMonth = date.getMonth();
            inst.drawYear = inst.selectedYear = date.getFullYear();
            // the below two lines are new
            this._setDateDatepicker(target, date);
            this._selectDate(id, this._getDateDatepicker(target));
    }
    this._notifyChange(inst);
    this._adjustDate(target);
}

var defaultToNow = true;
var timeAfterTag = <g:if test="${prefs['displayTimeAfterTag']}">true</g:if><g:else>false</g:else>;

var cachedDate, cachedDateUTC;
var timeZoneName;

function cacheDate() {
	cachedDate = $("input#datepicker").datepicker('getDate');
	cachedDateUTC = cachedDate.toUTCString();
	timeZoneName = jstz.determine().name();
}

var currentTimeUTC;

function cacheNow() {
	cacheDate();
	var now = new Date();
	currentTimeUTC = now.toUTCString();
}

function changeDate(amount) {
	var $datepicker = $("#datepicker");
	var currentDate = $datepicker.datepicker('getDate');
	$datepicker.datepicker('setDate', new Date(currentDate.getTime() + amount * 86400000));
	refreshPage();
}

function refreshPage() {
	cacheNow();
	
	$.getJSON("/home/getListData?date="+ cachedDateUTC + "&currentTime=" + currentTimeUTC + "&userId=" + currentUserId + "&timeZoneName=" + timeZoneName + "&callback=?",
		getCSRFPreventionObject("getListDataCSRF"),
		function(entries){
			if (checkData(entries))
				refreshEntries(entries);
		});
	tagList.load();
}
var tagList;
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
var CONTINUOUS_BIT = 0x100;
var GHOST_BIT = 0x200;
var CONCRETEGHOST_BIT = 0x400;
var TIMED_BIT = 0x1 | 0x2 | 0x4;
var REPEAT_BIT = 0x1 | 0x2;
var REMIND_BIT = 0x4;

function displayEntry(entry, isUpdating, args) {
	var id = entry.id,
		date = entry.date,
		datePrecisionSecs = entry.datePrecisionSecs,
		description = entry.description,
		amount = entry.amount,
		amountPrecision = entry.amountPrecision,
		units = entry.units,
		comment = entry.comment,
		classes = "entry",
		$entryToReplace, $appendAfterEntry;
		
	if (args && args instanceof Object) {
		if (args.replaceEntry) {
			$entryToReplace = $(args.replaceEntry);
		}
		if (args.appendAfterEntry) {
			$appendAfterEntry = $(args.appendAfterEntry);
		}
	}

	var isGhost = false, isConcreteGhost = false, isAnyGhost = false, isContinuous = false, isTimed = false, isRepeat = false, isRemind = false;
	if (entry.repeatType) {
		if ((entry.repeatType & GHOST_BIT) != 0) {
			isGhost = true;
			isAnyGhost = true;
			classes += " ghost anyghost";
		}
		if ((entry.repeatType & CONCRETEGHOST_BIT) != 0) {
			isConcreteGhost = true;
			isAnyGhost = true;
			classes += " concreteghost anyghost";
		}
		if ((entry.repeatType & CONTINUOUS_BIT) != 0) {
			isContinuous = true;
			classes += " continuous"
		}
		if ((entry.repeatType & TIMED_BIT) != 0) {
			isTimed = true;
			classes += " timedrepeat"
		}
		if ((entry.repeatType & REPEAT_BIT) != 0) {
			isRepeat = true;
		}
		if ((entry.repeatType & REMIND_BIT) != 0) {
			isRemind = true;
		}
	}

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
	var selectStart = (timeAfterTag ? 0 : dateStr.length) + description.length + 1 + (formattedAmount.length == 0 ? 1 : 0);
	var selectEnd = selectStart + formattedAmount.length - 1;
	entrySelectData[id] = [selectStart, selectEnd, formattedAmount == 0]; // if third item is true, insert extra space at cursor
	
	var innerHTMLContent = '<span class="content-wrapper">' + (timeAfterTag ? '' : '<span class="entryTime">' + escapehtml(dateStr) + '</span>') + '<span class="entryDescription">'
			+ escapehtml(description) + '</span>' + '<span class="entryAmount">' + escapehtml(formattedAmount) + '</span>'
			+ '<span class="entryUnits">' + escapehtml(formatUnits(units)) + '</span>' + (timeAfterTag ? '<span class="entryTime">'
			+ escapehtml(dateStr) + '</span>' : '') + (comment != '' ? ' ' + '<span class="' + (comment.startsWith('repeat') || comment.startsWith('daily') || comment.startsWith('weekly') || comment.startsWith('remind') ? 'entryRepeat' : 'entryComment') + '">' + escapehtml(comment) + '</span>' : '')
			+ '</span><a href="#" style="padding-left:0;" class="entryDelete entryNoBlur" id="entrydelid' + id + '" onclick="deleteEntryId(' + id + ')"><img width="12" height="12" src="/images/x.gif"></a>';

	if(isUpdating) {
		$("#entry0 li#entryid" + id).html(innerHTMLContent);
	} else {
		var newEntryContent = '<li id="entryid' + id + '" class="' + classes + '">' + innerHTMLContent + '</li>';
		if ($entryToReplace) {
			$entryToReplace.replaceWith(newEntryContent);
		} else if ($appendAfterEntry) {
			$appendAfterEntry.after(newEntryContent);
		} else {
			$("#entry0").append(newEntryContent);
		}
	}
	var data = {entry: entry, entryId:id, isGhost:isGhost, isConcreteGhost:isConcreteGhost, isAnyGhost:isAnyGhost, isContinuous:isContinuous,
			isTimed:isTimed, isRepeat:isRepeat, isRemind:isRemind};
	$("#entry0 li#entryid" + id).data(data);
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

function getEntryElement(entryId) {
	return $("li#entryid" + entryId);
}

function toggleSuffix($control, suffix) {
	var text = $control.val();
	
	if (text.endsWith(" repeat")) {
		text = text.substr(0, text.length - 7);
		$control.val(text);
		
		if (suffix == "repeat")
			return text.length > 0;
	}
	if (text.endsWith(" remind")) {
		text = text.substr(0, text.length - 7);
		$control.val(text);
		
		if (suffix == "remind")
			return text.length > 0;
	}
	if (text.endsWith(" pinned")) {
		text = text.substr(0, text.length - 7);
		$control.val(text);
		
		if (suffix == "pinned")
			return text.length > 0;
	}
	
	$control.val(text + " " + suffix);
	
	return text.length > 0;
}

function modifyEdit(suffix) {
	var $control = $('#tagTextInput');
	//$control.data('cancelBlur', true);
	toggleSuffix($control, suffix);
	/*if (toggleSuffix($control, suffix)) {
		var $selectee = $control.parents("li");
		unselecting($selectee);
		selected($selectee, false);
	}*/
}

function modifyInput(suffix) {
	initInput();
	toggleSuffix($('#input0'), suffix);
	//if (toggleSuffix($('#input0'), suffix))
	//	processInput();
}

function deleteGhost($entryToDelete, entryId, allFuture) {
	$.getJSON(makeGetUrl("deleteGhostEntryData"), makeGetArgs(getCSRFPreventionObject("deleteGhostEntryDataCSRF", {entryId:entryId,
			all:(allFuture ? "true" : "false"), date:cachedDateUTC})),
		function(ret) {
			if (checkData(ret, 'success', "Error deleting entry")) {
				$entryToDelete.remove();
			}
		});
}

function deleteEntryId(entryId) {
	cacheNow();
	
	if (entryId == undefined) {
		showAlert("Please select entry you wish to delete");
		return false;
	}
	var $entryToDelete = getEntryElement(entryId);
	if ($entryToDelete.data("isTimed") || $entryToDelete.data("isGhost")) {
		if ($entryToDelete.data("isContinuous")) {
			deleteGhost($entryToDelete, entryId, true);
		} else {
			showAB("Delete just this one event or also future events?", "One", "Future", function() {
					deleteGhost($entryToDelete, entryId, false);
				}, function() {
					deleteGhost($entryToDelete, entryId, true);
				});
		}
	} else {
		cacheNow();
		$.getJSON("/home/deleteEntrySData?entryId=" + entryId
				+ "&currentTime=" + currentTimeUTC + "&baseDate=" + cachedDateUTC
				+ "&timeZoneName=" + timeZoneName + "&displayDate=" + cachedDateUTC + "&"
				+ getCSRFPreventionURI("deleteEntryDataCSRF") + "&callback=?",
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

function doUpdateEntry(entryId, text, defaultToNow, allFuture) {
	cacheNow();
	$.getJSON("/home/updateEntrySData?entryId=" + entryId
			+ "&currentTime=" + currentTimeUTC + "&text=" + escape(text) + "&baseDate="
			+ cachedDateUTC + "&timeZoneName=" + timeZoneName + "&defaultToNow=" + (defaultToNow ? '1':'0') + "&"
			+ getCSRFPreventionURI("updateEntrySDataCSRF") + "&allFuture=" + (allFuture? '1':'0') + "&callback=?",
		function(entries){
			if (checkData(entries, 'success', "Error updating entry")) {
				tagList.load();
				refreshEntries(entries[0]);
				//$.each(entries[0], function(index, entry) {
					/**
					 * Finding only that entry which is recently updated, and
					 * refreshing only that entry in UI.
					 */
				/*	if(entry.id == entryId) {
						displayEntry(entry, true);
					}
				}) */
				updateAutocomplete(entries[1][0], entries[1][1], entries[1][2], entries[1][3]);
				if (entries[2] != null)
					updateAutocomplete(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
			}
		});
}

function updateEntry(entryId, text, defaultToNow) {
	var $oldEntry = getEntryElement(entryId);
	$(".content-wrapper", $oldEntry).html(text);

	if (($oldEntry.data("isRepeat") && (!$oldEntry.data("isRemind"))) || $oldEntry.data("isGhost")) {
		showAB("Update just this one event or also future events?", "One", "Future", function() {
				doUpdateEntry(entryId, text, defaultToNow, false);
			}, function() {
				doUpdateEntry(entryId, text, defaultToNow, true);
			});
	} else
		doUpdateEntry(entryId, text, defaultToNow, true);
}

function addEntry(userId, text, defaultToNow) {
	cacheNow();
	
	$.getJSON("/home/addEntrySData?currentTime=" + currentTimeUTC
			+ "&userId=" + userId + "&text=" + escape(text) + "&baseDate=" + cachedDateUTC
			+ "&timeZoneName=" + timeZoneName + "&defaultToNow=" + (defaultToNow ? '1':'0') + "&"
			+ getCSRFPreventionURI("addEntryCSRF") + "&callback=?",
			function(entries){
				if (checkData(entries, 'success', "Error adding entry")) {
					if (entries[1] != null) {
						showAlert(entries[1]);
					}
					tagList.load();
					refreshEntries(entries[0]);
					updateAutocomplete(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
				}
			});
}

function initInput() {
	if (!$("#input0").data('entryTextSet')) {
		setEntryText('');
	}
}

function processInput() {
	var $field = $("#input0");
	$field.autocomplete("close");
	var text = $field.val();
	if (text == "") return; // no entry data
	$field.val("");
	addEntry(currentUserId, text, defaultToNow);
	return true;
}

function setEntryText(text, startSelect, endSelect) {
	var $inp = $("#input0");
	$inp.autocomplete("close");
	$inp.val(text);
	$inp.css('color','#000000');
	if (startSelect) {
		$inp.selectRange(startSelect, endSelect);
	}
	$inp.focus();
	$inp.data("entryTextSet", true);
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

/*
 * Used to unselect an entry, or called when entry is
 * being unselected.
 */
function unselecting($unselectee, doNotUpdate) {
	if ($unselectee.data('entryIsSelected') == 1) {
		var $textInput = $("#tagTextInput");
		/*if ($textInput.data('cancelBlur')) {
			$textInput.data('cancelBlur', false);
			return;
		}*/
		$unselectee.removeClass('ui-selected');
		$unselectee.data('entryIsSelected', 0);
		$("a.entryDelete", $unselectee).hide();
		checkAndUpdateEntry($unselectee);
		currentEntryId = null;
	}
}

/*
 * Gets called on selection of the entry, or used to select an entry. If forceUpdate true, always send update whether text changed or not.
 */
function selected($selectee, forceUpdate) {
	var state = $selectee.data('entryIsSelected');
	$selectee.data('forceUpdate', forceUpdate);
	var $contentWrapper = $selectee.find(".content-wrapper");
	if ($("#tagTextInput").size() == 1) return;
	$selectee.siblings().removeClass("ui-selected").data('entryIsSelected', 0);

	if (state == undefined || state == 0) {
		$selectee.data('contentHTML', $contentWrapper.html()); // store original HTML for later restoration
		currentEntryId = $selectee.data("entry-id");
		$selectee.data('entryIsSelected', 1);
		$selectee.addClass('ui-selected');
		$("#entrydelid" + currentEntryId).css('display', 'inline');

		var entryText = $selectee.text();
		var selectRange = entrySelectData[currentEntryId];
		if (selectRange != undefined) {
			if (selectRange[2]) { // insert space at selectRange[0]
				entryText = entryText.substr(0, selectRange[0] - 1) + " " + entryText.substr(selectRange[0] - 1);
			}
		}
		$selectee.data('originalText', entryText); // store entry text for comparison
		$contentWrapper.hide();
		$selectee.append('<span id="tagTextEdit" style="display:inline"><input type="text" id="tagTextInput" style="margin: 2px; width: 85%;"></input>'
				+ '<img class="entryModify entryNoBlur" src="/images/repeat.png" id="tagEditRepeat" style="width:14px;height:14px;padding-left:1px;padding-top:2px;">'
				+ '<img class="entryModify entryNoBlur" src="/images/remind.png" id="tagEditRemind" style="width:14px;height:14px;padding-left:1px;padding-top:2px;">'
				+ '<img class="entryModify entryNoBlur" src="/images/pin.png" id="tagEditPinned" style="width:14px;height:14px;padding-left:1px;padding-top:2px;"></span>');

		$("#tagEditRepeat").off("mousedown");
		$("#tagEditRemind").off("mousedown");
		$("#tagEditPinned").off("mousedown");
		
		$("#tagEditRepeat").on("mousedown", function(e) {
			modifyEdit('repeat');
		});
		$("#tagEditRemind").on("mousedown", function(e) {
			modifyEdit('remind');
		});
		$("#tagEditPinned").on("mousedown", function(e) {
			modifyEdit('pinned');
		});
		
		// Adding logic to prevent blur from activating when clicking on certain controls
		$("#tagTextInput").bind('focus', function() {
			$(document).bind('mousedown', function(e) {
				var $target = $(e.target);
				if ($target.closest('#tagTextEdit').length) return;
				if (! $target.closest('.entryNoBlur').length) {
					$selectee.data('entryIsSelected', 0);
					var $unselectee = $target.parents("li");
					checkAndUpdateEntry($unselectee);
				}
				if (! $target.closest('.entryModify').length)
					$(document).unbind('mousedown', arguments.callee);
			})
		});
		
		$("#tagTextInput").on("keyup", function(e) {
			var $selectee = $(this).parents("li");
			if (e.keyCode == 13) {	// Enter pressed
				unselecting($selectee);
			} else if(e.keyCode == 27) {	// Esc pressed
				unselecting($selectee, true);
			}
		});
		$("#tagTextInput").val(entryText).focus();
		$("#tagTextInput").data('entryTextSet', true);
		if(selectRange) {
			$("#tagTextInput").selectRange(selectRange[0], selectRange[1]);
		}
	}
}
/**
 * Sees to check if text is different from original text.
 * IF different than call updateEntry() method to notify
 * server and update in UI.
 */
function checkAndUpdateEntry($unselectee) {
	var $contentWrapper = $unselectee.find(".content-wrapper");
	
	var newText = $("input#tagTextInput").val();
	var $oldEntry = getEntryElement(currentEntryId);

	if (($oldEntry.data('originalText') == newText) && (!$unselectee.data('forceUpdate'))) {
		var $contentWrapper = $oldEntry.find(".content-wrapper");
		$contentWrapper.html($oldEntry.data('contentHTML'));
		$contentWrapper.show();
	} else {
		$contentWrapper.show();
		$unselectee.data('forceUpdate', 0);
		$contentWrapper
				.append("&nbsp;&nbsp;<img src='../images/spinner.gif' />");
		updateEntry(currentEntryId, newText, defaultToNow);
	}
	
	$("#tagTextEdit").remove();
}

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

	var $datepicker = $("#datepicker");

	currentDate = new Date();
	if (${showTime} > 0)
		currentDate = new Date(${showTime});

	$datepicker
		.datepicker({defaultDate: currentDate, dateFormat: 'DD MM dd, yy', showButtonPanel: true})
		.val($.datepicker.formatDate('DD MM dd, yy', currentDate))
		.datepicker("hide")
		.change(function () {
			refreshPage();
			localStorage['stateStored'] = "2";
			localStorage['currentDate'] = $.toJSON($datepicker.datepicker('getDate'));
		})
		.click(function() {
			$('button.ui-datepicker-current').removeClass('ui-priority-secondary').addClass('ui-priority-primary');
		});
		 
	$(document).on("click", ".ui-datepicker-buttonpane button.ui-datepicker-current", function() {
		$datepickerField.datepicker("setDate", new Date()).datepicker("hide").trigger("change").blur();
	})
	
	$("#input0").off("click");
	$("#input0").on("click", function(e) {
		if (!$("#input0").data('entryTextSet'))
			setEntryText('');
	});
	$("#input0").keyup(function(e) {
		if (e.keyCode == 13) {
			processInput();
		}
	});
	$("#entry0").listable({cancel: 'a,input,li.entry.ghost'});
	$("#entry0").off("listableselected");
	/*
	$("#entry0").off("listableunselecting");
	$("#entry0").on("listableunselecting", function(e, ui) {
		var $unselectee = $("#" + ui.unselecting.id);
		unselecting($unselectee);
	});*/
	$("#entry0").on("listableselected", function(e, ui) {
		var $selectee = $("#" + ui.selected.id);
		selected($selectee, false);
	});
	
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
			selected($selectee, false);
		}
		return false;
	});
	$(document).on("click", "li.entry.ghost", function(e, doNotSelectEntry) {
		if(e.target.nodeName && $(e.target).closest("a,img").length) {
			// Not doing anything when delete icon clicked like 'cancel' option in selectable.
			return false;
		}
		cacheNow();
		var $ghostEntry = $(this);
		var entryId = $ghostEntry.data("entry-id");
		var isContinuous = $ghostEntry.data("isContinuous");
		$.getJSON("/home/activateGhostEntry?entryId=" + entryId + "&date=" + cachedDateUTC + "&currentTime=" + currentTimeUTC + "&timeZoneName=" + timeZoneName + "&"
				+ getCSRFPreventionURI("activateGhostEntryCSRF") + "&callback=?",
				function(newEntry) {
					if (checkData(newEntry)) {
						var newEntryId = newEntry.id;
						if (isContinuous) {
							var $lastContinuousGhostEntry = $("#entry0 li.entry.ghost.continuous:last");
							displayEntry(newEntry, false, {appendAfterEntry: $lastContinuousGhostEntry});
						} else {
							displayEntry(newEntry, false, {replaceEntry: $ghostEntry});
						}
						var $newEntry = $("li#entryid" + newEntryId);
						if (!doNotSelectEntry) {
							selected($newEntry, true);
						}
						tagList.load();
					}
				});
	})

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
<body class="track-page">

	<div id="alert-message" class="hide">
		<p><p>
		<div id="alert-message-text"></div>
	</div>

	<div class="row custom-row">
		<g:render template="/tag/tagListWidget" model="[header: true]" />
		<div class="col-xs-9 floating-column entry-header-container">
			<div class="red-header date-controls">
				<h1 class="clearfix">
					<a class="back icon-triangle icon-triangle-left" href="#" onclick="changeDate(-1);">
						<span class="hide">back</span>
					</a>
					<span class="date"><input id="datepicker" type="text" value="" /></span>
					<a class="next  icon-triangle icon-triangle-right" href="#" onclick="changeDate(1);">
						<span class="hide">next</span>
					</a>
				</h1>
			</div>
		</div>
	</div>

<!-- MAIN -->
<div class="row custom-row">
	<!-- RIGHT NAV -->
	<g:render template="/tag/tagListWidget" />
	<!-- /RIGHT NAV -->
<div class="col-xs-9 floating-column entry-container">
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
		<div class="border-separator"></div>

		<div id="recordList">
			<ol id="entry0">
			</ol>
		</div>
	</div>
</div>
</div>
<!-- /MAIN -->

</div>

</body>
</html>
