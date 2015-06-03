<%! import grails.converters.JSON %>
<html>
<head>
<meta name="layout" content="feed" />
<c:jsCSRFToken keys= "getSprintParticipantsDataCSRF"/>
<script type="text/javascript">
$(document).ready(function() {
	$('#queryTitle').text('Tracking Sprint');

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
</script>
</head>
<body>
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
											${entry.description } 
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
							<a href="/home/joinSprint/${sprintInstance.hash }">
								<button id="join-sprint" class="sprint-button">Follow</button>
							</a>
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
