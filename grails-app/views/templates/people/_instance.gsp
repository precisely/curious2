<div class="people-wrapper">
	<div class="user-details-header">
		<a href="#" onclick="showUserDetails("{{- user.hash }}")">
			<img class="avatar img-circle" src="/images/avatar.png" alt="avatar">
			<span class="username">{{- user.username }}</span>
		</a>
		<button class="follow">FOLLOW</button>
	</div>
	<div class="user-details-content">
		<span>Interest Tags:</span>
		<span class="label-value">{{- user.interestTags.join(", ") }}</span>
	</div>
	<div class="user-details-content">
		<span>Public Sprints:</span>
		<span class="label-value">
			{{ user.sprints.forEach(function(sprint, index) { }}
				{{- sprint.name }}
				{{ if (index < user.sprints.length - 1) { print(", ") }; }}
			{{ }) }}
		</span>
	</div>
	<div class="user-details-content">
		<span>Start Date:</span>
		<span class="label-value">{{- $.datepicker.formatDate('mm/dd/yy', user.updated) }}</span>
	</div>
</div>