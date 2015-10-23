var autocompleteWidget, commentsArgs;
var maxCommentsPerDiscussion = 4;		// Comments to display in the discussion listing page (feeds page) at once
var sprintListURL = '/home/sprint';
var sprintShowURL = sprintListURL + '#';

/**
 * A copy of types defined in the server side code. See "SearchService.groovy"
 */
var FEED_TYPE = {
	ALL: -1,
	DISCUSSION: 1,
	SPRINT: 2,
	USER: 8
};

function getFeedURL(type, offset, max) {
	var params = getCSRFPreventionObject('getFeedsDataCSRF', {type: type, max: max, offset: offset});
	return '/search/indexData?' + jQuery.param(params) + '&callback=?';
}

function isTabActive(anchor) {
	return location.hash == anchor;
}

/**
 * A generic method to render a list of feed items like "sprints", "discussions", "people".
 * @param listItems List of feed items
 * @param template Name of the template to render
 * @param key Main key name to pass as for each feed item
 */
function renderFeedItems(listItems, template, key) {
	$.each(listItems, function(index, item) {
		var args = {};
		args[key] = item;

		$('#feed').append(compileTemplate(template, args));
	});

	showCommentAgeFromDate();
}
function renderUsers(listItems) {
	renderFeedItems(listItems, "_people", 'user');
}
function renderDiscussions(listItems) {
	renderFeedItems(listItems, "_discussions", 'discussionData');
}
function renderSprints(listItems) {
	renderFeedItems(listItems, "_sprints", 'sprint');
}

function registerScroll(feedType) {
	$('#feed').infiniteScroll({
		bufferPx: 20,
		bindTo: $('.main'),
		onScrolledToBottom: function(e, $element) {
			this.pause();
			var url = getFeedURL(feedType, this.getOffset(), 5);

			queueJSON('Loading data', url, function(data) {
				if (!checkData(data))
					return;

				if (data.success) {
					if (!data.listItems) {
						this.finish();
					} else {
						if (isTabActive('#people')) {
							renderUsers(data.listItems);
						} else if (isTabActive('#discussions')) {
							renderDiscussions(data.listItems);
						} else if (isTabActive('#all')) {
							addAllFeedItems(data);
						} else {
							renderSprints(data.listItems);
						}
						this.setNextPage();
						this.resume();
					}
				} else {
					this.finish();
					if (data.message) {
						$('.alert').text(data.message);
					}
				}
			}.bind(this), function(data) {
				showAlert('Internal server error occurred.');
			});
		}
	});
}

function checkAndDisplayTabData() {
	// Reset these variables as we change state
	window.singleDiscussionPage = false;

	commentsArgs = {offset: 0, sort: "created", order: "desc"};

	$('.container-fluid').addClass('main');
	var hash = window.location.hash;
	var hashData = hash.split("/");

	if (actionName === "sprint") {                               // See "actionName" in the "base.gsp" layout
		if (!hash) {
			showSprints();                                      // For sprint listing page
		} else {                                                // For single sprint page
			sprintShow(hashData[0].substring(1));               // Removing "#" from the beginning
		}
	} else if (actionName === "social") {
		if (hash == "#sprints") {
			window.location.href = sprintListURL;               // Backward support for old URL for list of sprints
		} else if (hash == '#discussions') {
			showDiscussions();
		} else if (hash == '#people') {
			showPeople();
		} else if (!hash || hash == '#all') {
			// If the all tab is active or the user is on the social page without a hash
			showAllFeeds();
		} else if (hash == "#sprints/" + hashData[1]) {
			window.location.href = sprintShowURL + hashData[1];   // Backward support for old URL of sprint show page
		} else if (hash == "#discussions/" + hashData[1]) {
			window.singleDiscussionPage = true;
			discussionShow(hashData[1]);
		} else if (hash == "#people/" + hashData[1]) {
			showUserDetails(hashData[1]);
		}
	}

	$(window).scrollTop(0);
}

