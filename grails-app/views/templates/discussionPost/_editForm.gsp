<form class="comment-form edit-comment-form">
	<textarea name="message" rows="1" class="comment-fields comment-message auto-resize enter-submit allow-shift" required
		autofocus maxlength="${us.wearecurio.model.DiscussionPost.MAXMESSAGELEN}">{{- message}}</textarea>

	<input type="hidden" name="id" value="{{- id }}">
</form>