
$(function() {
	// Only execute this code on the /analyticsTask/index page.
	if ($('body.analytics-tasks').length < 1 ) { return undefined; }

	var activeParentIds = {};

	var start_analytics_job = function() {
		var url = '/analyticsTask/processUsers';
		// POST to /analyticsTask/processUsers
		$.ajax({
			url: url,
			type: 'POST',
			dataType: 'json',
			data: { },
			error: function(e) {
				console.log("FAILURE");
				console.log("There was an error in retrieving JSON from " + url)
			},
			success: function(data) {
				console.log(data);
			}
		});
	};

	var stop_analytics_job = function(port) {
		return function() {
			var url = 'http://localhost:8090/cluster/stop';
			// POST to /cluster/stop.
			$.ajax({
				url: url,
				type: 'POST',
				dataType: 'json',
				data: { },
				error: function(e) {
					console.log("FAILURE");
					console.log("There was an error in retrieving JSON from " + url)
				},
				success: function(data) {
					_.each(data, function(d) {
					});
				}
			});
		};
	};

	var pingServers = function(cb) {
		var url = '/analyticsTask/pingServers';
		// GET to /cluster/stop.
		$.ajax({
			url: url,
			type: 'GET',
			dataType: 'json',
			error: function(e) {
				$('tr[server="WEB"] .status').html("OFF");
				console.log("FAILURE");
				console.log("There was an error in retrieving JSON from " + url)
			},
			success: function(data) {
				_.forOwn(data, function(item) {
					var url = item.url;
					$('tr[server="'+url+ '"] .status').html(item.status);
				});
			}
		});
	};

	var makeTaskRow = function(task) {
		return Mustache.render(taskTemplate,
			{
				id: task.id,
				parentId: task.parentId,
				userId: task.userId,
				typeEn: task.type.toLowerCase(),
				statusEn: task.status,
				percentSuccessful: task.percentSuccessful,
				serverAddress: task.serverAddress,
				createdAt: task.createdAt,
				updatedAt: task.updatedAt,
				notes: task.notes,
				error: task.error
			});
	}

	var addTaskRow = function(taskId, task) {
		var newRow = makeTaskRow(task);
		$('.task-row[task-id='+taskId+']').after(newRow);
	};

	var updateParentRow = function(task) {
		if (!task || !task.id) { return null; }
		if($('.task-row[task-id='+task.id+']').length == 0) {
			var newRow = makeTaskRow(task);
			$('#task-template').after(newRow);
		}
		if (task.status) {
			$('.task-row[task-id='+task.id+']').attr('class', 'task-row parent ' + task.status.toLowerCase())
		}
		$('.task-row[task-id='+task.id+']').find('.user-id').html(task.userId);
		$('.task-row[task-id='+task.id+']').find('.status').html(task.status);
		$('.task-row[task-id='+task.id+']').find('.percent-successful').html(task.percentSuccessful);
		$('.task-row[task-id='+task.id+']').find('.updated-at').html(task.updatedAt);
		$('.task-row[task-id='+task.id+']').find('.notes').html(task.notes);
	};

	var getChildTasks = function(e) {
		var $el = $(this);
		var taskId = $el.attr('task-id');
		var expanded = $el.attr('expanded');
		if ('yes' == expanded) {
			$(this).attr('expanded', 'no');
			var sel = '.task-row.child[parent-id=' + taskId + ']';
			$(sel).remove();
		} else {
			$el.attr('expanded', 'yes');
			var url = 'http://127.0.0.1:8080/analyticsTask/children/'+taskId;
			var success2 = function(data) {
				_.each(data, function(d) {
					addTaskRow(taskId, d);
				});
			};
			queuePostJSON('task-listing', url, {}, success2);
		}
	};

	var makeParentActive = function(parentId) {
		var latestParentRowStatus = $('.task-row[task-id="'+parentId+'"]').find('.status').html();
		var notThere = $('.task-row[task-id="'+parentId+'"]').length == 0;
		var notFinished = 'string' == typeof(latestParentRowStatus) && !latestParentRowStatus.match(/COMPLETED/)
		if (notThere || notFinished) {
			activeParentIds[parentId] = true;
		} else {
			delete activeParentIds[parentId];
		}
	};

	var updateLatestParent = function() {
		var url = '/analyticsTask/getLatestParent';
		// GET to /cluster/stop.
		$.ajax({
			url: url,
			type: 'GET',
			dataType: 'json',
			error: function(e) {
				console.log("FAILURE");
				console.log("There was an error in retrieving JSON from " + url)
			},
			success: function(data) {
				if (data && data.id) {
					makeParentActive(data.id);
				}
			}
		});
	};

	var updateActiveParents = function() {
		var parentIds = _.keys(activeParentIds);
		console.log('parentIds', parentIds);
		var url = '/analyticsTask/getTasks';
		// GET to /cluster/stop.
		$.ajax({
			url: url,
			type: 'GET',
			dataType: 'json',
			data: {taskIds: JSON.stringify(_.keys(activeParentIds)) },
			error: function(e) {
				console.log("FAILURE updateActiveParents");
				console.log("There was an error in retrieving JSON from " + url)
			},
			success: function(data) {
				_.each(data, function(task) {
					updateParentRow(task);
				});
			}
		});
	};

	var rerunParent = function() {
		var $el = $(this);
		var taskId = $el.closest('[task-id]').attr('task-id');
		var url = '/analyticsTask/rerunParent';

		// GET to /cluster/stop.
		$.ajax({
			url: url,
			type: 'GET',
			dataType: 'json',
			data: {parentId: taskId},
			error: function(e) {
				console.log("FAILURE (signals.js): rerunParent()");
			},
			success: function(data) {
				makeParentActive(taskId);
			}
		});

		// Stop propagating this click event up the DOM tree.
		return false;
	};

	taskTemplate = $('#task-template').html();
	Mustache.parse(taskTemplate);

	$('#start-analytics-job-button').on('click', start_analytics_job);
	$('#stop-analytics-job-button').on('click', stop_analytics_job(8090));
	$('#task-list').on('click', '.task-row', getChildTasks);
	$('#task-list').on('click', '.rerun', rerunParent);
	//setInterval(pingServers, 2000);
	//setInterval(updateLatestParent, 2000);
	//setInterval(updateActiveParents, 2000);

});
