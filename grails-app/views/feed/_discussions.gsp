<g:each in="${discussionList }" var="discussionData">
	<div class="feed-item">
		<div class="discussion">
			<div class="discussion-topic">
			<div class="contents">
				<div class="row">
						<a href="#"><img class="avatar" src="/images/avatar.png"
							alt="..."><span class="user-name"> ${discussionData.userName}</span></a>
					<div class="pull-right">
						<span class="posting-time"><g:formatDate date="${discussionData.created}"
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
				</div>
				<div class="row buttons">
					<button>
						<img src="/images/share.png" alt="share">
					</button>
					<button>
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
				<div class="row">
					<a href="/home/discuss?discussionId=${discussionData.id }"> <span class="view-all">VIEW ALL
							COMMENTS(18)</span>
					</a>
				</div>
				<div class="row">
						<a href="#"><img class="avatar" src="/images/avatar2.png"
							alt="..."><span class="user-name"> ${discussionData.userName}</span></a>
						<span class="posting-time"><g:formatDate date="${discussionData.updated}"
								format="MM/dd/yyyy" /></span>
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
									id="post-comment" name="message">
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
