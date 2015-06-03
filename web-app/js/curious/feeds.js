/**
 * 
 */
var autocompleteWidget;
var searchList = [];

function isTabActive(anchor) {
	return location.hash == anchor;
}

function isOnFeedPage() {
	var anchor = location.hash.slice(1);
	return ['all', 'people', 'discussions', 'sprints'].indexOf(anchor) > -1
}

function registerScroll() {
	$('#feed').infiniteScroll({
		bufferPx: 20,
		bindTo: $('.main'),
		onScrolledToBottom: function(e, $element) {
			this.pause();
			var url;
			var anchor = location.hash.slice(1);

			if (isOnFeedPage()) {
				url = '/search/indexData?type=' + anchor + '&offset=' + this.getOffset() + '&max=5&' +
						getCSRFPreventionURI('getFeedsDataCSRF') + '&callback=?';
			} else {
				this.finish();
				return;
			}

			queueJSON('Loading data', url,
					function(data) {
				if (!checkData(data))
					return;

				if (data.success) {
					if (data.listItems == false) {
						this.finish();
					} else {
						if (isTabActive('#people')) {
							$.each(data.listItems, function(index, user) {                                                           
								var compiledHtml = _.template(_people)({'user': user});                                       
								$('#feed').append(compiledHtml);                                                                
							});                                                                                                      
						} else if (isTabActive('#discussions')) {
							$.each(data.listItems.discussionList, function(index, discussionData) {
								var compiledHtml = _.template(_discussions)({'discussionData': discussionData, 'groupName': data.listItems.groupName});
								$('#feed').append(compiledHtml);
								showCommentAgeFromDate();
							});
						} else if (isTabActive('#all')) {
							addAllFeedItems(data);
						} else {
							$.each(data.listItems.sprintList, function(index, sprint) {
								var compiledHtml = _.template(_sprints)({'sprint': sprint});
								$('#feed').append(compiledHtml);
							});
							showCommentAgeFromDate();
						}
						this.setNextPage();
						this.resume();
					}
				} else {
					$('.alert').text(data.message);
				}
			}.bind(this), function(data) {
				showAlert('Internal server error occurred.');
			});
		}
	});
}

$(window).load(function() {
	if (isTabActive('#sprints')) {
		showSprints();
	} else if (isTabActive('#discussions')) {
		showDiscussions();
	} else if (isTabActive('#people')) {
		showPeople();
	} else if (isTabActive('#all')) {
		showAllFeeds();
	} else {
		location.hash = '#all';
		showAllFeeds();
	}

});

function setUrlHash(newHash) {
	var hashLoc = location.href.indexOf('#')
	if (hashLoc > -1) {
		location.href = location.href.substring(0, hashLoc) + newHash;
	} else
		location.href = location.href + newHash;
}

