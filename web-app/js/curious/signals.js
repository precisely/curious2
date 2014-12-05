var correlationIndex = {};

$(function() {
	// Only execute this code on the /home/signals page.
	if ($('body.signals').length < 1 ) { return undefined; }
	LABELS = {positive: "proportional", negative: "inversely proportional", triggered: "triggered"};

	var deleteCorrelationRowUI = function(correlation_id) {
		$('.signal-row[data-id=' + correlation_id + ']').fadeOut(300, function() {
			$(this).remove();
		});
	};

	var moveCorrelationToSavedSection = function(correlation_id) {
		deleteCorrelationRowUI(correlation_id);
	}

	var moveCorrelationToNoiseSection = function(correlation_id) {
		deleteCorrelationRowUI(correlation_id);
	};

	var viewGraph = function(correlation_id) {
		var c = correlationIndex[correlation_id];
		var new_uri = '/home/graph/signals/' + c.description1 + '/' + c.description2;
		window.location = new_uri;
	};

	var execute_correlation_action = function(actionName, action, correlation_id) {
		var action2url = {save: 'markSaved', noise: 'markNoise', graph: 'markViewed'}
		var url = '/correlation/' + correlation_id + '/' + action2url[action];
		queuePostJSON(actionName, url, { _method: 'PATCH' },
				function(data) {
					if (action == 'save') {
						moveCorrelationToSavedSection(correlation_id);
					} else if (action == 'noise') {
						moveCorrelationToNoiseSection(correlation_id);
					}
				});
		if (action == 'graph') {
			viewGraph(correlation_id);
		}
	};

	// Load data.

	var correlation_template = $('#correlation-template').html();
	Mustache.parse(correlation_template);

	var in_english = {
		triggered: 'triggered by',
		positive: 'correlated with',
		negative: 'inversely related to'
	};

	var bubble = function(signalLevel, bubble_position) {
		if (bubble_position == signalLevel) {
			return "filled";
		} else {
			return "empty";
		}
	};

	var renderCorrelationRow = function(id, type, description1, description2, score, signalLevel) {
		var new_row = Mustache.render(correlation_template,
			{
				id: id,
				type: type,
				label: LABELS[type],
				description1: description1,
				description2: description2,
				bubble_0: bubble(signalLevel, 0),
				bubble_1: bubble(signalLevel, 1),
				bubble_2: bubble(signalLevel, 2),
				bubble_3: bubble(signalLevel, 3),
				bubble_4: bubble(signalLevel, 4),
				relation_in_english: in_english[type],
				score: score
			});
		$('#correlation-container').append(new_row);
	};

	$('#correlation-container').on('mouseenter', '.bubble', function() {
		if ($(this).attr('src').match(/empty/)) {
			$(this).attr('src', "/images/signals/hover_circle.png")
		}
	});

	$('#correlation-container').on('mouseleave', '.bubble', function() {
		if ($(this).attr('src').match(/hover/)) {
			$(this).attr('src', "/images/signals/empty_circle.png")
		}
	});

	$('#correlation-container').on('click', '.bubble', function() {
		var signalLevel = $(this).attr('signal-level');
		var correlationId = $(this).parent().parent().attr('data-id');
		var action = 'updateSignalLevel'
		var url = '/correlation/' + correlationId + '/updateSignalLevel'
		queuePostJSON(action, url, { _method: 'PATCH', signalLevel: signalLevel},
										function(data) {
														console.log('success', data);
														//if (action == 'save') {
														//				moveCorrelationToSavedSection(correlation_id);
														//} else if (action == 'noise') {
														//				moveCorrelationToNoiseSection(correlation_id);
														//}
										});
		$(this).siblings('img').attr('src', "/images/signals/empty_circle.png");
		$(this).siblings('img').attr('selected', false);
		$(this).attr('src', "/images/signals/filled_circle.png");
		$(this).attr('selected', true);
	});

	// Click on row to view graph.
	$('#correlation-container').on('click', '.signal-row-top', function() {
		var correlationId = $(this).attr('data-id');
		viewGraph(correlationId);
	});

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
					var signalLevel = data[i].signalLevel;
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
						score: score,
						signalLevel: signalLevel
					};
					correlationIndex[id] = correlation;

					if (saved == null && noise == null) {
						renderCorrelationRow(id, intent, description1, description2, score, signalLevel);
					}
				} // for
				if (afterSuccess) { afterSuccess(); }
			}
		});
	};

	loadData('positive');
	loadData('negative');
});
