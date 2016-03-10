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
		return checkData(data, status, errorMessage, successMessage);
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
			plot.queueClearStartSlider();
			if (dateText == "") {
				properties.initStartDate();
				plot.loadAllData();
			}
		},
		changeYear: true,
		changeMonth: true
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
			plot.queueClearEndSlider();
			if (dateText == "") {
				plot.properties.initEndDate();
				plot.loadAllData();
			}
		},
		changeYear: true,
		changeMonth: true
	});

	datepicker.change(function () {
		this.loadAllData();
	}.bind(this));

	if (this.interactive) {
		var nameField = this.properties.getNameField();
		var renameFunction = function() {
			var newName = prompt("Rename plot:", plot.getName());

			if (newName) {
				plot.setManualName(true);
				plot.setName(newName);
				plot.store();
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
	
	this.queueClearStartSlider = function() {
		this.resetStartSlider = true;
	}
	
	this.queueClearEndSlider = function() {
		this.resetEndSlider = true;
	}
	
	this.setupSlider = function() {
		if (!this.interactive) return;
		
		var refresh = false;

		var zoomDiv = this.properties.getZoomControl();
		if (!zoomDiv) return;
		
		zoomDiv.addClass("queryplotzoom");

		var leftSlider, rightSlider;

		if (this.cycleTagLine) {
			leftSlider = this.leftCycleSlider == undefined ? -5780 : this.leftCycleSlider;
			rightSlider = this.rightCycleSlider == undefined ? 5780 : this.rightCycleSlider;
		} else {
			var startTime = this.properties.getStartTime();
			var endTime = this.properties.getEndTime();

			if (!startTime) startTime = this.minTime;
			if (!endTime) endTime = this.maxTime;
			
			if (this.resetStartSlider) {
				startTime = this.minTime;
				this.resetStartSlider = false;
				this.leftLinearSlider = startTime;
				refresh = true;
			}
			if (this.resetEndSlider) {
				endTime = this.maxTime;
				this.resetEndSlider = false;
				this.rightLinearSlider = endTime;
				refresh = true;
			}

			var span = endTime - startTime;

			if (!this.leftLinearSlider) leftSlider = -10000;
			else leftSlider = (this.leftLinearSlider - startTime) * 20000 / span - 10000;
			if (!this.rightLinearSlider) rightSlider = 10000;
			else rightSlider = (this.rightLinearSlider - startTime) * 20000 / span - 10000;
		}

		zoomDiv.dragslider({range: true, rangeDrag: true, animate: true, min:-10000, max: 10000, values: [leftSlider,rightSlider]});
		zoomDiv.off("dragsliderslide");
		zoomDiv.on("dragsliderslide", function(event, ui) {
			return plot.slideCallback(ui.values[0], ui.values[1]);
		})

		var plot = this;
		zoomDiv.off("dragsliderslidestop");
		zoomDiv.on("dragsliderslidestop", function(event, ui) {
			return plot.store();
		})

		if (this.cycleTagLine) {
			plot.slideCallback(leftSlider, rightSlider);
		}
		
		if (refresh)
			this.refreshAll();
	}

	this.drawPlot = function() {
		var plotArea = this.plotArea;
		var plot = this;
		this.lastItemClicked = null;
		this.lastItemClickTime = null;
		if (plotArea.is(":hidden")) {
			// Preventing flot.js exception of 0 height or width. (Hiding an element returns width & height as 0)
			console.warn("Plotarea is hidden. Not drawing the graph.");
			return false;
		}
		$.plot(plotArea, this.plotData, this.plotOptions);
		plotArea.off("click");
		plotArea.on("click", function(event) {
			if (plot.ignoreClick) {
				plot.ignoreClick = false;
				return;
			}
			if (typeof plot.activeLineId != 'undefined' && plot.activeLineId != null) {
				var activeLine = plot.getLine(plot.activeLineId);
				if (activeLine) {
					activeLine.deactivate();
					plot.getDialogDiv().dialog().dialog("close");
				}
				plot.activeLineId = undefined;
			}
		});
		plotArea.off("plotclick");
		plotArea.on("plotclick", function(event, pos, item) {
			if (item) {
				var now = new Date().getTime();
				//if (plot.lastItemClicked == null) {
				//	plot.lastItemClicked = item;
				//	plot.lastItemClickTime = now;
				//} else if (plot.lastItemClicked.datapoint[0] == item.datapoint[0] && plot.lastItemClicked.pageY == item.pageY
				//			&& now - plot.lastItemClickTime < 1000 && now - plot.lastItemClickTime > 30) {
				//	plot.lastItemClicked = null;
				//	plot.lastItemClickTime = null;
				//	if (plot.interactive) {
				//		plot.properties.showData(plot.userId, plot.userName, item.datapoint[0]);
				//	}
				//	return;
				//}
				plot.lastItemClicked = item;
				plot.lastItemClickTime = now;
				var dialogDiv = plot.getDialogDiv();
				var plotLine = plot.plotData[item.seriesIndex]['plotLine'];
				plot.ignoreClick = true;
				plot.deactivateActivatedLine(plotLine);
				if (plotLine.hasSmoothLine()) {	//means there is a smooth line of this accordion line
					plot.activeLineId = plotLine.smoothLine.id;
					plotLine.smoothLine.activate();
					console.log('plotclick: activating line id: ' + plotLine.id);
				} else {
					plot.activeLineId = plotLine.id;
					plotLine.activate();
					console.log('plotclick: activating line id: ' + plotLine.id);
				}
				if (!plotLine.isSmoothLine()) {	// If current line clicked is a actual line (parent line)
					console.log('plotclick: parent of a smooth line with line id: ' + plotLine.id);
					dialogDiv.html(item.series.data[item.dataIndex][2].t + ': <a href="' + plot.properties.showDataUrl(plot.userId, plot.userName, item.datapoint[0])
							+ '">' + $.datepicker.formatDate('M d', new Date(item.datapoint[0])) + "</a>"
							+ ' (' + item.datapoint[1] + ')');
					dialogDiv.dialog({ position: { my: "left+3 bottom-5", at: "left+" + pos.pageX + " top+" + pos.pageY, of: ".container", collision: "fit"}, width: 140, height: 62});
				}
			} else {
				console.log('plotclick: Item not found');
			}
		});

		this.store();
	}

	this.slideCallback = function(left, right) {
		var lastRezeroWidth = this.rezeroWidth ? this.rezeroWidth : 0;
		
		if (this.cycleTagLine) {
			this.leftCycleSlider = left;
			this.rightCycleSlider = right;
			if (left > 0) left = 0;
			if (right < 0) right = 0;
			if (left < 0)
				this.minCycleRange = -Math.exp(4.362 * (-left/5000.0))/77.416;
			else
				this.minCycleRange = Math.exp(4.362 * (left/5000.0))/77.416;
			if (right < 0)
				this.maxCycleRange = -Math.exp(4.362 * (-right/5000.0))/77.416;
			else
				this.maxCycleRange = Math.exp(4.362 * (right/5000.0))/77.416;
			this.refreshLinearSliders();
				
			if (this.rezeroWidth != lastRezeroWidth)
				this.refreshPlot();
			else
				this.redrawPlot();
			return true;
		} else {
			var startTime = this.properties.getStartTime();
			var endTime = this.properties.getEndTime();

			if (!startTime) startTime = this.minTime;
			if (!endTime) endTime = this.maxTime;

			var span = endTime - startTime;

			var leftTime = undefined;
			var rightTime = undefined;
			if (left != -10000)
				leftTime = ((left + 10000) / 20000.0) * span + startTime;
			if (right != 10000)
				rightTime = ((right + 10000) / 20000.0) * span + startTime;

			this.leftLinearSlider = leftTime;
			if (left == -10000) this.leftLinearSlider = null;
			this.rightLinearSlider = rightTime;
			if (right == 10000) this.rightLinearSlider = null;

			this.refreshLinearSliders();
			
			if (this.rezeroWidth != lastRezeroWidth)
				this.refreshPlot();
			else
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

	this.save = function() {
		var first = true;
		var plotDataStr = this.store();
		if (plotDataStr == null) {
			this.showAlert("No plotted data to save");
			return;
		}

		this.queuePostJSON("saving graph", this.makePostUrl("savePlotData"), { name: this.getName(), plotData: plotDataStr },
				function(data) {
					this.checkData(data[0], '', "Error while saving live graph", "Graph saved");
				});
	};

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

	this.clearGraphs = function () {
		if (confirm("Are you sure you want to clear the graph and start over?")) {
			this.clear();
		}
	}
}

PlotWeb.prototype = Object.create(Plot);

PlotLine.prototype.handleDropTag = function(event, ui) {
	var plotLine = this;
	var $sourceElement = $(ui.draggable[0]);
	var tagListItem = $sourceElement.data(DATA_KEY_FOR_ITEM_VIEW).getData();
	if (!tagListItem)
		return false;
	plotLine.addTag(tagListItem);
};

PlotLine.prototype.appendHTML = function() {
	if (this.isSmoothLine() || this.isFreqLine()) return; // don't show controls for smooth line
	var makeActive = false;
	var idSuffix = this.getIdSuffix();
	if (!this.isContinuous) this.isContinuous = false;
	if (!this.showPoints) this.showPoints = false;
	var html = '<div id="plotline' + idSuffix + '" class="'+plotColorClass[this.color]+'">\
			<h3><div class="plotGroup"><span id="plotline' + idSuffix + '" class="description">'
			+ escapehtml(this.name) + '</span></div>' +
			' <span class="plotGroup-edit-options">' + (this.snapshot ? '' : '<img class="edit" onclick="renamePlotLine(\'' + this.plot.id
			+ "','" + this.id + '\')" src="/images/edit.gif"/><span class="delete" onclick="removePlotLine(\'' +
			this.plot.id + "','" + this.id + '\')" >x</span>')
			+ '</span></h3><div class="plotlineinfo hide"><div id="editplotline'
			+ idSuffix + '" style="position:absolute;left:15px;top:15px"></div>';
	if (this.isCycle) {
		html += '<div style="display:inline-block;">range <div style="display:inline-block;" id="plotlinerangemin' + idSuffix
				+ '"></div><div style="display:inline-block;margin-left:10px;width:50px;display:relative;top:3px;" id="plotlinecyclerange'
				+ idSuffix + '"></div><div style="display:inline-block;margin-left:15px;" id="plotlinerangemax' + idSuffix
				+ '"></div></div>';
	}
	html += '<h4 style="margin-top:15px">DATA TYPE</h4><div class="form-group"><div class="widget"><input type="radio"'
			+ 'value="continuous" name="plotlinecontinuous' + idSuffix + '" id="plotlinecontinuous' + idSuffix + '"'
			+ (this.isContinuous ? 'checked' : '') + '/> <label>CONTINUOUS</label></div> \
			<img src="/images/gf-continuous.png" class="graph-icon" /> </div> \
			<div class="form-group"><div class="widget"><input type="radio" value="event" name="plotlinecontinuous' + idSuffix
			+ '" id="plotlinecontinuous' + idSuffix + '"' + (this.isContinuous ? '' : 'checked')
			+ '/> <label>EVENT</label></div><img src="/images/gf-event.png" class="graph-icon" /> </div>';
	/*if ((!this.isCycle) && (!this.isSmoothLine()) && (!this.isFreqLine()))
	 html += '<input type="checkbox" name="plotlineshow' + idSuffix + '" id="plotlineshow' + idSuffix + '" '
	 + (this.showYAxis ? 'checked' : '') + '/> yaxis ';*/

	if ((!this.snapshot) && (!this.isSmoothLine()) && (!this.isFreqLine()) && (!this.isCycle))
		html += '<h4 style="margin-top:15px">OPERATIONS</h4> <div class="form-group"><input type="checkbox" name="plotlinesum' + idSuffix + '" id="plotlinesum' + idSuffix + '"'
				+ (this.sumData ? 'checked' : '') + '/> <label>SUM</label></div> ';
	if (!this.isCycle) {
		html += '<label>SMOOTH</label>'
		html += '<div style="display:inline-block;margin-left:10px;width:90%;display:relative;top:3px;" id="plotlinesmoothwidth' + idSuffix + '"></div>';
		//html += '<div style="display:inline-block;">frequency <div style="display:inline-block;margin-left:10px;width:70px;display:relative;top:3px;" id="plotlinefreqwidth' + idSuffix + '"></div></div>';
	}
	html += '<ul class="tags" id="plotline' + idSuffix + 'list"></ul></div></div>';

	if (this.isCycle) {
		var cycleTagDiv = this.plot.properties.getCycleTagDiv();
		cycleTagDiv.html('');
		cycleTagDiv.css('padding-top','0px');
		cycleTagDiv.css('height','30px');
		cycleTagDiv.append(html);
	} else {
		$("#plotLines" + this.plot.id).append(html);
	}
	if (!this.isCycle) {
		var smoothwidth = $("#plotlinesmoothwidth" + idSuffix);
		smoothwidth.slider({min: 0, max: 90, value:this.smoothDataWidth});
		var plotLine = this;
		smoothwidth.off("slide");
		smoothwidth.on("slide", function(event, ui) {
			return plotLine.setSmoothDataWidth(ui.value);
		})
		var freqwidth = $("#plotlinefreqwidth" + idSuffix);
		freqwidth.slider({min: 0, max: 90, value:this.freqDataWidth});
		var plotLine = this;
		freqwidth.off("slide");
		freqwidth.on("slide", function(event, ui) {
			return plotLine.setFreqDataWidth(ui.value);
		})
	}
	if (this.isCycle) {
		var smoothwidth = $("#plotlinecyclerange" + idSuffix);
		smoothwidth.slider({range: true, min:0, max: 10000, values: [this.minRange,this.maxRange]});
		var plotLine = this;
		smoothwidth.off("slide");
		smoothwidth.on("slide", function(event, ui) {
			return plotLine.setRange(ui.value == ui.values[0] ? 0 : 1, ui.value);
		})
	}

	var plotLineId = this.id;
	if (!this.isCycle) {
		$("#plotlineshow" + idSuffix).change(function(e) {
			var plotLine = plot.getLine(plotLineId);
			if (plotLine.showYAxis) plotLine.showYAxis = false;
			else plotLine.showYAxis = true;
			plot.refreshPlot();
		});
		$("#plotlinehide" + idSuffix).change(function(e) {
			var plotLine = plot.getLine(plotLineId);
			plotLine.setHidden(!plotLine.isHidden());
			plot.refreshPlot();
		});
		$("#plotlineint" + idSuffix).change(function(e) {
			var plotLine = plot.getLine(plotLineId);
			if (plotLine.showLines) plotLine.showLines = false;
			else plotLine.showLines = true;
			if (plotLine.plotData != null) {
				plotLine.plotData.lines = { show: plotLine.showLines };
			}
			plot.refreshPlot();
		});
		$("#plotlineflatten" + idSuffix).change(function(e) {
			var plotLine = plot.getLine(plotLineId);
			if (plotLine.flatten) {
				plotLine.flatten = false;
				if (plotLine.hasSmoothLine()) plotLine.smoothLine.flatten = false;
			}
			else {
				plotLine.flatten = true;
				if (plotLine.hasSmoothLine()) plotLine.smoothLine.flatten = true;
			}
			plot.refreshPlot();
		});
	}
	if (!this.snapshot) {
		$("#plotlinesum" + idSuffix).change(function(e) {
			var plotLine = plot.getLine(plotLineId);
			if (plotLine.sumData) plotLine.sumData = false;
			else plotLine.sumData = true;
			plot.loadAllData();
			plot.refreshPlot();
		});
	}
	$("input[name='plotlinecontinuous" + idSuffix + "']").change(function(e) {
		var plotLine = plot.getLine(plotLineId);
		if ($(e.target).val() == 'continuous') {
			plotLine.setIsContinuous(true);
			if (plotLine.parentLine) {
				plotLine.parentLine.setIsContinuous(true);
				plotLine.parentLine.setContinuousCheckbox(true);
			} else if (plotLine.smoothLine && plotLine.smoothLine != 1 && plotLine.smoothDataWidth > 0) {
				plotLine.smoothLine.setIsContinuous(true);
			}
			if (plotLine.smoothDataWidth == 1) {
				plotLine.setSmoothDataWidth(0);
			}
			plot.refreshPlot();
		} else {
			plotLine.setIsContinuous(false);
			if (plotLine.parentLine) {
				plotLine.parentLine.setIsContinuous(false);
				plotLine.parentLine.setContinuousCheckbox(false);
			} else if (plotLine.smoothLine && plotLine.smoothLine != 1 && plotLine.smoothDataWidth > 0) {
				plotLine.smoothLine.setIsContinuous(false);
			}
			plot.refreshPlot();
			if ((!plotLine.parentLine) && (plotLine.smoothDataWidth == 0)) {
				plotLine.setSmoothDataWidth(1, true);
			}
		}
	});
	$("input[name='plotlinepoints" + idSuffix + "']").change(function(e) {
		var plotLine = plot.getLine(plotLineId);
		if ($(e.target).val() == 'points') {
			console.log('setShowPoints - true');
			plotLine.setShowPoints(true);
			plot.refreshPlot();
		}
		else {
			console.log('setShowPoints - false');
			plotLine.setShowPoints(false);
			plot.refreshPlot();
		}
	});
	$("#plotlinefill" + idSuffix).change(function(e) {
		var plotLine = plot.getLine(plotLineId);
		if (plotLine.fill) plotLine.fill = false;
		else plotLine.fill = true;
		plot.refreshPlot();
	});
	var div = $('#plotline' + idSuffix);

	$('.plotlineinfo', div).mouseleave(function(e) {
		$(e.target).closest('.plotlineinfo').toggle();
	});
	$('.plotGroup', div).click(function(e) {
		var $droppableElement = $(e.target).closest('.ui-droppable');
		$('.plotlineinfo', $droppableElement).toggle();
	});

	var plotLine = this;

	div.droppable({
		drop:function(event, ui) { plotLine.handleDropTag(event, ui); }
	});
}

function publishChart() {
	var groupName = $('input[name="group"]:checked').val();
	plot.setName($('#new-chart-name').val());
	plot.saveSnapshot(groupName);
}

function loadGroupsToPublish() {
	queueJSON('Loading group list', '/api/user/action/getGroupsToShare?' +  getCSRFPreventionURI('getGroupsList') + '&callback=?', function(data) {
		if (checkData(data) && data.success) {
				var modalHTML = _.template($('#publish-to-groups').html(), {groups: data.groups});
				$('#publish-to-groups').html(modalHTML);
				$('#publish-to-groups').modal("show");
		}
	});
};
