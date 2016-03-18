"use strict";

/**
 *
 * @param deviceTagEntries will be the list of entries with same base tag like "walk", "bike", "excercise"
 * for a particular device like "Moves".
 */
function EntryDeviceDataSummary(deviceTagEntries) {

    var aggregatedUnitAmounts;
    var groupedData = {};
    var collapsed = true;
    var baseTag = "";
    var deviceName = "";

    if (deviceTagEntries && deviceTagEntries[0]) {
        baseTag = deviceTagEntries[0].description;
        deviceName = deviceTagEntries[0].sourceName;
    }

    this.getBaseTag = function() {
        return baseTag;
    };

    this.getDeviceName = function() {
        return deviceName;
    };

    this.getAssociatedEntriesClass = function() {
        return this.getDeviceName().sanitizeTitle() + "-" + this.getBaseTag();
    };

    this.getTriangle = function () {
        if (collapsed) {
            return '<i class="fa toggle-icon fa-chevron-right"></i>';
        }

        return '<i class="fa toggle-icon fa-chevron-down"></i>';
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
            aggregateIndex++;
        }
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