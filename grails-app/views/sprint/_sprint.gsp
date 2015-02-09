<div class="sprint">
	<div class="row">
		<div class="left-content">
			<span class="label label-default">DEFAULT</span>
		</div>
		<div class="right-content">
			<h2>
				${title}
			</h2>
			<p>
				${description }
			</p>
		</div>
	</div>
	<hr>
	<div class="row">
		<div class="left-content">
			<span class="label label-default">TAGS</span>
		</div>
		<div class="right-content">
			<ul>
			<g:each in="${tags}" var="tagName">
				<li>${tagName } <button type="button" id="deleteTag">
								<i class="fa fa-times-circle"></i>
							</button>
				</li>
			</g:each>
			<form action="/feed/createSprint" method="post">
				<input id="addTags" name="tag" placeholder="click to add tag">
			</form>
			</ul>
		</div>
	</div>
	<hr>
	<div class="row">
		<div class=" left-content">
			<span class="label label-default">DATA</span>
		</div>
		<div class="right-content">
			<p>TBD</p>
		</div>
	</div>
	<hr>
	<div class="row">
		<div class=" left-content">
			<span class="label-default label-participants">PARTICIPANTS</span>
		</div>
		<div class="right-content">
			<g:each in="${participants}" var="participant">
				<img src="/images/track-avatar.png" alt="avatar" class="participantsAvatar">
			</g:each>
			<g:if test="${totalParticipants >= 5}">
				<img src="/images/moreParticipants.png" alt="avatar" id="moreAvatars" onclick="">
			</g:if>
			<form action="/feed/createSprint" method="post">
				<input id="invitePartcipants" name="participants" placeholder="click to invite participants">
			</form>
		</div>
	</div>
</div>