<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />
<c:jsCSRFToken keys="getPeopleDataCSRF,getInterestTagsDataCSRF, addInterestTagDataCSRF" />
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

$(document).ready(function() {
	var App = window.App || {};
	App.discussion = {};
	App.discussion.lockInfiniteScroll = false;
	App.discussion.offset = 5;

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

	$(window).scroll(function() {
		if ($("#getMoreDiscussions").length == 0)
			return false;

		var $element = $('#getMoreDiscussions');
		var docViewTop = $(window).scrollTop();
		var docViewBottom = docViewTop + $(window).innerHeight();

		var elemTop = $element.offset().top;
		
		if ((elemTop < docViewBottom) && !App.discussion.lockInfiniteScroll) {
			$element.addClass(" waiting-icon");
			setTimeout(function() {
				$.ajax ({
					type: 'POST',
					url: '/home/feed?offset=' + App.discussion.offset,
					success: function(data, textStatus) {
						if (data == "false") {
							$("#getMoreDiscussions").text('No more discussions to show.');
							setTimeout(function() {
								$("#getMoreDiscussions").fadeOut()
							}, 5000);
						} else {
							$('#discussions').append(data);
						}
						App.discussion.offset = App.discussion.offset + 5;
					}
				});
				$element.removeClass("waiting-icon");
			}, 600);
			App.discussion.lockInfiniteScroll = true;
		} else if (elemTop > docViewBottom) {
			App.discussion.lockInfiniteScroll = false;
			return;
		}
	});
});
</script>
</head>
<body class="feed">
<!-- MAIN -->
	<div class="row red-header">
		<div>
			<h1 class="clearfix">
				<span id="queryTitle">${groupFullname}</span>
			</h1>
		</div>
	</div>
	<div class="main container-fluid">
		<div id="graphList">
			<div class="new-post">
				<form action="/discussion/createTopic" method="post">
					<div class="input-affordance left-addon">
						<i class="fa fa-pencil"></i> <input class="full-width"
							type="text" placeholder="New question or discussion topic?"
							name="name" id="discussion-topic" required />
					</div>
					<%--					<textarea class="full-width"  name="discussionPost" style="height:7em" required></textarea>--%>
					<input type="hidden" name="group" value="${groupName}" />
					<%--					<input type="submit" name="POST" value="POST"  />--%>
				</form>
			</div>
			<div id="discussions">
				<g:render template="/feed/discussions" />
			</div>
		</div>
		<div id="getMoreDiscussions"></div>

	</div>
	<!-- /MAIN -->

<div style="clear: both;"></div>
	<div id="share-dialog" class="hide" title="Share">
		<select name="shareOptions" id="shareOptions" multiple="multiple"
			class="form-control" size="8">
			<option value="isPublic">Visible
				to the world</option>
			<g:each in="${associatedGroups }" var="userGroup">
				<option value="${userGroup.id }">
					${userGroup.fullName }
				</option>
			</g:each>
		</select>
	</div>
	<div id="comment-dialog" class="hide" title="Comment">
			<input type="text" name="comment" id="userComment" required placeholder="Add Comment...">
			<input type="hidden" name="discussionId" value="${discussionId}">
		</div>
</body>
</html>
