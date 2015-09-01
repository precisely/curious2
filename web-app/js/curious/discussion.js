/**
 *
 *
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

function showCommentDialog(discussionHash) {
	$('div#comment-dialog').dialog({
		dialogClass: "no-close",
		modal: false,
		resizable: false,
		title: "Comment",
		buttons: {
			"Post ": function() {
				$(this).dialog("close");
				if (discussionHash) {
					addComment(discussionHash);
				} else {
					addComment(null);
				}
			}
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
	if (!window.isDiscussionSinglePage) {
		// Then reverse the comments to display the most recent comment at last
		posts = posts.reverse();
	}

	$.each(posts, function(index, post) {
		compiledHTML += compileTemplate("_comments", {discussionPost: post, discussionDetails: data.discussionDetails, userId: data.userId});
	});
}

$(document).ready(function() {
	$(document).on("click", "a.delete-discussion", function() {
		var $this = $(this);
		showYesNo('Are you sure want to delete this?', function() {
			var discussionHash = $this.data('discussionHash');
			queueJSONAll('Deleting Discussion', '/api/discussion/' + discussionHash,
					getCSRFPreventionObject('deleteDiscussionDataCSRF'),
					function(data) {
				if (!checkData(data))
					return;

				if (data.success) {
					if (isOnFeedPage()) {
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
						$this.parent().closest('.discussion-comment').fadeOut();
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
});

