/**
 * Entry editor widget
 * 
 * This widget depends on jstz
 * 
 * formatAmount(), escapehtml(), checkData() from curious.js
 * makeGetUrl() from the template or mobileBase.js
 * dateToTimeStr(), getCSRFPreventionObject(), callLogoutCallbacks() from base.js
 * showAlert(), showAB() from webBase.js or mobileBase.js
 */

// widget must be constructed from within $.ready()

function EntryListWidget(tagListWidget, divIds, autocompleteWidget) {
	var self = this;
	
	if (!divIds) divIds = {};
	
	divIds = $.extend({editId:"input0", listId:"entry0", calendarId:"datepicker", dragId:"drag", areaId:"area"}, divIds);

	this.editId = divIds.editId;
	this.listId = divIds.listId;
	this.calendarId = divIds.calendarId;
	this.dragId = divIds.dragId;
	this.tagListWidget = tagListWidget;
	this.tagList = tagListWidget.list;
	if (!autocompleteWidget) {
		autocompleteWidget = new AutocompleteWidget("autocomplete", this.editId);
	}
	
	this.autocompleteWidget = autocompleteWidget;
	
	/* var isTodayOrLater, cachedDate, cachedDateUTC, timeZoneName, currentTimeUTC, tagList, currentDrag, currentDragTag,
	entrySelectData, freqTagList, dateTagList,
	defaultToNow = true; */
	
	var dayDuration = 86400000;
	
	this.defaultToNow = true;

	this.cacheDate = function() {
		var now = new Date();
		this.cachedDate = $("#" + this.calendarId).datepicker('getDate');
		this.isTodayOrLater = now.getTime() - (24 * 60 * 60000) < this.cachedDate.getTime();
		this.cachedDateUTC = this.cachedDate.toUTCString();
		this.timeZoneName = jstz.determine().name();
	}

	this.cacheNow = function() {
		this.cacheDate();
		var now = new Date();
		this.currentTimeUTC = now.toUTCString();
	}

	this.changeDate = function(amount) {
		var $datepicker = $("#" + this.calendarId);
		var currentDay = $datepicker.datepicker('getDate');
		$datepicker.datepicker('setDate', new Date(currentDay.getTime() + amount * 86400000));
		this.refresh();
	}

	this.refresh = function() {
		this.cacheNow();

		queueJSON("getting entries", "/home/getListData?date="+ this.cachedDateUTC + "&currentTime=" + this.currentTimeUTC + "&userId=" + currentUserId + "&timeZoneName=" + this.timeZoneName + "&callback=?",
				getCSRFPreventionObject("getListDataCSRF"),
				function(entries){
			if (checkData(entries))
				self.refreshEntries(entries);
		});
		this.tagList.load();
	}

	this.clearEntries = function() {
		$("#" + this.listId).html('');
	}

	$(document).mousemove(function(e) {
		if (self.currentDrag != null) {
			self.currentDrag.setAbsolute({ x: e.pageX - 6, y: e.pageY - 6 });
		}
	});

	$(document).mouseup(function(e) {
		if (self.currentDrag == null) return;
		$("#" + self.dragId).html('');
		self.currentDrag = null;
		if ($("#" + self.areaId).isUnderEvent(e)) {
			self.setEntryText(self.currentDragTag.description + ' ');
			$("#" + self.editId).focus();
			return;
		}
		self.currentDragTag = null;
	});
	
	this.displayEntry = function(entry, isUpdating, args) {
		var id = entry.id;
		var date = entry.date;
		var datePrecisionSecs = entry.datePrecisionSecs;
		var description = entry.description;
		var comment = entry.comment;
		var classes = "entry full-width " + ((comment != '')?comment:'no-tag') + " ";
		var $entryToReplace, $appendAfterEntry;
		console.log('entry: ',entry);

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

		var diff = date.getTime() - this.cachedDate.getTime();
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
		
		var innerHTMLContent = '<span class="content-wrapper">' + (timeAfterTag ? '' : '<span class="entryTime">' + escapehtml(dateStr) + '</span>') + '<span class="entryDescription">'
				+ escapehtml(description) + '</span>';
		
		var amounts = entry.amounts;
		
		var selectStart = null;
		var selectEnd = null;
		
		var i = 0, iString;
		while ((iString = (i++).toString()) in amounts) {
			var amountEntry = amounts[iString];
			var amount = amountEntry.amount;
			var amountPrecision = amountEntry.amountPrecision;
			var units = amountEntry.units;
			
			var formattedAmount = formatAmount(amount, amountPrecision);
			
			// store first amount for post-selection highlighting
			if (selectStart == null) {
				selectStart = (timeAfterTag ? 0 : dateStr.length) + description.length + 1 + (formattedAmount.length == 0 ? 1 : 0);
				selectEnd = selectStart + formattedAmount.length - 1;
				self.entrySelectData[id] = [selectStart, selectEnd, amountPrecision < 0 && amount != null]; // if third item is true, insert extra space at cursor
			}
			
			innerHTMLContent += '<span class="entryAmount">' + escapehtml(formattedAmount) + '</span>'
					+ '<span class="entryUnits">' + escapehtml(formatUnits(units)) + '</span>'
		}
		
		innerHTMLContent += (timeAfterTag ? '<span class="entryTime">'
				+ escapehtml(dateStr) + '</span>' : '') + (comment != '' ? ' ' + '<br> <span class="' + (comment.startsWith('repeat') || comment.startsWith('daily') || comment.startsWith('weekly') || comment.startsWith('remind') ? 'entryRepeat' : 'entryComment') + '">' + escapehtml(comment) + '</span>' : '')
				+ '</span><button class="edit">Edit</button><a href="#" style="padding-left:0;" class="entryDelete entryNoBlur" id="entrydelid' + this.editId + id + '"><img class="entryModify edit-delete" src="/images/x.png"></a>';

		
		var entryEditItem;
		
		if (isUpdating) {
			entryEditItem = $("#" + this.editId + "entryid" + id);
			entryEditItem.html(innerHTMLContent);
		} else {
			var newEntryContent = '<li id="' + this.editId + "entryid" + id + '" class="' + classes + '">' + innerHTMLContent + '</li>';
			if ($entryToReplace) {
				$entryToReplace.replaceWith(newEntryContent);
			} else if ($appendAfterEntry) {
				$appendAfterEntry.after(newEntryContent);
			} else {
				$("#" + this.listId).append(newEntryContent);
			}
			entryEditItem = $("#" + this.editId + "entryid" + id);
		}
		$("#entrydelid" + this.editId + id).click(function() {
			self.deleteEntryId(id);
		});
		
		var data = {entry: entry, entryId:id, isGhost:isGhost, isConcreteGhost:isConcreteGhost, isAnyGhost:isAnyGhost, isContinuous:isContinuous,
				isTimed:isTimed, isRepeat:isRepeat, isRemind:isRemind};
		entryEditItem.data(data);
	}

	this.displayEntries = function(entries) {
		self.entrySelectData = {};
		jQuery.each(entries, function() {
			self.displayEntry(this, false);
			return true;
		});
	}

	this.refreshEntries = function(entries) {
		this.clearEntries();
		this.displayEntries(entries);
	}

	this.getEntryElement = function(entryId) {
		return $("#" + this.editId + "entryid" + entryId);
	}

	this.toggleSuffix = function($control, suffix) {
		var text = $control.val();

		if (text.endsWith(" repeat")) {
			text = text.substr(0, text.length - 7);
			$control.val(text);

			if (suffix == "repeat") {
				window.setTimeout(function() {
					$control.selectRange(text.length, text.length);
					$control.focus();
				}, 1);
				return text.length > 0;
			}
		}
		if (text.endsWith(" remind")) {
			text = text.substr(0, text.length - 7);
			$control.val(text);

			if (suffix == "remind") {
				window.setTimeout(function() {
					$control.selectRange(text.length, text.length);
					$control.focus();
				}, 1);
				return text.length > 0;
			}
		}
		if (text.endsWith(" pinned")) {
			text = text.substr(0, text.length - 7);
			$control.val(text);

			if (suffix == "pinned") {
				window.setTimeout(function() {
					$control.selectRange(text.length, text.length);
					$control.focus();
				}, 1);
				return text.length > 0;
			}
		}

		var retVal = text.length > 0;	
		text = text + " " + suffix;
		$control.val(text);
		window.setTimeout(function() {
			$control.selectRange(text.length, text.length);
			$control.focus();
		}, 1);

		return retVal;
	}

	this.modifyEdit = function(suffix) {
		var $control = $('#' + this.editId + 'tagTextInput');
		this.toggleSuffix($control, suffix);
	}

	this.modifyInput = function(suffix) {
		this.initInput();
		this.toggleSuffix($('#' + this.editId), suffix);
	}

	this.deleteGhost = function($entryToDelete, entryId, allFuture) {
		queueJSON("deleting entry", makeGetUrl("deleteGhostEntryData"), makeGetArgs(getCSRFPreventionObject("deleteGhostEntryDataCSRF", {entryId:entryId,
			all:(allFuture ? "true" : "false"), date:this.cachedDateUTC})),
			function(ret) {
				if (checkData(ret, 'success', "Error deleting entry")) {
					$entryToDelete.remove();
				}
			});
	}

	this.deleteEntryId = function(entryId) {
		this.cacheNow();

		if (entryId == undefined) {
			showAlert("Please select entry you wish to delete");
			return false;
		}
		var $entryToDelete = this.getEntryElement(entryId);
		if ($entryToDelete.data("isTimed") || $entryToDelete.data("isGhost")) {
			if ($entryToDelete.data("isContinuous") || this.isTodayOrLater) {
				this.deleteGhost($entryToDelete, entryId, true);
			} else {
				showAB("Delete just this one event or also future events?", "One", "Future", function() {
					self.deleteGhost($entryToDelete, entryId, false);
				}, function() {
					self.deleteGhost($entryToDelete, entryId, true);
				});
			}
		} else {
			this.cacheNow();
			queueJSON("deleting entry", "/home/deleteEntrySData?entryId=" + entryId
					+ "&currentTime=" + this.currentTimeUTC + "&baseDate=" + this.cachedDateUTC
					+ "&timeZoneName=" + this.timeZoneName + "&displayDate=" + this.cachedDateUTC + "&"
					+ getCSRFPreventionURI("deleteEntryDataCSRF") + "&callback=?",
					function(entries) {
				if (checkData(entries, 'success', "Error deleting entry")) {
					self.tagList.load();
					self.refreshEntries(entries[0]);
					if (entries[1] != null)
						self.autocompleteWidget.update(entries[1][0], entries[1][1], entries[1][2], entries[1][3]);
					if (entries[2] != null) {
						self.autocompleteWidget.update(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
					}
				}
			});
		}

		return false;
	}

	this.doUpdateEntry = function(entryId, text, defaultToNow, allFuture) {
		this.cacheNow();
		queueJSON("updating entry", "/home/updateEntrySData?entryId=" + entryId
				+ "&currentTime=" + this.currentTimeUTC + "&text=" + escape(text) + "&baseDate="
				+ this.cachedDateUTC + "&timeZoneName=" + this.timeZoneName + "&defaultToNow=" + (defaultToNow ? '1':'0') + "&"
				+ getCSRFPreventionURI("updateEntrySDataCSRF") + "&allFuture=" + (allFuture? '1':'0') + "&callback=?",
				function(entries){
			if (checkData(entries, 'success', "Error updating entry")) {
				self.tagList.load();
				self.refreshEntries(entries[0]);
				if (entries[1] != null)
					self.autocompleteWidget.update(entries[1][0], entries[1][1], entries[1][2], entries[1][3]);
				if (entries[2] != null) {
					self.autocompleteWidget.update(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
				}
			}
		});
	}

	this.updateEntry = function(entryId, text, defaultToNow) {
		var $oldEntry = this.getEntryElement(entryId);

		if ((($oldEntry.data("isRepeat") && (!$oldEntry.data("isRemind"))) || $oldEntry.data("isGhost")) && (!this.isTodayOrLater)) {
			showAB("Update just this one event or also future events?", "One", "Future", function() {
				self.doUpdateEntry(entryId, text, defaultToNow, false);
			}, function() {
				self.doUpdateEntry(entryId, text, defaultToNow, true);
			});
		} else {
			self.doUpdateEntry(entryId, text, defaultToNow, true);
		}
	}

	this.addEntry = function(userId, text, defaultToNow) {
		this.cacheNow();

		queueJSON("adding new entry", "/home/addEntrySData?currentTime=" + this.currentTimeUTC
				+ "&userId=" + userId + "&text=" + escape(text) + "&baseDate=" + this.cachedDateUTC
				+ "&timeZoneName=" + this.timeZoneName + "&defaultToNow=" + (defaultToNow ? '1':'0') + "&"
				+ getCSRFPreventionURI("addEntryCSRF") + "&callback=?",
				function(entries){
			if (checkData(entries, 'success', "Error adding entry")) {
				if (entries[1] != null) {
					showAlert(entries[1]);
				}
				self.tagList.load();
				self.refreshEntries(entries[0]);
				if (entries[2] != null)
					self.autocompleteWidget.update(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
			}
		});
	}

	this.initInput = function() {
		if (!$("#" + this.editId).data('entryTextSet')) {
			this.setEntryText('');
		}
	}

	this.processInput = function() {
		var $field = $("#" + this.editId);
		$field.autocomplete("close");
		var text = $field.val();
		if (text == "") return; // no entry data
		$field.val("");
		this.addEntry(currentUserId, text, this.defaultToNow);
		return true;
	}

	this.setEntryText = function(text, startSelect, endSelect) {
		var $inp = $("#" + this.editId);
		$inp.autocomplete("close");
		$inp.val(text);
		$inp.css('color','#000000');
		if (startSelect) {
			$inp.selectRange(startSelect, endSelect);
		}
		$inp.focus();
		$inp.data("entryTextSet", true);
	}

	/**
	 * Used to un-select and entry. Removes the entry edit text field
	 * & displays the original content back.
	 */
	this.unselectEntry = function($unselectee, displayNewText, displaySpinner) {
		console.log('Unselect Entry:', $unselectee.attr('id'));

		var $contentWrapper = $unselectee.find(".content-wrapper");
		var displayText = $unselectee.data('contentHTML');

		if (displayNewText) {
			var newText = $('#' + this.editId + 'tagTextInput').val();
			if (newText) {
				displayText = newText;
			}
		}

		$contentWrapper.html(displayText);
		$contentWrapper.show();
		if (displaySpinner) {
			$contentWrapper.append(" &nbsp;<img src='/images/spinner.gif' />");
		}

		$("#" + this.editId + "tagTextEdit").remove();
		$unselectee.removeClass('ui-selected');
	}

	/**
	 * Used to select an entry.
	 */
	this.selectEntry = function($selectee) {
		console.debug('Select Entry:', $selectee.attr("id"));

		$selectee.siblings().removeClass("ui-selected");
		var $contentWrapper = $selectee.find(".content-wrapper");

		$selectee.data('contentHTML', $contentWrapper.html()); // store original HTML for later restoration
		var currentEntryId = $selectee.data("entryId");
		$selectee.addClass('ui-selected');

		var entryText = $selectee.text();

		var selectRange = self.entrySelectData[currentEntryId];
		if (selectRange != undefined) {
			if (selectRange[2]) { // insert space at selectRange[0]
				entryText = entryText.substr(0, selectRange[0] - 1) + " " + entryText.substr(selectRange[0] - 1);
			}
		}
		$selectee.data('originalText', entryText); // store entry text for comparison
		$contentWrapper.hide();
		$selectee.append('<span id="' + this.editId + 'tagTextEdit"><input type="text" class="entryNoBlur" id="' + this.editId + 'tagTextInput" style="margin: 2px; width: calc(100% - 75px);"></input>'
				+ '<img class="entryModify edit-repeat" data-suffix="repeat" src="/images/repeat.png">'
				+ '<img class="entryModify edit-remind" data-suffix="remind" src="/images/remind.png">'
				+ '<img class="entryModify edit-pin" data-suffix="pinned" src="/images/pin.png"></span>');

		$('#' + self.editId + 'tagTextInput')
			.val(entryText).focus()
			.data('entryTextSet', true)
			.on("keyup", function(e) {
				var entryData = $selectee.data();
				if (e.keyCode == 13) {	// Enter pressed
					self.checkAndUpdateEntry($selectee);
				} else if (e.keyCode == 27) {	// Esc pressed
					self.unselectEntry($selectee);
				}
			});

		if ($selectee.data('isContinuous'))
			self.toggleSuffix($('#' + this.editId + 'tagTextInput'), 'pinned');

		if (selectRange) {
			$('#' + self.editId + 'tagTextInput').selectRange(selectRange[0], selectRange[1]);
		}
	}
	
	/**
	 * Checks if text is different from original text.
	 * IF different than call updateEntry() method to notify
	 * server and update in UI.
	 */
	this.checkAndUpdateEntry = function($unselectee) {
		if ($unselectee == undefined) {
			console.warn("Undefined unselectee.");
			return;
		}
		console.debug('Check and update entry:', $unselectee.attr('id'));

		var newText = $('#' + this.editId + 'tagTextInput').val();
		if (newText == undefined) {
			console.warn("Undefined new text");
			return;
		}
		var currentEntryId = $unselectee.data("entryId");

		if ($unselectee.data('isContinuous')) {
			console.debug('Is a continuous entry:', $unselectee.attr('id'));
			this.addEntry(currentUserId, newText, this.defaultToNow);
			this.unselectEntry($unselectee, false, true);
		} else if (!$unselectee.data('isRemind') && $unselectee.data('originalText') == newText) {
			console.debug('Is not remind & no change in entry.');
			this.unselectEntry($unselectee);
		} else {
			console.log('Either remind or change in entry.');
			this.unselectEntry($unselectee, true, true);
			this.updateEntry(currentEntryId, newText, this.defaultToNow);
		}
	}

	/**
	 * Global mousedown handler for entry & data.
	 */
	$(document).on("mousedown", function(e) {
		// Only handle for left mouse click.
		if (e.which != 1) {
			return;
		}
		if (__dialogInProgress > 0)
			return;

		var $target = $(e.target);
		var $alreadySelectedEntry = $("li.entry.ui-selected");

		// Checking if any entry is already selected when mousedown event triggered.
		var isAnyEntrySelected = $alreadySelectedEntry.length > 0;

		// When clicked on any entry element i.e. li.entry
		var isClickedOnEntry = $target.closest("li.entry").length > 0;

		// If such elements are clicked, where we not have to do anything. (Like deleteEntry)
		var isEventToCancel = $target.closest(".entryNoBlur").length > 0;

		// If any of the 3 image buttons (besides edit entry text field) are clicked.
		var isEntryModify = $target.closest("img.entryModify").length > 0;
		
		if (isEventToCancel) {
			return;
		}

		if (isAnyEntrySelected) {
			if (isEntryModify) {
				var suffix = $target.data('suffix');
				self.modifyEdit(suffix);
				return;
			}
			console.debug('Mousedown: There is a selcted entry. Will now unselect.')
			self.checkAndUpdateEntry($("li.entry.ui-selected"));
			return;
		}

		if (isClickedOnEntry) {
			// parents() method returns all anscestors as list. So element at 0th position will be li.entry
			var selectee = $target.parents("li.entry").andSelf()[0];
			console.debug('Mousedown: Clicked on an entry. Will now select.');
			self.selectEntry($(selectee), false);
			return false;
		}
	});

	this.adjustDatePicker = function() {
		var datepickerFormat;
		var $datepicker = $("#" + this.calendarId);

		// If not a mobile sized device
		if ($(window).width() > 480) {
			datepickerFormat = 'DD MM dd, yy';
		} else {
			datepickerFormat = 'D MM dd, yy';
		}
		$datepicker.val($.datepicker.formatDate(datepickerFormat, currentDate));
	}
	
	$("#" + self.editId).droppable({
		drop : function(event, ui) {
			var droppedItem = $(ui.draggable[0]).data(DATA_KEY_FOR_ITEM_VIEW).getData();
			self.setEntryText(droppedItem.description);
		}
	});

	var $datepicker = $("#" + self.calendarId);

	$datepicker
			.datepicker({defaultDate: currentDate, dateFormat: 'DD MM dd, yy', showButtonPanel: true})
			.val($.datepicker.formatDate('DD MM dd, yy', currentDate))
			.datepicker("hide")
			.change(function () {
				self.refresh();
			})
			.click(function() {
				$('button.ui-datepicker-current').removeClass('ui-priority-secondary').addClass('ui-priority-primary');
			});

	$(window).resize(function(e) {
		self.adjustDatePicker();
	});
	self.adjustDatePicker();

	$("#" + this.editId)
			.on("click", function(e) {
				if (!$("#" + this.editId).data('entryTextSet')) {
					self.setEntryText('');
				}
			})
			.keyup(function(e) {
				if (e.keyCode == 13) {	// Enter pressed.
					self.processInput();
				}
			});

	/**
	 * Keycode= 37:left, 38:up, 39:right, 40:down
	 */
	$("#" + self.editId).keydown(function(e) {
		if ($.inArray(e.keyCode, [38, 40]) == -1) {
			return true;
		}
		var $unselectee = $("li.ui-selected", "#" + self.listId);
		if (!$unselectee) {
			return false;
		}
		var $selectee;
		if (e.keyCode == 40) { 
			$selectee = $unselectee.next();
		} else if (e.keyCode == 38) { 
			$selectee = $unselectee.prev();
		}
		if ($selectee) {
			self.checkAndUpdateEntry($unselectee);
			self.selectEntry($selectee, false);
		}
		return false;
	});
	
	self.refresh();
}