$(document).ready(function() {
	
	if (isOnFeedPage()) {
		registerScroll();
	}

	$('html').on('click', function(e) {
		if (typeof $(e.target).data('original-title') == 'undefined' && !$(e.target).is('.share-button img')) {
			$('[data-original-title]').popover('hide');
		}
	});

	$(document).on("click", "a.delete-discussion", function() {
		var $this = $(this);
		showYesNo('Are you sure want to delete this?', function() {
			var discussionHash = $this.data('discussionHashId');
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

	$('#sprint-tags').keypress(function (e) {
		var key = e.which;

		if (key == 13) { // the enter key code
			addEntryToSprint('sprint-tags', '');
			return false;
		}
	});

	$('#submitSprint').submit(function(event) {
		// See base.js for implementation details of $.serializeObject()
		var params = $(this).serializeObject();
		queuePostJSON('Updating sprint', '/data/updateSprintData', getCSRFPreventionObject('updateSprintDataCSRF', params),
				function(data) {
			if (!checkData(data))
				return;

			if (data.error) {
				$('.modal-dialog .alert').text('Error occurred while submitting the form.').removeClass('hide');
				setInterval(function() {
					$('.modal-dialog .alert').addClass('hide');
				}, 5000);
			} else {
				location.assign('/home/sprint/' + data.hash);
				clearSprintFormData()
			}
		}, function(xhr) {
		});
		return false;
	});

	createAutocomplete('sprint-participants', 'participantsAutocomplete');
	createAutocomplete('sprint-admins', 'adminsAutocomplete');

	$(document).on("click", ".deleteSprintEntry", function() {
		var $element = $(this);
		var repeatType = $(this).data('repeatType');
		var id = $(this).data('id');
		console.log('repeat type: ', repeatType);
		deleteGhost($element, id, true);
		return false;
	});

	$(document).on("click", ".deleteParticipants", function() {
		var $element = $(this);
		var username = $(this).data('username');
		deleteParticipantsOrAdmins($element, username, 'participants');
		return false;
	});

	$(document).on("click", ".deleteAdmins", function() {
		var $element = $(this);
		var username = $(this).data('username');
		deleteParticipantsOrAdmins($element, username, 'admins');
		return false;
	});

	$('#close-sprint-modal').click(function() {
		$('#createSprintOverlay').modal('hide').data('bs.modal', null);
		clearSprintFormData();
		return false;
	});

	$('#feed-sprints-tab a').click(function(e) {
		showSprints();
	});

	$('#feed-discussions-tab a').click(function(e) {
		showDiscussions();
	})

	$('#feed-people-tab a').click(function(e) {
		showPeople();
	});

	$('#feed-all-tab a').click(function(e) {
		showAllFeeds();
	})
});

function showSprints() {
	queueJSON('Getting sprint list', '/search/indexData?type=sprints&' + 
			getCSRFPreventionURI('getFeedsDataCSRF') + '&callback=?',
			function(data) {
		if (data.success) {
			if (data.listItems != false) {
				// Adding custom classes according to the tabs, so as to be able to modify the elements differently in respective tabs if required
				$('#feed').removeClass().addClass('type-sprints').html('');
				$.each(data.listItems.sprintList, function(index, sprint) {
					var compiledHtml = _.template(_sprints)({'sprint': sprint});
					$('#feed').append(compiledHtml);
				});
				showCommentAgeFromDate();
			} else {
				$('#feed').html('No sprints to show.');
			}
		} else {
			$('.alert').text(data.message);
		}
		$('#feed-right-tab').html('<a onclick="createSprint()" href="#">START NEW SPRINT</a>');
		$('#queryTitle').text('Tracking Sprints');
		$('#feed-sprints-tab a').tab('show');
	}, function(data) {
		showAlert('Internal server error occurred.');
	});
	registerScroll();
}

function showDiscussions() {
	queueJSON('Getting discussion data', '/search/indexData?type=discussions&offset=0&max=5&' + 
			getCSRFPreventionURI('getFeedsDataCSRF') + '&callback=?',
			function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			if (data.listItems == false) {
				$('#feed').text('No discussions to show.');
			} else {
				$('#feed').removeClass().addClass('type-discussions').html('');

				// Showing create discussion form only once
				var createDiscussionForm = _.template(_createDiscussionForm)({'groupName': data.listItems.groupName});
				$('#feed').append(createDiscussionForm);

				// Handlers for discussion form input fields
				$('#discussion-topic').keypress(function (e) {
					var key = e.which;
					if (key == 13) {
						$('.input-affordance hr').show();
						$('input[name = discussionPost]').show();
						return false;  
					}
				});

				$('#discussion-discription').keypress(function (e) {
					var key = e.which;
					if (key == 13) {
						$('#create-discussion').submit();
						return false;  
					}
				});

				$.each(data.listItems.discussionList, function(index, discussionData) {
					var compiledHtml = _.template(_discussions)({'discussionData': discussionData});
					$('#feed').append(compiledHtml);
					showCommentAgeFromDate();
				});
				$('#feed-discussions-tab a').tab('show');
				$('#feed-right-tab').html('');
				$('#queryTitle').text('Discussions');
				$('.share-button').popover({html:true});
				$('.share-button').on('click', function () {
					$('.share-link').select();
				});
			}
		} else {
			$('.alert').text(data.message);
		}
	}, function(data) {
		showAlert('Internal server error occurred.');
	});
	registerScroll();
}

function showPeople() {
	queueJSON('Getting people list', '/search/indexData?type=people&offset=0&max=5&' + 
			getCSRFPreventionURI('getFeedsDataCSRF') + '&callback=?',
			function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			if (data.listItems == false) {
				$('#feed').text('No people to show.');
			} else {
				$('#feed').removeClass().addClass('type-people').html('');
				$.each(data.listItems, function(index, user) {
					var compiledHtml = _.template(_people)({'user': user});
					$('#feed').append(compiledHtml);
				});
				$('#feed-people-tab a').tab('show');
				$('#feed-right-tab').html('');
				$('#queryTitle').text('People');		
			}
		} else {
			$('.alert').text(data.message);
		}
	}, function(data) {
		showAlert('Internal server error occurred.');
	});
	registerScroll();
}

