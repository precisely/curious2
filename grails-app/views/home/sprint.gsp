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
			<g:render template="/sprint/sprint" />
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
		<g:render template="/layouts/createSprintModal" /> 
	</content>
</body>
</html>
