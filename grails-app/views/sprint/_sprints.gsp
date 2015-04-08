<g:each in="${sprintList}" var="sprint">
	<div class="feed-item">
		<div class="sprint">
			<div class="contents">
				<div class="row">
					<div class="col-xs-9">
						<a href="/home/sprint/${sprint.id}">
							<span> ${sprint.name}</span>
						</a>
					</div>
					<div class="col-xs-3 align-right">
						<span class="posting-time" data-time="${sprint.created.time}"></span>
					</div>
				</div>
				<div class="sprint-description">
					<div>
						${sprint.description}
					</div>
				</div>
				<div class="row">
					<div class="col-xs-8 tags-participants-label">
						<a href="/home/sprint/${sprint.id}">
							TAGS(${sprint.getEntriesCount()})
						</a>
						<a href="/home/sprint/${sprint.id}">
							PARTICIPANTS(${sprint.getParticipantsCount()})
						</a>
					</div>
					<div class="col-xs-4 align-right read-more">
						<a href="/home/sprint/${sprint.id}">
							VIEW MORE
						</a>
					</div>
				</div>
			</div>
		</div>
	</div>
</g:each>
