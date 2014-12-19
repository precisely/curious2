// Global variable for "Curious."
C = {};

// correlationIndex is a master list of all correlations seens so far.
C.correlationIndex = {};

// Collect correlation ids for the page into an array.
C.pageIds = [];

// Each combination of search terms has a different page number.
C.signalPageNumber = {};

// Collect numbr of search results for a given set of search parameters / filter / sortBy combinations
//	 so that we'll know ahead of time whether or noth there will be any results.	If the last search had 0
//	 results then there will always be no more results since we're storing past results in C.correlationIndex.
C.signalNumSearchResults = {};

$(function() {

	var loadedPositive = false;
	var loadedNegative = false;
	var loadedTrigger  = false;
	C.signalScrollReady = true;

	// Possible sort orders include:
	// 'natural', 'alpha asc', 'alpha desc', 'marked asc', 'marked desc',
	// 'score asc', score desc', 'type positive', 'type negative', 'type triggered'
	// We will store the last 5 sort orders.	If no orders are found in in
	// localStorage['signalSortOrder'] or if localStorage is not available, then it
	// will be initialized to 'natural'.
	//
	var possibleSortOrders = ['natural', 'alpha asc', 'alpha desc', 'marked asc', 'marked desc', 'score asc', 'score desc', 'type positive', 'type negative', 'type triggered'];
	// A transition "matrix" for toggling sort orders.
	var sortOrderTransitions = {'natural': 'natural', 'alpha asc': 'alpha desc', 'alpha desc': 'alpha asc', 'marked asc': 'marked desc', 'marked desc': 'marked asc', 'score asc': 'score desc', 'score desc': 'score asc', 'type positive': 'type negative', 'type negative': 'type positive'};
	var LABELS = {positive: "proportional", negative: "inversely proportional", triggered: "triggered"};

	var log = function() {
		if (console) {
			return console.log.apply(console, arguments);
		}
	}

	// Save the last clicked signal filter in localStorage['signalFilter'].
	// Possible signal filters are: 'all', 'signal', 'noise'.
	var possibleFilters = ['all', 'signal', 'noise'];

	var getDomIdFromOrder = function(order) {
		return _.first(order.split(" "));
	};

	var validateFilterValue = function(filter) {
		var success = _.contains(possibleFilters, filter);
		if (!success) {
			log('invalid filter value:', filter);
		}
		return success;
	};

	var initSignalFilter = function() {
		try {
			C.signalFilter = JSON.parse(localStorage['signalFilter']);
			if (!validateFilterValue(C.signalFilter)) {
				C.signalFilter = 'all';
			}
		} catch(e) {
			C.signalFilter = 'all';
			log('Could not load signal filter from localStorage.');
		}
		return C.signalFilter;
	}

	var validateSortOrderValue = function(order) {
		var success = _.contains(possibleSortOrders, order);
		if (!success) {
			log('invalid sort order value:', order);
		}
		return success;
	};

	var initSortOrder = function() {
		try {
			C.signalSortOrder = JSON.parse(localStorage['signalSortOrder']);
			// Make sure every element is one of the possible values.
			if (! _.isArray(C.signalSortOrder)) {
				log('C.signalSortOrder is not an array.', C.signalSortOrder);
			} else if ( ! _.every(C.signalSortOrder, function(x) { return _.contains(possibleSortOrders, x)})) {
				log('C.signalSortOrder contains an invalid value.');
				C.signalSortOrder = ['natural'];
				saveSortOrder();
			}
		} catch(e) {
			C.signalSortOrder = ['natural'];
			log('Could not load sort order from localStorage.', e.message);
		}
		updateUISortOrder();
		updateUIFilter();
		return C.signalSortOrder;
	}

	var saveSignalFilter = function() {
		var success = false;
		try {
			localStorage['signalFilter'] = JSON.stringify(C.signalFilter);
			success = true;
		} catch(e) {
			log('Could not save signal filter.');
		}
		return success;
	};

	var saveSortOrder = function() {
		var success = false;
		try {
			localStorage['signalSortOrder'] = JSON.stringify(C.signalSortOrder);
			success = true;
		} catch(e) {
			log('Could not save sort order.');
		}
		return success;
	};

	var appendSortOrder = function(order) {
		if (! validateSortOrderValue(order)) {
			return false;
		}
		C.signalSortOrder.unshift(order);
		if (C.signalSortOrder.length > 5) {
			C.signalSortOrder = _.first(C.signalSortOrder,	5);
		}
		saveSortOrder();
		return C.signalSortOrder;
	};

	var replaceFirstSortOrder = function(order) {
		C.signalSortOrder.shift();
		return appendSortOrder(order);
	};

	var setSignalFilter = function(filter) {
		if (! validateFilterValue(filter)) {
			return false;
		}
		C.signalFilter = filter;
		saveSignalFilter();
		return C.signalFilter;
	};

	// Only execute this code on the /home/signals page.
	if ($('body.signals').length < 1 ) { return undefined; }

	var reRenderCorrelations = function(ids) {
		$('.signal-row-container').remove();
		_.forEach(ids, function(id, i) {
			// e stands for element, which is the raw data for a correlation row, stored in C.correlationIndex.
			var e = C.correlationIndex[id];
			setTimeout(function() {
				renderCorrelationRow(e.id, e.type, e.description1, e.description2, e.score, e.signalLevel, e.marked);
			}, 2*i);
		});
	};

	var getCorrelations = function(ids) {
		return _.map(ids, function(id) { return C.correlationIndex[id]; });
	};

	var mapToIds = function(objArr) {
		return _.map(objArr, function(o) { return o.id; });
	};

	// Use Lodash's stable sort.
	var naturalSort = function(ids) {
		var sortedItems = _.chain(getCorrelations(ids)).sortBy(function(e) {
			var number_score = e.score;
			var abs_score = Math.abs(number_score);
			return -abs_score;
		}).sortBy(function(e) {
			return e.signalLevel;
		}).value();
		return mapToIds(sortedItems);
	};

	// A function that generates all the required sorting functions.
	var sortByAttr = function(attr, desc, firstValue) {
		return function(ids) {
			var sorted = _.sortBy(_.values(C.correlationIndex), function(e) {
				if (firstValue && e[attr] == firstValue) {
					if (typeof firstValue == 'string') {
						return 'aaa';
					} else {
						return -Infinity;
					}
				} else {
					return e[attr];
				}
			});
			if (desc) { sorted = sorted.reverse(); }
			return mapToIds(sorted);
		};
	};

	// Note: all these functions (and naturalSort) require 1 argument: a list of correlation ids.
	var sortFunction = {
		'natural': naturalSort,
		'alpha asc': sortByAttr('description1'),
		'alpha desc': sortByAttr('description1', 'desc'),
		'marked asc': sortByAttr('marked'),
		'marked desc': sortByAttr('marked', 'desc'),
		'score asc': sortByAttr('score'),
		'score desc': sortByAttr('score', 'desc'),
		'type positive': sortByAttr('type', false, 'positive'),
		'type negative': sortByAttr('type', false, 'negative'),
		'type triggered': sortByAttr('type', false, 'triggered')};

	var resort = function(ids, order) {
		if (undefined == sortFunction[order]) {
			log("ERROR: unhandled order", order, sortFunction);
		}
		return sortFunction[order].call(null, ids);
	};

	var resortAll = function(ids) {
		var sortedIds = ids;
		var reverseOrder = C.signalSortOrder.slice();
		reverseOrder.reverse(); // reverse() mutates order of array.
		_.forEach(reverseOrder, function(order) {
			sortedIds = resort(sortedIds, order);
		});
		return sortedIds;
	}

	var viewGraph = function(correlation_id) {
		var c = C.correlationIndex[correlation_id];
		var new_uri = '/home/graph/signals/' + c.description1 + '/' + c.description2;
		window.location = new_uri;
	};

/* // Marked for deletion...
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
	*/


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
			return "marked";
		} else {
			return "empty";
		}
	};

	var renderCorrelationRow = function(id, type, description1, description2, score, signalLevel, marked) {
		var display = 'block';
		if (C.signalFilter == "noise") {
			if (signalLevel != 0) {
				display = "none";
			}
		} else if (C.signalFilter == "signal") {
			if (signalLevel != 4) {
				display = "none";
			}
		}

		var new_row = Mustache.render(correlation_template,
			{
				id: id,
				type: type,
				marked: marked,
				label: LABELS[type],
				description1: description1,
				description2: description2,
				bubble_0: bubble(signalLevel, 0),
				bubble_1: bubble(signalLevel, 1),
				bubble_2: bubble(signalLevel, 2),
				bubble_3: bubble(signalLevel, 3),
				bubble_4: bubble(signalLevel, 4),
				relation_in_english: in_english[type],
				score: score,
				display: display
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

	var makeActiveById = function(id) {
		var v  = $('#' + id).parents('ul').find('.filter').removeClass('active');
		$('#' + id).addClass('active');
	};

	$('.filter-group').on('click', '.filter', function() {
		var id = $(this).attr('id');
		makeActiveById(id);
		if (shouldLoad()) {
			performSearch();
		}
	});

	$('#correlation-container').on('click', '.bubble', function() {
		var signalLevel = $(this).attr('signal-level');
		var correlationId = parseInt($(this).parent().parent().parent().attr('data-id'));
		var action = 'updateSignalLevel'
		var url = '/correlation/' + correlationId + '/updateSignalLevel'
		queuePostJSON(action, url, { _method: 'PATCH', signalLevel: signalLevel},
										function(data) {
														log('success', data);
										});
		$(this).siblings('img').attr('src', "/images/signals/empty_circle.png");
		$(this).siblings('img').attr('marked', 'empty');
		$(this).attr('src', "/images/signals/marked_circle.png");
		$(this).attr('marked', 'marked');
		C.correlationIndex[correlationId].marked = 'marked';
	});

	// Click on signal filter.
	$('#signal').on('click', function() {
		setSignalFilter('signal');
		$('.signal-row-container:not(:has(img[signal-level=4][marked=marked]))').hide();
		$('.signal-row-container:has(img[signal-level=4][marked=marked])').show();
	});

	// Click on noise filter.
	$('#noise').on('click', function() {
		setSignalFilter('noise');
		$('.signal-row-container:not(:has(img[signal-level=0][marked=marked]))').hide();
		$('.signal-row-container:has(img[signal-level=0][marked=marked])').show();
	});

	// Click on 'all' filter.
	$('#all').on('click', function() {
		setSignalFilter('all');
		$('.signal-row-container').show();
	});

	var incPageNumber = function(searchId) {
		if (undefined == C.signalPageNumber[searchId]) {
			C.signalPageNumber[searchId] = 0;
		}
		C.signalPageNumber[searchId] += 1;
	};

	performSearch = function(q) {
		if (undefined == q) {
			var q = $('#search-input').val();
		}
		var searchId = getSearchId(q);
		C.signalLastSearch = q;
		searchWithDefaults(afterSearch(q), q, C.signalPageNumber[searchId]);
	};

	// search
	$('#search-input').keyup(function(e) {
		if (13 == e.which) {
			performSearch();
		}
	});

	$('#search-image').click(function() {
		var q = $('#search-input').val();
		performSearch();
	});

	var toggleSortOrder = function(order) {
		if (!_.contains(_.keys(sortOrderTransitions), order)) {
			log('unhandled sort order value in sortOrderTransitions.', order)
		}
		// Default is to do nothing to the input order.
		return sortOrderTransitions[order];
	};

	// Record the last 5 sort orders and store it in localStorage if it is available.
	$('#sort-by-row').on('click', '.filter', function(e) {
		var id = $(this).attr('id');
		var idReg = new RegExp(id);
		var order = $(this).attr('data-order');
		var newOrder = order;
		if (_.first(C.signalSortOrder).match(idReg)) {
			newOrder = toggleSortOrder(order);
		}
		$(this).attr('data-order', newOrder);
		C.signalSortOrder = _.filter(C.signalSortOrder, function(sortOrder) { return !sortOrder.match(idReg); });
		appendSortOrder(newOrder);

		log("C.signalSortOrder", C.signalSortOrder);

		C.pageIds = resort(C.pageIds, newOrder);
		reRenderCorrelations(C.pageIds);
	});

	// Click on row to view graph.
	$('#correlation-container').on('click', '.signal-row-top', function() {
		var correlationId = $(this).parent().attr('data-id');
		viewGraph(correlationId);
	});

	var getSearchId = function(q) {
		return [q.replace(/,/g, ' '), C.signalFilter, C.order1, C.order2].join(",");
	};

	descriptionFilter = function(q) {
		var reg = new RegExp(q);
		_.forEach(C.pageIds, function(id) {
			var obj = C.correlationIndex[id];
			var $row = $('.signal-row-container[data-id=' + id + ']');
			if (obj.description1.match(reg) || obj.description2.match(reg)) {
				$row.show();
			} else {
				$row.hide();
			}
		});
	};

	// Separate the search function from the application-specific prep work for calling the search function
	//	 in order to make testing of the search function easier.
	var searchWithDefaults = function(afterSuccess, q) {
		var searchId = getSearchId(q);
		incPageNumber(searchId);
		var pageNumber = C.signalPageNumber[searchId];
		var filter = C.signalFilter;
		var order1 = C.signalSortOrder[0];
		var order2 = undefined;
		if (C.signalSortOrder.length > 1) {
			order2 = C.signalSortOrder[1];
		}

		// There are no more search results for the current search parameters/filter so no need to do the
		//	 search on the server.
		if (noMoreSearchResults(searchId)) {
			log("no more search results for the filter:", "q:", q, "filter:", C.signalFilter, "order1:", C.order1, "order2", C.order2);
			return 0;
		}
		search(afterSuccess, q, pageNumber, filter, order1, order2);
	};

	var processSearchResults = function(searchId, afterSuccess) {
		return function(data) {
			log( "search results", data.length);
			C.signalNumSearchResults[searchId] = data.length;
			for (var i=0; i < data.length; i++) {
				// Aliases for readability.
				var id = data[i].id;
				var description1 = data[i].description1;
				var description2 = data[i].description2;
				var saved = data[i].saved;
				var noise = data[i].noise;
				var signalLevel = data[i].signalLevel;
				var score = 0.0;
				var marked = (-1 == signalLevel) ? 'empty' : 'marked';
				if (data[i].value) {
					score = data[i].value.toFixed(2);
					if (typeof score == 'string') {
						score = parseFloat(score);
					}
				}

				// TODO: update this when "triggered" case is handled.
				if (score >= 0) {
					intent = 'positive';
				} else {
					intent = 'negative';
				}

				if (C.pageIds == undefined) {
					C.pageIds = [];
				}
				if (undefined == C.correlationIndex[id]) {
					C.pageIds.push(id);
				}

				// Save in the master list in browser memory.
				var correlation = {
					id: id,
					type: intent,
					marked: marked,
					description1: description1,
					description2: description2,
					score: score,
					signalLevel: signalLevel
				};
				C.correlationIndex[id] = correlation;
				renderCorrelationRow(id, intent, description1, description2, score, signalLevel, marked);

			} // for
			if (afterSuccess) { afterSuccess(); }
		};
	};	 // processSearchResults

	search = function(afterSuccess, q, pageNumber, filter, order1, order2) {
		var url = '/correlation/search';
		log('search more data via AJAX', url);
		var searchId = getSearchId(q)
		queuePostJSON('search', url, {q: q, page: pageNumber, filter: filter, order1: order1, order2: order2}, processSearchResults(searchId, afterSuccess));
	};

	var updateUISortOrder = function() {
		var domId = getDomIdFromOrder(_.first(C.signalSortOrder));
		makeActiveById(domId);
	};

	var updateUIFilter = function() {
		var domId = C.signalFilter;
		makeActiveById(domId);
	};

	var resetScrollReady = function() {
		C.signalScrollReady = true;
	};

	var afterSearch  =	function(q) {
		return function() {
			resetScrollReady();
			descriptionFilter(q);
		}
	};

	var noMoreSearchResults = function(searchId) {
		return (undefined != C.signalNumSearchResults[searchId] && C.signalNumSearchResults[searchId] == 0);
	};

	var shouldLoad = function () {
		var scrollPos = $(window).scrollTop();
		var pageHeight = $(window).height();
		var docHeight = $(document).height();
		return (C.signalScrollReady && (docHeight - (scrollPos + pageHeight) < 800) && C.signalScrollReady);
	};

	var handleScroll = function() {
		// Infinite scroll.
		if (shouldLoad()) {
			var q = $('#search-input').val();
			var searchId = getSearchId(q);
			if ( noMoreSearchResults(searchId) ) {
				log("no more search results:", searchId);
				return 0;
			}
			C.signalScrollReady = false;
			searchWithDefaults(afterSearch(q), q, C.signalPageNumber);
			log('infinite scroll!', C.signalPageNumber);
		}
	};

	$(window).on('scroll', handleScroll);

	initSignalFilter();
	initSortOrder();
	performSearch();
});
