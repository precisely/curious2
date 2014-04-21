<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery"/>
<html>
<head>
<meta name="layout" content="plot" />
<title>Curious</title>
<style type="text/css">
.ui-accordion-header {
	overflow:hidden;
}
</style>
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
}

function deletePost(postId) {
	showYesNo("Are you sure you want to delete this post?", function() {
			window.location = "/home/discuss?discussionId=${discussionId}&deletePostId=" + postId;
	});
}

$(function(){
	initTemplate();
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
								discussTitleArea.off('mouseup');
								discussTitleArea.on('mouseup', discussTitle.data('rename'));
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
			discussTitleArea.off('mouseup');
			discussTitleArea.on('mouseup', discussTitleRename);
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
});
</script>
</head>
<body class="discuss-page">
<!-- SHARE PAGE -->
<div id="container" class="sharePage" style="position:relative">
	<g:if test="${flash.message}">
		<div class="loginmessage">${flash.message.encodeAsHTML()}</div>
	</g:if>

	<div class="discussTitle" id="discussTitleArea">
		<h1><span id="discussTitleSpan"><g:if test="${!isNew}">${discussionTitle}</g:if></span>
		<div class="byline">
		<span class="username bittext" id="discussUsername"></span>
		<span class="date bittext" id="discussDate"></span>
		</div>
		</h1>

		<div id="plotLeftNav">
			<div class="discussPlotLines plotlines" id="plotLinesplotDiscussArea"></div>
		</div>

	</div>

<!-- MAIN -->
<g:if test="${firstPost?.getPlotDataId() != null}">
<div class="row row-custom">
<div class="col-xs-12">
<!-- /LEFT NAV-->
<div class="main discussmain" style="margin:0px">
	<div id="dialogDivplotDiscussArea" class="display:none;">
	</div>
	<div class="graphData">
		<div class="discussPlotArea" id="plotDiscussArea"></div>

		<div class="main discusscontrols">
			<div class="calendarRange">
				<div class="zoomline"><div id="zoomcontrol1"></div></div>
				<div class="dateline row">
					<div class="col-sm-4">
					<span class="startDate"><input id="startdatepicker1" type="text" value="" class="startdatepicker cycleInput"/></span>
					</div>
					<div class="col-sm-4">
					<span class="cycleTag" id="cycleTag1"><input type="text" class="cycleTagInput" name="cycletag" value="" class="cycleInput" /></span>
					</div>
					<div class="col-sm-4">
					<span class="endDate"><input id="enddatepicker1" type="text" value="" class="enddatepicker cycleInput"/></span>
					</div>
				</div>
			</div>
		</div>
		
	</div>
</div>
</div>
</div>
<!-- /MAIN -->

<div style="clear:both"></div>

<!-- PRE-FOOTER -->
<div id="preFooter">

	<div class="tagNav">
		view:
		<img src="/images/scatter.gif" alt="scatter" />
		<img src="/images/line.gif" alt="line" />
		<img src="/images/fill.gif" alt="fill" />
	</div>
</div>

<!-- /PRE-FOOTER -->

<div style="clear:both"></div>
</g:if>

<!-- COMMENTS -->
<div class="discusscomments">
	<g:if test="${firstPost?.getPlotDataId() != null && firstPost.getMessage() != null}">
	<div class="description">
		<div class="comment">${firstPost ? (firstPost.getMessage() ? firstPost.getMessage().encodeAsHTML() : "") : ""}</div>
		<div class="messageControls">
			<g:if test="${(firstPost.getAuthor().getUserId() == userId || isAdmin) && firstPost.getMessage() != null}">
				<!--span class="edit"></span-->
				<span class="delete"><a href="#" onclick="clearPostMessage(${firstPost.getId()})"><img src="/images/x.gif" width="8" height="8"></a></span>
			</g:if>
		</div>
		<!--<g:if test="${firstPost}">
			<div class="messageInfo">
				<div class="username"><g:if test="${firstPost.author.getSite()}"><a href="${firstPost.author.getSite().encodeAsURL()}"></g:if><g:if test="${firstPost.author.getUsername()}">${firstPost.author.getUsername().encodeAsHTML()}</g:if><g:else>${description.author.getName().encodeAsHTML()}</g:else><g:if test="${firstPost.author.getSite()}"></a></g:if></div>
				<div class="date"><g:formatDate date="${firstPost.getCreated()}" type="datetime" style="MEDIUM"/></div>
			</div>
		</g:if>-->
		<!--div class="button"><a href="#">Try it out. Track the same tags.</a></div -->
		<br/>&nbsp;
	</div>
	</g:if>

	<div class="commentList">
		<a name="comments"></a>
		<g:if test="${firstPost != null && firstPost.getPlotDataId() != null}">
		<h1>Comments</h1>
		</g:if>
				
		<g:each in="${posts}" var="post">
			<div class="comment">
				<a name="comment${post.getId()}"></a>
				<div class="message">${post.getMessage() ? post.getMessage().encodeAsHTML() : "[graph]"}</div>
				<div class="messageInfo">
					<g:if test="${post.author.getSite()}"><a href="${post.author.getSite().encodeAsURL()}"></g:if><g:if test="${post.author.getUsername()}">${post.author.getUsername().encodeAsHTML()}</g:if><g:else>${post.author.getName().encodeAsHTML()}</g:else><g:if test="${post.author.getSite()}"></a></g:if>
					<br/>Last post <g:formatDate date="${post.getUpdated()}" type="datetime" style="SHORT"/>
				</div>
				<div class="messageControls">
					<g:if test="${post.getAuthor().getUserId() == userId || isAdmin}">
						<!--span class="edit"></span-->
						<span class="delete"><a href="#" onclick="deletePost(${post.getId()})"><img src="/images/x.gif" width="8" height="8"></a></span>
					</g:if>
				</div>
				<div style="clear:both"></div>
			</div>
		</g:each>
	</div>
	<div id="addComment">
		<g:if test="${firstPost != null && firstPost.getMessage() != null}">
		<h1>Join the conversation</h1>
		</g:if>
        <form action="/home/discuss?commentForm=true" method="post" id="commentForm">
			<g:if test="${notLoggedIn}">
				<p>Enter your details below</p>
	
				<div id="postname"><input type="text" id="postname" name="postname" value="" class="postInput" /> Name</div>
				<div id="postemail"><input type="text" id="postemail" name="postemail" value="" class="postInput" /> Email (not publicly visible)</div>
				<div id="posturl"><input type="text" id="postsite" name="postsite" value="" class="postInput" /> Website URL (optional)</div>
				<div id="postcomment"><textarea rows="20" cols="100" style="border-style:solid" id="postcommentarea" name="message"></textarea></div>
				<br /><input type="button" class="submitButton" id="commentSubmitButton" value="submit" />
				<!--p class="decorate">Comments must be approved, so will not appear immediately. </p-->
			</g:if>
			<g:else>
				<div id="postcomment"><textarea rows="20" cols="100" style="border-style:solid" id="postcommentarea" name="message"></textarea></div>
				<br /><input type="button" class="submitButton" id="commentSubmitButton" value="submit" />
				<!--p class="decorate">Comments must be approved, so will not appear immediately. </p-->
			</g:else>
			<input type="hidden" name="discussionId" value="${discussionId}">
		</form>
	</div>
</div>
<!-- /COMMENTS -->

</div>
<!-- /TOTAL PAGE -->

<div style="clear:both;"></div>
	
</body>
</html>
