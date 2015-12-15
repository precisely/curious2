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
		$(".share-button").popover({html:true});
		$('.share-button').on('click', function () {
			$('.share-link').select();
		});
	}
}
var offset = 10;

function showPreviousParticipants() {
	var leftPos = $('#participants-list ul').scrollLeft();                                             
	$("#participants-list ul").scrollLeft(leftPos - 250); 
}

function showMoreParticipants(sprintInstance, infiniteScroll) {
	var participantsCount = sprintInstance.totalParticipants;
	if ((participantsCount - offset) > 0) {
		queueJSON("Getting more participants", "/data/getSprintParticipantsData?id=" + sprintInstance.hash
				+ "&offset=" + offset + "&max=10&"
				+ getCSRFPreventionURI("getSprintParticipantsDataCSRF") + "&callback=?",
				function(data) {
			if (!checkData(data))
				return;

			if (data.success) {
				if (data.participants.length > 0) {
					$.each(data.participants, function(index, participant) {
						var avatarImage = participant.avatarURL ? '<img src="' + participant.avatarURL + '" alt="avatar" class="participantsAvatar">' : 
								'<img src="/images/track-avatar.png" alt="avatar" class="participantsAvatar">';
						$('#participants-list ul li ul').append('<li>' + avatarImage + '<p>' + participant.username + '</p></li>');
					})
					offset += 10;
					var leftPos = $('#participants-list ul').scrollLeft();

					// Maximum four participants to be displayed at once, 180 is approx. with of div with two participants
					$("#participants-list ul").scrollLeft(leftPos + 250);
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
	} else {
		var leftPos = $('#participants-list ul').scrollLeft();
		$("#participants-list ul").scrollLeft(leftPos + 380);
	}
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
				var compiledHTML = compileTemplate("_showSprints", sprintInstance);
				$('#feed').html(compiledHTML);
				showDiscussionData(data.discussions, sprintInstance.hash);
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
		setQueryHeader('Tracking Sprint', true);
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
		} else {
			showAlert(data.message);
		}
	});
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
	});
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
			$('#stop-sprint').show();
			$('#start-sprint').hide();
			getSprintElement(sprintHash).removeClass('not-following').addClass('following');
		} else {
			showAlert(data.message);
		}
	});
}

function getSprintElement(hash) {
	return $('#sprint-' + hash);
}