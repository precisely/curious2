<html>
<head>
<meta name="layout" content="feedLayout" />
<c:jsCSRFToken keys="getCommentsCSRF, deleteDiscussionPostDataCSRF" />
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
	queuePostJSON('Getting search results', '/dummy/getSearchResults', getCSRFPreventionObject('getSearchResultsCSRF', {
			userId: params.userId,
			searchString: searchString,
			max: 10,
			offset: params.offset ? params.offset : 0}),
			function(data) {
				if (checkData(data)) {
					console.log('data: ', data);
					if (data.success) {
						$('#discussions').html(data.searchResults);
					} else {
						showAlert(data.message);
					}
				}
			}, function(xhr) {
				console.log('error: ', xhr);
			});
}
</script>
</head>
<body>
	<content tag="feedContent">
		<div id="discussions" class="text-center">
			<i class="fa fa-circle-o-notch fa-spin fa-3x"></i>
		</div>
	</content>
</body>
</html>
