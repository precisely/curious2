<g:if test="${!parameters.offset}">
	<div class="new-post">
		<form id="create-discussion" action="/discussion/createTopic" method="post">
			<div class="input-affordance left-addon">
				<i class="fa fa-pencil"></i> 
				<input class="full-width discussion-topic-input"
						type="text" placeholder="New question or discussion topic?"
						name="name" id="discussion-topic" required />
				<input type="radio" class="radio-public" name="visibility" id="public" value="public" checked><label for="public" class="radio-public-label">Public</label>
				<input type="radio" class="radio-private" name="visibility" id="private" value="private"><label for="private" class="radio-private-label">Private</label>
				<hr class="hide">
				<input type="text" id="discussion-discription" class="full-width discussion-topic-description hide" placeholder="Enter comment/description"
						name="discussionPost">
			</div>
			<input type="hidden" name="group" value="${groupName}" />
		</form>
	</div>
</g:if>
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
							<span class="posting-time" data-time="${discussionData.created.time}"></span>
							<g:if test="${discussionData.isAdmin }">
								<li class="dropdown">
									<a href="#" data-toggle="dropdown"><b class="caret"></b></a>
									<ul class="dropdown-menu" role="menu">
										<li>
											<a href="#" class="delete-discussion" data-discussion-id="${discussionData.id }"> 
												<img src="/images/x.png" width="auto" height="23">Delete
											</a>
										</li>
									</ul>
								</li>
							</g:if>
						</div>
					</div>
					<div class="group"> 
						${discussionData.groupName }
					</div>
					<div class="row">
						<div class="col-xs-7">
							<a href="/home/discuss?discussionId=${discussionData.id}"> 
								<span> ${discussionData.name ?: '(No Title)' }</span>
							</a>
						</div>
						<div class="col-xs-5 button-box">
							<div class="buttons">
								<g:if test="true">
									<button onclick="showShareDialog(${discussionData.id})">
										<img src="/images/follow.png" alt="follow">Follow
									</button>
								</g:if>
								<g:if test="${discussionData.isAdmin }">
									<button class="share-button" onclick="showShareDialog(${discussionData.id})">
										<img src="/images/share.png" alt="share">Share
									</button>
								</g:if>
								<button onclick="toggleCommentsList(${discussionData.id})">
									<g:if test="${!discussionPostData[discussionData.id]?.totalPosts}">
										<img src="/images/comment.png" alt="comment"> Comment</img>
									</g:if>
									<g:else>
									<div class="dark-comment">${discussionPostData[discussionData.id]?.totalPosts}</div>
										Comment
									</g:else>
								</button>
							</div>
						</div>
					</div>
				</div>
			</div>
			<div class="discussion-comment hide" id="discussion${discussionData.id}-comment-list">
				<div class="comments">
				</div>
				<div class="bottom-margin">
					<a href="#">
						<span class="view-comment">
							VIEW MORE COMMENTS
						</span>
					</a>
				</div>
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
