<g:each in="${posts}" var="discussPostInstance">
	<div class="comment">
		<a name="comment${discussPostInstance.getId()}"></a>
		<div class="message">
			${discussPostInstance.getMessage() ? discussPostInstance.getMessage().encodeAsHTML() : "[graph]"}
		</div>
		<div class="messageInfo">
			<g:set var="authorName"
				value="${(discussPostInstance.author.getUsername() ?: discussPostInstance.author.getName()).encodeAsURL()}" />

			<g:if test="${discussPostInstance.author.getSite()}">
				<a href="${discussPostInstance.author.getSite().encodeAsURL()}">${authorName }</a>
			</g:if>
			<g:else>
				${authorName }
			</g:else>

			<br />Last post
			<g:formatDate date="${discussPostInstance.getUpdated()}" type="datetime" style="SHORT" />
		</div>
		<div class="messageControls">
			<g:if test="${discussPostInstance.getAuthor().getUserId() == userId || isAdmin}">
				<span class="delete">
					<a href="#" onclick="return deletePost(${discussPostInstance.getId()})">
						<img src="/images/x.gif" width="8" height="8">
					</a>
				</span>
			</g:if>
		</div>
		<div style="clear: both"></div>
	</div>
</g:each>

<ul class="pagination" id="posts-pagination">
	<g:paginate total="${totalPostCount }" params="[discussionId: params.discussionId]" />
</ul>