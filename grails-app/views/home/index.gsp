<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />
<script type="text/javascript">
/**
 * Custom Curious mobile widget forked from jQuery UI selectable widget
 */
(function( $, undefined ) {

$.widget("ui.listable", $.ui.mouse, {
	options: {
		appendTo: 'body',
		autoRefresh: true,
		distance: 0,
		filter: '*',
		tolerance: 'touch'
	},
	_create: function() {
		var self = this;

		this.element.addClass("ui-selectable");

		this.dragged = false;

		// cache selectee children based on filter
		var selectees;
		this.refresh = function() {
			selectees = $(self.options.filter, self.element[0]);
			selectees.each(function() {
				var $this = $(this);
				var pos = $this.offset();
				$.data(this, "selectable-item", {
					element: this,
					$element: $this,
					left: pos.left,
					top: pos.top,
					right: pos.left + $this.outerWidth(),
					bottom: pos.top + $this.outerHeight(),
					startselected: false,
					selected: $this.hasClass('ui-selected'),
					selecting: $this.hasClass('ui-selecting'),
					unselecting: $this.hasClass('ui-unselecting')
				});
			});
		};
		this.refresh();

		this.selectees = selectees.addClass("ui-selectee");

		this._mouseInit();

		this.helper = $("<div class='ui-selectable-helper'></div>");
	},

	destroy: function() {
		this.selectees
			.removeClass("ui-selectee")
			.removeData("selectable-item");
		this.element
			.removeClass("ui-selectable ui-selectable-disabled")
			.removeData("selectable")
			.unbind(".selectable");
		this._mouseDestroy();

		return this;
	},

	_mouseStart: function(event) {
		var self = this;

		this.opos = [event.pageX, event.pageY];

		if (this.options.disabled)
			return;

		var options = this.options;

		this.selectees = $(options.filter, this.element[0]);

		this._trigger("start", event);

		$(options.appendTo).append(this.helper);
		// position helper (lasso)
		this.helper.css({
			"left": event.clientX,
			"top": event.clientY,
			"width": 0,
			"height": 0
		});

		if (options.autoRefresh) {
			this.refresh();
		}

		this.selectees.filter('.ui-selected').each(function() {
			var selectee = $.data(this, "selectable-item");
			selectee.startselected = true;
			if (!event.metaKey) {
				selectee.$element.removeClass('ui-selected');
				selectee.selected = false;
				selectee.$element.addClass('ui-unselecting');
				selectee.unselecting = true;
				// selectable UNSELECTING callback
				self._trigger("unselecting", event, {
					unselecting: selectee.element
				});
			}
		});

		$(event.target).parents().andSelf().each(function() {
			var selectee = $.data(this, "selectable-item");
			if (selectee) {
				var doSelect = !event.metaKey || !selectee.$element.hasClass('ui-selected');
				selectee.$element
					.removeClass(doSelect ? "ui-unselecting" : "ui-selected")
					.addClass(doSelect ? "ui-selecting" : "ui-unselecting");
				selectee.unselecting = !doSelect;
				selectee.selecting = doSelect;
				selectee.selected = doSelect;
				// selectable (UN)SELECTING callback
				if (doSelect) {
					self._trigger("selecting", event, {
						selecting: selectee.element
					});
				} else {
					self._trigger("unselecting", event, {
						unselecting: selectee.element
					});
				}
				return false;
			}
		});

	},

	_mouseDrag: function(event) {
		var self = this;
		this.dragged = true;

		if (this.options.disabled)
			return;

		var options = this.options;

		var x1 = this.opos[0], y1 = this.opos[1], x2 = event.pageX, y2 = event.pageY;
		if (x1 > x2) { var tmp = x2; x2 = x1; x1 = tmp; }
		if (y1 > y2) { var tmp = y2; y2 = y1; y1 = tmp; }
		this.helper.css({left: x1, top: y1, width: x2-x1, height: y2-y1});

		this.selectees.each(function() {
			var selectee = $.data(this, "selectable-item");
			//prevent helper from being selected if appendTo: selectable
			if (!selectee || selectee.element == self.element[0])
				return;
			var hit = false;
			if (options.tolerance == 'touch') {
				hit = ( !(selectee.left > x2 || selectee.right < x1 || selectee.top > y2 || selectee.bottom < y1) );
			} else if (options.tolerance == 'fit') {
				hit = (selectee.left > x1 && selectee.right < x2 && selectee.top > y1 && selectee.bottom < y2);
			}

			if (hit) {
				// SELECT
				if (selectee.selected) {
					selectee.$element.removeClass('ui-selected');
					selectee.selected = false;
				}
				if (selectee.unselecting) {
					selectee.$element.removeClass('ui-unselecting');
					selectee.unselecting = false;
				}
				if (!selectee.selecting) {
					selectee.$element.addClass('ui-selecting');
					selectee.selecting = true;
					// selectable SELECTING callback
					self._trigger("selecting", event, {
						selecting: selectee.element
					});
				}
			} else {
				// UNSELECT
				if (selectee.selecting) {
					if (event.metaKey && selectee.startselected) {
						selectee.$element.removeClass('ui-selecting');
						selectee.selecting = false;
						selectee.$element.addClass('ui-selected');
						selectee.selected = true;
					} else {
						selectee.$element.removeClass('ui-selecting');
						selectee.selecting = false;
						if (selectee.startselected) {
							selectee.$element.addClass('ui-unselecting');
							selectee.unselecting = true;
						}
						// selectable UNSELECTING callback
						self._trigger("unselecting", event, {
							unselecting: selectee.element
						});
					}
				}
				if (selectee.selected) {
					if (!event.metaKey && !selectee.startselected) {
						selectee.$element.removeClass('ui-selected');
						selectee.selected = false;

						selectee.$element.addClass('ui-unselecting');
						selectee.unselecting = true;
						// selectable UNSELECTING callback
						self._trigger("unselecting", event, {
							unselecting: selectee.element
						});
					}
				}
			}
		});

		return false;
	},

	_mouseStop: function(event) {
		var self = this;

		this.dragged = false;

		var options = this.options;

		$('.ui-unselecting', this.element[0]).each(function() {
			var selectee = $.data(this, "selectable-item");
			selectee.$element.removeClass('ui-unselecting');
			selectee.unselecting = false;
			selectee.startselected = false;
			self._trigger("unselected", event, {
				unselected: selectee.element
			});
		});
		$('.ui-selecting', this.element[0]).each(function() {
			var selectee = $.data(this, "selectable-item");
			selectee.$element.removeClass('ui-selecting').addClass('ui-selected');
			selectee.selecting = false;
			selectee.selected = true;
			selectee.startselected = true;
			self._trigger("selected", event, {
				selected: selectee.element
			});
		});
		this._trigger("stop", event);

		this.helper.remove();

		return false;
	}

});

$.extend($.ui.listable, {
	version: "1.8.16"
});

})(jQuery);

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

