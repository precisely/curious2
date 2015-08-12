<div class="discussions">
	<div class="feed-item">
		<div class="discussion">
			<div class="discussion-topic">
				<div class="contents">
					<div class="row">
						<div class="col-xs-9 discussion-header">
							<a href="#">
								<img class="avatar img-circle" src="/images/avatar.png" alt="avatar">
								<span class="user-name">{{- discussionData.userName }}</span>
							</a>
						</div>
						<div class="col-xs-3 discussion-topic-span discussion-header">
							<span class="posting-time" data-time="{{- discussionData.created }}"></span>
							{{ if (discussionData.isAdmin) { }}
								<li class="dropdown">
									<a href="#" data-toggle="dropdown"><b class="caret"></b></a>
									<ul class="dropdown-menu" role="menu">
										<li>
											<a href="#" class="delete-discussion" data-discussion-hash="{{- discussionData.hash }}">
												<img src="/images/x.png" width="auto" height="23">Delete
											</a>
										</li>
									</ul>
								</li>
							{{ } }}
						</div>
					</div>
					<div class="group">
						{{- discussionData.groupName }}
					</div>
					<div class="row">
						<div class="col-xs-7">
							<a href="/home/discuss?discussionHash={{- discussionData.hash }}">
								<span>{{- discussionData.name ? discussionData.name: '(No Title)' }}</span>
							</a>
						</div>
						<div class="col-xs-5 button-box">
							<div class="buttons">
								<button onclick="showShareDialog('{{- discussionData.hash }}')">
									<img src="/images/follow.png" alt="follow">Follow
								</button>
								{{ if (discussionData.isAdmin) {  }}
									<button class="share-button" data-toggle="popover" title="Share:"
										data-placement="top" data-content="<input class='share-link' type='text' value='{{- location.protocol+'//'+location.hostname+(location.port ? ':' + location.port : '') }}/home/discuss?discussionHash={{- discussionData.hash }}'>">
										<img src="/images/share.png" alt="share">Share
									</button>
								{{ } }}
								<button onclick="toggleCommentsList('{{- discussionData.hash }}')">
									{{ if (!discussionData.totalComments || discussionData.totalComments < 1) {  }}
										<img src="/images/comment.png" alt="comment"> Comment</img>
									{{ } else { }}
										<div class="dark-comment comment-button" data-total-comments="{{- discussionData.totalComments }}">
											{{- discussionData.totalComments }}
										</div>
										Comment
									{{ } }}
								</button>
							</div>
						</div>
					</div>
				</div>
			</div>
			<div class="discussion-comment hide" id="discussion{{- discussionData.hash }}-comment-list">
				<div class="comments"></div>
				<div class="bottom-margin">
					<span class="view-comment"> VIEW MORE COMMENTS </span>
				</div>
				<div class="row">
					<div class="col-md-6 add-comment">
						<form action="/home/discuss" method="post" id="commentForm">
							{{ if (false) { }}
								<p>Enter your details below</p>
								<div id="postname">
									<input type="text" id="postname" name="postname" value="" class="postInput" />Name</div>
									<div id="postemail">
										<input type="text" id="postemail" name="postemail" value="" class="postInput" />
										Email (not publicly visible)
									</div>
									<div id="posturl">
										<input type="text" id="postsite" name="postsite" value="" class="postInput" />
										Website URL (optional)
									</div>
									<div id="postcomment">
										<textarea rows="20" cols="100" style="border-style: solid" id="postcommentarea" name="message"></textarea>
									</div><br />
									<input type="button" class="submitButton" id="commentSubmitButton" value="submit" />
									<!--p class="decorate">Comments must be approved, so will not appear immediately. </p-->
								{{ } else { }}
									<input type="text" placeholder="Add Comment..." id="post-comment" name="message" required>
								{{ } }}
							<input type="hidden" name="discussionHash" value="{{- discussionData.hash }}">
						</form>
					</div>
					<div class="class-md-6"></div>
				</div>
			</div>
		</div>
	</div>
</div>