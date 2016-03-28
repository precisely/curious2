"use strict";

function EntryDeviceData(deviceEntries, settings) {

    StateView.call(this);

    /* Private members start */

    var groupedData = {};
    var collapsed = false;
    var source, sourceName;

    /* Private members end */

    if (deviceEntries && deviceEntries[0]) {
        sourceName = deviceEntries[0].sourceName;
        source = sourceName.split(" ")[0].toUpperCase();

        if (settings[source]) {     // If user has previously collapsed the device top level entry
            collapsed = true;
        }
    }

    /* Getters start */

    this.getSanitizedSourceName = function() {
        return sourceName.sanitizeTitle();
    };

    this.getTriangle = function () {
        if (collapsed) {
            return '<i class="fa toggle-icon fa-chevron-right"></i>';
        }

        return '<i class="fa toggle-icon fa-chevron-down"></i>';
    };

    this.getDisplayText = function () {
        var text = this.getTriangle();
        return text + ' ' + deviceEntries[0]["sourceName"];
    };

    /* Getters end */

    this.group = function() {
        jQuery.each(deviceEntries, function(index, entry) {
            if (!entry["normalizedAmounts"]) {
                return;
            }

            var description = entry["description"];
            var currentGroup = groupedData[description] = groupedData[description] || [];
            currentGroup.push(entry);
        });

        return groupedData;
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
        return collapsed;
    };

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