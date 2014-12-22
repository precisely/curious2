<html>
<head>
<meta name="layout" content="feedLayout" />
<script type="text/javascript">
$(document).ready(function() {
	$('#queryTitle').text('Tracking Sprint');
});
</script>
</head>
<body>
	<content tag="feedContent">
		<div id="discussions">
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
								<li>
									${tagName } 
									<button type="button" id="deleteTag">
										<i class="fa fa-times-circle"></i>
									</button>
								</li>
							</g:each>
						</ul>
						<form action="/dummy/createSprint" method="post">
							<input id="addTags" name="tag" placeholder="click to add tag">
						</form>
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
						<form action="/dummy/createSprint" method="post">
							<input id="invitePartcipants" name="participants" placeholder="click to invite participants">
						</form>
					</div>
				</div>
			</div>
		</div>
	
		<div style="clear: both;"></div>
		<div id="share-dialog" class="hide" title="Share">
			<select name="shareOptions" id="shareOptions" multiple="multiple"
				class="form-control" size="8">
				<option value="isPublic">Visible to the world</option>
				<g:each in="${associatedGroups }" var="userGroup">
					<option value="${userGroup.id }">
						${userGroup.fullName }
					</option>
				</g:each>
			</select>
		</div>
		<div id="comment-dialog" class="hide" title="Comment">
			<input type="text" name="comment" id="userComment" required
				placeholder="Add Comment..."> <input type="hidden"
				name="discussionId" value="${discussionId}">
		</div>
		<g:render template="/sprint/createSprintModal" /> 
	</content>
</body>
</html>
