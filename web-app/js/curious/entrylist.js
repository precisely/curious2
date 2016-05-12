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

function __removePrefix(str, prefix) {
	if (str) {
		if (str == prefix) return '';
		if (str.startsWith(prefix + ' ')) {
			return str.substr(prefix.length + 1);
		}
	}
	return str;
}

/**
 * Updating the popover position on scrolling the recordList.
 */
$('#recordList').scroll(function() {
	var $selectee = $('.ui-selected');
	if (!$selectee || !$selectee.length) {
		// Returning if there is no selected entry.
		return;
	}

	var $popover = $('.popover');
	if (!$popover || !$popover.length) {
		// Returning if there is no popover.
		return;
	}

	var $popoverLauncher = $selectee.find('.track-input-modifiers');
	var listPosition = $(this).position();
	var entryPosition = $selectee.position();
	entryPosition.bottom = entryPosition.top + $selectee.outerHeight(true);
	listPosition.bottom = listPosition.top + $(this).outerHeight(true);
	if (entryPosition && (((entryPosition.top + 3) <= listPosition.top) || ((entryPosition.bottom - 3) >= listPosition.bottom))) {
		// Hiding popover when it exceeds the bounds of the list.
		$popover.css({'visibility': 'hidden'});
		return
	}
	var top = $popoverLauncher.offset().top + $popoverLauncher.outerHeight(true);
	$popover.css({'top': top, 'visibility': 'visible'});
});

function hidePopover(element) {
	if (!element) {
		element = $('[data-toggle="popover"]');
	}
	element.popover('hide');
}

/**
 * Used to hide all previously opened popovers and launch the required popover.
 */
function showPopover($launcher) {
	var $popover = $('.popover');
	if ($popover.length !== 0) {
		$.each($popover, function(popover) {
			hidePopover($('[aria-describedby="' + popover.id + '"]'));
		});
	}

	$launcher.popover('show');
}

function createPopover(element, content, containerId) {
	element.popover({
		trigger: 'manual',
		placement: 'bottom',
		html: true,
		container: containerId,
		content: content,
		template: '<div class="popover dropdown-menu"><div class="arrow"></div><h3 class="popover-title">' +
				'</h3><div class="popover-content"></div></div>'
	});
}

$(document).on('shown.bs.popover', function() {
	/*
	 * Setting the datepicker options here. The popover in the new entry affordance has both changeYear as well as
	 * changeMonth enabled.
	 */
	var datepickerOptions = ($('.entry-details-dropdown-menu').data('for-entry')) ? {} : {changeYear: true, changeMonth: true, yearRange: "-120:+0"};
	$('.choose-date-input').datepicker(datepickerOptions);
	var $selectee = $('.ui-selected');
	if ($selectee.length) {
		var entry = $selectee.data('entry');
		if (entry.repeatEnd) {
			var oldRepeatEndDate = new Date(entry.repeatEnd);
			$(".choose-date-input").datepicker('setDate', oldRepeatEndDate);
		}
	}
});

