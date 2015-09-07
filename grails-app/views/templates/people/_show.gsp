<div class="people-detail">
	<div class="upper-body">
		{{ if (user.avatarURL) { }}
			<img src="{{- user.avatarURL }}" alt="avatar" class="img-circle">
		{{ } else { }}
			<i class="fa fa-circle fa-4x"></i>
		{{ } }}
		<h2>{{- user.username }}</h2>
		<button>FOLLOW</button>
	</div>

	<div class="lower-body">
		<div class="user-description">
			<p>{{- user.bio }}</p>
			<hr>
		</div>
		<div class="public-interests">
			<label class="people-label">
				PUBLIC INTEREST TAGS
			</label>
			<div class="media">
				<div class="media-left">
					<img src="/images/tag.png" height="30" width="auto">
				</div>
				<div class="media-body">
					<p> {{- (user.interestTags.length == 0) ? "No Tags" : user.interestTags.join(", ") }}</p>
				</div>
			</div>
		</div>
		<div class="public-sprints">
			<label class="people-label">
				SPRINTS
			</label>
			{{ if (user.sprints.length == 0) { }}
				<div class="media">
					<div class="media-left">
						<img src="/images/sprint-icon.png" height="30" width="auto">
					</div>
					<div class="media-body">
						<p>No Sprints</p>
					</div>
				</div>
			{{ } else { }}
				{{ _.each(user.sprints, function(sprint) { }}
				<div class="media">
					<div class="media-left">
						<img src="/images/sprint-icon.png" height="30" width="auto">
					</div>
					<div class="media-body">
						<h4 class="media-heading">{{- sprint.name }}</h4>
						<p>{{- sprint.description }}</p>
					</div>
				</div>
			{{ })} }}
		</div>
		<div class="public-groups">
			<label class="people-label">
				GROUPS
			</label>
			{{ _.each(user.groups, function(group) { }}
			<div class="media">
				<div class="media-left">
					<i class="fa fa-circle fa-2x"></i>
				</div>
				<div class="media-body">
					<h4 class="media-heading">{{- group.name }}</h4>
					<p>{{- group.description }}</p>
				</div>
			</div>
			{{ }) }}
		</div>
	</div>
</div>
