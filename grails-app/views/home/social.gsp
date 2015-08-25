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
				
				found = false;
				
				jQuery.each(data, function() {
					if (!found) {
						// set first user id as the current
						setUserId(this['id']);
						setUserName(this['username']);
						plot = new Plot(tagList, this['id'], this['username'], "#plotDiscussArea", true, true, new PlotProperties({
							'startDate':'#startdatepicker1',
							'startDateInit':'start date and/or tag',
							'endDate':'#enddatepicker1',
							'endDateInit':'end date and/or tag',
							'cycleTag':'#cycleTag1',
							'zoomControl':'#zoomcontrol1',
							'username':'#queryUsername',
							'name':'',
							'logout':'#logoutLink'
							}));
						found = true;
					}
					addPerson(this['name'],
						this['username'], this['id'], this['sex']);
					
						//plot.loadSnapshotId(179);

	
				return true;
				});
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