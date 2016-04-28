<div class="new-post">
	<div class="new-post-container">
		<label>New post:&nbsp;</label>
	</div>
	<div class="new-post-container">
		<button class="btn btn-discussion" onclick="location.href='/home/graph?group=public'">Chart of your data</button>
		<button class="btn btn-discussion"
				onclick="showDiscussionAffordance('howto')">How to article</button>
		<button class="btn btn-discussion"
				onclick="showDiscussionAffordance('support')">Support question</button>
	</div>
	<form id="create-discussion" class="hide">
		<div class="input-affordance left-addon">
			<i class="fa fa-pencil"></i>
			<input class="full-width discussion-topic-input" type="text"
				name="discussionTopic" id="discussion-topic" required />
		</div>
	</form>
</div>