function showAllFeeds() {
	queueJSON('Getting feeds', '/search/indexData?type=all&offset=0&max=5&' + 
			getCSRFPreventionURI('getFeedsDataCSRF') + '&callback=?',
			function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			if (data.listItems == false) {
				$('#feed').text('No feeds to show.');
			} else {
				$('#feed').removeClass().addClass('type-all').html('');
				addAllFeedItems(data);
				$('#feed-all-tab a').tab('show');
				$('#feed-right-tab').html('');
				$('#queryTitle').text('All Feeds');		
				$(".share-button").popover({html:true});
				$('.share-button').on('click', function () {
					$('.share-link').select();
				});
			}
		} else {
			$('.alert').text(data.message);
		}
	}, function(data) {
		showAlert('Internal server error occurred.');
	});
	registerScroll();
}

function addAllFeedItems(data) {
	data.listItems.sort(function(a, b) {
		return a.updated > b.updated ? -1 : (a.updated < b.updated ? 1 : 0)
	});

	$.each(data.listItems, function(index, item) {
		var compiledHtml = '';
		if (item.type == 'spr') {
			compiledHtml = _.template(_sprints)({'sprint': item});
		} else if (item.type == 'dis') {
			compiledHtml = _.template(_discussions)({'discussionData': item});
		} else if (item.type == 'usr') {
			compiledHtml = _.template(_people)({'user': item});
		}
		$('#feed').append(compiledHtml);
	});
	showCommentAgeFromDate();
}

function clearSprintFormData() {
	var form = $('#submitSprint');
	// iterate over all of the inputs for the form
	// element that was passed in
	$(':input', form).each(function() {
		var type = this.type;
		var tag = this.tagName.toLowerCase();
		if (type == 'text' || tag == 'textarea') {
			this.value = '';
		} else if (type == 'checkbox' || type == 'radio') {
			this.checked = false;
		}
	});
	$('.modal ul li').html('');
	$('.submit-sprint').text('Create Sprint');
	$('#createSprintOverlay .modal-title').text('Create Sprint');
}

function deleteParticipantsOrAdmins($element, username, actionType) {
	var actionName = (actionType === 'participants') ? 'deleteSprintMemberData' : 'deleteSprintAdminData';

	queuePostJSON('Removing members', '/data/' + actionName, getCSRFPreventionObject(actionName + 'CSRF', 
			{username: username, now: new Date().toUTCString(), sprintHash: $('#sprintIdField').val(), 
			timeZoneName: jstz.determine().name()}), 
			function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			$element.parents('li').remove();
		} else {
			$('.modal-dialog .alert').text(data.errorMessage).removeClass('hide');
			setInterval(function() {
				$('.modal-dialog .alert').addClass('hide');
			}, 5000);
		}
	}, function(xhr) {
		console.log('error: ', xhr);
	});
}

