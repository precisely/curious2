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

function getRecentSearches(params) {
	if ($('#recentSearches ul').length == 0) {
		$.ajax ({
			url: '/feed/getRecentSearches',
			data: {
				userId: params.userId,
				max: 5,
				offset: params.offset?params.offset:0
			},
			success: function(data) {
				if (data) {
					console.log('data: ', JSON.parse(data));
					$('#recentSearches').slideDown( 1000, function() {
						$(this).append('<ul>');
						jQuery.each(JSON.parse(data).terms, function() {
							$('#recentSearches ul').append('<li><a herf="#">' + this + '</a></li>');
						});
						$(this).append('</ul>');
					});
				} else {
					console.log('no data', data);
				}
			},
			error: function(xhr) {
				console.log('error: ', xhr);
			}
		});
	} else {
		$('#recentSearches ul').remove();
	}
}

function getMyThreads(params) {
	$.ajax ({
		type: 'POST',
		url: '/feed/getMyThreads',
		success: function(data) {
			if (data) {
				console.log('data: ', data);
				$('.new-post').remove();
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

function searchFeeds(params) {
	if (params == null) {
		var params = {};
		params.userId = $('#userId').val()
	}
	var searchString = $('#searchFeed').val();
	console.log('search feeds:', searchString);
	$.ajax ({
		url: '/feed/getSearchResults',
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

$(document).ready(function() {
	var App = window.App || {};
	App.discussion = {};
	App.discussion.lockInfiniteScroll = false;
	App.discussion.offset = 5;

	$(document).on("click", "a.delete-discussion", function() {
		var $this = $(this);
		showYesNo('Are you sure want to delete this?', function() {
			var discussionId = $this.data('discussionId');
			console.log('discussion ID: ',discussionId);
			$.ajax({
				url: '/home/discuss',
				data: {
					discussionId: discussionId,
					deleteDiscussion: true
				},
				success: function(data) {
					showAlert(JSON.parse(data).message, function() {
						$this.parents('.feed-item').fadeOut();
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

	$(document).on("click", ".left-menu ul li a", function() {
		$('.left-menu ul li .active').removeClass('active');
		$(this).addClass('active');
		
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

	$('.main').scroll(function() {
		if ($("#getMoreDiscussions").length == 0)
			return false;

		var $element = $('#getMoreDiscussions');
		var docViewTop = $(window).scrollTop();
		var docViewBottom = docViewTop + $(window).innerHeight();

		var elemTop = $element.offset().top;

		if ((elemTop < docViewBottom) && !App.discussion.lockInfiniteScroll) {
			$element.addClass(" waiting-icon");
			setTimeout(function() {
				var requestUrl = '/home/feed';
				if ($('#myThreads a').hasClass('active')) {
					requestUrl = '/feed/getMyThreads';
				}
				$.ajax ({
					type: 'POST',
					url: requestUrl+'?offset=' + App.discussion.offset,
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
		<div class="pull-right search-bar left-addon">
			<form onsubmit="searchFeeds()">
				<i class="fa fa-search"></i>
				<input type="text" id="searchFeed" placeholder="Search Feed" required >
				<input type="hidden" id="userId" value="${userId}">
			</form>
		</div>
	</div>
	<div class="feed-body">
	<div class="left-menu">
		<ul>
			<li id="allFeeds"><g:link controller='home' action="feed">FEED</g:link></li>
			<li id="recentSearches"><a href="#" onclick="getRecentSearches({usetId: ${userId}})">RECENT SEARCHES</a></li>
			<li id="myThreads"><a href="#" onclick="getMyThreads({usetId: ${userId}})">MY THREADS</a></li>
			<li id="sprints"><a href="#" >SPRINTS</a></li>
		</ul>
	</div>
	<div class="main container-fluid">
		<div id="graphList">
			<div class="new-post">
				<form action="/discussion/createTopic" method="post">
					<div class="input-affordance left-addon">
						<i class="fa fa-pencil"></i> <input class="full-width discussion-topic-input"
							type="text" placeholder="New question or discussion topic?"
							name="name" id="discussion-topic" required />
						<input type="radio" class="radio-public" name="visibility" id="public" value="public" checked><label for="public" class="radio-public-label">Public</label>
						<input type="radio" class="radio-private" name="visibility" id="private" value="private"><label for="private" class="radio-private-label">Private</label>
						<hr>
						<input type="text" id="discussion-discription" class="full-width discussion-topic-description" placeholder="Enter Comment/Description"
						name="discussionPost" required></textarea>
					</div>
					<input type="hidden" name="group" value="${groupName}" />
				</form>
			</div>
			<div id="discussions">
				<g:render template="/feed/discussions" />
			</div>
		</div>
		<div id="getMoreDiscussions"></div>
	</div>
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
