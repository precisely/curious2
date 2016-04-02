"use strict";

function EntryData(entriesData) {

	var deviceEntries, normalEntries;
	var entries = entriesData;

	this.getDeviceEntries = function() {
		return deviceEntries;
	};

	this.getNormalEntries = function() {
		return normalEntries;
	};

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