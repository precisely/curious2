var correlationIndex = {};
$(function() {
	// Only execute this code on the /home/signals page.
	if ($('body.signals').length < 1 ) { return undefined; }

	// Register click events for carousel.
	var width = 220;
	var steps_per_click = 4;
	var carouselTimer = undefined;
	$('.saved-carousel-layout .arrow-left').on('click', function() {
		var min_left = -1 * ($('.nav-box').length - 1) * width;
		var left = parseInt($('#secret-container').css('left'));
		var next_pos = left - width*steps_per_click;
		if (min_left < left && next_pos > min_left) {
			$('#secret-container').css("left", "-="+(width*steps_per_click));
			// Correct for over-shooting, cancel previous timeout if the user is clicking rapidly.
			clearTimeout(carouselTimer);
			carouselTimer = setTimeout(function() {
				var left = parseInt($('#secret-container').css('left'));
				if (left < min_left) {
					$('#secret-container').css("left", min_left);
				}
			}, 800);
		}
	});

	$('.saved-carousel-layout .arrow-right').on('click', function() {
		var left = parseInt($('#secret-container').css('left'));
		if (left < 0) {
			var min_left = -1 * ($('.nav-box').length - 1) * width;
			$('#secret-container').css("left", "+="+(width*steps_per_click));
			// Correct for over-shooting, cancel previous timeout if the user is clicking rapidly.
			clearTimeout(carouselTimer);
			carouselTimer = setTimeout(function() {
				var left = parseInt($('#secret-container').css('left'));
				if (left > 0) {
					$('#secret-container').css("left", 0);
				}
			}, 800);
		}
	});

	var fadeToolTip = function() {
		$('.arrow-box').css('opacity', 0.0);
		$('.arrow-box').css('z-index', 1);
	};

	var appendSavedItemUI = function(correlation_id) {
		var c = correlationIndex[correlation_id];
		if ($('.nav-box[data-id=' + correlation_id + ']').length == 0) {
			renderSavedItem(c.id, c.type, c.description1, c.description2, c.score);
		}
	};

	var deleteCorrelationRowUI = function(correlation_id) {
		$('.signal-row[data-id=' + correlation_id + ']').fadeOut(300, function() {
			$(this).remove();
			updateSignalCount();
		});
	};

	var moveCorrelationToSavedSection = function(correlation_id) {
		deleteCorrelationRowUI(correlation_id);
		appendSavedItemUI(correlation_id);
		fadeToolTip();
	}

	var moveCorrelationToNoiseSection = function(correlation_id) {
		deleteCorrelationRowUI(correlation_id);
		fadeToolTip();
	};

	var execute_correlation_action = function(actionName, action, correlation_id) {
		console.log(action, correlation_id);
		var action2url = {save: 'markSaved', noise: 'markNoise', graph: 'markViewed'}
		var url = '/correlation/' + correlation_id + '/' + action2url[action];
		WORK STOPPED HERE: add CSRF protection
		queuePostJSON(actionName, url, { _method: 'PATCH' },
				function(data) {
					console.log(data);
					if (action == 'save') {
						moveCorrelationToSavedSection(correlation_id);
					} else if (action == 'noise') {
						moveCorrelationToNoiseSection(correlation_id);
					}
				});
		/*$.ajax({
			url: url,
			type: 'POST',
			dataType: 'json',
			data: { _method: 'PATCH' },
			error: function(e) {
				console.log("There was an error in retrieving JSON from " + url)
			},
			success: function(data) {
				console.log(data);
				if (action == 'save') {
					moveCorrelationToSavedSection(correlation_id);
				} else if (action == 'noise') {
					moveCorrelationToNoiseSection(correlation_id);
				}
			}
		});*/

		if (action == 'graph') {
			var c = correlationIndex[correlation_id];
			var new_uri = '/home/graph/signals/' + c.description1 + '/' + c.description2;
			window.location = new_uri;
		}
	};

	var action_handler = function() {
		var actionName = $(this).attr('action-name');
		var action = $(this).attr('data-action');
		var correlation_id = $(this).parent().parent().attr('data-id');
		execute_correlation_action(actionName, action, correlation_id);
	};

	$('.tooltip-action-button').click(action_handler);

	var signalActionsTimer;
	var registerTooltip = function(elt) {
		// Tooltip for signal actions.
		$(elt).on('mouseover', function() {
			clearTimeout(signalActionsTimer);
			var left = $(this).offset().left;
			var top = $(this).offset().top;
			var correlation_id = $(this).parent().parent().attr('data-id');
			$('.arrow-box').attr('data-id', correlation_id);
			// Type will be "triggered", "positive", or "negative" for "triggered-event", "positively correlated", and
			//	 "negatively correlated" cases respectively.
			var type = $(this).parent().parent().attr('type');
			var action=$(this).attr('data-action');
			var tooltip_title = $('#tooltip-title-' + action).text();
			var tooltip_body =	$('#tooltip-body-' + action).text();
			var tooltip_go =	$('#tooltip-go-' + action).text();
			$('.arrow-box .tooltip-title').text(tooltip_title);
			$('.arrow-box .tooltip-body').text(tooltip_body);
			$('.arrow-box .tooltip-action-button').text(tooltip_go);
			$('.arrow-box .tooltip-action-button').attr('data-action', action);
			var height = parseInt($('.arrow-box').css('height'));
			$('.arrow-box').css('left', left - 440);
			$('.arrow-box').css('top', top - height/2 - 12);
			$('.arrow-box').css('opacity', 1);
			$('.arrow-box').css('z-index', 10000);
		});

		$(elt).on('click', action_handler );

		$(elt).on('mouseleave', function() {
			signalActionsTimer = setTimeout(function() {
				fadeToolTip();
			}, 2000);
		});
	};

	$('.signal-action-button').each(function(i, elt) {
		registerTooltip(elt);
	});

	// Don't fade out if the user hovers over the tooltip.
	$('.arrow-box').hover(function() {
		clearTimeout(signalActionsTimer);
	}, function() {
		fadeToolTip();
	});

	$('.maybe-later-button').on('click', function() {
		fadeToolTip();
	});


	var close_saved_area = function() {
			$('img.toggle-saved').attr('src', '/images/signals/arrow-right-sm.png');
			$('#garbage-can-icon').css('opacity', 0);
			$('#garbage-can-icon').css('z-index', 1);
			$('.saved-carousel-layout').fadeOut();
	};

	var open_saved_area = function() {
			$('img.toggle-saved').attr('src', '/images/signals/arrow-down-sm.png');
			$('#garbage-can-icon').css('opacity', 1);
			$('#garbage-can-icon').css('z-index', 1000);
			$('.saved-carousel-layout').fadeIn();
	};

	// Toggle saved area.
	var toggle_saved_area = function() {
		var src = $('.toggle-arrow').attr('src');
		if (src.match(/down/)) {
			close_saved_area();
		} else {
			open_saved_area();
		}
	};
	$('.signals .toggle-saved').on('click', toggle_saved_area );
	$('.signals .saved-action').on('click', toggle_saved_area );


	var fadeGarbageBin = function() {
		$('#garbage-can-icon').css('opacity', 0);
		$('#garbage-can-icon').css('z-index', 1);
	};

	var delayedFadeGarbageBin = function() {
		garbageBinTimer = setTimeout(function() {
			fadeGarbageBin();
		}, 2000);
	};

	// Hovering over saved elments show the garbage bin above the item.
	var garbageBinTimer, garbageId;
	$('#secret-container').on('mouseenter', '.nav-box', function() {
			$(this).find('#garbage-can-icon').fadeIn();
			clearTimeout(garbageBinTimer);
			var left = $(this).offset().left;
			var top = $(this).offset().top;
			$('#garbage-can-icon').css('left', left + 30);
			$('#garbage-can-icon').css('top', top - 40);
			$('#garbage-can-icon').css('opacity', 1);
			$('#garbage-can-icon').css('z-index', 10000);
			garbageId = $(this).attr('data-id');
		});
	$('.nav-carousel-container').on('mouseleave', '.nav-box', function() {
		delayedFadeGarbageBin();
	});

	$('#garbage-can-icon').on('click', function() {
		execute_correlation_action('Mark as noise', 'noise', garbageId);
		fadeGarbageBin();
		$('.nav-box[data-id='+garbageId+']').fadeOut(300, function() { $(this).remove(); });

		// Set next item to be deleted.
		var $nextItem = $('.nav-box[data-id='+garbageId+']').next();
		if ($nextItem) {
			garbageId = $nextItem.attr('data-id');
		}

	});

	$('#garbage-can-icon').hover(function() {
		clearTimeout(garbageBinTimer);
	}, function() {
		delayedFadeGarbageBin();
	});


	// Load data.

	var correlation_template = $('#correlation-template').html();
	var saved_item_template = $('#saved-item-template').html();
	Mustache.parse(correlation_template);
	Mustache.parse(saved_item_template);

	var in_english = {
		triggered: 'triggered by',
		positive: 'correlated with',
		negative: 'inversely related to'
	};

	var updateSignalCount = function() {
		var num_signals = $('.signal-row').length;
		if (num_signals == 0) {
			$('.new-signals').fadeOut();
		} else if (num_signals == 1) {
			$('.new-signals').fadeIn();
		}
		$('.new-signal-count').html(num_signals);
	};

	var renderCorrelationRow = function(id, type, description1, description2, score) {
		var new_row = Mustache.render(correlation_template,
			{
				id: id,
				type: type,
				description1: description1,
				description2: description2,
				relation_in_english: in_english[type],
				score: score
			});
		$('#correlation-container').append(new_row);
		var new_buttons = $('#correlation-container .signal-row')
			.last()
			.find('.signal-action-button');
		registerTooltip(new_buttons);
		updateSignalCount();
	};

	var renderSavedItem = function(id, type, description1, description2, score) {
		var new_row = Mustache.render(saved_item_template,
			{
				id: id,
				type: type,
				description1: description1,
				description2: description2,
				score: score
			});
		$('#secret-container').append(new_row);
		//var new_buttons = $('#correlation-container .signal-row')
		//	.last()
		//	.find('.signal-action-button');
		//registerGarbageBin(new_buttons);
	};

	//renderSavedItem(42, 'positive', 'proportional', 'the body');

	var loadData = function(intent, afterSuccess) {
		var url = '/correlation/index/' + intent;
		$.ajax({
			url: url,
			dataType: 'text json',
			error: function(e) {
				console.log("There was an error in retrieving JSON from " + url)
			},
			success: function(data) {
				for (var i=0; i < data.length; i++) {

					// Aliases for readability.
					var id = data[i].id;
					var description1 = data[i].description1;
					var description2 = data[i].description2;
					var saved = data[i].saved;
					var noise = data[i].noise;
					var score = 0.0;
					if (data[i].value) {
						score = data[i].value.toFixed(2);
					}

					// Save in the master list in browser memory.
					var correlation = {
						id: id,
						type: intent,
						description1: description1,
						description2: description2,
						score: score
					};
					correlationIndex[id] = correlation;

					// Render either downstairs (in the table or correlation rows)	or upstairs (in saved items).
					if (intent == 'saved' && saved && noise == null) {
						var type = 'positive';
						if (score < 0) {
							type = 'negative';
						}
						renderSavedItem(id, type, description1, description2, score);
					} else {
						if (saved == null && noise == null) {
							renderCorrelationRow(id, intent, description1, description2, score);
						}
					}
				} // for
				if (afterSuccess) { afterSuccess(); }
			}
		});
	};

	loadData('positive');
	loadData('negative');
	loadData('saved', function() {
		var num_saved = $('.nav-box').length;
		if (num_saved == 0) {
			close_saved_area();
		}
	});
});
