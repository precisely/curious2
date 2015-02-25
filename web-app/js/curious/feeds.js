/**
 * 
 */
var autocompleteWidget;
var searchList = [];
$(function() {
	queueJSON("getting login info", "/home/getPeopleData?callback=?",
			getCSRFPreventionObject("getPeopleDataCSRF"),
			function(data) {
				if (!checkData(data))
					return;
			
			var found = false;
			
			jQuery.each(data, function() {
				if (!found) {
					// set first user id as the current
					setUserId(this['id']);
					found = true;
				}
				addPerson(this['first'] + ' ' + this['last'],
						this['username'], this['id'], this['sex']);
				return true;
			});
			autocompleteWidget = new AutocompleteWidget('autocomplete1', 'sprint-tags');
		});
});

$(document).ready(function() {
	var App = window.App || {};
	App.discussion = {};
	App.discussion.lockInfiniteScroll = false;
	App.discussion.offset = 5;

	//var autocompleteWidget = new AutocompleteWidget('autocomplete', 'sprint-tags');

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
			$.ajax({
				url: '/home/discuss',
				data: {
					discussionId: discussionId,
					deleteDiscussion: true
				},
				success: function(data) {
					showAlert(JSON.parse(data).message, function() {
						$this.parents('.feed-item').fadeOut();
					});
				},
				error: function(xhr) {
					var data = JSON.parse(xhr.responseText);
					showAlert(data.message);
				}
			});
		});
		return false;
	});

	$(document).on("click", "ul#discussion-pagination a", function() {
		var url = $(this).attr('href');
		$.ajax({
			url: url,
			success: function(data) {
				$('div#discussions').html(data);
				wrapPagination();
			}
		});
		return false;
	});

	$('.main').scroll(function() {
		if (($("#getMoreDiscussions").length == 0) || ($(".feed-item").length == 0))
			return false;

		var $element = $('#getMoreDiscussions');
		var docViewTop = $(window).scrollTop();
		var docViewBottom = docViewTop + $(window).innerHeight();

		var elemTop = $element.offset().top;

		if ((elemTop < docViewBottom) && !App.discussion.lockInfiniteScroll) {
			$element.addClass(" waiting-icon");
			setTimeout(function() {
				var requestUrl = '/home/feed';
				if ($('#myThreads a').hasClass('active')) {
					requestUrl = '/dummy/getMyThreads';
				}
				$.ajax ({
					type: 'POST',
					url: requestUrl+'?offset=' + App.discussion.offset,
					success: function(data, textStatus) {
						if (data == "false") {
							$("#getMoreDiscussions").text('No more discussions to show.');
							setTimeout(function() {
								$("#getMoreDiscussions").fadeOut()
							}, 5000);
						} else {
							$('#discussions').append(data);
							showCommentAgeFromDate();
						}
						App.discussion.offset = App.discussion.offset + 5;
					}
				});
				$element.removeClass("waiting-icon");
			}, 600);
			App.discussion.lockInfiniteScroll = true;
		} else if (elemTop > docViewBottom) {
			App.discussion.lockInfiniteScroll = false;
			return;
		}
	});

	$('#submitSprint').submit(function( event ) {
		console.log('submitting');
		params  = $(this).serializeArray();
		$.ajax ({
			type: 'POST',
			url: '/data/createSprintData',
			data: params,
			success: function(data) {
				data = JSON.parse(data);
				console.log('data: ', data);
				if (data.error) {
					$('.modal-dialog .alert').text('Error occurred while submitting the form.').removeClass('hide');
					setInterval(function() {
						$('.modal-dialog .alert').addClass('hide');
					}, 5000);
				} else {
					location.assign('/home/sprint?id=' + data.id);
				}
			},
			error: function(xhr) {
				console.log('error: ', xhr);
			}
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
});

function deleteParticipantsOrAdmins($element, username, actionType) {
	var actionName = (actionType === 'participants') ? 'deleteSprintMemberData' : 'deleteSprintAdminData';
	$.ajax ({
		type: 'POST',
		url: '/data/' + actionName,
		data: {
			username: username,
			now: new Date().toUTCString(),
			sprintId: $('#sprintIdField').val(),
		},
		success: function(data) {
			data = JSON.parse(data);
			if (data.success) {
				$element.parents('li').remove();
			} else {
				$('.modal-dialog .alert').text(data.errorMessage).removeClass('hide');
				setInterval(function() {
					$('.modal-dialog .alert').addClass('hide');
				}, 5000);
			}
		},
		error: function(xhr) {
			console.log('error: ', xhr);
		}
	});
}

function createAutocomplete(inputId, autocompleteId) {
	$( "#" + inputId ).autocomplete({
		appendTo: "#" + autocompleteId,
		minLength: 0,
		source: []
	});

	$( "#" + inputId ).on( "keyup", function() {
		var searchString = $("#" + inputId).val();
		$.ajax ({
			type: 'GET',
			url: '/home/getAutocompleteParticipants',
			data: {
				searchString: searchString
			},
			success: function(data) {
				data = JSON.parse(data);
				console.log('data: ', data);
				if (data.success) {
					$( "#" + inputId ).autocomplete( "option", "source", data.usernameList);
				}
			},
			error: function(xhr) {
				console.log('error: ', xhr);
			}
		});
	});

	$("#" + inputId).keypress(function (e) {
		userName = $("#" + inputId).val();
		var actionName = (inputId === 'sprint-participants')?'addMemberToSprintData':'addAdminToSprintData';
		var deleteButtonClass = (inputId === 'sprint-participants')?'deleteParticipants':'deleteAdmins';
		var key = e.which;
		if (key == 13) { // the enter key code
			$.ajax ({
				type: 'POST',
				url: '/data/' + actionName,
				data: {
					username: userName,
					sprintId: $('#sprintIdField').val()
				},
				success: function(data) {
					data = JSON.parse(data);
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
				},
				error: function(xhr) {
					console.log('error: ', xhr);
				}
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

	queueJSON("deleting entry", "/home/deleteEntrySData?entryId=" + id
			+ "&currentTime=" + this.currentTimeUTC + "&baseDate=" + this.cachedDateUTC
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
	var now = new Date();
	this.cachedDateUTC = now.toUTCString();

	queueJSON("deleting entry", makeGetUrl("deleteGhostEntryData"), makeGetArgs(getCSRFPreventionObject("deleteGhostEntryDataCSRF", {entryId:entryId,
		all:(allFuture ? "true" : "false"), date:this.cachedDateUTC, baseDate:this.cachedDateUTC})),
		function(ret) {
				$tagToDelete.parents('li').remove();
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

	queueJSON("adding new entry", "/home/addEntrySData?currentTime=" + this.currentTimeUTC
			+ "&userId=" + virtualUserId + "&text=" + $inputElement.val() + "&baseDate=" + this.cachedDateUTC
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

function getMyThreads(params) {
	$.ajax ({
		type: 'POST',
		url: '/dummy/getMyThreads',
		success: function(data) {
			if (data) {
				$('.new-post').remove();
				$('#discussions').html(data);
			} else {
				console.log('no data', data);
			}
		},
		error: function(xhr) {
			console.log('error: ', xhr);
			
		}
	});
}

function createSprint() {
	$.ajax ({
		type: 'GET',
		url: '/data/createNewSprintData',
		success: function(data) {
			console.log('data: ', data);
			data = JSON.parse(data);
			if (!data.error) {
				$('#sprintIdField').val(data.id);
				$('#sprintVirtualUserId').val(data.virtualUserId);
				$('#sprintVirtualGroupId').val(data.virtualGroupId);
				$('#createSprintOverlay').modal({show: true});
			} else {
				showAlert("Unable to create new sprint!")
			}
		},
		error: function(xhr) {
			console.log('error: ', xhr);
		}
	});
}

function editSprint(sprintId) {
	queueJSON("getting login info", "/data/fetchSprintData?callback=?",
			{sprintId: sprintId},
			function(data) {
				console.log('data: ', data);
				if (data.error) {
					showAlert("No sprint found to edit!");
				} else {
					console.log(data.sprint);
					$('#sprintIdField').val(data.sprint.id);
					$('#sprintVirtualUserId').val(data.sprint.virtualUserId);
					$('#sprintVirtualGroupId').val(data.sprint.virtualGroupId);
					$('#sprint-title').val(data.sprint.name);
					$('#sprint-duration').val(data.sprint.daysDuration);
					$('#sprint-details').val(data.sprint.description);
					//$('#sprint-title').val(sprint.name);
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
			});
}

function deleteSprint(sprintId) {
	showYesNo('Delete this sprint?', function() {
		$.ajax ({
			type: 'GET',
			url: '/data/deleteSprintData?sprintId=' + sprintId,
			success: function(data) {
				console.log('data: ', data);
				data = JSON.parse(data);
				if (!data.success) {
					showAlert('Unable to delete sprint!');
				} else {
					location.assign('/home/feed');
				}
			},
			error: function(xhr) {
				console.log('error: ', xhr);
				showAlert('Unable to delete sprint!');
			}
		});
	});
}

function startSprint(sprintId) {
	backgroundPostJSON("Starting Sprint", "/data/startSprintData", {
		sprintId: sprintId,
		now: new Date().toUTCString()
	}, function(data) {
		if (data.success) {
			$('#start-sprint').removeClass('prompted-action').prop('disabled', true);
			$('#stop-sprint').addClass(' prompted-action').prop('disabled', false);
		} else {
			showAlert("Unable to start sprint!")
		}
	});
}

function stopSprint(sprintId) {
	backgroundPostJSON("Stopping Sprint", "/data/stopSprintData", {
		sprintId: sprintId,
		now: new Date().toUTCString()
	}, function(data) {
		if (data.success) {
			$('#stop-sprint').removeClass('prompted-action').prop('disabled', true);
			$('#start-sprint').addClass(' prompted-action').prop('disabled', false);
		} else {
			showAlert("Unable to stop sprint!")
		}
	});
}

function leaveSprint(sprintId) {
	backgroundPostJSON("Leaving Sprint", "/data/leaveSprintData", {
		sprintId: sprintId,
		now: (new Date()).toUTCString()
	}, function(data) {
		if (data.success) {
			location.reload();
		} else {
			showAlert("Unable to leave sprint!")
		}
	});
}

function joinSprint(sprintId) {
	backgroundPostJSON("Leaving Sprint", "/data/joinSprintData", {
		sprintId: sprintId
	}, function(data) {
		if (data.success) {
			location.reload();
		} else {
			showAlert("Unable to join sprint!")
		}
	});
}