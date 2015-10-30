<div class="people-wrapper">
	<div class="user-details-header">
		<a href="#people/{{- user.hash }}">
			<img src="{{- user.avatarURL || '/images/avatar.png' }}" alt="avatar" class="avatar img-circle">
			<span class="username">{{- user.name }}</span>
		</a>
		<button class="follow">FOLLOW</button>
	</div>
	<div class="user-details-content">
		<span>Interest Tags:</span>
		<span class="label-value">{{- user.interestTagsString }}</span>
	</div>
	<div class="user-details-content hide">
		<span>Public Sprints:</span>
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
