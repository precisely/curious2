<form class="comment-form edit-comment-form">
	<textarea name="message" rows="1" class="comment-fields comment-message auto-resize ctrl-enter" required
		autofocus maxlength="${us.wearecurio.model.DiscussionPost.MAXMESSAGELEN}">{{- message}}</textarea>

	<input type="hidden" name="id" value="{{- id }}">
	<g:render template="/templates/discussionPost/formSubmit"/>
</form>