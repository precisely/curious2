$(function() {
	// Only execute this code on the /home/signals page.
	if ($('body.signals').length < 1 ) { return undefined; }

	// Register click events for carousel.
	var width = 220;
	var steps_per_click = 4;
	var carouselTimer = undefined;
	$('.saved-carousel-layout .arrow-left').on('click', function() {
		var min_left = -1 * ($('.nav-box').length - 1) * width;
		var left = parseInt($('.secret-container').css('left'));
		var next_pos = left - width*steps_per_click;
		if (min_left < left && next_pos > min_left) {
			$('.secret-container').css("left", "-="+(width*steps_per_click));
			// Correct for over-shooting, cancel previous timeout if the user is clicking rapidly.
			clearTimeout(carouselTimer);
			carouselTimer = setTimeout(function() {
				var left = parseInt($('.secret-container').css('left'));
				if (left < min_left) {
					$('.secret-container').css("left", min_left);
				}
			}, 800);
		}
	});
	$('.saved-carousel-layout .arrow-right').on('click', function() {
		var left = parseInt($('.secret-container').css('left'));
		if (left < 0) {
			var min_left = -1 * ($('.nav-box').length - 1) * width;
			$('.secret-container').css("left", "+="+(width*steps_per_click));
			// Correct for over-shooting, cancel previous timeout if the user is clicking rapidly.
			clearTimeout(carouselTimer);
			carouselTimer = setTimeout(function() {
				var left = parseInt($('.secret-container').css('left'));
				if (left > 0) {
					$('.secret-container').css("left", 0);
				}
			}, 800);
		}
	});

	var fadeToolTip = function() {
		$('.arrow-box').css('opacity', 0.0);
		$('.arrow-box').css('z-index', 1);
	};



	var signalActionsTimer;
	var registerTooltip = function(elt) {
		// Tooltip for signal actions.
		$(elt).on('mouseover', function() {
			clearTimeout(signalActionsTimer);
			var left = $(this).offset().left;
			var top = $(this).offset().top;
			// Type will be "triggered", "positive", or "negative" for "triggered-event", "positively correlated", and
			//	 "negatively correlated" cases respectively.
			var type = $(this).parent().parent().attr('type');
			var action=$(this).attr('data-action');
			var tooltip_title = $('#tooltip-title-' + action).text();
			var tooltip_body =	$('#tooltip-body-' + action).text();
			var tooltip_go =	$('#tooltip-go-' + action).text();
			$('.arrow-box .tooltip-title').text(tooltip_title);
			$('.arrow-box .tooltip-body').text(tooltip_body);
			$('.arrow-box .tooltip-go-button').text(tooltip_go);
			var height = parseInt($('.arrow-box').css('height'));
			$('.arrow-box').css('left', left - 440);
			$('.arrow-box').css('top', top - height/2 - 12);
			$('.arrow-box').css('opacity', 1);
			$('.arrow-box').css('z-index', 10000);
		});
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

	// Toggle saved area.
	$('.signals .toggle-saved').on('click', function() {
		var src = $('.toggle-arrow').attr('src');
		if (src.match(/down/)) {
			$('img.toggle-saved').attr('src', '/images/signals/arrow-right-sm.png');
			$('.garbage-can-icon').css('opacity', 0);
			$('.garbage-can-icon').css('z-index', 1);
		} else {
			$('img.toggle-saved').attr('src', '/images/signals/arrow-down-sm.png');
		}
		$('.saved-carousel-layout').fadeToggle();

	});

	var fadeGarbageBin = function() {
		garbageBinTimer = setTimeout(function() {
			$('.garbage-can-icon').css('opacity', 0);
			$('.garbage-can-icon').css('z-index', 1);
		}, 2000);
	};

	// Hovering over saved elments show the garbage bin above the item.
	var garbageBinTimer, garbageId;
	$('.nav-carousel-container .nav-box').hover(function() {
		$(this).find('.garbage-can-icon').fadeToggle();
		clearTimeout(garbageBinTimer);
		var left = $(this).offset().left;
		var top = $(this).offset().top;
		$('.garbage-can-icon').css('left', left + 30);
		$('.garbage-can-icon').css('top', top - 40);
		$('.garbage-can-icon').css('opacity', 1);
		$('.garbage-can-icon').css('z-index', 10000);
		garbageId = $(this).attr('data-id');
	}, function() {
		fadeGarbageBin();
	});

	$('.garbage-can-icon').on('click', function() {
		// If you delete the last element, the garbageId will be undefined.
		//	 In this case don't do anything.
		if (garbageId == undefined) {
			return false;
		}
		if (confirm("You are about to delete saved item #" + garbageId)) {
			var $nextItem = $('.nav-box[data-id='+garbageId+']').next();
			$('.nav-box[data-id='+garbageId+']').fadeOut();
			if ($nextItem) {
				garbageId = $nextItem.attr('data-id');
			}
		}
	});

	$('.garbage-can-icon').hover(function() {
		clearTimeout(garbageBinTimer);
	}, function() {
		fadeGarbageBin();
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

	var loadData = function(intent) {
		var url = '/correlation/index/' + intent;
		$.ajax({
			url: url,
			dataType: 'text json',
			error: function(e) {
				console.log("There was an error in retrieving JSON from " + url)
			},
			success: function(data) {
				for (var i=0; i < data.length; i++) {
					var id = data[i].id;
					var description1 = data[i].description1;
					var description2 = data[i].description2;
					var score = 0.0;
					if (data[i].value) {
						score = data[i].value.toFixed(2);
					}
					if (intent == 'saved') {
						var type = 'positive';
						if (score < 0) {
							type = 'negative';
						}
						renderSavedItem(id, type, description1, description2, score);
					} else {
						renderCorrelationRow(id, intent, description1, description2, score);
					}
				}
			}
		});
	};

	loadData('positive');
	loadData('negative');
	loadData('saved');
});
