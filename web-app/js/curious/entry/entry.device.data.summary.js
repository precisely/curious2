"use strict";

/**
 * Class to construct device summary entries for each base tag by using normalized amounts of given entries.
 * Call the method "group()" after passing the device entries to get the entry data for device summary entry.
 
 * @param deviceTagEntries will be the list of entries with same base tag like "walk", "bike", "exercise"
 * for a particular device like "Moves".
 */
function EntryDeviceDataSummary(deviceTagEntries) {

	StateView.call(this);

	if (!deviceTagEntries || !deviceTagEntries[0]) {
		throw "At least one entry need to be passed."
	}

	var aggregatedUnitAmounts;
	var groupedData = {};

	var baseTag = deviceTagEntries[0].description;
	var deviceName = deviceTagEntries[0].sourceName;
	var source = deviceName.split(" ")[0].toUpperCase();
	var collapsed = true;

	var entrySessionStorage = store.namespace("entries.state").session;      // store2.min.js
	// Will be like "oura.device.summary.sleep"
	var storageKey = source.toLowerCase() + ".device.summary." + baseTag;

	this.getBaseTag = function() {
		return baseTag;
	};

	this.getDeviceName = function() {
		return deviceName;
	};

	this.getAssociatedEntriesClass = function() {
		return this.getDeviceName().sanitizeTitle() + "-" + this.getBaseTag().sanitizeTitle();
	};

	this.getTriangle = function () {
		if (this.isCollapsed()) {
			return '<i class="fa fa-fw toggle-icon fa-chevron-right"></i>';
		}

		return '<i class="fa fa-fw toggle-icon fa-chevron-down"></i>';
	};

	this.group = function () {
		deviceTagEntries.forEach(function(entry) {
			var normalizedAmounts = entry["normalizedAmounts"];
			if (!normalizedAmounts) {
				return;
			}

			for (var i in normalizedAmounts) {
				var currentGroup = groupedData[normalizedAmounts[i].units] =
						groupedData[normalizedAmounts[i].units] || [];
				currentGroup.push(normalizedAmounts[i]);
			}
		}.bind(this));

		this.aggregateUnits();
		var copiedEntry = jQuery.extend(true, {}, deviceTagEntries[0]);
		copiedEntry.id = -1;
		copiedEntry.amounts = aggregatedUnitAmounts;

		return copiedEntry;
	};

	this.aggregateUnits = function () {
		aggregatedUnitAmounts = {};

		var aggregateIndex = 0; // Index used to create the amounts object. See DeviceData.js
		for (var unit in groupedData) {
			var groupedUnitData = groupedData[unit];
			var shouldSum;
			var amount = 0;
			for (var i in groupedUnitData) {
				var unitData = groupedUnitData[i];
				var calculatedAmount = aggregatedUnitAmounts[aggregateIndex];
				if (calculatedAmount) {
					amount = calculatedAmount.amount;
				}
				calculatedAmount = aggregatedUnitAmounts[aggregateIndex] = JSON.parse(JSON.stringify(unitData));

				if (typeof calculatedAmount.sum !== 'undefined' && typeof shouldSum == 'undefined') {
					shouldSum = calculatedAmount.sum;
				}
				amount += unitData.amount;
				calculatedAmount.amount = amount;
			}
			if (!shouldSum) {
				calculatedAmount.amount = calculatedAmount.amount / groupedUnitData.length;
			}

			// If there is decimal value
			if ((calculatedAmount.amount % 1) !== 0) {
				// Then only round to 2 decimal
				calculatedAmount.amount = calculatedAmount.amount.toFixed(2);
			}
			aggregateIndex++;
		}
	};

	this.collapse = function() {
		collapsed = true;
		this.saveState();
	};

	this.expand = function() {
		collapsed = false;
		this.saveState();
	};

	this.isCollapsed = function() {
		return !this.getCurrentState();
	};

	this.getCurrentState = function() {
		var summaryPreferences = entrySessionStorage.get(storageKey) || {};
		return summaryPreferences[constructDateKey()];
	};

	/**
	 * Will create an object like:
	 * {
	 *     "MOVES": {
	 *         walk: {
	 *              "09-Feb-2016": true,
	 *              "12-Feb-2016": false
	 *         },
	 *         bike: {
	 *              "09-Feb-2016": false
	 *         }
	 *     },
	 *     "WITHINGS": {
	 *          "sleep": {
	 *               "09-Feb-2016": false
	 *          }
	 *     }
	 * }
	 *
	 * This means, the walk summary entry for 9th Feb from Moves was expanded; while the bike summary entry for 9th
	 * Feb from Moves was collapsed.
	 */
	this.saveState = function() {
		var summaryPreferences = entrySessionStorage.get(storageKey) || {};
		summaryPreferences[constructDateKey()] = !collapsed;
		entrySessionStorage.set(storageKey, summaryPreferences);
	};

	function constructDateKey() {
		return $.datepicker.formatDate('dd-M-yy', deviceTagEntries[0].date);
	}
}

inherit(EntryDeviceDataSummary, StateView);
