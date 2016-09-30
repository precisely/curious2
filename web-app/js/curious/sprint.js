function showDiscussionData(sprintDiscussions, sprintHash) {
	$('#sprint-discussions').infiniteScroll({
		bufferPx: 25,
		offset: 5,
		bindTo: $('.discussions-wrapper'),
		onScrolledToBottom: function(e, $element) {
			this.pause();
			showMoreDiscussions(sprintHash, this);
		}
	});


	var discussionData = sprintDiscussions;
	if (discussionData.listItems) {
		addAllFeedItems({listItems: discussionData.listItems}, '#sprint-discussions');
	}
}
var offset = 10;

function showPreviousParticipants() {
	var leftPos = $('#participants-list ul').scrollLeft();
	$("#participants-list ul").scrollLeft(leftPos - 250);
}

function showMoreParticipants(sprintInstance, infiniteScroll, editFormType) {
	var participantsCount = sprintInstance.totalParticipants;
	var currentOffset = offset, methodName = "Participants";
	if (editFormType === 'editParticipants') {
		currentOffset = editParticipantsOffset;
	} else if (editFormType === 'editAdmins') {
		currentOffset = editAdminsOffset;
		methodName = "Admins";
	}
	if ((participantsCount - currentOffset) > 0) {
		queueJSON("Getting more participants", "/data/getSprint" + methodName + "Data?id=" + sprintInstance.hash
				+ "&offset=" + currentOffset + "&max=10&"
				+ getCSRFPreventionURI("getSprintParticipantsDataCSRF") + "&callback=?",
				function(data) {
			if (!checkData(data))
				return;

			if (data.success) {
				if (data.participants.length > 0) {
					if (editFormType === 'editParticipants') {
						addEditSprintParticipants(data, infiniteScroll);
					} else if (editFormType === 'editAdmins') {
						addEditSprintAdmins(data, infiniteScroll);
					} else {
						addSprintParticipants(data, infiniteScroll);
					}
					if (infiniteScroll) {
						infiniteScroll.resume();
					}
				} else {
					if (infiniteScroll) {
						infiniteScroll.stop();
					}
				}
			} else {
				showBootstrapAlert($('.alert'), data.message);
			}
		});
	} else if (!editFormType) {
		var leftPos = $('#participants-list ul').scrollLeft();
		$("#participants-list ul").scrollLeft(leftPos + 380);
	} else {
		if (infiniteScroll) {
			infiniteScroll.stop();
		}
	}
}

function addSprintParticipants(data, infiniteScroll) {
	$.each(data.participants, function(index, participant) {
		var avatarImage = participant.avatarURL ? '<img src="' + participant.avatarURL + '" alt="avatar" class="participantsAvatar">' :
			'<img src="/images/track-avatar.png" alt="avatar" class="participantsAvatar">';
		$('#participants-list ul li ul').append('<li>' + avatarImage + '<p>' + participant.username + '</p></li>');
	})
	offset += 10;
	var leftPos = $('#participants-list ul').scrollLeft();

	// Maximum four participants to be displayed at once, 180 is approx. with of div with two participants
	$("#participants-list ul").scrollLeft(leftPos + 250);
}

function showMoreDiscussions(sprintHash, infiniteScroll) {
	queueJSON("Getting more discussions", "/api/sprint/action/discussions?sprintHash=" + sprintHash
			+ "&offset=" + infiniteScroll.getOffset() + "&max=5&"
			+ getCSRFPreventionURI("getSprintDiscussionsDataCSRF") + "&callback=?",
			function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			if (data.listItems) {
				addAllFeedItems({listItems: data.listItems}, '#sprint-discussions');
				infiniteScroll.setNextPage();
				infiniteScroll.resume();
			} else {
				infiniteScroll.stop();
			}
		}
	}, function(error) {
		console.log(error);
	});
}

/**
 * Used to reset the participant list, when it gets updated.
 * @param sprintInstanceData
 */
function resetParticipantList(sprintInstanceData) {
	var htmlString = '<div id="participants-list" class="inline-block"><ul><li><ul>';
	$.each(sprintInstanceData.participants, function(index, participant) {
		htmlString += '<li>';
		var avatarUrl = participant.avatarURL ? participant.avatarURL : '/images/track-avatar.png';
		htmlString += '<img src="' + avatarUrl + '" alt="avatar" class="participantsAvatar img-circle"> ' +
			'<p>' + participant.username + '</p>';
		htmlString += '</li>';
	});
	htmlString += '</ul></li></ul></div>';

	var $participantList = $('#participants-list');
	$participantList.remove();
	$('.participants-wrapper').append(htmlString);
	$participantList.html(htmlString);

	console.log('$participantList',$('#participants-list ul>li>ul'));
	offset = 10;
	$participantList.infiniteScroll({
		bufferPx: 15,
		scrollHorizontally: true,
		offset: 10,
		bindTo: $('#participants-list ul'),
		onScrolledToBottom: function(e, $element) {
			this.pause();
			showMoreParticipants(sprintInstanceData, this);
		}
	});
}

