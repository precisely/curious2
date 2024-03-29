var autocompleteWidget, commentsArgs, ownedFeed = false, nextSuggestionOffset = 0, currentSprintsOffset = 0;
var maxCommentsPerDiscussion = 5;
var sprintListURL = '/home/sprint';
var sprintShowURL = sprintListURL + '#';

// See "controllerName" and "actionName" in the "base.gsp" layout
var isSocialGSP = (controllerName == "home" && actionName == "social");
var isSprintGSP = (controllerName == "home" && actionName == "sprint");
var isSearchGSP = (controllerName == "search" && actionName == "index");

function getSearchControllerURL(actionName, params) {
	//var params = getCSRFPreventionObject(csrfpreventionObjectName, params);
	var params = getCSRFPreventionObject('getFeedsDataCSRF', params);

	return '/search/' + actionName + '?' + jQuery.param(params) + '&callback=?';
}

function getSearchControllerURLSearch(actionName, params) {
	var params = getCSRFPreventionObject('getFeedsDataCSRF', params);
	params.q = $("#global-search input[name=q]").val();

	return '/search/' + actionName + '?' + jQuery.param(params) + '&callback=?';
}

function getURLSocialAll(offset, max) {
	return getSearchControllerURL(
			"getAllSocialData",
			{
				offset: offset,
				max: max,
				nextSuggestionOffset: nextSuggestionOffset  //global variable
			}
	)
}

function getURLSocialDiscussions(offset, max) {
	return getSearchControllerURL(
			"getDiscussionSocialData",
			{
				offset: offset,
				max: max
			}
	)
}

function getURLSocialNotifications(offset, max) {
	return getSearchControllerURL(
			"getSocialNotifications",
			{
				offset: offset,
				max: max
			}
		)
}

function getURLSocialPeople(offset, max) {
	return getSearchControllerURL(
			"getPeopleSocialData",
			{
				offset: offset,
				max: max
			}
		)
}

function getURLSocialOwned(offset, max) {
	return getSearchControllerURL(
			"getOwnedSocialData",
			{
				offset: offset,
				max: max
			}
	)
}

function getURLSprintsAll(offset, max) {
	currentSprintsOffset = offset;
	return getSearchControllerURL(
			"getAllSprintData",
			{
				offset: offset,
				max: max,
				nextSuggestionOffset: nextSuggestionOffset //global variable
			}
	)
}

function getURLSprintsOwned(offset, max) {
	currentSprintsOffset = offset;
	return getSearchControllerURL(
			"getOwnedSprintData",
			{
				offset: offset,
				max: max
			}
	)
}

function getURLSprintsStarted(offset, max) {
	currentSprintsOffset = offset;
	return getSearchControllerURL(
			"getStartedSprintData",
			{
				offset: offset,
				max: max
			}
		)
}

function getURLSearchAll(offset, max) {
	return getSearchControllerURLSearch(
			"searchAllData",
			{
				offset: offset,
				max: max
			}
	)
}

function getURLSearchDiscussions(offset, max) {
	return getSearchControllerURLSearch(
			"searchDiscussionData",
			{
				offset: offset,
				max: max
			}
	)
}

function getURLSearchSprints(offset, max) {
	return getSearchControllerURLSearch(
			"searchSprintData",
			{
				offset: offset,
				max: max
			}
		)
}

function getURLSearchPeople(offset, max) {
	return getSearchControllerURLSearch(
			"searchPeopleData",
			{
				offset: offset,
				max: max
			}
		)
}

function getURLSearchOwned(offset, max) {
	return getSearchControllerURLSearch(
			"searchAllOwnedData",
			{
				offset: offset,
				max: max
			}
		)
}

function isTabActive(anchor) {
	return location.hash == anchor;
}

function setFollowUser(userHash, follow) {
	queueJSON('change follow status', '/user/follow', makeGetArgs(getCSRFPreventionObject("followCSRF", {id:userHash, unfollow:(follow ? '' : 'true')})),
		function(data) {
			if (data.success) {
				console.log(data);
				var button = $('#follow-user-' + userHash);
				button.text(follow ? 'UNFOLLOW' : 'FOLLOW');
				button.toggleClass('follow', !follow);
				button.toggleClass('following', follow);
				button.attr("onclick", "setFollowUser('" + userHash + "', " + ((!follow) ? "true" : "false") + ")");
			} else {
				showAlert("Failed to follow user");
			}
		}
	);
}

