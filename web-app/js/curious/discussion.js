/**
 * Script contains Discussion & DiscussionPost (comment) related code.
 */
var commentsArgs = {offset: 0, sort: "created", order: "desc"};

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

		renderComments(discussionHash, data.posts, data, window.isDiscussionSinglePage);

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
 * @param data {isAdmin: true/false, userId: 1234}
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
	if (!window.isDiscussionSinglePage) {
		// Then reverse the comments to display the most recent comment at last
		posts = posts.reverse();
	}

	$.each(posts, function(index, post) {
		compiledHTML += compileTemplate("_comments", {discussionPost: post, isAdmin: data.isAdmin, userId: data.userId});
	});

	if (append) {
		$('.comments', discussionElementID).append(compiledHTML);
	} else {
		$('.comments', discussionElementID).prepend(compiledHTML);
	}

	showCommentAgeFromDate();
}

$(document).ready(function() {
	/**
	 * Click handler to execute when user clicks on the button to delete a Discussion.
	 */
	$(document).on("click", "a.delete-discussion", function() {
		var $this = $(this);
		showYesNo('Are you sure want to delete this?', function() {
			var discussionHash = $this.data('discussionHash');

			queueJSONAll('Deleting Discussion', '/api/discussion/' + discussionHash,
					getCSRFPreventionObject('deleteDiscussionDataCSRF'), function(data) {

				if (!checkData(data))
					return;

				if (data.success) {
					if (isOnFeedPage() || location.pathname.indexOf('/home/sprint') > -1) {
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
			}, null, 'delete');
		});
		return false;
	});

	/**
	 * Click handler to execute when user clicks on the button to delete a DiscussionPost/comment.
	 */
	$(document).on("click", "a.delete-post", function() {
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
				} else {
					showAlert(data.message);
				}
			}, function(xhr) {
				showAlert('Internal server error occurred.');
			}, null, 'delete');
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

			renderComments(params.discussionHash, [data.post], data, !window.isDiscussionSinglePage);
			$form[0].reset();
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