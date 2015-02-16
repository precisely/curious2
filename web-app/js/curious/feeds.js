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
			$('.input-affordance hr').removeClass('hide');
			$('input[name = discussionPost]').removeClass('hide');
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
			url: '/dummy/createSprint',
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
					location.assign('/dummy/sprint/id?=' + data.id);
				}
			},
			error: function(xhr) {
				console.log('error: ', xhr);
			}
		});
	
		return false;
	});

	handelAutocomplete('sprint-participants', 'participantsAutocomplete');
	handelAutocomplete('sprint-admins', 'adminsAutocomplete');

	$(document).on("click", "#deleteTag", function() {
		var $element = $(this);
		var repeatType = $(this).data('repeatType');
		var id = $(this).data('id');
		console.log('repeat type: ', repeatType);
		if (RepeatType.isGhost(repeatType) || RepeatType.isContinuous(repeatType) || RepeatType.isTimed(repeatType) ||
				RepeatType.isRepeat(repeatType) || RepeatType.isRemind(repeatType)) {
			deleteGhost($element, id, true);
		} else {
			deleteSimpleEntry(id, $element);
		}
		return false;
	});

	$(document).on("click", "#deleteParticipants", function() {
		var $element = $(this);
		var username = $(this).data('username');
		deleteParticipantsOrAdmins($element, username, 'participants');
	});

	$(document).on("click", "#deleteAdmins", function() {
		var $element = $(this);
		var username = $(this).data('username');
		deleteParticipantsOrAdmins($element, username, 'admins');
	});
});

function deleteParticipantsOrAdmins($element, username, actionType) {
	var actionName = (actionType === 'participants')?'deleteSprintParticipant':'deleteSprintAdmin';
	$.ajax ({
		type: 'POST',
		url: '/dummy/' + actionName,
		data: {
			username: username,
			virtualGroupId: $('#sprintVirtualGroupId').val(),
			error: false
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

function handelAutocomplete(inputId, autocompleteId) {
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
				if (data.success) {
				} else {
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
		var actionName = (inputId === 'sprint-participants')?'addSprintParticipants':'addSprintAdmins';
		var deleteButtonId = (inputId === 'sprint-participants')?'deleteParticipants':'deleteAdmins';
		var key = e.which;
		if (key == 13) { // the enter key code
			$.ajax ({
				type: 'POST',
				url: '/dummy/' + actionName,
				data: {
					participantUsername: userName,
					virtualGroupId: $('#sprintVirtualGroupId').val(),
					error: false
				},
				success: function(data) {
					data = JSON.parse(data);
					if (data.success) {
						$("#" + inputId).val('');
						$("#" + inputId + "-list").append('<li>' + userName + 
								' (<i>invited</i>) <button type="button" id="' + deleteButtonId + '" data-username="' + 
								userName + '"><i class="fa fa-times-circle"></i></button></li>');
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

function addData(usernameList) {
	searchList = usernameList;
}

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

function createSprintTags(inputElement, suffix) {
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
			if (addedEntry.comment === 'pinned') {
				$('#sprint-tag-list').append('<li><div class="pinnedDarkLabelImage"></div> ' + addedEntry.description + 
						' (<i>Pinned</i>) <button type="button" id="deleteTag" data-id="' + addedEntry.id + '" data-repeat-type="' + 
						addedEntry.repeatType + '"> <i class="fa fa-times-circle"></i></button></li>');
			} else if (addedEntry.comment === 'remind') {
				$('#sprint-tag-list').append('<li><div class="remindDarkLabelImage"></div> ' + addedEntry.description + 
						' (<i>Remind</i>) <button type="button" id="deleteTag" data-id="' + addedEntry.id + '" data-repeat-type="' + 
						addedEntry.repeatType + '"><i class="fa fa-times-circle"></i></button></li>');
			} else if (addedEntry.comment === 'repeat') {
				$('#sprint-tag-list').append('<li><div class="repeatDarkLabelImage"></div> ' + addedEntry.description + 
						' (<i>Repeat</i>) <button type="button" id="deleteTag" data-id="' + addedEntry.id + '" data-repeat-type="' + 
						addedEntry.repeatType + '"><i class="fa fa-times-circle"></i></button></li>');
			}
		}
	});
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

function createBlankSprint() {
	$.ajax ({
		type: 'GET',
		url: '/dummy/createNewBlankSprint',
		success: function(data) {
			data = JSON.parse(data);
			if (data.success) {
				$('.new-post').remove();
				$('#sprintIdField').val(data.id);
				$('#sprintVirtualUserId').val(data.userId);
				$('#sprintVirtualGroupId').val(data.groupId);
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