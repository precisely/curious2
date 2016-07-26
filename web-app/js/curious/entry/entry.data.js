"use strict";

/**
 * Accepts all the entries received from the server to separate the normal entries (which is not associated with any
 * source/device) and the device entries (which are imported from any device API). Call methods, "getNormalEntries()"
 * to get the list of normal entries and call "getDeviceEntries" to get the list of device entries.
 *
 * @param entries List of entries
 */
function EntryData(entries) {

	var deviceEntries, normalEntries;

	this.getDeviceEntries = function() {
		return deviceEntries;
	};

	this.getNormalEntries = function() {
		return normalEntries;
	};

	function collectDeviceEntries() {
		deviceEntries = {};
		normalEntries = [];
		// If a key "disableDeviceDataGrouping" in localStorage is set
		if (window.store && window.store.get("disableDeviceDataGrouping")) {
			// Then don't group entries. Might be used for debugging
			normalEntries = entries;
			return;
		}

		jQuery.each(entries, function(index, entry) {
			var source = entry["sourceName"];
			if (source) {
				deviceEntries[source] = deviceEntries[source] || [];
				deviceEntries[source].push(entry);
			} else {
				normalEntries.push(entry);
			}
		}.bind(this));
	}

	collectDeviceEntries();
}
