<div class="new-post">
	<form id="create-discussion" action="/home/discuss?createTopic=true" method="post">
		<div class="input-affordance left-addon">
			<i class="fa fa-pencil"></i> 
			<input class="full-width discussion-topic-input" type="text" placeholder="New question or discussion topic?"
				name="discussionTopic" id="discussion-topic" required />

			<input type="radio" class="radio-public" name="visibility" id="public" value="public" checked>
			<label for="public" class="radio-public-label">Public</label>

			<input type="radio" class="radio-private" name="visibility" id="private" value="private">
			<label for="private" class="radio-private-label">Private</label>
		</div>
		<input type="hidden" name="group" value="{{- groupName }}" />
	</form>
</div>