/**
 * Script contains Discussion & DiscussionPost (comment) related code.
 */
function modifyShare(discussionHash) {
	var $selectElement = $('select#shareOptions');
	if (discussionHash == null) {
		discussionHash = $('input#discussionHash').val()
	}
	$.ajax({
		type: 'POST',
		url: '/home/shareDiscussion',
		data: {
			discussionHash: discussionHash,
			shareOptions: $selectElement.val().join(',')
		},
		success: function(data) {
			showAlert(JSON.parse(data).message);
		},
		error: function(xhr) {
			showAlert(JSON.parse(xhr.responseText).message);
		}
	});
}

function showShareDialog(discussionHash) {
	$('div#share-dialog').dialog({
		dialogClass: "no-close",
		modal: false,
		resizable: false,
		title: "Query",
		buttons: {
			"Yes ": function() {
				$(this).dialog("close");
				if (discussionHash != null) {
					modifyShare(discussionHash);
				} else {
					modifyShare(null);
				}
			},
			No: function() {
				$(this).dialog("close");
			}
		}
	});
}

/**
 * Used to get the DiscussionPost/comments data associated with a Discussion of given hash.
 * @param discussionHash
 * @param args Supported values: max, offset, sort, order 
 * @param callback Callback to execute when response are rendered in the DOM.
 */
function getComments(discussionHash, args, callback) {
	args.discussionHash = discussionHash;
	var url = "/api/discussionPost?" + $.param(getCSRFPreventionObject("getCommentsCSRF", args)) + "&callback=?";

	queueJSON("fetching more comments", url, function(data) {
		if (!checkData(data)) {
			return;
		}

		renderComments(discussionHash, data.posts, data, window.singleDiscussionPage);

		if (callback) {
			callback(data);
		}
	});
}

/**
 * This method is used to render the new comments data to the DOM/UI using the Lodash template.
 * 
 * @param discussionHash Hash ID of the discussion to render comments for.
 * @param posts list of posts to render
 * @param data {discussionDetails: {isAdmin: true/false}, userId: 1234}}
 * @param append Whether to append the new discussion posts should be added to the last of the comments list.
 */
function renderComments(discussionHash, posts, data, append) {
	var discussionElementID = '#discussion-' + discussionHash;

	if (!posts) {
		$('.view-comment', discussionElementID).html('');
		return;
	}

	var compiledHTML = "";

	// If we are in discussion listing page (i.e. feeds page)
	if (!window.singleDiscussionPage) {
		// Then reverse the comments to display the most recent comment at last
		posts = posts.reverse();
	}

	$.each(posts, function(index, post) {
		compiledHTML += compileTemplate("_comments", {discussionPost: post, discussionDetails: data.discussionDetails, userId: data.userId});
	});

	if (!append) {
		$('.comments', discussionElementID).prepend(compiledHTML);
	} else {
		$('.comments', discussionElementID).append(compiledHTML);
	}

	showCommentAgeFromDate();
}

$(document).ready(function() {
	/**
	 * Click handler to execute when user clicks on the button to delete a Discussion.
	 */
	var httpArgs = {requestMethod: 'delete'};
	$(document).on("click", "a.delete-discussion", function() {
		var $this = $(this);
		showYesNo('Are you sure want to delete this?', function() {
			var discussionHash = $this.data('discussionHash');

			queueJSONAll('Deleting Discussion', '/api/discussion/' + discussionHash,
					getCSRFPreventionObject('deleteDiscussionDataCSRF'), function(data) {

				if (!checkData(data))
					return;

				if (data.success) {
					if (isOnFeedPage() || location.hash.indexOf('#sprints') > -1) {
						showAlert(data.message, function() {
							$this.parents('.feed-item').fadeOut();
						});
					} else {
						location.href = '/home/social#all';
					}
				} else {
					showAlert(data.message);
				}
			}, function(xhr) {
				showAlert('Internal server error occurred.');
			}, null, httpArgs);
		});
		return false;
	});

	/**
	 * Click handler to execute when user clicks on the button to delete a DiscussionPost/comment.
	 */
	$(document).on("click", "a.delete-post", function() {
		var httpArgs ={requestMethod:'delete'};
		var $this = $(this);
		showYesNo('Are you sure want to delete this?', function() {
			var postId = $this.data('postId');
			queueJSONAll('Deleting comment', '/api/discussionPost/' + postId,
					getCSRFPreventionObject('deleteDiscussionPostDataCSRF'),
					function(data) {
				if (!checkData(data))
					return;

				if (data.success) {
					showAlert(data.message, function() {
						$this.parent().closest('.discussion-comment').fadeOut(null, function() {
							$(this).remove();
						});
						if (isOnFeedPage()) {
							var $commentButton = $this.parents().closest('.discussion').find('.comment-button');
							var totalComments = $commentButton.data('totalComments') - 1;
							$commentButton.data('totalComments', totalComments);
							$commentButton.text(totalComments);
						}
					});
					window.discussionCommentsOffsetMargin--;
				} else {
					showAlert(data.message);
				}
			}, function(xhr) {
				showAlert('Internal server error occurred.');
			}, null, httpArgs);
		});
		return false;
	});

	/**
	 * Click handler to execute when user hit enter i.e. submits the form of adding new DiscussionPost/comment.
	 */
	$(document).on("submit", ".comment-form", function() {
		var $form = $(this);
		// See base.js for implementation details of $.serializeObject()
		var params = $form.serializeObject();

		queuePostJSON('Adding Comment', '/api/discussionPost', getCSRFPreventionObject('addCommentCSRF', params),
				function(data) {
			if (!checkData(data)) {
				return;
			}

			renderComments(params.discussionHash, [data.post], data, !window.singleDiscussionPage);
			$form[0].reset();
			window.discussionCommentsOffsetMargin++;
		}, function(xhr) {
			console.log('Internal server error');
		});

		return false;
	});

	$(document).on("click", ".share-button", function() {
		$(this).popover({html: true});
		$('.share-link').select();
	});

	// On click of comment button in the single discussion page
	$(document).on("click", ".comment-button", function() {
		// Just put focus on the comment box
		$(this).parents(".discussion").find("input[name=message]").focus();
		return false;
	});
});

