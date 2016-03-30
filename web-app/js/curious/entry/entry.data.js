"use strict";

function EntryData(entriesData) {

	/* Private members start */

	var deviceEntries, normalEntries;
	var entries = entriesData;

	/* Private members end */

	/* Getters start */

	this.getDeviceEntries = function() {
		return deviceEntries;
	};

	this.getNormalEntries = function() {
		return normalEntries;
	};

	/* Getters end */

	this.collectDeviceEntries = function() {
		deviceEntries = {};
		normalEntries = [];

		jQuery.each(entries, function(index, entry) {
			var source = entry["sourceName"];
			if (source) {
				deviceEntries[source] = deviceEntries[source] || [];
				deviceEntries[source].push(entry);
			} else {
				normalEntries.push(entry);
			}
		}.bind(this));
	};
}