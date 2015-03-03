<g:each in="${posts}" var="discussPostInstance">
	<div class="discussion-comment multiple-comments">
		<a name="comment${discussPostInstance.getId()}"></a>
		<div class="row">
			<a href="#"><img class="avatar" src="/images/avatar2.png" alt="avatar">
				<span class="user-name"> 
					<g:set var="authorName" 
						value="${(discussPostInstance.author.getUsername() ?: discussPostInstance.author.getName()).encodeAsURL()}" />
					<g:if test="${discussPostInstance.author.getSite()}">
						<%--<a href="${discussPostInstance.author.getSite().encodeAsURL()}">
							${authorName }
						</a>
					--%>
					</g:if> <g:else>
						${authorName }
					</g:else>
				</span>
			</a>
			<span class="posting-time" data-time="${discussPostInstance.getUpdated().time}"></span>
			<div class="pull-right">
				<g:if
					test="${discussPostInstance.getAuthor().getUserId() == userId || isAdmin}">
					<span class="delete"> <a href="#"
						onclick="return deletePost(${discussPostInstance.getId()})"> <img
							src="/images/x.gif" width="8" height="8">
					</a>
					</span>
				</g:if>
			</div>
		</div>
		<div class="row">
			<p class="without-margin">
				${discussPostInstance.getMessage() ? discussPostInstance.getMessage().encodeAsHTML() : ""}
			</p>
		</div>
	</div>
</g:each>
