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
 * @param max
 * @param offset
 * @param callback Callback to execute when response are rendered in the DOM.
 */
function getComments(discussionHash, max, offset, callback) {
	var url = "/api/discussionPost?discussionHash=" + discussionHash + "&offset=" + offset  + "&max=" + max + "&" +
			getCSRFPreventionURI("getCommentsCSRF") + "&callback=?";

	var discussionElementID = '#discussion-' + discussionHash;

	queueJSON("fetching more comments", url, function(data) {
		if (!checkData(data)) {
			return;
		}

		if (!data.posts) {
			$(' .view-comment', discussionElementID).html('');
		} else {
			var compiledHTML = "";

			$.each(data.posts, function(index, item) {
				compiledHTML += compileTemplate("_comments", {discussionPost: item, isAdmin: data.isAdmin,
						userId: data.userId});
			});

			$('.comments', discussionElementID).append(compiledHTML);
			showCommentAgeFromDate();
		}

		if (callback) {
			callback(data);
		}
	});
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

	/**
	 * Click handler to execute when user hit enter i.e. submits the form of adding new DiscussionPost/comment.
	 */
	$(document).on("submit", ".comment-form", function() {
		// See base.js for implementation details of $.serializeObject()
		var params = $(this).serializeObject();

		queuePostJSON('Adding Comment', '/api/discussionPost', getCSRFPreventionObject('addCommentCSRF', params),
				function(data) {
			if (!checkData(data)) {
				return;
			}

			if (data.success) {
				location.reload();
			}
		}, function(xhr) {
			console.log('Internal server error');
		});

		return false;
	});

	$(document).on("click", ".share-button", function() {
		$(this).popover({html: true});
		$('.share-link').select();
	});
});