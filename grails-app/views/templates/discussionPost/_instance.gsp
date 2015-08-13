<div class="discussion-comment">
	<div>
		<a href="#">
			<img class="avatar" src="/images/avatar2.png" alt="avatar">
			<span class="user-name"> 
				{{- discussionPost.authorName || 'anonymous'}}
			</span>
		</a>
		<span class="posting-time" data-time="{{- discussionPost.created || discussionPost.updated}}"></span>
		<div class="pull-right">
			{{ if (discussionPost.authorUserId == userId || isAdmin) { }}
				<span class="delete"> 
					<a href="#" class="delete-post" data-post-id="{{-discussionPost.id}}"> 
						<img src="/images/x.gif" width="8" height="8">
					</a>
				</span>
			{{ } }}
		</div>
	</div>
	<p class="without-margin">
		{{- discussionPost.message }}
	</p>
</div>