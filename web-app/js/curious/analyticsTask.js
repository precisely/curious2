var to_dom_id = function(server_name) {
	return "server-" + server_name.replace(/:/, "-");
};

var get_server_list = function(cb) {
	var url = '/analyticsTask/servers';
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
			console.log(data.servers)
			for(var i=0; i < data.servers.length; i++) {
				var server_name = cb.call(i, data.servers[i])
				console.log(server_name);
			}
		}
	});
};

var set_server_status = function(server_name, value) {
	$('#available-server-table tr[server="' + server_name + '"] .server-status').html(value);
}

$(function() {
	// Only execute this code on the /analyticsTask/index page.
	if ($('body.admin-analytics-job').length < 1 ) { return undefined; }

	var start_analytics_job = function() {
		console.log("start analytics job");
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
				console.log("SUCCESS");
				console.log(data);
			}
		});
	};

	var stop_analytics_job = function(port) {
		return function() {
			console.log("stop analytics job");
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
					console.log("SUCCESS");
					console.log(data);
				}
			});
		};
	};


	$('#start-analytics-job-button').on('click', start_analytics_job);
	$('#stop-analytics-job-button').on('click', stop_analytics_job(8090));

});
