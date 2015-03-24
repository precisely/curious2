/**
 * 
 */
var autocompleteWidget;
var searchList = [];

// This function returns url parameters as key value pair
function getUrlVars() {
	var vars = {};
	var parts = window.location.href.replace(/[?&]+([^=&]+)=([^&]*)/gi,
		function(m,key,value) {
			vars[key] = value;
		}
	);
	return vars;
}

$(document).ready(function() {
	var App = window.App || {};
	App.discussion = {};
	App.discussion.lockInfiniteScroll = false;
	App.discussion.offset = 5;

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
	
	$(document).on("click", "a.delete-discussion", function() {
		var $this = $(this);
		showYesNo('Are you sure want to delete this?', function() {
			var discussionId = $this.data('discussionId');
			queueJSON('Deleting Discussion', '/data/deleteDiscussionData?' + getCSRFPreventionURI('deleteDiscussionDataCSRF') + '&callback=?',
					{discussionId: discussionId, deleteDiscussion: true}, 
					function(data) {
				if (data.success) {
					showAlert(data.message, function() {
						$this.parents('.feed-item').fadeOut();
					});
				} else {
					showAlert(data.message);
				}
			}, function(xhr) {
				console.log('error:', xhr)
				showAlert('Internal server error occurred.');
			});
		});
		return false;
	});

	$('#sprint-tags').keypress(function (e) {
		var key = e.which;
		if (key == 13) { // the enter key code
			return false;
		}
	});

	$('.main').scroll(function() {
		if (($("#getMoreDiscussions").length == 0) || ($(".feed-item").length == 0))
			return false;

		var $element = $('#getMoreDiscussions');
		var docViewTop = $(window).scrollTop();
		var docViewBottom = docViewTop + $(window).innerHeight();

		var elemTop = $element.offset().top;

		if ((elemTop < docViewBottom) && !App.discussion.lockInfiniteScroll) {
			$element.addClass(' waiting-icon');
			setTimeout(function() {
				var requestUrl = '/home/feed';
				var parameters = '?offset=' + App.discussion.offset;
				if (getUrlVars()['userId']) {
					parameters += '&userId=' + getUrlVars()['userId'];
				}
				queueJSON('Getting discussion data', requestUrl + parameters,
						function(data) {
					if (data == false) {
						$('#getMoreDiscussions').text('No more discussions to show.');
						setTimeout(function() {
							$('#getMoreDiscussions').fadeOut()
						}, 5000);
					} else {
						$('#discussions').append(data.feeds);
						showCommentAgeFromDate();
					}
					App.discussion.offset = App.discussion.offset + 5;
				}, function(data) {
					console.log('error: ', data);
					showAlert('Internal server error occurred.');
				});
				$element.removeClass('waiting-icon');
			}, 600);
			App.discussion.lockInfiniteScroll = true;
		} else if (elemTop > docViewBottom) {
			App.discussion.lockInfiniteScroll = false;
			return;
		}
	});

	$('#submitSprint').submit(function(event) {
		// See base.js for implementation details of $.serializeObject()
		var params = $(this).serializeObject();
		queuePostJSON('Updating sprint', '/data/updateSprintData', getCSRFPreventionObject('updateSprintDataCSRF', params),
				function(data) {
			if (data.error) {
				$('.modal-dialog .alert').text('Error occurred while submitting the form.').removeClass('hide');
				setInterval(function() {
					$('.modal-dialog .alert').addClass('hide');
				}, 5000);
			} else {
				location.assign('/home/sprint?id=' + data.id);
				clearSprintFormData()
			}
		}, function(xhr) {
			console.log('error: ', xhr);
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
	});

	$(document).on("click", ".deleteAdmins", function() {
		var $element = $(this);
		var username = $(this).data('username');
		deleteParticipantsOrAdmins($element, username, 'admins');
	});

	$('#close-sprint-modal').click(function() {
		$('#createSprintOverlay').modal('hide').data('bs.modal', null);
		clearSprintFormData();
	});
});

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
}

function deleteParticipantsOrAdmins($element, username, actionType) {
	var actionName = (actionType === 'participants') ? 'deleteSprintMemberData' : 'deleteSprintAdminData';

	queuePostJSON('Removing members', '/data/' + actionName, getCSRFPreventionObject(actionName + 'CSRF', 
			{username: username, now: new Date().toUTCString(), sprintId: $('#sprintIdField').val(), 
			timeZoneName: jstz.determine().name()}), 
			function(data) {
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
	$("#" + inputId).autocomplete({
		appendTo: "#" + autocompleteId,
		minLength: 0,
		source: []
	});

	$("#" + inputId).on("keyup", function() {
		var searchString = $("#" + inputId).val();
		queueJSON('Getting autocomplete', '/data/getAutocompleteParticipantsData?' + getCSRFPreventionURI("getAutocompleteParticipantsDataCSRF") + "&callback=?", 
				{searchString: searchString},
				function(data) {
			if (data.success) {
				$("#" + inputId).autocomplete("option", "source", data.usernameList);
			}
		}, function(xhr) {
			console.log('error: ', xhr);
		});
	});

	$("#" + inputId).keypress(function (e) {
		userName = $("#" + inputId).val();
		var actionName = (inputId === 'sprint-participants')?'addMemberToSprintData':'addAdminToSprintData';
		var deleteButtonClass = (inputId === 'sprint-participants')?'deleteParticipants':'deleteAdmins';
		var key = e.which;
		if (key == 13) { // the enter key code
			queuePostJSON('Adding members', '/data/' + actionName, getCSRFPreventionObject(actionName + 'CSRF', 
					{username: userName, sprintId: $('#sprintIdField').val()}),
					function(data) {
				if (data.success) {
					console.log('added persons: ', data);
					$("#" + inputId).val('');
					addParticipantsAndAdminsToList($("#" + inputId + "-list"), deleteButtonClass, userName);
				} else {
					$('.modal-dialog .alert').text(data.errorMessage).removeClass('hide');
					setInterval(function() {
						$('.modal-dialog .alert').addClass('hide');
					}, 5000);
				}
			}, function(xhr) {
				console.log('error: ', xhr);
			});
			return false;  
		}
	});
}
$(document).on("click", ".left-menu ul li a", function() {
	$('.left-menu ul li .active').removeClass('active');
	$(this).addClass('active');
});

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
				$('.modal-dialog .alert').text(data.message).show();
				setInterval(function() {
					$('.modal-dialog .alert').hide();
				}, 5000);
			} else {
				$('.alert').text(data.message).show();
				setInterval(function() {
					$('.alert').hide();
				}, 5000);
			}
		}
	});
}

