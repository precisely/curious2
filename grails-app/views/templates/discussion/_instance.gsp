<!--
	A Javascript Lodash or Underscore template used to render a single Discussion
-->

<div class="feed-item discussion" id="discussion-{{- discussionData.hash}}">
	<div class="discussion-topic">
		<div class="contents">
			<div class="row">
				<div class="col-xs-9 discussion-header">
					<img src="/images/feed/discussion.png" class="helper-icon" />
					<a href="/home/social#people/{{- discussionData.userHash }}">
						<img src="{{- discussionData.userAvatarURL || '/images/avatar.png' }}" alt="avatar" class="avatar img-circle">
						&nbsp; <span class="username">{{- discussionData.publicUserName ? (discussionData.userName + '(' + discussionData.publicUserName + ')') : discussionData.userName}}</span>
					</a>
				</div>
				<div class="col-xs-3 discussion-topic-span discussion-header">
					<span class="posting-time" data-time="{{- discussionData.created }}"></span>
					{{ if (discussionData.isAdmin) { }}
						<div class="dropdown">
							<a href="#" data-toggle="dropdown"><b class="caret"></b></a>
							<ul class="dropdown-menu" role="menu">
								<li>
									<a href="#" class="delete-discussion" data-discussion-hash="{{- discussionData.hash }}">
										<i class="fa fa-trash fa-fw"></i> Delete
									</a>
								</li>
							</ul>
						</div>
					{{ } }}
				</div>
			</div>
			<div class="group">
				{{- discussionData.groupName }}
			</div>
			<div class="row">
				<div class="col-md-5">
					<div class="name">
						<a href="/home/social#discussions/{{- discussionData.hash }}">
							<span>{{- discussionData.name ? discussionData.name: '(No Title)' }}</span>
						</a>
					</div>
				</div>
				<div class="col-md-7">
					<div class="buttons">
						{{ if (discussionData.isFollower) {  }}
							<button id="follow-button-{{- discussionData.hash }}" onclick="followDiscussion({id: '{{- discussionData.hash }}', unfollow: true})">
							<img src="/images/unfollow.png" alt="unfollow" >Unfollow
						{{ } else { }}
							<button id="follow-button-{{- discussionData.hash }}" onclick="followDiscussion({id: '{{- discussionData.hash }}' })">
							<img src="/images/follow.png" alt="follow">Follow
						{{ } }}
						</button>
						<button class="share-button" data-toggle="popover" title="Share:"
							data-placement="top" data-content="<input class='share-link' type='text' value='{{- serverURL}}/home/social#discussions/{{- discussionData.hash }}'>">
							<img src="/images/share.png" alt="share">Share
						</button>
						<button onclick="toggleCommentsList('{{- discussionData.hash }}')">
							{{ if (!discussionData.totalComments || discussionData.totalComments < 1) {  }}
								<img src="/images/comment.png" alt="comment"> Comment
							{{ } else { }}
								<div class="dark-comment comment-button" data-total-comments="{{- discussionData.totalComments }}">
									{{- discussionData.totalComments }}
								</div>
								Comment
							{{ } }}
						</button>
					</div>
				</div>
			</div>
		</div>
	</div>
	<div class="discussion-comments-wrapper hide clearfix">
		{{ if ( discussionData.totalComments > 5 ) { }}
			<div class="view-comment" data-discussion-hash="{{- discussionData.hash}}">VIEW MORE COMMENTS</div>
		{{ } }}

		<div class="comments media-list"></div>
		{{var isCommentAllowed = discussionData.isAdmin || !discussionData.disableComments}}

		<div class="add-comment">
			<form class="comment-form {{- isCommentAllowed ? '' : 'comment-disabled' }}">
				{{ if (isCommentAllowed) { }}
					<input type="text" placeholder="Add Comment..." name="message" required autofocus>
				{{ } else { }}
					<input type="text" placeholder="&#xf05e;  Comments disabled" name="message" required>
				{{ } }}
				<input type="hidden" name="discussionHash" value="{{- discussionData.hash }}">
			</form>
		</div>
	</div>
</div>
