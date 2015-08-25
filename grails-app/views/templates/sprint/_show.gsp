<div class="feed">
	<div class="sprint">
		<div class="row">
			<div class="col-xs-10 sprint-content">
				<div class="row">
					<div class="left-content">
						<span class="label label-default">DETAILS</span>
					</div>
					<div class="right-content">
						<h2>
							{{- sprintInstance.name }}
						</h2>
						<p>
							{{- sprintInstance.description }}
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
							{{ _.each (sprintInstance.entries, function(entry) { }}
								<li class="sprintTag">
									{{- entry.description }} {{ if (entry.comment) { }}(<i>{{- entry.comment }}</i>){{ } }}
									<button type="button" class="deleteSprintEntry" data-id="{{- entry.id }}" 
											data-repeat-type="{{- entry.repeatType }}">
										<i class="fa fa-times-circle"></i>
									</button>
								</li>
							{{ }) }}
						</ul>
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
											{{ _.each(sprintInstance.participants, function(participant) { }}
												<li>
													<img src="/images/track-avatar.png" alt="avatar" class="participantsAvatar">
													<p>{{- participant.username }}</p>
												</li>
											{{ }) }}
										</ul>
									</li>
								</ul>
							</div>
						</div>
						<i class="nav fa fa-chevron-right fa-4x pull-right" onclick="showMoreParticipants('{{- sprintInstance.hash }}')"></i>
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
								<input type="hidden" name="group" value="{{- sprintInstance.virtualGroupName }}" />
							</form>
						</div>
					</div>
				</div>
				<div class="row">
					<div class="right-content discussions-wrapper">
						<div id="sprint-discussions" class="discussions"></div>
					</div>
				</div>
			</div>
			<div class="col-xs-2">
				{{ if (sprintInstance.hasMember) { }}
					<button id="leave-sprint" class="sprint-button" onclick="leaveSprint('{{- sprintInstance.hash }}')">Unfollow</button>
					{{ if (sprintInstance.hasStarted && !sprintInstance.hasEnded) { }}
						<button id="stop-sprint" class="sprint-button prompted-action" 
								onclick="stopSprint('{{- sprintInstance.hash }}')">Stop</button>
						<button id="start-sprint" class="prompted-action sprint-button hidden"
								onclick="startSprint('{{- sprintInstance.hash }}')">Start</button>
					{{ } else { }}
						<button id="start-sprint" class="prompted-action sprint-button"
								onclick="startSprint('{{- sprintInstance.hash }}')">Start</button>
						<button id="stop-sprint" class="sprint-button prompted-action hidden"
								onclick="stopSprint('{{- sprintInstance.hash }}')">Stop</button>
					{{ } }}
				{{ } else { }}
					<button id="join-sprint" class="sprint-button" onclick="joinSprint('{{- sprintInstance.hash }}')">Follow</button>
				{{ } }}
				{{ if (sprintInstance.hasAdmin) { }}">
					<button class="sprint-button" onclick="editSprint('{{- sprintInstance.hash }}')">Edit</button>
					<button class="sprint-button" onclick="deleteSprint('{{- sprintInstance.hash }}')">Delete</button>
				{{ } }}
			</div>
		</div>
	</div>
</div>