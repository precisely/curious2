<%! import grails.converters.JSON %>
<html>
<head>
<meta name="layout" content="social" />
<c:jsCSRFToken keys= "getSprintParticipantsDataCSRF, getSprintDiscussionsDataCSRF, createDiscussionDataCSRF, addMemberCSRF, 
addAdminCSRF, deleteMemberCSRF, deleteAdminCSRF, joinSprintDataCSRF, leaveSprintDataCSRF"/>
<script type="text/javascript">
$(document).ready(function() {
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

	$('#discussion-topic').keypress(function(e) {
		var key = e.which;
		if (key == 13) {
			$('.input-affordance hr').show();
			$('input[name = discussionPost]').show();
			return false;  
		}
	});

	$('#discussion-description').keypress(function (e) {
		var key = e.which;
		if (key == 13) {
			$('#create-discussion').submit(submitDiscussion());
			return false;  
		}
	});

	var discussionData = ${discussions.encodeAsRaw()};
	if (discussionData.listItems) {
		addAllFeedItems({listItems: discussionData.listItems.discussionList}, '#sprint-discussions');
	}
});
var offset = 10;

function showPreviousParticipants() {
	var leftPos = $('#participants-list ul').scrollLeft();                                             
	console.log('left: ', leftPos);
	$("#participants-list ul").scrollLeft(leftPos - 250);    
}

function showMoreParticipants(sprintHash, infiniteScroll) {
	if ((${sprintInstance.getParticipantsCount()} - offset) > 0) {
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


function submitDiscussion() {
	// See base.js for implementation details of $.serializeObject()
	var params = $('#create-discussion').serializeObject();
	queuePostJSON('Creating discussion', '/api/discussion', getCSRFPreventionObject('createDiscussionDataCSRF', params),
			function(data) {
		if (!checkData(data))
			return;
		if (data.success) {
			addAllFeedItems({listItems: [data.discussion]}, '#sprint-discussions', true);
			$('#create-discussion')[0].reset();
			$('input[name = discussionPost]').hide();
		}
	}, function(xhr) {
		console.log('Internal server error');
	});
	return false;
}
</script>
</head>
<body class="feed">
	<content tag="feedContent">
			<div class="sprint">
				<div class="row">
					<div class="col-xs-10 sprint-content">
						<div class="row">
							<div class="left-content">
								<span class="label label-default">DETAILS</span>
							</div>
							<div class="right-content">
								<h2>
									${sprintInstance.name}
								</h2>
								<p>
									${sprintInstance.description }
								</p>
							</div>
						</div>
						<hr>
						<div class="row">
							<div class="left-content">
								<span class="label label-default">TAGS</span>
							</div>
							<div class="right-content">
								<ul>
									<g:each in="${entries}" var="entry">
										<li class="sprintTag">
											${entry.description} <g:if test="${entry.comment}">(<i>${entry.comment}</i>)</g:if>
											<button type="button" class="deleteSprintEntry" data-id="${entry.id}" 
													data-repeat-type="${entry.repeatType}">
												<i class="fa fa-times-circle"></i>
											</button>
										</li>
									</g:each>
								</ul>
							</div>
						</div>
						<hr>
						<div class="row">
							<div class=" left-content">
								<span class="label label-default">DATA</span>
							</div>
							<div class="right-content">
								<p>TBD</p>
							</div>
						</div>
						<hr>
						<div class="row">
							<div class=" left-content">
								<span class="label-default label-participants">PARTICIPANTS</span>
							</div>
							<div class="right-content">
								<i class="nav fa fa-chevron-left fa-4x" onclick="showPreviousParticipants()"></i>
								<div class="participants-wrapper inline-block">
									<div id="participants-list" class="inline-block">
										<ul>
											<li>
												<ul>
													<g:each in="${participants}" var="participant">
														<li>
															<img src="/images/track-avatar.png" alt="avatar" class="participantsAvatar">
															<p>${participant.username}</p>
														</li>
													</g:each>
												</ul>
											</li>
										</ul>
									</div>
								</div>
								<i class="nav fa fa-chevron-right fa-4x pull-right" onclick="showMoreParticipants('${sprintInstance.hash}')"></i>
							</div>
						</div>
						<div class="row">
							<div class="right-content">
								<div class="new-post">
									<form id="create-discussion">
										<div class="input-affordance left-addon">
											<i class="fa fa-pencil"></i> 
											<input class="full-width discussion-topic-input"
													type="text" placeholder="New question or discussion topic?"
													name="name" id="discussion-topic" required />
											<input type="radio" class="radio-public" name="visibility" id="public" value="public" checked><label for="public" class="radio-public-label">Public</label>
											<input type="radio" class="radio-private" name="visibility" id="private" value="private"><label for="private" class="radio-private-label">Private</label>
											<hr class="hide">
											<input type="text" id="discussion-description" class="full-width discussion-topic-description hide" placeholder="Enter comment/description"
													name="discussionPost">
										</div>
										<input type="hidden" name="group" value="${virtualGroupName}" />
									</form>
								</div>
							</div>
						</div>
						<div class="row">
							<div class="right-content discussions-wrapper">
								<div id="sprint-discussions">
								</div>
							</div>
						</div>
					</div>
					<div class="col-xs-2">
						<g:if test="${sprintInstance.hasMember(user.id)}">
							<button id="leave-sprint" class="sprint-button" onclick="leaveSprint('${sprintInstance.hash }')">Unfollow</button>
							<g:if test="${sprintInstance.hasStarted(user.id, new Date()) && !sprintInstance.hasEnded(user.id, new Date())}">
								<button id="stop-sprint" class="sprint-button prompted-action" 
										onclick="stopSprint('${sprintInstance.hash }')">Stop</button>
								<button id="start-sprint" class="prompted-action sprint-button hidden"
										onclick="startSprint('${sprintInstance.hash }')">Start</button>
							</g:if>
							<g:else>
								<button id="start-sprint" class="prompted-action sprint-button"
										onclick="startSprint('${sprintInstance.hash }')">Start</button>
								<button id="stop-sprint" class="sprint-button prompted-action hidden"
										onclick="stopSprint('${sprintInstance.hash }')">Stop</button>
							</g:else>
						</g:if>
						<g:else>
							<button id="join-sprint" class="sprint-button" onclick="joinSprint('${sprintInstance.hash }')">Follow</button>
						</g:else>
						<g:if test="${sprintInstance.hasAdmin(user.id)}">
							<button class="sprint-button" onclick="editSprint('${sprintInstance.hash}')">Edit</button>
							<button class="sprint-button" onclick="deleteSprint('${sprintInstance.hash}')">Delete</button>
						</g:if>
					</div>
				</div>
			</div>
	</content>
</body>
</html>
