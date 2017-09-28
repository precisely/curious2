<div class="people-detail">
	<div class="upper-body">
		{{ if (user.avatarURL) { }}
			<img src="{{- user.avatarURL }}" alt="avatar" class="img-circle">
		{{ } else { }}
			<i class="fa fa-circle fa-4x"></i>
		{{ } }}
		<h2>{{- user.username }}</h2>
		<button id="follow-user-{{- user.hash }}" onclick="setFollowUser('{{- user.hash }}', ! {{- user.followed }})"
			class="people-follow {{- user.followed ? 'following' : 'follow'}}">{{- user.followButtonText}}</button>
	</div>

	<div class="lower-body">
		{{ if (user.bio) { }}
			<div class="user-description">
				{{= _.linkify(user.bio) }}
				<hr>
			</div>
		{{ } }}
		<div class="public-interests">
			<label class="people-label">
				PUBLIC INTEREST TAGS
			</label>
			<div class="media">
				<div class="media-left">
					<img src="/images/tag.png" height="25" width="auto">
				</div>
				<div class="media-body">
					<p> {{- (user.interestTags.length == 0) ? "No Tags" : user.interestTags.join(", ") }}</p>
				</div>
			</div>
		</div>
		<div class="public-sprints">
			<label class="people-label">
				TRACKATHONS
			</label>
			{{ if (user.sprints.length == 0) { }}
				<div class="media">
					<div class="media-left">
						<img src="/images/sprint-icon.png" height="30" width="auto">
					</div>
					<div class="media-body">
						<p>No Trackathons</p>
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
