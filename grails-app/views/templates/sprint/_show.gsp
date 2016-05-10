	<div class="sprint {{- hasMember ? 'following' : 'not-following' }} {{- hasAdmin ? 'admin': 'not-admin'}}" id="sprint-{{- hash}}">
		<div class="row">
			<div class="col-xs-10 sprint-content">
				<div class="row">
					<div class="left-content">
						<span class="label-wide label-default">INSTRUCTIONS</span>
					</div>
					<div class="right-content">
						<h2>
							{{- name }}
						</h2>
						<p style="word-wrap: break-word">{{ _.each(_.initial(lines), function(entry) { }}
							{{= entry }}<br/>
							{{ }) }}
							{{= _.last(lines) }}
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
							{{ _.each (entries, function(entry) { }}
								<li class="sprintTag">
									{{- entry.description }}
									{{ if (RepeatType.isRemind(entry.repeatType)) { }}
										(<i>remind</i>)
									{{ } else if (RepeatType.isRepeat(entry.repeatType)) { }}
										(<i>repeat</i>)
									{{ } else if (RepeatType.isContinuous(entry.repeatType)) { }}
										(<i>bookmark</i>)
									{{ } }}
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
						<span class="label-default label-wide">PARTICIPANTS</span>
					</div>
					<div class="right-content">
						<i class="nav fa fa-chevron-left fa-4x" onclick="showPreviousParticipants()"></i>
						<div class="participants-wrapper inline-block">
							<div id="participants-list" class="inline-block">
								<ul>
									<li>
										<ul>
											{{ _.each(participants, function(participant) { }}
												<li>
													{{ if (participant.avatarURL) { }}
														<img src="{{- participant.avatarURL }}" alt="avatar" class="participantsAvatar img-circle">
													{{ } else { }}
														<img class="participantsAvatar img-circle" src="/images/track-avatar.png" alt="avatar">
													{{ } }}
													<p>{{- participant.username }}</p>
												</li>
											{{ }) }}
										</ul>
									</li>
								</ul>
							</div>
						</div>
						<i class="nav fa fa-chevron-right fa-4x pull-right" onclick="showMoreParticipants('{{- hash }}')"></i>
					</div>
				</div>
				{{ if (hasAdmin || (hasMember && !disableComments)) { }}
					<div class="row">
						<div class="right-content">
							<div class="new-post">
								<div class="new-post-container">
									<label>New post:&nbsp;</label>
								</div>
								<div class="new-post-container">
									<button class="btn btn-discussion" onclick="location.href='/home/graph?group={{- virtualGroupName}}'">Chart of your data</button>
									<button class="btn btn-discussion"
										onclick="showDiscussionAffordance()">
										Create a new trackathon discussion topic
									</button>
								</div>
								<form id="create-discussion" class="hide">
									<div class="input-affordance left-addon">
										<i class="fa fa-pencil"></i>
										<input class="full-width discussion-topic-input" type="text"
											   name="discussionTopic" id="discussion-topic" required />
									</div>
									<input type="hidden" name="group" value="{{- virtualGroupName }}" />
								</form>
							</div>
						</div>
					</div>
				{{ } }}
				<div class="row">
					<div class="right-content discussions-wrapper">
						<div id="sprint-discussions" class="discussions feed-items"></div>
					</div>
				</div>
			</div>
			<div class="col-xs-2 col-lg-1">
				<button id="leave-sprint" class="sprint-button {{- hasMember ? '' : 'hide' }}"
						onclick="leaveSprint('{{- hash }}')">Unfollow</button>

				<button id="join-sprint" class="sprint-button {{- hasMember ? 'hide' : '' }}"
						onclick="joinSprint('{{- hash }}')">Follow</button>

				{{ if (hasMember) { }}
					{{ if (hasStarted && !hasEnded) { }}
						<button id="stop-sprint" class="btn-purple sprint-button prompted-action"
								onclick="stopSprint('{{- hash }}')">Stop</button>
						<button id="start-sprint" class="btn-purple prompted-action sprint-button hidden"
								onclick="startSprint('{{- hash }}')">Start</button>
					{{ } else { }}
						<button id="start-sprint" class="btn-purple prompted-action sprint-button"
								onclick="startSprint('{{- hash }}')">Start</button>
						<button id="stop-sprint" class="btn-purple sprint-button prompted-action hidden"
								onclick="stopSprint('{{- hash }}')">Stop</button>
					{{ } }}
				{{ } else { }}
					<button id="stop-sprint" class="btn-purple sprint-button prompted-action hidden"
							onclick="stopSprint('{{- hash }}')">Stop</button>
					<button id="start-sprint" class="btn-purple prompted-action sprint-button"
							onclick="startSprint('{{- hash }}')">Start</button>
				{{ } }}
				{{ if (hasAdmin) { }}
					<button id="edit-sprint" class="sprint-button" onclick="editSprint('{{- hash }}')">Edit</button>
					<button class="sprint-button" onclick="deleteSprint('{{- hash }}')">Delete</button>
				{{ } }}
			</div>
		</div>
	</div>
