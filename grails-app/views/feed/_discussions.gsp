<g:each in="${discussionList }" var="discussionData">
	<div class="feed-item">
		<div class="discussion">
			<div class="discussion-topic">
			<div class="contents">
				<div class="row">
					<div class="col-xs-9 discussion-header">
						<a href="#">
							<img class="avatar" src="/images/avatar.png" alt="avatar">
							<span class="user-name"> ${discussionData.userName}</span>
						</a>
					</div>
					<div class="col-xs-3 discussion-topic-span discussion-header">
						<span class="posting-time" data-time="${discussionData.created}"></span>
						<g:if test="${discussionData.isAdmin }">
							<li class="dropdown">
								<a href="#" data-toggle="dropdown"><b class="caret"></b></a>
								<ul class="dropdown-menu" role="menu">
									<li>
										<a href="#" class="delete-discussion" data-discussion-id="${discussionData.id } "> 
											<img src="/images/x.png" width="auto" height="23">Delete
										</a>
									</li>
								</ul>
							</li>
						</g:if>
					</div>
						
				</div>
				<div class="row top-left-margin">
					<span class="group"> ${discussionData.groupName }
					</span>
				</div>
				<div class="row title">
					<a href="/home/discuss?discussionId=${discussionData.id }"> 
						<span> ${discussionData.name ?: '(No Title)' }</span>
					</a>
				</div>
				<div class="row">
<%--				<g:if test="${discussionPostData["${discussionData.id}"][0]?.id == discussionData.firstPostId }">--%>
					<p>${discussionData.firstPost?.message }</p>
<%--				</g:if>--%>
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
			</div>
			<div class="discussion-comment">
				<div class="row bottom-margin">
					<a href="/home/discuss?discussionId=${discussionData.id }"> <span class="view-comment">VIEW ALL
							COMMENTS (${discussionPostData[discussionData.id]?.totalPosts?:0})</span>
					</a>
				</div>
				<g:if test="${discussionPostData[discussionData.id]?.secondPost}">
					<div class="row">
						<a href="#"><img class="avatar" src="/images/avatar2.png"
							alt="..."><span class="user-name"> ${discussionData.userName}</span></a>
						<span class="posting-time" data-time="${discussionData.updated}"></span>
					</div>
					<div class="row">
						<p>
							${discussionPostData[discussionData.id]?.secondPost }
						</p>
					</div>
				</g:if>
				<div class="row">
					<div class="col-md-6 add-comment">
						<form action="/home/discuss?commentForm=true" method="post" id="commentForm">
							<g:if test="${notLoggedIn}">
								<p>Enter your details below</p>

								<div id="postname">
									<input type="text" id="postname" name="postname" value="" class="postInput" /> Name
								</div>
								<div id="postemail">
									<input type="text" id="postemail" name="postemail" value="" class="postInput" /> Email (not publicly visible)
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
							<input type="hidden" name="discussionId" value="${discussionData.id}">
						</form>
					</div>
					<div class="class-md-6"></div>
				</div>
			</div>
		</div>
	</div>
</g:each>