function createAutocomplete(inputId, autocompleteId) {
	$('#' + inputId).autocomplete({
		appendTo: '#' + autocompleteId,
		minLength: 0,
		source: []
	});

	$('#' + inputId).on('keyup', function() {
		var searchString = $('#' + inputId).val();
		queueJSON('Getting autocomplete', '/data/getAutocompleteParticipantsData?' + getCSRFPreventionURI("getAutocompleteParticipantsDataCSRF") + "&callback=?", 
				{searchString: searchString},
				function(data) {
			if (!checkData(data))
				return;

			if (data.success) {
				$('#' + inputId).autocomplete('option', 'source', data.usernameList);
			}
		}, function(xhr) {
			console.log('error: ', xhr);
		});
	});

	$('#' + inputId).keypress(function (e) {
		var userName = $(this).val();
		var key = e.which;
		if (key == 13) { // the enter key code
			addSprintMemberOrAdmin(inputId, userName);
			return false;  
		}
	});

	$('#' + inputId).on('autocompleteselect', function( event, ui ) {
		addSprintMemberOrAdmin(inputId, ui.item.value);
		return false;
	});
}

function addSprintMemberOrAdmin(inputId, userName) {
	var actionName = (inputId === 'sprint-participants') ? 'addMemberToSprintData' : 'addAdminToSprintData';
	var deleteButtonClass = (inputId === 'sprint-participants') ? 'deleteParticipants' : 'deleteAdmins';
	queuePostJSON('Adding members', '/data/' + actionName, getCSRFPreventionObject(actionName + 'CSRF', 
			{username: userName, sprintHash: $('#sprintIdField').val()}),
			function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			console.log('added persons: ', data);
			$("#" + inputId).val('');
			addParticipantsAndAdminsToList($("#" + inputId + "-list"), deleteButtonClass, userName);
		} else {
			showBootstrapAlert($('.modal-dialog .alert'), data.message);
		}
	}, function(xhr) {
		console.log('error: ', xhr);
	});
}

