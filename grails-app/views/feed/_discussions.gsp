<g:each in="${discussionList }" var="discussionData">
	<div class="feed-item">
		<div class="discussion">
			<div class="discusssion-topic">
				<div class="row">
					<div class="pull-left ">
						<a href="#"><img class="avatar" src="/images/avatar.png"
							alt="..."><span class="user-name"> ${discussionData.userName}</span></a>
					</div>
					<div class="pull-right">
						<span><g:formatDate date="${discussionData.created}"
								format="MM/dd/yyyy" /></span>
					</div>
				</div>
				<div class="row">
					<a href="/home/discuss?discussionId=${discussionData.id }"> <span
						class="title"> ${discussionData.name ?: '(No Title)' }
					</span>
					</a>
				</div>
				<div class="row">
					<p>dkdknvkdasnvck sikucnksancfawe ciwaefnuwaeiofc aweufinaweu</p>
				</div>
				<hr>
				<div class="row buttons">
					<button ><img src="/images/share.png" alt="share"></button>
					<button ><img src="/images/comment.png" alt="comment"></button>
				</div>
				<%--<div class="text-right">
					<span> LAST COMMENT ON <g:formatDate
							date="${discussionData.updated}" format="MM/dd/yyyy" /></span>
				</div>
				--%>
				<g:if test="${discussionData.isAdmin }">
					<div class="text-right">
						<a href="#" class="delete-discussion"
							data-discussion-id="${discussionData.id } "> <img
							src="/images/x.gif" width="8" height="8">
						</a>
					</div>
				</g:if>
			</div>
		</div>
	</div>
</g:each>
