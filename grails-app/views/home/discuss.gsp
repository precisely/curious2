<!DOCTYPE HTML>
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="plot" />
<title>Curious</title>
<style type="text/css">
.ui-accordion-header {
	overflow: hidden;
}
</style>

<script type="text/javascript" src="/js/jquery/jquery.infinite.scroll.js"></script>
<script type="text/javascript">
// list of users to plot
function refreshPage() {
}

var plot = null;
var tagList = null;
var discussionTitle = "${discussionTitle}";
var blankPost = <g:if test="${firstPost == null}">true</g:if><g:else>false</g:else>;
var alreadySentName = null;
var preventCommentSubmit = false;

function doLogout() {
	callLogoutCallbacks();
}

function clearPostMessage(postId) {
	showYesNo("Are you sure you want to delete this comment?", function() {
			window.location = "/home/discuss?discussionId=${discussionId}&clearPostId=" + postId;
	});
	return false;
}

function deletePost(postId) {
	showYesNo("Are you sure you want to delete this post?", function() {
			window.location = "/home/discuss?discussionId=${discussionId}&deletePostId=" + postId;
	});
	return false;
}

$(function() {
	initTagListOnly();
	
	queueJSON("getting login info", "/home/getPeopleData?callback=?",
		function(data) {
			if (data == 'login') {
				data = [{id:-1,first:'',last:'',username:'(anonymous)',sex:''}];
			}
			if (data != 'login' && (!checkData(data)))
				return;
			
			found = false;
			
			jQuery.each(data, function() {
				if (!found) {
					// set first user id as the current
					setUserId(this['id']);
					setUserName(this['username']);
					plot = new Plot(tagList, this['id'], this['username'], "#plotDiscussArea", true, true, new PlotProperties({
						'startDate':'#startdatepicker1',
						'startDateInit':'start date and/or tag',
						'endDate':'#enddatepicker1',
						'endDateInit':'end date and/or tag',
						'cycleTag':'#cycleTag1',
						'zoomControl':'#zoomcontrol1',
						'username':'#queryUsername',
						'name':'',
						'logout':'#logoutLink'
						}));
					found = true;
				}
				addPerson(this['first'] + ' ' + this['last'],
					this['username'], this['id'], this['sex']);

				<g:if test="${firstPost?.getPlotDataId() != null}">
					plot.loadSnapshotId(${firstPost.getPlotDataId()});
				</g:if>

				return true;
			});

			var discussTitleArea = $("#discussTitleArea");
			var discussTitle = $("#discussTitleSpan");

			var saveTitle = function(closure) {
				var discussTitleInput = $("#discussTitleInput");
				if (discussTitleInput) {
					var newName = discussTitleInput.val();
					if (newName != alreadySentName && newName != discussionTitle && newName !== undefined) {
						alreadySentName = newName;
						preventCommentSubmit = true; // for some reason, comment submission happening twice?
						backgroundJSON("setting discussion name", makeGetUrl('setDiscussionNameData'), makeGetArgs({ discussionId:${discussionId}, name:newName }), function(data) {
							if (checkData(data)) {
								preventCommentSubmit = false;
								discussionTitle = newName;
								discussTitle.html(newName);
								discussTitle.off('mouseup');
								discussTitle.on('mouseup', discussTitle.data('rename'));
								if (closure) closure();
							} else {
								showAlert('Failed to set name');
							}
						});
					} else if (closure && (!preventCommentSubmit))
						closure();
				}
			}

			var renameDiscussionHandler = function(e) {
				if (e.keyCode == 13) {
					saveTitle();
					$("#postcommentarea").focus();
				}
			}

			var discussTitleRename = function(e) {
				discussTitleArea.off('mouseup');
				discussTitle.html('<input type="text" id="discussTitleInput"></input>');
				var discussTitleInput = $("#discussTitleInput");
				discussTitleInput.val(discussionTitle);
				discussTitleInput.keyup(renameDiscussionHandler);
				discussTitleInput.focus();
				discussTitleInput.blur(function() {
					saveTitle();
				});
			}

			discussTitle.data('rename', discussTitleRename);

			<g:if test="${!isNew}">
			discussTitle.off('mouseup');
			discussTitle.on('mouseup', discussTitleRename);
			$("#postcommentarea").focus();
			</g:if>
			<g:else>
			discussTitleRename();
			discussTitleInput.select();
			</g:else>

			$("#postcommentarea").keyup(function(e) {
				if (e.keyCode == 13) {
					saveTitle(function() {
						$("#commentForm").submit();
					});
				}
			});

			$("#commentSubmitButton").click(function() {
				saveTitle(function() {
					$("#commentForm").submit();
				});
			});
			
			refreshPage();
		});

	$(document).on("click", "ul#posts-pagination a", function() {
		var url = $(this).attr('href');
		$.ajax({
			url: url,
			success: function(data) {
				$('div#postList').html(data);
				wrapPagination();
			}
		});
		return false;
	});

	$('li#share-discussion').on('click', function() {
		showShareDialog(null);
		return false;
	});

});