function displayNoDataMessage(listItems) {
	$(".no-data-msg").remove();
	if (!listItems || listItems.length === 0) {
		$('#feed').append('<span class="no-data-msg">No data to display.</span>');
		return;
	}
}

function registerScroll(getURLMethod) {
	$('#feed').infiniteScroll({
		bufferPx: 20,
		bindTo: $('.main'),
		onScrolledToBottom: function(e, $element) {
			this.pause();
			var url = getURLMethod(this.getOffset(), 5);
			queueJSON('Loading data', url, function(data) {
				if (!checkData(data))
					return;

				if (data.success) {
					nextSuggestionOffset = data.nextSuggestionOffset;
					if (!data.listItems || data.listItems.length === 0) {
						this.finish();
					} else {
						addAllFeedItems(data);
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

function isHash(values) {
	var urlHashValue = location.hash.substring(1).trim();
	return (values.indexOf(urlHashValue) > -1);
}

function initializeListing() {
	$(".nav").show();
	$('.container-fluid').addClass("main");
	$(".nav a[href=" + window.location.hash + "]").tab("show");
}

function removeNewPostUI() {
	$(".new-post").remove();
}

function processResults(data) {
	var parentElement;

	$("#feed").html("");      // Remove spinner
	removeNewPostUI();

	if (window.location.hash !== "#people" && (window.location.hash === "#discussions" || isSocialGSP)) {
		/*
		 * Adding "Create discussion" form on every sub tab except the people tab of the social page and on the discussions tab in the
		 * social page and search results.
		 */
		var createDiscussionForm = compileTemplate("_createDiscussionForm", {groupName: data.groupName});
		$("#feed").before(createDiscussionForm);
	}

	if (!checkData(data)) {
		return;
	}

	if (data.success) {
		if (window.location.hash === "#notifications") {
			setNotificationBadge(0);
		}
		nextSuggestionOffset = data.nextSuggestionOffset;
		addAllFeedItems(data, parentElement);
	} else {
		if (data.message) {
			showAlert(data.message);
		}

		$("#feed").text("No feeds to display.");
	}
}

function displaySocialPage() {
	var hash = window.location.hash;
	if (hash == "#sprints") {
		// Backward support for old URL for list of sprints
		window.location.href = sprintListURL;
		return;
	} else if (hash.startsWith("#sprint/")) {
		// Backward support for old URL of sprint show page
		window.location.href = sprintShowURL + hash.split("/")[1];
		return;
	}

	if (!isHash(["all", "discussions", "people", "notifications", "owned"])) {
		displayDetail();
		return;
	}

	initializeListing();

	setQueryHeader("Social Activity", false);

	switch (hash) {
		case "#all":
			queueJSON("Getting feeds", getURLSocialAll(0, 5), processResults);
			registerScroll(getURLSocialAll);
			break;
		case "#discussions":
			queueJSON("Getting discussions", getURLSocialDiscussions(0, 5), processResults);
			registerScroll(getURLSocialDiscussions);
			break;
		case "#people":
			queueJSON("Getting people", getURLSocialPeople(0, 5), processResults);
			registerScroll(getURLSocialPeople);
			break;
		case "#notifications":
			queueJSON("Getting notifications", getURLSocialNotifications(0, 5), processResults);
			registerScroll(getURLSocialNotifications);
			break;
		case "#owned":
			queueJSON("Getting owned discussions", getURLSocialOwned(0, 5), processResults);
			registerScroll(getURLSocialOwned);
			break;
	}
}

function displaySprintPage() {
	if (!isHash(["all", "owned", "started"])) {
		displayDetail();
		return;
	}

	initializeListing();

	setQueryHeader("Trackathons", false);

	switch (window.location.hash) {
		case "#all":
			queueJSON("Getting trackathons feed", getURLSprintsAll(0, 5), processResults);
			registerScroll(getURLSprintsAll);
			break;
		case "#owned":
			queueJSON("Getting owned trackathons", getURLSprintsOwned(0, 5), processResults);
			registerScroll(getURLSprintsOwned);
			break;
		case "#started":
			queueJSON("Getting started trackathons", getURLSprintsStarted(0, 5), processResults);
			registerScroll(getURLSprintsStarted);
			break;
	}
}

function displaySearchPage() {
	if (!isHash(["all", "discussions", "sprints", "people", "owned"])) {
		displayDetail();
		return;
	}

	initializeListing();

	setQueryHeader('Search Results: ' + $("#global-search input[name=q]").val(), false);

	switch (window.location.hash) {
		case "#all":
			queueJSON("Getting search results", getURLSearchAll(0, 5), processResults)
			registerScroll(getURLSearchAll)
			break;
		case "#discussions":
			queueJSON("Getting search results", getURLSearchDiscussions(0, 5), processResults)
			registerScroll(getURLSearchDiscussions)
			break;
		case "#sprints":
			queueJSON("Getting search results", getURLSearchSprints(0, 5), processResults)
			registerScroll(getURLSearchSprints)
			break;
		case "#people":
			queueJSON("Getting search results", getURLSearchPeople(0, 5), processResults)
			registerScroll(getURLSearchPeople)
			break;
		case "#owned":
			queueJSON("Getting search results", getURLSearchOwned(0, 5), processResults)
			registerScroll(getURLSearchOwned)
			break;
	}
}

function displayDetail() {
	$("#feed").removeClass("feed-items");
	removeNewPostUI();
	$(".nav").hide();
	var hash = window.location.hash;

	if (isSocialGSP) {
		var domainHashValue = hash.split("/")[1];

		if (hash.startsWith("#discussions/")) {
			discussionShow(domainHashValue);
		} else if (hash.startsWith("#people/")) {
			showUserDetails(domainHashValue);
		}
	} else if (isSprintGSP) {
		sprintShow(hash.substring(1));               // Removing "#" from the beginning
	}
}

function checkAndDisplayTabData() {
	// Reset these variables as we change state/tab
	nextSuggestionOffset = 0;
	commentsArgs = {offset: 0, sort: "created", order: "desc", max: maxCommentsPerDiscussion};

	// Clear the main content and display a spinner
	$("#feed").html('<div class="text-center"><i class="fa fa-circle-o-notch fa-spin fa-3x"></i></div>');

	// Make sure to remove existing infinite scroll so that feeds can be reloaded based on the new selected tab and
	// the search filter.
	$('#feed').infiniteScroll('stop');

	$("#feed").addClass("feed-items");

	// If no "hash" is specified or hash is empty
	if (!window.location.hash) {
		window.location.hash = "#all";
		return;
	}

	if (isSocialGSP) {
		displaySocialPage()
	} else if (isSprintGSP) {
		displaySprintPage()
	} else if (isSearchGSP) {
		displaySearchPage()
	}

	$(window).scrollTop(0);

	// This block is used to alert the status of sharing to twitter, immediately after authorization.
	var queryParams = getSearchParams();
	if (queryParams.tweetStatus == 'true' && queryParams.duplicateTweet == 'false') {
		showAlert('Successfully posted discussion to Twitter.', removeQueryString);
	} else if (queryParams.tweetStatus == 'true' && queryParams.duplicateTweet == 'true') {
		showAlert('You have already tweeted this discussion.', removeQueryString);
	} else if (queryParams.tweetStatus == 'false') {
		showAlert('Sorry but we could not tweet your discussion.', removeQueryString);
	}
	// Positioning the dialog to the approximate center of the screen.
	$('.ui-dialog').css({"position": "absolute", "top": "40%"})
}

$(window).load(checkAndDisplayTabData).on('hashchange', checkAndDisplayTabData);

function showDiscussionAffordance(type) {
	var placeholder;
	if (type === 'howto') {
		placeholder  = 'Enter text of your how-to article';
	} else if (type === 'support') {
		placeholder = 'Ask a support question of the community?';
	} else {
		placeholder = 'New question or discussion topic?'
	}
	$('#create-discussion').show('fast').find('input').prop('placeholder', placeholder).data('type', type);
	$('.discussion-topic-input').focus();
}

$(document).ready(function() {
	$('#sprint-tags').keypress(function (e) {
		var key = e.which;

		if (key == 13) { // the enter key code
			addEntryToSprint('sprint-tags', 'bookmark');
			autocompleteWidget.close();
			return false;
		}
	});

	$('#submitSprint').submit(function() {
		var $form = $(this);
		// See base.js for implementation details of $.serializeObject()
		var params = $form.serializeObject();
		var id = $('#sprintIdField').val();
		var args = {requestMethod: 'PUT', spinner: {selector: $form, withMask: true}};

		queueJSONAll('Updating sprint', '/api/sprint/' + id + '?' + getCSRFPreventionURI('updateSprintDataCSRF'), params,
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
		}, null, args);
		return false;
	});

	createAutocomplete('sprint-participants', 'participantsAutocomplete');
	createAutocomplete('sprint-admins', 'adminsAutocomplete');

	$(document).on("click", ".deleteSprintEntry", function() {
		var $element = $(this);
		var repeatType = $(this).data('repeatType');
		var id = $(this).data('id');
		console.log('repeat type: ', repeatType);
		deleteEntry($element, id, true);
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
		var offset = $('#discussion-' + discussionHash).data("offset") || maxCommentsPerDiscussion;
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
			var questionType = $(this).data('type');
			data.name = questionType ? (data.name + ' #' + questionType) : data.name;

			// See base.js for implementation details of $.serializeObject()
			var params = $('#create-discussion').serializeObject();
			params.name = data.name;
			params.discussionPost = data.post;

			queuePostJSON('Creating discussion', '/api/discussion', getCSRFPreventionObject('createDiscussionDataCSRF', params),
					function(data) {
				if (!checkData(data))
					return;
				if (data.success) {
					var element = $(".discussions").length !== 0 ? ".discussions" : "#feed";
					addAllFeedItems({listItems: [data.discussion]}, element, true);
					$('#create-discussion').hide('fast');
					var $discussionForm = $('#create-discussion')[0];
					if ($discussionForm) {
						$discussionForm.reset();
					}
				} else {
					showAlert(data.message);
				}
			}, function(xhr) {
				console.log('Internal server error', xhr);
				if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
					showAlert(xhr.responseJSON.message);
				}
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
			if (!checkData(data)) {
				return;
			}

			if (data.success) {
				console.log('added people: ', data);
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
	if (discussionName.length > 100) {
		discussionName = shorten(discussionName, 100).trim();		// See base.js for "shorten" method
		// And the rest of the string (if any) will be used as first discussion comment message,
		// reducing 3 characters as trailing ellipsis appended at the end of the post title
		discussionPost = value.substring(discussionName.length - 3).trim();
	} else {
		discussionPost = value.substring(discussionName.length).trim();
	}

	return {name: discussionName, post: discussionPost};
}

function addAllFeedItems(data, elementId, prepend) {
	displayNoDataMessage(data.listItems);

	elementId = elementId || '#feed';

	if ((location.pathname.indexOf('sprint') > -1) && isHash(["all", "owned", "started"])) {
		if (!closedExplanationCardTrackathon) {
			$('#sprint-explanation-card').remove();
			showExplanationCardTrackathon();
		}
	}

	$.each(data.listItems, function(index, item) {
		var compiledHTML = '';

		if (item.type == 'spr') {
			compiledHTML = compileTemplate("_sprints", {'sprint': item});
		} else if (item.type == 'dis') {
			compiledHTML = compileTemplate("_discussions", {'discussionData': item, serverURL: serverURL});
		} else if (item.type == 'usr') {
			if (item.name) {
				item.nameInfo = item.username + ' (' + item.name + ')';
			} else
				item.nameInfo = item.username
			item.followButtonText = item.followed ? 'UNFOLLOW' : 'FOLLOW';
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
	var autocompleteParticipantsList = [];
	$('#' + inputId).autocomplete({
		appendTo: '#' + autocompleteId,
		minLength: 0,
		source: function(request, response) {
			if (response) {
				response($.map(autocompleteParticipantsList, function(data) {
					return {label: data.label, value: '"'+ data.value + '"' };
				}));
			}
		}
	});

	$('#' + inputId).on('keyup', function() {
		var searchString = $('#' + inputId).val();
		queueJSON('Getting autocomplete', '/data/getAutocompleteParticipantsData?' + getCSRFPreventionURI("getAutocompleteParticipantsDataCSRF") + "&callback=?",
				{searchString: searchString, max: 10},
				function(data) {
			if (!checkData(data)) {
				return;
			}

			if (data.success) {
				autocompleteParticipantsList = data.displayNames;
				$('#' + inputId).autocomplete('source');
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
	userName = userName.replaceAll('"', '');
	var actionName = (inputId === 'sprint-participants') ? 'addMember' : 'addAdmin';
	var deleteButtonClass = (inputId === 'sprint-participants') ? 'deleteParticipants' : 'deleteAdmins';
	queuePostJSON('Adding members', '/api/sprint/action/' + actionName, getCSRFPreventionObject(actionName + 'CSRF',
			{username: userName, sprintHash: $('#sprintIdField').val()}),
			function(data) {
		if (!checkData(data)) {
			return;
		}

		if (data.success) {
			console.log('added people: ', data);
			$("#" + inputId).val('');
			addParticipantsAndAdminsToList($("#" + inputId + "-list"), deleteButtonClass, userName);
		} else {
			showBootstrapAlert($('.modal-dialog .alert'), data.errorMessage);
		}
	}, function(xhr) {
		console.log('error: ', xhr);
	});
}

function addEditSprintParticipants(data, infiniteScroll) {
	$.each(data.participants, function(index, participant) {
		addParticipantsAndAdminsToList($("#sprint-participants-list"),
			'deleteParticipants', participant.username);
	});
	editParticipantsOffset += 10;
}

function addEditSprintAdmins(data, infiniteScroll) {
	$.each(data.participants, function(index, participant) {
		addParticipantsAndAdminsToList($("#sprint-admins-list"),
			'deleteAdmins', participant.username);
	});
	editAdminsOffset += 10;
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

function deleteEntry($tagToDelete, entryId, allFuture) {
	var baseDate = new Date('January 1, 2001 12:00 am').toUTCString();
	var args = {
		entryId: entryId,
		currentTime: new Date().toUTCString(),
		baseDate: baseDate,
		timeZoneName: jstz.determine().name(),
		displayDate: baseDate
	};
	queueJSON("deleting entry", makeGetUrl("deleteEntrySData"), makeGetArgs(getCSRFPreventionObject("deleteEntryDataCSRF", args)),
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
	var repeatTypeId;
	if (suffix == 'repeat') {
		repeatTypeId = RepeatType.REPEAT_BIT;
	} else if (suffix == 'remind') {
		repeatTypeId = RepeatType.REMIND_BIT;
	} else {
		repeatTypeId = RepeatType.CONTINUOUS_BIT;
	}

	var now = new Date();
	var currentTimeUTC = now.toUTCString();
	var cachedDateUTC = now.toUTCString();
	var timeZoneName = jstz.determine().name();
	var baseDate = new Date('January 1, 2001 12:00 am').toUTCString();

	queueJSON("adding new entry", "/home/addEntrySData?currentTime=" + currentTimeUTC
			+ "&userId=" + virtualUserId + "&text=" + $inputElement.val() + "&baseDate=" + baseDate
			+ "&timeZoneName=" + timeZoneName + "&defaultToNow=" + (true ? '1':'0') + "&repeatTypeId=" + repeatTypeId + "&"
			+ getCSRFPreventionURI("addEntryCSRF") + "&callback=?",
			function(entries) {
				if (checkData(entries, 'success', "Error adding entry")) {
					$inputElement.val('');
					if (entries[1] != null) {
						showAlert(entries[1]);
					}
					if (entries[2] != null) {
						autocompleteWidget.update(entries[2][0], entries[2][1], entries[2][2], entries[2][3]);
					}
					var addedEntry = entries[3];
					addTagsToList(addedEntry);
				}
			});
}

function addTagsToList(addedEntry) {
	if (RepeatType.isRemind(addedEntry.repeatType)) {
		addedEntry.comment = 'remind';
	} else if (RepeatType.isRepeat(addedEntry.repeatType)) {
		addedEntry.comment = 'repeat';
	} else if (RepeatType.isContinuous(addedEntry.repeatType)) {
		addedEntry.comment = 'bookmark';
	}
	$('#sprint-tag-list').append('<li><div class="' + addedEntry.comment + 'DarkLabelImage"></div> ' + escapehtml(addedEntry.description )+ (addedEntry.comment ?
			' (<i>' + _stripParens(addedEntry.comment.capitalizeFirstLetter()) + '</i>)' : '') + ' <button type="button" class="deleteSprintEntry" data-id="' +
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
		}, 0, {spinner: {selector: $('.create-new-sprint'), withMask: true}}
	);
}

var editParticipantsOffset = 10;
var editAdminsOffset = 10;

function editSprint(sprintHash) {
	editParticipantsOffset = 10;
	editAdminsOffset = 10;
	queueJSON("Getting sprint data", '/api/sprint/' + sprintHash + '?' + getCSRFPreventionURI("fetchSprintDataCSRF") + "&callback=?",
			null, function (data) {
		if (!checkData(data))
			return;

		if (!data.success) {
			showAlert(data.message);
		} else {
			//Clearing data from last load
			var sprintInstance = data.sprint;
			clearSprintFormData();
			$('#sprintIdField').val(data.sprint.hash);
			$('#sprintVirtualUserId').val(data.sprint.virtualUserId);
			$('#sprintVirtualGroupId').val(data.sprint.virtualGroupId);
			$('#sprint-title').val(data.sprint.name);
			$('#sprint-duration').val(data.sprint.daysDuration);
			$('#sprint-details').val(data.sprint.description);
			$('.submit-sprint').text('Update Sprint');
			$('#createSprintOverlay .modal-title').text('Edit Sprint');
			$('#disable-sprint-comments').prop('checked', data.sprint.disableComments);

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
			$('#sprint-participants-list').infiniteScroll({
				bufferPx: 5,
				scrollHorizontally: false,
				offset: 10,
				bindTo: $('#sprint-participants-list'),
				onScrolledToBottom: function(e, $element) {
					this.pause();
					showMoreParticipants(sprintInstance, this, "editParticipants");
				}
			});
			$('#sprint-admins-list').infiniteScroll({
				bufferPx: 5,
				scrollHorizontally: false,
				offset: 10,
				bindTo: $('#sprint-admins-list'),
				onScrolledToBottom: function(e, $element) {
					this.pause();
					showMoreParticipants(sprintInstance, this, "editAdmins");
				}
			});
			$('#createSprintOverlay').modal({show: true});
		}
		autocompleteWidget = new AutocompleteWidget('autocomplete1', 'sprint-tags');
	}, null, 0, false, {spinner: {selector: $("#edit-sprint")}});
}

function deleteSprint(sprintHash) {
	var httpArgs ={requestMethod:'delete'};
	showYesNo('Delete this trackathon?', function() {
		queueJSONAll('Deleting sprint', '/api/sprint/' + sprintHash,
				getCSRFPreventionObject('deleteSprintDataCSRF'),
				function(data) {
			if (!checkData(data)) {
				return;
			}

			console.log('data: ', data);
			if (!data.success) {
				showAlert('Unable to delete trackathon!');
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
		$.autoResize.init();
	}
}

function showUserDetails(hash) {
	queueJSON('Getting user details', '/api/user/' + hash + '?' + getCSRFPreventionURI('getUserDataCSRF') + '&callback=?',
			function(data) {
		if (data.success) {
			data.user.followButtonText = data.user.followed ? 'UNFOLLOW' : 'FOLLOW';
			var compiledHTML = compileTemplate("_peopleDetails", {'user': data.user});
			$('#feed').html(compiledHTML);
			setQueryHeader('User Profile', true);
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

function followDiscussion(args) {
	var httpArgs = {requestMethod: 'GET'};
	queueJSONAll('Following discussion', '/api/discussion/action/follow?callback=?', getCSRFPreventionObject('followDiscussionCSRF', args),
	function(data) {
		if (checkData(data)) {
			if (data.success) {
				if (args.unfollow) {
					$('#follow-button-' + args.id).attr("onclick", "followDiscussion({id: '" + args.id + "'})").html('<img src="/images/follow.png" alt="follow">Follow');
				} else {
					$('#follow-button-' + args.id).attr("onclick", "followDiscussion({id: '" + args.id + "', unfollow: true})").html('<img src="/images/unfollow.png" alt="unfollow">Unfollow');
				}
			} else {
				showAlert(data.message);
			}
		}
	}, function(error) {
		console.log('error: ', error);
	}, null, httpArgs);
};
