<div class="feed-item">
	<div class="sprint">
		<div class="contents">
			<div class="row">
				<div class="col-xs-9">
					<a onclick="sprintShow('{{- sprint.hash }}');" href="javascript:void(0);">
						<span> {{- sprint.name }}</span>
					</a>
				</div>
				<div class="col-xs-3 align-right">
					<span class="posting-time" data-time="{{- sprint.created }}"></span>
				</div>
			</div>
			<div class="sprint-description">
				<div>
					{{- sprint.description }}
				</div>
			</div>
			<div class="row">
				<div class="col-xs-8 tags-participants-label">
					<a href="/home/sprint/{{- sprint.hash }}">
						TAGS({{- sprint.totalTags }})
					</a>
					<a href="/home/sprint/{{- sprint.hash }}">
						PARTICIPANTS({{- sprint.totalParticipants }})
					</a>
				</div>
				<div class="col-xs-4 align-right read-more">
					<a href="/home/sprint/{{- sprint.hash }}">
						VIEW MORE
					</a>
				</div>
			</div>
		</div>
	</div>
</div>