var plot = null;
var tagList = null;
var discussionTitle = "${discussionTitle}";
var alreadySentName = null;
var preventCommentSubmit = false;

function doLogout() {
	callLogoutCallbacks();
}

$(function() {
	initTagListOnly();

	// plot.loadSnapshotId(${firstPost.plotDataId});
	var discussTitleArea = $("#discussTitleArea");
	var discussTitle = $("#discussTitleSpan");

	var saveTitle = function(closure) {
		var discussTitleInput = $("#discussTitleInput");
		if (discussTitleInput) {
			var newName = discussTitleInput.val();
			if (newName != alreadySentName && newName != discussionTitle && newName !== undefined) {
				alreadySentName = newName;
				preventCommentSubmit = true; // for some reason, comment submission happening twice?
				backgroundJSON("setting discussion name", makeGetUrl('setDiscussionNameData'), makeGetArgs({ discussionHash:"${discussionHash}", name:newName }), function(data) {
					if (checkData(data)) {
						preventCommentSubmit = false;
						discussionTitle = newName;
						discussTitle.html(newName);
						discussTitle.off('mouseup');
						discussTitle.on('mouseup', discussTitle.data('rename'));
						if (closure) closure();
					} else {
						showAlert('Failed to set name');
					}
				});
			} else if (closure && (!preventCommentSubmit))
				closure();
		}
	}

	var renameDiscussionHandler = function(e) {
		if (e.keyCode == 13) {
			saveTitle();
			$("#postcommentarea").focus();
		}
	}

	var discussTitleRename = function(e) {
		discussTitleArea.off('mouseup');
		discussTitle.html('<input type="text" id="discussTitleInput"></input>');
		var discussTitleInput = $("#discussTitleInput");
		discussTitleInput.val(discussionTitle);
		discussTitleInput.keyup(renameDiscussionHandler);
		discussTitleInput.focus();
		discussTitleInput.blur(function() {
			saveTitle();
		});
	}

	discussTitle.data('rename', discussTitleRename);

	$("#postcommentarea").keyup(function(e) {
		if (e.keyCode == 13) {
			saveTitle(function() {
				$(".comment-form").submit();
			});
		}
	});

	$("#commentSubmitButton").click(function() {
		saveTitle(function() {
			$(".comment-form").submit();
		});
	});

	$('li#share-discussion').on('click', function() {
		showShareDialog(null);
		return false;
	});
});

function infiniteScrollComments(discussionHash) {
	commentsArgs.max = 5;
	
	$(".comments").infiniteScroll({
		bufferPx: 360,
		finalMessage: 'No more comments to show',
		onScrolledToBottom: function(e, $element) {
			// Pause the scroll event to not trigger again untill AJAX call finishes
			// Can be also called as: $("#postList").infiniteScroll("pause")
			this.pause();
			commentsArgs.offset = this.getOffset();
			commentsArgs.offset += window.discussionCommentsOffsetMargin;
			this.setOffset(commentsArgs.offset);
			window.discussionCommentsOffsetMargin = 0;

			getComments(discussionHash, commentsArgs, function(data) {
				if (!data.posts) {
					this.finish();
				} else {
					this.setNextPage();		// Increment offset for next page
					this.resume();			// Re start scrolling event to fetch next page data on reaching to bottom
				}
			}.bind(this));
		}
	});
}

function discussionShow(hash) {
	$('#feed').infiniteScroll("stop");

	queueJSON('Getting discussion', '/api/discussion/' + hash + '?' + getCSRFPreventionURI('getDiscussionList') + '&callback=?',
			function(data) { 
		if (data.success) { 
			$('.container-fluid').removeClass('main');
			var discussionDetails = data.discussionDetails;
			discussionDetails.serverURL = window.location.host;
			var compiledHTML = compileTemplate("_showDiscussion", discussionDetails);
			$('#feed').html(compiledHTML);

			infiniteScrollComments(hash);
			getComments(hash, commentsArgs);		// See feeds.js for "commentsArgs"
			if (discussionDetails.firstPost && discussionDetails.firstPost.plotDataId) {
				plot = new Plot(tagList, discussionDetails.userId, discussionDetails.username, "#plotDiscussArea", true, true, new PlotProperties({
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
				plot.loadSnapshotId(discussionDetails.firstPost.plotDataId);
			}
		} else {
			$('.alert').text(data.message);
		}
		
		$('.nav').hide();
		setQueryHeader('Curious Discussions', true);
	}, function(data) {
		showAlert('Internal server error occurred.');
	});
}
