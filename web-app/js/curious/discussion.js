"use strict";

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
	var discussionElementID = '#discussion-' + discussionHash;
	var totalComments = $('.comment-button', discussionElementID).data('totalComments');
	var url = "/api/discussionPost?" + $.param(getCSRFPreventionObject("getCommentsCSRF", args)) + "&callback=?";

	queueJSON("fetching more comments", url, function(data) {
		if (!checkData(data) || !data.posts) {
			return;
		}

		if ((commentsArgs.offset + commentsArgs.max) >= totalComments) {
			$('.view-comment', discussionElementID).remove();
		}
		renderComments(discussionHash, data.posts, data);

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
	var compiledHTML = "";

	$.each(posts, function(index, post) {
		post.message = post.message.replace(/<br.*?>/g, '\n');
		compiledHTML = compileTemplate('_comments', {discussionPost: post, discussionDetails: data.discussionDetails, userId: data.userId});
		if (!append) {
			$(compiledHTML).prependTo(discussionElementID + ' .comments').slideDown();
		} else {
			$(compiledHTML).appendTo(discussionElementID + ' .comments').slideDown();
		}
	});


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
					// If not on the single discussion show page
					if (!isTabActive('#discussions/' + discussionHash)) {
						// Then we are on some listing page so just remove the discussion element.
						$this.parents('.feed-item').fadeOut();
					} else {
						if (!window.history.back()) {
							location.href = '/home/social#discussions';
						}
					}
				} else {
					showAlert(data.message);
				}
			}, function() {
				showAlert('Internal server error occurred.');
			}, null, httpArgs);
		});
		return false;
	});

	/**
	 * Click handler to execute when user clicks on the button to delete a DiscussionPost/comment.
	 */
	$(document).on("click", "a.delete-post", function() {
		var httpArgs = {requestMethod: 'delete'};
		var $this = $(this);
		showYesNo('Are you sure want to delete this?', function() {
			var postId = $this.data('postId');
			var currentOffset = $this.closest('.feed-item').data('offset') || maxCommentsPerDiscussion;

			queueJSONAll('Deleting comment', '/api/discussionPost/' + postId + '?offset=' + (currentOffset - 1) + '&' +
					getCSRFPreventionURI('deleteDiscussionPostDataCSRF'), null,
					function(data) {
				if (!checkData(data))
					return;

				if (data.success) {
					if (data.nextFollowupPost) {
						renderComments(data.discussionHash, [data.nextFollowupPost], {discussionDetails: {isAdmin: data.isAdmin}});
					}
					$this.parent().closest('.discussion-comment').slideUp('normal', function() {
						$(this).remove();
					});
					if (isHash(["all", "discussions", "people", "owned"])) {
						var $commentButton = $this.parents().closest('.discussion').find('.comment-button');
						var totalComments = $commentButton.data('totalComments') - 1;
						$commentButton.data('totalComments', totalComments);
						$commentButton.text(totalComments);
					}
				} else {
					showAlert(data.message);
				}
			}, function(xhr) {
					console.log('Internal server error', xhr);
					if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
						showAlert(xhr.responseJSON.message);
					}
				}, null, httpArgs);
		});
		return false;
	});

	/**
	 * Click handler to execute when user clicks on the link to edit a post/comment.
	 */
	$(document).on("click", ".edit-post", function() {
		var $this = $(this);
		var $parent = $this.parents(".discussion-comment");
		var $message = $parent.find(".message");
		var previousMessage = $message.html().trim().brToNewLine();
		var postID = $this.data("postId");

		var html = compileTemplate("_commentEditForm", {id: postID, message: previousMessage});
		$parent.addClass("editing-comment");
		$message.after(html);
		$message.hide();
		$this.hide();
		$.autoResize.init();

		return false;
	});

	$(document).on("click", ".cancel-comment", function() {
		hideInlinePostEdit($(this).parents(".discussion-comment"));
		return false;
	});

	/**
	 * Click handler to execute when user hit enter i.e. submits the form of adding new DiscussionPost/comment.
	 */
	$(document).on("submit", ".new-comment-form", function() {
		var $form = $(this);
		// See base.js for implementation details of $.serializeObject()
		var params = $form.serializeObject();

		queuePostJSON('Adding Comment', '/api/discussionPost', getCSRFPreventionObject('addCommentCSRF', params),
				function(data) {
			if (!checkData(data)) {
				return;
			}
			if (!data.success) {
				showAlert(data.message);
				return;
			}
			renderComments(params.discussionHash, [data.post], data, true);
			$form[0].reset();
			var discussionElement = getDiscussionElement(params.discussionHash);
			var currentOffset = discussionElement.data('offset') || maxCommentsPerDiscussion;
			discussionElement.data('offset', ++currentOffset);
		}, function(xhr) {
			console.log('Internal server error');
			if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
				showAlert(xhr.responseJSON.message);
			}
		}, 0, {spinnerOn: $form.parents(".add-comment")});

		return false;
	});

	/**
	 * Form submit handler which will be executed when the user either submits the form for editing a post/comment
	 * or sets the first post message/description of a discussion.
	 */
	$(document).on("submit", ".edit-comment-form", function() {
		var $form = $(this);
		// See base.js for implementation details of $.serializeObject()
		var params = $form.serializeObject();
		var $parentComment = $form.parents(".discussion-comment");
		var isFirstPostUpdate = $parentComment.length === 0;
		var args = {requestMethod: "PUT", spinnerOn: (isFirstPostUpdate ? $form : $parentComment)};

		queueJSONAll('Adding Comment', '/api/discussionPost', getCSRFPreventionObject('addCommentCSRF', params),
				function(data) {
					if (!checkData(data)) {
						return;
					}
					if (!data.success) {
						showAlert(data.message);
						return;
					}

					if (isFirstPostUpdate) {
						$(".first-post-container").html("").html(params.message.newLineToBr());
					} else {
						hideInlinePostEdit($parentComment);
						$parentComment.find(".message").html(params.message.newLineToBr());
					}
				}, function(xhr) {
					console.log('Internal server error');
					if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
						showAlert(xhr.responseJSON.message);
					}
				}, 0, args);

		return false;
	});

	$(document).on("click", ".share-button", function() {
		$(this).popover({html: true}).popover('show');
		$('.share-link').select();
	});

	// On click of comment button in the single discussion page
	$(document).on("click", ".comment-button", function() {
		// Just put focus on the comment box
		$(this).parents(".discussion").find(".comment-message").focus();
		return false;
	});

	$(document).on("change", "#disable-comments", function() {
		var disabled = $(this).is(":checked");
		var params = {
			id: $(this).data("hash"),
			disable: disabled
		};

		queueJSON('Disallowing commenting', '/api/discussion/action/disableComments', params,
			function(data) {
				if (!checkData(data)) {
					return;
				}

				displayFlashMessage();
			});
	});

	var $modal = $("#edit-discussion-modal");

	$(document).on("click", ".edit-discussion", function() {
		var existingTitle = $(".discussion-title").text().trim();
		var existingDescription = $(".first-post-message").html();
		if (existingDescription) {
			existingDescription = existingDescription.trim().brToNewLine();
		}

		$("#new-discussion-name").val(existingTitle);
		$("#new-description").val(existingDescription);

		$modal.modal("show");
		return false;
	});

	$(document).on("shown.bs.modal", $modal, function() {
		$("#new-discussion-name").focus().select();
		$.autoResize.init();
	});

	$(document).on("submit", "#edit-discussion-form", function() {
		var $form = $(this);
		var newName = $("#new-discussion-name").val();
		var newDescription = $("#new-description").val().trim();

		var existingName = $(".discussion-title").text();
		var existingDescription = $(".first-post-message").html();
		if (existingDescription) {
			existingDescription = existingDescription.trim().brToNewLine();
		}

		var discussionHash = $("input[name=discussionHash]").val();
		var params = {discussionHash: discussionHash, name: newName, message: newDescription};
		var args = {requestMethod: "PUT", spinnerOn: $form};

		if (newName && (newName !== existingName || newDescription !== existingDescription)) {
			queueJSONAll("", "/api/discussion", makeGetArgs(params), function(data) {
				if (checkData(data)) {
					$("#edit-discussion-modal").modal("hide");
					$(".discussion-title").text(newName);
					$(".first-post-message").html(newDescription.newLineToBr());
					displayFlashMessage("#title-updated", 2500);
				} else {
					showAlert('Failed to set name');
				}
			}, function() {}, 0, args);
		}

		return false;
	})
});

