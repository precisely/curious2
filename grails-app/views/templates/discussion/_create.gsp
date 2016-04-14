<div class="new-post">
	<label>New post:&nbsp;</label>
	<button class="btn btn-discussion" onclick="location.href='/home/graph?group=public'">Chart of your data</button>
	<button class="btn btn-discussion"
			onclick="$('#create-discussion').show('fast').find('input').prop('placeholder', 'Enter text of your how-to article').data('type', 'howto')">How to article</button>
	<button class="btn btn-discussion"
			onclick="$('#create-discussion').show('fast').find('input').prop('placeholder', 'Ask a support question of the community?').data('type', 'support')">Support question</button>
	<form id="create-discussion" class="hide">
		<div class="input-affordance left-addon">
			<i class="fa fa-pencil"></i>
			<input class="full-width discussion-topic-input" type="text"
				name="discussionTopic" id="discussion-topic" required />
		</div>
	</form>
</div>