$(document).ready(function() {
	$("#postList").infiniteScroll({
		bufferPx: 360,
		onFinishedMessage: 'No more comments to show',
		onScrolledToBottom: function(e, $element) {
			// Pause the scroll event to not trigger again untill AJAX call finishes
			// Can be also called as: $("#postList").infiniteScroll("pause")
			this.pause();

			$.ajax ({
				type: 'POST',
				url: '/home/discuss?discussionId=${discussionId}&offset=' + this.getOffset(),
				success: function(data) {
					if (data == "false") {
						this.finish();
					} else {
						$element.append(data);
						showCommentAgeFromDate();
						this.setNextPage();		// Increment offset for next page
						this.resume();			// Re start scrolling event to fetch next page data on reaching to bottom
					}
				}.bind(this)
			});
		}
	});
});
</script>
</head>
<body class="discuss-page">
	<!-- SHARE PAGE -->
	<div id="container" class="sharePage">
		<div class="row red-header">
				<!-- <span id="actions"> <span
					class="icon-triangle icon-triangle-right toggle"></span>
					<ul>
						<li id="share-discussion"><a href="#">Change Visibility</a></li>
						<li class="${isAdmin ? '' : 'disabled text-muted' }"><g:link
								params="[discussionId: params.discussionId, deleteDiscussion: true]"
								action="discuss">Delete</g:link></li>
					</ul>
				</span>--!><h1 class="clearfix">
                               			 <span id="queryTitle">${associatedGroups[0]?.shared ? associatedGroups[0].fullName : 'Open to all'}</span>	 			   </h1>
		</div>
		<div id="plotLeftNav">
			<div class="discussPlotLines plotlines" id="plotLinesplotDiscussArea"></div>
		</div>

		<!-- MAIN -->
		<g:if test="${firstPost?.getPlotDataId() != null}">
			<div class="row row-custom">
				<div class="col-xs-12">
					<!-- /LEFT NAV-->
					<div class="main discussmain" style="margin: 0px">
						<div id="dialogDivplotDiscussArea" class="display:none;"></div>
						<div class="graphData">
							<div class="discussPlotArea" id="plotDiscussArea"></div>

							<div class="main discusscontrols">
								<div class="calendarRange">
									<div class="zoomline">
										<div id="zoomcontrol1"></div>
									</div>
									<div class="dateline row">
										<div class="col-xs-4">
											<span class="startDate"><input id="startdatepicker1"
												type="text" value="" class="startdatepicker cycleInput" /></span>
										</div>
										<div class="col-xs-4">
											<!-- span class="cycleTag" id="cycleTag1"><input type="text" class="cycleTagInput" name="cycletag" value="" class="cycleInput" /></span -->
										</div>
										<div class="col-xs-4">
											<span class="endDate"><input id="enddatepicker1"
												type="text" value="" class="enddatepicker cycleInput" /></span>
										</div>
									</div>
								</div>
							</div>

						</div>
					</div>
				</div>
			</div>
			<!-- /MAIN -->

			<div style="clear: both"></div>

			<!-- PRE-FOOTER -->
			<div id="preFooter"></div>

			<!-- /PRE-FOOTER -->

			<div style="clear: both"></div>
		</g:if>

		<!-- COMMENTS -->
		<div class="main container-fluid">
			<div class="discusscomments">
				<g:if
					test="${firstPost?.getPlotDataId() != null && firstPost.getMessage() != null}">
					<div class="description">
						<div class="comment">
							${firstPost ? (firstPost.getMessage() ? firstPost.getMessage().encodeAsHTML() : "") : ""}
						</div>
						<div class="messageControls">
							<g:if
								test="${(firstPost.getAuthor().getUserId() == userId || isAdmin) && firstPost.getMessage() != null}">
								<!--span class="edit"></span-->
								<span class="delete"><a href="#"
									onclick="return clearPostMessage(${firstPost.getId()})"><img
										src="/images/x.gif" width="8" height="8"></a></span>
							</g:if>
						</div>
						<!--<g:if test="${firstPost}">
			<div class="messageInfo">
				<div class="username"><g:if test="${firstPost.author.getSite()}"><a href="${firstPost.author.getSite().encodeAsURL()}"></g:if><g:if test="${firstPost.author.getUsername()}">${firstPost.author.getUsername().encodeAsHTML()}</g:if><g:else>${description.author.getName().encodeAsHTML()}</g:else><g:if test="${firstPost.author.getSite()}"></a></g:if></div>
				<div class="date"><g:formatDate date="${firstPost.getCreated()}" type="datetime" style="MEDIUM"/></div>
			</div>
		</g:if>-->
						<!--div class="button"><a href="#">Try it out. Track the same tags.</a></div -->
						<br />&nbsp;
					</div>
				</g:if>

				<div class="feed-item">
					<div class="discussion">
						<div class="discussion-topic">
							<div class="contents">
								<div class="row">
									<div class="col-md-9 discussion-header">
										<a href="#">
											<img class="avatar" src="/images/avatar.png" alt="...">
											<span class="user-name"> ${discussionOwner}</span>
										</a>
									</div>
									<div class="col-md-3 discussion-topic-span discussion-header">
										<span class="posting-time" data-time="${discussionCreatedOn.time}"></span>
										<g:if test="${isAdmin }">
											<li class="dropdown">
												<a href="#" data-toggle="dropdown">
													<b class="caret"></b>
												</a>
												<ul class="dropdown-menu" role="menu">
													<li>
														<a href="#" class="delete-discussion" data-discussion-id="${discussionId}"> 
															<img src="/images/x.png" width="auto" height="23">Delete
														</a>
													</li>
												</ul>
											</li>
										</g:if>
									</div>
								</div>
								<div class="group">
									${associatedGroups[0]?.shared ? associatedGroups[0].fullName : 'Open to all'}
								</div>
								<a href="/home/discuss?discussionId=${discussionId }"> ${discussionTitle ?: '(No Title)' }</a>
								<p>
									${firstPost?.message}
								</p>
							</div>
							<hr>
							<div class="buttons">
								<button onclick="showShareDialog(${discussionId })">
									<img src="/images/share.png" alt="share">
								</button>
								<button onclick="showCommentDialog(null)">
									<img src="/images/comment.png" alt="comment">
								</button>
							</div>
						</div>
						<div class="commentList">
							<a name="comments"></a>
							<g:if
								test="${firstPost != null && firstPost.getPlotDataId() != null}">
								<h1>Comments</h1>
							</g:if>
							<div class="discussion-comment">
								<div class="row">
									<div class="add-comment-to-discussion">
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
												<input type="text" placeholder="Add Comment to this discussion..."
													id="post-comment" name="message" required>
											</g:else>
											<input type="hidden" name="discussionId"
												value="${discussionId}">
										</form>
									</div>
								</div>
								<div class="row">
									<a href="/home/discuss?discussionId=${discussionId }">
										<span class="view-comment">VIEW LESS COMMENTS (${totalPostCount})
									</span>
									</a>
								</div>
							</div>
							<div id="postList">
								<g:render template="/discussion/posts" model="[posts: posts]"></g:render>
							</div>
							<div id="getMoreComments"></div>
						</div>
					</div>
				</div>
			</div>
			<!-- /COMMENTS -->

		</div>
		<!-- /TOTAL PAGE -->

		<div style="clear: both;"></div>

		<g:hiddenField name="discussionId" value="${discussionId }" />

		<div id="share-dialog" class="hide" title="Share">
			<select name="shareOptions" id="shareOptions" multiple="multiple"
				class="form-control" size="8">
				<option value="isPublic" ${isPublic ? 'selected="selected"' : '' }>Visible
					to the world</option>
				<g:each in="${associatedGroups }" var="userGroup">
					<option value="${userGroup.id }"
						${userGroup.shared ? 'selected="selected"' : '' }>
						${userGroup.fullName }
					</option>
				</g:each>
			</select>
		</div>
		<div id="comment-dialog" class="hide" title="Comment">
			<input type="text" name="comment" id="userComment" required placeholder="Add Comment...">
			<input type="hidden" name="discussionId" value="${discussionId}">
		</div>
</div>
</body>
</html>
