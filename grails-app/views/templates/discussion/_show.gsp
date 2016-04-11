<div class="discuss-page">
	{{ if (firstPost && firstPost.plotDataId) { }}
	<div id="plotLeftNav" style="margin-top: 4px;">
		<div class="discussPlotLines plotlines clearfix" id="plotLinesplotDiscussArea"></div>
	</div>
	<div class="row row-custom">
		<div class="col-xs-12">
			<div class="main discussmain" style="margin: 0">
				<div id="dialogDivplotDiscussArea" class="display:none;"></div>
				<div class="graphData">
					<div class="discussPlotArea" id="plotDiscussArea"></div>

					<div class="main discusscontrols">
						<div class="calendarRange">
							<div class="zoomline">
								<div id="zoomcontrol1"></div>
							</div>
							<div class="dateline row">
								<div class="col-xs-4">
									<span class="startDate">
										<input id="startdatepicker1" type="text" value="" class="startdatepicker cycleInput" />
									</span>
								</div>
								<div class="col-xs-4">
									<!-- span class="cycleTag" id="cycleTag1"><input type="text" class="cycleTagInput" name="cycletag" value="" class="cycleInput" /></span -->
								</div>
								<div class="col-xs-4">
									<span class="endDate"><input id="enddatepicker1"
											type="text" value="" class="enddatepicker cycleInput" /></span>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>

	<div id="preFooter"></div>
	{{ } }}

	<div class="main" style="height: auto;">
		<div class="feed-item discussion" id="discussion-{{- discussionHash }}">
			<div class="discussion-topic">
				<div class="contents">
					<div class="row">
						<div class="col-xs-9 discussion-header">
							<a href="#people/{{- discussionOwnerHash }}">
								{{ if (discussionOwnerAvatarURL) { }}
								<img src="{{- discussionOwnerAvatarURL }}" alt="avatar" class="avatar img-circle">
								{{ } else { }}
								<img class="avatar img-circle" src="/images/avatar.png" alt="avatar">
								{{ } }}
								&nbsp; <span class="username">{{- discussionOwner }}</span>
							</a>
						</div>
						<div class="col-xs-3 discussion-topic-span discussion-header">
							<span class="posting-time" data-time="{{- discussionCreatedOn }}"></span>
							{{ if (isAdmin) { }}
							<div class="dropdown">
								<a href="#" data-toggle="dropdown">
									<b class="caret"></b>
								</a>
								<ul class="dropdown-menu" role="menu">
									<li>
										<a href="#" class="delete-discussion" data-discussion-hash="{{- discussionHash }}">
											<i class="fa fa-trash fa-fw"></i> Delete
										</a>
									</li>
									<li>
										<a href="#" class="edit-discussion" data-discussion-hash="{{- discussionHash }}">
											<i class="fa fa-pencil-square fa-fw"></i> Edit
										</a>
									</li>
								</ul>
							</div>
							{{ } }}
						</div>
					</div>
					<div class="group">
						{{- (associatedGroups[0] && associatedGroups[0].shared) ? associatedGroups[0].fullName : '' }}
					</div>
					<span class="discussion-title"> {{- discussionTitle || '(No Title)' }}</span>
					<small id="title-updated" class="text-red hide margin-left">
						<i class="fa fa-check-square-o"> Discussion updated.</i>
					</small>

					<div class="first-post-container">
						{{ if (firstPost && firstPost.message) { }}
						<span class="first-post-message">{{= _.linkify(_.escape(firstPost.message).newLineToBr()) }}</span>
						{{ } else { }}
						<span class="first-post-message"></span>
						{{ } }}

						<form action="#"
								class="edit-comment-form add-description-form relative {{- (isAdmin && (!firstPost || !firstPost.message)) ? '' : 'hide'}}">
							<input type="hidden" name="id" value="{{- firstPost.id}}" />

							<textarea name="message" rows="1" class="auto-resize enter-submit allow-shift"
									  placeholder="Add description..." autofocus
									  maxlength="${us.wearecurio.model.DiscussionPost.MAXMESSAGELEN}"></textarea>

							<div class="edit-options hide text-right">
								<a href="#" title="Clear description"
										onclick="$('.edit-comment-form').find('textarea').val('').trigger('change'); return false;">
									<i class="fa fa-times-circle fa-fw"></i>
								</a>
							</div>
						</form>
					</div>
					<hr>
					<div class="buttons">
						<div class="row">
							<div class="col-sm-6 text-left">
								{{ if (isAdmin) { }}
								<span class="checkbox-orange checkbox-sm">
									<input type="checkbox" id="disable-comments" data-hash="{{- discussionHash }}"
										   {{- disableComments ? "checked" : "" }}>
									<label for="disable-comments"></label>
									<small>Disable Comments</small>

									<small id="flash-message" class="text-red hide margin-left">
										<i class="fa fa-check-square-o"> Preference saved.</i>
									</small>
								</span>
								{{ } }}
							</div>
							<div class="col-sm-6">
								{{ if (isFollowing) {  }}
								<button id="follow-button-{{- discussionHash }}" onclick="followDiscussion({id: '{{- discussionHash }}', unfollow: true})">
									<img src="/images/unfollow.png" alt="unfollow">Unfollow
								</button>
								{{ } else { }}
								<button id="follow-button-{{- discussionHash }}" onclick="followDiscussion({id: '{{- discussionHash }}' })">
									<img src="/images/follow.png" alt="follow">Follow
								</button>
								{{ } }}
								{{ if (isAdmin) { }}
									<button class="share-button" data-toggle="modal" data-target="#share-modal"
											data-share-url="{{- serverURL }}/home/social/discussions/{{- discussionHash }}"
											data-discussion-title="{{- discussionTitle }}">
										<img src="/images/share.png" alt="share"> Share
									</button>
								{{ } }}
								<button class="comment-button" data-total-comments="{{- totalPostCount }}">
									<img src="/images/comment.png" alt="comment"> Comment
								</button>
							</div>
						</div>
					</div>
				</div>
			</div>

			<div class="discussion-comments-wrapper clearfix">
				{{ if ( totalPostCount > 5 ) { }}
				<div class="view-comment" data-discussion-hash="{{- discussionHash }}">VIEW MORE COMMENTS</div>
				{{ } }}

				<!-- All comments will be inserted here -->
				<div class="comments"></div>

				<div class="add-comment {{- disableComments && (!isAdmin) ? 'hide' : ''}}">
					{{var isCommentAllowed = !disableComments || isAdmin }}

					<form class="comment-form new-comment-form {{- isCommentAllowed ? '' : 'comment-disabled' }}">
					%{--Temporarily removing commenting by users outside the platform.--}%
						%{--{{ if (notLoggedIn) { }}
							<p>Enter your details below</p>

						<div class="form-group">
							<label>Name</label>
							<input type="text" name="postname" class="comment-fields" />
						</div>

						<div class="form-group">
							<label>Email (not publicly visible)</label>
							<input type="text" name="postemail" class="comment-fields" />
						</div>

						<div class="form-group">
							<label>Website URL (optional)</label>
							<input type="text" name="postsite" class="comment-fields" />
						</div>

							<div class="form-group">
								<label>Message</label>
								<g:render template="/templates/discussionPost/commentField" ></g:render>
							</div>
						{{ } else if (canWrite) { }}--}%
						{{ if (canWrite) { }}
							<g:render template="/templates/discussionPost/commentField" ></g:render>
						</div>
						{{ } else if (canWrite) { }}
						<g:render template="/templates/discussionPost/commentField" ></g:render>
						{{ } }}

						<input type="hidden" name="discussionHash" value="{{- discussionHash }}">
					</form>
				</div>
			</div>
		</div>
	</div>

	<div style="clear: both;"></div>

	<input type="hidden" name="discussionHash" value="{{- discussionHash }}" />

	<div id="share-dialog" class="hide" title="Share">
		<select name="shareOptions" id="shareOptions" multiple="multiple" class="form-control" size="8">
			<option value="isPublic" {{- isPublic ? 'selected="selected"' : '' }}>Visible to the world</option>
			{{ _.each(associatedGroups, function(userGroup) { }}
				<option value="{{- userGroup.id }}" {{- userGroup.shared ? 'selected="selected"' : '' }}>
					{{- userGroup.fullName }}
				</option>
			{{ }) }}
		</select>
	</div>
</div>
