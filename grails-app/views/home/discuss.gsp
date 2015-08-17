<!DOCTYPE HTML>
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="plot" />
<script type="text/javascript" src="/js/curious/discussion.js?ver=21"></script>
<style type="text/css">
.ui-accordion-header {
	overflow: hidden;
}
</style>

<c:jsCSRFToken keys="getCommentsCSRF, getFeedsDataCSRF, addCommentCSRF, deleteDiscussionPostDataCSRF, deleteDiscussionDataCSRF" />

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
			window.location = "/home/discuss?discussionHash=${discussionHash}&clearPostId=" + postId;
	});
	return false;
}

$(function() {
	initTagListOnly();

	<content tag="processUserData">
		processUserData = function(data) {
			if (data == 'login') {
				data = [{id:-1,name:'',username:'(anonymous)',sex:''}];
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
				addPerson(this['name'],
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
						backgroundJSON("setting discussion name", makeGetUrl('setDiscussionNameData'), makeGetArgs({ discussionHash:"${discussionHash}", name:newName }), function(data) {
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
						$(".comment-form").submit();
					});
				}
			});
	
			$("#commentSubmitButton").click(function() {
				saveTitle(function() {
					$(".comment-form").submit();
				});
			});
			
			refreshPage();
		};
	</content>
	
	$('li#share-discussion').on('click', function() {
		showShareDialog(null);
		return false;
	});
});

window.isDiscussionSinglePage = true;
var discussionHash = "${discussionHash}";

$(document).ready(function() {
	commentsArgs.max = 5;
	getComments(discussionHash, commentsArgs);		// See discussion.js for "commentsArgs"

	$(".comments").infiniteScroll({
		bufferPx: 360,
		finalMessage: 'No more comments to show',
		onScrolledToBottom: function(e, $element) {
			// Pause the scroll event to not trigger again untill AJAX call finishes
			// Can be also called as: $("#postList").infiniteScroll("pause")
			this.pause();
			commentsArgs.offset = this.getOffset();

			getComments(discussionHash, commentsArgs, function(data) {
				if (!data.posts) {
					this.finish();
				} else {
					this.setNextPage();		// Increment offset for next page
					this.resume();			// Re start scrolling event to fetch next page data on reaching to bottom
				}
			}.bind(this));
		}
	});
});
</script>
</head>
<body class="discuss-page">
	<div id="container" class="sharePage">
		<div class="row red-header">
			<h1 class="clearfix">
				<span id="queryTitle">${associatedGroups[0]?.shared ? associatedGroups[0].fullName : 'Open to all'}</span>
			</h1>
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
				<div class="feed-item discussion" id="discussion-${discussionHash }">
						<div class="discussion-topic">
							<div class="contents">
								<div class="row">
									<div class="col-xs-9 discussion-header">
										<a href="#">
											<img class="avatar img-circle" src="/images/avatar.png" alt="...">
											&nbsp; <span class="username">${discussionOwner}</span>
										</a>
									</div>
									<div class="col-xs-3 discussion-topic-span discussion-header">
										<span class="posting-time" data-time="${discussionCreatedOn.time}"></span>
										<g:if test="${isAdmin }">
											<li class="dropdown">
												<a href="#" data-toggle="dropdown">
													<b class="caret"></b>
												</a>
												<ul class="dropdown-menu" role="menu">
													<li>
														<a href="#" class="delete-discussion" data-discussion-hash="${discussionHash}"> 
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
								<a href="#"> ${discussionTitle ?: '(No Title)' }</a>
								<p>
									${firstPost?.message}
								</p>
								<hr>
								<div class="buttons">
									<button class="share-button" data-toggle="popover" data-placement="top" 
											data-content="<input class='share-link' type='text' value='${grailsApplication.config.grails.serverURL}home/discuss?discussionHash=${discussionHash}'>"
											title="Share:">
										<img src="/images/share.png" alt="share"> Share
									</button>
									<button class="comment-button">
										<img src="/images/comment.png" alt="comment"> Comment
									</button>
								</div>
							</div>
						</div>
						<div class="commentList">
							<div class="discussion-comments-wrapper">
									<div class="add-comment-to-discussion">
										<form method="post" class="comment-form">
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
											<input type="hidden" name="discussionHash" value="${discussionHash}">
										</form>
									</div>
									<div class="comments media-list"></div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>

		<div style="clear: both;"></div>

		<g:hiddenField name="discussionHash" value="${discussionHash }" />

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
			<input type="hidden" name="discussionHash" value="${discussionHash}">
		</div>
		<c:renderJSTemplate template="/discussionPost/instance" id="_comments" />
	</div>
</div>
</body>
</html>