var plot = null;
var tagList = null;
function doLogout() {
	callLogoutCallbacks();
}

$(function() {
	initTagListOnly();

	$('li#share-discussion').on('click', function() {
		showShareDialog(null);
		return false;
	});
});

function discussionShow(hash) {
	$('#feed').infiniteScroll("stop");

	queueJSON('Getting discussion', '/api/discussion/' + hash + '?' + getCSRFPreventionURI('getDiscussionList') + '&callback=?',
			function(data) { 
		if (data.success) { 
			$('.container-fluid').removeClass('main');
			var discussionDetails = data.discussionDetails;
			discussionDetails.serverURL = serverURL;
			var compiledHTML = compileTemplate("_showDiscussion", discussionDetails);
			$('#feed').html(compiledHTML);

			showCommentAgeFromDate();
			getComments(hash, commentsArgs);		// See feeds.js for "commentsArgs"
			$.autoResize.init();

			if (discussionDetails.firstPost && discussionDetails.firstPost.plotDataId) {
				plot = new PlotWeb(tagList, discussionDetails.userId, discussionDetails.username, "#plotDiscussArea", true, true, new PlotProperties({
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
				plot.loadSnapshotId(discussionDetails.firstPost.plotDataId, hash); // send discussion hash as authentication confirmation
			}
		} else {
			$('.alert').text(data.message);
		}
		
		setQueryHeader('Curious Discussions', true);
	});
}

function hideInlinePostEdit($comment) {
	$comment.removeClass("editing-comment");    // Remove the class to revert the UI
	$comment.find(".edit-post").show();         // Show the pencil icon again to re-edit later
	$comment.find(".message").show();           // Show the actual post message again
	$comment.find(".comment-form").remove();    // Remove the added inline form
}