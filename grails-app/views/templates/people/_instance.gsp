<div class="feed-item user">
	<div class="user-details-header">
		<img src="/images/feed/people.png" class="helper-icon" />
		<a href="/home/social#people/{{- user.hash }}">
			<img src="{{- user.avatarURL || '/images/avatar.png' }}" alt="avatar" class="avatar img-circle">
			<span class="username">{{- user.nameInfo }}</span>
		</a>
		<button class="people-follow {{- user.followed ? 'following' : 'follow'}}" id="follow-user-{{- user.hash }}"
				onclick="setFollowUser('{{- user.hash }}', ! {{- user.followed }})">{{- user.followButtonText}}</button>
	</div>
	<div class="user-details-content">
		<span>Interest Tags:</span>
		<span class="label-value">{{- user.publicInterestTagsString }}</span>
	</div>
	<div class="user-details-content hide">
		<span>Public Trackathons:</span>
		<span class="label-value">
			<!-- Using an empty list and keeping the code for now. Can be removed if user's sprints “user.sprints”
				 are not going to be used in the future. -->
			{{ [].forEach(function(sprint, index) { }}
				{{- sprint.name }}
				{{ if (index < user.sprints.length - 1) { print(", ") }; }}
			{{ }) }}
		</span>
	</div>
	<div class="user-details-content">
		<span>Start Date:</span>
		<span class="label-value">{{- $.datepicker.formatDate('mm/dd/yy', new Date(user.created)) }}</span>
	</div>
</div>
