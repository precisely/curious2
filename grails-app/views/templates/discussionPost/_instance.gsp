<div class="discussion-comment media">
	<div class="media-left">
		<a href="#">
			<img class="avatar" src="/images/avatar2.png" alt="avatar">
		</a>
	</div>
	<div class="media-body">
		<span class="username"> 
			{{- discussionPost.authorName || 'anonymous'}}
		</span>
		<span class="posting-time" data-time="{{- discussionPost.created || discussionPost.updated}}"></span>
		{{ if (discussionPost.authorUserId == userId || discussionDetails.isAdmin) { }}
			<a href="#" class="delete-post pull-right" data-post-id="{{-discussionPost.id}}"> 
				<i class="fa fa-times-circle"></i>
			</a>
		{{ } }}
		<div class="message">
			{{- discussionPost.message }}
		</div>
	</div>
</div>