function sprintShow(hash) {
	queueJSON('Getting sprint details', '/api/sprint/' + hash + '?' + getCSRFPreventionURI('showsprintCSRF') + '&callback=?',
			function(data) {
		if (data.success) {
				var sprintInstance = data.sprint;
				var entries = data.entries;
				for (var i in entries) {
					var entry = entries[i];
					entry.comment = _stripParens(entry.comment);
				}
				sprintInstance.entries = entries;
				sprintInstance.participants = data.participants;
				sprintInstance.description = _.linkify(sprintInstance.description);
				sprintInstance.lines = sprintInstance.description ? sprintInstance.description.split("\n") : [];
				var compiledHTML = compileTemplate("_showSprints", sprintInstance);
				$('#feed').html(compiledHTML);

				showDiscussionData(data.discussions, sprintInstance.hash);
				offset = 10;
				$('#participants-list ul>li>ul').infiniteScroll({
					bufferPx: 15,
					scrollHorizontally: true,
					offset: 10,
					bindTo: $('#participants-list ul'),
					onScrolledToBottom: function(e, $element) {
						this.pause();
						showMoreParticipants(sprintInstance, this);
					}
				});
				$('.nav-pills').hide();
		} else {
			showAlert(data.message);
			window.location.hash = 'sprints';
		}
		setQueryHeader('Trackathon', true);
	}, function(data) {
		showAlert('Internal server error occurred.');
	});
}

function startSprint(sprintHash) {
	var timeZoneName = jstz.determine().name();
	var now = new Date().toUTCString();
	queueJSON('Starting Sprint', '/api/sprint/action/start?' + getCSRFPreventionURI('startSprintDataCSRF') + '&callback=?', {
		id: sprintHash,
		now: now,
		timeZoneName: timeZoneName
	}, function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			$('#leave-sprint').show();
			$('#join-sprint').hide();
			$('#start-sprint').hide();
			$('#stop-sprint').show();
			if (getSprintElement(sprintHash).hasClass('not-following')) {
				resetParticipantList({hash: sprintHash, totalParticipants: data.totalParticipants, participants: data.participants});
				getSprintElement(sprintHash).addClass('following').removeClass('not-following');
			}
		} else {
			showAlert(data.message);
		}
	}, null, 0, false, {spinner: {selector: $("#start-sprint")}});
}

function stopSprint(sprintHash) {
	var timeZoneName = jstz.determine().name();
	var now = new Date().toUTCString();
	queueJSON('Stopping Sprint', '/api/sprint/action/stop?' + getCSRFPreventionURI('stopSprintDataCSRF') + '&callback=?', {
		id: sprintHash,
		now: now,
		timeZoneName: timeZoneName
	}, function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			$('#leave-sprint').show();
			$('#join-sprint').hide();
			$('#stop-sprint').hide();
			$('#start-sprint').show();
		} else {
			showAlert(data.message);
		}
	}, null, 0, false, {spinner: {selector: $("#stop-sprint")}});
}

function leaveSprint(sprintHash) {
	var timeZoneName = jstz.determine().name();
	var now = new Date().toUTCString();
	queueJSON('Unfollow Sprint', '/api/sprint/action/leave?' + getCSRFPreventionURI('leaveSprintDataCSRF') + '&callback=?', {
		id: sprintHash,
		now: now,
		timeZoneName: timeZoneName
	}, function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			$('#leave-sprint').hide();
			$('#join-sprint').show();
			$('#stop-sprint').hide();
			$('#start-sprint').show();
			getSprintElement(sprintHash).addClass('not-following').removeClass('following');
			resetParticipantList({hash: sprintHash, totalParticipants: data.totalParticipants, participants: data.participants});
		} else {
			showAlert(data.message);
		}
	});
}

function joinSprint(sprintHash) {
	queueJSON('Follow Sprint', '/api/sprint/action/join?' + getCSRFPreventionURI('joinSprintDataCSRF') + '&callback=?', {
		id: sprintHash
	}, function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			$('#leave-sprint').show();
			$('#join-sprint').hide();
			$('#stop-sprint').hide();
			$('#start-sprint').show();
			getSprintElement(sprintHash).removeClass('not-following').addClass('following');
			resetParticipantList({hash: sprintHash, totalParticipants: data.totalParticipants, participants: data.participants});
		} else {
			showAlert(data.message);
		}
	}, null, 0, false, {spinner: {selector: $("#join-sprint")}});
}

function getSprintElement(hash) {
	return $('#sprint-' + hash);
}

$(document).ready(function() {
	$(document).on("change", "#disable-sprint-comments", function() {
		var disabled = $(this).is(":checked");

		var params = {
			id: $('#sprintIdField').val(),
			disable: disabled
		};

		queueJSON('Saving preference', '/api/sprint/action/disableComments', params,
			function(data) {
				if (!checkData(data)) {
					return;
				}

			});
	});

});
