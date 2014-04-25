var isTodayOrLater, cachedDate, cachedDateUTC, timeZoneName, currentTimeUTC, tagList, currentDrag, currentDragTag,
entrySelectData, freqTagList, dateTagList,
currentEntryId = undefined,
dayDuration = 86400000,
defaultToNow = true;

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

function cacheDate() {
	var now = new Date();
	cachedDate = $("input#datepicker").datepicker('getDate');
	isTodayOrLater = now.getTime() - (24 * 60 * 60000) < cachedDate.getTime();
	cachedDateUTC = cachedDate.toUTCString();
	timeZoneName = jstz.determine().name();
}

function cacheNow() {
	cacheDate();
	var now = new Date();
	currentTimeUTC = now.toUTCString();
}

function changeDate(amount) {
	var $datepicker = $("#datepicker");
	var currentDay = $datepicker.datepicker('getDate');
	$datepicker.datepicker('setDate', new Date(currentDay.getTime() + amount * 86400000));
	refreshPage();
}

function refreshPage() {
	cacheNow();

	queueJSON("getting entries", "/home/getListData?date="+ cachedDateUTC + "&currentTime=" + currentTimeUTC + "&userId=" + currentUserId + "&timeZoneName=" + timeZoneName + "&callback=?",
			getCSRFPreventionObject("getListDataCSRF"),
			function(entries){
		if (checkData(entries))
			refreshEntries(entries);
	});
	tagList.load();
}

function clearEntries() {
	currentEntryId = undefined;
	$("#entry0").html('');
}

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
		var repeatType = entry.repeatType;
		if (RepeatType.isGhost(repeatType)) {
			isGhost = true;
			isAnyGhost = true;
			classes += " ghost anyghost";
		}
		if (RepeatType.isConcreteGhost(repeatType)) {
			isConcreteGhost = true;
			isAnyGhost = true;
			classes += " concreteghost anyghost";
		}
		if (RepeatType.isContinuous(repeatType)) {
			isContinuous = true;
			classes += " continuous"
		}
		if (RepeatType.isTimed(repeatType)) {
			isTimed = true;
			classes += " timedrepeat"
		}
		if (RepeatType.isRepeat(repeatType)) {
			isRepeat = true;
			classes += " repeat"
		}
		if (RepeatType.isRemind(repeatType)) {
			isRemind = true;
			classes += " remind"
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
	entrySelectData[id] = [selectStart, selectEnd, amountPrecision < 0 && amount != null]; // if third item is true, insert extra space at cursor

	var innerHTMLContent = '<span class="content-wrapper">' + (timeAfterTag ? '' : '<span class="entryTime">' + escapehtml(dateStr) + '</span>') + '<span class="entryDescription">'
	+ escapehtml(description) + '</span>' + '<span class="entryAmount">' + escapehtml(formattedAmount) + '</span>'
	+ '<span class="entryUnits">' + escapehtml(formatUnits(units)) + '</span>' + (timeAfterTag ? '<span class="entryTime">'
			+ escapehtml(dateStr) + '</span>' : '') + (comment != '' ? ' ' + '<span class="' + (comment.startsWith('repeat') || comment.startsWith('daily') || comment.startsWith('weekly') || comment.startsWith('remind') ? 'entryRepeat' : 'entryComment') + '">' + escapehtml(comment) + '</span>' : '')
			+ '</span><a href="#" style="padding-left:0;" class="entryDelete entryNoBlur" id="entrydelid' + id + '" onclick="deleteEntryId(' + id + ')"><img width="12" height="12" src="/images/x.gif"></a>';

	if (isUpdating) {
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

		if (suffix == "repeat") {
			$control.selectRange(text.length, text.length);
			$control.focus();
			return text.length > 0;
		}
	}
	if (text.endsWith(" remind")) {
		text = text.substr(0, text.length - 7);
		$control.val(text);

		if (suffix == "remind") {
			$control.selectRange(text.length, text.length);
			$control.focus();
			return text.length > 0;
		}
	}
	if (text.endsWith(" pinned")) {
		text = text.substr(0, text.length - 7);
		$control.val(text);

		if (suffix == "pinned") {
			$control.selectRange(text.length, text.length);
			$control.focus();
			return text.length > 0;
		}
	}

	var retVal = text.length > 0;	
	text = text + " " + suffix;
	$control.val(text);
	$control.selectRange(text.length, text.length);
	$control.focus();

	return retVal;
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
	if (toggleSuffix($('#input0'), suffix)) {
		processInput();
	}
}

