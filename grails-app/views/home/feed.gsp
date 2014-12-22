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
	if (params == null) {
		var params = {};
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
			offset: params.offset?params.offset:0
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

function refreshPage() {
}

$(function() {
	queueJSON("getting login info", "/home/getPeopleData?callback=?",
			getCSRFPreventionObject("getPeopleDataCSRF"),
			function(data) {
				if (!checkData(data))
					return;
			
			var found = false;
			
			jQuery.each(data, function() {
				if (!found) {
					// set first user id as the current
					setUserId(this['id']);
					found = true;
				}
				addPerson(this['first'] + ' ' + this['last'],
						this['username'], this['id'], this['sex']);
				return true;
			});
		});
});
</script>
</head>
<body>
	<content tag="feedContent">
		<div id="discussions">
			<g:render template="/feed/discussions" />
		</div>
	
		<div style="clear: both;"></div>
		<div id="share-dialog" class="hide" title="Share">
			<select name="shareOptions" id="shareOptions" multiple="multiple"
				class="form-control" size="8">
				<option value="isPublic">Visible to the world</option>
				<g:each in="${associatedGroups }" var="userGroup">
					<option value="${userGroup.id }">
						${userGroup.fullName }
					</option>
				</g:each>
			</select>
		</div>
		<div id="comment-dialog" class="hide" title="Comment">
			<input type="text" name="comment" id="userComment" required
				placeholder="Add Comment..."> <input type="hidden"
				name="discussionId" value="${discussionId}">
		</div>
		<g:render template="/sprint/createSprintModal" /> 
	</content>
</body>
</html>