$(window).load(checkAndDisplayTabData).on('hashchange', checkAndDisplayTabData);

$(document).ready(function() {
	$('html').on('click', function(e) {
		if (typeof $(e.target).data('original-title') == 'undefined' && !$(e.target).is('.share-button img')) {
			$('[data-original-title]').popover('hide');
		}
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
		var id = $('#sprintIdField').val();
		var httpArgs = { requestMethod: 'PUT' };
		queueJSONAll('Updating sprint', '/api/sprint/' + id + '?' + getCSRFPreventionURI('updateSprintDataCSRF'), JSON.stringify(params),
				function(data) {
			if (!checkData(data))
				return;

			if (!data.success) {
				$('.modal-dialog .alert').text('Error occurred while submitting the form.').removeClass('hide');
				setInterval(function() {
					$('.modal-dialog .alert').addClass('hide');
				}, 5000);
			} else {
				if (isTabActive('#' + data.hash)) {
					sprintShow(data.hash);
				} else {
					location.assign(sprintShowURL + data.hash);
				}
				clearSprintFormData()
				$('#createSprintOverlay').modal('hide');
			}
		}, function(xhr) {
		}, null, httpArgs);
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

	/**
	 * Click handler for event when user clicks on the "VIEW MORE COMMENTS" in the listing of discussion and their
	 * comments.
	 */
	$(document).on("click", ".discussion .view-comment", function() {
		var discussionHash = $(this).data("discussionHash");
		var offset = $('#discussion-' + discussionHash).data("offset") || 4;
		commentsArgs.offset = offset;

		getComments(discussionHash, commentsArgs, function() {
			$('#discussion-' + discussionHash).data("offset", offset + maxCommentsPerDiscussion);
		}.bind(this));
	});

	// Handlers for discussion form input fields
	$(document).on('keypress', '#discussion-topic', function(e) { 
		var key = e.which;
		if (key == 13) {
			var value = $(this).val();
			if (!value) {
				return false;
			}

			var data = extractDiscussionNameAndPost(value);

			// See base.js for implementation details of $.serializeObject()
			var params = $('#create-discussion').serializeObject();
			params.name = data.name
			params.discussionPost = data.post;

			queuePostJSON('Creating discussion', '/api/discussion', getCSRFPreventionObject('createDiscussionDataCSRF', params),
					function(data) {
				if (!checkData(data))
					return;
				if (data.success) {
					addAllFeedItems({listItems: [data.discussion]}, '.discussions', true);
					$('#create-discussion')[0].reset();
				}
			}, function(xhr) {
				console.log('Internal server error');
			});

			return false;
		}
	});

	$('#close-sprint-modal').click(function() {
		$('#createSprintOverlay').modal('hide').data('bs.modal', null);
		clearSprintFormData();
		queuePostJSON('Canceling sprint edit', '/data/cancelSprintEdit', getCSRFPreventionObject('cancelSprintEditCSRF', 
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
		return false;
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

function extractDiscussionNameAndPost(value) {
	var discussionName, discussionPost;

	// Try to get the first sentence i.e. a line ending with either "." "?" or "!"
	var firstSentenceData = /^.*?[\.!\?](?:\s|$)/.exec(value);

	if (firstSentenceData) {
		discussionName = firstSentenceData[0].trim();
	} else {	// If user has not used any of the above punctuations
		discussionName = value;
	}

	// Trim the entered text max upto the 100 characters and use it as the discussion name/title
	discussionName = shorten(discussionName, 100).trim();		// See base.js for "shorten" method
	// And the rest of the string (if any) will be used as first discussion comment message
	discussionPost = value.substring(discussionName.length).trim();

	return {name: discussionName, post: discussionPost};
}

function showSprints() {
	queueJSON('Getting sprint list', getFeedURL(FEED_TYPE.SPRINT, 0, 5), function(data) {
		if (data.success) {
			// Adding custom classes according to the tabs, so as to be able to modify the elements differently in respective tabs if required
			$('#feed').removeClass().addClass('type-sprints').html('');
			renderSprints(data.listItems);
		} else {
			if (data.message) {
				showAlert(data.message);
			}
			$('#feed').html('No sprints to show.');
		}

		setQueryHeader('Tracking Sprints', false);
		$('#feed-sprints-tab a').tab('show');
		$('.nav').show();
	}, function(data) {
		showAlert('Internal server error occurred.');
	});

	registerScroll(FEED_TYPE.SPRINT);
}

function showDiscussions() {
	// Change the red bar title
	setQueryHeader('Discussions', false);

	$('.nav').show();
	
	// Select the tab
	$('#feed-discussions-tab a').tab('show');

	// Clear the tab content and display a spinner
	$('#feed').html('<div class="text-center"><i class="fa fa-circle-o-notch fa-spin fa-3x"></i></div>');

	$('#feed-right-tab').html('');

	// Get the discussion data
	queueJSON('Getting discussion data', getFeedURL(FEED_TYPE.DISCUSSION, 0, 5), function(data) {
		var createDiscussionForm = compileTemplate("_createDiscussionForm", {groupName: data.groupName});
		$('#feed').removeClass().addClass('type-discussions')
				.html(createDiscussionForm)		// Reset the main container with create discussion form
				.append('<div class="discussions"></div>')	// And append one container for holding all discussions

		if (!checkData(data)) {
			return;
		}

		if (!data.success) {
			if (data.message) {
				showAlert(data.message);
			}
			$('#feed').append('No discussions to show.');
			return;
		}

		renderDiscussions(data.listItems);
	}, function(data) {
		showAlert('Internal server error occurred.');
	});

	registerScroll(FEED_TYPE.DISCUSSION);
}

function showPeople() {
	queueJSON('Getting people list', getFeedURL(FEED_TYPE.USER, 0, 5), function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			$('#feed').removeClass().addClass('type-people').html('');
			$('.nav').show();
			renderUsers(data.listItems);
			$('#feed-right-tab').html('');
		} else {
			if (data.message) {
				showAlert(data.message);
			}
			$('#feed').text('No people to show.');
		}
		setQueryHeader('People', false);
		$('#feed-people-tab a').tab('show');
	}, function(data) {
		showAlert('Internal server error occurred.');
	});

	registerScroll(FEED_TYPE.USER);
}

function showAllFeeds() {
	queueJSON('Getting feeds', getFeedURL(FEED_TYPE.ALL, 0, 5), function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			$('#feed').removeClass().addClass('type-all').html('');
			addAllFeedItems(data);
			$('#feed-right-tab').html('');
		} else {
			if (data.message) {
				showAlert(data.message);
			}
			$('#feed').text('No feeds to show.');
		}
		setQueryHeader('All Feeds', false);
		$('#feed-all-tab a').tab('show');
		$('.nav').show();
	}, function(data) {
		showAlert('Internal server error occurred.');
	});

	registerScroll(FEED_TYPE.ALL);
}

function addAllFeedItems(data, elementId, prepend) {
	elementId = elementId || '#feed';
	data.listItems.sort(function(a, b) {
		return a.updated > b.updated ? -1 : (a.updated < b.updated ? 1 : 0)
	});

	$.each(data.listItems, function(index, item) {
		var compiledHTML = '';

		if (item.type == 'spr') {
			compiledHTML = compileTemplate("_sprints", {'sprint': item});
		} else if (item.type == 'dis') {
			compiledHTML = compileTemplate("_discussions", {'discussionData': item});
		} else if (item.type == 'usr') {
			compiledHTML = compileTemplate("_people", {'user': item});
		}

		if (prepend) {
			$(elementId).hide().prepend(compiledHTML).fadeIn('slow');
		} else {
			$(elementId).append(compiledHTML);
		}
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
	var actionName = (actionType === 'participants') ? 'deleteMember' : 'deleteAdmin';

	queuePostJSON('Removing members', '/api/sprint/action/' + actionName, getCSRFPreventionObject(actionName + 'CSRF', 
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
				{searchString: searchString, max: 10},
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
	var actionName = (inputId === 'sprint-participants') ? 'addMember' : 'addAdmin';
	var deleteButtonClass = (inputId === 'sprint-participants') ? 'deleteParticipants' : 'deleteAdmins';
	queuePostJSON('Adding members', '/api/sprint/action/' + actionName, getCSRFPreventionObject(actionName + 'CSRF', 
			{username: userName, sprintHash: $('#sprintIdField').val()}),
			function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			console.log('added persons: ', data);
			$("#" + inputId).val('');
			addParticipantsAndAdminsToList($("#" + inputId + "-list"), deleteButtonClass, userName);
		} else {
			showBootstrapAlert($('.modal-dialog .alert'), data.errorMessage);
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

$(document).on('click', '.create-new-sprint', function() {
	createSprint();
	return false;
});

function createSprint() {
	queuePostJSON('Creating sprint', '/api/sprint', getCSRFPreventionObject('createNewSprintDataCSRF'), 
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
	queueJSON("Getting sprint data", '/api/sprint/' + sprintHash + '?' + getCSRFPreventionURI("fetchSprintDataCSRF") + "&callback=?",
			function(data) {
		if (!checkData(data))
			return;

		console.log('data: ', data);
		if (!data.success) {
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
	var httpArgs ={requestMethod:'delete'};
	showYesNo('Delete this sprint?', function() {
		queueJSONAll('Deleting sprint', '/api/sprint/' + sprintHash, 
				getCSRFPreventionObject('deleteSprintDataCSRF'), 
			function(data) {
				if (!checkData(data))
					return;
	
				console.log('data: ', data);
				if (!data.success) {
					showAlert('Unable to delete sprint!');
				} else {
					if (!window.history.back()) {
						location.href = sprintListURL;
					}
				}
			}, function(data) {
				showAlert(data.message);
			}, null, httpArgs);
	});
}

function toggleCommentsList(discussionHash) {
	var $element = $('.discussion-comments-wrapper', '#discussion-' + discussionHash);

	if ($element.is(':visible')) {
		$element.hide();
		$('.comments', $element).html('');
	} else {
		commentsArgs.offset = 0;
		commentsArgs.max = maxCommentsPerDiscussion;

		getComments(discussionHash, commentsArgs);
		$('.discussion .view-comment').show();
		$element.show();
	}
}

function showUserDetails(hash) {
	queueJSON('Getting user details', '/api/user/' + hash + '?' + getCSRFPreventionURI('getUserDataCSRF') + '&callback=?',
			function(data) { 
		if (data.success) { 
			var compiledHTML = compileTemplate("_peopleDetails", {'user': data.user});
			$('#feed').html(compiledHTML);
			$('.nav').hide();
			setQueryHeader('User Profile', true);
			$('#feed-sprints-tab a').tab('show');
		} else {
			showAlert(data.message);
			window.history.back();
		}
	}, function(data) {
		showAlert('Internal server error occurred.');
	});
}

function setQueryHeader(text, setGobackButton) {
	if ($('#go-back-arrow').length > 0) {
		$('#go-back-arrow').remove();
	}
	if (setGobackButton) {
		$('#queryTitle').parent().prepend('<img alt="back" id="go-back-arrow" class="date-left-arrow" src="/images/left-arrow-white.png" onclick="window.history.back()" style="cursor: pointer; margin-right: 15px;">');
	} 
	$('#queryTitle').text(text);
}

function getDiscussionElement(hash) {
	return $('#discussion-' + hash);
}
