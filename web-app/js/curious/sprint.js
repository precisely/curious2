function showDiscussionData(sprintDiscussions) {
	$('#queryTitle').text('Tracking Sprint');

	$('#sprint-discussions').infiniteScroll({
		bufferPx: 25,
		offset: 5,
		bindTo: $('.discussions-wrapper'),
		onScrolledToBottom: function(e, $element) {
			this.pause();
			showMoreDiscussions('${sprintInstance.hash}', this);
		}
	});

	$('#participants-list ul>li>ul').infiniteScroll({
		bufferPx: 15,
		scrollHorizontally: true,
		offset: 10,
		bindTo: $('#participants-list ul'),
		onScrolledToBottom: function(e, $element) {
			this.pause();
			showMoreParticipants('${sprintInstance.hash}', this);
		}
	});

	var discussionData = sprintDiscussions;
	if (discussionData.listItems) {
		addAllFeedItems({listItems: discussionData.listItems.discussionList}, '#sprint-discussions');
		$(".share-button").popover({html:true});
		$('.share-button').on('click', function () {
			$('.share-link').select();
		});
	}
}
var offset = 10;

function showPreviousParticipants() {
	var leftPos = $('#participants-list ul').scrollLeft();                                             
	console.log('left: ', leftPos);
	$("#participants-list ul").scrollLeft(leftPos - 250);    
}

function showMoreParticipants(sprintHash, infiniteScroll) {
	var participantsCount = $(this).data(sprintInstance.getParticipantsCount());
	if ((participantsCount - offset) > 0) {
		queueJSON("Getting more participants", "/data/getSprintParticipantsData?id=" + sprintHash
				+ "&offset=" + offset + "&max=10&"
				+ getCSRFPreventionURI("getSprintParticipantsDataCSRF") + "&callback=?",
				function(data) {
			if (!checkData(data))
				return;

			if (data.success) {
				if (data.participants.length > 0) {
					$.each(data.participants, function(index, participant) {
						$('#participants-list ul li ul').append('<li>' + 
								'<img src="/images/track-avatar.png" alt="avatar" class="participantsAvatar">' + 
								'<p>' + participant.username + '</p></li>');
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
	queueJSON("Getting more discussoins", "/api/sprint/action/discussions?sprintHash=" + sprintHash
			+ "&offset=" + infiniteScroll.getOffset() + "&max=5&"
			+ getCSRFPreventionURI("getSprintDiscussionsDataCSRF") + "&callback=?",
			function(data) {
		if (!checkData(data))
			return;

		if (data.success) {
			if (data.listItems) {
				addAllFeedItems({listItems: data.listItems.discussionList}, '#sprint-discussions');
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

	queueJSON('Getting discussion list', '/api/sprint/' + hash + '?' + getCSRFPreventionURI('showsprintCSRF') + '&callback=?',
			function(data) { 
		if (data.success) { 
				sprintInstance = data.sprint;
				sprintInstance.entries = data.entries;
				sprintInstance.participants = data.participants;
				var compiledHTML = compileTemplate("_showSprints", sprintInstance);
				$('#feed').html(compiledHTML);
				showDiscussionData(data.discussions);
				window.location.hash = 'sprints/' + hash;
		} else {
			$('.alert').text(data.message);
		}
		$('#queryTitle').text('Tracking Sprint');
	}, function(data) {
		showAlert('Internal server error occurred.');
	});
}