function EntryListWidget(divIds, autocompleteWidget) {
	var self = this;

	if (!divIds) divIds = {};

	divIds = $.extend({editId:"input0", listId:"entry0", calendarId:"datepicker", dragId:"drag", areaId:"area"}, divIds);

	this.entryListItems = [];
	this.entriesSortOrder = {
		ascendingDescription: true,
		ascendingTime: true
	}
	this.editId = divIds.editId;
	this.listId = divIds.listId;
	this.calendarId = divIds.calendarId;
	this.dragId = divIds.dragId;
	this.latestEntryId;
	if (!autocompleteWidget) {
		autocompleteWidget = new AutocompleteWidget("autocomplete", this.editId);
	}

	this.autocompleteWidget = autocompleteWidget;

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
		var params = {
			date: this.cachedDateUTC,
			currentTime: this.currentTimeUTC,
			userId: currentUserId,
			timeZoneName: this.timeZoneName,
			// TODO Fix this later
			respondAsMap: true
		};

		queueJSON("getting entries", "/home/getListData?callback=?", getCSRFPreventionObject("getListDataCSRF", params),
				function(data) {
			if (!checkData(data)) {
				return;
			}

			self.storeDeviceEntryStates(data.deviceEntryStates);
			self.refreshEntries(data.entries);
		});
	};

	this.storeDeviceEntryStates = function(states) {
		console.log("Device entries states", states);
		this.deviceEntryStates = states;
	};

	this.clearEntries = function() {
		this.entryListItems = [];
		$("#" + this.listId).html('');
		$("#pinned-tag-list").html('');
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

	/**
	 * Render the given entry data or instance into the view.
	 * @param {Object} entryInstance
	 * @param {boolean} isUpdating
	 * @param {Object} args
	 */
	this.displayEntry = function(entryInstance, isUpdating, args) {
		args = args || {};

		var isDeviceSummaryEntry = entryInstance instanceof EntryDeviceDataSummary;
		var isDeviceTagEntry = args.deviceTagEntry;     // Boolean

		var entry = entryInstance;
		if (isDeviceSummaryEntry) {
			entry = entryInstance.group();
		} else {
			this.entryListItems.push(entry);
		}

		var id = entry.id;
		var date = entry.date;
		var datePrecisionSecs = entry.datePrecisionSecs;
		var description = entry.description;
		var comment = entry.comment;
		var classes = ["entry"];

		if (args.classes) {
			// Push all the classes
			classes.push.apply(classes, args.classes);
		}

		var isGhost = false, isConcreteGhost = false, isAnyGhost = false, isContinuous = false, isTimed = false, isRepeat = false, isRemind = false, isPlain = true;
		if (entry.repeatType) {
			var repeatType = entry.repeatType;
			if (RepeatType.isGhost(repeatType)) {
				isPlain = false;
				isGhost = true;
				isAnyGhost = true;
				classes.push("ghost", "anyghost");
			}
			if (RepeatType.isConcreteGhost(repeatType)) {
				isConcreteGhost = true;
				isPlain = false;
				isAnyGhost = true;
				classes.push("concreteghost", "anyghost");
			}
			if (RepeatType.isContinuous(repeatType)) {
				isContinuous = true;
				isPlain = false;
				classes.push("continuous");
			}
			if (RepeatType.isRemind(repeatType)) {
				isRemind = true;
				isPlain = false;
				classes.push("remind");
			}
			if (RepeatType.isRepeat(repeatType) || RepeatType.isDaily(repeatType) || RepeatType.isWeekly(repeatType) ||
					RepeatType.isMonthly(repeatType)) {
				isRepeat = true;
				isPlain = false;
				classes.push("repeat");
			}
		} else {
			classes.push("no-tag");
		}

		if (isPlain) {
			classes.push("plain");
		}
		if (isDeviceSummaryEntry) {
			classes.push("device-summary-entry", entry.sourceName.sanitizeTitle());
		} else {
			classes.push("editable-entry");
		}
		if (isDeviceTagEntry) {
			classes.push("device-tag-entry");
			if (!args.singleDeviceTagEntry) {
				classes.push(args.deviceDataSummaryInstance.getAssociatedEntriesClass());
			}
		}

		if (isContinuous) {
			var buttonText = escapehtml(description);
			var nullAmount = false;
			if (entry.amountPrecision > 0) {
				buttonText += ' ' + entry.amount + ' ' + escapehtml(entry.units);
			} else {
				if (entry.amount == null) {
					buttonText += ' # ' + escapehtml(entry.units);
					nullAmount = true;
				}
			}

			var pinnedTagButtonHTMLContent = '<div class="pin-button" id="' + this.editId + 'entryid' + id + '">' +
				' <button class="pin-entry" id="pin-button' + id + '" onclick="entryListWidget.createEntryFromPinnedEntry(' + currentUserId
				+',' + id + ',\'' + buttonText +'\',' + this.defaultToNow +',' + (nullAmount ? 'true' : 'false') + ')">'+
				buttonText + '</button>' + '<li class="dropdown hide-important"><a href="#" data-toggle="dropdown">' +
				'<b class="caret"></b></a><ul class="dropdown-menu" role="menu"><li>' +
				'<a href="#" id="#entrydelid' + this.editId + id + '" onclick="entryListWidget.deleteEntryId(' + id + ');return false;">' +
				'<img src="/images/pin-x.png" width="auto" height="23">Delete</a></li></ul></li></div>';

			$("#pinned-tag-list").append(pinnedTagButtonHTMLContent);
			return;
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

		var innerHTMLContent = '<div class="content-wrapper '+ ((entry.repeatType) ? '' : 'no-tag') +'">';

		if (isDeviceSummaryEntry) {
			innerHTMLContent += entryInstance.getTriangle() + ' ';
		}

		innerHTMLContent += (timeAfterTag ? '' : '<span class="entryTime">' + escapehtml(dateStr) + '</span>') + '<span class="entryDescription">'
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

		comment = __removePrefix(comment, 'repeat');
		comment = __removePrefix(comment, 'remind');
		comment = __removePrefix(comment, 'daily');
		comment = __removePrefix(comment, 'weekly');
		comment = __removePrefix(comment, 'monthly');

		var commentHTML = comment ? ' <span class="entryComment">' + escapehtml(comment) : '</span>';
		var commentLabel = '';
		if (isRemind && isRepeat) {
			commentLabel = '<div class="comment-label "> <div class="repeatLabelImage"></div><span class="entryRepeat">REPEAT + ALERT</span></div>';
		} else if (isRemind) {
			commentLabel = '<div class="comment-label "> <div class="remindLabelImage"></div><span class="entryRemind">ALERT</span></div>';
		} else if (isRepeat) {
			commentLabel = '<div class="comment-label "> <div class="repeatLabelImage"></div><span class="entryRepeat">REPEAT</span></div>';
		} else {
			commentLabel = '<div class="comment-label "></div>';
		}

		innerHTMLContent += (timeAfterTag ? '<span class="entryTime">'
				+ escapehtml(dateStr) + '</span>' : '') + commentHTML + '</div>' + commentLabel;

		if (!isDeviceSummaryEntry) {
		var entryDetailsPopover = _.template($('#entry-details-popover').clone().html())({'editType': id + '-', 'entryId': this.editId + "entryid" + id});

			innerHTMLContent += '<button class="edit">Edit</button><button class="btn-purple save save-entry hide">Save' +
					' Edit</button><a href="#" style="padding-left:0;" class="entryDelete entryNoBlur" id="entrydelid' +
					this.editId + id + '"><img class="entryModify edit-delete" src="/images/x.png"></a>';

			// Do not display the entry details button and popover for device entries
			if (!isDeviceTagEntry) {
				innerHTMLContent += entryDetailsPopover;
			}
		}

		var entryEditItem;

		var elementId = "" + this.editId + "entryid" + id;
		if (isDeviceSummaryEntry) {
			elementId = entryInstance.getAssociatedEntriesClass();
		}

		if (isUpdating) {
			entryEditItem = $("#" + elementId);
			entryEditItem.html(innerHTMLContent);
		} else {
			var newEntryContent = '<li id="' + elementId + '" class="' + classes.join(" ") + '">' + innerHTMLContent + '</li>';
			$("#" + this.listId).append(newEntryContent);
			entryEditItem = $("#" + elementId);
		}
		$("#entrydelid" + this.editId + id).click(function() {
			self.deleteEntryId(id);
			return false;       // Do not throw user back to the top
		});

		var data = {entry: entry, entryId:id, isGhost:isGhost, isConcreteGhost:isConcreteGhost, isAnyGhost:isAnyGhost, isContinuous:isContinuous,
				isTimed:isTimed, isRepeat:isRepeat, isRemind:isRemind};

		if (isDeviceSummaryEntry) {
			data.instance = entryInstance;
		} else if (isDeviceTagEntry) {
			data.instance = args.deviceDataSummaryInstance;
		}
		entryEditItem.data(data);
	};

	this.displayDeviceNameEntry = function(deviceEntry) {
		var entryDeviceDataInstance = new EntryDeviceData(deviceEntry, this.deviceEntryStates);
		var groupedData = entryDeviceDataInstance.group();

		var id = entryDeviceDataInstance.getSanitizedSourceName();
		var html = '<li class="entry no-tag device-entry plain" id="' + id + '"><div class="no-tag">' +
				entryDeviceDataInstance.getDisplayText() + '</div></li>';

		$("#" + this.listId).append(html);
		$("#" + id).data({instance: entryDeviceDataInstance});

		jQuery.each(groupedData, function(index, groupedEntry) {
			this.displayDeviceSummaryEntry(groupedEntry, entryDeviceDataInstance);
		}.bind(this));
	};

	this.displayDeviceSummaryEntry = function(groupedEntries, entryDeviceDataInstance) {
		var classes = [];
		if (entryDeviceDataInstance.isCollapsed()) {
			classes.push("hide");
		}

		// If there is only single entry for a base tag. For example: single entry for "walk"
		if (groupedEntries.length === 1) {
			// Then do not display that as expandable entry instead show it as simple entry (but indented)
			classes.push(entryDeviceDataInstance.getSanitizedSourceName());
			classes.push("no-summary");

			this.displayEntry(groupedEntries[0], false, {deviceTagEntry: true, singleDeviceTagEntry: true, classes:
					classes});
			return;
		}

		var deviceDataSummaryEntry = new EntryDeviceDataSummary(groupedEntries);
		this.displayEntry(deviceDataSummaryEntry, false, {classes: classes});

		groupedEntries.forEach(function(entry) {
			// If summary entry is collapsed (user might have collapsed it in the current session)
			if (deviceDataSummaryEntry.isCollapsed()) {
				// Then only hide all nested entries
				if (classes.indexOf("hide") === -1) {
					classes.push("hide");
				}
			}

			this.displayEntry(entry, false, {deviceTagEntry: true, deviceDataSummaryInstance: deviceDataSummaryEntry,
				classes: classes});
		}.bind(this));
	};

	/**
	 * This method iterates through list of entries and displays them
	 * @param {Array} entries list of entries to display
	 * @param {boolean} onlyPinned to display only pinned entries
	 */
	this.displayEntries = function(entries, onlyPinned) {
		self.entrySelectData = {};
		self.groupedData = {};
		var entryDataInstance = new EntryData(entries);

		jQuery.each(entryDataInstance.getNormalEntries(), function() {
			if (onlyPinned && !RepeatType.isContinuous(this.repeatType)) {
				return;
			}
			self.displayEntry(this, false);
		});

		/*
		 * Do not display device entry when creating bookmark entry because creating bookmark entry only updates the
		 * bookmark entry container, otherwise it will duplicate the device data.
		 */
		if (!onlyPinned) {
			jQuery.each(entryDataInstance.getDeviceEntries(), function(index, deviceEntry) {
				this.displayDeviceNameEntry(deviceEntry);
			}.bind(this));
		}

		$('#pinned-tag-list').children('div').each(function () {
			$(this).hover(
				function() {
					$(this).children('.dropdown').removeClass('hide-important');
				}, function() {
					$(this).children('.dropdown').addClass('hide-important');
				}
			);
		});
	};

	this.refreshEntries = function(entries) {
		this.clearEntries();
		this.displayEntries(entries);
	}

	this.refreshPinnedEntries = function(entries) {
		$("#pinned-tag-list").html('');
		this.displayEntries(entries, true);
	}

	/**
	 * Click handler to execute when someone clicks on the device entry (top level) like "Moves Data".
	 *
	 * @param $target jQuery DOM element object for above said entry
	 */
	this.toggleDeviceEntry = function($target) {
		/**
		 * @type {EntryDeviceData}
		 */
		var entryDeviceDataInstance = $target.data("instance");        // Instance of EntryDeviceData

		// This will be all device summary entries under the current device entry
		var elementsToToggle = $("." + $target.attr("id"));

		// We have to save the collapse/expand state of device entries only when on the current day entry page
		var shouldSaveState = areSameDate(this.cachedDate, new Date());

		// Is already collapsed
		if (entryDeviceDataInstance.isCollapsed()) {
			entryDeviceDataInstance.expand(shouldSaveState);
			elementsToToggle.slideDown();
		} else {
			// Collapse device entry
			entryDeviceDataInstance.collapse(shouldSaveState);
			elementsToToggle.slideUp();

			jQuery.each(elementsToToggle, function(index, element) {
				var $element = $(element);
				if (!$element.data("instance")) {
					/*
					 * It might not be summary entry since we do not create a summary entry if there is only entry
					 * for a base tag.
					 */
					return;
				}

				// And collapse device summary entries also
				this.toggleDeviceSummaryEntry($element, true);
			}.bind(this));
		}

		$target.find(".toggle-icon").replaceWith(entryDeviceDataInstance.getTriangle());
	};

	/**
	 * Click handler to execute when someone clicks on the device summary entry (2nd level) like
	 * "walk 3760 steps 70.23 mins 1.6 mi 12:28am (Moves)"
	 *
	 * @param $target jQuery DOM element object for above said entry
	 * @param {boolean} forceCollapse Confirms the entries are collapsed
	 */
	this.toggleDeviceSummaryEntry = function($target, forceCollapse) {
		/**
		 * @type {EntryDeviceDataSummary}
		 */
		var entryDeviceSummaryInstance = $target.data("instance");        // Instance of EntryDeviceDataSummary
		var elementsToToggle = $("." + $target.attr("id"));

		if (!forceCollapse && entryDeviceSummaryInstance.isCollapsed()) {
			entryDeviceSummaryInstance.expand();
			elementsToToggle.slideDown();
		} else {
			entryDeviceSummaryInstance.collapse();
			elementsToToggle.slideUp();
		}

		$target.find(".toggle-icon").replaceWith(entryDeviceSummaryInstance.getTriangle());
	};

	this.getEntryElement = function(entryId) {
		return $("#" + this.editId + "entryid" + entryId);
	}

	this.sortByTime = function() {
		if (this.entriesSortOrder.ascendingTime) {
			this.entryListItems.sort(function(item1,item2) {
				if (new Date(item1.date) < new Date(item2.date))
					return -1;
				if (new Date(item1.date) > new Date(item2.date))
					return 1;
				return 0;
			});
		} else {
			this.entryListItems.sort(function(item1,item2) {
				if (new Date(item1.date) < new Date(item2.date)) {
					return 1;
				}
				if (new Date(item1.date) > new Date(item2.date)) {
					return -1;
				}
				return 0;
			});
		}
		var $sortByDescription = $("#sort-by-description");
		var $sortByTime = $("#sort-by-time");
		this.manipulateSortElementClass($sortByTime, $sortByDescription, this.entriesSortOrder.ascendingTime);

		this.entriesSortOrder.ascendingTime = !this.entriesSortOrder.ascendingTime;
		this.refreshEntries(this.entryListItems);
	}

	this.sortByDescription = function() {
		if (this.entriesSortOrder.ascendingDescription) {
			this.entryListItems.sort(function(item1,item2) {
				if (item1.description < item2.description)
					return -1;
				if (item1.description > item2.description)
					return 1;
				return 0;
			});
		} else {
			this.entryListItems.sort(function(item1,item2) {
				if (item1.description < item2.description)
					return 1;
				if (item1.description > item2.description)
					return -1;
				return 0;
			});
		}

		var $sortByDescription = $("#sort-by-description");
		var $sortByTime = $("#sort-by-time");
		this.manipulateSortElementClass($sortByDescription, $sortByTime, this.entriesSortOrder.ascendingDescription);

		this.entriesSortOrder.ascendingDescription = !this.entriesSortOrder.ascendingDescription;
		this.refreshEntries(this.entryListItems);
	}

	this.manipulateSortElementClass = function($activeElement, $passiveElement, isAscending) {
		$passiveElement.removeClass("active");
		$passiveElement.find("img").addClass(" hide");
		$passiveElement.find("img").attr("src","");

		if (!$activeElement.hasClass("active")) {
			$activeElement.addClass("active");
		}
		if (isAscending) {
			$activeElement.find("img").removeClass("hide");
			$activeElement.find("img").attr("src", "/images/asc.png");
		} else {
			$activeElement.find("img").removeClass("hide");
			$activeElement.find("img").attr("src", "/images/desc.png");
		}
	}

	this.deleteGhost = function($entryToDelete, isContinuous, entryId, allFuture) {
		this.cacheNow();

		queueJSON("deleting entry", makeGetUrl("deleteGhostEntryData"), makeGetArgs(getCSRFPreventionObject("deleteGhostEntryDataCSRF", {entryId:entryId,
			all:(allFuture ? "true" : "false"), currentTime:this.cachedDateUTC, baseDate:this.cachedDateUTC})),
			function(ret) {
				if (checkData(ret, 'success', "Error deleting entry")) {
					if (isContinuous) {
						$("#pin-button"+entryId).remove();
					}
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
			if ($entryToDelete.data("isContinuous")) {
				this.deleteGhost($entryToDelete, true, entryId, true);
			} else if (this.isTodayOrLater) {
				this.deleteGhost($entryToDelete, false, entryId, true);
			} else {
				showAB("Delete just this one event or also future events?", "One", "Future", function() {
					self.deleteGhost($entryToDelete, false, entryId, false);
				}, function() {
					self.deleteGhost($entryToDelete, false, entryId, true);
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
					self.storeDeviceEntryStates(entries[3]);
					self.refreshEntries(entries[0]);

					if (entries[1] != null)
						self.autocompleteWidget.update(entries[1][0], entries[1][1], entries[1][2], entries[1][3], entries[1][4]);
					if (entries[2] != null) {
						self.autocompleteWidget.update(entries[2][0], entries[2][1], entries[2][2], entries[2][3], entries[2][4]);
					}
				}
			});
		}

		return false;
	}

	this.doUpdateEntry = function(entryId, text, defaultToNow, repeatTypeId, repeatEnd, allFuture) {
		this.cacheNow();
		var repeatParams = this.getRepeatParamsStr(repeatTypeId, repeatEnd);

		queueJSON("updating entry", "/home/updateEntrySData?entryId=" + entryId
				+ "&currentTime=" + this.currentTimeUTC + "&text=" + escape(text) + repeatParams + "&baseDate="
				+ this.cachedDateUTC + "&timeZoneName=" + this.timeZoneName + "&defaultToNow=" + (defaultToNow ? '1':'0') + "&"
				+ getCSRFPreventionURI("updateEntrySDataCSRF") + "&allFuture=" + (allFuture? '1':'0') + "&callback=?",
				function(entries) {
			if (checkData(entries, 'success', "Error updating entry")) {
				self.storeDeviceEntryStates(entries[3]);
				self.refreshEntries(entries[0]);

				if (self.nextSelectionId) {
					var nextSelection = $('#' + self.nextSelectionId);
					self.nextSelectionId = null;
					self.selectEntry(nextSelection);
				}
				if (entries[1] != null)
					self.autocompleteWidget.update(entries[1][0], entries[1][1], entries[1][2], entries[1][3], entries[1][4]);
				if (entries[2] != null) {
					self.autocompleteWidget.update(entries[2][0], entries[2][1], entries[2][2], entries[2][3], entries[2][4]);
				}
			}
		});
	}

	this.updateEntry = function(entryId, text, defaultToNow, repeatTypeId, repeatEnd) {
		var $oldEntry = this.getEntryElement(entryId);

		console.log('update....');
		if ((($oldEntry.data("isRepeat") && (!$oldEntry.data("isRemind"))) || $oldEntry.data("isGhost")) && (!this.isTodayOrLater)) {
			showAB("Update just this one event or also future events?", "One", "Future", function() {
				self.doUpdateEntry(entryId, text, defaultToNow, repeatTypeId, repeatEnd, false);
			}, function() {
				self.doUpdateEntry(entryId, text, defaultToNow, repeatTypeId, repeatEnd, true);
			});
		} else {
			self.doUpdateEntry(entryId, text, defaultToNow, repeatTypeId, repeatEnd, true);
		}
	}

	this.createEntryFromPinnedEntry = function(userId, entryId, text, defaultToNow, nullAmount) {
		var entries = [];
		this.entryListItems.forEach(function(entryItem) {
			if (entryItem.id == entryId) entries.push(entryItem);
		});

		var tagStats = this.autocompleteWidget.tagStatsMap.get(text);
		if (!tagStats) tagStats = this.autocompleteWidget.tagStatsMap.getFromText(text);

		if ((!tagStats) || tagStats.typicallyNoAmount || (!tagStats.typicallyNoAmount && entries[0].amount)) {
			if (nullAmount) {
				this.addEntry(userId, text, defaultToNow, null, null, function() {
					var selectee = $('#' + self.editId + 'entryid' + self.latestEntryId);
					self.selectEntry($(selectee));
				});
			} else
				this.addEntry(userId, text, defaultToNow);
		} else {
			this.addEntry(userId, tagStats.createText(), defaultToNow, null, null, function() {
				var selectee = $('#' + self.editId + 'entryid' + self.latestEntryId);
				self.selectEntry($(selectee));
			});
		}
	}

	this.addEntry = function(userId, text, defaultToNow, repeatTypeId, repeatEnd, callBack) {
		if (text == '') {
			return;
		}
		this.cacheNow();
		var repeatParams = this.getRepeatParamsStr(repeatTypeId, repeatEnd);
		queueJSON("adding new entry", "/home/addEntrySData?currentTime=" + this.currentTimeUTC
				+ "&userId=" + userId + "&text=" + escape(text) + repeatParams + "&baseDate=" + this.cachedDateUTC
				+ "&timeZoneName=" + this.timeZoneName + "&defaultToNow=" + (defaultToNow ? '1':'0') + "&"
				+ getCSRFPreventionURI("addEntryCSRF") + "&callback=?",
				function(entries) {
			if (checkData(entries, 'success', "Error adding entry")) {
				if (entries[1] != null) {
					showAlert(entries[1]);
				}
				self.latestEntryId = entries[3].id;
				if (RepeatType.isContinuous(entries[3].repeatType)) {
					self.refreshPinnedEntries(entries[0]);
				} else {
					self.storeDeviceEntryStates(entries[4]);
					self.refreshEntries(entries[0]);
				}
				if (entries[2] != null)
					self.autocompleteWidget.update(entries[2][0], entries[2][1], entries[2][2], entries[2][3], entries[2][4]);
				if (callBack && typeof callBack == 'function') {
					callBack();
				}
				// Scroll to position where the entry was added and glow
				if (!RepeatType.isContinuous(entries[3].repeatType)) {
					var entryContainer = $('#recordList'), scrollTo = $('#' + self.editId + 'entryid' + entries[3].id),
					contentWrapper = $('#' + self.editId + 'entryid' + entries[3].id + ' .content-wrapper');
					entryContainer.animate({
						scrollTop: scrollTo.offset().top - entryContainer.offset().top + entryContainer.scrollTop()
					});
					var originalBackgroundColor = scrollTo.css('background-color');
					scrollTo.css('background-color', '#F14A42');
					contentWrapper.css('color', '#fff');
					setTimeout(function() {
						scrollTo.css('background-color', originalBackgroundColor);
						contentWrapper.css('color', '');
					}, 1000);
				}
			}
		});
	}

	this.getRepeatParamsStr = function(repeatTypeId, repeatEnd) {
		var repeatTypeParam = '';
		var repeatEndParam = '';
		if (repeatTypeId) {
			repeatTypeParam = "&repeatTypeId=" + repeatTypeId;
		}
		if (repeatEnd && typeof repeatEnd == 'string') {
			repeatEnd = new Date(repeatEnd).setHours(23, 59, 59, 0);
			var now = new Date();
			if(new Date(repeatEnd) < now) {
				now.setHours(23, 59, 59, 0);
				repeatEnd = now;
			}
			repeatEndParam = "&repeatEnd=" + new Date(repeatEnd).toUTCString();
		}
		return repeatEndParam + repeatTypeParam;
	}

	this.initInput = function() {
		if (!$("#" + this.editId).data('entryTextSet')) {
			this.setEntryText('');
		}
	}

	this.processInput = function() {
		var $field = $("#" + this.editId);
		$field.autocomplete("close");
		$('.repeat-modifiers').addClass('hide');
		var text = $field.val();
		if (text == "") return; // no entry data
		$field.val("");
		var repeatTypeId = this.getRepeatTypeId('new');
		var repeatEnd = $('.choose-date-input').val();
		$('.entry-details-form').trigger('reset');
		this.addEntry(currentUserId, text, this.defaultToNow, repeatTypeId, repeatEnd);
		return true;
	}

	this.getRepeatTypeId = function(idSelector) {
		var isRepeat = $('#' + idSelector + '-repeat-checkbox').is(':checked');
		var setAlert = $('#' + idSelector + '-remind-checkbox').is(':checked');

		if (!isRepeat && !setAlert) {
			return false;
		}

		var confirmRepeat = $('#' + idSelector + '-confirm-each-repeat').is(':checked');
		var frequencyBit = RepeatType.DAILY_BIT;	// Repeat daily by default
		var repeatTypeBit;
		var frequency = $('input[name=' + idSelector + '-repeat-frequency]:checked').val();

		if (isRepeat) {
			if (frequency == 'daily') {
				frequencyBit = RepeatType.DAILY_BIT;
			} else if (frequency == 'monthly') {
				frequencyBit = RepeatType.MONTHLY_BIT;
			} else if (frequency == 'weekly') {
				frequencyBit = RepeatType.WEEKLY_BIT;
			}
			repeatTypeBit = RepeatType.CONCRETEGHOST_BIT | frequencyBit;

			// Confirm repeat should only be applicable if repeat is enabled
			if (confirmRepeat) {
				repeatTypeBit = repeatTypeBit | RepeatType.GHOST_BIT;
			}
		}

		if (setAlert) {
			if (repeatTypeBit) {
				repeatTypeBit = (RepeatType.REMIND_BIT | repeatTypeBit);
			} else {
				repeatTypeBit = RepeatType.REMIND_BIT;
			}
		}

		return (repeatTypeBit);
	};

	this.createPinnedEntry = function(editId) {
		editId = editId || this.editId;
		var $field = $("#" + editId);
		var text = $field.val();
		if (text != '') {
			this.addEntry(currentUserId, text, this.defaultToNow, RepeatType.CONTINUOUSGHOST);
		}
	}

	this.setEntryText = function(text, startSelect, endSelect) {
		var $inp = $("#" + this.editId);
		$inp.autocomplete("close");
		$inp.val(text);
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

		hidePopover($unselectee.find('.track-input-modifiers'));

		var $contentWrapper = $unselectee.find(".content-wrapper");
		var displayText = $unselectee.data('contentHTML');

		if (displayNewText) {
			var newText = $('#' + this.editId + 'tagTextInput').val();
			if (newText) {
				displayText = newText;
			}
		}

		$unselectee.find(".edit").show();
		$unselectee.find(".save").hide();

		$('#' + $unselectee.attr('id') + ' .track-input-dropdown').hide();

		$contentWrapper.html(displayText);
		$contentWrapper.show();
		if (displaySpinner) {
			$contentWrapper.append(" &nbsp;<img src='/images/spinner.gif' />");
		}

		$("#" + this.editId + "tagTextEdit").remove();
		$unselectee.removeClass('ui-selected');
		if ($unselectee.find('.comment-label').length > 0) {
			$unselectee.find('.comment-label').show();
		}
	}

	/**
	 * Used to select an entry.
	 */
	this.selectEntry = function($selectee) {
		hidePopover();
		console.debug('Select Entry:', $selectee.attr("id"));
		var entry = $selectee.data("entry");

		console.log('width: ', $(window).width());
		if ($(window).width() <= 1025) {
			// Hides tag list, if not already hidden hide it
			var elementToCollapse = $("#tagNav");
			var isHidden = elementToCollapse.is(":hidden");
			var triangleElement = $("span.icon-triangle");

			if (!isHidden) {	// Means tags going to hide.
				$("body").toggleClass("tags-collapsed", true);
				$("body").toggleClass("tags-displayed", false);
				triangleElement.removeClass("icon-triangle-down").addClass("icon-triangle-right");
				elementToCollapse.slideToggle(5, function() {
					if (window.afterTagCollapseToggle) {
						window.afterTagCollapseToggle();
					}
				});
			}
		}

		$selectee.siblings().removeClass("ui-selected");
		if ($selectee.find('.comment-label').length > 0) {
			$selectee.find('.comment-label').hide();
		}
		var $contentWrapper = $selectee.find(".content-wrapper");
		$selectee.find(".edit").hide();
		$selectee.find(".save").show();

		$selectee.data('contentHTML', $contentWrapper.html()); // store original HTML for later restoration
		var currentEntryId = $selectee.data("entryId");
		$selectee.addClass('ui-selected');
		var entryText = $contentWrapper.text();

		var selectRange = self.entrySelectData[currentEntryId];
		if (selectRange != undefined) {
			if (selectRange[2]) { // insert space at selectRange[0]
				entryText = entryText.substr(0, selectRange[0] - 1) + " " + entryText.substr(selectRange[0] - 1);
			}
		}
		var repeatType = $selectee.data("entry").repeatType;

		$selectee.data('originalText', entryText); // store entry text for comparison
		$contentWrapper.hide();

		$selectee.append('<span id="' + this.editId + 'tagTextEdit"><input type="text" class="entryNoBlur" id="' +
				this.editId + 'tagTextInput" style="margin: 8px 2px 2px 0px; width: calc(100% - 75px);" /></span>');
		$('#' + $selectee.attr('id') + ' .track-input-dropdown').show();
		var popoverContent = _.template($('#entry-details-popover-content').html())({editType: currentEntryId, entryId: $selectee.attr('id'), repeatType: repeatType});
		createPopover($selectee.find('.track-input-dropdown'), popoverContent, '#recordList');

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

		if (selectRange) {
			$('#' + self.editId + 'tagTextInput').selectRange(selectRange[0], selectRange[1]);
		}

		$(".choose-date-input").datepicker();
		if (entry.repeatEnd) {
			var oldRepeatEndDate = new Date(entry.repeatEnd);
			$(".choose-date-input").datepicker('setDate', oldRepeatEndDate);
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
		var currentEntryId = $unselectee.data('entryId');
		var repeatTypeId = this.getRepeatTypeId(currentEntryId);
		var repeatEnd = $('.choose-date-input').val();
		var oldRepeatEnd = $unselectee.data('entry').repeatEnd;
		var oldRepeatEndMidnightTime = oldRepeatEnd ? oldRepeatEnd.setHours(0, 0, 0, 0) : null;
		var isOldRepeatEndChanged = false;

		if (oldRepeatEndMidnightTime || !isNaN(new Date(repeatEnd).getTime())) {
			isOldRepeatEndChanged = !(oldRepeatEndMidnightTime == new Date(repeatEnd).getTime());
		}

		if ($unselectee.data('isContinuous')) {
			console.debug('Is a continuous entry:', $unselectee.attr('id'));
			this.unselectEntry($unselectee, true, true);
			this.updateEntry(currentEntryId, newText, this.defaultToNow, repeatTypeId, repeatEnd);
		} else if (!$unselectee.data('isGhost') && ($unselectee.data('originalText') == newText &&
				$unselectee.data("entry").repeatType == repeatTypeId && !isOldRepeatEndChanged)) {
			console.debug('Is not remind & no change in entry.');
			this.unselectEntry($unselectee);
		} else {
			console.log('Either remind or change in entry.');
			this.unselectEntry($unselectee, true, true);
			this.updateEntry(currentEntryId, newText, this.defaultToNow, repeatTypeId, repeatEnd);
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

		if ($target[0].className == 'make-pin-button' || $target[0].className === 'bookmark-icon') {
			if ($target.closest('#recordList').length == 0) {
				// On clicking the bookmark button from input-affordance, hide the popoup and reset the affordance.
				self.createPinnedEntry();
				$('#input0').val('');
				$('.entry-details-form').trigger('reset');
				$('.repeat-modifiers').addClass('hide');
			} else {
				self.createPinnedEntry('input0tagTextInput');
			}
			hidePopover($target.parents('#addData').find('.track-input-dropdown'));
			return;
		}

		var $alreadySelectedEntry = $("li.entry.ui-selected");

		// Checking if any entry is already selected when mousedown event triggered.
		var isAnyEntrySelected = $alreadySelectedEntry.length > 0;

		// When clicked on any entry element i.e. li.entry
		var clickedOnEntry = $target.closest("li.entry").length > 0;

		// If such elements are clicked, where we not have to do anything. (Like deleteEntry)
		var isEventToCancel = ($target.closest(".entryNoBlur").length > 0) || $target.is(".save-entry");

		// On clicking Details button show Popover.
		if ($target.is('.track-input-dropdown, .track-input-dropdown img')) {
			var $launcher = $target.hasClass('track-input-dropdown') ? $target : $target.parent();
			showPopover($launcher);
			return
		}

		// Hide popover on clicking on either popover close button or Save entry button or delete entry button.
		if ($target.is('#close-track-input-modifier, #close-track-input-modifier .fa-times, .save-entry, .edit-delete')) {
			if ($('.popover').length !== 0) {
				hidePopover();
			}
		}

		if (isEventToCancel) {
			return;
		}

		var $deviceEntry = $target.closest(".device-entry");
		var $deviceSummaryEntry = $target.closest(".device-summary-entry");
		var clickedOnDeviceEntry = $deviceEntry.length !== 0;
		var clickedOnDeviceSummaryEntry = $deviceSummaryEntry.length !== 0;

		// If a device entry (example: "Moves Data") or device summary entry is clicked
		if (clickedOnDeviceEntry || clickedOnDeviceSummaryEntry) {
			if (clickedOnDeviceEntry) {
				self.toggleDeviceEntry($deviceEntry);
			} else if (clickedOnDeviceSummaryEntry) {
				self.toggleDeviceSummaryEntry($deviceSummaryEntry);
			}

			if (isAnyEntrySelected) {
				console.debug('Device entry clicked. Will now unselect selected entry.');
				self.unselectEntry($alreadySelectedEntry);
			}

			return false;
		}

		/*
		 * Unselect entry only if there is a previously selected entry and target was not the Details button or was not
		 * in the popover or datepicker.
		 */
		if (isAnyEntrySelected && !$target.is('.track-input-dropdown, .track-input-dropdown img') && $target.parents('.popover').length === 0 &&
			$target.parents('#ui-datepicker-div').length === 0 ) {
			console.debug('Mousedown: There is a selected entry. Will now unselect.');
			// Do nothing when user click outside https://github.com/syntheticzero/curious2/issues/783#issue-123920844
			//self.checkAndUpdateEntry($("li.entry.ui-selected"));
			// Just unselect the entry
			self.unselectEntry($alreadySelectedEntry);
			return;
		}

		if (clickedOnEntry) {
			// parents() method returns all ancestors as list. So element at 0th position will be li.entry
			var selectee = $target.parents("li.entry").andSelf()[0];
			console.debug('Mousedown: Clicked on an entry. Will now select.');
			self.selectEntry($(selectee));
			return false;
		}

		// Hide popover if clicked anywhere outside the popover ,details button or datepicker.
		if (!$target.is('.track-input-dropdown, .track-input-dropdown img') && $target.parents('.popover').length === 0 &&
			$target.parents('#ui-datepicker-div').length === 0 ) {
			hidePopover();
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

		/*
		 * Update the date picker with already selected date (if any) or to the currentDate to avoid re-setting the
		 * date to "today" since this method also being called on window resize.
		 */
		$datepicker.val($.datepicker.formatDate(datepickerFormat, this.cachedDate || currentDate));
	};

	$("#" + self.editId).droppable({
		drop : function(event, ui) {
			var droppedItem = $(ui.draggable[0]).data(DATA_KEY_FOR_ITEM_VIEW).getData();
			self.setEntryText(droppedItem.description);
		}
	});

	var $datepicker = $("#" + self.calendarId);

	$datepicker
			.datepicker({defaultDate: currentDate, dateFormat: 'DD MM dd, yy', showButtonPanel: true, changeYear: true, changeMonth: true})
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

		var $popover = $('.popover');

		if(!$popover.length) {
			return;
		}

		// Adjusting the position of the popover on window resize.
		var $popoverlauncher = $('.ui-selected').find('.track-input-modifiers');
		var top = $popoverlauncher.offset().top + $popoverlauncher.outerHeight(true);
		var right = $(window).innerWidth() - ($popoverlauncher.offset().left + $popoverlauncher.outerWidth(true));
		$popover.css('top', top);
		$popover.css('right', right);
	});
	self.adjustDatePicker();

	$("#" + this.editId)
			.on("click", function(e) {
				if (!$(this).data('entryTextSet')) {
					self.setEntryText('');
				}
			})
			.keyup(function(e) {
				if (e.keyCode == 13) {	// Enter pressed.
					self.processInput();
				}
			});

	$(document).on("click", ".save-entry", function() {
		var $selectedEntry = $("li.ui-selected", "#" + self.listId);
		if (!$selectedEntry || ($selectedEntry.length === 0)) {
			// Means button is clicked for a new entry
			self.processInput();
			return;
		}

		self.checkAndUpdateEntry($selectedEntry);
	});

	/**
	 * Keycode= 37:left, 38:up, 39:right, 40:down
	 */
	$("#" + self.listId).keydown(function(e) {
		if ($.inArray(e.keyCode, [38, 40]) == -1) {
			return true;
		}
		var $unselectee = $("li.ui-selected", "#" + self.listId);
		if (!$unselectee) {
			return false;
		}

		/**
		 * Only select the previous/next entry on Keyboard Up/Down key which is editable and visible. This is to
		 * avoid selecting either the device entry, device summary entry or any simple entry collapsed inside the
		 * device summary entry.
		 * @type {string}
		 */
		var selector = ".editable-entry:visible:first";
		var $selectee;
		if (e.keyCode == 40) {
			$selectee = $unselectee.nextAll(selector);
		} else if (e.keyCode == 38) {
			$selectee = $unselectee.prevAll(selector);
		}
		if ($selectee) {
			self.nextSelectionId  = $selectee.attr('id');
			self.checkAndUpdateEntry($unselectee);
			self.selectEntry($selectee, false);
		}
		return false;
	});

	self.refresh();
}