function deleteSimpleEntry(id, $element) {
	var now = new Date();
	this.currentTimeUTC = now.toUTCString();
	this.cachedDateUTC = now.toUTCString();
	this.timeZoneName = jstz.determine().name();
	this.baseDate = new Date('January 1, 2001 12:00 am').toUTCString();

	queueJSON("deleting entry", "/home/deleteEntrySData?entryId=" + id
			+ "&currentTime=" + this.currentTimeUTC + "&baseDate=" + this.baseDate
			+ "&timeZoneName=" + this.timeZoneName + "&displayDate=" + this.cachedDateUTC + "&"
			+ getCSRFPreventionURI("deleteEntryDataCSRF") + "&callback=?",
			function(entries) {
		if (checkData(entries, 'success', "Error deleting entry")) {
			$element.parents('li').remove();
			if (entries[1] != null)
				autocompleteWidget.update(entries[1][0], entries[1][1], entries[1][2], entries[1][3]);
			if (entries[2] != null) {
				autocompleteWidget.update(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
			}
		} else {
			if ($element.parents('.modal-dialog').length > 0) {
				showBootstrapAlert($('.modal-dialog .alert'), data.message);
			} else {
				showBootstrapAlert($('.alert'), data.message);
			}
		}
	});
}

function deleteGhost($tagToDelete, entryId, allFuture) {
	this.baseDate = new Date('January 1, 2001 12:00 am').toUTCString();

	queueJSON("deleting entry", makeGetUrl("deleteGhostEntryData"), makeGetArgs(getCSRFPreventionObject("deleteGhostEntryDataCSRF", {entryId:entryId,
		all:(allFuture ? "true" : "false"), date: this.baseDate, baseDate: this.baseDate})),
		function(response) {
			if (!checkData(response))
				return;

			if (typeof response == 'string') {
				showAlert(response);
			} else {
				$tagToDelete.parents('li').remove();
			}
		}
	);
}

function addEntryToSprint(inputElement, suffix) {
	var $inputElement = $('#' + inputElement);
	var virtualUserId = $('#sprintVirtualUserId').val();
	$inputElement.val($inputElement.val() + ' ' + suffix);

	var now = new Date();
	var currentTimeUTC = now.toUTCString();
	var cachedDateUTC = now.toUTCString();
	var timeZoneName = jstz.determine().name();
	var baseDate = new Date('January 1, 2001 12:00 am').toUTCString();

	queueJSON("adding new entry", "/home/addEntrySData?currentTime=" + currentTimeUTC
			+ "&userId=" + virtualUserId + "&text=" + $inputElement.val() + "&baseDate=" + baseDate
			+ "&timeZoneName=" + timeZoneName + "&defaultToNow=" + (true ? '1':'0') + "&"
			+ getCSRFPreventionURI("addEntryCSRF") + "&callback=?",
			function(entries) {
		if (checkData(entries, 'success', "Error adding entry")) {
			$inputElement.val('');
			if (entries[1] != null) {
				showAlert(entries[1]);
			}
			if (entries[2] != null)
				autocompleteWidget.update(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
			var addedEntry = entries[3];
			addTagsToList(addedEntry);
		}
	});
}

function addTagsToList(addedEntry) {
	$('#sprint-tag-list').append('<li><div class="' + addedEntry.comment + 'DarkLabelImage"></div> ' + addedEntry.description + (addedEntry.comment ?
			' (<i>' + addedEntry.comment.capitalizeFirstLetter() + '</i>)' : '') + ' <button type="button" class="deleteSprintEntry" data-id="' + 
			addedEntry.id + '" data-repeat-type="' + 
			addedEntry.repeatType + '"><i class="fa fa-times-circle"></i></button></li>');
}

function addParticipantsAndAdminsToList($element, deleteButtonClass, userName) {
	$element.append('<li>' + userName + 
			' (<i>invited</i>) <button type="button" class="' + deleteButtonClass + '" data-username="' + 
			userName + '"><i class="fa fa-times-circle"></i></button></li>');
}

function createSprint() {
	queueJSON('Creating sprint', '/data/createNewSprintData?' + getCSRFPreventionURI('createNewSprintDataCSRF') + '&callback=?', 
		function(data) {
			console.log('data: ', data);
			if (!data.error) {
				$('#sprintIdField').val(data.hash);
				$('#sprintVirtualUserId').val(data.virtualUserId);
				$('#sprintVirtualGroupId').val(data.virtualGroupId);
				$('#createSprintOverlay').modal({show: true});
			} else {
				showAlert("Unable to create new sprint!");
			}
			autocompleteWidget = new AutocompleteWidget('autocomplete1', 'sprint-tags');
		}, function(xhr) {
			console.log('error: ', xhr);
		}
	);
}

function editSprint(sprintHash) {
	queueJSON("Getting sprint data", "/data/fetchSprintData?" + getCSRFPreventionURI("fetchSprintDataCSRF") + "&callback=?",
			{id: sprintHash},
			function(data) {
				if (!checkData(data))
					return;

				console.log('data: ', data);
				if (data.error) {
					showAlert(data.message);
				} else {
					console.log(data.sprint);
					//Clearing data from last load
					clearSprintFormData();
					$('#sprintIdField').val(data.sprint.hash);
					$('#sprintVirtualUserId').val(data.sprint.virtualUserId);
					$('#sprintVirtualGroupId').val(data.sprint.virtualGroupId);
					$('#sprint-title').val(data.sprint.name);
					$('#sprint-duration').val(data.sprint.daysDuration);
					$('#sprint-details').val(data.sprint.description);
					$('.submit-sprint').text('Update Sprint');
					$('#createSprintOverlay .modal-title').text('Edit Sprint');
					if (data.sprint.visibility === 'PRIVATE') {
						$('#closed').prop('checked', true);
					} else {
						$('#open').prop('checked', true);
					}

					$.each(data.entries, function(index, value) {
						addTagsToList(value);
					});
					$.each(data.participants, function(index, participant) {
						if (!participant.virtual) {
							addParticipantsAndAdminsToList($("#sprint-participants-list"), 
									'deleteParticipants', participant.username);
						}
					});
					$.each(data.admins, function(index, admin) {
						if (!admin.virtual) {
							addParticipantsAndAdminsToList($("#sprint-admins-list"), 
									'deleteAdmins', admin.username);
						}
					});
					$('#createSprintOverlay').modal({show: true});
				}
				autocompleteWidget = new AutocompleteWidget('autocomplete1', 'sprint-tags');
			});
}

function deleteSprint(sprintHash) {
	showYesNo('Delete this sprint?', function() {
		queueJSON('Deleting sprint', '/data/deleteSprintData?id=' + sprintHash + 
				'&' + getCSRFPreventionURI('deleteSprintDataCSRF') + '&callback=?', 
			function(data) {
				if (!checkData(data))
					return;
	
				console.log('data: ', data);
				if (!data.success) {
					showAlert('Unable to delete sprint!');
				} else {
					location.assign('/home/social#all');
				}
			}, function(data) {
				showAlert(data.message);
			}
		);
	});
}

function startSprint(sprintHash) {
	var timeZoneName = jstz.determine().name();
	var now = new Date().toUTCString();
	queueJSON('Starting Sprint', '/data/startSprintData?' + getCSRFPreventionURI('startSprintDataCSRF') + '&callback=?', {
		id: sprintHash,
		now: now,
		timeZoneName: timeZoneName
	}, function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			$('#start-sprint').hide();
			$('#stop-sprint').show();
		} else {
			showAlert(data.message);
		}
	});
}

