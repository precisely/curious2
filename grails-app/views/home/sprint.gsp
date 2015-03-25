<%! import grails.converters.JSON %>
<html>
<head>
<meta name="layout" content="feedLayout" />
<script type="text/javascript">
$(document).ready(function() {
	$('#queryTitle').text('Tracking Sprint');
});
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
								<g:each in="${participants}" var="participant">
									<g:if test="${!participant.virtual}">
										<div class="inline-block">
											<img src="/images/track-avatar.png" alt="avatar" class="participantsAvatar">
											<p>${participant.username}</p>
										</div>
									</g:if>
								</g:each>
								<g:if test="${participants.size() >= 5}">
									<div class="inline-block">
										<img src="/images/moreParticipants.png" alt="avatar" id="moreAvatars" onclick="">
										<p>&nbsp;</p>
									</div>
								</g:if>
								</div>
						</div>
					</div>
					<div class="col-xs-2">
						<g:if test="${sprintInstance.hasMember(user.id)}">
							<button id="leave-sprint" class="sprint-button" onclick="leaveSprint(${sprintInstance.id })">Leave</button>
							<button id="start-sprint" 
								${(sprintInstance.hasStarted(user.id, new Date()) && !sprintInstance.hasEnded(user.id, new Date()))? 
									raw('class="sprint-button" disabled') : raw('class="prompted-action sprint-button"')}
								onclick="startSprint(${sprintInstance.id })">Start</button>
							<button id="stop-sprint" 
								${(sprintInstance.hasStarted(user.id, new Date()) && !sprintInstance.hasEnded(user.id, new Date()))? 
									raw('class="sprint-button prompted-action"'): raw('class="sprint-button" disabled')} 
								onclick="stopSprint(${sprintInstance.id })">Stop</button>
						</g:if>
						<g:else>
							<a href="/home/joinSprint?sprintId=${sprintInstance.id }">
								<button id="join-sprint" class="sprint-button">Join</button>
							</a>
						</g:else>
						<g:if test="${sprintInstance.hasAdmin(user.id)}">
							<button class="sprint-button" onclick="editSprint(${sprintInstance.id})">Edit</button>
							<button class="sprint-button" onclick="deleteSprint(${sprintInstance.id})">Delete</button>
						</g:if>
					</div>
				</div>
			</div>
	</content>
</body>
</html>
