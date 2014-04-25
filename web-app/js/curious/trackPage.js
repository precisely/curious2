var isTodayOrLater, cachedDate, cachedDateUTC, timeZoneName, currentTimeUTC, tagList, currentDrag, currentDragTag,
entrySelectData, freqTagList, dateTagList,
dayDuration = 86400000,
entryEventStack = [],
defaultToNow = true;

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
	$("#entry0").html('');
}

$(document).mousemove(function(e) {
	if (currentDrag != null) {
		currentDrag.setAbsolute({ x: e.pageX - 6, y: e.pageY - 6 });
	}
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
	toggleSuffix($control, suffix);
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
			updateAutocomplete(entries[1][0], entries[1][1], entries[1][2], entries[1][3]);
			if (entries[2] != null) {
				updateAutocomplete(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
			}
		}
	});
}

function updateEntry(entryId, text, defaultToNow) {
	var $oldEntry = getEntryElement(entryId);

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

//static autocomplete
//changes to the autocomplete code should also get put into the index.gsp for the mobile app
var autoCache = {};

function doLogout() {
	callLogoutCallbacks();
}

/**
 * Used to un-select and entry. Removes the entry edit text field
 * & displays the original content back.
 */
function unselectEntry($unselectee, displayNewText, displaySpinner) {
	console.log('Unselect Entry', $unselectee.attr('id'));

	var $contentWrapper = $unselectee.find(".content-wrapper");
	var displayText = $unselectee.data('contentHTML');

	if (displayNewText) {
		var newText = $("input#tagTextInput").val();
		if (newText) {
			displayText = newText;
		}
	}

	$contentWrapper.html(displayText);
	$contentWrapper.show();
	if (displaySpinner) {
		$contentWrapper.append(" &nbsp;<img src='/images/spinner.gif' />");
	}

	$("#tagTextEdit").remove();
	$unselectee.removeClass('ui-selected');
}

/**
 * Gets called on selection of the entry, or used to select an entry. If forceUpdate true,
 * always send update whether text changed or not.
 */
function selectEntry($selectee, forceUpdate) {
	console.debug('Entry selected.');

	$selectee.data('forceUpdate', forceUpdate);
	$selectee.siblings().removeClass("ui-selected");
	var $contentWrapper = $selectee.find(".content-wrapper");

	// TODO Remove if condition. Not removing because to view git diff properly.
	if (true) {
		$selectee.data('contentHTML', $contentWrapper.html()); // store original HTML for later restoration
		var currentEntryId = $selectee.data("entry-id");
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
				var cancelElements = 'a.deleteEntry,img,input#tagTextInput,#tagTextEdit,.entryNoBlur,.entryModify';
				if ($target.closest(cancelElements).length > 0) {
					return;
				}
				entryEventStack.push({name: 'blur', target: $target});

				// Will force above callback to call only once. (Unregister previous binding)
				$(document).unbind('mousedown', arguments.callee);
				checkAndUpdateEntry($selectee);
			});
		});

		$("#tagTextInput").on("keyup", function(e) {
			var $selectee = $(this).parents("li");
			var entryData = $selectee.data();
			if (e.keyCode == 13) {	// Enter pressed
				checkAndUpdateEntry($selectee);
			} else if (e.keyCode == 27) {	// Esc pressed
				unselectEntry($selectee);
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
function checkAndUpdateEntry($unselectee) {
	if ($unselectee == undefined) {
		console.warn("Undefined unselectee.");
		return;
	}
	console.debug('Check and update entry:', $unselectee.attr('id'));

	var newText = $("input#tagTextInput").val();
	if (newText == undefined) {
		console.warn("Undefined new text");
		return;
	}
	var currentEntryId = $unselectee.data("entry-id");

	if ($unselectee.data('isContinuous')) {
		console.debug('Is a continuous entry:', $unselectee.attr('id'));
		addEntry(currentUserId, newText, defaultToNow);
		unselectEntry($unselectee, false, true);
	} else if (!$unselectee.data('isRemind') && $unselectee.data('originalText') == newText) {
		console.debug('Is not remind & no change in entry.');
		unselectEntry($unselectee);
	} else {
		console.log('Either remind or change in entry.');
		unselectEntry($unselectee, true, true);
		updateEntry(currentEntryId, newText, defaultToNow);
	}
}

$(document).on("click", "li.entry:not(.ui-selected)", function(e, doNotSelectEntry) {
	var latestEvent = entryEventStack.pop();

	/**
	 * Checking if this click events if when an entry is selected & active edit entry is blurred.
	 * (Since blur event triggeres before click event or mousedown event triggers before blur event).
	 */
	if (latestEvent && latestEvent.name === "blur") {
		if (latestEvent.target.closest('li.entry.ui-selected')) {
			console.info('Entry clicked. (Doing nothing, another entry already selected)');
			return false;
		}
	}

	// Useless. Making sure to do not activate a new entry if one is selected or being unselected.
	if ($('li.entry.ui-selected').length > 0) {
		console.warn('Entry clicked. (Doing nothing, another entry already selected)');
		unselectEntry($('li.entry.ui-selected'), true);
		return false;
	}
	console.debug('Entry clicked.');

	var $selectee = $(this);
	selectEntry($selectee, false);
});

/**
 * Global click handler to popup event stack if clicked other than on entry.
 * 
 * @Note Must be defined after above click handler (entry click handler) to
 * call that handler before this handler.
 */
$(document).on("click", function(e) {
	entryEventStack.pop();
});

$(function() {
	initTemplate();
	initAutocomplete();
	initTagListWidget();

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
	})
	.click(function() {
		$('button.ui-datepicker-current').removeClass('ui-priority-secondary').addClass('ui-priority-primary');
	});

	$(document).on("click", ".ui-datepicker-buttonpane button.ui-datepicker-current", function() {
		$datepickerField.datepicker("setDate", new Date()).datepicker("hide").trigger("change").blur();
	})

	$("#input0")
	.on("click", function(e) {
		if (!$("#input0").data('entryTextSet')) {
			setEntryText('');
		}
	})
	.keyup(function(e) {
		if (e.keyCode == 13) {	// Enter pressed.
			processInput();
		}
	});

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
			unselectEntry($unselectee);
			selectEntry($selectee, false);
		}
		return false;
	});

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