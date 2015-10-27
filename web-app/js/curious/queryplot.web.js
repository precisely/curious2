function PlotWeb(tagList, userId, userName, plotAreaDivId, store, interactive, properties) {
	Plot.apply(this, arguments);
	var plot = this;

	this.showAlert = function(alertMessage) {
		showAlert(alertMessage);
	};

	this.queuePostJSON = function(description, url, args, successCallback, failCallback, delay) {
		queuePostJSON(description, url, args, successCallback, failCallback, delay);
	};

	this.queueJSON = function(description, url, args, successCallback, failCallback, delay, post, background) {
		queueJSON(description, url, args, successCallback, failCallback, delay, post, background);
	}

	this.makeGetUrl = function(url) {
		return makeGetUrl(url);
	}

	this.makePlainUrl = function(url) {
		return makePlainUrl(url);
	}

	this.makePostUrl = function(url) {
		return makePostUrl(url);
	}

	this.checkData = function(data, status, errorMessage, successMessage) {
		checkData(data, status, errorMessage, successMessage);
	}

	this.makeGetArgs = function(args) {
		return makeGetArgs(args);
	}
	var datepicker = this.properties.getStartDatePicker();

	datepicker.datepicker({dateFormat: 'DD MM dd, yy', disabled:!this.interactive,
		beforeShow: function() {
			setTimeout(function(){
				$('.ui-datepicker').css('z-index', 99999999999999);
			}, 0);
		},
		onClose: function(dateText, inst) {
			if (dateText == "") {
				properties.initStartDate();
				plot.loadAllData();
			}
		}
	});

	datepicker.change(function () {
		this.loadAllData();
	}.bind(this));

	datepicker = properties.getEndDatePicker();

	datepicker.datepicker({dateFormat: 'DD MM dd, yy', disabled:!plot.interactive,
		beforeShow: function() {
			setTimeout(function(){
				$('.ui-datepicker').css('z-index', 99999999999999);
			}, 0);
		},
		onClose: function(dateText, inst) {
			if (dateText == "") {
				plot.properties.initEndDate();
				plot.loadAllData();
			}
		}
	});

	datepicker.change(function () {
		this.loadAllData();
	}.bind(this));

	if (this.interactive) {
		var nameField = this.properties.getNameField();
		var renameFunction = function() {
			var newName = prompt("Rename plot:", plot.getName());

			if (newName) {
				this.setManualName(true);
				this.setName(newName);
				this.store();
				$(document).trigger(afterQueryTitleChangeEvent);
			}
		};
		nameField.off('mouseup');
		nameField.on('mouseup', renameFunction);
		var renameField = this.properties.getRenameField();
		if (renameField) {
			renameField.off('mouseup');
			renameField.on('mouseup', renameFunction);
		}
	}

	this.setupSlider = function() {
		if (!this.interactive) return;

		var zoomDiv = this.properties.getZoomControl();
		if (!zoomDiv) return;

		var leftSlider, rightSlider;

		if (this.cycleTagLine) {
			leftSlider = this.leftCycleSlider == undefined ? -5780 : this.leftCycleSlider;
			rightSlider = this.rightCycleSlider == undefined ? 5780 : this.rightCycleSlider;
		} else {
			var startTime = this.properties.getStartTime();
			var endTime = this.properties.getEndTime();

			if (!startTime) startTime = this.minTime;
			if (!endTime) endTime = this.maxTime;

			var span = endTime - startTime;

			if (!this.leftLinearSlider) leftSlider = -10000;
			else leftSlider = (this.leftLinearSlider - startTime) * 20000 / span - 10000;
			if (!this.rightLinearSlider) rightSlider = 10000;
			else rightSlider = (this.rightLinearSlider - startTime) * 20000 / span - 10000;
		}

		zoomDiv.slider({range: true, min:-10000, max: 10000, values: [leftSlider,rightSlider]});
		zoomDiv.off("slide");
		zoomDiv.on("slide", function(event, ui) {
			return plot.slideCallback(ui.value == ui.values[0] ? 0 : 1, ui.value);
		})

		var plot = this;
		zoomDiv.off("slidestop");
		zoomDiv.on("slidestop", function(event, ui) {
			return plot.store();
		})

		if (this.cycleTagLine) {
			plot.slideCallback(0, leftSlider);
			plot.slideCallback(1, rightSlider);
		}
	}

	this.slideCallback = function(handle, value) {
		if (this.cycleTagLine) {
			if (handle == 0) {
				this.leftCycleSlider = value;
			} else {
				this.rightCycleSlider = value;
			}
			if (handle == 0 && value > 0)
				return false;
			else if (handle == 1 && value < 0)
				return false;
			else {
				if (handle == 0) {
					if (value < 0)
						this.minCycleRange = -Math.exp(4.362 * (-value/5000.0))/77.416;
					else
						this.minCycleRange = Math.exp(4.362 * (value/5000.0))/77.416;
				} else {
					if (value < 0)
						this.maxCycleRange = -Math.exp(4.362 * (-value/5000.0))/77.416;
					else
						this.maxCycleRange = Math.exp(4.362 * (value/5000.0))/77.416;
				}
				this.redrawPlot();
				return true;
			}
		} else {
			var startTime = this.properties.getStartTime();
			var endTime = this.properties.getEndTime();

			if (!startTime) startTime = this.minTime;
			if (!endTime) endTime = this.maxTime;

			var span = endTime - startTime;

			var handleTime = undefined;
			if (!((handle == 0 && value == -10000) || (handle == 1 && value == 10000)))
				handleTime = ((value + 10000) / 20000.0) * span + startTime;

			if (handle == 0) {
				this.leftLinearSlider = handleTime;
				if (value == -10000) this.leftLinearSlider = null;
			} else {
				this.rightLinearSlider = handleTime;
				if (value == 10000) this.rightLinearSlider = null;
			}

			this.redrawPlot();
			return true;
		}

		return true;
	}

	if (this.interactive) {
		var replotDataOnEntry = function(e) {
			if (e.keyCode == 13) {
				this.loadAllData();
			}
		};

		var textClickHandler = function(e) {
			resetTextField($(this));
		};

		this.properties.getStartDatePicker().off("click");
		this.properties.getEndDatePicker().off("click");
		this.properties.getStartDatePicker().on("click", textClickHandler);
		this.properties.getEndDatePicker().on("click", textClickHandler);
	}

	this.getLinearSliderValues = function() {
		var startTime = this.properties.getStartTime();
		var endTime = this.properties.getEndTime();

		if (!startTime) startTime = this.minTime;
		if (!endTime) endTime = this.maxTime;

		var start = startTime;
		var end = endTime;

		if (this.leftLinearSlider > endTime) { this.leftLinearSlider = endTime; start = endTime; }
		else if (this.leftLinearSlider != null && this.leftLinearSlider < startTime) this.leftLinearSlider = null;
		else start = this.leftLinearSlider ? this.leftLinearSlider : startTime;

		if (this.rightLinearSlider > endTime) this.rightLinearSlider = null;
		else if (this.rightLinearSlider != null && this.rightLinearSlider < startTime) { this.rightLinearSlider = startTime; end = startTime; }
		else end = this.rightLinearSlider ? this.rightLinearSlider : endTime;

		return [start, end];
	}

	this.handleDropTag = function(event, ui) {
		var $sourceElement = $(ui.draggable[0]);
		console.log($sourceElement);
		var tagListItem = $sourceElement.data(DATA_KEY_FOR_ITEM_VIEW).getData();
		var plot = this;

		if (tagListItem instanceof TagGroup) {
			tagListItem.fetchAll(function() { plot.addLine(tagListItem); });
		} else {
			tagListItem.getTagProperties(function(tagProperties){
				console.log("import tag properties");
				plot.addLine(tagListItem);
			});
		}
	}

}

PlotWeb.prototype = Object.create(Plot);