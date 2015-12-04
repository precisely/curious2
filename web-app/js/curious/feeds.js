var autocompleteWidget, commentsArgs, ownedFeed = false, nextSuggestionOffset = 0;
var maxCommentsPerDiscussion = 4;		// Comments to display in the discussion listing page (feeds page) at once
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
	return getSearchControllerURL(
			"getOwnedSprintData",
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

function processResults(data) {
	var parentElement;

	if (window.location.hash === "#discussions") {
		//special handling for discussions social page
		var createDiscussionForm = compileTemplate("_createDiscussionForm", {groupName: data.groupName});
		$("#feed").html(createDiscussionForm).append('<div class="discussions"></div>');
		parentElement = ".discussions";
	} else {
		$("#feed").html("");      // Remove spinner
	}
	
	if (!checkData(data)) {
		return;
	}

	if (data.success) {
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
	
	if (!isHash(["all", "discussions", "people", "owned"])) {
		displayDetail();
		return;
	}
	
	initializeListing();
	
	setQueryHeader("Social Activity", false);
	
	switch (hash) {
	case "#all":
		queueJSON("Getting feeds", getURLSocialAll(0, 5), processResults)
		registerScroll(getURLSocialAll);
		break;
	case "#discussions":
		queueJSON("Getting discussions", getURLSocialDiscussions(0, 5), processResults)
		registerScroll(getURLSocialDiscussions);
		break;
	case "#people":
		queueJSON("Getting people", getURLSocialPeople(0, 5), processResults)
		registerScroll(getURLSocialPeople);
		break;
	case "#owned":
		queueJSON("Getting owned discussions", getURLSocialOwned(0, 5), processResults)
		registerScroll(getURLSocialOwned);
		break;
	}
}

function displaySprintPage() {
	if (!isHash(["all", "owned"])) {
		displayDetail();
		return;
	}
	
	initializeListing();
		
	setQueryHeader("Tracking Sprints", false);
	
	switch (window.location.hash) {
	case "#all":
		queueJSON("Getting sprints feed", getURLSprintsAll(0, 5), processResults)
		registerScroll(getURLSprintsAll);
		break;
	case "#owned":
		queueJSON("Getting owned sprints", getURLSprintsOwned(0, 5), processResults)
		registerScroll(getURLSprintsOwned);
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
	$(".nav").hide();
	var hash = window.location.hash;

	if (isSocialGSP) {
		var domainHashValue = hash.split("/")[1];

		if (hash.startsWith("#discussions/")) {
			window.singleDiscussionPage = true;
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
	window.singleDiscussionPage = false;
	nextSuggestionOffset = 0;
	commentsArgs = {offset: 0, sort: "created", order: "desc"};	
	
	// Clear the main content and display a spinner
	$("#feed").html('<div class="text-center"><i class="fa fa-circle-o-notch fa-spin fa-3x"></i></div>');

	// Make sure to remove existing infinite scroll so that feeds can be reloaded based on the new selected tab and
	// the search filter.
	$('#feed').infiniteScroll('stop');
		
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

function addAllFeedItems(data, elementId, prepend) {
	displayNoDataMessage(data.listItems);

	elementId = elementId || '#feed';

	$.each(data.listItems, function(index, item) {
		var compiledHTML = '';

		if (item.type == 'spr') {
			compiledHTML = compileTemplate("_sprints", {'sprint': item});
		} else if (item.type == 'dis') {
			compiledHTML = compileTemplate("_discussions", {'discussionData': item});
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