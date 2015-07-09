var _people = '\
<div class="people-wrapper">\
	<div class="user-details-header">\
		<a href="#">\
			<img class="avatar" src="/images/avatar.png" alt="avatar">\
			<span class="user-name"><%- user.username %></span>\
		</a>\
		<button class="follow">FOLLOW</button>\
	</div>\
	<div class="user-details-content">\
		<span>Interest Tags:</span>\
		<span class="label-value"><%- user.interestTags.join(", ") %></span>\
	</div>\
	<div class="user-details-content">\
		<span>Public Sprints:</span>\
		<span class="label-value">\
			<% user.sprints.forEach(function(sprint, index) { %>\
				<%- sprint.name %>\
				<% if(index < user.sprints.length - 1) { print(", ") }; %>\
			<% }) %>\
		</span>\
	</div>\
	<div class="user-details-content">\
		<span>Start Date:</span>\
		<span class="label-value"><%- $.datepicker.formatDate(\'mm/dd/yy\', user.updated) %></span>\
	</div>\
</div>\
';

var _createDiscussionForm = '\
<div class="new-post">\
	<form id="create-discussion" action="/discussion/save" method="post">\
		<div class="input-affordance left-addon">\
			<i class="fa fa-pencil"></i> \
			<input class="full-width discussion-topic-input"\
					type="text" placeholder="New question or discussion topic?"\
					name="name" id="discussion-topic" required />\
			<input type="radio" class="radio-public" name="visibility" id="public" value="public" checked><label for="public" class="radio-public-label">Public</label>\
			<input type="radio" class="radio-private" name="visibility" id="private" value="private"><label for="private" class="radio-private-label">Private</label>\
			<hr class="hide">\
			<input type="text" id="discussion-discription" class="full-width discussion-topic-description hide" placeholder="Enter comment/description"\
					name="discussionPost">\
		</div>\
		<input type="hidden" name="group" value="<%- groupName %>" />\
	</form>\
</div>\
';
var _discussions = ' \
<div class="feed-item">\
	<div class="discussion">\
		<div class="discussion-topic">\
		<div class="contents">\
				<div class="row">\
					<div class="col-xs-9 discussion-header">\
						<a href="#">\
							<img class="avatar" src="/images/avatar.png" alt="avatar">\
							<span class="user-name"> <%- discussionData.userName %></span>\
						</a>\
					</div>\
					<div class="col-xs-3 discussion-topic-span discussion-header">\
						<span class="posting-time" data-time="<%- discussionData.created %>"></span>\
						<% if (discussionData.isAdmin) { %>\
							<li class="dropdown">\
								<a href="#" data-toggle="dropdown"><b class="caret"></b></a>\
								<ul class="dropdown-menu" role="menu">\
									<li>\
										<a href="#" class="delete-discussion" data-discussion-hash-id="<%- discussionData.hash %>"> \
											<img src="/images/x.png" width="auto" height="23">Delete\
										</a>\
									</li>\
								</ul>\
							</li>\
						<% } %>\
					</div>\
				</div>\
				<div class="group"> \
					<%- discussionData.groupName %>\
				</div>\
				<div class="row">\
					<div class="col-xs-7">\
						<a href="/discussion/show/<%- discussionData.hash %>"> \
							<span> <%- discussionData.name ? discussionData.name: \'(No Title)\' %></span>\
						</a>\
					</div>\
					<div class="col-xs-5 button-box">\
						<div class="buttons">\
							<button onclick="showShareDialog(<%- discussionData.id %>)">\
								<img src="/images/follow.png" alt="follow">Follow\
							</button>\
							<% if (discussionData.isAdmin) {  %>\
								<button class="share-button" data-toggle="popover" title="Share:" data-placement="top" \
										data-content="<input class=\'share-link\' type=\'text\' value=\'<%- location.protocol+\'//\'+location.hostname+(location.port ? \':\' + location.port : \'\') %>/discussion/show/<%- discussionData.hash %>\'>">\
									<img src="/images/share.png" alt="share">Share\
								</button>\
							<% } %>\
							<button onclick="toggleCommentsList(<%- discussionData.id %>)">\
								<% if (!discussionData.totalComments || \
										discussionData.totalComments < 1) {  %>\
									<img src="/images/comment.png" alt="comment"> Comment</img>\
								<% } else { %>\
								<div class="dark-comment comment-button" data-total-comments="<%- discussionData.totalComments %>">\
									<%- discussionData.totalComments %></div>\
										Comment\
								<% } %>\
							</button>\
						</div>\
					</div>\
				</div>\
			</div>\
		</div>\
		<div class="discussion-comment hide" id="discussion<%- discussionData.id %>-comment-list">\
			<div class="comments">\
			</div>\
			<div class="bottom-margin">\
				<span class="view-comment">\
					VIEW MORE COMMENTS\
				</span>\
			</div>\
			<div class="row">\
				<div class="col-md-6 add-comment">\
					<form action="/discussionPost/save" method="post" id="commentForm">\
						<% if (false) { %>\
							<p>Enter your details below</p>\
								<div id="postname">\
									<input type="text" id="postname" name="postname" value="" class="postInput" /> Name\
							</div>\
							<div id="postemail">\
								<input type="text" id="postemail" name="postemail" value="" class="postInput" /> Email (not publicly visible)\
							</div>\
							<div id="posturl">\
								<input type="text" id="postsite" name="postsite" value=""\
										class="postInput" /> Website URL (optional)\
							</div>\
							<div id="postcomment">\
								<textarea rows="20" cols="100" style="border-style: solid"\
										id="postcommentarea" name="message"></textarea>\
							</div>\
							<br />\
							<input type="button" class="submitButton"\
									id="commentSubmitButton" value="submit" />\
							<!--p class="decorate">Comments must be approved, so will not appear immediately. </p-->\
						<% } else { %>\
							<input type="text" \
									placeholder="Add Comment..."\
									id="post-comment" name="message" required>\
						<% } %>\
						<input type="hidden" name="discussionHash" value="<%- discussionData.hash %>">\
					</form>\
				</div>\
				<div class="class-md-6"></div>\
			</div>\
		</div>\
	</div>\
</div>\
';

var _sprints = '\
	<div class="feed-item">\
		<div class="sprint">\
			<div class="contents">\
				<div class="row">\
					<div class="col-xs-9">\
						<a href="/home/sprint/<%- sprint.hash %>">\
							<span> <%- sprint.name %></span>\
						</a>\
					</div>\
					<div class="col-xs-3 align-right">\
						<span class="posting-time" data-time="<%- sprint.created %>"></span>\
					</div>\
				</div>\
				<div class="sprint-description">\
					<div>\
						<%- sprint.description %>\
					</div>\
				</div>\
				<div class="row">\
					<div class="col-xs-8 tags-participants-label">\
						<a href="/home/sprint/<%- sprint.hash %>">\
							TAGS(<%- sprint.totalTags %>)\
						</a>\
						<a href="/home/sprint/<%- sprint.hash %>">\
							PARTICIPANTS(<%- sprint.totalParticipants %>)\
						</a>\
					</div>\
					<div class="col-xs-4 align-right read-more">\
						<a href="/home/sprint/<%- sprint.hash %>">\
							VIEW MORE\
						</a>\
					</div>\
				</div>\
			</div>\
		</div>\
	</div>\
';