function deleteGhost($tagToDelete, entryId, allFuture) {
	this.baseDate = new Date('January 1, 2001 12:00 am').toUTCString();

	queueJSON("deleting entry", makeGetUrl("deleteGhostEntryData"), makeGetArgs(getCSRFPreventionObject("deleteGhostEntryDataCSRF", {entryId:entryId,
		all:(allFuture ? "true" : "false"), date: this.baseDate, baseDate: this.baseDate})),
		function(response) {
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
	this.currentTimeUTC = now.toUTCString();
	this.cachedDateUTC = now.toUTCString();
	this.timeZoneName = jstz.determine().name();
	this.baseDate = new Date('January 1, 2001 12:00 am').toUTCString();

	queueJSON("adding new entry", "/home/addEntrySData?currentTime=" + this.currentTimeUTC
			+ "&userId=" + virtualUserId + "&text=" + $inputElement.val() + "&baseDate=" + this.baseDate
			+ "&timeZoneName=" + this.timeZoneName + "&defaultToNow=" + (true ? '1':'0') + "&"
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
	if (addedEntry.comment === 'pinned') {
		$('#sprint-tag-list').append('<li><div class="pinnedDarkLabelImage"></div> ' + addedEntry.description + 
				' (<i>Pinned</i>) <button type="button" class="deleteSprintEntry" data-id="' + addedEntry.id + '" data-repeat-type="' + 
				addedEntry.repeatType + '"> <i class="fa fa-times-circle"></i></button></li>');
	} else if (addedEntry.comment === 'remind') {
		$('#sprint-tag-list').append('<li><div class="remindDarkLabelImage"></div> ' + addedEntry.description + 
				' (<i>Remind</i>) <button type="button" class="deleteSprintEntry" data-id="' + addedEntry.id + '" data-repeat-type="' + 
				addedEntry.repeatType + '"><i class="fa fa-times-circle"></i></button></li>');
	} else if (addedEntry.comment === 'repeat') {
		$('#sprint-tag-list').append('<li><div class="repeatDarkLabelImage"></div> ' + addedEntry.description + 
				' (<i>Repeat</i>) <button type="button" class="deleteSprintEntry" data-id="' + addedEntry.id + '" data-repeat-type="' + 
				addedEntry.repeatType + '"><i class="fa fa-times-circle"></i></button></li>');
	}
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
				$('#sprintIdField').val(data.id);
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

function editSprint(sprintId) {
	queueJSON("Getting sprint data", "/data/fetchSprintData?" + getCSRFPreventionURI("fetchSprintDataCSRF") + "&callback=?",
			{sprintId: sprintId},
			function(data) {
				console.log('data: ', data);
				if (data.error) {
					showAlert(data.message);
				} else {
					console.log(data.sprint);
					//Clearing data from last load
					clearSprintFormData();
					$('#sprintIdField').val(data.sprint.id);
					$('#sprintVirtualUserId').val(data.sprint.virtualUserId);
					$('#sprintVirtualGroupId').val(data.sprint.virtualGroupId);
					$('#sprint-title').val(data.sprint.name);
					$('#sprint-duration').val(data.sprint.daysDuration);
					$('#sprint-details').val(data.sprint.description);
					$('.submit-sprint').text('Update Sprint');
					if (data.sprint.visibility === 'PRIVATE') {
						$('#closed').prop('checked', true);
					} else {
						$('#open').prop('checked', true);
					}

					$.each(data.tags, function(index, value) {
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

function deleteSprint(sprintId) {
	showYesNo('Delete this sprint?', function() {
		queueJSON('Deleting sprint', '/data/deleteSprintData?sprintId=' + sprintId + 
				'&' + getCSRFPreventionURI('deleteSprintDataCSRF') + '&callback=?', 
			function(data) {
				console.log('data: ', data);
				if (!data.success) {
					showAlert('Unable to delete sprint!');
				} else {
					location.assign('/home/feed');
				}
			}, function() {
				showAlert(data.message);
			}
		);
	});
}

function startSprint(sprintId) {
	var timeZoneName = jstz.determine().name();
	var now = new Date().toUTCString();
	queueJSON('Starting Sprint', '/data/startSprintData?' + getCSRFPreventionURI('startSprintDataCSRF') + '&callback=?', {
		sprintId: sprintId,
		now: now,
		timeZoneName: timeZoneName
	}, function(data) {
		if (data.success) {
			$('#start-sprint').removeClass('prompted-action').prop('disabled', true);
			$('#stop-sprint').addClass(' prompted-action').prop('disabled', false);
		} else {
			showAlert(data.message);
		}
	});
}

function stopSprint(sprintId) {
	var timeZoneName = jstz.determine().name();
	var now = new Date().toUTCString();
	queueJSON('Stopping Sprint', '/data/stopSprintData?' + getCSRFPreventionURI('stopSprintDataCSRF') + '&callback=?', {
		sprintId: sprintId,
		now: now,
		timeZoneName: timeZoneName
	}, function(data) {
		if (data.success) {
			$('#stop-sprint').removeClass('prompted-action').prop('disabled', true);
			$('#start-sprint').addClass(' prompted-action').prop('disabled', false);
		} else {
			showAlert(data.message);
		}
	});
}

function leaveSprint(sprintId) {
	var timeZoneName = jstz.determine().name();
	var now = new Date().toUTCString();
	location.assign('/home/leaveSprint?sprintId=' + sprintId + '&timeZoneName=' + timeZoneName + '&now=' + now);
}