function deleteGhost($entryToDelete, entryId, allFuture) {
	queueJSON("deleting entry", makeGetUrl("deleteGhostEntryData"), makeGetArgs(getCSRFPreventionObject("deleteGhostEntryDataCSRF", {entryId:entryId,
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
		if ($entryToDelete.data("isContinuous") || isTodayOrLater) {
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
		queueJSON("deleting entry", "/home/deleteEntrySData?entryId=" + entryId
				+ "&currentTime=" + currentTimeUTC + "&baseDate=" + cachedDateUTC
				+ "&timeZoneName=" + timeZoneName + "&displayDate=" + cachedDateUTC + "&"
				+ getCSRFPreventionURI("deleteEntryDataCSRF") + "&callback=?",
				function(entries) {
			if (checkData(entries, 'success', "Error deleting entry")) {
				tagList.load();
				refreshEntries(entries[0]);
				updateAutocomplete(entries[1][0], entries[1][1], entries[1][2], entries[1][3]);
				if (entries[2] != null) {
					updateAutocomplete(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
				}
			}
		});
	}
}

function deleteCurrentEntry() {
	deleteEntryId(currentEntryId);
}

function doUpdateEntry(entryId, text, defaultToNow, allFuture) {
	cacheNow();
	queueJSON("updating entry", "/home/updateEntrySData?entryId=" + entryId
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
			/*	if (entry.id == entryId) {
					displayEntry(entry, true);
				}
			}) */
			updateAutocomplete(entries[1][0], entries[1][1], entries[1][2], entries[1][3]);
			if (entries[2] != null) {
				updateAutocomplete(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
			}
		}
	});
}

function updateEntry(entryId, text, defaultToNow) {
	var $oldEntry = getEntryElement(entryId);
	$(".content-wrapper", $oldEntry).html(text);

	if ((($oldEntry.data("isRepeat") && (!$oldEntry.data("isRemind"))) || $oldEntry.data("isGhost")) && (!isTodayOrLater)) {
		showAB("Update just this one event or also future events?", "One", "Future", function() {
			doUpdateEntry(entryId, text, defaultToNow, false);
		}, function() {
			doUpdateEntry(entryId, text, defaultToNow, true);
		});
	} else {
		doUpdateEntry(entryId, text, defaultToNow, true);
	}
}

function addEntry(userId, text, defaultToNow) {
	cacheNow();

	queueJSON("adding new entry", "/home/addEntrySData?currentTime=" + currentTimeUTC
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

//dynamic autocomplete
//var lastAutoXhr;

//static autocomplete
//changes to the autocomplete code should also get put into the index.gsp for the mobile app
var autoCache = {};

function doLogout() {
	callLogoutCallbacks();
}

/**
 * Used to unselect an entry, or called when entry is
 * being unselected.
 */
function unselecting($unselectee, doNotUpdate) {
	console.log('Unselecting entry.');
	if (doNotUpdate) {
		showEntryContent($unselectee);
		$("#tagTextEdit").remove();
	} else {
		checkAndUpdateEntry($unselectee, doNotUpdate);
	}
	$unselectee.removeClass('ui-selected');
	currentEntryId = null;
}

function showEntryContent($entry) {
	var $contentWrapper = $entry.find(".content-wrapper");
	$contentWrapper.html($entry.data('contentHTML'));
	$contentWrapper.show();
}

/**
 * Gets called on selection of the entry, or used to select an entry. If forceUpdate true,
 * always send update whether text changed or not.
 */
function selected($selectee, forceUpdate) {
	console.debug('Entry selected.');

	$selectee.data('forceUpdate', forceUpdate);
	$selectee.siblings().removeClass("ui-selected");
	var $contentWrapper = $selectee.find(".content-wrapper");

	// TODO Remove this if condition.
	if (true) {
		$selectee.data('contentHTML', $contentWrapper.html()); // store original HTML for later restoration
		currentEntryId = $selectee.data("entry-id");
		$selectee.addClass('ui-selected');

		var entryText = $selectee.text();
		var selectRange = entrySelectData[currentEntryId];
		if (selectRange != undefined) {
			if (selectRange[2]) { // insert space at selectRange[0]
				entryText = entryText.substr(0, selectRange[0] - 1) + " " + entryText.substr(selectRange[0] - 1);
			}
		}
		$selectee.data('originalText', entryText); // store entry text for comparison
		$contentWrapper.hide();
		$selectee.append('<span id="tagTextEdit"><input type="text" id="tagTextInput" style="margin: 2px; width: calc(100% - 75px);"></input>'
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

		$("#tagTextInput").bind('focus', function() {
			$(document).bind('mousedown', function(e) {
				var $target = $(e.target);
				var cancelElements = 'a.deleteEntry,img,input#tagTextEdit,.entryNoBlur,.entryModify';
				if ($target.closest(cancelElements).length > 0) {
					return;
				}
				var isClickedOnOtherEntry = $target.closest('li.entry').length > 0;

				// Will force above callback to call only once.
				$(document).unbind('mousedown', arguments.callee);
				var $unselectee = $('li.entry.ui-selected');
				checkAndUpdateEntry($unselectee, false, isClickedOnOtherEntry);
			});
		});

		$("#tagTextInput").on("keyup", function(e) {
			var $selectee = $(this).parents("li");
			var entryData = $selectee.data();
			if (e.keyCode == 13) {	// Enter pressed
				unselecting($selectee);
			} else if (e.keyCode == 27) {	// Esc pressed
				unselecting($selectee, true);
			}
		});

		$("#tagTextInput").val(entryText).focus();
		$("#tagTextInput").data('entryTextSet', true);
		if ($selectee.data('isContinuous'))
			toggleSuffix($("#tagTextInput"), 'pinned');

		if (selectRange) {
			$("#tagTextInput").selectRange(selectRange[0], selectRange[1]);
		}
	}
}
/**
 * Checks if text is different from original text.
 * IF different than call updateEntry() method to notify
 * server and update in UI.
 */
function checkAndUpdateEntry($unselectee, doNotUpdate, doNotUnselectEntry) {
	if ($unselectee == undefined) {
		console.warn("Undefined unselectee.");
		return;
	}
	var $contentWrapper = $unselectee.find(".content-wrapper");

	var newText = $("input#tagTextInput").val();
	if (newText == undefined) {
		console.warn("Undefined new text");
		return;
	}
	var $oldEntry = getEntryElement(currentEntryId);

	if ($oldEntry.data('isContinuous') && (!doNotUpdate)) {
		var $contentWrapper = $oldEntry.find(".content-wrapper");
		$contentWrapper.html($oldEntry.data('contentHTML'));
		$contentWrapper.show();
		addEntry(currentUserId, newText, defaultToNow);
		doNotUpdate = true;
	}

	if ((!$oldEntry.data('isRemind')) &&
			(doNotUpdate || ($oldEntry.data('originalText') == newText) && (!$unselectee.data('forceUpdate')))) {
		if (!doNotUnselectEntry) {
			unselecting($oldEntry, true);
		} else {
			showEntryContent($oldEntry);
		}
	} else {
		$contentWrapper.show();
		$unselectee.data('forceUpdate', 0);
		$contentWrapper.append("&nbsp;&nbsp;<img src='/images/spinner.gif' />");
		updateEntry(currentEntryId, newText, defaultToNow);
	}

	$("#tagTextEdit").remove();
}

function activateEntry($ghostEntry, doNotSelectEntry) {
	console.debug('Activate entry');
	cacheNow();
	var entryId = $ghostEntry.data("entry-id");
	var isContinuous = $ghostEntry.data("isContinuous");
	var text = $ghostEntry.find('input#tagTextInput').val();
	queueJSON("creating entry", "/home/activateGhostEntry?entryId=" + entryId + "&date=" + cachedDateUTC + "&currentTime=" + currentTimeUTC + "&timeZoneName=" + timeZoneName + "&text=" + text + "&"
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
}

$(function(){
	initTemplate();
	initAutocomplete();
	initTagListWidget();

	/*
	 * Commenting whole section, because date evaluated from here
	 * will be overwritten on next lines.
	if (supportsLocalStorage() && localStorage['stateStored'] == "2") {
		currentDate = $.evalJSON(localStorage['currentDate']);
	} else {
		currentDate = new Date();
		localStorage['currentDate'] = $.toJSON(currentDate);
		localStorage['stateStored'] = "2";
	}
	 *
	 */

	$("#input0").val('Enter a tag.  For example: nap at 2pm');
	$("#input0").droppable({
		drop : function(event, ui) {
			var droppedItem = $(ui.draggable[0]).data(DATA_KEY_FOR_ITEM_VIEW).getData();
			setEntryText(droppedItem.description);
		}
	});

	var $datepicker = $("#datepicker");

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
	/*$("#entry0").listable({cancel: 'a,input,.entryNoBlur'});
	$("#entry0").off("listableselected");
	$("#entry0").off("listableunselecting");
	$("#entry0").on("listableunselecting", function(e, ui) {
		var $unselectee = $("#" + ui.unselecting.id);
		unselecting($unselectee);
	});
	$("#entry0").on("listableselected", function(e, ui) {
		var $selectee = $("#" + ui.selected.id);
		selected($selectee, false);
	});*/

	/**
	 * Keycode= 37:left, 38:up, 39:right, 40:down
	 */
	$("#entry0").keydown(function(e) {
		if ($.inArray(e.keyCode, [38, 40]) == -1) {
			return true;
		}
		var $unselectee = $("li.ui-selected", "ol#entry0");
		if (!$unselectee) {
			return false;
		}
		var $selectee;
		if (e.keyCode == 40) { 
			$selectee = $unselectee.next();
		}
		if (e.keyCode == 38) { 
			$selectee = $unselectee.prev();
		}
		if ($selectee) {
			unselecting($unselectee);
			selected($selectee, false);
		}
		return false;
	});

	$(document).on("click", "li.entry", function(e, doNotSelectEntry) {
		if (e.target.nodeName && $(e.target).closest("a,img,input,.entryNoBlur").length) {
			console.debug('Entry clicked. (Doing nothing)');
			// Not doing anything when delete icon clicked like 'cancel' option in selectable.
			return false;
		}

		if ($('li.entry.ui-selected').length > 0) {
			console.info('Entry clicked. (Doing nothing, another entry already selected)');
			unselecting($('li.entry.ui-selected'), true);
			return false;
		}
		console.debug('Entry clicked.');

		var $selectee = $(this);
		/*var entryData = $entry.data();
		if (entryData.isContinuous || (entryData.isGhost && entryData.isRemind)) {
			// Handled by selected event, i.e. selected() method is called.
		} else {
			activateEntry($entry, doNotSelectEntry);
		}*/
		selected($selectee, false);
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

	initTemplate();

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

		refreshPage();
	});
});