function stopSprint(sprintHash) {
	var timeZoneName = jstz.determine().name();
	var now = new Date().toUTCString();
	queueJSON('Stopping Sprint', '/data/stopSprintData?' + getCSRFPreventionURI('stopSprintDataCSRF') + '&callback=?', {
		id: sprintHash,
		now: now,
		timeZoneName: timeZoneName
	}, function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			$('#stop-sprint').hide();
			$('#start-sprint').show();
		} else {
			showAlert(data.message);
		}
	});
}

function leaveSprint(sprintHash) {
	var timeZoneName = jstz.determine().name();
	var now = new Date().toUTCString();
	location.assign('/home/leaveSprint?id=' + sprintHash + '&timeZoneName=' + timeZoneName + '&now=' + now);
}

function toggleCommentsList(discussionId) {
	var $element = $('#discussion' + discussionId + '-comment-list');
	if ($element.is(':visible')) {
		$element.hide();
		$('#discussion' + discussionId + '-comment-list .comments').html('');
	} else {
		getMoreComments(discussionId, 0);
		$element.show();
	}
}

function getMoreComments(discussionId, offset) {

	var url = "/home/discuss?discussionId=" + discussionId + "&offset=" + offset  + "&max=4&" +
			getCSRFPreventionURI("getCommentsCSRF") + "&callback=?";
	
	var discussionElementId = '#discussion' + discussionId + '-comment-list';

	queueJSON("fetching more comments", url, function(data) {
		if (!checkData(data))
			return;

		if (!data.posts) {
			$(discussionElementId + ' .bottom-margin').html('');
		} else {
			$(discussionElementId + ' .comments').append(data.posts);
			showCommentAgeFromDate();
			offset = offset + 4;
			$(discussionElementId + ' .bottom-margin span').off('click').on('click', function() {
				getMoreComments(discussionId, offset);
			});
		}
	});
	return;
}

function addComment(discussionHash) {
	var $inputElement = $('input#userComment');
	if (!discussionHash) {
		discussionHash = $('input#discussionId').val()
	}
	queuePostJSON('Adding comment', '/api/discussionPost',
		getCSRFPreventionObject('addCommentCSRF', {discussionHash: discussionHash, message: $inputElement.val()}),
		function(data) {
			if (data.success) {
				if (location.href.indexOf('/discussion/show') > -1) {
					location.reload();
				} else {
					location.href = '/discussion/show/' + discussionHash;
				}
			} else {
				showAlert(data.message);
			}	
	}, function(error) {
		showAlert('Internal server error occurred');
	});
}
