"use strict";

function EntryDeviceData(deviceEntries) {

    /* Private members start */

    var groupedData = [];

    /* Private members end */

    /* Getters start */

    this.getTriangle = function () {
        if (this.collapsed) {
            return '<i class="fa fa-chevron-right"></i>';
        }

        return '<i class="fa fa-chevron-down"></i>';
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

        console.log(groupedData)
    }
}