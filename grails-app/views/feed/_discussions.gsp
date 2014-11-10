<g:each in="${discussionList }" var="discussionData">
	<div class="feed-item">
		<div class="discussion">
			<div class="discussion-topic">
			<div class="contents">
				<div class="row">
					<div class="col-md-9 discussion-header">
						<a href="#"><img class="avatar" src="/images/avatar.png"
							alt="..."><span class="user-name"> ${discussionData.userName}</span></a>
					</div>
					<div class="col-md-3 discussion-topic-span discussion-header">
						<span class="posting-time" data-time="${discussionData.created}"></span>
					</div>
						
				</div>
				<div class="row top-left-margin">
					<span class="group"> ${groupFullname }
					</span>
				</div>
				<div class="row title">
					<a href="/home/discuss?discussionId=${discussionData.id }"> <span
						> ${discussionData.name ?: '(No Title)' }
					</span>
					</a>
				</div>
				<div class="row">
					<p>dkdknvkdasnvck sikucnksancfawe ciwaefnuwaeiofc aweufinaweu</p>
				</div>
				</div>
				<div class="row buttons">
					<g:if test="${discussionData.isAdmin }">
					<button onclick="showShareDialog(${discussionData.id })">
						<img src="/images/share.png" alt="share">
					</button>
					</g:if>
					<button onclick="showCommentDialog(${discussionData.id })">
						<img src="/images/comment.png" alt="comment">
					</button>
				</div>
				<%--<div class="text-right">
					<span> LAST COMMENT ON <g:formatDate
							date="${discussionData.updated}" format="MM/dd/yyyy" /></span>
				</div>
				--%>
				<%--<g:if test="${discussionData.isAdmin }">
					<div class="text-right">
						<a href="#" class="delete-discussion"
							data-discussion-id="${discussionData.id } "> <img
							src="/images/x.gif" width="8" height="8">
						</a>
					</div>
				</g:if>
			--%>
			</div>
			<div class="discussion-comment">
				<div class="row bottom-margin">
					<a href="/home/discuss?discussionId=${discussionData.id }"> <span class="view-comment">VIEW ALL
							COMMENTS (18)</span>
					</a>
				</div>
				<div class="row">
						<a href="#"><img class="avatar" src="/images/avatar2.png"
							alt="..."><span class="user-name"> ${discussionData.userName}</span></a>
						<span class="posting-time" data-time="${discussionData.updated}"></span>
				</div>
				<div class="row">
					<p>
						Random comment to perform visual test.
					</p>
				</div>
				<div class="row">
					<div class="col-md-6 add-comment">
						<form action="/home/discuss?commentForm=true" method="post"
							id="commentForm">
							<g:if test="${notLoggedIn}">
								<p>Enter your details below</p>

								<div id="postname">
									<input type="text" id="postname" name="postname" value=""
										class="postInput" /> Name
								</div>
								<div id="postemail">
									<input type="text" id="postemail" name="postemail" value=""
										class="postInput" /> Email (not publicly visible)
								</div>
								<div id="posturl">
									<input type="text" id="postsite" name="postsite" value=""
										class="postInput" /> Website URL (optional)
								</div>
								<div id="postcomment">
									<textarea rows="20" cols="100" style="border-style: solid"
										id="postcommentarea" name="message"></textarea>
								</div>
								<br />
								<input type="button" class="submitButton"
									id="commentSubmitButton" value="submit" />
								<!--p class="decorate">Comments must be approved, so will not appear immediately. </p-->
							</g:if>
							<g:else>
								<input type="text" 
									placeholder="Add Comment..."
									id="post-comment" name="message" required>
							</g:else>
							<input type="hidden" name="discussionId" value="${discussionId}">
						</form>
					</div>
					<div class="class-md-6"></div>
				</div>
			</div>
		</div>
	</div>
</g:each>
<ul class="pagination" id="discussion-pagination">
	<g:paginate total="${totalDiscussionCount ?: 0 }" action="feed"
		params="[userGroupNames: params.userGroupNames]" />
</ul>