function displayEntry(id, date, datePrecisionSecs, description, amount, amountPrecision, units, comment) {
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
	
	$("#entry0").append("<li id=\"entryid" + id + "\">" + (timeAfterTag ? '' : '<span class="entryTime">' + escapehtml(dateStr) + '</span>') + '<span class="entryDescription">'
			+ escapehtml(description) + '</span>' + '<span class="entryAmount">' + escapehtml(formattedAmount) + '</span>'
			+ '<span class="entryUnits">' + escapehtml(formatUnits(units)) + '</span>' + (timeAfterTag ? '<span class="entryTime">'
			+ escapehtml(dateStr) + '</span>' : '') + (comment != '' ? ' ' + '<span class="' + (comment.startsWith('repeat') || comment.startsWith('daily') || comment.startsWith('weekly') || comment.startsWith('remind') ? 'entryRepeat' : 'entryComment') + '">' + escapehtml(comment) + '</span>' : '')
			+ '<a href="#" class="entryDelete" id="entrydelid' + id + '" onclick="deleteEntryId(' + id + ')"><img width="12" height="12" src="/images/x.gif"></a></li>');
}

function displayEntries(entries) {
	entrySelectData = {};
	jQuery.each(entries, function() {
		displayEntry(this['id'], this['date'], this['datePrecisionSecs'], this['description'], this['amount'], this['amountPrecision'], this['units'], this['comment']);
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
			refreshEntries(entries[0]);
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
			+ "&timeZoneOffset=" + timeZoneOffset + "&defaultToNow=" + (defaultToNow ? '1':'0') + "&callback=?",
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
	$("#entry0").listable({cancel:'a'});
	$("#entry0").off("listableselected");
	$("#entry0").off("listableunselecting");
	$("#entry0").on("listableunselecting", function(e, ui) {
		var $unselectee = $("#" + ui.unselecting.id);
		unselecting($unselectee);
	});
	function unselecting($unselectee) {
		if($unselectee.data('entryIsSelected') == 1) {
			$unselectee.removeClass('ui-selected');
			$unselectee.data('entryIsSelected', 2);
			$("a.entryDelete", $unselectee).hide();
			currentEntryId = null;
			setEntryText('');
		}
	}
	$("#entry0").on("listableselected", function(e, ui) {
		var $selectee = $("#" + ui.selected.id);
		selected($selectee);
	});
	function selected($selectee) {
		currentEntryId = null;
		if($selectee.attr("id")) {
			currentEntryId = $selectee.attr("id").substring(7);
		}
		var state = $selectee.data('entryIsSelected');
		if (state == 1) {
			$selectee.removeClass('ui-selected');
			$selectee.data('entryIsSelected', 0);
			$("a.entryDelete", $selectee).hide();
			currentEntryId = null;
			setEntryText('');
		} else if (!state) {
			$selectee.data('entryIsSelected', 1);
			$selectee.addClass('ui-selected');
			$("#entrydelid" + currentEntryId).css('display', 'inline');
			var entryText = $selectee.text();
			setEntryText(entryText);
			var selectRange = entrySelectData[currentEntryId];
			if (selectRange)
				setEntryText(entryText, selectRange[0], selectRange[1]);
			else
				setEntryText(entryText);
			if (lastEntrySelected != null)
				lastEntrySelected.data('entryIsSelected', 0);
			lastEntrySelected = $selectee;
		} else if (state == 2) {
			$selectee.removeClass('ui-selected');
			$selectee.data('entryIsSelected', 0);
		}
	}
	/**
	 * Keycode: 37:left, 38:up, 39:right, 40:down
	 */
	$(document).keydown(function(e) {
		if($.inArray(e.keyCode, [37, 38, 39, 40]) == -1) {
			return true;
		}
		var $unselectee = $("li.ui-selected", "ol#entry0");
		if(!$unselectee) {
			//$currentSelectedEntry = $("li:first-child", "ol#entry0");
		}
		if(!$unselectee) {
			return false;
		}
		var $selectee;
		if(e.keyCode == 37 || e.keyCode == 40) { 
			$selectee = $unselectee.next();
		}
		if(e.keyCode == 38 || e.keyCode == 39) { 
			$selectee = $unselectee.prev();
		}
		if($selectee) {
			unselecting($unselectee);
			selected($selectee);
		}
		return false
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
	
	$.getJSON("/home/getPeopleData?callback=?",
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
</script>
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
