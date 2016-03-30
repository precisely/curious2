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
		<span class="posting-time" data-time="{{- discussionPost.created }}"></span>
		{{ if (discussionPost.created !== discussionPost.updated) { }}
			<!--<span class="posting-time" title="{{- _.formatDate(discussionPost.created, 'M/dd/yy h:mm a') }}">
			(edited)
			</span>-->
		{{ } }}

		{{ if (discussionPost.authorUserId == userId || discussionDetails.isAdmin) { }}
			<span class="pull-right">
				<a href="#" class="cancel-edit-post comment-buttons hide" data-post-id="{{- discussionPost.id}}"
					data-discussion-hash="{{-discussionPost.hash}}" title="Cancel editing">
					<i class="fa fa-times-circle fa-fw"></i>
				</a>
				<a href="#" class="edit-post comment-buttons hide" data-post-id="{{- discussionPost.id}}"
					data-discussion-hash="{{-discussionPost.hash}}" title="Edit comment">
					<i class="fa fa-pencil fa-fw"></i>
				</a>
				<a href="#" class="delete-post comment-buttons hide" data-post-id="{{- discussionPost.id}}"
					data-discussion-hash="{{-discussionPost.hash}}" title="Delete comment">
					<i class="fa fa-trash"></i>
				</a>
			</span>
		{{ } }}
		<div class="message">
			{{= _.escape(discussionPost.message).newLineToBr() }}
		</div>
	</div>
</div>
