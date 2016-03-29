<!-- Just renders the textarea for adding/modifying a discussion post/comment or for adding the description to the
discussion. This is to make the code DRY.
 -->
<textarea name="message" rows="1" class="comment-fields comment-message auto-resize enter-submit allow-shift"
	maxlength="${us.wearecurio.model.DiscussionPost.MAXMESSAGELEN}"
	placeholder="{{= isCommentAllowed ? 'Add a comment to this discussion...' : '&#xf05e;  Comments disabled'}}"></textarea>