<div class="discussion-comment media hide">
	<div class="media-left">
		<a href="#people/{{- discussionPost.authorHash }}">
			<img src="{{- discussionPost.authorAvatarURL || '/images/avatar2.png' }}" alt="avatar" class="avatar img-circle">
		</a>
	</div>
	<div class="media-body">
		<span class="username"> 
			{{- discussionPost.authorName || 'anonymous'}}
		</span>
		<span class="posting-time" data-time="{{- discussionPost.created || discussionPost.updated}}"></span>
		{{ if (discussionPost.authorUserId == userId || discussionDetails.isAdmin) { }}
			<a href="#" class="delete-post pull-right" data-post-id="{{-discussionPost.id}}" data-discussion-hash="{{-discussionPost.hash}}"> 
				<i class="fa fa-times-circle"></i>
			</a>
		{{ } }}
		<div class="message">
			{{- discussionPost.message }}
		</div>
	</div>
</div>
