<html>
<head>
	<meta name="layout" content="social" />
	<c:jsCSRFToken keys="getCommentsCSRF, deleteDiscussionPostDataCSRF, getUserDataCSRF" />
	<script type="text/javascript">
		function doLogout() {
			callLogoutCallbacks();
		}

		<content tag="processUserData">
			processUserData = function(data) {
				if (data == 'login') {
					data = [{id:-1,name:'',username:'(anonymous)',sex:''}];
				}
				if (data != 'login' && (!checkData(data)))
					return;
				return true;
			}
		</content> 

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
<body class="feed">
	<content tag="feedContent">
		<div class="text-center">
			<i class="fa fa-circle-o-notch fa-spin fa-3x"></i>
		</div>
	</content>
</body>
</html>
