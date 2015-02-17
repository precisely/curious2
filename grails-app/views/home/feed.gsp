<html>
<head>
<meta name="layout" content="feedLayout" />
<script type="text/javascript">

function deleteDiscussionId(id) {
	showYesNo("Are you sure you want to delete the saved discussion?", function() {
			backgroundJSON("deleting discussion", "/home/deleteDiscussionId?id=" + escape(id) + "&callback=?",
				function(entries) {
					if (checkData(entries))
						location.reload(true);
			});
	});
}

function doLogout() {
	callLogoutCallbacks();
}

function searchFeeds(params) {
	if (!params) {
		params = {};
		params.userId = $('#userId').val()
	}
	var searchString = $('#searchFeed').val();
	console.log('search feeds:', searchString);
	$.ajax ({
		url: '/dummy/getSearchResults',
		data: {
			userId: params.userId,
			searchString: searchString,
			max: 10,
			offset: params.offset ? params.offset : 0
		},
		success: function(data) {
			if (data) {
				$('#discussions').html(data);
			} else {
				console.log('no data', data);
			}
		},
		error: function(xhr) {
			console.log('error: ', xhr);
		}
	});
}
</script>
</head>
<body>
	<content tag="feedContent">
		<div id="discussions">
			<g:render template="/feed/discussions" />
		</div>
	</content>
</body>
</html>
