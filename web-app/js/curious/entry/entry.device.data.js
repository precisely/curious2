"use strict";

function EntryDeviceData(deviceEntries) {

    /* Private members start */

    var groupedData = {};
    var collapsed = false;

    /* Private members end */

    /* Getters start */

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
    };

    this.expand = function() {
        collapsed = false;
    };

    this.isCollapsed = function() {
        return collapsed;
    }
}