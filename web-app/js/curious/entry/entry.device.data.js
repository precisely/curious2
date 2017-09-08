"use strict";

/**
 * Class used to group the given entries based on the matching base tag.
 *
 * @params deviceEntries List of plain entries received from the server for a partucular device. For example: all
 * entries from Oura.
 * @params savedStates Map of saved state of collapse/expand for the user
 */
function EntryDeviceData(deviceEntries, savedStates) {

	StateView.call(this);

	if (!deviceEntries || !deviceEntries[0]) {
		throw "At least one entry need to be passed."
	}

	var groupedData = {};
	var collapsed = false;

	var sourceName = deviceEntries[0].sourceName;
	var source = sourceName.split(" ")[0].toUpperCase();

	if (savedStates[source]) {     // If user has previously collapsed the device top level entry
		collapsed = true;
	}

	this.getSanitizedSourceName = function() {
		return sourceName.sanitizeTitle();
	};

	this.getTriangle = function () {
		if (collapsed) {
			return '<i class="fa fa-fw toggle-icon fa-chevron-right"></i>';
		}

		return '<i class="fa fa-fw toggle-icon fa-chevron-down"></i>';
	};

	this.getDisplayText = function () {
		var text = this.getTriangle();
		return text + ' ' + deviceEntries[0]["sourceName"];
	};

	this.group = function() {
		jQuery.each(deviceEntries, function(index, entry) {
			if (!entry["normalizedAmounts"]) {
				return;
			}

			//Below is the code to round off the values to 1 decimal places if the value is not a whole number.
			for (let key in entry.amounts) {
				if (Math.floor(entry.amounts[key].amount) !== entry.amounts[key].amount) {
					entry.amounts[key].amount = entry.amounts[key].amount.toFixed(1);
				}
			}

			var description = entry["description"];
			var currentGroup = groupedData[description] = groupedData[description] || [];
			currentGroup.push(entry);
		});

		return groupedData;
	};

	this.collapse = function(saveState) {
		collapsed = true;
		if (saveState) {
			this.saveState();
		}
	};

	this.expand = function(saveState) {
		collapsed = false;
		if (saveState) {
			this.saveState(saveState);
		}
	};

	this.isCollapsed = function() {
		return collapsed;
	};

	// This is just the helper method for readability and to provide implementation of getCurrentState method
	this.getCurrentState = function() {
		return this.isCollapsed();
	};

	this.saveState = function() {
		var params = {
			isCollapsed: this.getCurrentState(),
			device: source
		};

		params = getCSRFPreventionObject("saveDeviceEntriesStateDataCSRF", params);

		backgroundPostJSON("Saving state", "/home/saveDeviceEntriesStateData", params, function(data) {
			if (!checkData(data)) {
				return;
			}
			console.log("Preference saved.");
		}, function(resp) {
			console.log("Could not save preference.", resp);
		});
	};
}

inherit(EntryDeviceData, StateView);
