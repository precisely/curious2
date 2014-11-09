<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />
<c:jsCSRFToken keys="getPeopleDataCSRF" />
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

function refreshPage() {
}

$(function() {
	queueJSON("getting login info", "/home/getPeopleData?callback=?",
			getCSRFPreventionObject("getPeopleDataCSRF"),
			function(data){
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

$(document).ready(function() {
	$(document).on("click", "a.delete-discussion", function() {
		var $this = $(this);
		showYesNo('Are you sure want to delete this?', function() {
			var discussionId = $this.data('discussionId');
			$.ajax({
				url: '/home/discuss',
				data: {
					discussionId: discussionId,
					deleteDiscussion: true
				},
				success: function(data) {
					showAlert(JSON.parse(data).message, function() {
						$this.parents('.graphItem').fadeOut();
					});
				},
				error: function(xhr) {
					var data = JSON.parse(xhr.responseText);
					showAlert(data.message);
				}
			});
		});
		return false;
	});

	$(document).on("click", "ul#discussion-pagination a", function() {
		var url = $(this).attr('href');
		$.ajax({
			url: url,
			success: function(data) {
				$('div#discussions').html(data);
				wrapPagination();
			}
		});
		return false;
	});
});
</script>
</head>
<body class="feed">
<!-- MAIN -->
<div class="main container-fluid" >
	<div class="row red-header">
		<div class="col-md-3">
			<div id="actions">
				<span class="icon-triangle icon-triangle-right toggle"></span>
				<ul>
					<li><a Ref="/home/feed" >Home Feed</a></li>
				</ul>
			</div>
		</div>
		<div class="col-md-9">
			<h1 class="clearfix">
				<span id="queryTitle">${groupFullname}</span>
			</h1>
		</div>
	</div>
	<div class="row">
		<div class="col-md-3">
			<h2 class="subscription-list"> YOUR SUBSCRIPTIONS </h2>
			<ul class="subscriptions">
				<li>
					<a href = "/home/feed"> Home Feed </a>
				</li>
				<g:each var="membership" in="${groupMemberships}">
					<li>
						<a href = "/home/feed?userGroupNames=${membership[0].name}"> ${membership[0].fullName} </a>
					</li>
				</g:each>
			</ul>
		</div>
		<div id="graphList" class="col-md-9">
			<div class="new-post">
				<form action="/discussion/createTopic" method="post">
					<input class="full-width" type="text" placeholder="New question or discussion title?" name="name" />
					<textarea class="full-width"  name="discussionPost" style="height:7em"></textarea>
					<input type="hidden" name="group" value="${groupName}" />
					<input type="submit" name="POST" value="POST"  />
				</form>
			</div>
			<div id="discussions">
				<g:render template="/feed/discussions" />
			</div>
		</div>

	</div>

</div>
<!-- /MAIN -->

<div style="clear: both;"></div>

</body>
</html>
